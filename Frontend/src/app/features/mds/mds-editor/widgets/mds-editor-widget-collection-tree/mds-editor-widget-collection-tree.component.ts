import { ConnectedPosition } from '@angular/cdk/overlay';
import {
    Component,
    computed,
    DestroyRef,
    effect,
    ElementRef,
    inject,
    OnDestroy,
    OnInit,
    signal,
    untracked,
    viewChild,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { AuthenticationService, Node } from 'ngx-edu-sharing-api';
import { ValueType } from 'ngx-edu-sharing-ui';
import { map } from 'rxjs/operators';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';
import { UIHelper } from '../../../../../core-ui-module/ui-helper';
import { MdsEditorWidgetBase } from '../mds-editor-widget-base';
import {
    CollectionTreeConfig,
    DEFAULT_COLLECTION_TREE_PAGE_SIZE,
    DEFAULT_COLLECTION_TREE_SCOPES,
    parseCollectionTreeConfig,
    PERSONAL_COLLECTION_TREE_SCOPES,
} from './collection-tree-config';
import { CollectionTreeState, getCollectionTitle, PATH_SEPARATOR } from './collection-tree-state';
import { CollectionTreeService } from './collection-tree.service';
import {
    CollectionToggle,
    MdsEditorWidgetCollectionTreeCoreComponent,
} from './mds-editor-widget-collection-tree-core/mds-editor-widget-collection-tree-core.component';

type ResolvedCollection =
    | { readonly status: 'loading' | 'error' }
    | {
          readonly status: 'ready';
          readonly node: Node;
          readonly ancestorIds: string[];
          /** Titles from the top-most collection down to this one, `null` until loaded. */
          readonly path: string | null;
      };

export interface CollectionChip {
    readonly id: string;
    readonly status: ResolvedCollection['status'];
    readonly title: string;
    readonly path: string | null;
    readonly hasWrite: boolean;
}

/**
 * Selects collections from a lazily loaded, searchable tree of all collections the user can
 * read. The tree starts at the collection the hosting page passes (`collectionRootId` of the
 * editor), else at `rootCollectionId` of the widget configuration, else at the top level.
 */
@Component({
    selector: 'es-mds-editor-widget-collection-tree',
    templateUrl: './mds-editor-widget-collection-tree.component.html',
    styleUrls: ['./mds-editor-widget-collection-tree.component.scss'],
    standalone: false,
})
export class MdsEditorWidgetCollectionTreeComponent
    extends MdsEditorWidgetBase
    implements OnInit, OnDestroy
{
    private readonly collectionTreeService = inject(CollectionTreeService);
    private readonly destroyRef = inject(DestroyRef);
    private readonly isGuest = toSignal(
        inject(AuthenticationService)
            .observeLoginInfo()
            .pipe(map((login) => !!login?.isGuest)),
        { initialValue: false },
    );

    readonly valueType = ValueType.MultiValue;
    readonly control = new FormControl<string[]>([]);
    readonly overlayPositions: ConnectedPosition[] = [
        { originX: 'start', originY: 'bottom', offsetY: -34, overlayX: 'start', overlayY: 'top' },
        { originX: 'start', originY: 'top', overlayX: 'start', overlayY: 'bottom' },
    ];
    readonly config = signal<CollectionTreeConfig>({});
    readonly selectedIds = signal<string[]>([]);
    readonly selectedSet = computed(() => new Set(this.selectedIds()));
    private readonly hadValues = signal(false);
    /**
     * With `hideIfEmpty`, the widget shows up where the page passes a collection, like a search
     * opened from a topic page, and once it has a value, staying so the value can be replaced.
     */
    readonly hasValues = () => this.hadValues() || !!this.mdsEditorInstance.collectionRootId();
    readonly overlayOpen = signal(false);
    readonly treeState = signal<CollectionTreeState | null>(null);
    private readonly resolved = signal<ReadonlyMap<string, ResolvedCollection>>(new Map());
    readonly chips = computed(() =>
        this.selectedIds().map((id) => toChip(id, this.resolved().get(id))),
    );
    /** Collections the tree may start at, tried in order. */
    private readonly rootCandidates = computed(
        () => [
            ...new Set(
                [this.mdsEditorInstance.collectionRootId(), this.config().rootCollectionId].filter(
                    (id): id is string => !!id,
                ),
            ),
        ],
        { equal: sameIds },
    );

    private readonly originRef = viewChild.required<ElementRef<HTMLElement>>('origin');
    private readonly openButton = viewChild<MatButton>('openButton');
    private readonly core = viewChild(MdsEditorWidgetCollectionTreeCoreComponent);

    constructor() {
        super();
        effect(() => {
            this.rootCandidates();
            untracked(() => this.resetTreeState());
        });
    }

    async ngOnInit(): Promise<void> {
        this.config.set(parseCollectionTreeConfig(this.widget.definition.configuration));
        this.control.setValidators(this.getStandardValidators());
        const initialValues = (await this.widget.getInitalValuesAsync())?.jointValues ?? [];
        // the hosting page already holds the initial values, so no change is emitted for them
        this.control.setValue(initialValues, { emitEvent: false });
        this.applySelection(initialValues);
        this.control.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((ids) => {
            const next = ids ?? [];
            if (!sameIds(next, this.selectedIds())) {
                this.applySelection(next);
                this.setValue(next);
            }
        });
        this.registerValueChanges(this.control);
    }

    ngOnDestroy(): void {
        this.treeState()?.destroy();
    }

    focus(): void {
        this.openOverlay();
    }

    openOverlay(): void {
        if (this.control.disabled) {
            return;
        }
        if (!this.overlayOpen()) {
            this.ensureTreeState();
            // the overlay always opens with the tree, not with the results of an earlier search
            this.treeState().setSearchTerm('');
            this.overlayOpen.set(true);
        }
        setTimeout(() => this.core()?.focusInput());
    }

    toggleOverlay(): void {
        if (this.overlayOpen()) {
            this.closeOverlay(undefined, true);
        } else {
            this.openOverlay();
        }
    }

    closeOverlay(event?: MouseEvent, restoreFocus = false): void {
        // clicks on the widget itself are outside of the overlay, but handled by the widget
        if (
            UIHelper.isParentElementOfElement(
                event?.target as HTMLElement,
                this.originRef().nativeElement,
            )
        ) {
            return;
        }
        this.overlayOpen.set(false);
        if (restoreFocus) {
            this.openButton()?.focus();
        }
        this.onBlur.emit();
    }

    /** Closes the overlay when the focus leaves both the widget and the overlay. */
    onFocusOut(event: FocusEvent): void {
        const next = event.relatedTarget as HTMLElement | null;
        if (
            !this.overlayOpen() ||
            !next ||
            UIHelper.isParentElementOfElement(next, this.originRef().nativeElement) ||
            next.closest('.suggestion-card')
        ) {
            return;
        }
        this.overlayOpen.set(false);
        this.onBlur.emit();
    }

    onOverlayKeydown(event: KeyboardEvent): void {
        if (event.key === 'Escape') {
            event.stopPropagation();
            this.closeOverlay(undefined, true);
        } else if (this.core()?.handleKeydown(event)) {
            event.preventDefault();
        }
    }

    onToggled({ node, ancestorIds, checked }: CollectionToggle): void {
        const id = node.ref.id;
        const current = this.selectedIds();
        if (checked === current.includes(id)) {
            return;
        }
        if (checked && !this.resolved().has(id)) {
            this.setResolved(id, { status: 'ready', node, ancestorIds, path: null });
            this.loadPath(id);
        }
        // a selected collection includes its sub-collections, so selected ones below it are dropped
        const next = checked
            ? [...current.filter((selectedId) => !this.isBelow(selectedId, id)), id]
            : current.filter((selectedId) => selectedId !== id);
        this.control.setValue(next);
        this.control.markAsDirty();
    }

    /** Opens the overlay with the chip's collection revealed in the tree. */
    onChipClick(chip: CollectionChip): void {
        this.openOverlay();
        const collection = this.resolved().get(chip.id);
        if (collection?.status === 'ready') {
            const path = [...collection.ancestorIds, chip.id];
            setTimeout(() => this.core()?.reveal(path));
        }
    }

    remove(id: string): void {
        this.control.setValue(this.selectedIds().filter((selectedId) => selectedId !== id));
        this.control.markAsDirty();
    }

    private applySelection(ids: string[]): void {
        this.selectedIds.set(ids);
        if (ids.length) {
            this.hadValues.set(true);
        }
        for (const id of ids) {
            if (!this.resolved().has(id)) {
                this.setResolved(id, { status: 'loading' });
                this.loadPath(id);
            }
        }
    }

    private loadPath(id: string): void {
        this.collectionTreeService
            .getAncestors(id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (path) =>
                    this.setResolved(id, {
                        status: 'ready',
                        node: path[path.length - 1],
                        ancestorIds: path.slice(0, -1).map((node) => node.ref.id),
                        path: path.map(getCollectionTitle).join(PATH_SEPARATOR),
                    }),
                error: () => {
                    // a collection selected in the tree stays usable without its path
                    if (this.resolved().get(id)?.status !== 'ready') {
                        this.setResolved(id, { status: 'error' });
                    }
                },
            });
    }

    private setResolved(id: string, collection: ResolvedCollection): void {
        this.resolved.update((resolved) => new Map(resolved).set(id, collection));
    }

    private isBelow(id: string, ancestorId: string): boolean {
        const collection = this.resolved().get(id);
        return collection?.status === 'ready' && collection.ancestorIds.includes(ancestorId);
    }

    private ensureTreeState(): void {
        if (this.treeState()) {
            return;
        }
        const config = this.config();
        const state = new CollectionTreeState(this.collectionTreeService, {
            rootCandidates: this.rootCandidates(),
            // guests have no collections of their own or shared with them
            scopes: (config.scopes ?? DEFAULT_COLLECTION_TREE_SCOPES).filter(
                (scope) => !this.isGuest() || !PERSONAL_COLLECTION_TREE_SCOPES.includes(scope),
            ),
            pageSize:
                (config.pageSize ?? 0) > 0 ? config.pageSize : DEFAULT_COLLECTION_TREE_PAGE_SIZE,
            mdsId: this.mdsEditorInstance.mdsId,
        });
        state.init();
        this.treeState.set(state);
    }

    private resetTreeState(): void {
        const state = this.treeState();
        if (!state) {
            return;
        }
        state.destroy();
        this.treeState.set(null);
        if (this.overlayOpen()) {
            this.ensureTreeState();
        }
    }
}

function toChip(id: string, collection: ResolvedCollection | undefined): CollectionChip {
    if (collection?.status === 'ready') {
        return {
            id,
            status: 'ready',
            title: getCollectionTitle(collection.node),
            path: collection.path,
            hasWrite: !!collection.node.access?.includes(RestConstants.ACCESS_WRITE),
        };
    }
    const status = collection?.status ?? 'loading';
    // an unreadable collection can only be shown by its id
    return { id, status, title: status === 'error' ? id : '', path: null, hasWrite: true };
}

function sameIds(a: readonly string[], b: readonly string[]): boolean {
    return a.length === b.length && a.every((id, index) => id === b[index]);
}
