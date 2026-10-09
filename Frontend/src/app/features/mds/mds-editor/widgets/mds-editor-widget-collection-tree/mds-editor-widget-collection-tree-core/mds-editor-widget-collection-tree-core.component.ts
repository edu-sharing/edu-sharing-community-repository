import { LiveAnnouncer } from '@angular/cdk/a11y';
import {
    afterRenderEffect,
    Component,
    computed,
    DestroyRef,
    effect,
    ElementRef,
    inject,
    input,
    output,
    signal,
    untracked,
    viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslateService } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { Subscription } from 'rxjs';
import { RestConstants } from '../../../../../../core-module/rest/rest-constants';
import { COLLECTION_TREE_SCOPE_ICONS } from '../collection-tree-config';
import {
    CollectionCategoryRow,
    CollectionNodeRow,
    CollectionStatusRow,
    CollectionTreeState,
    getCollectionTitle,
} from '../collection-tree-state';

/** A node row together with what the panel shows for it. */
export interface CollectionNodeView extends CollectionNodeRow {
    readonly title: string;
    readonly checked: boolean;
    readonly disabled: boolean;
    readonly hasWrite: boolean;
    /** Translation key explaining why the row cannot be changed. */
    readonly hint: string | null;
}

export type CollectionRowView = CollectionNodeView | CollectionCategoryRow | CollectionStatusRow;

export interface CollectionToggle {
    readonly node: Node;
    readonly ancestorIds: string[];
    readonly checked: boolean;
}

let nextUniqueId = 0;

/**
 * Overlay content of the `collectionTree` widget: a search field above either the lazily loaded
 * collection tree or the search hits placed in it. Rows are navigated via `aria-activedescendant`.
 */
@Component({
    selector: 'es-mds-editor-widget-collection-tree-core',
    templateUrl: './mds-editor-widget-collection-tree-core.component.html',
    styleUrls: ['./mds-editor-widget-collection-tree-core.component.scss'],
    standalone: false,
})
export class MdsEditorWidgetCollectionTreeCoreComponent {
    private readonly destroyRef = inject(DestroyRef);
    private readonly liveAnnouncer = inject(LiveAnnouncer);
    private readonly translate = inject(TranslateService);

    readonly state = input.required<CollectionTreeState>();
    readonly selected = input.required<ReadonlySet<string>>();
    readonly requireWrite = input(false);
    readonly toggled = output<CollectionToggle>();
    readonly closed = output<void>();

    readonly uid = `es-collection-tree-${nextUniqueId++}`;
    readonly activeKey = signal<string | null>(null);
    private readonly inputRef = viewChild<ElementRef<HTMLInputElement>>('inputRef');
    private revealSubscription: Subscription | null = null;

    readonly searchActive = computed(() => this.state().searchActive());
    readonly rootTitle = computed(() => {
        const root = this.state().rootNode();
        return root ? getCollectionTitle(root) : '';
    });
    readonly rows = computed((): CollectionRowView[] => {
        const state = this.state();
        const rows = state.searchActive() ? state.searchRows() : state.treeRows();
        const selected = this.selected();
        const requireWrite = this.requireWrite();
        return rows.map((row) =>
            row.kind === 'node' ? toNodeView(row, selected, requireWrite) : row,
        );
    });
    /** Rows reachable with the arrow keys. */
    private readonly navigableRows = computed(() =>
        this.rows().filter((row) => ['node', 'category', 'more', 'error'].includes(row.kind)),
    );
    readonly activeDescendant = computed(() => {
        const key = this.activeKey();
        return key && this.navigableRows().some((row) => row.key === key) ? this.rowId(key) : null;
    });

    constructor() {
        afterRenderEffect(() => {
            const id = this.activeDescendant();
            if (id) {
                document.getElementById(id)?.scrollIntoView({ block: 'nearest' });
            }
        });
        effect(() => {
            const state = this.state();
            if (state.searchActive() && state.searchStatus() === 'ready') {
                const count = state.searchTotal();
                untracked(() => {
                    void this.liveAnnouncer.announce(
                        this.translate.instant('MDS.COLLECTION_TREE.RESULTS_COUNT', { count }),
                    );
                });
            }
        });
    }

    focusInput(): void {
        const element = this.inputRef()?.nativeElement;
        element?.focus();
        element?.setSelectionRange(element.value.length, element.value.length);
    }

