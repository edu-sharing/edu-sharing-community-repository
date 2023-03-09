import { Directive, EventEmitter, Injectable, Input, OnDestroy } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, combineLatest, Observable, of, ReplaySubject, Subject, from } from 'rxjs';
import { map, shareReplay, switchMap } from 'rxjs/operators';
import {
    ConfigurationHelper,
    ConfigurationService,
    Node,
    RestConnectorService,
    RestConstants,
    RestLocatorService,
    RestMdsService,
    RestSearchService,
} from '../../../core-module/core.module';
import { SearchService } from '../../../modules/search/search.service';
import { BulkBehavior } from '../mds/mds.component';
import { MdsEditorCommonService } from './mds-editor-common.service';
import { NativeWidgetComponent } from './mds-editor-view/mds-editor-view.component';
import {
    BulkMode,
    EditorBulkMode,
    EditorMode,
    EditorType,
    InputStatus,
    MdsDefinition,
    MdsGroup,
    MdsView,
    MdsWidget,
    MdsWidgetCondition,
    MdsWidgetType,
    MdsWidgetValue,
    NativeWidgetType,
    RequiredMode,
    Suggestions,
    Values,
    ViewRelation,
} from './types';
import { MdsEditorWidgetVersionComponent } from './widgets/mds-editor-widget-version/mds-editor-widget-version.component';
import {Helper} from "../../../core-module/rest/helper";

export interface CompletionStatusEntry {
    completed: number;
    total: number;
}

export type Widget = InstanceType<typeof MdsEditorInstanceService.Widget>;

type CompletionStatus = { [key in RequiredMode]: CompletionStatusEntry };

/**
 * NativeWidget and Widget
 */
interface GeneralWidget {
    status: Observable<InputStatus>;
    viewId: string;
}

// TODO: use this object for data properties and register it with the component.
interface NativeWidget extends GeneralWidget {
    component: NativeWidgetComponent;
}

export interface InitialValues {
    /** Values that are initially present in all nodes. */
    readonly jointValues: string[];
    /**
     * Values that are initially present in some but not all nodes.
     *
     * Can be null but will never be set to an empty array.
     */
    readonly individualValues?: string[];
}

@Directive()
export abstract class MdsEditorWidgetCore {
    @Input() widget: Widget;
    readonly editorMode: EditorMode;
    readonly editorBulkMode: EditorBulkMode;

    constructor(
        private mdsEditorInstance: MdsEditorInstanceService,
        protected translate: TranslateService,
    ) {
        this.editorMode = this.mdsEditorInstance.editorMode;
        this.editorBulkMode = this.mdsEditorInstance.editorBulkMode;
    }
}

/**
 * Manages state for an MDS editor instance.
 *
 * Do _not_ use in legacy `<mds>` component.
 */
