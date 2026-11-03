import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import {
    DEFAULT,
    HOME_REPOSITORY,
    MdsService,
    Node,
    RestConstants,
    ScopeDefinition,
    ScopeField,
    ScopeFieldVisibility,
    ScopesService,
    SearchService,
} from 'ngx-edu-sharing-api';
import {
    ColumnType,
    EduSharingUiCommonModule,
    InteractionType,
    ListItem,
    MdsHelperService,
    MdsWidgetType,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeEntriesModule,
    Scope,
} from 'ngx-edu-sharing-ui';
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, map, switchMap } from 'rxjs/operators';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Toast } from '../../../services/toast';
import { ScopeMdsService, ScopeWidgets } from '../../../features/scopes/scope-mds.service';
import { ScopePropertyListComponent } from './scope-property-list/scope-property-list.component';

/**
 * a row of the field list editor. publishedValues / excludedValues / uploadValues are a property of
 * the editorial property itself, not of a scope: they are not edited here, but taken from
 * `DEFAULT_FIELD_VALUES` for new fields and kept unchanged for existing ones
 */
type FieldDraft = Pick<
    ScopeField,
    'publishedValues' | 'excludedValues' | 'uploadValues' | 'replaces'
> & {
    id: string;
    visibility: ScopeFieldVisibility;
};

/** semantics of the predefined editorial properties (keys of their valuespaces in the default mds) */
const DEFAULT_FIELD_VALUES: Record<
    string,
    Pick<ScopeField, 'publishedValues' | 'excludedValues' | 'uploadValues'>
> = {
    'ccm:editorial_inventory': {
        publishedValues: ['accepted', 'recommended'],
        excludedValues: ['rejected'],
        uploadValues: ['accepted'],
    },
};

/** mandatory field of every scope: the inventory decision controls publishing */
const MANDATORY_FIELD = RestConstants.CCM_PROP_EDITORIAL_INVENTORY;

type ScopeDraft = {
    /** null while creating a new scope */
    scopeId: string | null;
    name: string;
    displayName: string;
    mds: string;
    fields: FieldDraft[];
};

/** list of the mds providing the default card columns (search page) */
const DEFAULT_CARD_LIST = 'search';
/** shown for free text values of the preview card */
const DUMMY_TEXT = 'Lorem ipsum';
/** example values of the preview card if the repository has no value (the renderer needs a valid one) */
const EXAMPLE_VALUES: Record<string, string> = {
    [RestConstants.CCM_PROP_LICENSE]: 'CC_BY',
};
/** widgets whose values are free text, others get a real value or none */
const FREE_TEXT_WIDGETS: string[] = [
    MdsWidgetType.Text,
    MdsWidgetType.Textarea,
    MdsWidgetType.TinyMCE,
    MdsWidgetType.MultiValueBadges,
    MdsWidgetType.MultiValueSuggestBadges,
    MdsWidgetType.SingleValueSuggestBadges,
];

/**
 * Admin view of all scope groups (see scopes concept §4.1, §8): create scope groups and maintain
 * their mds and field list. The list itself is projected by the page: the organization list
 * (`es-permissions-authorities`, group type SCOPE) with the regular organization options; it calls
 * `configure` to open the editor of a scope.
 */
@Component({
    selector: 'es-scope-management',
    templateUrl: './scope-management.component.html',
    styleUrls: ['./scope-management.component.scss'],
    imports: [
        EduSharingUiCommonModule,
        FormsModule,
        MatButtonModule,
        MatCheckboxModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatTooltipModule,
        // preview card
        NodeEntriesModule,
        ScopePropertyListComponent,
        TranslateModule,
    ],
})
export class ScopeManagementComponent {
    private scopesService = inject(ScopesService);
    private mdsService = inject(MdsService);
    private scopeMdsService = inject(ScopeMdsService);
    private mdsHelperService = inject(MdsHelperService);
    private searchService = inject(SearchService);
    private translate = inject(TranslateService);
    private toast = inject(Toast);

    protected readonly MANDATORY_FIELDS = [MANDATORY_FIELD];
    protected readonly Scope = Scope;
    protected readonly InteractionType = InteractionType;
    protected readonly NodeEntriesDisplayType = NodeEntriesDisplayType;

    /** properties that only exist in scope entries: they never replace an original value */
    private readonly scopeOnlyProperties = toSignal(this.scopesService.getScopeOnlyProperties(), {
        initialValue: [] as string[],
    });
    /** all scope definitions (configuration of the listed scope groups) */
    readonly scopes = signal<ScopeDefinition[] | null>(null);
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
    /** fields of the draft by id; bumped on every change of the field list */
    private readonly fieldsVersion = signal(0);
    readonly fieldIds = computed(() => {
        this.fieldsVersion();
        return this.draft()?.fields.map((f) => f.id) ?? [];
    });
    /** real (stored) properties of the mds as field candidates, editorial properties first */
    readonly fieldProperties = computed<ListItem[]>(() =>
        this.toListItems(
            Object.keys(this.widgets()).filter((id) =>
                ScopeManagementComponent.PROPERTY_PREFIXES.some((prefix) => id.startsWith(prefix)),
            ),
        ).sort(
            (a, b) =>
                Number(!a.name.startsWith('ccm:editorial_')) -
                    Number(!b.name.startsWith('ccm:editorial_')) || a.label.localeCompare(b.label),
        ),
    );

