import { Component, ViewChild, inject, ChangeDetectionStrategy, signal } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import {
    ColumnType,
    CustomOptions,
    DefaultGroups,
    ElementType,
    InteractionType,
    ListItem,
    ListSortConfig,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeClickEvent,
    NodeEntriesWrapperComponent,
    NodePersonNamePipe,
    OPEN_URL_MODE,
    OptionItem,
    Scope,
    CombinedRenderData,
    RenderHelperService,
    TemporaryStorageService,
    TranslationsService,
    UIConstants,
} from 'ngx-edu-sharing-ui';
import {
    ConfigurationHelper,
    Person,
    RestConnectorService,
    RestConstants,
    RestHelper,
    RestNodeService,
    RestSharingService,
    SharingInfo,
} from '../../core-module/core.module';
import { Toast } from '../../services/toast';
import { Helper } from '../../core-module/rest/helper';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { BridgeService } from '../../services/bridge.service';
import { NodeHelperService } from '../../services/node-helper.service';
import { ConfigService, Node } from 'ngx-edu-sharing-api';
import { OptionsHelperService } from '../../services/options-helper.service';
import { firstValueFrom } from 'rxjs';

@Component({
    selector: 'es-sharing-page',
    templateUrl: 'sharing-page.component.html',
    styleUrls: ['sharing-page.component.scss'],
    providers: [OptionsHelperService],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class SharingPageComponent {
    private router = inject(Router);
    private route = inject(ActivatedRoute);
    private connector = inject(RestConnectorService);
    private nodeService = inject(RestNodeService);
    private optionsHelper = inject(OptionsHelperService);
    private sharingService = inject(RestSharingService);
    private bridge = inject(BridgeService);
    private nodeHelperService = inject(NodeHelperService);
    private storage = inject(TemporaryStorageService);
    private toast = inject(Toast);
    private config = inject(ConfigService);
    private nodePersonNamePipe = inject(NodePersonNamePipe);
    private translations = inject(TranslationsService);
    private renderHelper = inject(RenderHelperService);

    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    readonly Scope = Scope;
    @ViewChild('nodeEntries') nodeEntries: NodeEntriesWrapperComponent<Node>;
    loading = true;
    /** data of the rendered (shared or child) node, only set if the share allows viewing */
    renderData = signal<CombinedRenderData | null>(null);
    renderLoading = signal(false);
    /** the element of a shared folder that is currently displayed, null for the shared node itself */
    viewedChild = signal<Node | null>(null);
    passwordInput: string;
    private params: Params;
    sharingInfo: SharingInfo;
    nodesDataSource = new NodeDataSource<Node>();
    columns: ColumnType;
    sort: ListSortConfig = {
        allowed: true,
        columns: RestConstants.POSSIBLE_SORT_BY_FIELDS,
        active: RestConstants.CM_NAME,
        direction: 'asc',
    };
    options: CustomOptions = {
        useDefaultOptions: false,
        addOptions: [],
    };
    constructor() {
        this.columns = {
            Default: [
                new ListItem('NODE', RestConstants.CM_NAME),
                new ListItem('NODE', RestConstants.CM_MODIFIED_DATE),
                new ListItem('NODE', RestConstants.SIZE),
            ],
        };
        const download = new OptionItem('SHARING.DOWNLOAD', 'cloud_download', (node: Node) =>
            this.download(
                this.optionsHelper.getObjects(node, this.nodeEntries.optionsHelper.getData()),
            ),
        );
        download.elementType = [ElementType.Node];
        download.group = DefaultGroups.Primary;
        download.showAsAction = true;
        const open = new OptionItem('SHARING.OPEN', 'open_in_new', (node: Node) => {
            node = this.optionsHelper.getObjects(
                node,
                this.nodeEntries.optionsHelper.getData(),
            )?.[0];
            UIHelper.openUrl(
                node.properties[RestConstants.CCM_PROP_IO_WWWURL][0],
                this.bridge,
                OPEN_URL_MODE.BlankSystemBrowser,
            );
        });
        open.group = DefaultGroups.Primary;
        open.showAsAction = true;
        download.customShowCallback = async (nodes: Node[]) =>
            !!this.sharingInfo?.download && nodes?.[0]?.mediatype !== 'link';
        open.customShowCallback = async (nodes: Node[]) => nodes?.[0]?.mediatype === 'link';
        this.options.addOptions.push(download);
        this.options.addOptions.push(open);
        this.translations.waitForInit().subscribe(() => {
            this.route.queryParams.subscribe((params) => {
                this.params = params;
                this.sharingService.getInfo(params.nodeId, params.token).subscribe(
                    (result) => {
                        this.loading = false;
                        this.sharingInfo = result;
                        if (result.expired) {
                            void this.router.navigate([
                                UIConstants.ROUTER_PREFIX,
                                'messages',
                                'share_expired',
                            ]);
                            return;
                        }
                        this.loadChildren();
                        void this.loadRenderData();
                    },
                    (error) => {
                        console.warn(error);
                        void this.router.navigate([
                            UIConstants.ROUTER_PREFIX,
                            'messages',
                            'share_expired',
                        ]);
                        this.loading = false;
                    },
                );
            });
        });
    }

    validatePassword() {
        this.sharingService
            .getInfo(this.params.nodeId, this.params.token, this.passwordInput)
            .subscribe((result) => {
                if (!result.passwordMatches) {
                    this.toast.error(null, 'SHARING.ERROR_INVALID_PASSWORD');
                }
                this.sharingInfo = result;
                this.loadChildren();
                void this.loadRenderData();
            });
    }

    /**
     * Whether the content of the shared node is displayed in the browser: only if the share allows
     * viewing and the node itself has content (a folder only lists its elements, a link has none)
     */
    get showsRenderedNode(): boolean {
        return (
            !!this.sharingInfo?.view &&
            !this.sharingInfo.node.isDirectory &&
            this.sharingInfo.node.mediatype !== 'link'
        );
    }

    /**
     * Loads the data to render the shared node or - for a shared folder - the given element
     */
    private async loadRenderData(child: Node | null = null): Promise<void> {
        this.viewedChild.set(child);
        const viewsChild = !!child && !!this.sharingInfo?.view;
        if (!viewsChild && !this.showsRenderedNode) {
            this.renderData.set(null);
            return;
        }
        if (this.sharingInfo.password && !this.sharingInfo.passwordMatches) {
            return;
        }
        this.renderLoading.set(true);
        // the renderer only reads its inputs on creation, so drop the old data first
        this.renderData.set(null);
        try {
            const securedNode = await firstValueFrom(
                this.sharingService.getSecuredNode(
                    this.params.nodeId,
                    this.params.token,
                    this.passwordInput ?? '',
                    child?.ref.id ?? null,
                ),
            );
            this.renderData.set(await this.renderHelper.getRenderDataForShare(securedNode));
        } catch (error) {
            console.warn(error);
            this.toast.error(error);
        } finally {
            this.renderLoading.set(false);
        }
    }

    /**
     * Click on an element of a shared folder: display it if the share allows viewing
     */
    openChild(event: NodeClickEvent<Node>) {
        if (this.sharingInfo?.view && event.element.mediatype !== 'link') {
            void this.loadRenderData(event.element);
        }
    }

    closeChild() {
        void this.loadRenderData(null);
    }
    download(children: Node[] = null) {
        const node = this.params.nodeId;
        const token = this.params.token;
        let ids;
        let url =
            this.connector.getAbsoluteEndpointUrl() +
            '../share?mode=download&token=' +
            encodeURIComponent(token) +
            '&password=' +
            encodeURIComponent(this.passwordInput) +
            '&nodeId=' +
            encodeURIComponent(node);
        if (!children?.length && this.sharingInfo.node.isDirectory) {
            ids = RestHelper.getNodeIds(this.nodesDataSource.getData());
        } else {
            if (children != null) {
                ids = RestHelper.getNodeIds(children);
            }
        }
        if (ids?.length) {
            const form = document.createElement('form');
            form.method = 'POST';
            form.action = url;
            const input = document.createElement('input');
            input.type = 'hidden';
            input.name = 'childIds';
            input.value = ids.join(',');
            form.appendChild(input);
            document.body.appendChild(form);
            form.submit();
            document.body.removeChild(form);
        } else {
            window.open(url);
        }
    }
    changeSort(sort: ListSortConfig) {
        this.sort = sort;
        this.loadChildren();
    }
    private loadChildren() {
        if (this.sharingInfo.password && !this.sharingInfo.passwordMatches) return;
        if (!this.sharingInfo.node.isDirectory) {
            return;
        }
        this.nodesDataSource.reset();
        this.nodesDataSource.isLoading = true;
        const request = {
            count: RestConstants.COUNT_UNLIMITED,
            sortBy: [this.sort.active],
            sortAscending: [this.sort.direction === 'asc'],
            propertyFilter: [RestConstants.ALL],
        };
        this.sharingService
            .getChildren(this.params.nodeId, this.params.token, this.passwordInput, request)
            .subscribe((nodes) => {
                this.nodesDataSource.setData(nodes.nodes);
                this.nodesDataSource.isLoading = false;
                setTimeout(() => {
                    void this.nodeEntries.initOptionsGenerator({
                        customOptions: this.options,
                    });
                });
            });
    }
    inviterIsAuthor() {
        return Helper.objectEquals(this.sharingInfo.invitedBy, this.sharingInfo.node.createdBy);
    }
    getPersonName(person: Person) {
        return ConfigurationHelper.getPersonWithConfigDisplayName(person, this.nodePersonNamePipe);
    }

    childCount() {
        if (this.sharingInfo.node.type === RestConstants.CCM_TYPE_IO) {
            try {
                return (
                    parseInt(
                        this.sharingInfo.node.properties[
                            RestConstants.VIRTUAL_PROP_CHILDOBJECTCOUNT
                        ]?.[0],
                        10,
                    ) || 0
                );
            } catch (e) {}
        }
        return 0;
    }
}