@Injectable()
export class MdsEditorInstanceService implements OnDestroy {
    static Widget = class implements GeneralWidget {
        readonly addValue = new EventEmitter<MdsWidgetValue>();
        readonly status = new BehaviorSubject<InputStatus>(null);
        readonly meetsDynamicCondition = new BehaviorSubject<boolean>(true);
        private hasUnsavedDefault: boolean; // fixed after `ready`
        private initialValues: InitialValues;
        private readonly value$ = new BehaviorSubject<string[]>(null);
        private isDirty = false;
        /**
         * Values that are shown as indeterminate to the user and will not be overwritten when
         * saving.
         */
        private indeterminateValues?: string[];
        /**
         * Whether the widget has unsaved changes.
         *
         * Also emits every time the widget value changes.
         */
        private hasChanged = new BehaviorSubject(false);
        private readonly bulkMode = new BehaviorSubject<BulkMode>(null); // only when `isBulk`
        private showMissingRequiredFunction: (shouldScrollIntoView: boolean) => boolean;
        private readonly ready = new Subject<void>();

        /**
         * An observable of the values that are common between all nodes if the property was to be
         * saved now.
         */
        private readonly jointProperty = combineLatest([this.ready, this.hasChanged]).pipe(
            map(() => this.getJointProperty()),
            shareReplay(1),
        );

        get definition() {
            return this._definition;
        }

        constructor(
            private mdsEditorInstanceService: MdsEditorInstanceService,
            private _definition: MdsWidget,
            public readonly viewId: string,
            public readonly repositoryId: string,
            public readonly relation: ViewRelation = null,
            public readonly variables: { [key: string]: string } = null,
        ) {
            // deep copy to prevent persistence from inline overrides
            this._definition = Helper.deepCopy(this._definition)
            this.replaceVariables();
            combineLatest([this.value$, this.bulkMode, this.ready])
                .pipe(
                    map(([value, bulkMode]) => {
                        switch (bulkMode) {
                            case 'no-change':
                                return false;
                            case 'replace':
                            case null:
                                return (
                                    !!this.initialValues.individualValues ||
                                    !arrayIsEqual(value, this.initialValues.jointValues)
                                );
                        }
                    }),
                )
                .subscribe(this.hasChanged);
        }

        /**
         * Checks a dynamic property condition of another widget against the property of this
         * widget.
         *
         * @param condition must satisfy `condition.type === 'PROPERTY'` and `condition.dynamic ===
         * true`
         */
        checkPropertyCondition(condition: MdsWidgetCondition): Observable<boolean> {
            const pattern = condition.pattern ? new RegExp(`^(?:${condition.pattern})$`) : null;
            return this.jointProperty.pipe(
                map((jointProperty) => {
                    if (pattern) {
                        return (
                            jointProperty &&
                            jointProperty.some((property) => pattern.test(property))
                        );
                    } else {
                        return jointProperty?.[0]?.length >= 1;
                    }
                }),
                map((result) => result !== condition.negate),
            );
        }

        private getJointProperty(): string[] {
            const changedValues = this.mdsEditorInstanceService.getNewPropertyValue(
                this,
                this.initialValues.jointValues,
            );
            return changedValues ?? this.initialValues.jointValues;
        }

        /**
         * replace variables from client.config inside parameters of the widget
         */
        private async replaceVariables() {
            if (this.variables != null) {
                for (const field of ['caption', 'placeholder', 'icon', 'defaultvalue']) {
                    (this.definition as any)[field] = this.replaceVariableString(
                        (this.definition as any)[field],
                    );
                }
            }
        }

        private replaceVariableString(str: string, variables: { [key: string]: string } = this.variables) {
            if (!str || !str.match('\\${.+}')) {
                return str;
            }
            for (const key in variables) {
                if ('${' + key + '}' === str) {
                    return variables[key];
                }
            }
            console.warn(
                'mds declared variable ' +
                    str +
                    ', but it was not found in the config variables. List of known variables below',
            );
            console.warn(variables);
            return str;
        }

        initWithNodes(nodes: Node[]): void {
            const nodeValues = nodes.map((node) => this.readNodeValue(node, this.definition));
            if (nodeValues.every((nodeValue) => nodeValue === undefined)) {
                const defaultValue = this.definition.defaultvalue
                    ? [this.definition.defaultvalue]
                    : [];
                this.initialValues = { jointValues: defaultValue };
                this.hasUnsavedDefault = defaultValue.length > 0;
            } else {
                this.initialValues = this.calculateInitialValues(nodeValues);
            }
            // Set initial values, so the initial completion status is calculated correctly.
            this.value$.next([...this.initialValues.jointValues]);
            if (this.mdsEditorInstanceService.getIsBulk(nodes)) {
                this.bulkMode.next('no-change');
            }
            this.ready.next();
            this.ready.complete();
        }

        initWithValues(values?: Values): void {
            if (this.relation === 'suggestions') {
                this.initialValues = { jointValues: [] };
            } else {
                console.log(this.definition.id, this.definition.defaultvalue);
                this.initialValues = {
                    jointValues:
                        values?.[this.definition.id] ||
                        (this.definition.defaultvalue ? [this.definition.defaultvalue] : []),
                };
            }
            // Set initial values, so the initial completion status is calculated correctly.
            this.value$.next([...this.initialValues.jointValues]);
            this.ready.next();
            this.ready.complete();
        }

        getInitialValues(): InitialValues {
            return this.initialValues;
        }

        getHasUnsavedDefault(): boolean {
            return this.hasUnsavedDefault;
        }

        getValue(): string[] {
            return this.value$.value;
        }

        getIndeterminateValues(): string[] {
            return this.indeterminateValues;
        }

        getHasChanged(): boolean {
            return this.hasChanged.value;
        }

        getStatus(): InputStatus {
            return this.status.value;
        }

        getBulkMode(): BulkMode {
            return this.bulkMode.value;
        }

        getIsDirty(): boolean {
            return this.isDirty;
        }

        async getSuggestedValues(searchString?: string): Promise<MdsWidgetValue[]> {
            if (this.definition.values) {
                return this.getLocalSuggestedValues(searchString);
            } else {
                return this.getRemoteSuggestedValues(searchString);
            }
        }

        setValue(value: string[], dirty: boolean = true): void {
            // this.mdsEditorInstanceService.valueChanged.emit({
            //     property: this.definition.id,
            //     newValue: value,
            // });
            this.isDirty = dirty;
            this.value$.next(value);
            this.mdsEditorInstanceService.updateHasChanges();
        }

        setIndeterminateValues(indeterminateValues?: string[]): void {
            this.indeterminateValues = indeterminateValues;
        }

        setStatus(value: InputStatus): void {
            this.status.next(value);
        }

        setBulkMode(value: BulkMode): void {
            this.bulkMode.next(value);
            this.mdsEditorInstanceService.updateHasChanges();
        }

        observeValue(): Observable<string[]> {
            return this.value$.asObservable();
        }

        observeHasChanged(): Observable<boolean> {
            return this.hasChanged.asObservable();
        }

        observeBulkMode(): Observable<BulkMode> {
            return this.bulkMode.asObservable();
        }

        observeIsDisabled(): Observable<boolean> {
            return this.bulkMode.pipe(map((bulkMode) => bulkMode === 'no-change'));
        }

        /**
         * Register function to reveal a missing required field.
         *
         * @param f reveals the required hint if the field is required and has no value; returns
         * whether the field was missing and scrolled into view
         */
        onShowMissingRequired(f: (shouldScrollIntoView: boolean) => boolean) {
            if (this.showMissingRequiredFunction) {
                throw new Error('onShowMissingRequired was called more than once');
            }
            this.showMissingRequiredFunction = f;
        }

        /**
         * @returns whether the the widget was scrolled into view
         */
        showMissingRequired(shouldScrollIntoView: boolean): boolean {
            if (this.showMissingRequiredFunction) {
                return this.showMissingRequiredFunction(shouldScrollIntoView);
            } else {
                return false;
            }
        }

        private calculateInitialValues(nodeValues: string[][]): InitialValues {
            const allValues = nodeValues.reduce((acc, values = []) => {
                return [...acc, ...values.filter((value) => !acc.includes(value))];
            }, [] as string[]);
            const jointValues: string[] = allValues.filter((value) =>
                nodeValues.every((values = []) => values.includes(value)),
            );
            let individualValues: string[] = null;
            if (allValues.length !== jointValues.length) {
                individualValues = allValues.filter((value) =>
                    nodeValues.some((values = []) => !values.includes(value)),
                );
            }
            return { jointValues, individualValues };
        }

        private getLocalSuggestedValues(searchString?: string): MdsWidgetValue[] {
            if (searchString) {
                const filterString = searchString.toLowerCase();
                return this.definition.values.filter(
                    (value) =>
                        value.caption.toLowerCase().indexOf(filterString) !== -1 ||
                        value.id.toLowerCase().indexOf(filterString) !== -1,
                );
            } else {
                return this.definition.values;
            }
        }

        private async getRemoteSuggestedValues(searchString?: string): Promise<MdsWidgetValue[]> {
            if (!searchString || searchString.length < 2) {
                return [];
            }
            let criteria: any[] = [];
            if (this.mdsEditorInstanceService.editorMode === 'search') {
                const values = await this.mdsEditorInstanceService.getValues();
                delete values[this.definition.id];
                values[RestConstants.PRIMARY_SEARCH_CRITERIA] = [
                    this.mdsEditorInstanceService.searchService.searchTerm,
                ];
                criteria = RestSearchService.convertCritierias(
                    values,
                    this.mdsEditorInstanceService.widgets.value.map((w) => w.definition),
                );
            }
            return this.mdsEditorInstanceService.restMdsService
                .getValues(
                    {
                        valueParameters: {
                            query: RestConstants.DEFAULT_QUERY_NAME,
                            property: this.definition.id,
                            pattern: searchString,
                        },
                        criteria,
                        /*
                        criterias: RestSearchService.convertCritierias(
                            Helper.arrayJoin(this._currentValues, this.getValues()),
                            this.mds.widgets,
                        ),*/
                    },
                    this.mdsEditorInstanceService.mdsId,
                    this.repositoryId,
                )
                .map(({ values }) => {
                    return values.map((v) => {
                        return {
                            id: v.key,
                            caption: v.displayString ?? v.key,
                        };
                    });
                })
                .toPromise();
        }

        private readNodeValue(node: Node, definition: MdsWidget): string[] {
            if (definition.type === MdsWidgetType.Range) {
                const from: string[] = node.properties[`${definition.id}_from`];
                const to: string[] = node.properties[`${definition.id}_to`];
                if (from !== undefined && to !== undefined) {
                    return [from?.[0], to?.[0]];
                } else {
                    return undefined;
                }
            } else {
                return node.properties[definition.id];
            }
        }
    };