    /** full mds of the draft: default card columns (list "search") */
    private readonly mdsDefinition = toSignal(
        toObservable(this.draftMds).pipe(
            switchMap((mds) =>
                mds === null
                    ? of(null)
                    : this.mdsService.getMetadataSet({
                          repository: HOME_REPOSITORY,
                          metadataSet: mds || undefined,
                      }),
            ),
        ),
        { initialValue: null },
    );
    private readonly defaultCardColumns = computed<ListItem[]>(() =>
        this.mdsDefinition()
            ? this.mdsHelperService.getColumns(this.mdsDefinition(), DEFAULT_CARD_LIST)?.Default ??
              []
            : [],
    );
    /** card columns of the scope, null: the mds default (later mds changes apply) */
    readonly customCardColumns = signal<string[] | null>(null);
    readonly cardColumnIds = computed(
        () => this.customCardColumns() ?? this.defaultCardColumns().map((c) => c.name),
    );
    /**
     * every widget of the mds (incl. virtual properties), the scope fields and the columns of the mds
     * card list (license, source: they have no widget of their own)
     */
    readonly cardProperties = computed<ListItem[]>(() => {
        const ids = new Set([
            ...Object.keys(this.widgets()).filter((id) => id.includes(':')),
            ...this.fieldIds(),
            ...this.defaultCardColumns().map((c) => c.name),
        ]);
        return this.toListItems([...ids]).sort((a, b) => a.label.localeCompare(b.label));
    });
    /** columns of the preview card (grid), formats of the mds columns are kept */
    readonly previewColumns = computed<ColumnType>(() => ({
        Default: ScopeMdsService.toColumns(
            this.cardColumnIds(),
            this.defaultCardColumns(),
            this.mdsDefinition(),
        ),
    }));
    /**
     * most frequent real value of every stored card column (facets of the repository): renderers
     * interpret their values (license icon, repository of the source), a placeholder would break them
     */
    private readonly realValues = toSignal(
        toObservable(
            computed(() =>
                this.cardColumnIds()
                    .filter((id) => this.isFacetable(id))
                    .join(','),
            ),
        ).pipe(
            debounceTime(300),
            switchMap((ids) =>
                !ids
                    ? of({} as Record<string, string>)
                    : this.searchService
                          .requestSearch({
                              repository: HOME_REPOSITORY,
                              metadataset: this.draftMds() || DEFAULT,
                              query: RestConstants.DEFAULT_QUERY_NAME,
                              contentType: 'FILES',
                              maxItems: 1,
                              body: {
                                  criteria: [],
                                  facets: ids.split(',').map((property) => ({ property })),
                                  facetLimit: 1,
                                  facetMinCount: 1,
                              },
                          })
                          .pipe(
                              map(
                                  (result) =>
                                      Object.fromEntries(
                                          (result.facets ?? [])
                                              .filter((f) => f.values?.length)
                                              .map((f) => [f.property, f.values[0].value]),
                                      ) as Record<string, string>,
                              ),
                              catchError(() => of({} as Record<string, string>)),
                          ),
            ),
        ),
        { initialValue: {} as Record<string, string> },
    );
    /** a dummy content with a value for every card column */
    readonly previewDataSource = computed(
        () =>
            new NodeDataSource<Node>([
                this.createPreviewNode(this.cardColumnIds(), this.realValues()),
            ]),
    );

    constructor() {
        this.refresh();
        effect(() => this.draftMds.set(this.draft()?.mds ?? null));
    }

    refresh(then?: () => void) {
        this.scopesService.getAllScopes().subscribe({
            next: (scopes) => {
                this.scopes.set(scopes ?? []);
                then?.();
            },
            error: (e) => {
                this.scopes.set([]);
                this.toast.error(e);
            },
        });
    }

    /** opens the editor of a scope group of the list (authority name) */
    configure(scopeId: string) {
        // reload: the definition may have changed meanwhile (other admins, base query of the editors)
        this.refresh(() => {
            const scope = this.scopes()?.find((s) => s.scopeId === scopeId);
            if (scope) {
                this.edit(scope);
            } else {
                this.toast.error(null, 'SCOPES.ADMIN.NOT_FOUND');
            }
        });
    }

    create() {
        this.draft.set({
            scopeId: null,
            name: '',
            displayName: '',
            mds: this.metadataSets()?.[0]?.id ?? '',
            fields: [this.newField(MANDATORY_FIELD)],
        });
        this.customCardColumns.set(null);
    }

