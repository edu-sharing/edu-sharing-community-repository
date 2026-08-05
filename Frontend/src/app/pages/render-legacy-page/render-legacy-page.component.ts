import { trigger } from '@angular/animations';
import { Location, PlatformLocation } from '@angular/common';
import {
    AfterViewInit,
    Component,
    ComponentFactoryResolver,
    ElementRef,
    EventEmitter,
    HostListener,
    Injector,
    Input,
    NgZone,
    OnDestroy,
    OnInit,
    Output,
    ViewChild,
    ViewContainerRef,
    inject,
} from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import {
    ConfigService,
    HOME_REPOSITORY,
    MdsDefinition,
    MdsService,
    NetworkService,
    Node,
    NodeService,
    ProposalNode,
} from 'ngx-edu-sharing-api';
import {
    ActionbarComponent,
    ColumnType,
    DefaultGroups,
    ElementType,
    InteractionType,
    LocalEventsService,
    MdsHelperService,
    MdsWidgetComponent,
    NodeDataSource,
    NodeEntriesDisplayType,
    OptionItem,
    OptionsHelperDataService,
    Scope,
    Target,
    TemporaryStorageService,
    TranslationsService,
    UIAnimation,
    UIConstants,
    ViewInstanceService,
} from 'ngx-edu-sharing-ui';
import { BehaviorSubject, combineLatest, Subject } from 'rxjs';
import { AppComponent } from '../../app.component';
import { debounceTime, filter, first, skipWhile, takeUntil } from 'rxjs/operators';
import {
    ConfigurationHelper,
    ConfigurationService,
    EventListener,
    FrameEventsService,
    LoginResult,
    NodeList,
    RestConnectorService,
    RestConnectorsService,
    RestConstants,
    RestHelper,
    RestIamService,
    RestNetworkService,
    RestNodeService,
    RestSearchService,
    RestToolService,
    UIService,
} from '../../core-module/core.module';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { LoadingScreenService } from '../../main/loading-screen/loading-screen.service';
import { MainNavService } from '../../main/navigation/main-nav.service';
import { CardService } from '../../services/card.service';
import { NodeHelperService } from '../../services/node-helper.service';
import { OptionsHelperService } from '../../services/options-helper.service';
import { Toast } from '../../services/toast';
import $ from 'jquery';
import { BreadcrumbsService } from '../../shared/components/breadcrumbs/breadcrumbs.service';
import { CardComponent } from '../../shared/components/card/card.component';
import { RenderHelperService } from './render-helper.service';
import { CardDialogService } from '../../features/dialogs/card-dialog/card-dialog.service';
import { DialogsService } from '../../features/dialogs/dialogs.service';
import { MdsEditorInstanceService } from '../../features/mds/mds-editor/mds-editor-instance.service';
import { RouterHelper } from '../../util/router.helper';
import { SharedModule } from '../../shared/shared.module';
import { RenderingServiceLibModule } from 'ngx-rendering-service-lib';

export type SpecialRenderTemplate = 'revoked' | null;

@Component({
    selector: 'es-render-legacy-page',
    templateUrl: 'render-legacy-page.component.html',
    styleUrls: ['render-legacy-page.component.scss'],
    imports: [SharedModule, RenderingServiceLibModule],
    providers: [
        OptionsHelperDataService,
        RenderHelperService,
        MdsEditorInstanceService,
        ViewInstanceService,
    ],
    animations: [trigger('fadeFast', UIAnimation.fade(UIAnimation.ANIMATION_TIME_FAST))],
})
export class RenderLegacyPageComponent implements EventListener, OnInit, OnDestroy, AfterViewInit {
    private translate = inject(TranslateService);
    private translations = inject(TranslationsService);
    private uiService = inject(UIService);
    private nodeHelper = inject(NodeHelperService);
    private renderHelper = inject(RenderHelperService);
    private location = inject(Location);
    private mdsEditorInstanceService = inject(MdsEditorInstanceService);
    private viewInstanceService = inject(ViewInstanceService);
    private connector = inject(RestConnectorService);
    private connectors = inject(RestConnectorsService);
    private iam = inject(RestIamService);
    private mdsService = inject(MdsService);
    private mdsHelperService = inject(MdsHelperService);
    private nodeApi = inject(RestNodeService);
    private nodeService = inject(NodeService);
    private searchApi = inject(RestSearchService);
    private toolService = inject(RestToolService);
    private injector = inject(Injector);
    private cardServcie = inject(CardService);
    private viewContainerRef = inject(ViewContainerRef);
    private componentFactoryResolver = inject(ComponentFactoryResolver);
    private cardDialogService = inject(CardDialogService);
    private dialogsService = inject(DialogsService);
    private frame = inject(FrameEventsService);
    private toast = inject(Toast);
    private configLegacy = inject(ConfigurationService);
    private configService = inject(ConfigService);
    private route = inject(ActivatedRoute);
    private networkServiceLegacy = inject(RestNetworkService);
    private networkService = inject(NetworkService);
    private breadcrumbsService = inject(BreadcrumbsService);
    private router = inject(Router);
    private platformLocation = inject(PlatformLocation);
    private optionsHelper = inject(OptionsHelperDataService);
    private loadingScreen = inject(LoadingScreenService);
    mainNavService = inject(MainNavService);
    private temporaryStorageService = inject(TemporaryStorageService);
    private localEvents = inject(LocalEventsService);