    // Fixed after initialization
    mdsId: string;
    repository: string;
    groupId: string;
    /** Complete MDS definition. */
    mdsDefinition$ = new BehaviorSubject<MdsDefinition>(null);
    /** Nodes with updated and complete metadata. */
    nodes$ = new BehaviorSubject<Node[]>(null);
    /** Current values (if not in node mode) */
    values$ = new BehaviorSubject<Values>(null);
    /** MDS Views of the relevant group (in order). */
    views: MdsView[];
    /** Whether the editor is in bulk mode to edit multiple nodes at once. */
    editorBulkMode: EditorBulkMode;
    editorMode: EditorMode;
    isEmbedded: boolean;
    // Not used any more?
    // valueChanged = new EventEmitter<{ property: string; newValue: string[] }>();

    // Mutable state
    shouldShowExtendedWidgets$ = new BehaviorSubject(false);
    /**
     * Fires when (a different) MDS definition was loaded and widgets and views were updated
     * accordingly.
     */
    mdsInitDone = new ReplaySubject<void>(1);
    /** Fires when all widgets have been injected. */
    mdsInflated = new ReplaySubject<void>(1);
    suggestions$ = new BehaviorSubject<Suggestions>(null);
    /** Views that have at least one widget, that is not hidden due to dynamic conditions. */
    activeViews = new ReplaySubject<MdsView[]>(1);
    /** Updated widget values, not considering nodes. */
    readonly values: Observable<{ [id: string]: string[] }>;