    edit(scope: ScopeDefinition) {
        const fields: FieldDraft[] = (scope.fields ?? []).map((f) => ({
            ...f,
            id: f.id ?? '',
            // the inventory decision is always internal
            visibility: f.id === MANDATORY_FIELD ? 'internal' : f.visibility ?? 'internal',
            // scopes configured before the defaults existed: complete them, saving stores them
            publishedValues: f.publishedValues ?? DEFAULT_FIELD_VALUES[f.id]?.publishedValues,
            excludedValues: f.excludedValues ?? DEFAULT_FIELD_VALUES[f.id]?.excludedValues,
            uploadValues: f.uploadValues ?? DEFAULT_FIELD_VALUES[f.id]?.uploadValues,
        }));
        if (!fields.some((f) => f.id === MANDATORY_FIELD)) {
            fields.unshift(this.newField(MANDATORY_FIELD));
        }
        this.draft.set({
            scopeId: scope.scopeId,
            name: scope.scopeId,
            displayName: scope.displayName,
            mds: scope.mds ?? '',
            fields,
        });
        const cardColumns = scope.presentation?.cardColumns;
        this.customCardColumns.set(cardColumns?.length ? cardColumns : null);
    }

    isMandatory(id: string): boolean {
        return id === MANDATORY_FIELD;
    }

    private newField(id: string): FieldDraft {
        return {
            id,
            visibility: 'internal',
            // same rule as the backend, which derives the stored value
            replaces: !this.scopeOnlyProperties().includes(id),
            ...DEFAULT_FIELD_VALUES[id],
        };
    }

    cancel() {
        this.draft.set(null);
    }

    setMds(mds: string) {
        this.draft().mds = mds;
        this.draftMds.set(mds);
    }

    /** new field list from the property list: existing drafts are kept, new ones get the defaults */
    setFields(ids: string[]) {
        const draft = this.draft();
        draft.fields = ids.map((id) => draft.fields.find((f) => f.id === id) ?? this.newField(id));
        this.fieldsVersion.update((v) => v + 1);
    }

    getField(id: string): FieldDraft | undefined {
        return this.draft()?.fields.find((f) => f.id === id);
    }

    /** caption of the mds widget, else the translation of the property (e.g. NODE.ccm:commonlicense_key) */
    getLabel(id: string): string {
        const caption = ScopeMdsService.getLabel(this.widgets(), id);
        if (caption !== id) {
            return caption;
        }
        const key = 'NODE.' + id;
        const translated = this.translate.instant(key);
        return translated && translated !== key ? translated : id;
    }

    /** stored, non date properties can be aggregated */
    /**
     * stored, non date properties with a widget in the mds can be aggregated (the search rejects facets
     * without a widget)
     */
    private isFacetable(id: string): boolean {
        const widget = this.widgets()[id];
        return (
            !!widget &&
            ScopeManagementComponent.PROPERTY_PREFIXES.some((prefix) => id.startsWith(prefix)) &&
            !widget.type?.startsWith('date')
        );
    }

    setCardColumns(ids: string[]) {
        this.customCardColumns.set(ids);
    }

    resetCardColumns() {
        this.customCardColumns.set(null);
    }

    private toListItems(ids: string[]): ListItem[] {
        return ids.map((id) =>
            Object.assign(new ListItem('NODE', id), { label: this.getLabel(id) }),
        );
    }

    /**
     * dummy content of the preview: every card column gets a value, a real value of the repository,
     * the first value of a valuespace, a date for date widgets or a placeholder text
     */
    private createPreviewNode(columns: string[], realValues: Record<string, string>): Node {
        const widgets = this.widgets();
        const properties: Record<string, string[]> = {};
        for (const id of columns) {
            const widget = widgets[id];
            const value =
                realValues[id] ??
                EXAMPLE_VALUES[id] ??
                widget?.values?.[0]?.id ??
                (widget?.type?.startsWith('date')
                    ? new Date().toISOString()
                    : FREE_TEXT_WIDGETS.includes(widget?.type)
                    ? DUMMY_TEXT
                    : null);
            // no value: the renderer shows its empty state (a placeholder would break e.g. the
            // license icon or the repository of the source)
            if (value) {
                properties[id] = [value];
            }
        }
        properties[RestConstants.CM_NAME] ??= [DUMMY_TEXT];
        properties[RestConstants.LOM_PROP_TITLE] ??= [DUMMY_TEXT];
        return {
            ref: { id: 'scope-card-preview', repo: HOME_REPOSITORY, isHomeRepo: true },
            name: DUMMY_TEXT,
            title: DUMMY_TEXT,
            type: RestConstants.CCM_TYPE_IO,
            mediatype: 'file',
            isDirectory: false,
            aspects: [],
            access: [],
            // no image: the card shows the icon, like a content without a preview
            preview: { isIcon: true, url: '', width: 0, height: 0 },
            icon: { fontGlyphId: 'description' },
            properties,
        } as Node;
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
        const custom = this.customCardColumns();
        const presentation = custom ? { cardColumns: custom } : null;
        this.saving.set(true);
        const request = draft.scopeId
            ? this.scopesService.updateScope(draft.scopeId, { mds, fields, presentation })
            : this.scopesService.createScope({
                  name: draft.name.trim(),
                  displayName: draft.displayName?.trim() || draft.name.trim(),
                  mds,
                  fields,
                  presentation,
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
