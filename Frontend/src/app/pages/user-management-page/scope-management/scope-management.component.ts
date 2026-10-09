import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import {
    MdsService,
    ScopeDefinition,
    ScopeField,
    ScopeFieldVisibility,
    ScopesService,
} from 'ngx-edu-sharing-api';
import { AsyncPipe } from '@angular/common';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { EduSharingUiCommonModule, ListItem, NodeEntriesModule } from 'ngx-edu-sharing-ui';
import { Observable, of } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Toast } from '../../../services/toast';
import { ScopeMdsService, ScopeWidgets } from '../../../features/scopes/scope-mds.service';

/**
 * a row of the field list editor. publishedValues / excludedValues / uploadValues are a property of
 * the editorial property itself, not of a scope: they are not edited here, but taken from
 * `DEFAULT_FIELD_VALUES` for new fields and kept unchanged for existing ones
 */
type FieldDraft = Pick<ScopeField, 'publishedValues' | 'excludedValues' | 'uploadValues'> & {
    id: string;
    visibility: ScopeFieldVisibility;
    replaces: boolean;
};

/** semantics of the predefined editorial properties (keys of their valuespaces in the default mds) */
const DEFAULT_FIELD_VALUES: Record<
    string,
    Pick<ScopeField, 'publishedValues' | 'excludedValues' | 'uploadValues'>
> = {
    'ccm:editorial_release': { publishedValues: ['released'] },
    'ccm:editorial_inventory': { excludedValues: ['rejected'], uploadValues: ['accepted'] },
};

type ScopeDraft = {
    /** null while creating a new scope */
    scopeId: string | null;
    name: string;
    displayName: string;
    mds: string;
    fields: FieldDraft[];
};

/**
 * Admin view of all scope groups (see scopes concept §4.1, §8): create scope groups and maintain
 * their mds and field list. Members are managed via the organization the scope group is based on.
 */
@Component({
    selector: 'es-scope-management',
    templateUrl: './scope-management.component.html',
    styleUrls: ['./scope-management.component.scss'],
    imports: [
        AsyncPipe,
        DragDropModule,
        EduSharingUiCommonModule,
        FormsModule,
        MatButtonModule,
        MatCheckboxModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatTooltipModule,
        NodeEntriesModule,
        TranslateModule,
    ],
})
export class ScopeManagementComponent {
    private scopesService = inject(ScopesService);
    private mdsService = inject(MdsService);
    private scopeMdsService = inject(ScopeMdsService);
    private toast = inject(Toast);

    /** search string of the page's search field, filters the list client side */
    readonly searchQuery = input<string>(null);
    /** emits the scope id (authority name of the organization) to manage its members */
    readonly manageMembers = output<string>();

    readonly scopes = signal<ScopeDefinition[] | null>(null);
    readonly filteredScopes = computed(() => {
        const query = this.searchQuery()?.trim().toLowerCase();
        const scopes = this.scopes() ?? [];
        if (!query) {
            return scopes;
        }
        return scopes.filter(
            (s) =>
                s.scopeId?.toLowerCase().includes(query) ||
                s.displayName?.toLowerCase().includes(query),
        );
    });
    readonly metadataSets = toSignal(
        this.mdsService.getAvailableMetadataSets() as Observable<{ id: string; name: string }[]>,
        { initialValue: [] },
    );
    readonly draft = signal<ScopeDraft | null>(null);
    readonly saving = signal(false);
    readonly visibilities: ScopeFieldVisibility[] = ['internal', 'public'];
    /** only real (stored) properties can be scope fields, no virtual or system properties */
    private static readonly PROPERTY_PREFIXES = ['cm:', 'ccm:', 'cclom:'];