    /**
     * Active widgets.
     *
     * Widgets are not added or removed after initialization, but hold mutable state.
     */
    widgets: BehaviorSubject<Widget[]> = new BehaviorSubject(null);
    /**
     * Active, "native" widgets (which are not defined via mds properties directly).
     *
     * E.g. `preview`, `version`, `author`.
     *
     * Will be appended on init depending if they exist in the currently rendered group.
     */
    private nativeWidgets = new BehaviorSubject<NativeWidget[]>([]);

    // Mutable state
    private readonly completionStatus$ = new ReplaySubject<CompletionStatus>(1);
    /** Whether the value would be updated on save due to changes by the user. */
    private hasUserChanges$ = new BehaviorSubject(false);
    /**
     * Whether the value would be updated on save without the user having touched the widget due to
     * defaults.
     */
    private hasProgrammaticChanges$ = new BehaviorSubject(false);
    private isValid$ = new BehaviorSubject(true);
    private canSave$ = new BehaviorSubject(false);
    private lastScrolledIntoViewIndex: number = null;
    private isDestroyed = false;

    constructor(
        private mdsEditorCommonService: MdsEditorCommonService,
        private restLocator: RestLocatorService,
        private restMdsService: RestMdsService,
        private configService: ConfigurationService,
        private restConnector: RestConnectorService,
        private searchService: SearchService,
    ) {
        // TODO: register all dynamic properties via observable pipes as done here. This way, new
        // properties can easily be derived from existing ones without having to get all the points
        // right where we have to call the respective `updateX` methods.
        combineLatest([this.hasUserChanges$, this.hasProgrammaticChanges$, this.isValid$])
            .pipe(
                map(
                    ([hasUserChanges, hasProgrammaticChanges, isValid]) =>
                        (this.editorMode === 'nodes'
                            ? hasUserChanges || hasProgrammaticChanges
                            : true) && isValid,
                ),
            )
            .subscribe(this.canSave$);
        // Updated list of widgets that meet dynamic conditions and should be considered when saving
        // values and counting completion status.
        const activeWidgets = combineLatest([this.widgets, this.nativeWidgets]).pipe(
            switchMap(([widgets, nativeWidgets]) =>
                combineLatest([
                    ...(widgets?.map((widget) =>
                        widget.meetsDynamicCondition.pipe(
                            map((meetsCondition) => ({ widget, meetsCondition })),
                        ),
                    ) ?? []),
                    ...(nativeWidgets?.map((widget) => of({ widget, meetsCondition: true })) ?? []),
                ]).pipe(
                    map((entry) =>
                        entry
                            .filter(({ meetsCondition }) => meetsCondition)
                            .map(({ widget }) => widget),
                    ),
                ),
            ),
        );
        activeWidgets
            .pipe(
                switchMap((widgets) =>
                    combineLatest(
                        widgets.filter((widget) => widget.status).map((widget) => widget.status),
                    ),
                ),
                map((statuses) => statuses.every((status) => status !== 'INVALID')),
            )
            .subscribe(this.isValid$);
        activeWidgets
            .pipe(
                switchMap((widgets) => {
                    const filteredWidgets: Widget[] = widgets
                        // Filter out native widgets since we cannot tell whether they set a value
                        // for now.
                        .filter(
                            (widget): widget is Widget =>
                                'definition' in widget &&
                                !Object.values(NativeWidgetType).includes(
                                    widget.definition.id as NativeWidgetType,
                                ),
                        )
                        // Filter out widgets that are not shown to the user.
                        .filter((widget) => widget.definition.type !== MdsWidgetType.DefaultValue);
                    return combineLatest(
                        filteredWidgets.map((widget) =>
                            widget.observeHasChanged().pipe(map(() => widget)),
                        ),
                    ).pipe(map((ws) => this.calculateCompletionStatus(ws)));
                }),
            )
            .subscribe(this.completionStatus$);
        activeWidgets
            .pipe(
                map((widgets) =>
                    this.views.filter((view) =>
                        widgets.some((widget) => widget.viewId === view.id),
                    ),
                ),
            )
            .subscribe(this.activeViews);
        this.values = activeWidgets.pipe(
            // Don't spam the observable with changes while UI is constructing.
            //
            // TODO: Does this work when widgets are reset and constructed a second time?
            switchMap((widgets) => this.mdsInflated.pipe(map(() => widgets))),
            switchMap((widgets) =>
                // FIXME: The mappings below are a bit hacky. We take take the raw `value`
                // observable for regular widgets and the `hasChanges` observable for native widgets
                // and extract the actual values later in the pipe.
                //
                // TODO: Provide observables for the mapped values by the widgets themselves, so
                // they can be trivially combined here.
                combineLatest([
                    // regular widgets
                    combineLatest(
                        widgets
                            .filter(
                                (widget): widget is Widget =>
                                    widget instanceof MdsEditorInstanceService.Widget,
                            )
                            .filter((widget) => widget.meetsDynamicCondition.value)
                            .map((widget) => widget.observeValue().pipe(map((value) => widget))),
                    ).pipe(map((regularWidgets) => this.mapWidgetValues(regularWidgets))),
                    // native widgets
                    ...widgets
                        .filter(
                            (widget): widget is NativeWidget =>
                                'component' in widget && !!widget.component.getValues,
                        )
                        .map((nativeWidget) =>
                            nativeWidget.component.hasChanges.pipe(
                                switchMap((hasChanges) =>
                                    from(nativeWidget.component.getValues({}, null)),
                                ),
                            ),
                        ),
                ]),
            ),
            map((values) =>
                values.reduce((acc, v) => ({ ...acc, ...v }), {} as { [id: string]: string[] }),
            ),
            shareReplay(),
        );
    }

