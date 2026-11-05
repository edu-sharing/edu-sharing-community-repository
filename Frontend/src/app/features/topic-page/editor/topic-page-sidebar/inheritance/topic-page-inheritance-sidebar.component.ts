import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    Injector,
    Signal,
    resource,
    ResourceRef,
    signal,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Node, PROPERTY_FILTER_ALL, RestConstants } from 'ngx-edu-sharing-api';
import { catchError, firstValueFrom, map, Observable, of } from 'rxjs';
import { Toast } from '../../../../../services/toast';
import { SharedModule } from '../../../../../shared/shared.module';
import { TopicPageHelperService } from '../../../shared/services/topic-page-helper.service';
import {
    ResolvedInheritance,
    resolveInheritance,
    stopsPropagation,
} from '../../../shared/utils/topic-page-inheritance-util';
import { checkUserAccess } from '../../../shared/utils/node-util';
import {
    retrievePageConfigPropagateRef,
    retrievePageConfigRef,
} from '../../../shared/utils/template-util';
import { TopicPageSidebarService } from '../topic-page-sidebar.service';

type InheritanceState =
    | { kind: 'own' }
    | { kind: 'inherits'; from: Node }
    | { kind: 'stopped'; at: Node }
    | { kind: 'none' };

/** i18n key and params describing a state */
type StateText = { key: string; params?: Record<string, string> };

// the sub-collections of a row and how they are shown
type Subcollections = {
    nodes: Node[];
    exist: boolean;
    expandable: boolean;
    expanded: boolean;
    isLoading: boolean;
};

export type InheritanceRow = {
    node: Node;
    depth: number;
    expandable: boolean;
    expanded: boolean;
    isLoading: boolean;
    state: InheritanceState;
    stateText: StateText;
    // the state as saved, set while unsaved changes alter it
    savedStateText: StateText | null;
    passesOwnTemplates: boolean;
    // whether the collection passes on what it inherits, unsaved changes included
    passesOn: boolean;
    // the "pass on" switch only matters for a collection receiving templates it could pass on
    canToggle: boolean;
};

/**
 * Shows how a collection and its sub-collections inherit topic pages, and lets the user decide
 * where inherited templates are no longer passed on. Changes are collected first, then saved.
 */
