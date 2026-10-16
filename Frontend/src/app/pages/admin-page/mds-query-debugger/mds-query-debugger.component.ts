import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import {
    AdminV1Service,
    MdsQueryDebugParameters,
    MdsQueryDebugResult,
    MdsService,
    MetadataSetInfo,
    SessionStorageService,
} from 'ngx-edu-sharing-api';
import { NodeListElastic, RestConstants } from '../../../core-module/core.module';
import { Toast } from '../../../services/toast';

export interface MdsQueryDebugCriterionState {
    property: string;
    /** one value per line */
    values: string;
}

/**
 * State of the mds query debugger.
 * Stored via the session storage so that other parts of the ui (i.e. a search debug widget)
 * can prefill the debugger by writing to {@link MdsQueryDebuggerComponent.STORAGE_KEY}
 */
export interface MdsQueryDebugState {
    metadataSet: string;
    query: string;
    criteria: MdsQueryDebugCriterionState[];
    /** sort property (e.g. "score"), empty for the default sorting */
    sortProperty?: string;
    sortAscending?: boolean;
    /** facet properties */
    facets?: string[];
    includeGlobalConditions: boolean;
    /** basequery and conditions of the query itself */
    includeQueryBasequery: boolean;
    /** basequery and conditions of the <queries> element (shared by all queries) */
    includeSharedBasequery: boolean;
}

@Component({
    selector: 'es-mds-query-debugger',
    templateUrl: './mds-query-debugger.component.html',
    styleUrls: ['./mds-query-debugger.component.scss'],
    standalone: false,
})
export class MdsQueryDebuggerComponent implements OnInit {
    static readonly STORAGE_KEY = 'admin_mds_debug';
    private adminV1 = inject(AdminV1Service);
    private mdsService = inject(MdsService);
    private storage = inject(SessionStorageService);
    private toast = inject(Toast);

    @Input() offset = 0;
    @Input() count = 10;
    /** request to open the generated dsl in the elastic dsl editor */
    @Output() openDsl = new EventEmitter<{ dsl: string; index: string }>();
    /** result of an executed debug query, null if the dsl was only generated (previous results are outdated) */
    @Output() searchResult = new EventEmitter<NodeListElastic | null>();

    readonly codeOptions = {
        minimap: { enabled: false },
        language: 'json',
        autoIndent: true,
        automaticLayout: true,
        readOnly: true,
    };
    /** suggestions, any (sortable) property can be used */
    readonly sortProperties = ['score', 'cm:modified', 'cm:created', 'cclom:title', 'random'];
    metadataSets: MetadataSetInfo[] = [];
    state: MdsQueryDebugState = MdsQueryDebuggerComponent.getDefaultState();
    result: MdsQueryDebugResult;
    loading = false;

    static getDefaultState(): MdsQueryDebugState {
        return {
            metadataSet: RestConstants.DEFAULT,
            query: RestConstants.DEFAULT_QUERY_NAME,
            criteria: [{ property: RestConstants.PRIMARY_SEARCH_CRITERIA, values: '' }],
            facets: [],
            includeGlobalConditions: true,
            includeQueryBasequery: true,
            includeSharedBasequery: true,
        };
    }

    ngOnInit(): void {
        this.mdsService.getAvailableMetadataSets().subscribe((sets) => {
            this.metadataSets = sets ?? [];
        });
        void this.storage
            .get<MdsQueryDebugState>(MdsQueryDebuggerComponent.STORAGE_KEY, this.state)
            .then((state) => {
                this.state = { ...MdsQueryDebuggerComponent.getDefaultState(), ...state };
                // facets were stored as a comma separated string before
                if (!Array.isArray(this.state.facets)) {
                    this.state.facets = ((this.state.facets as unknown as string) ?? '')
                        .split(',')
                        .map((f) => f.trim())
                        .filter((f) => f);
                }
            });
    }

    addCriterion(property = '') {
        this.state.criteria.push({ property, values: '' });
    }

    removeCriterion(criterion: MdsQueryDebugCriterionState) {
        this.state.criteria.splice(this.state.criteria.indexOf(criterion), 1);
    }

    addFacet(input: HTMLInputElement) {
        const facet = input.value?.trim();
        if (facet && !this.state.facets.includes(facet)) {
            this.state.facets.push(facet);
        }
        input.value = '';
    }

    removeFacet(facet: string) {
        this.state.facets.splice(this.state.facets.indexOf(facet), 1);
    }

    /** parameters of the query which are not used as criterion yet */
    getUnusedParameters() {
        const used = this.state.criteria.map((c) => c.property);
        return (this.result?.availableParameters ?? []).filter((p) => !used.includes(p));
    }

    debug(execute = false) {
        void this.storage.set(MdsQueryDebuggerComponent.STORAGE_KEY, this.state);
        const body: MdsQueryDebugParameters = {
            criteria: this.state.criteria
                .filter((c) => c.property?.trim())
                .map((c) => ({
                    property: c.property.trim(),
                    values: (c.values ?? '')
                        .split('\n')
                        .map((v) => v.trim())
                        .filter((v) => v),
                })),
            facets: (this.state.facets ?? []).map((property) => ({ property })),
            includeGlobalConditions: this.state.includeGlobalConditions,
            includeQueryBasequery: this.state.includeQueryBasequery,
            includeSharedBasequery: this.state.includeSharedBasequery,
            execute,
        };
        this.loading = true;
        this.adminV1
            .debugMdsQuery({
                metadataset: this.state.metadataSet,
                query: this.state.query,
                // the debugger shows the mds query only, the content type filter of the search token is not applied
                contentType: 'ALL',
                ...(this.state.sortProperty?.trim()
                    ? {
                          sortProperties: [this.state.sortProperty.trim()],
                          sortAscending: [!!this.state.sortAscending],
                      }
                    : {}),
                skipCount: this.offset ?? 0,
                maxItems: this.count ?? 10,
                body,
            })
            .subscribe({
                next: (result) => {
                    this.loading = false;
                    this.result = result;
                    this.searchResult.emit(
                        result.result ? (result.result as unknown as NodeListElastic) : null,
                    );
                },
                error: (error) => {
                    this.loading = false;
                    this.toast.error(error);
                },
            });
    }

    openInDslEditor() {
        this.openDsl.emit({ dsl: this.result.dsl, index: this.result.index });
    }

    getStatements(statements: { [key: string]: string }) {
        return Object.keys(statements ?? {}).map((key) => ({
            key,
            statement: MdsQueryDebuggerComponent.formatStatement(statements[key]),
        }));
    }

    /** statements are taken from the mds xml as they are (including line breaks and indentation) */
    private static formatStatement(statement: string) {
        try {
            return JSON.stringify(JSON.parse(statement), null, 2);
        } catch (e) {
            // placeholders like ${value} outside of strings are no valid json
            return statement?.trim();
        }
    }
}