    ngOnDestroy() {
        this.isDestroyed = true;
    }

    /**
     * Initializes the service, fetching data from the backend.
     *
     * @throws UserPresentableError
     */
    async initWithNodes(
        nodes: Node[],
        {
            groupId = null,
            refetch = true,
            bulkBehavior = BulkBehavior.Default,
        }: { groupId?: string; refetch?: boolean; bulkBehavior?: BulkBehavior } = {},
    ): Promise<EditorType> {
        this.editorMode = 'nodes';
        if (refetch) {
            this.nodes$.next(await this.mdsEditorCommonService.fetchNodesMetadata(nodes));
        } else {
            this.nodes$.next(nodes);
        }
        if (this.getIsBulk(this.nodes$.value)) {
            this.editorBulkMode = { isBulk: true, bulkBehavior };
        } else {
            this.editorBulkMode = { isBulk: false };
        }
        if (!groupId) {
            groupId = this.mdsEditorCommonService.getGroupId(this.nodes$.value);
        }
        const mdsId = this.mdsEditorCommonService.getMdsId(this.nodes$.value);
        await this.initMds(groupId, mdsId, undefined, this.nodes$.value);
        for (const widget of this.widgets.value) {
            widget.initWithNodes(this.nodes$.value);
        }
        // to lower case because of remote repos wrong mapping
        return this.getGroup(
            this.mdsDefinition$.value,
            groupId,
        ).rendering?.toLowerCase() as EditorType;
    }

    async initWithoutNodes(
        groupId: string,
        mdsId: string = null,
        repository: string = '-home-',
        editorMode: EditorMode = 'search',
        initialValues?: Values,
    ): Promise<EditorType> {
        this.editorMode = editorMode;
        this.editorBulkMode = { isBulk: false };
        this.values$.next(initialValues);
        if(mdsId === null) {
            try {
                const sets = ConfigurationHelper.filterValidMds(
                    repository,
                    (await this.restMdsService.getSets().toPromise()).metadatasets,
                    this.configService);
                mdsId = sets[0]?.id;
            } catch(e) {
                console.warn('Error while resolving primary mds', e);
            }
            if(!mdsId) {
                mdsId = RestConstants.DEFAULT;
            }
        }
        await this.initMds(groupId, mdsId, repository, null, initialValues);
        if(!initialValues) {
            initialValues = {};
        }
        for (const widget of this.widgets.value) {
            widget.initWithValues(initialValues);
        }
        for (const widget of this.nativeWidgets.value) {
            if (widget instanceof MdsEditorWidgetCore) {
                (widget as MdsEditorWidgetCore).widget.initWithValues(initialValues);
            }
        }
        // to lower case because of remote repos wrong mapping
        return this.getGroup(
            this.mdsDefinition$.value,
            groupId,
        ).rendering?.toLowerCase() as EditorType;
    }

    async clearValues(): Promise<void> {
        // At the moment, widget components don't support changing or resetting the value from
        // outside the component. Therefore, we have to reload and redraw everything here.
        //
        // TODO: Handle values in a way that allows dynamic updates.
        await this.initMds(
            this.groupId,
            this.mdsId,
            this.repository,
            this.nodes$.value,
            this.values$.value,
        );
        for (const widget of this.widgets.value) {
            widget.initWithValues();
        }
    }

