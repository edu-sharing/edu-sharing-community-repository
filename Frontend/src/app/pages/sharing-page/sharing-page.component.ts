import {
    Component,
    ViewChild,
    inject,
    ChangeDetectionStrategy,
    HostBinding,
    OnDestroy,
} from '@angular/core';
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
import { ConfigService, MdsService, Node } from 'ngx-edu-sharing-api';
import { SelectionChange } from '@angular/cdk/collections';
import { PreviewConfig } from '../../features/editorial-sidebar/editorial-sidebar.component';
import { EditorialSidebarService } from '../../features/editorial-sidebar/editorial-sidebar.service';
import { RENDER_DATA_LOADER } from '../render2-page/render-wrapper-component/render-data-loader';
import { ShareAccessService, ShareMdsService } from './share-mds.service';
import { ShareRenderService } from './share-render.service';
import { OptionsHelperService } from '../../services/options-helper.service';
import { firstValueFrom } from 'rxjs';

@Component({
    selector: 'es-sharing-page',
    templateUrl: 'sharing-page.component.html',
    styleUrls: ['sharing-page.component.scss'],
    providers: [
        OptionsHelperService,
        ShareAccessService,
        { provide: MdsService, useClass: ShareMdsService },
        ShareRenderService,
        { provide: RENDER_DATA_LOADER, useFactory: () => inject(ShareRenderService).load },
    ],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class SharingPageComponent implements OnDestroy {
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
    private access = inject(ShareAccessService);
    private shareRender = inject(ShareRenderService);
    readonly editorialSidebar = inject(EditorialSidebarService);

    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    readonly Scope = Scope;
    @ViewChild('nodeEntries') nodeEntries: NodeEntriesWrapperComponent<Node>;
    loading = true;
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
                this.access.nodeId = params.nodeId;
                this.access.token = params.token;
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
        this.access.password = this.passwordInput ?? '';
        this.sharingService
            .getInfo(this.params.nodeId, this.params.token, this.passwordInput)
            .subscribe((result) => {
                if (!result.passwordMatches) {
                    this.toast.error(null, 'SHARING.ERROR_INVALID_PASSWORD');
                }
                this.sharingInfo = result;
                this.loadChildren();
            });
    }

    ngOnDestroy(): void {
        // the sidebar service is shared by all pages, so what was selected here must not outlive the page
        this.editorialSidebar.close();
    }

    get nodeId(): string {
        return this.params?.nodeId;
    }

    /**
     * The share allows viewing and the password (if any) was given
     */
    private get viewable(): boolean {
        const info = this.sharingInfo;
        return !!info?.view && (!info.password || info.passwordMatches);
    }

    /**
     * Whether the page shows the shared material like the regular render page: the content has to
     * be displayable (a link has none) and not be a folder
     */
    get rendersMaterial(): boolean {
        return (
            this.viewable &&
            !this.sharingInfo.node.isDirectory &&
            this.sharingInfo.node.mediatype !== 'link'
        );
    }

    /**
     * Whether the page shows a shared folder with the viewed element and the list of elements in the
     * right column
     */
    @HostBinding('class.folder-view')
    get rendersFolder(): boolean {
        return this.viewable && this.sharingInfo.node.isDirectory;
    }

    /**
     * Loads the render data of the shared material (single view) through the share
     */
    renderDataLoader = this.shareRender.load;

    /**
     * Actions of the render view: the download of the shown material, if the share allows it
     */
    renderOptions: OptionItem[] = this.createRenderOptions();

    /** what the preview in the sidebar offers: the download of the element, no link to the full view */
    private readonly previewConfig: PreviewConfig = {
        showAllDetails: false,
        customOptions: { useDefaultOptions: false, addOptions: this.renderOptions },
    };

    private createRenderOptions(): OptionItem[] {
        const download = new OptionItem('SHARING.DOWNLOAD', 'cloud_download', () =>
            // the single view downloads the shared node, the sidebar the selected element of the folder
            this.download(
                this.sharingInfo?.node.isDirectory
                    ? (this.editorialSidebar.nodes() as Node[])
                    : null,
            ),
        );
        download.elementType = [ElementType.Node];
        download.group = DefaultGroups.View;
        download.showAsAction = true;
        download.customShowCallback = async () => !!this.sharingInfo?.download;
        return [download];
    }

    /**
     * Click on an element of a shared folder: show it in the sidebar like the search does, if the
     * share allows viewing. Mirrors `EditorialSidebarService.handleSelect`, except that the sidebar
     * offers nothing besides the preview, so there is no way back to an option list.
     */
    onClick(entries: NodeEntriesWrapperComponent<Node>, event: NodeClickEvent<Node>) {
        if (!this.sharingInfo?.view || event.element.mediatype === 'link') {
            return;
        }
        const selection = entries.getSelection();
        const wasSelected =
            selection.selected.length === 1 && selection.selected[0] === event.element;
        selection.clear();
        if (wasSelected) {
            this.editorialSidebar.close();
            return;
        }
        selection.select(event.element);
        // the list of a share is no primary list, so the preview has to be told which one to step through
        // (after clearing the selection, since closing the sidebar forgets it)
        this.editorialSidebar.setPreviewList(entries);
        this.editorialSidebar.scope.set(Scope.Sharing);
        this.editorialSidebar.nodes.set([event.element]);
        this.editorialSidebar.showOption({
            option: 'PREVIEW',
            trap: true,
            optionConfig: this.previewConfig,
        });
    }

    /**
     * Keeps the sidebar and what the metadata set is loaded for in sync with the selection, also when
     * the preview steps to the previous / next element
     */
    selectionChange(selection: SelectionChange<Node>) {
        this.access.childId = selection.source.selected[0]?.ref.id ?? null;
        this.editorialSidebar.handleSelection(selection);
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