    readonly DisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    specialTemplate: SpecialRenderTemplate;
    @Input() set node(node: Node | string) {
        const id = (node as Node).ref ? (node as Node).ref.id : (node as string);
        $('#nodeRenderContent').html('');
        this._nodeId = id;
        void this.loadRenderData();
    }
    constructor() {
        const _ngZone = inject(NgZone);

        (window as any).nodeRenderComponentRef = { component: this, zone: _ngZone };
        (window as any).ngRender = {
            setDownloadUrl: (url: string) => {
                void this.setDownloadUrl(url);
            },
        };
        this.frame.addListener(this, this.destroyed$);
        this.renderHelper.setViewContainerRef(this.viewContainerRef);

        this.translations.waitForInit().subscribe(() => {
            this.banner = ConfigurationHelper.getBanner(this.configService);
            this.connector.setRoute(this.route, this.router);
            this.networkServiceLegacy.prepareCache();
            combineLatest([this.route.queryParams, this.route.params])
                .pipe(debounceTime(10))
                .subscribe(([queryParams, params]) => {
                    this.closeOnBack = queryParams.closeOnBack === 'true';
                    this.editor = queryParams.editor;
                    this.fromLogin =
                        queryParams.fromLogin === 'true' || queryParams.redirectFromSSO === 'true';
                    this.repository = queryParams.repo || queryParams.repository || HOME_REPOSITORY;
                    this.queryParams = queryParams;
                    const childobject = queryParams.childobject_id
                        ? queryParams.childobject_id
                        : null;
                    this.isChildobject = childobject != null;
                    if (params.node) {
                        this.isRoute = true;
                        const dataSource: NodeDataSource<Node> = this.temporaryStorageService.get(
                            TemporaryStorageService.NODE_RENDER_PARAMETER_DATA_SOURCE,
                        );
                        if (dataSource) {
                            this.list = dataSource.getData();
                        } else {
                            this.list = this.temporaryStorageService.get(
                                TemporaryStorageService.NODE_RENDER_PARAMETER_LIST,
                            );
                        }
                        this.connector.isLoggedIn(false).subscribe((data: LoginResult) => {
                            this.isSafe = data.currentScope == RestConstants.SAFE_SCOPE;
                            if (params.version) {
                                this.version = params.version;
                            }
                            setTimeout(() => {
                                if (childobject) {
                                    this.node = childobject;
                                } else {
                                    this.node = params.node;
                                }
                            }, 10);
                        });
                    }
                });
        });
        this.frame.broadcastEvent(FrameEventsService.EVENT_VIEW_OPENED, 'node-render');
    }

    ngAfterViewInit(): void {
        this.mainNavService.getDialogs().storedAddToCollection.subscribe((event) => {
            this.refresh();
        });
    }

    ngOnInit(): void {
        this.mainNavService.setMainNavConfig({
            show: true,
            showNavigation: false,
            currentScope: 'render',
        });
        this.optionsHelper.registerGlobalKeyboardShortcuts();
        this.localEvents.nodesChanged
            .pipe(takeUntil(this.destroyed$))
            .subscribe(() => this.refresh());
        this.localEvents.nodesDeleted
            .pipe(takeUntil(this.destroyed$))
            .subscribe(() => this.close());
    }