    /** Shows the collection at the end of `path` in the tree and makes it the active row. */
    reveal(path: readonly string[]): void {
        this.state().setSearchTerm('');
        this.revealSubscription?.unsubscribe();
        this.revealSubscription = this.state()
            .reveal(path)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((key) => this.activeKey.set(key));
    }

    readonly scopeIcons = COLLECTION_TREE_SCOPE_ICONS;

    rowId(key: string): string {
        return `${this.uid}-row-${key}`;
    }

    onSearchInput(event: Event): void {
        this.activeKey.set(null);
        this.state().setSearchTerm((event.target as HTMLInputElement).value);
    }

    toggle(row: CollectionNodeView, checked: boolean): void {
        if (row.disabled) {
            return;
        }
        this.toggled.emit({ node: row.node, ancestorIds: row.ancestorIds ?? [], checked });
    }

    toggleExpanded(row: CollectionNodeView): void {
        this.state().toggleExpanded(row.item);
    }

    loadMore(row: CollectionStatusRow): void {
        this.state().loadMore(row.target);
    }

    retry(row: CollectionStatusRow): void {
        this.state().retry(row.target);
    }

    /**
     * Handles a key pressed while the overlay is open.
     *
     * @returns whether the key was consumed and its default action is to be prevented
     */
    handleKeydown(event: KeyboardEvent): boolean {
        const target = event.target as HTMLElement | null;
        // the buttons and checkboxes above the rows handle their keys themselves
        if (target?.closest('.panel-header') && target !== this.inputRef()?.nativeElement) {
            return false;
        }
        const rows = this.navigableRows();
        const index = rows.findIndex((row) => row.key === this.activeKey());
        const active = index >= 0 ? rows[index] : null;
        switch (event.key) {
            case 'ArrowDown':
                this.activeKey.set(rows[Math.min(index + 1, rows.length - 1)]?.key ?? null);
                return true;
            case 'ArrowUp':
                this.activeKey.set(rows[Math.max(index - 1, 0)]?.key ?? null);
                return true;
            case 'ArrowRight':
                if (active?.kind === 'category') {
                    this.state().expandCategory(active.category);
                    return true;
                }
                if (active?.kind !== 'node') {
                    return false;
                }
                if (active.expandable && !active.expanded) {
                    this.state().expand(active.item);
                }
                return true;
            case 'ArrowLeft':
                if (active?.kind === 'category') {
                    this.state().collapseCategory(active.category);
                    return true;
                }
                if (active?.kind !== 'node') {
                    return false;
                }
                this.collapseOrMoveUp(active, rows);
                return true;
            // a space is typed into the search field unless a row is active
            case ' ':
            case 'Enter':
                if (!active) {
                    return false;
                }
                if (active.kind === 'node') {
                    this.toggle(active, !active.checked);
                } else if (active.kind === 'category') {
                    this.state().toggleCategory(active.category);
                } else if (active.kind === 'more') {
                    this.loadMore(active);
                } else {
                    this.retry(active);
                }
                return true;
        }
        return false;
    }

    private collapseOrMoveUp(active: CollectionNodeView, rows: CollectionRowView[]): void {
        if (active.expanded) {
            this.state().collapse(active.item);
        } else if (active.parentKey && rows.some((row) => row.key === active.parentKey)) {
            this.activeKey.set(active.parentKey);
        }
    }
}

/** Derives the selection state and write access shown for a node row. */
function toNodeView(
    row: CollectionNodeRow,
    selected: ReadonlySet<string>,
    requireWrite: boolean,
): CollectionNodeView {
    const id = row.node.ref.id;
    // a selected collection includes all of its sub-collections
    const implied = !!row.ancestorIds?.some((ancestorId) => selected.has(ancestorId));
    const access = row.node.access;
    // collections listed only as the location of a search hit come without the user's rights
    const hasWrite = !access || access.includes(RestConstants.ACCESS_WRITE);
    const writeMissing =
        requireWrite && !access?.includes(RestConstants.ACCESS_WRITE) && !selected.has(id);
    return {
        ...row,
        title: getCollectionTitle(row.node),
        checked: implied || selected.has(id),
        disabled: implied || writeMissing,
        hasWrite,
        hint: implied
            ? 'MDS.COLLECTION_TREE.INCLUDED_BY_PARENT'
            : writeMissing
            ? 'MDS.COLLECTION_TREE.WRITE_REQUIRED'
            : null,
    };
}
