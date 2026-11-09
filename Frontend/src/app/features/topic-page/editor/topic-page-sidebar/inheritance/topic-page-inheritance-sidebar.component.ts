import {
    afterRenderEffect,
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    ElementRef,
    inject,
    Injector,
    Signal,
    resource,
    ResourceRef,
    signal,
    viewChild,
    viewChildren,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslateService } from '@ngx-translate/core';
import { Node, PROPERTY_FILTER_ALL, RestConstants } from 'ngx-edu-sharing-api';
import { catchError, firstValueFrom, map, Observable, of } from 'rxjs';
import { Toast } from '../../../../../services/toast';
import { SharedModule } from '../../../../../shared/shared.module';
import { OPEN_PAGE_TEMPLATE_QUERY_PARAM } from '../../../shared/types/custom-definitions';
import { TopicPageGlobalService } from '../../../shared/services/topic-page-global.service';
import { TopicPageHelperService } from '../../../shared/services/topic-page-helper.service';
import {
    ResolvedInheritance,
    resolveInheritance,
    stopsPropagation,
} from '../../../shared/utils/topic-page-inheritance-util';
import { checkUserAccess } from '../../../shared/utils/node-util';
import {
    retrievePageConfig,
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

/**
 * ancestor: read-only path from the inheritance source down to the collection of the page;
 * current: the collection of the page; subcollection: anything below it
 */
type RowRole = 'ancestor' | 'current' | 'subcollection';

export type InheritanceRow = {
    node: Node;
    role: RowRole;
    depth: number;
    expandable: boolean;
    expanded: boolean;
    isLoading: boolean;
    state: InheritanceState;
    stateText: StateText;
    // the state as saved, set while unsaved changes alter it
    savedStateText: StateText | null;
    hasOwnPage: boolean;
    passesOwnTemplates: boolean;
    // whether the collection passes templates on, its own or inherited, unsaved changes included
    passesOn: boolean;
    // the "pass on" switch only matters for a collection with templates it could pass on
    canToggle: boolean;
    // whether the user switched passing on for this collection without saving yet
    toggled: boolean;
};

/** A line from a collection passing its templates on to the shown rows inheriting them. */
type TemplateLine = { sourceId: string; color: number; targetIds: string[] };

/** The drawing of a template line, or of a stop mark below a collection passing nothing on. */
type LineDrawing =
    | { kind: 'line'; color: number; path: string }
    | { kind: 'stop'; path: string; cx: number; cy: number };

// number of line colors before they repeat, see --inheritanceLine* in the stylesheet
const LINE_COLORS: number = 3;
// radius of the bend from a line into a row, and the size of its arrow head
const LINE_BEND: number = 8;
const LINE_ARROW: number = 6;

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
    private readonly topicPageGlobalService = inject(TopicPageGlobalService);
    private readonly toast = inject(Toast);
    private readonly translate = inject(TranslateService);
    private readonly injector = inject(Injector);

    /** the collection whose topic page is shown */
    private readonly topicPageSidebarService = inject(TopicPageSidebarService);
    readonly collectionId: Signal<string> = this.topicPageSidebarService.host.collectionNodeId;
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
    // titles of the templates passed on, by propagate config ref; filled once a row shows it
    private readonly passedTemplateTitles = signal<Map<string, string[]>>(new Map());
    private readonly requestedPropagateConfigs = new Set<string>();
    private readonly templateHints = viewChildren<ElementRef<HTMLElement>>('templatesHint');
    // ids of the collections whose list of passed on templates is cut off
    readonly truncatedTemplateHints = signal<ReadonlySet<string>>(new Set());
    private readonly treeWrapper = viewChild<ElementRef<HTMLElement>>('treeWrapper');
    // changes whenever the tree is resized, so the lines are measured again
    private readonly treeSize = signal(0);
    readonly lineDrawings = signal<LineDrawing[]>([]);

    readonly rows = computed((): InheritanceRow[] => {
        if (!this.collectionHierarchy.hasValue()) {
            return [];
        }
        const { collection, parents, subcollections } = this.collectionHierarchy.value();
        const ancestors: Node[] = this.ancestorsFromSource(collection, parents);
        const rows: InheritanceRow[] = [];
        // what reaches the topmost row, then passed down the path including unsaved changes
        const topmost: Node = ancestors[0] ?? collection;
        const above: Node[] = parents.slice(
            parents.findIndex((parent) => parent.ref.id === topmost.ref.id) + 1,
        );
        let incoming: ResolvedInheritance = resolveInheritance(topmost, above);
        let savedIncoming: ResolvedInheritance = incoming;
        ancestors.forEach((ancestor: Node, depth: number) => {
            // each parent leads to the next one on the path, or to the collection of the page
            const next: Subcollections = {
                nodes: [],
                exist: true,
                expandable: false,
                expanded: true,
                isLoading: false,
            };
            rows.push(this.buildRow(ancestor, 'ancestor', depth, incoming, savedIncoming, next));
            const savedStop: boolean = stopsPropagation(ancestor);
            incoming = this.outgoing(ancestor, incoming, this.stopOf(ancestor));
            savedIncoming = this.outgoing(ancestor, savedIncoming, savedStop);
        });
        // the collection of the page comes with its sub-collections and always shows them
        this.addRows(rows, collection, ancestors.length, 0, incoming, savedIncoming, {
            nodes: subcollections,
            exist: subcollections.length > 0,
            expandable: false,
            expanded: subcollections.length > 0,
            isLoading: this.collectionHierarchy.isLoading(),
        });
        return rows;
    });
    readonly templateLines = computed((): TemplateLine[] => {
        const rows: InheritanceRow[] = this.rows();
        const lines: TemplateLine[] = [];
        for (const row of rows) {
            if (!row.passesOwnTemplates) {
                continue;
            }
            // given even while stopped, so turning passing on keeps the colors of the others
            const color: number = this.lineColorFor(row.node.ref.id);
            if (!row.passesOn) {
                continue;
            }
            lines.push({
                sourceId: row.node.ref.id,
                color,
                targetIds: rows
                    .filter(
                        (target) =>
                            target.state.kind === 'inherits' &&
                            target.state.from.ref.id === row.node.ref.id,
                    )
                    .map((target) => target.node.ref.id),
            });
        }
        return lines;
    });
    // the line color of every collection passing templates on, by its id
    private readonly lineColors = computed(
        (): Map<string, number> =>
            new Map(this.templateLines().map((line) => [line.sourceId, line.color])),
    );
    // line colors by collection id, kept once given so switching or expanding never recolors
    private readonly lineColorAssignments = new Map<string, number>();
    // the collections marked as changed: switched ones, shown or not, and shown ones affected
    readonly changedCount = computed(
        (): number =>
            new Set([
                ...this.unsavedChanges().keys(),
                ...this.rows()
                    .filter((row: InheritanceRow) => row.savedStateText)
                    .map((row: InheritanceRow) => row.node.ref.id),
            ]).size,
    );
    readonly hasUnsavedChanges = computed((): boolean => this.unsavedChanges().size > 0);
    // a parent's switch also reaches its other sub-collections, which the tree does not show
    readonly changesParents = computed((): boolean =>
        this.rows().some(
            (row: InheritanceRow) =>
                row.role === 'ancestor' && this.unsavedChanges().has(row.node.ref.id),
        ),
    );

    constructor() {
        effect(() => {
            for (const row of this.rows()) {
                if (row.passesOwnTemplates) {
                    void this.loadPassedTemplateTitles(retrievePageConfigPropagateRef(row.node));
                }
            }
        });
        afterRenderEffect(() => {
            const truncated = new Set<string>(
                this.templateHints()
                    .map((hint) => hint.nativeElement)
                    .filter((element) => element.scrollWidth > element.offsetWidth)
                    .map((element) => element.dataset['collectionId']),
            );
            const current: ReadonlySet<string> = this.truncatedTemplateHints();
            if (truncated.size !== current.size || [...truncated].some((id) => !current.has(id))) {
                this.truncatedTemplateHints.set(truncated);
            }
        });
        effect((onCleanup) => {
            const wrapper: HTMLElement | undefined = this.treeWrapper()?.nativeElement;
            if (!wrapper) {
                return;
            }
            const observer = new ResizeObserver(() => this.treeSize.update((size) => size + 1));
            observer.observe(wrapper);
            onCleanup(() => observer.disconnect());
        });
        afterRenderEffect(() => {
            this.treeSize();
            this.passedTemplateTitles();
            const drawings: LineDrawing[] = this.measureLines();
            if (JSON.stringify(drawings) !== JSON.stringify(this.lineDrawings())) {
                this.lineDrawings.set(drawings);
            }
        });
    }

    readonly rowLevel = (row: InheritanceRow): number => row.depth;
    readonly rowKey = (row: InheritanceRow): string => row.node.ref.id;

    /** The line color of a collection with templates, given on its first appearance. */
    private lineColorFor(id: string): number {
        if (!this.lineColorAssignments.has(id)) {
            this.lineColorAssignments.set(id, (this.lineColorAssignments.size % LINE_COLORS) + 1);
        }
        return this.lineColorAssignments.get(id);
    }

    /** The line color of the collection passing on the given one's templates, if drawn. */
    lineColorOf(source: Node): number | undefined {
        return this.lineColors().get(source.ref.id);
    }

    /** What changes for a row when applying: its saved state and, if switched, its pass on. */
    savedDescription(row: InheritanceRow): string {
        const before: string[] = [];
        if (row.savedStateText) {
            before.push(
                this.translate.instant(
                    this.i18nPrefix + row.savedStateText.key,
                    row.savedStateText.params,
                ),
            );
        }
        if (row.toggled) {
            before.push(
                this.translate.instant(
                    this.i18nPrefix + (row.passesOn ? 'NOT_PASSED_ON' : 'PASSED_ON'),
                ),
            );
        }
        return `${this.translate.instant(this.i18nPrefix + 'SAVED_STATE')} ${before.join(', ')}`;
    }

    /** The titles of the templates a collection passes on, undefined until they are loaded. */
    passedTemplates(row: InheritanceRow): string[] | undefined {
        return this.passedTemplateTitles().get(retrievePageConfigPropagateRef(row.node));
    }

    /** The link opening a collection's templates, or null where no topic page URL is known. */
    templatesUrl(node: Node): string | null {
        const url: string | null =
            this.topicPageGlobalService.getCustomUrlFunction()?.(node) || null;
        if (!url) {
            return null;
        }
        const target: URL = new URL(url, window.location.href);
        target.searchParams.set(OPEN_PAGE_TEMPLATE_QUERY_PARAM, 'true');
        return target.toString();
    }

    /** Lets the page open the templates on a plain click; other clicks follow the link. */
    openTemplates(event: MouseEvent, node: Node): void {
        if (
            event.button !== 0 ||
            event.ctrlKey ||
            event.metaKey ||
            event.shiftKey ||
            event.altKey
        ) {
            return;
        }
        event.preventDefault();
        void this.topicPageSidebarService.host?.editInheritedTemplates(node);
    }

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
        indent: number,
        incoming: ResolvedInheritance,
        savedIncoming: ResolvedInheritance,
        subcollections: Subcollections,
    ): void {
        const role: RowRole = indent === 0 ? 'current' : 'subcollection';
        rows.push(this.buildRow(node, role, depth, incoming, savedIncoming, subcollections));
        if (!subcollections.expanded) {
            return;
        }
        for (const child of subcollections.nodes) {
            this.addRows(
                rows,
                child,
                depth + 1,
                indent + 1,
                this.outgoing(node, incoming, this.stopOf(node)),
                this.outgoing(node, savedIncoming, stopsPropagation(node)),
                this.subcollectionsOf(child),
            );
        }
    }

    private buildRow(
        node: Node,
        role: RowRole,
        depth: number,
        incoming: ResolvedInheritance,
        savedIncoming: ResolvedInheritance,
        subcollections: Subcollections,
    ): InheritanceRow {
        const stop: boolean = this.stopOf(node);
        const state: InheritanceState = this.stateOf(node, incoming);
        const savedState: InheritanceState = this.stateOf(node, savedIncoming);
        const passesOwnTemplates: boolean = !!retrievePageConfigPropagateRef(node);
        return {
            node,
            role,
            depth,
            expandable: subcollections.expandable,
            expanded: subcollections.expanded,
            isLoading: subcollections.isLoading,
            state,
            stateText: this.stateText(state),
            savedStateText: this.sameState(state, savedState) ? null : this.stateText(savedState),
            hasOwnPage: !!retrievePageConfigRef(node),
            passesOwnTemplates,
            passesOn: !stop,
            toggled: this.unsavedChanges().has(node.ref.id),
            canToggle:
                subcollections.exist &&
                (passesOwnTemplates || !!incoming.source || stop) &&
                checkUserAccess(node),
        };
    }

    /**
     * Draws each template line from below the chevron of its source down the tree, bending into
     * every inheriting row; rows passing nothing on to their shown children get a stop mark.
     */
    private measureLines(): LineDrawing[] {
        const wrapper: HTMLElement | undefined = this.treeWrapper()?.nativeElement;
        if (!wrapper) {
            return [];
        }
        const origin: DOMRect = wrapper.getBoundingClientRect();
        const rowElement = (id: string): HTMLElement | null =>
            wrapper.querySelector(`[data-row-id="${CSS.escape(id)}"]`);
        // where a line leaves a row: centered below its chevron
        const exit = (element: HTMLElement): { x: number; y: number } => {
            const slot: DOMRect = element.querySelector('.expand-slot').getBoundingClientRect();
            return {
                x: Math.round(slot.left + slot.width / 2 - origin.left),
                y: Math.round(slot.top + slot.height / 2 + 12 - origin.top),
            };
        };
        const drawings: LineDrawing[] = [];
        for (const line of this.templateLines()) {
            const source: HTMLElement | null = rowElement(line.sourceId);
            const targets: HTMLElement[] = line.targetIds
                .map(rowElement)
                .filter((element): element is HTMLElement => !!element);
            if (!source || !targets.length) {
                continue;
            }
            const start = exit(source);
            const segments: string[] = [];
            let end: number = start.y;
            for (const target of targets) {
                const slot: DOMRect = target.querySelector('.expand-slot').getBoundingClientRect();
                const y: number = Math.round(slot.top + slot.height / 2 - origin.top);
                // ends before the chevron, or before the icon of a row without one
                const tip: number = Math.round(
                    (target.querySelector('.expand-slot > *')
                        ? slot.left + 10
                        : target.querySelector('.row-icon').getBoundingClientRect().left - 4) -
                        origin.left,
                );
                const bend: number = Math.max(0, Math.min(LINE_BEND, tip - start.x - LINE_ARROW));
                segments.push(
                    `M${start.x} ${y - bend} Q${start.x} ${y} ${start.x + bend} ${y} H${tip}`,
                    `M${tip - LINE_ARROW} ${y - LINE_ARROW / 1.5} L${tip} ${y} L${
                        tip - LINE_ARROW
                    } ${y + LINE_ARROW / 1.5}`,
                );
                end = Math.max(end, y - bend);
            }
            segments.unshift(`M${start.x} ${start.y} V${end}`);
            drawings.push({ kind: 'line', color: line.color, path: segments.join(' ') });
        }
        const rows: InheritanceRow[] = this.rows();
        rows.forEach((row: InheritanceRow, index: number) => {
            const element: HTMLElement | null = rowElement(row.node.ref.id);
            if (
                row.passesOn ||
                !row.canToggle ||
                !element ||
                !(rows[index + 1]?.depth > row.depth)
            ) {
                return;
            }
            const { x, y } = exit(element);
            drawings.push({
                kind: 'stop',
                path: `M${x} ${y} V${y + 8} M${x - 4} ${y + 19} L${x + 4} ${y + 11}`,
                cx: x,
                cy: y + 15,
            });
        });
        return drawings;
    }

    /** Whether the collection stops passing on what it inherits, unsaved changes included. */
    private stopOf(node: Node): boolean {
        return this.unsavedChanges().get(node.ref.id) ?? stopsPropagation(node);
    }

    /**
     * The ancestors from the nearest one passing templates on down to the direct parent,
     * so the tree shows where inherited templates come from and where they are stopped.
     */
    private ancestorsFromSource(collection: Node, parents: Node[]): Node[] {
        const ancestors: Node[] = parents.filter((parent) => parent.ref.id !== collection.ref.id);
        const sourceIndex: number = ancestors.findIndex(
            (parent) => !!retrievePageConfigPropagateRef(parent),
        );
        return sourceIndex < 0 ? [] : ancestors.slice(0, sourceIndex + 1).reverse();
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
            return stop ? { source: null, stoppedAt: node } : { source: node, stoppedAt: null };
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

    private async loadPassedTemplateTitles(propagateConfigRef: string): Promise<void> {
        if (this.requestedPropagateConfigs.has(propagateConfigRef)) {
            return;
        }
        this.requestedPropagateConfigs.add(propagateConfigRef);
        try {
            const configNode: Node | null = await this.topicPageHelperService.getNodeIfAvailable(
                propagateConfigRef,
                true,
            );
            const templates: (Node | null)[] = await Promise.all(
                (retrievePageConfig(configNode).variants ?? []).map((ref: string) =>
                    this.topicPageHelperService.getNodeIfAvailable(ref, true),
                ),
            );
            const titles: string[] = templates
                .filter((template): template is Node => !!template)
                .map((template: Node) => template.title || template.name);
            this.passedTemplateTitles.update((all) => new Map(all).set(propagateConfigRef, titles));
        } catch {
            // the row keeps its generic hint
            this.requestedPropagateConfigs.delete(propagateConfigRef);
        }
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