    public isLoading = true;
    public isBuildingPage = false;
    /**
     * Show a bar at the top with the node name or not
     * @type {boolean}
     */
    @Input() showTopBar = true;
    /**
     * Node version, -1 indicates the latest
     * @type {string}
     */
    @Input() version = RestConstants.NODE_VERSION_CURRENT;
    /**
     *   display metadata
     */
    @Input() metadata = true;
    private isRoute = false;
    private list: Node[];
    private isSafe = false;
    private isOpenable: boolean;
    private closeOnBack: boolean;
    private editor: string;
    private fromLogin = false;
    public banner: any;
    private repository: string;
    private downloadButton: OptionItem;
    private downloadUrl: string;
    currentOptions: OptionItem[];
    sequence: NodeList;
    sequenceParent: Node;
    canScrollLeft = false;
    canScrollRight = false;
    private queryParams: Params;
    public similarNodes = new NodeDataSource<Node>();
    mds = new BehaviorSubject<MdsDefinition>(null);
    isDestroyed = false;
    private readonly destroyed$ = new Subject<void>();

    @ViewChild('sequencediv') sequencediv: ElementRef;
    @ViewChild('actionbar') actionbar: ActionbarComponent;
    isChildobject = false;
    _node: Node;
    _fromHomeRepository: boolean;
    _nodeId: string;
    @Output() closePage = new EventEmitter<void>();
    similarNodeColumns: ColumnType;

    @HostListener('window:beforeunload', ['$event'])
    beforeunloadHandler(event: any) {
        if (this.isSafe) {
            void this.connector.logout().toPromise();
        }
    }
    @HostListener('window:resize', ['$event'])
    onResize(event: any) {
        this.setScrollparameters();
    }

