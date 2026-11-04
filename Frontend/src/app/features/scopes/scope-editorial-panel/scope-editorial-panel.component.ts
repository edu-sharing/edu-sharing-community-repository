import {
    Component,
    computed,
    effect,
    inject,
    input,
    OnDestroy,
    signal,
    untracked,
    viewChild,
} from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import {
    DEFAULT,
    HOME_REPOSITORY,
    Node,
    RestConstants,
    ScopeAssignment,
    ScopeBulkRequest,
    ScopeBulkStatus,
    ScopeField,
    ScopesService,
} from 'ngx-edu-sharing-api';
import { NodeEntriesDataType, NodeHelperService } from 'ngx-edu-sharing-ui';
import { firstValueFrom, Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { SharedModule } from '../../../shared/shared.module';
import { MdsModule } from '../../mds/mds.module';
import { MdsEditorWrapperComponent } from '../../mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { Toast } from '../../../services/toast';
import { DialogsService } from '../../dialogs/dialogs.service';
import {
    DELETE_OR_CANCEL,
    OK_OR_CANCEL,
} from '../../dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { ScopeEditorialService } from '../scope-editorial.service';
import { ScopeMdsService, ScopeWidgets } from '../scope-mds.service';
import { ScopeHistoryComponent } from './scope-history/scope-history.component';
import { ScopeInventoryComponent } from '../scope-inventory/scope-inventory.component';

type ScopeValues = { [property: string]: string[] };

/** shared empty list: a stable reference for the widget bindings */
const EMPTY_VALUES: string[] = [];

/** one property of the field list, prepared for rendering (the control itself is the mds widget) */
type FieldModel = {
    field: ScopeField;
    label: string;
};

function clean(values: string[] | undefined): string[] {
    return (values ?? []).filter((v) => v != null && v !== '');
}

function sameValues(a: string[] | undefined, b: string[] | undefined) {
    const x = [...(a ?? [])].sort();
    const y = [...(b ?? [])].sort();
    return x.length === y.length && x.every((v, i) => v === y[i]);
}

/**
 * Editorial changes inside the active scope (sidebar option `SCOPE_EDITORIAL`).
 *
 * The inventory decision is chosen in its own block, all other fields of the scope group are edited
 * with the mds editor (widgets of the scope mds, `widgetIds`), in form mode on pseudo nodes carrying
 * the values of the scope entries: it never saves itself. A single node is saved directly, several
 * nodes or all hits of a search via a bulk job (keep / replace / append per field, like the
 * workspace editor).
 */
@Component({
    selector: 'es-scope-editorial-panel',
    templateUrl: 'scope-editorial-panel.component.html',
    styleUrls: ['scope-editorial-panel.component.scss'],
    imports: [SharedModule, MdsModule, ScopeHistoryComponent, ScopeInventoryComponent],
})
export class ScopeEditorialPanelComponent implements OnDestroy {
    private scopesService = inject(ScopesService);
    private scopeMdsService = inject(ScopeMdsService);
    private scopeEditorialService = inject(ScopeEditorialService);
    private nodeHelperService = inject(NodeHelperService);
    private dialogs = inject(DialogsService);
    private toast = inject(Toast);

    /** current selection of the sidebar */
    readonly nodes = input<NodeEntriesDataType[]>([]);

    protected readonly scope = this.scopesService.activeScope;
    protected readonly searchTarget = this.scopeEditorialService.searchTarget;
    protected readonly widgets = toSignal(
        toObservable(computed(() => this.scope()?.definition?.mds)).pipe(
            switchMap((mds) => this.scopeMdsService.observeWidgets(mds)),
        ),
        { initialValue: {} as ScopeWidgets },
    );
    protected readonly targetNodes = computed(() => (this.nodes() ?? []) as Node[]);
    protected readonly isBulk = computed(
        () => !!this.searchTarget() || this.targetNodes().length > 1,
    );
    /** number of nodes a bulk change applies to */
    protected readonly bulkCount = computed(
        () => this.searchTarget()?.count ?? this.targetNodes().length,
    );
    protected readonly node = computed(() => (this.isBulk() ? null : this.targetNodes()[0]));
    protected readonly nodeId = computed(() =>
        this.node() ? this.nodeHelperService.getOriginalId(this.node()) : null,
    );
    protected readonly mdsId = computed(() => this.scope()?.definition?.mds || DEFAULT);
    protected readonly fieldModels = computed<FieldModel[]>(() => {
        const widgets = this.widgets();
        return this.scopesService.fields().map((field) => ({
            field,
            label: ScopeMdsService.getLabel(widgets, field.id),
        }));
    });
    /** the inventory decision (mandatory field of every scope): directly selectable at the top */
    protected readonly inventoryField = computed(() =>
        this.fieldModels().find((f) => f.field.id === RestConstants.CCM_PROP_EDITORIAL_INVENTORY),
    );
    protected readonly replacingFields = computed(() =>
        this.fieldModels().filter((f) => f.field.replaces),
    );
    /** fields of the mds editor: all but the inventory decision */
    protected readonly editorFields = computed(() =>
        this.fieldModels()
            .map((f) => f.field)
            .filter((f) => f.id !== RestConstants.CCM_PROP_EDITORIAL_INVENTORY),
    );
    protected readonly editorFieldIds = computed(() => this.editorFields().map((f) => f.id));
    protected readonly editor = viewChild(MdsEditorWrapperComponent);
    /** re-creates the editor when its nodes or the stored values change */
    protected readonly editorKey = computed(
        () =>
            `${this.scope()?.scopeId}|${this.targetNodes()
                .map((n) => n.ref?.id)
                .join(',')}|${!!this.searchTarget()}|${this.revision()}`,
    );
    /** replacing fields with a value of this scope (single node) */
    protected readonly overriddenFields = computed(() =>
        this.replacingFields().filter((f) => this.isOverridden(f.field.id)),
    );
    /**
     * the editor works on pseudo nodes with the values of the scope entries (replacing fields: the
     * effective value), one per selected node, a single empty one for all hits of a search
     */
    protected readonly editorNodes = computed<Node[]>(() => {
        if (this.searchTarget()) {
            return [this.toEditorNode(null, null, 0)];
        }
        if (this.isBulk()) {
            return this.targetNodes().map((node, i) =>
                this.toEditorNode(node, this.scopesService.getNodeScope(node)?.properties, i),
            );
        }
        const node = this.node();
        return node ? [this.toEditorNode(node, this.assignment()?.properties, 0)] : [];
    });

    /**
     * bulk: the common inventory decision of the selected contents (null: none yet), undefined if they differ
     * or the target is a search (its hits are unknown here)
     */
    protected readonly bulkInventory = computed<string | null | undefined>(() => {
        if (!this.isBulk() || this.searchTarget()) {
            return undefined;
        }
        const values = new Set(
            this.targetNodes().map(
                (node) =>
                    this.scopesService.getNodeScope(node)?.properties?.[
                        RestConstants.CCM_PROP_EDITORIAL_INVENTORY
                    ]?.[0] ?? null,
            ),
        );
        return values.size === 1 ? [...values][0] : undefined;
    });

    /** the entry of the single node, null if it has none yet */
    protected readonly assignment = signal<ScopeAssignment | null>(null);
    protected readonly loading = signal(false);
    /** inventory decision chosen by the user (an empty list removes it) */
    protected readonly edits = signal<ScopeValues>({});
    protected readonly reason = signal('');
    protected readonly saving = signal(false);
    protected readonly bulkStatus = signal<ScopeBulkStatus | null>(null);
    protected readonly historyReload = signal(0);
    /** increased when the values change from outside the widgets (load, discard, marking): re-creates them */
    protected readonly revision = signal(0);

    protected readonly changedProperties = computed<ScopeValues>(() => {
        const edits = this.edits();
        if (this.isBulk()) {
            return edits;
        }
        const current = this.assignment()?.properties ?? {};
        return Object.fromEntries(
            Object.entries(edits).filter(([key, values]) => !sameValues(values, current[key])),
        );
    });
    /** hint: the inventory decision publishes content that is not public (visible for guests of the scope) */
    protected readonly hintNotPublic = computed(() => {
        const field = this.inventoryField()?.field;
        const publishing = (this.changedProperties()[field?.id] ?? []).some((v) =>
            field.publishedValues?.includes(v),
        );
        return (
            publishing &&
            (!!this.searchTarget() || this.targetNodes().some((n) => n.isPublic === false))
        );
    });

    private assignmentSubscription: Subscription;
    private bulkSubscription: Subscription;

    constructor() {
        // the entry was changed elsewhere (e.g. the inventory decision in the preview)
        effect(() => {
            const change = this.scopeEditorialService.entryChanged();
            untracked(() => {
                if (
                    change &&
                    change.scopeId === this.scope()?.scopeId &&
                    change.nodeId === this.nodeId() &&
                    change.assignment !== this.assignment()
                ) {
                    this.assignment.set(change.assignment);
                    this.revision.update((r) => r + 1);
                    this.historyReload.update((r) => r + 1);
                }
            });
        });
        // (re)load the entry whenever the single node or the scope changes
        effect(() => {
            const scopeId = this.scope()?.scopeId;
            const nodeId = this.nodeId();
            this.assignmentSubscription?.unsubscribe();
            this.edits.set({});
            this.reason.set('');
            this.assignment.set(null);
            this.revision.update((r) => r + 1);
            if (!scopeId || !nodeId) {
                return;
            }
            this.loading.set(true);
            this.assignmentSubscription = this.scopesService
                .getAssignment(scopeId, nodeId)
                .subscribe({
                    next: (assignment) => {
                        this.assignment.set(assignment);
                        this.loading.set(false);
                        this.revision.update((r) => r + 1);
                    },
                    error: () => this.loading.set(false),
                });
        });
    }

    ngOnDestroy(): void {
        this.assignmentSubscription?.unsubscribe();
        this.bulkSubscription?.unsubscribe();
    }

    /**
     * effective values of all fields: the edit, else the stored value (single node only).
     * A computed on purpose: the widgets bind it via `ngModel`, a new array on every change
     * detection would make NgModel write the value again and again (endless change detection)
     */
    protected readonly effectiveValues = computed<ScopeValues>(() => {
        const edits = this.edits();
        const stored = this.isBulk() ? {} : this.assignment()?.properties ?? {};
        return Object.fromEntries(
            this.fieldModels().map(({ field }) => [
                field.id,
                field.id in edits ? edits[field.id] : stored[field.id] ?? EMPTY_VALUES,
            ]),
        );
    });

    /** effective values of a property, see `effectiveValues` */
    protected getValues(property: string): string[] {
        return this.effectiveValues()[property] ?? EMPTY_VALUES;
    }

    protected isTouched(property: string) {
        return property in this.changedProperties();
    }

    protected setValues(property: string, values: string[]) {
        this.edits.update((edits) => ({ ...edits, [property]: values }));
    }

    /** value of a widget; ignores the initial emit and unchanged values */
    protected onWidgetChange(property: string, values: string[]) {
        const cleaned = (values ?? []).filter((v) => v != null && v !== '');
        if (!sameValues(cleaned, this.getValues(property))) {
            this.setValues(property, cleaned);
        }
    }

    // replacing fields (single node only)

    protected isOverridden(property: string) {
        return !!this.assignment()?.properties?.[property]?.length;
    }

    /** original value of a replaced field; the node dto carries the effective value otherwise */
    protected getOriginal(property: string): string[] {
        const node = this.node();
        const originals = this.scopesService.getNodeScope(node)?.originals;
        if (originals?.[property]) {
            return originals[property];
        }
        return this.isOverridden(property) ? [] : node?.properties?.[property] ?? [];
    }

    /** removes the replaced value of the scope, the original applies again */
    protected async resetToOriginal(property: string) {
        await this.saveProperties({ [property]: [] });
    }

    private toEditorNode(
        node: Node | null,
        entry: ScopeValues | null | undefined,
        index: number,
    ): Node {
        // the node's own properties stay: conditions of the mds widgets refer to them (e.g. title
        // vs. link), an empty list would count as "set"
        const properties: ScopeValues = { ...(node?.properties ?? {}) };
        for (const field of this.editorFields()) {
            // replacing fields: the node dto carries the effective value inside the scope
            const values = clean(field.replaces ? node?.properties?.[field.id] : entry?.[field.id]);
            if (values.length) {
                properties[field.id] = values;
            } else {
                delete properties[field.id];
            }
        }
        return {
            ref: { id: 'scope-editor-' + index, repo: HOME_REPOSITORY, isHomeRepo: true },
            name: node?.name ?? '',
            type: RestConstants.CCM_TYPE_IO,
            metadataset: this.mdsId(),
            isDirectory: false,
            aspects: [],
            access: [],
            properties,
        } as Node;
    }

    /** changed fields of the single node editor (null: invalid) */
    private async getEditorChanges(): Promise<ScopeValues | null> {
        const editor = this.editor();
        if (!editor) {
            return {};
        }
        const values = await editor.getValues();
        if (!values) {
            return null;
        }
        const initial = this.editorNodes()[0]?.properties ?? {};
        return Object.fromEntries(
            this.editorFieldIds()
                .map((id): [string, string[]] => [id, clean(values[id])])
                .filter(([id, v]) => !sameValues(v, initial[id])),
        );
    }

    /**
     * bulk request of the editor: replaced fields set their values; with values of only some of
     * the selected nodes (indeterminate) or "append", the values are added and the deselected ones
     * removed per node, like the workspace editor does it
     */
    private getEditorBulkRequest(): Pick<
        ScopeBulkRequest,
        'properties' | 'appendProperties' | 'removeValues'
    > {
        const properties: ScopeValues = {};
        const appendProperties: string[] = [];
        const removeValues: ScopeValues = {};
        const ids = this.editorFieldIds();
        const nodes = this.editorNodes();
        for (const widget of this.editor()?.mdsEditorInstance.widgets.value ?? []) {
            const id = widget.definition.id;
            const mode = widget.getBulkMode();
            if (!ids.includes(id) || !mode || mode === 'no-change') {
                continue;
            }
            const values = clean(widget.getValue());
            const multi = !!widget.definition.type?.startsWith('multivalue');
            const indeterminate = widget.getIndeterminateValues() ?? [];
            if (mode === 'append') {
                // only the added values: the values of a part of the nodes stay on exactly these nodes
                properties[id] = values.filter((v) => !indeterminate.includes(v));
                appendProperties.push(id);
            } else if (multi && (indeterminate.length || nodes.length > 1)) {
                const initial = [...new Set(nodes.flatMap((n) => n.properties?.[id] ?? []))];
                properties[id] = values.filter((v) => !indeterminate.includes(v));
                appendProperties.push(id);
                const removed = initial.filter(
                    (v) => !values.includes(v) && !indeterminate.includes(v),
                );
                if (removed.length) {
                    removeValues[id] = removed;
                }
            } else {
                properties[id] = values;
            }
        }
        return { properties, appendProperties, removeValues };
    }

    protected discard() {
        this.edits.set({});
        this.reason.set('');
        // re-creates the editor with the stored values
        this.revision.update((r) => r + 1);
    }

    async save() {
        if (!this.scope()?.scopeId) {
            return;
        }
        const inventory = this.changedProperties();
        if (this.isBulk()) {
            const request = this.getEditorBulkRequest();
            const properties = { ...request.properties, ...inventory };
            if (!Object.keys(properties).length && !Object.keys(request.removeValues).length) {
                this.toast.toast('TOAST.SCOPE.NOTHING_CHANGED');
                return;
            }
            await this.runBulk({ ...request, properties, reason: this.reason() || undefined });
            return;
        }
        const changes = await this.getEditorChanges();
        if (changes === null) {
            return;
        }
        const properties = { ...changes, ...inventory };
        if (!Object.keys(properties).length) {
            this.toast.toast('TOAST.SCOPE.NOTHING_CHANGED');
            return;
        }
        await this.saveProperties(properties);
    }

    private async saveProperties(properties: ScopeValues) {
        const scopeId = this.scope()?.scopeId;
        this.saving.set(true);
        this.scopesService
            .setAssignment(scopeId, this.nodeId(), properties, this.reason() || undefined)
            .subscribe({
                next: (assignment) => {
                    this.saving.set(false);
                    this.assignment.set(assignment);
                    this.discard();
                    this.historyReload.update((r) => r + 1);
                    this.scopeEditorialService.notifyEntryChanged({
                        scopeId,
                        nodeId: this.nodeId(),
                        assignment,
                    });
                    this.toast.toast('TOAST.SCOPE.SAVED');
                },
                error: (error) => {
                    this.saving.set(false);
                    this.toast.error(error);
                },
            });
    }

    async deleteEntries() {
        const scopeId = this.scope()?.scopeId;
        if (!scopeId) {
            return;
        }
        if (this.isBulk()) {
            await this.runBulk({ delete: true, reason: this.reason() || undefined });
            return;
        }
        const dialog = await this.dialogs.openGenericDialog({
            title: 'SCOPES.EDITORIAL.DELETE.TITLE',
            message: 'SCOPES.EDITORIAL.DELETE.MESSAGE',
            buttons: DELETE_OR_CANCEL,
        });
        if ((await firstValueFrom(dialog.afterClosed())) !== 'YES_DELETE') {
            return;
        }
        this.saving.set(true);
        this.scopesService
            .deleteAssignment(scopeId, this.nodeId(), this.reason() || undefined)
            .subscribe({
                next: () => {
                    this.saving.set(false);
                    this.assignment.set(null);
                    this.discard();
                    this.historyReload.update((r) => r + 1);
                    this.scopeEditorialService.notifyEntryChanged({
                        scopeId,
                        nodeId: this.nodeId(),
                        assignment: null,
                    });
                    this.toast.toast('TOAST.SCOPE.DELETED');
                },
                error: (error) => {
                    this.saving.set(false);
                    this.toast.error(error);
                },
            });
    }

    private async runBulk(request: Omit<ScopeBulkRequest, 'nodeIds' | 'search'>) {
        const scopeId = this.scope().scopeId;
        const dialog = await this.dialogs.openGenericDialog({
            title: request.delete
                ? 'SCOPES.EDITORIAL.BULK.CONFIRM_DELETE_TITLE'
                : 'SCOPES.EDITORIAL.BULK.CONFIRM_TITLE',
            message: request.delete
                ? 'SCOPES.EDITORIAL.BULK.CONFIRM_DELETE_MESSAGE'
                : 'SCOPES.EDITORIAL.BULK.CONFIRM_MESSAGE',
            messageParameters: { count: String(this.bulkCount()) },
            buttons: OK_OR_CANCEL,
        });
        if ((await firstValueFrom(dialog.afterClosed())) !== 'OK') {
            return;
        }
        const target = this.searchTarget();
        const body: ScopeBulkRequest = {
            ...request,
            ...(target
                ? { search: target.search }
                : {
                      nodeIds: this.targetNodes().map((n) =>
                          this.nodeHelperService.getOriginalId(n),
                      ),
                  }),
        };
        this.saving.set(true);
        this.bulkSubscription?.unsubscribe();
        this.bulkSubscription = this.scopesService
            .startBulk(scopeId, body)
            .pipe(switchMap((jobId) => this.scopesService.observeBulkJob(scopeId, jobId)))
            .subscribe({
                next: (status) => {
                    this.bulkStatus.set(status);
                    if (status.state === 'FINISHED' || status.state === 'FAILED') {
                        this.saving.set(false);
                        this.discard();
                        this.scopeEditorialService.notifyChanged();
                        this.toast.toast(
                            status.state === 'FINISHED' && !status.failed?.length
                                ? 'TOAST.SCOPE.BULK_FINISHED'
                                : 'TOAST.SCOPE.BULK_FINISHED_WITH_ERRORS',
                            { done: status.done, total: status.total },
                        );
                    }
                },
                error: (error) => {
                    this.saving.set(false);
                    this.toast.error(error);
                },
            });
    }

    /** waiting: progress of the jobs ahead in the queue */
    protected queueProgress(status: ScopeBulkStatus) {
        const queue = status.queue;
        return queue?.total ? Math.round((100 * (queue.done ?? 0)) / queue.total) : 0;
    }

    protected bulkProgress(status: ScopeBulkStatus) {
        return status.total ? Math.round((100 * (status.done ?? 0)) / status.total) : 0;
    }
}