    getWidgetsByTagName(tagName: string, viewId: string): Widget[] {
        tagName = tagName.toLowerCase();
        return this.widgets.value.filter(
            (widget) => widget.definition.id.toLowerCase() === tagName && widget.viewId === viewId,
        );
    }

    getPrimaryWidget(propertyName: string): Widget {
        return this.widgets.value.find(
            (widget) => widget.definition.id === propertyName && widget.relation === null,
        );
    }

    getHasUserChanges(): boolean {
        return this.hasUserChanges$.value;
    }

    getCanSave(): boolean {
        return this.canSave$.value;
    }

    observeCanSave(): Observable<boolean> {
        return this.canSave$.asObservable();
    }

    observeCompletionStatus(): Observable<CompletionStatus> {
        return this.completionStatus$.asObservable();
    }

    /**
     * Shows the required hints of all missing widgets and scrolls widgets into view, rotating
     * through all widgets when called multiple times.
     */
    showMissingRequiredWidgets(shouldScrollIntoView = true): void {
        if (this.lastScrolledIntoViewIndex === null) {
            // No widget was scrolled into view yet. We need to touch all widgets so they will
            // display the required hint and tell them to scroll into view until we found a missing
            // one.
            let hasBeenScrolledIntoView = false;
            for (const [index, widget] of this.widgets.value.entries()) {
                const hasJustBeenScrolledIntoView = widget.showMissingRequired(
                    shouldScrollIntoView && !hasBeenScrolledIntoView,
                );
                if (hasJustBeenScrolledIntoView) {
                    hasBeenScrolledIntoView = true;
                    this.lastScrolledIntoViewIndex = index;
                }
            }
        } else if (shouldScrollIntoView) {
            // We already touched all widgets and scrolled one into view. Just iterate the widgets
            // starting from the one that was last scrolled into view until we find the next missing
            // one.
            for (let i = 0; i < this.widgets.value.length; i++) {
                const index = (i + this.lastScrolledIntoViewIndex + 1) % this.widgets.value.length;
                const hasJustBeenScrolledIntoView = this.widgets.value[index].showMissingRequired(
                    true,
                );
                if (hasJustBeenScrolledIntoView) {
                    this.lastScrolledIntoViewIndex = index;
                    break;
                }
            }
        }
    }

    async save(): Promise<Node[] | Values> {
        if (!this.nodes$.value) {
            return this.getValues();
        }
        const newValues = await this.getNodeValuePairs();
        let updatedNodes: Node[];
        const versionWidget: MdsEditorWidgetVersionComponent = this.nativeWidgets.value.find(
            (w) => w.component instanceof MdsEditorWidgetVersionComponent,
        )?.component as MdsEditorWidgetVersionComponent;
        for (const widget of this.nativeWidgets.value) {
            if (widget.component.onSaveNode) {
                await widget.component.onSaveNode(this.nodes$.value);
            }
        }
        if (versionWidget) {
            if (versionWidget.file) {
                updatedNodes = await this.mdsEditorCommonService.saveNodesMetadata(newValues);
                await this.mdsEditorCommonService.saveNodeContent(
                    this.nodes$.value[0],
                    versionWidget.file,
                    versionWidget.comment,
                );
                return updatedNodes;
            }
        }
        updatedNodes = await this.mdsEditorCommonService.saveNodesMetadata(
            newValues,
            versionWidget?.comment || RestConstants.COMMENT_METADATA_UPDATE,
        );
        return updatedNodes;
    }

    getIsValid() {
        return this.isValid$.value;
    }

    async getValues(node?: Node): Promise<Values> {
        // same behaviour as old mds, do not return values until it is valid
        if (!this.isValid$.value) {
            this.showMissingRequiredWidgets(true);
            return null;
        }

        let values = this.mapWidgetValues(this.widgets.value, node);
        // Native widgets don't necessarily match their ID and relevant property or even affect
        // multiple properties. Therefore, we allow them to set arbitrary properties by implementing
        // `getValues()`.
        for (const widget of this.nativeWidgets.value) {
            values = widget.component.getValues
                ? await widget.component.getValues(values, node)
                : values;
        }

        return values;
    }

    private mapWidgetValues(widgets: Widget[], node?: Node): { [id: string]: string[] } {
        return widgets
            .filter((widget) => widget.relation === null)
            .reduce((acc, widget) => {
                const property = widget.definition.id;
                const newValue = this.getNewPropertyValue(widget, node?.properties[property]);
                // filter null values in search
                if(this.editorMode === 'search' && newValue?.length === 1 && newValue[0] === null) {
                    return acc;
                } else if (newValue) {
                    if (widget.definition.type === MdsWidgetType.Range) {
                        acc[`${property}_from`] = [newValue[0]];
                        acc[`${property}_to`] = [newValue[1]];
                    } else {
                        if (acc[property]) {
                            console.error(
                                'Encountered more than one widget setting the same property',
                                property,
                            );
                        }
                        acc[property] = newValue;
                    }
                }
                return acc;
            }, {} as { [key: string]: string[] });
    }