    /** mds of the draft; a signal of its own since the draft object is edited in place */
    readonly draftMds = signal<string | null>(null);
    /** widgets of the draft's mds: source of the selectable properties and their captions */
    readonly widgets = toSignal(
        toObservable(this.draftMds).pipe(
            switchMap((mds) =>
                mds === null ? of({} as ScopeWidgets) : this.scopeMdsService.observeWidgets(mds),
            ),
        ),
        { initialValue: {} as ScopeWidgets },
    );
    /** filter text of the property picker */
    readonly propertyFilter = signal('');
    /** fields of the draft by id; bumped on every change of the field list */
    private readonly fieldsVersion = signal(0);
    readonly selectedIds = computed(() => {
        this.fieldsVersion();
        return new Set(this.draft()?.fields.map((f) => f.id) ?? []);
    });
    /**
     * selectable properties as list items (same picker as the export columns of the statistics),
     * editorial properties first. Fields of the draft that are not part of the mds stay listed so
     * they can be deselected
     */
    readonly availableProperties = computed<ListItem[]>(() => {
        const widgets = this.widgets();
        const ids = new Set(
            Object.keys(widgets).filter((id) =>
                ScopeManagementComponent.PROPERTY_PREFIXES.some((prefix) => id.startsWith(prefix)),
            ),
        );
        this.selectedIds().forEach((id) => ids.add(id));
        const filter = this.propertyFilter().trim().toLowerCase();
        return [...ids]
            .map((id) => {
                const item = new ListItem('NODE', id);
                item.label = ScopeMdsService.getLabel(widgets, id);
                return item;
            })
            .filter(
                (item) =>
                    !filter ||
                    item.name.toLowerCase().includes(filter) ||
                    item.label.toLowerCase().includes(filter),
            )
            .sort(
                (a, b) =>
                    Number(!a.name.startsWith('ccm:editorial_')) -
                        Number(!b.name.startsWith('ccm:editorial_')) ||
                    a.label.localeCompare(b.label),
            );
    });

    constructor() {
        this.refresh();
        effect(() => this.draftMds.set(this.draft()?.mds ?? null));
    }

    refresh() {
        this.scopesService.getAllScopes().subscribe({
            next: (scopes) => this.scopes.set(scopes ?? []),
            error: (e) => {
                this.scopes.set([]);
                this.toast.error(e);
            },
        });
    }

    create() {
        this.draft.set({
            scopeId: null,
            name: '',
            displayName: '',
            mds: this.metadataSets()?.[0]?.id ?? '',
            fields: [],
        });
    }

    edit(scope: ScopeDefinition) {
        this.draft.set({
            scopeId: scope.scopeId,
            name: scope.scopeId,
            displayName: scope.displayName,
            mds: scope.mds ?? '',
            fields: (scope.fields ?? []).map((f) => ({
                ...f,
                id: f.id ?? '',
                visibility: f.visibility ?? 'internal',
                replaces: !!f.replaces,
            })),
        });
    }

    cancel() {
        this.draft.set(null);
    }

    setMds(mds: string) {
        this.draft().mds = mds;
        this.draftMds.set(mds);
    }

    isSelected(item: ListItem): boolean {
        return this.selectedIds().has(item.name);
    }

    toggleField(item: ListItem, checked: boolean) {
        const fields = this.draft().fields;
        if (checked && !fields.some((f) => f.id === item.name)) {
            fields.push({
                id: item.name,
                visibility: 'internal',
                replaces: false,
                ...DEFAULT_FIELD_VALUES[item.name],
            });
        } else if (!checked) {
            this.draft().fields = fields.filter((f) => f.id !== item.name);
        }
        this.fieldsVersion.update((v) => v + 1);
    }

    getLabel(id: string): string {
        return ScopeMdsService.getLabel(this.widgets(), id);
    }

    removeField(index: number) {
        this.draft().fields.splice(index, 1);
        this.fieldsVersion.update((v) => v + 1);
    }

    dropField(event: CdkDragDrop<FieldDraft[]>) {
        moveItemInArray(this.draft().fields, event.previousIndex, event.currentIndex);
        this.fieldsVersion.update((v) => v + 1);
    }

    canSave(): boolean {
        const draft = this.draft();
        return (
            !!draft &&
            !this.saving() &&
            (!!draft.scopeId || !!draft.name.trim()) &&
            draft.fields.every((f) => !!f.id.trim())
        );
    }

    save() {
        const draft = this.draft();
        const fields: ScopeField[] = draft.fields.map((f) => ({
            ...f,
            id: f.id.trim(),
        }));
        const mds = draft.mds?.trim() || null;
        this.saving.set(true);
        const request = draft.scopeId
            ? this.scopesService.updateScope(draft.scopeId, { mds, fields })
            : this.scopesService.createScope({
                  name: draft.name.trim(),
                  displayName: draft.displayName?.trim() || draft.name.trim(),
                  mds,
                  fields,
              });
        request.subscribe({
            next: () => {
                this.saving.set(false);
                this.toast.toast(draft.scopeId ? 'SCOPES.ADMIN.UPDATED' : 'SCOPES.ADMIN.CREATED');
                this.draft.set(null);
                this.refresh();
            },
            error: (e) => {
                this.saving.set(false);
                this.toast.error(e);
            },
        });
    }
}