    @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent) {
        if (
            CardComponent.getNumberOfOpenCards() > 0 ||
            this.cardDialogService.openDialogs.length > 0
        ) {
            return;
        }
        if (event.code == 'ArrowLeft') {
            this.switchPosition(-1, true);
            event.preventDefault();
            event.stopPropagation();
            return;
        }
        if (event.code == 'ArrowRight') {
            this.switchPosition(+1, true);
            event.preventDefault();
            event.stopPropagation();
            return;
        }
    }
    close() {
        if (this.isRoute) {
            if (this.closeOnBack) {
                window.close();
            } else {
                if (this.fromLogin && !AppComponent.isRedirectedFromLogin()) {
                    void UIHelper.goToDefaultLocation(
                        this.router,
                        this.platformLocation,
                        this.configLegacy,
                        false,
                    );
                } else {
                    this.location.back();
                    // use a timeout to let the browser try to go back in history first
                    setTimeout(() => {
                        if (!this.isDestroyed) {
                            if (AppComponent.history.value?.length > 1) {
                                const last =
                                    AppComponent.history.value[
                                        AppComponent.history.value.length - 2
                                    ];
                                console.info('Enforcing back, may h5p navigation was present');
                                RouterHelper.navigateToAbsoluteUrl(
                                    this.platformLocation,
                                    this.router,
                                    last,
                                    true,
                                );
                                return;
                            }
                            this.mainNavService.patchMainNavConfig({ showNavigation: true });
                            setTimeout(() => {
                                this.mainNavService.getMainNav().topBar?.toggleMenuSidebar();
                                this.mainNavService
                                    .getMainNav()
                                    .topBar.closeScopeSelector.pipe(takeUntil(this.destroyed$))
                                    .subscribe(() => {
                                        this.mainNavService.patchMainNavConfig({
                                            showNavigation: false,
                                        });
                                    });
                            });
                        }
                    }, 250);
                }
            }
        } else this.closePage.emit();
    }

    showDetails() {
        const element = document.getElementById('edusharing_rendering_metadata');
        element.setAttribute('tabindex', '-1');
        element.addEventListener(
            'blur',
            (event) => (event.target as HTMLElement).removeAttribute('tabindex'),
            { once: true },
        );
        element.focus({ preventScroll: true });
        element.scrollIntoView({ behavior: 'smooth' });
    }
    public getPosition() {
        if (!this._node || !this.list) return -1;
        let i = 0;
        for (const node of this.list) {
            if (node.ref.id == this._node.ref.id || node.ref.id == this.sequenceParent.ref.id)
                return i;
            i++;
        }
        return -1;
    }
    onEvent(event: string, data: any): void {
        if (event == FrameEventsService.EVENT_REFRESH) {
            this.refresh();
        }
    }
    ngOnDestroy() {
        (window as any).ngRender = null;
        this.isDestroyed = true;
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    public switchPosition(offset: number, useSequence = false) {
        const pos = this.getPosition() + offset;
        if (this.sequence?.nodes?.length > 1 && useSequence) {
            if (this.getSequencePosition() === 0 && offset === -1) {
                void this.router.navigate(['./'], {
                    replaceUrl: true,
                    relativeTo: this.route,
                    queryParamsHandling: 'merge',
                    queryParams: { childobject_id: null },
                });
                return;
            }
            const index = this.getSequencePosition() + offset;
            if (index >= 0 && index < this.sequence.nodes.length) {
                void this.router.navigate(['./'], {
                    replaceUrl: true,
                    relativeTo: this.route,
                    queryParamsHandling: 'merge',
                    queryParams: { childobject_id: this.sequence.nodes[index].ref.id },
                });
                return;
            }
        }
        if ((offset > 0 && !this.canSwitchForward()) || (offset < 0 && !this.canSwitchBack())) {
            return;
        }
        this.isLoading = true;
        this.sequence = null;
        void this.router.navigate([UIConstants.ROUTER_PREFIX, 'render', this.list[pos].ref.id], {
            replaceUrl: true,
            queryParamsHandling: 'merge',
            queryParams: { childobject_id: null },
        });
    }

    private getSequencePosition() {
        return this.sequence.nodes.findIndex((n) => n.ref.id === this._nodeId);
    }

    public canSwitchBack() {
        return (
            this.list && this.getPosition() > 0 && !this.list[this.getPosition() - 1].isDirectory
        );
    }
    public canSwitchForward() {
        return (
            this.list &&
            this.getPosition() < this.list.length - 1 &&
            !this.list[this.getPosition() + 1].isDirectory
        );
    }
    public refresh() {
        if (this.isLoading) {
            return;
        }
        this.optionsHelper.clearComponents(this.actionbar);
        this.isLoading = true;
        this.node = this._nodeId;
    }
    viewParent() {
        this.isChildobject = false;
        void this.router.navigate([], {
            relativeTo: this.route,
            queryParamsHandling: 'merge',
            queryParams: {
                childobject_id: null,
            },
            replaceUrl: true,
        });
    }
    viewChildobject(node: Node, pos: number) {
        this.isChildobject = true;
        void this.router.navigate([], {
            relativeTo: this.route,
            queryParamsHandling: 'merge',
            queryParams: {
                childobject_id: node.ref.id,
            },
            replaceUrl: true,
        });
    }
    private loadNode() {
        if (!this._node) {
            this.isBuildingPage = false;
            return;
        }

        const download = new OptionItem('OPTIONS.DOWNLOAD', 'cloud_download', () =>
            this.downloadCurrentNode(),
        );
        download.elementType = OptionsHelperService.DownloadElementTypes;
        // use callback since isEnabled gets ignored
        download.customEnabledCallback = async (nodes) => {
            return this.downloadUrl
                ? true
                : this._node.downloadUrl != null &&
                      (!(
                          this._node.properties[RestConstants.CCM_PROP_IO_WWWURL] &&
                          !this._node.properties[RestConstants.VIRTUAL_PROP_LINK_DOWNLOAD_ALLOWED]
                      ) ||
                          !this._fromHomeRepository);
        };
        download.group = DefaultGroups.View;
        download.priority = 25;
        download.showAsAction = true;
        if (this.isCollectionRef()) {
            this.nodeApi
                .getNodeMetadata(this._node.properties[RestConstants.CCM_PROP_IO_ORIGINAL]?.[0])
                .subscribe(
                    (node) => {
                        this.addDownloadButton(download);
                    },
                    (error: any) => {
                        if (error.status == RestConstants.HTTP_NOT_FOUND) {
                            download.isEnabled = false;
                        }
                        this.addDownloadButton(download);
                    },
                );
            return;
        }
        this.addDownloadButton(download);
    }
    private async loadRenderData() {
        const loadingTask = this.loadingScreen.addLoadingTask({ until: this.destroyed$ });
        this.isLoading = true;
        this.optionsHelper.clearComponents(this.actionbar);
        if (this.isBuildingPage) {
            setTimeout(() => this.loadRenderData(), 50);
            return;
        }
        const parameters = {
            showDownloadButton: this.configLegacy.instant('rendering.showDownloadButton', false),
            showDownloadAdvice: !this.isOpenable,
        };
        this._node = null;
        this.specialTemplate = null;
        this.isBuildingPage = true;
        // we only fetching versions for the primary parent (child objects don't have versions)
        this.nodeApi
            .getNodeRenderSnippet(
                this._nodeId,
                this.version && !this.isChildobject ? this.version : '-1',
                parameters,
                this.repository,
            )
            .subscribe(
                async (data) => {
                    if (!data.detailsSnippet) {
                        console.error(data);
                        this.toast.error(null, 'RENDERSERVICE_API_ERROR');
                    } else {
                        try {
                            this._node = data.node;
                            // if it's an remote repo, the repo id will be updated from the node endpoint
                            this.repository = data.node.ref.repo || HOME_REPOSITORY;
                            this._fromHomeRepository = await this.networkService
                                .isFromHomeRepository(this._node)
                                .pipe(first())
                                .toPromise();
                            if (this._fromHomeRepository) {
                                this.nodeService.getParents(this._nodeId).subscribe(
                                    (nodes) =>
                                        this.breadcrumbsService.setNodePath(nodes.nodes.reverse()),
                                    (error) => {
                                        console.info("Parent can' be fetched", error);
                                        error.preventDefault();
                                    },
                                );
                            }
                            this.isOpenable =
                                this.connectors.connectorSupportsEdit(this._node) != null;
                        } catch (e) {
                            console.error('error post-processing rendering node', data.node, e);
                            this.toast.error(e);
                        }
                        this.mds.pipe(filter((set) => !!set)).subscribe((set) => {
                            this.similarNodeColumns = this.mdsHelperService.getColumns(
                                set,
                                'search',
                            );
                            void this.linkSearchableWidgets();
                        });
                        this.mdsService
                            .getMetadataSet({
                                repository: this.repository,
                                metadataSet: this.getMdsId(),
                            })
                            .subscribe((mds: MdsDefinition) => {
                                this.mds.next(mds);
                            });
                        const finish = () => {
                            this.isLoading = false;
                            const nodeRenderContent = $('#nodeRenderContent');
                            nodeRenderContent.html(data.detailsSnippet);
                            this.moveInnerStyleToHead(nodeRenderContent);
                            this.postprocessHtml();
                            void this.handleProposal();
                            this.renderHelper.doAll(this._node);
                            this.loadNode();
                            // this.loadSimilarNodes();
                            void this.linkSearchableWidgets();
                            this.specialTemplate = null;
                            if (this.nodeHelper.isNodeRevoked(this._node)) {
                                this.specialTemplate = 'revoked';
                                // revoked type have NO content if they've been published nodes
                                if (this._node.size === '0') {
                                    const element = document.getElementsByClassName(
                                        'edusharing_rendering_content_wrapper',
                                    )?.[0];
                                    element.parentElement?.removeChild(element);
                                }
                            }
                        };
                        this.getSequence(() => {
                            finish();
                        });
                    }
                    this.isLoading = false;
                    loadingTask.done();
                },
                (error) => {
                    console.warn(error.error.error);
                    if (
                        error?.error?.error === 'org.edu_sharing.restservices.DAOMissingException'
                    ) {
                        this.toast.error(null, 'TOAST.RENDER_NOT_FOUND', null, null, null, {
                            link: {
                                caption: 'BACK',
                                callback: () => this.close(),
                            },
                        });
                    } else {
                        this.toast.error(error);
                    }
                    this.isLoading = false;
                    loadingTask.done();
                },
            );
    }
    private postprocessHtml() {
        if (!this.configLegacy.instant('rendering.showPreview', true)) {
            $('.edusharing_rendering_content_wrapper').hide();
            $('.showDetails').hide();
        }
        if (this.isOpenable) {
            $('#edusharing_downloadadvice').hide();
        }
        const element = $('#edusharing_rendering_content_href');
        element.click((event: any) => {
            if (this.connector.getBridgeService().isRunningCordova()) {
                const href = element.attr('href');
                this.connector.getBridgeService().getCordova().openBrowser(href);
                event.preventDefault();
            }
        });
    }
    private downloadSequence() {
        const nodes = [this.sequenceParent].concat(this.sequence.nodes);
        void this.nodeHelper.downloadNodes(nodes, this.sequenceParent.name + '.zip');
    }

    private downloadCurrentNode() {
        if (this.downloadUrl) {
            this.nodeHelper.downloadUrl(this.downloadUrl, 'download', {
                node: this._node,
                triggerTrackingEvent: true,
            });
        } else {
            void this.nodeHelper.downloadNode(this._node, this.isChildobject ? null : this.version);
        }
    }

    private openConnector(node: Node, newWindow = true) {
        if (RestToolService.isLtiObject(node)) {
            this.toolService.openLtiObject(node);
        } else {
            void this.uiService.editConnector(node);
        }
    }

    private async initOptions() {
        this.optionsHelper.setData({
            scope: Scope.Render,
            activeObjects: [this._node],
            parent: { ref: { id: this._node.parent.id } },
            allObjects: this.list,
            customOptions: {
                useDefaultOptions: true,
                addOptions: this.currentOptions,
            },
            postPrepareOptions: (options, objects) => {
                if (this.version && this.version !== RestConstants.NODE_VERSION_CURRENT) {
                    options.filter((o) => o.name === 'OPTIONS.OPEN')[0].customEnabledCallback =
                        async () => false;
                    options.filter((o) => o.name === 'OPTIONS.OPEN')[0].isEnabled = false;
                }
            },
        });
        await this.optionsHelper.initComponents(this.actionbar);
        void this.optionsHelper.refreshComponents();
        this.postprocessHtml();
        this.isBuildingPage = false;
        void void this.handleQueryAction();
    }

    private isCollectionRef() {
        return this._node.aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) != -1;
    }

    private addDownloadButton(download: OptionItem) {
        const addButton = (data: NodeList = null) => {
            this.downloadButton = download;
            const options: OptionItem[] = [];
            options.splice(0, 0, download);
            if (
                data?.nodes.length > 0 ||
                this._node.aspects.indexOf(RestConstants.CCM_ASPECT_IO_CHILDOBJECT) != -1
            ) {
                const downloadAll = new OptionItem('OPTIONS.DOWNLOAD_ALL', 'archive', () => {
                    this.downloadSequence();
                });
                downloadAll.elementType = [
                    ElementType.Node,
                    ElementType.NodeChild,
                    ElementType.NodePublishedCopy,
                ];
                downloadAll.group = DefaultGroups.View;
                downloadAll.priority = 35;
                options.splice(1, 0, downloadAll);
            }
            this.currentOptions = options;
        };
        addButton(this.sequence);
        void this.initOptions();
    }

    async setDownloadUrl(url: string) {
        console.info('url from rendering', url);
        this.downloadUrl = url;
        void this.optionsHelper.refreshComponents();
    }

    private getSequence(onFinish: () => void) {
        if (this._node.aspects.indexOf(RestConstants.CCM_ASPECT_IO_CHILDOBJECT) != -1) {
            this.nodeApi
                .getNodeMetadata(this._node.parent.id, [RestConstants.ALL], this._node.parent.repo)
                .subscribe((data) => {
                    this.sequenceParent = data.node;
                    this.nodeApi
                        .getNodeChildobjects(
                            this.sequenceParent.ref.id,
                            this.sequenceParent.ref.repo,
                        )
                        .subscribe((data: NodeList) => {
                            if (data.nodes.length > 0) {
                                this.sequence = data;
                            } else {
                                this.sequence = null;
                            }
                            setTimeout(() => this.setScrollparameters(), 100);
                            onFinish();
                        });
                });
        } else {
            this.sequenceParent = this._node;
            this.nodeApi
                .getNodeChildobjects(this.sequenceParent.ref.id, this.sequenceParent.ref.repo)
                .subscribe(
                    (data: NodeList) => {
                        if (data.nodes.length > 0) {
                            this.sequence = data;
                        } else {
                            this.sequence = null;
                        }
                        setTimeout(() => this.setScrollparameters(), 100);
                        onFinish();
                    },
                    (error) => {
                        console.error('failed sequence fetching', error);
                        onFinish();
                    },
                );
        }
    }

    scroll(direction: string) {
        const element = this.sequencediv.nativeElement;
        const width = window.innerWidth / 2;
        void this.uiService
            .scrollSmoothElement(
                element.scrollLeft + (direction == 'left' ? -width : width),
                element,
                2,
                'x',
            )
            .then((limit) => {
                this.setScrollparameters();
            });
    }

    private setScrollparameters() {
        if (!this.sequence) return;
        const element = this.sequencediv.nativeElement;
        if (element.scrollLeft <= 20) {
            this.canScrollLeft = false;
        } else {
            this.canScrollLeft = true;
        }
        if (element.scrollLeft + 20 >= element.scrollWidth - window.innerWidth) {
            this.canScrollRight = false;
        } else {
            this.canScrollRight = true;
        }
    }
    private getNodeName(node: Node) {
        return RestHelper.getName(node);
    }
    getName(): string {
        if (this._node) {
            return this.getNodeName(this._node);
        } else {
            return '';
        }
    }
    getNodeTitle(node: Node) {
        return RestHelper.getTitle(node);
    }

    public switchNode(event: any) {
        this.uiService.scrollSmooth();
        this.node = event.node;
    }

    private loadSimilarNodes() {
        this.similarNodes.isLoading = true;
        this.searchApi.searchFingerprint(this._nodeId).subscribe((data: NodeList) => {
            this.similarNodes.isLoading = false;
            this.similarNodes.setData(data.nodes, data.pagination);
        });
    }

    private async linkSearchableWidgets() {
        try {
            this.viewInstanceService.treeDisplay = 'path';
            await this.mdsEditorInstanceService.initWithNodes([this._node], {
                editorMode: 'viewer',
            });
            this.mdsEditorInstanceService.widgets.value
                ?.filter((w) => w.definition.isSearchable)
                .forEach((w) => {
                    try {
                        const values = document.querySelectorAll(
                            "#edusharing_rendering_metadata [data-widget-id='" +
                                w.definition.id +
                                "'] .mdsWidgetMultivalue",
                        );
                        values.forEach((v: HTMLElement) => {
                            const parent = v.parentElement;
                            // we remove the caption since it is already present
                            w.definition.caption = null;
                            this.uiService.injectAngularComponent(
                                this.viewContainerRef,
                                MdsWidgetComponent,
                                v,
                                {
                                    widget: w,
                                },
                                {},
                                this.injector,
                            );
                        });
                    } catch (e) {
                        console.warn(e);
                    }
                });
            // document.getElementsByClassName("edusharing_rendering_content_wrapper")[0].ge;
        } catch (e) {
            console.warn('Could not read the widget list from the metadataset', e);
        }
    }

    private getMdsId() {
        return this._node.metadataset ? this._node.metadataset : RestConstants.DEFAULT;
    }

    private async handleProposal() {
        if (this.queryParams.proposal && this.queryParams.proposalCollection) {
            (this._node as ProposalNode).proposal = (
                await this.nodeApi
                    .getNodeMetadata(this.queryParams.proposal, [RestConstants.ALL])
                    .toPromise()
            ).node;
            (this._node as ProposalNode).proposalCollection = {
                ref: { id: this.queryParams.proposalCollection },
            } as Node;
            // access is granted when we can fetch the node
            (this._node as ProposalNode).accessible = true;
            void this.optionsHelper.refreshComponents();
        }
    }

    /**
     * check if the current url requested to directly open an action (from the actionbar),
     * and if so, call it
     */
    private async handleQueryAction() {
        if (this.queryParams.action) {
            const option = (await this.optionsHelper.getAvailableOptions(Target.Actionbar)).filter(
                (o) => o.name === this.queryParams.action,
            )?.[0];
            if (option) {
                if (option.isEnabled) {
                    option.callback();
                    // wait until a dialog has opened, then, as soon as the particular dialog closed
                    // trigger that the action has been done
                    this.cardServcie.hasOpenModals
                        .pipe(
                            skipWhile((h) => !h),
                            filter((h) => !h),
                        )
                        .subscribe(() => this.onQueryActionDone());
                } else {
                    console.warn('action ' + this.queryParams.action + ' is currently not enabled');
                }
            } else {
                console.warn(
                    'action ' +
                        this.queryParams.action +
                        ' is either not supported or not allowed for current user',
                );
            }
        }
    }

    private onQueryActionDone() {
        if (this.queryParams.action) {
            window.close();
        }
    }

    /**
     * Moves a style element that is a child of `element` to the document head.
     *
     * Existing style elements that were previously moved to the document head like this will be
     * removed.
     *
     * The style element will be removed from document head on `ngDestroy`.
     */
    private moveInnerStyleToHead(element: any): void {
        const styleAttr = 'data-render-content-style';
        $('[' + styleAttr + ']').remove();
        const style = element.find('style');
        style.attr(styleAttr, '');
        $(document.head).append(style);
        this.destroyed$.subscribe(() => style.remove());
    }
}