    registerNativeWidget(component: NativeWidgetComponent, viewId: string): void {
        this.nativeWidgets.next([
            ...this.nativeWidgets.value,
            { component, viewId, status: component.status ?? of('VALID') },
        ]);
        component.hasChanges.subscribe(() => {
            if (this.isDestroyed) {
                console.warn(
                    'Native widget is pushing state after having been destroyed:',
                    component,
                );
                component.hasChanges.complete();
                return;
            }
            this.updateHasChanges();
        });
    }

    updateWidgetDefinition(): void {
        this.widgets.next(this.widgets.value);
    }
    private async initMds(
        groupId: string,
        mdsId: string,
        repository?: string,
        nodes?: Node[],
        values?: Values,
    ): Promise<void> {
        if (
            this.mdsId !== mdsId ||
            this.repository !== repository ||
            this.groupId !== groupId ||
            !this.mdsDefinition$.value
        ) {
            this.mdsId = mdsId;
            this.repository = repository;
            this.groupId = groupId;
            this.mdsDefinition$.next(
                await this.mdsEditorCommonService.fetchMdsDefinition(mdsId, repository),
            );
        }
        const mdsDefinition = this.mdsDefinition$.value;
        const group = this.getGroup(mdsDefinition, groupId);
        this.views = this.getViews(mdsDefinition, group);
        this.widgets.next(await this.generateWidgets(mdsDefinition, this.views, nodes, values));
        this.mdsInitDone.next();
    }

    private getIsBulk(nodes: Node[]): boolean {
        return nodes?.length > 1;
    }

    private updateHasChanges(): void {
        const someWidgetsHaveUserChanges = this.widgets.value.some(
            (widget) =>
                widget.getHasChanged() && widget.getIsDirty() && widget.getStatus() !== 'DISABLED',
        );
        const someWidgetsHaveProgrammaticChanges = this.widgets.value.some(
            (widget) =>
                // Explicit default values, defined in MDS.
                (widget.getHasUnsavedDefault() ||
                    // Implicit default values, that have been set by the form without user
                    // interaction based on the widget type, e.g. `false` on a checkbox.
                    (widget.getHasChanged() && !widget.getIsDirty())) &&
                widget.getStatus() !== 'DISABLED',
        );
        const someNativeWidgetsHaveChanges = this.nativeWidgets.value.some(
            (w) => w.component.hasChanges.value,
        );
        this.hasUserChanges$.next(someWidgetsHaveUserChanges || someNativeWidgetsHaveChanges);
        this.hasProgrammaticChanges$.next(someWidgetsHaveProgrammaticChanges);
    }

    private getGroup(mdsDefinition: MdsDefinition, groupId: string): MdsGroup {
        return mdsDefinition.groups.find((g) => g.id === groupId);
    }

    private getViews(mdsDefinition: MdsDefinition, group: MdsGroup): MdsView[] {
        return group.views.map((viewId) => mdsDefinition.views.find((v) => v.id === viewId));
    }

    private async generateWidgets(
        mdsDefinition: MdsDefinition,
        views: MdsView[],
        nodes?: Node[],
        values?: Values,
    ): Promise<Widget[]> {
        const result: Widget[] = [];
        const availableWidgets = mdsDefinition.widgets
            .filter((widget) => views.some((view) => view.html.indexOf(widget.id) !== -1))
            .filter((widget) => this.meetsCondition(widget, nodes, values));
        const variables = await this.restLocator.getConfigVariables().toPromise();
        for (const view of views) {
            for (const widgetDefinition of this.getWidgetsForView(availableWidgets, view)) {
                const widget = new MdsEditorInstanceService.Widget(
                    this,
                    widgetDefinition,
                    view.id,
                    this.repository,
                    view.rel,
                    // temporary fix for 6.0
                    (variables as any),
                );
                result.push(widget);
            }
        }
        this.registerPropertyConditionObervers(result);
        return result;
    }

    private registerPropertyConditionObervers(widgets: Widget[]): void {
        for (const widget of widgets) {
            const condition = widget.definition.condition;
            if (condition?.dynamic && condition.type === 'PROPERTY') {
                const dependedOnWidget = widgets.find((w) => w.definition.id === condition.value);
                if (dependedOnWidget) {
                    const isShown = dependedOnWidget.checkPropertyCondition(condition);
                    isShown.subscribe(widget.meetsDynamicCondition);
                } else {
                    console.warn(
                        `Widget has a property condition on ${condition.value} ` +
                            `but could find no matching widget.`,
                        widget,
                    );
                }
            }
        }
    }

