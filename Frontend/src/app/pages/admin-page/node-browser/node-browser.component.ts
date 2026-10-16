import { Component, DestroyRef, inject, OnInit, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import {
    AdminV1Service,
    ElasticDocInspection,
    ElasticExportRequest,
    ElasticPropertyDiff,
    Node,
    SessionStorageService,
} from 'ngx-edu-sharing-api';
import {
    ActionbarComponent,
    ColumnType,
    InteractionType,
    ListItem,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
    Scope,
} from 'ngx-edu-sharing-ui';
import {
    Authority,
    NodeListElastic,
    RestAdminService,
    RestConnectorService,
    RestConstants,
    RestNodeService,
} from '../../../core-module/core.module';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { Toast } from '../../../services/toast';
import { WorkspaceExplorerComponent } from '../../workspace-page/explorer/explorer.component';

type LuceneData = {
    mode: 'NODEREF' | 'ELASTIC' | 'INSPECT' | 'MDS';
    store: 'Workspace' | 'Archive';
    offset: number;
    count: number;
    noderef?: string;
    query?: string;
    properties?: string;
    authorities?: Authority[];
    outputMode?: 'view' | 'export';
    exportFormat?: ElasticExportRequest['format'];
    exportAsync?: boolean;
    propertySource?: ElasticExportRequest['propertySource'];
    index?: string;
    elasticRaw: boolean;
    /** display type of the result list */
    displayType?: NodeEntriesDisplayType;
};
type IndexPreview = {
    /** data url of the decoded thumbnail (preview.small), if indexed */
    src?: string;
    /** size of the decoded thumbnail in bytes */
    bytes?: number;
    /** pixel dimensions, set once the image is loaded */
    width?: number;
    height?: number;
    type?: string;
    icon?: boolean;
};
type ElasticIndexOption = {
    value: string;
    description?: string;
};

/**
 * Admin node browser: lookup by node ref, elastic search (DSL / raw), export and index inspection.
 * Supports the query param {@link AdminNodeBrowserComponent.QUERY_PARAM_INSPECT} to directly inspect a node.
 */
@Component({
    selector: 'es-admin-node-browser',
    templateUrl: 'node-browser.component.html',
    styleUrls: ['node-browser.component.scss'],
    standalone: false,
})
export class AdminNodeBrowserComponent implements OnInit {
    /** query param of the admin page: node id which should be opened in the index inspector */
    static readonly QUERY_PARAM_INSPECT = 'inspect';
    private static readonly STORAGE_KEY = 'admin_lucene';

    private admin = inject(RestAdminService);
    private adminV1 = inject(AdminV1Service);
    private connector = inject(RestConnectorService);
    private destroyRef = inject(DestroyRef);
    private dialogs = inject(DialogsService);
    private node = inject(RestNodeService);
    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private storage = inject(SessionStorageService);
    private toast = inject(Toast);
    private translate = inject(TranslateService);

    readonly SCOPES = Scope;
    readonly InteractionType = InteractionType;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    @ViewChild('searchResults') nodeEntriesSearchResult: NodeEntriesWrapperComponent<Node>;
    @ViewChild('actionbarComponent') actionbarComponent: ActionbarComponent;

    globalProgress = false;
    elasticResponse: NodeListElastic;
    /** took / total hits of the last raw elastic response */
    elasticResponseStats: { took: number; total: number | string };
    elasticIndexOptions: ElasticIndexOption[] = [];
    defaultElasticIndex = '';
    /** index names / aliases which address the workspace index (the only one supported by the non-raw search) */
    workspaceIndexNames = ['-workspace-'];
    inspection: ElasticDocInspection;
    /** indexed preview per document (key: index + id) */
    inspectionPreviews: { [key: string]: IndexPreview } = {};
    inspectOnlyDifferences = true;
    readonly dslCodeOptions = {
        minimap: { enabled: false },
        language: 'json',
        autoIndent: true,
        automaticLayout: true,
    };
    readonly elasticResponseCodeOptions = {
        minimap: { enabled: false },
        language: 'json',
        autoIndent: true,
        automaticLayout: true,
        readOnly: true,
    };
    lucene: LuceneData = {
        mode: 'NODEREF',
        store: 'Workspace',
        offset: 0,
        count: 100,
        outputMode: 'view',
        exportFormat: 'csv',
        propertySource: 'elastic',
        elasticRaw: false,
        displayType: NodeEntriesDisplayType.Table,
        // raw and non-raw mode expect the same structure
        query: JSON.stringify({ query: { match_all: {} } }, null, 2),
    };
    /** columns which can be picked in the export properties selection */
    exportColumns: ListItem[] = [];
    searchResponse = new NodeDataSource<Node>();
    searchColumns = {
        Default: [
            new ListItem('NODE', RestConstants.CM_NAME),
            new ListItem('NODE', RestConstants.NODE_ID),
            new ListItem('NODE', RestConstants.CM_MODIFIED_DATE),
        ],
    } as ColumnType;

    async ngOnInit() {
        this.searchColumns.Default = WorkspaceExplorerComponent.getColumns(this.connector);
        this.searchColumns.Default.filter((s) =>
            [RestConstants.CM_NAME, RestConstants.NODE_ID, RestConstants.CM_CREATOR].includes(
                s.name,
            ),
        ).forEach((s) => (s.visible = true));
        // virtual properties are computed at read time and are not part of the index
        this.exportColumns = WorkspaceExplorerComponent.getColumns(this.connector).filter(
            (c) => !c.name.startsWith('virtual:'),
        );
        this.lucene = await this.storage.get<LuceneData>(
            AdminNodeBrowserComponent.STORAGE_KEY,
            this.lucene,
        );
        // solr / lucene queries are no longer supported
        if ((this.lucene.mode as string) === 'SOLR') {
            this.lucene.mode = 'ELASTIC';
        }
        this.lucene.exportFormat ??= 'csv';
        // the view toggle of the actionbar only switches between table and grid
        if (this.lucene.displayType !== NodeEntriesDisplayType.Grid) {
            this.lucene.displayType = NodeEntriesDisplayType.Table;
        }
        this.lucene.propertySource ??= 'elastic';
        this.loadElasticIndices();
        this.route.queryParams
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((params) =>
                this.handleInspectParam(params[AdminNodeBrowserComponent.QUERY_PARAM_INSPECT]),
            );
    }

    private handleInspectParam(nodeId: string) {
        if (!nodeId) {
            return;
        }
        this.lucene.mode = 'INSPECT';
        this.lucene.noderef = nodeId;
        this.inspectElasticDocument();
    }

    /** switched via the view toggle of the actionbar */
    setDisplayType(displayType: NodeEntriesDisplayType) {
        this.lucene.displayType = displayType;
        this.saveSettings();
    }

    private saveSettings() {
        void this.storage.set(AdminNodeBrowserComponent.STORAGE_KEY, this.lucene);
    }

    async debugNode(node: Node) {
        await this.dialogs.openNodeInfoDialog({ nodes: [node] });
    }

    searchNoderef() {
        this.saveSettings();
        this.globalProgress = true;
        this.node.getNodeMetadata(this.lucene.noderef, [RestConstants.ALL]).subscribe({
            next: (node) => {
                this.globalProgress = false;
                this.searchResponse.setData([node.node], {
                    from: 0,
                    count: 1,
                    total: 1,
                });
            },
            error: (error) => {
                this.globalProgress = false;
                this.toast.error(error);
            },
        });
    }

    async searchNodes() {
        this.saveSettings();
        const authorities = (this.lucene.authorities ?? []).map((auth) => auth.authorityName);
        await this.nodeEntriesSearchResult.initOptionsGenerator({
            actionbar: this.actionbarComponent,
        });
        const request = {
            offset: this.lucene.offset ? this.lucene.offset : 0,
            count: this.lucene.count,
            propertyFilter: [RestConstants.ALL],
        };
        this.globalProgress = true;
        // the raw response only belongs to the last raw search
        this.elasticResponse = null;
        this.elasticResponseStats = null;
        if (this.lucene.mode === 'ELASTIC' && !this.isElasticRaw()) {
            this.admin
                .searchLucene(this.getCompactQuery(), this.lucene.store, authorities, request)
                .subscribe({
                    next: (data) => {
                        this.globalProgress = false;
                        this.searchResponse.setData(data.nodes, data.pagination);
                    },
                    error: (error: any) => {
                        this.globalProgress = false;
                        this.toast.error(error);
                    },
                });
        } else if (this.lucene.mode === 'ELASTIC' && this.isElasticRaw()) {
            this.admin.searchElastic(this.getCompactQuery(), this.lucene.index).subscribe({
                next: (data) => {
                    this.globalProgress = false;
                    this.elasticResponse = data;
                    this.elasticResponseStats = this.getElasticResponseStats(data.elasticResponse);
                    this.searchResponse.setData(data.nodes, data.pagination);
                },
                error: (error: any) => {
                    this.globalProgress = false;
                    this.toast.error(error);
                },
            });
        }
    }

    /**
     * the query is sent as url parameter: drop the formatting, otherwise large (pretty printed) queries
     * exceed the maximum request header size of tomcat. Invalid/unparsable json is sent as it is
     */
    private getCompactQuery() {
        try {
            return JSON.stringify(JSON.parse(this.lucene.query));
        } catch (e) {
            return this.lucene.query;
        }
    }

    /** change the elastic index; results of the previous index are no longer valid */
    setIndex(index: string) {
        if (index === this.lucene.index) {
            return;
        }
        this.lucene.index = index;
        this.searchResponse.reset();
        this.elasticResponse = null;
        this.elasticResponseStats = null;
        this.inspection = null;
        this.inspectionPreviews = {};
    }

    /** the non-raw search only supports the workspace index, any other index requires the raw mode */
    isElasticRawEnforced() {
        const index = this.lucene.index?.trim();
        return !!index && !this.workspaceIndexNames.includes(index);
    }

    isElasticRaw() {
        return this.lucene.elasticRaw || this.isElasticRawEnforced();
    }

    /** result of an executed mds debug query, null if the dsl was only generated */
    async onMdsDebugResult(result: NodeListElastic | null) {
        this.saveSettings();
        if (!result) {
            this.searchResponse.reset();
            this.elasticResponse = null;
            this.elasticResponseStats = null;
            return;
        }
        await this.nodeEntriesSearchResult.initOptionsGenerator({
            actionbar: this.actionbarComponent,
        });
        this.elasticResponse = result;
        this.elasticResponseStats = this.getElasticResponseStats(result.elasticResponse);
        this.searchResponse.setData(result.nodes, result.pagination);
    }

    /** switch to the raw elastic dsl editor with the dsl generated by the mds debugger */
    openMdsDebugDsl({ dsl, index }: { dsl: string; index: string }) {
        this.lucene.mode = 'ELASTIC';
        this.lucene.elasticRaw = true;
        this.lucene.outputMode = 'view';
        this.lucene.query = dsl;
        this.lucene.index = index;
        this.elasticResponse = null;
        this.elasticResponseStats = null;
        this.saveSettings();
    }

    addLuceneAuthority(authority: Authority) {
        if (!this.lucene.authorities) {
            this.lucene.authorities = [];
        }
        this.lucene.authorities.push(authority);
    }

    removeLuceneAuthority(authority: Authority) {
        this.lucene.authorities.splice(this.lucene.authorities.indexOf(authority), 1);
    }

    loadElasticIndices() {
        this.adminV1.getElasticIndices().subscribe({
            next: (indices) => {
                const workspaceIndex = indices.find((i) => i.workspace);
                this.defaultElasticIndex = workspaceIndex?.name ?? '';
                this.workspaceIndexNames = [
                    '-workspace-',
                    ...(workspaceIndex
                        ? [workspaceIndex.name, ...(workspaceIndex.aliases ?? [])]
                        : []),
                ];
                const options: ElasticIndexOption[] = [
                    {
                        value: '-workspace-',
                        description: this.translate.instant(
                            'ADMIN.BROWSER.ELASTIC_INDEX_WORKSPACE',
                        ),
                    },
                    {
                        value: '-authorities-',
                        description: this.translate.instant(
                            'ADMIN.BROWSER.ELASTIC_INDEX_AUTHORITIES',
                        ),
                    },
                ];
                for (const index of indices) {
                    options.push({
                        value: index.name,
                        description: [
                            index.docsCount != null
                                ? this.translate.instant('ADMIN.BROWSER.ELASTIC_INDEX_DOCS', {
                                      count: index.docsCount,
                                  })
                                : null,
                            index.storeSize,
                            index.health,
                        ]
                            .filter((d) => !!d)
                            .join(' · '),
                    });
                    for (const alias of index.aliases ?? []) {
                        options.push({
                            value: alias,
                            description: this.translate.instant(
                                'ADMIN.BROWSER.ELASTIC_INDEX_ALIAS',
                                {
                                    index: index.name,
                                },
                            ),
                        });
                    }
                }
                // aliases may point to multiple indices
                this.elasticIndexOptions = options.filter(
                    (option, i) => options.findIndex((o) => o.value === option.value) === i,
                );
            },
            error: (error) => {
                // elastic might not be available, the index can still be entered manually
                console.warn('could not load elastic indices', error);
            },
        });
    }

    private getElasticResponseStats(response: string) {
        try {
            const json = JSON.parse(response);
            return {
                took: json.took,
                total:
                    (json.hits?.total?.relation === 'gte' ? '≥ ' : '') +
                    (json.hits?.total?.value ?? json.hits?.total ?? 0),
            };
        } catch (e) {
            return null;
        }
    }

    inspectElasticDocument() {
        if (!this.lucene.noderef?.trim()) {
            return;
        }
        this.saveSettings();
        this.globalProgress = true;
        this.inspection = null;
        this.adminV1
            .inspectElasticDocument({
                nodeId: this.lucene.noderef.trim(),
                index: this.lucene.index || undefined,
            })
            .subscribe({
                next: (inspection) => {
                    this.globalProgress = false;
                    this.inspection = inspection;
                    this.inspectionPreviews = this.getIndexPreviews(inspection);
                },
                error: (error) => {
                    this.globalProgress = false;
                    this.toast.error(error);
                },
            });
    }

    private getIndexPreviews(inspection: ElasticDocInspection) {
        const previews: { [key: string]: IndexPreview } = {};
        for (const document of inspection.documents ?? []) {
            try {
                const preview = JSON.parse(document.source)?.preview;
                if (!preview) {
                    continue;
                }
                previews[document.index + document.id] = {
                    src: preview.small
                        ? 'data:' + (preview.mimetype ?? 'image/jpeg') + ';base64,' + preview.small
                        : null,
                    bytes: preview.small ? this.getBase64Size(preview.small) : null,
                    type: preview.type,
                    icon: preview.icon,
                };
            } catch (e) {
                console.warn('could not parse indexed document', e);
            }
        }
        return previews;
    }

    private getBase64Size(base64: string) {
        const padding = base64.endsWith('==') ? 2 : base64.endsWith('=') ? 1 : 0;
        return Math.floor((base64.length * 3) / 4) - padding;
    }

    onPreviewLoaded(preview: IndexPreview, image: HTMLImageElement) {
        preview.width = image.naturalWidth;
        preview.height = image.naturalHeight;
    }

    getInspectionProperties(): ElasticPropertyDiff[] {
        return (this.inspection?.properties ?? []).filter(
            (p) => !this.inspectOnlyDifferences || p.status !== 'equal',
        );
    }

    exportElastic() {
        const properties = (this.lucene.properties ?? '')
            .split('\n')
            .map((p) => p.trim())
            .filter((p) => !!p);
        if (this.lucene.exportFormat !== 'dump' && !properties.length) {
            this.toast.error(null, 'ADMIN.BROWSER.LUCENE_PROPERTIES_REQUIRED');
            return;
        }
        this.saveSettings();
        const body: ElasticExportRequest = {
            index: this.lucene.index || undefined,
            query: this.lucene.query,
            format: this.lucene.exportFormat ?? 'csv',
            properties,
            propertySource: this.lucene.propertySource ?? 'elastic',
            async: !!this.lucene.exportAsync,
        };
        this.globalProgress = true;
        this.adminV1.exportByElastic$Response({ body }).subscribe({
            next: (response) => {
                this.globalProgress = false;
                if (body.async) {
                    this.toast.toast('ADMIN.BROWSER.EXPORT_SCHEDULED', null, null, null, {
                        link: {
                            caption: 'ADMIN.BROWSER.EXPORT_SCHEDULED_LINK',
                            callback: () =>
                                UIHelper.goToWorkspaceFolder(
                                    this.router,
                                    null,
                                    RestConstants.INBOX,
                                ),
                        },
                    });
                    return;
                }
                const filename =
                    /filename="?([^";]+)"?/.exec(
                        response.headers.get('Content-Disposition') ?? '',
                    )?.[1] ?? 'elastic-export.' + (body.format === 'dump' ? 'zip' : body.format);
                const url = URL.createObjectURL(response.body);
                const link = document.createElement('a');
                link.href = url;
                link.download = filename;
                link.click();
                setTimeout(() => URL.revokeObjectURL(url));
            },
            error: async (error) => {
                this.globalProgress = false;
                // the error body is a blob since the endpoint returns binary data
                if (error?.error instanceof Blob) {
                    try {
                        const message = JSON.parse(await error.error.text());
                        this.toast.error(null, message?.message ?? message?.error);
                        return;
                    } catch (e) {
                        // fall through
                    }
                }
                this.toast.error(error);
            },
        });
    }
}