@Component({
    selector: 'es-topic-page-inheritance-sidebar',
    imports: [SharedModule],
    templateUrl: './topic-page-inheritance-sidebar.component.html',
    styleUrls: ['./topic-page-inheritance-sidebar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TopicPageInheritanceSidebarComponent {
    private readonly topicPageHelperService = inject(TopicPageHelperService);
    private readonly toast = inject(Toast);
    private readonly injector = inject(Injector);

    /** the collection whose topic page is shown */
    readonly collectionId: Signal<string> = inject(TopicPageSidebarService).host.collectionNodeId;
    readonly i18nPrefix: string = 'TOPIC_PAGE.SIDEBAR.INHERITANCE.';
    readonly isSaving = signal(false);
    // the collection uncached, its ancestors nearest first, and its sub-collections
    readonly collectionHierarchy = resource({
        params: () => this.collectionId(),
        loader: async ({
            params: id,
        }): Promise<{ collection: Node; parents: Node[]; subcollections: Node[] }> => {
            const [collection, parents, subcollections] = await Promise.all([
                this.topicPageHelperService.getNodeUncached(id),
                this.topicPageHelperService.getNodeParents(id),
                firstValueFrom(this.loadSubcollections(id)),
            ]);
            return { collection, parents: parents.nodes ?? [], subcollections };
        },
    });
    // the sub-collections of every collection expanded so far, by collection id
    private readonly subcollectionLists = signal<Map<string, ResourceRef<Node[] | undefined>>>(
        new Map(),
    );
    private readonly expanded = signal<Set<string>>(new Set());
    // unsaved stop flags by collection id, only those differing from the saved ones
    private readonly unsavedChanges = signal<Map<string, boolean>>(new Map());

    readonly rows = computed((): InheritanceRow[] => {
        if (!this.collectionHierarchy.hasValue()) {
            return [];
        }
        const { collection, parents, subcollections } = this.collectionHierarchy.value();
        const incoming: ResolvedInheritance = resolveInheritance(collection, parents);
        const rows: InheritanceRow[] = [];
        // the collection of the page comes with its sub-collections and always shows them
        this.addRows(rows, collection, 0, incoming, incoming, {
            nodes: subcollections,
            exist: subcollections.length > 0,
            expandable: false,
            expanded: true,
            isLoading: this.collectionHierarchy.isLoading(),
        });
        return rows;
    });
    readonly changedCount = computed(
        (): number => this.rows().filter((row: InheritanceRow) => row.savedStateText).length,
    );
    readonly hasUnsavedChanges = computed((): boolean => this.unsavedChanges().size > 0);

    readonly rowLevel = (row: InheritanceRow): number => row.depth;
    readonly rowKey = (row: InheritanceRow): string => row.node.ref.id;

    setExpanded(row: InheritanceRow, expanded: boolean): void {
        const id: string = row.node.ref.id;
        if (!row.expandable || this.expanded().has(id) === expanded) {
            return;
        }
        const expandedIds = new Set(this.expanded());
        if (expanded) {
            expandedIds.add(id);
            if (!this.subcollectionLists().has(id)) {
                const subcollectionList = rxResource({
                    params: () => id,
                    stream: ({ params }) => this.loadSubcollections(params),
                    injector: this.injector,
                });
                this.subcollectionLists.update((lists) =>
                    new Map(lists).set(id, subcollectionList),
                );
            }
        } else {
            expandedIds.delete(id);
        }
        this.expanded.set(expandedIds);
    }

    setPassesOn(row: InheritanceRow, passesOn: boolean): void {
        const changes = new Map(this.unsavedChanges());
        if (stopsPropagation(row.node) === !passesOn) {
            changes.delete(row.node.ref.id);
        } else {
            changes.set(row.node.ref.id, !passesOn);
        }
        this.unsavedChanges.set(changes);
    }

    discardChanges(): void {
        this.unsavedChanges.set(new Map());
    }

    async saveChanges(): Promise<void> {
        this.isSaving.set(true);
        const property: string = RestConstants.CCM_PROP_PAGE_CONFIG_PROPAGATE_STOP;
        try {
            await Promise.all(
                [...this.unsavedChanges()].map(([id, stop]) =>
                    stop
                        ? this.topicPageHelperService.setProperty(id, property, 'true')
                        : this.topicPageHelperService.resetProperty(id, property),
                ),
            );
            this.unsavedChanges.set(new Map());
            this.toast.toast(this.i18nPrefix + 'SAVED');
        } catch (error) {
            this.topicPageHelperService.displayErrorToast(error);
        } finally {
            this.isSaving.set(false);
            this.collectionHierarchy.reload();
            for (const subcollectionList of this.subcollectionLists().values()) {
                subcollectionList.reload();
            }
        }
    }

    /** Adds the row of a collection and those of its expanded sub-collections. */
    private addRows(
        rows: InheritanceRow[],
        node: Node,
        depth: number,
        incoming: ResolvedInheritance,
        savedIncoming: ResolvedInheritance,
        subcollections: Subcollections,
    ): void {
        const savedStop: boolean = stopsPropagation(node);
        const stop: boolean = this.unsavedChanges().get(node.ref.id) ?? savedStop;
        const state: InheritanceState = this.stateOf(node, incoming);
        const savedState: InheritanceState = this.stateOf(node, savedIncoming);
        const passesOwnTemplates: boolean = !!retrievePageConfigPropagateRef(node);
        rows.push({
            node,
            depth,
            expandable: subcollections.expandable,
            expanded: subcollections.expanded,
            isLoading: subcollections.isLoading,
            state,
            stateText: this.stateText(state),
            savedStateText: this.sameState(state, savedState) ? null : this.stateText(savedState),
            passesOwnTemplates,
            passesOn: !stop,
            canToggle:
                subcollections.exist &&
                !passesOwnTemplates &&
                (!!incoming.source || stop) &&
                checkUserAccess(node),
        });
        if (!subcollections.expanded) {
            return;
        }
        for (const child of subcollections.nodes) {
            this.addRows(
                rows,
                child,
                depth + 1,
                this.outgoing(node, incoming, stop),
                this.outgoing(node, savedIncoming, savedStop),
                this.subcollectionsOf(child),
            );
        }
    }

    /** The sub-collections of a collection below the one of the page, loaded once expanded. */
    private subcollectionsOf(collection: Node): Subcollections {
        const id: string = collection.ref.id;
        const list: ResourceRef<Node[] | undefined> | undefined = this.subcollectionLists().get(id);
        const exist: boolean = (collection.collection?.childCollectionsCount ?? 0) > 0;
        return {
            nodes: list?.value() ?? [],
            exist,
            expandable: exist,
            expanded: exist && this.expanded().has(id),
            isLoading: !!list?.isLoading(),
        };
    }

    private outgoing(
        node: Node,
        incoming: ResolvedInheritance,
        stop: boolean,
    ): ResolvedInheritance {
        if (retrievePageConfigPropagateRef(node)) {
            return { source: node, stoppedAt: null };
        }
        if (stop && incoming.source) {
            return { source: null, stoppedAt: node };
        }
        return incoming;
    }

    private stateOf(node: Node, incoming: ResolvedInheritance): InheritanceState {
        if (retrievePageConfigRef(node)) {
            return { kind: 'own' };
        }
        if (incoming.source) {
            return { kind: 'inherits', from: incoming.source };
        }
        if (incoming.stoppedAt) {
            return { kind: 'stopped', at: incoming.stoppedAt };
        }
        return { kind: 'none' };
    }

    private stateText(state: InheritanceState): StateText {
        const title = (node: Node): string => node.title || node.name;
        switch (state.kind) {
            case 'own':
                return { key: 'STATE_OWN' };
            case 'inherits':
                return { key: 'STATE_INHERITS', params: { title: title(state.from) } };
            case 'stopped':
                return { key: 'STATE_STOPPED', params: { title: title(state.at) } };
            default:
                return { key: 'STATE_NONE' };
        }
    }

    private sameState(a: InheritanceState, b: InheritanceState): boolean {
        const nodeOf = (state: InheritanceState): string | undefined =>
            state.kind === 'inherits'
                ? state.from.ref.id
                : state.kind === 'stopped'
                ? state.at.ref.id
                : undefined;
        return a.kind === b.kind && nodeOf(a) === nodeOf(b);
    }

    private loadSubcollections(id: string): Observable<Node[]> {
        return this.topicPageHelperService
            .getSubcollections(id, true, { propertyFilter: [PROPERTY_FILTER_ALL] })
            .pipe(
                map((entries) => entries.collections ?? []),
                catchError((error: unknown) => {
                    this.topicPageHelperService.displayErrorToast(error);
                    return of([]);
                }),
            );
    }
}