    private getWidgetsForView(availableWidgets: MdsWidget[], view: MdsView): MdsWidget[] {
        return (
            availableWidgets
                .filter((widget) => new RegExp(`<${widget.id}[> ]`).test(view.html))
                .filter(
                    // We want either...
                    (widget) =>
                        // ...the overriding widget (naming this view's ID as `template`), or...
                        widget.template === view.id ||
                        // ...the default widget when there is no overriding widget.
                        (!widget.template &&
                            availableWidgets
                                .filter((w) => w.id === widget.id)
                                .every((w) => w.template !== view.id)),
                )
                // Sort widgets by order of appearance, so the list can be used to rotate through
                // widgets in a meaningful way.
                .sort((a, b) => view.html.indexOf(a.id) - view.html.indexOf(b.id))
        );
    }

    private meetsCondition(widget: MdsWidget, nodes?: Node[], values?: Values): boolean {
        if (!widget.condition) {
            return true;
        } else if (widget.condition.type === 'PROPERTY') {
            if (widget.condition.dynamic) {
                return true;
            }
            if (nodes) {
                return nodes.every(
                    (node) => widget.condition.negate === !node.properties[widget.condition.value],
                );
            } else if (values) {
                return widget.condition.negate === !values[widget.condition.value];
            } else {
                return true;
            }
        } else if (widget.condition.type === 'TOOLPERMISSION') {
            const result = (
                widget.condition.negate ===
                !this.restConnector.hasToolPermissionInstant(widget.condition.value)
            );
            if(!result) {
                // tslint:disable-next-line:no-console
                console.info('hide widget ' + widget.id + ' because toolpermission ' + widget.condition.value + ' condition not matched');
            }
            return result;
        }
        throw new Error(`Unsupported condition type: ${widget.condition.type}`);
    }

    private async getNodeValuePairs(): Promise<{ node: Node; values: Values }[]> {
        return Promise.all(
            this.nodes$.value.map(async (node) => ({
                node,
                values: await this.getValues(node),
            })),
        );
    }

    private getNewPropertyValue(widget: Widget, oldPropertyValue?: string[]): string[] {
        if (
            this.editorMode === 'nodes' &&
            !widget.getHasChanged() &&
            !widget.getHasUnsavedDefault()
        ) {
            return null;
        } else if (!widget.meetsDynamicCondition.value) {
            return null;
        } else if (!this.editorBulkMode.isBulk) {
            return widget.getValue();
        } else {
            switch (widget.getBulkMode()) {
                case 'no-change':
                    return null;
                case 'replace':
                    return removeDuplicates([
                        ...(oldPropertyValue ?? []).filter((value) =>
                            widget.getIndeterminateValues()?.includes(value),
                        ),
                        ...widget
                            .getValue()
                            .filter((value) => !widget.getIndeterminateValues()?.includes(value)),
                    ]);
            }
        }
    }

    private calculateCompletionStatus(widgets: Widget[]): CompletionStatus {
        return Object.values(RequiredMode)
            .filter((requiredMode) => requiredMode !== RequiredMode.Ignore)
            .reduce((acc, requiredMode) => {
                acc[requiredMode] = this.getCompletionStatusEntry(widgets, requiredMode);
                return acc;
            }, {} as CompletionStatus);
    }

    private getCompletionStatusEntry(
        widgets: Widget[],
        requiredMode: RequiredMode,
    ): CompletionStatusEntry {
        const total = widgets.filter((widget) => widget.definition.isRequired === requiredMode);
        const completed = total.filter((widget) => widget.getValue() && widget.getValue()[0]);
        return {
            total: total.length,
            completed: completed.length,
        };
    }

    /**
     * update currently used nodes with new data
     * (e.g when some metadata has changed outside of the current context)
     */
    updateNodes(nodes: Node[]) {
        this.nodes$.next(
            this.nodes$.value.map((n1) => {
                return (
                    nodes.find((n2) => n1.ref.id === n2.ref.id && n1.ref.repo === n2.ref.repo) || n1
                );
            }),
        );
    }

    resetWidgets() {
        this.nativeWidgets.next([]);
    }
}

function arraysAreEqual<T>(arrays: T[][]) {
    if (arrays.length === 0) {
        return true;
    } else {
        return arrays.slice(1).every((array) => arrayIsEqual(arrays[0], array));
    }
}

function arrayIsEqual<T>(lhs: readonly T[], rhs: readonly T[]): boolean {
    if (!lhs && !rhs) {
        return true;
    } else if (!lhs || !rhs) {
        return false;
    } else if (lhs.length !== rhs.length) {
        return false;
    } else {
        return lhs.every((value, index) => rhs[index] === value);
    }
}

function removeDuplicates<T>(array: T[]): T[] {
    return array.filter((value, index) => array.indexOf(value) === index);
}
