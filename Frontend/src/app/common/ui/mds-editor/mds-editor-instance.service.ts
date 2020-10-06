import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject, combineLatest, Observable, of, ReplaySubject } from 'rxjs';
import { map } from 'rxjs/operators';
import {
    Node,
    RestConnectorService,
    RestConstants,
    RestMdsService,
    View,
} from '../../../core-module/core.module';
import { MdsEditorCommonService } from './mds-editor-common.service';
import { NativeWidget } from './mds-editor-view/mds-editor-view.component';
import {
    BulkMode,
    EditorType,
    InputStatus,
    MdsDefinition,
    MdsGroup,
    MdsWidget,
    MdsWidgetType,
    MdsWidgetValue,
    NativeWidgetType,
    RequiredMode,
    Values,
} from './types';
import { MdsEditorWidgetVersionComponent } from './widgets/mds-editor-widget-version/mds-editor-widget-version.component';

export interface CompletionStatusEntry {
    completed: number;
    total: number;
}

export type Widget = InstanceType<typeof MdsEditorInstanceService.Widget>;
type CompletionStatus = { [key in RequiredMode]: CompletionStatusEntry };

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

/**
 * Manages state for an MDS editor instance.
 *
 * Do _not_ use in legacy `<mds>` component.
 */
@Injectable()
export class MdsEditorInstanceService implements OnDestroy {
    static Widget = class {
        readonly hasUnsavedDefault: boolean;
        readonly initialValues: InitialValues;
        private value: string[];
        /**
         * Values that are shown as indeterminate to the user and will not be overwritten when
         * saving.
         */
        private indeterminateValues?: string[];
        private hasChanged = false;
        private status: InputStatus;
        private bulkMode?: BehaviorSubject<BulkMode>; // only when `isBulk`
        private showMissingRequiredFunction: (shouldScrollIntoView: boolean) => boolean;

        constructor(
            private mdsEditorInstanceService: MdsEditorInstanceService,
            public readonly definition: MdsWidget,
            public readonly viewId: string,
            nodes?: Node[],
        ) {
            if (nodes) {
                const nodeValues = nodes.map((node) => this.readNodeValue(node, definition));
                if (nodeValues.every((nodeValue) => nodeValue === undefined)) {
                    const defaultValue = definition.defaultvalue ? [definition.defaultvalue] : [];
                    this.initialValues = { jointValues: defaultValue };
                    this.hasUnsavedDefault = defaultValue.length > 0;
                } else {
                    this.initialValues = this.getInitialValues(nodeValues);
                }
                // Set initial values, so the initial completion status is calculated correctly.
                this.value = [...this.initialValues.jointValues];
                if (this.mdsEditorInstanceService.getIsBulk(nodes)) {
                    this.bulkMode = new BehaviorSubject<BulkMode>('no-change');
                }
            } else {
                this.initialValues = { jointValues: [] };
            }
        }

        getValue(): string[] {
            return this.value;
        }

        getIndeterminateValues(): string[] {
            return this.indeterminateValues;
        }

        getHasChanged(): boolean {
            return this.hasChanged;
        }

        getStatus(): InputStatus {
            return this.status;
        }

        getBulkMode(): BulkMode {
            return this.bulkMode.value;
        }

        async getSuggestedValues(searchString?: string): Promise<MdsWidgetValue[]> {
            if (this.definition.values) {
                return this.getLocalSuggestedValues(searchString);
            } else {
                return this.getRemoteSuggestedValues(searchString);
            }
        }

        setValue(value: string[]): void {
            this.value = value;
            this.updateHasChanged();
            this.mdsEditorInstanceService.updateHasChanges();
            this.mdsEditorInstanceService.updateCompletionState();
        }

        setIndeterminateValues(indeterminateValues?: string[]): void {
            this.indeterminateValues = indeterminateValues;
        }

        setStatus(value: InputStatus): void {
            this.status = value;
            this.mdsEditorInstanceService.updateIsValid();
        }

        setBulkMode(value: BulkMode): void {
            this.bulkMode.next(value);
            this.updateHasChanged();
            this.mdsEditorInstanceService.updateHasChanges();
        }

        observeBulkMode(): Observable<BulkMode> {
            return this.bulkMode.asObservable();
        }

        observeIsDisabled(): Observable<boolean> {
            if (this.bulkMode) {
                return this.bulkMode.pipe(map((bulkMode) => bulkMode === 'no-change'));
            } else {
                return of(false);
            }
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

        private getInitialValues(nodeValues: string[][]): InitialValues {
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

        private updateHasChanged() {
            switch (this.bulkMode?.value) {
                case 'no-change':
                    this.hasChanged = false;
                    break;
                case 'replace':
                case undefined:
                    this.hasChanged =
                        !!this.initialValues.individualValues ||
                        !arrayIsEqual(this.value, this.initialValues.jointValues);
            }
        }

        private getLocalSuggestedValues(searchString?: string): MdsWidgetValue[] {
            if (searchString) {
                const filterString = searchString.toLowerCase();
                return this.definition.values.filter(
                    (value) =>
                        value.caption.toLowerCase().indexOf(filterString) === 0 ||
                        value.id.toLowerCase().indexOf(filterString) === 0,
                );
            } else {
                return this.definition.values;
            }
        }

        private async getRemoteSuggestedValues(searchString?: string): Promise<MdsWidgetValue[]> {
            if (searchString?.length < 2) {
                return new Promise((resolve) => resolve([]));
            }
            return this.mdsEditorInstanceService.restMdsService
                .getValues(
                    {
                        valueParameters: {
                            query: RestConstants.DEFAULT_QUERY_NAME,
                            property: this.definition.id,
                            pattern: searchString,
                        },
                        criterias: [],
                        /*
                        criterias: RestSearchService.convertCritierias(
                            Helper.arrayJoin(this._currentValues, this.getValues()),
                            this.mds.widgets,
                        ),*/
                    },
                    this.mdsEditorInstanceService.mdsId,
                    // TODO: Real repo id for search needs to be added
                    RestConstants.HOME_REPOSITORY,
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
    /** Complete MDS definition. */
    mdsDefinition$ = new BehaviorSubject<MdsDefinition>(null);
    /** Nodes with updated and complete metadata. */
    nodes$ = new BehaviorSubject<Node[]>(null);
    /** MDS Views of the relevant group (in order). */
    views: View[];
    /** Whether the editor is in bulk mode to edit multiple nodes at once. */
    isBulk: boolean;
    /** Whether the editor runs in embedded mode as opposed to an overlay card */
    isEmbedded: boolean;

    // Mutable state
    shouldShowExtendedWidgets$ = new BehaviorSubject(false);
    /** Fires a single time when all widgets have been injected. */
    onMdsInflated = new ReplaySubject<void>(1);

    /**
     * Active widgets.
     *
     * Widgets are not added or removed after initialization, but hold mutable state.
     */
    widgets: readonly Widget[];
    /**
     * Active, "native" widgets (which are not defined via mds properties directly).
     *
     * E.g. `preview`, `version`, `author`.
     *
     * Will be appended on init depending if they exist in the currently rendered group.
     */
    private nativeWidgets: NativeWidget[] = [];

    // Mutable state
    private completionStatus$ = new ReplaySubject<CompletionStatus>(1);
    private hasChanges$ = new BehaviorSubject(false);
    private isValid$ = new BehaviorSubject(true);
    private canSave$ = new BehaviorSubject(false);
    private lastScrolledIntoViewIndex: number = null;
    private isDestroyed = false;

    constructor(
        private mdsEditorCommonService: MdsEditorCommonService,
        private restMdsService: RestMdsService,
        private restConnector: RestConnectorService,
    ) {
        combineLatest([this.hasChanges$, this.isValid$])
            .pipe(map(([hasChanged, isValid]) => hasChanged && isValid))
            .subscribe(this.canSave$);
    }

    ngOnDestroy() {
        this.isDestroyed = true;
    }

    /**
     * Initializes the service, fetching data from the backend.
     *
     * @throws UserPresentableError
     */
    async initWithNodes(nodes: Node[], refetch = true): Promise<EditorType> {
        if (refetch) {
            this.nodes$.next(await this.mdsEditorCommonService.fetchNodesMetadata(nodes));
        } else {
            this.nodes$.next(nodes);
        }
        this.isBulk = this.getIsBulk(this.nodes$.value);
        this.mdsId = this.mdsEditorCommonService.getMdsId(this.nodes$.value);
        const mdsDefinition = await this.mdsEditorCommonService.fetchMdsDefinition(this.mdsId);
        this.mdsDefinition$.next(mdsDefinition);
        const groupId = this.mdsEditorCommonService.getGroupId(this.nodes$.value);
        const group = this.getGroup(mdsDefinition, groupId);
        this.views = this.getViews(mdsDefinition, group);
        this.widgets = this.initWidgets(mdsDefinition, this.views, this.nodes$.value);
        this.updateCompletionState();
        return group.rendering;
    }

    async initAlt(
        groupId: string,
        setId: string = '-default-',
        repository: string = '-home-',
        currentValues: any[] = [],
    ): Promise<EditorType> {
        this.isBulk = false;
        this.mdsId = setId;
        const mdsDefinition = await this.mdsEditorCommonService.fetchMdsDefinition(this.mdsId);
        this.mdsDefinition$.next(mdsDefinition);
        const group = this.getGroup(mdsDefinition, groupId);
        this.views = this.getViews(mdsDefinition, group);
        this.widgets = this.initWidgets(mdsDefinition, this.views);
        this.updateCompletionState();
        return group.rendering;
    }

    getWidget(propertyName: string, viewId: string): Widget {
        return this.widgets.find(
            (widget) => widget.definition.id === propertyName && widget.viewId === viewId,
        );
    }

    getHasChanges(): boolean {
        return this.hasChanges$.value;
    }

    observeCanSave(): Observable<boolean> {
        return this.canSave$.asObservable();
    }

    getCompletionStatus(): Observable<CompletionStatus> {
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
            for (const [index, widget] of this.widgets.entries()) {
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
            for (let i = 0; i < this.widgets.length; i++) {
                const index = (i + this.lastScrolledIntoViewIndex + 1) % this.widgets.length;
                const hasJustBeenScrolledIntoView = this.widgets[index].showMissingRequired(true);
                if (hasJustBeenScrolledIntoView) {
                    this.lastScrolledIntoViewIndex = index;
                    break;
                }
            }
        }
    }

    async save(): Promise<Node[]> {
        const newValues = this.getNodeValuePairs();
        let updatedNodes: Node[];
        const versionWidget: MdsEditorWidgetVersionComponent = this.nativeWidgets.find(
            (w) => w instanceof MdsEditorWidgetVersionComponent,
        ) as MdsEditorWidgetVersionComponent;
        for (const widget of this.nativeWidgets) {
            if (widget.onSaveNode) {
                await widget.onSaveNode(this.nodes$.value);
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

    private getIsBulk(nodes: Node[]): boolean {
        return nodes?.length > 1;
    }

    private updateHasChanges(): void {
        const someWidgetsHaveChanged = this.widgets.some(
            (widget) =>
                (widget.getHasChanged() || widget.hasUnsavedDefault) &&
                widget.getStatus() !== 'DISABLED',
        );
        const someNativeWidgetsHaveChanged = this.nativeWidgets.some((w) => w.hasChanges.value);
        this.hasChanges$.next(someWidgetsHaveChanged || someNativeWidgetsHaveChanged);
    }

    private updateIsValid(): void {
        const allAreValid = this.widgets.every((state) => state.getStatus() !== 'INVALID');
        this.isValid$.next(allAreValid);
    }

    private getGroup(mdsDefinition: MdsDefinition, groupId: string): MdsGroup {
        return mdsDefinition.groups.find((g) => g.id === groupId);
    }

    private getViews(mdsDefinition: MdsDefinition, group: MdsGroup): View[] {
        return group.views.map((viewId) => mdsDefinition.views.find((v) => v.id === viewId));
    }

    private initWidgets(mdsDefinition: MdsDefinition, views: View[], nodes?: Node[]): Widget[] {
        const result: Widget[] = [];
        const availableWidgets = mdsDefinition.widgets
            .filter(
                (widget) =>
                    !Object.values(NativeWidgetType).includes(widget.id as NativeWidgetType),
            )
            .filter((widget) => views.some((view) => view.html.indexOf(widget.id) !== -1))
            .filter((widget) => this.meetsCondition(widget, nodes));
        for (const view of views) {
            for (const widget of this.getWidgetsForView(availableWidgets, view)) {
                result.push(new MdsEditorInstanceService.Widget(this, widget, view.id, nodes));
            }
        }
        return result;
    }

    private getWidgetsForView(availableWidgets: MdsWidget[], view: View): MdsWidget[] {
        return (
            availableWidgets
                .filter((widget) => view.html.includes(widget.id))
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

    private meetsCondition(widget: MdsWidget, nodes?: Node[]): boolean {
        if (!widget.condition) {
            return true;
        } else if (widget.condition.type === 'PROPERTY') {
            if (nodes) {
                return nodes.every(
                    (node) => widget.condition.negate === !node.properties[widget.condition.value],
                );
            } else {
                return true;
            }
        } else if (widget.condition.type === 'TOOLPERMISSION') {
            return (
                widget.condition.negate ===
                !this.restConnector.hasToolPermissionInstant(widget.condition.value)
            );
        }
        throw new Error(`Unsupported condition type: ${widget.condition.type}`);
    }

    private getNodeValuePairs(): Array<{ node: Node; values: Values }> {
        return this.nodes$.value.map((node) => ({
            node,
            values: this.getValuesForNode(node),
        }));
    }

    private getValuesForNode(node: Node): Values {
        let values = this.widgets.reduce((acc, widget) => {
            const property = widget.definition.id;
            const newValue = this.getNewPropertyValue(widget, node.properties[property]);
            if (newValue) {
                if (widget.definition.type === MdsWidgetType.Range) {
                    acc[`${property}_from`] = [newValue[0]];
                    acc[`${property}_to`] = [newValue[1]];
                } else {
                    if (acc[property]) {
                        throw new Error('Merging of properties is not yet implemented');
                    }
                    acc[property] = newValue;
                }
            }
            return acc;
        }, {} as { [key: string]: string[] });
        this.nativeWidgets.forEach(
            (widget) => (values = widget.getValues ? widget.getValues(node, values) : values),
        );
        return values;
    }

    private getNewPropertyValue(widget: Widget, oldPropertyValue?: string[]): string[] {
        if (!widget.getHasChanged() && !widget.hasUnsavedDefault) {
            return null;
        } else if (!this.isBulk) {
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

    private updateCompletionState(): void {
        this.completionStatus$.next(
            Object.values(RequiredMode).reduce((acc, requiredMode) => {
                acc[requiredMode] = this.getCompletionStatusEntry(requiredMode);
                return acc;
            }, {} as CompletionStatus),
        );
    }

    private getCompletionStatusEntry(requiredMode: RequiredMode): CompletionStatusEntry {
        const total = this.widgets.filter(
            (widget) => widget.definition.isRequired === requiredMode,
        );
        const completed = total.filter((widget) => widget.getValue() && widget.getValue()[0]);
        return {
            total: total.length,
            completed: completed.length,
        };
    }

    registerNativeWidget(nativeWidget: NativeWidget) {
        this.nativeWidgets.push(nativeWidget);
        nativeWidget.hasChanges.subscribe(() => {
            if (this.isDestroyed) {
                console.warn(
                    'Native widget is pushing state after having been destroyed:',
                    nativeWidget,
                );
                nativeWidget.hasChanges.complete();
                return;
            }
            this.updateHasChanges();
        });
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
