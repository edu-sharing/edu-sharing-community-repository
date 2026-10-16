import { Component, computed, effect, inject, input, OnDestroy, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import {
    DEFAULT,
    Node,
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
import { Toast } from '../../../services/toast';
import { DialogsService } from '../../dialogs/dialogs.service';
import {
    DELETE_OR_CANCEL,
    OK_OR_CANCEL,
} from '../../dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { ScopeEditorialService } from '../scope-editorial.service';
import { ScopeMdsService, ScopeWidgets } from '../scope-mds.service';
import { ScopeHistoryComponent } from './scope-history/scope-history.component';

type ScopeValues = { [property: string]: string[] };

/** one property of the field list, prepared for rendering (the control itself is the mds widget) */
type FieldModel = {
    field: ScopeField;
    label: string;
};

function sameValues(a: string[] | undefined, b: string[] | undefined) {
    const x = [...(a ?? [])].sort();
    const y = [...(b ?? [])].sort();
    return x.length === y.length && x.every((v, i) => v === y[i]);
}

/**
 * Editorial changes inside the active scope (sidebar option `SCOPE_EDITORIAL`).
 *
 * Generic: the fields come from the field list of the scope group, each one is rendered by its
 * widget of the scope mds (`es-mds-editor-single-widget`). There are no enforced transitions, every value can be set at any time. A single node
 * is saved directly, several nodes or all hits of a search via a bulk job.
 */
@Component({
    selector: 'es-scope-editorial-panel',
    templateUrl: 'scope-editorial-panel.component.html',
    styleUrls: ['scope-editorial-panel.component.scss'],
    imports: [SharedModule, MdsModule, ScopeHistoryComponent],
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
    protected readonly statusFields = computed(() =>
        this.fieldModels().filter((f) => !f.field.replaces),
    );
    protected readonly replacingFields = computed(() =>
        this.fieldModels().filter((f) => f.field.replaces),
    );
    protected readonly publishFields = computed(() =>
        this.statusFields().filter((f) => f.field.publishedValues?.length),
    );
    protected readonly excludeFields = computed(() =>
        this.statusFields().filter((f) => f.field.excludedValues?.length),
    );

    /** the entry of the single node, null if it has none yet */
    protected readonly assignment = signal<ScopeAssignment | null>(null);
    protected readonly loading = signal(false);
    /** properties touched by the user (an empty list removes the property) */
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
    protected readonly hasChanges = computed(
        () => Object.keys(this.changedProperties()).length > 0,
    );
    /** the effective values publish the entry */
    protected readonly isPublished = computed(() =>
        this.publishFields().some((f) =>
            this.getValues(f.field.id).some((v) => f.field.publishedValues.includes(v)),
        ),
    );
    protected readonly isExcluded = computed(() =>
        this.excludeFields().some((f) =>
            this.getValues(f.field.id).some((v) => f.field.excludedValues.includes(v)),
        ),
    );
    /** hint: the change publishes content that is not public (visible for guests of the scope) */
    protected readonly hintNotPublic = computed(() => {
        const publishing = this.publishFields().some((f) =>
            (this.changedProperties()[f.field.id] ?? []).some((v) =>
                f.field.publishedValues.includes(v),
            ),
        );
        return (
            publishing &&
            (!!this.searchTarget() || this.targetNodes().some((n) => n.isPublic === false))
        );
    });
    protected readonly hintConflict = computed(() => this.isPublished() && this.isExcluded());

    private assignmentSubscription: Subscription;
    private bulkSubscription: Subscription;

    constructor() {
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

    /** effective values of a property: the edit, else the stored value (single node only) */
    protected getValues(property: string): string[] {
        const edits = this.edits();
        if (property in edits) {
            return edits[property];
        }
        return this.isBulk() ? [] : this.assignment()?.properties?.[property] ?? [];
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

    /** "publish" / "exclude": set the field to the first of its marking values */
    protected applyMarking(model: FieldModel, kind: 'published' | 'excluded') {
        const values =
            kind === 'published' ? model.field.publishedValues : model.field.excludedValues;
        this.setValues(model.field.id, [values[0]]);
        this.revision.update((r) => r + 1);
    }

    protected valueLabel(property: string, value: string) {
        return ScopeMdsService.getValueLabel(this.widgets(), property, value);
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

    protected resetToOriginal(property: string) {
        this.setValues(property, []);
        this.revision.update((r) => r + 1);
    }

    protected discard() {
        this.edits.set({});
        this.reason.set('');
        this.revision.update((r) => r + 1);
    }

    async save() {
        const scopeId = this.scope()?.scopeId;
        const properties = this.changedProperties();
        if (!scopeId) {
            return;
        }
        if (!Object.keys(properties).length) {
            this.toast.toast('TOAST.SCOPE.NOTHING_CHANGED');
            return;
        }
        if (this.isBulk()) {
            await this.runBulk({ properties, reason: this.reason() || undefined });
            return;
        }
        this.saving.set(true);
        this.scopesService
            .setAssignment(scopeId, this.nodeId(), properties, this.reason() || undefined)
            .subscribe({
                next: (assignment) => {
                    this.saving.set(false);
                    this.assignment.set(assignment);
                    this.discard();
                    this.historyReload.update((r) => r + 1);
                    this.scopeEditorialService.notifyChanged();
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
                    this.scopeEditorialService.notifyChanged();
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

    protected bulkProgress(status: ScopeBulkStatus) {
        return status.total ? Math.round((100 * (status.done ?? 0)) / status.total) : 0;
    }
}
