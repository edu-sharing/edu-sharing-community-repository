import { trigger } from '@angular/animations';
import { Component, HostListener, NgZone, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { delay, filter, first, switchMap } from 'rxjs/operators';
import {
    DropSource,
    DropTarget,
    NodeEntriesDisplayType,
    NodeRoot,
} from 'src/app/features/node-entries/entries-model';
import { NodeDataSource } from 'src/app/features/node-entries/node-data-source';
import {
    ClipboardObject,
    ConfigurationService,
    Connector,
    DialogButton,
    EventListener,
    Filetype,
    FrameEventsService,
    IamUser,
    Node,
    NodeList,
    NodeWrapper,
    RestConnectorService,
    RestConnectorsService,
    RestConstants,
    RestHelper,
    RestIamService,
    RestMdsService,
    RestNodeService,
    RestToolService,
    SessionStorageService,
    TemporaryStorageService,
    UIService,
} from '../../core-module/core.module';
import { Helper } from '../../core-module/rest/helper';
import { KeyEvents } from '../../core-module/ui/key-events';
import { UIAnimation } from '../../core-module/ui/ui-animation';
import { UIConstants } from '../../core-module/ui/ui-constants';
import { CardService } from '../../core-ui-module/card.service';
import { NodeHelperService } from '../../core-ui-module/node-helper.service';
import {
    CustomOptions,
    DefaultGroups,
    ElementType,
    OptionItem,
} from '../../core-ui-module/option-item';
import { Toast } from '../../core-ui-module/toast';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { LoadingScreenService } from '../../main/loading-screen/loading-screen.service';
import { MainNavService } from '../../main/navigation/main-nav.service';
import { DragData } from '../../services/nodes-drag-drop.service';
import { ActionbarComponent } from '../../shared/components/actionbar/actionbar.component';
import { CanDrop } from '../../shared/directives/nodes-drop-target.directive';
import { TranslationsService } from '../../translations/translations.service';
import { WorkspaceExplorerComponent } from './explorer/explorer.component';
import { canDragDrop, canDropOnNode } from './workspace-utils';

@Component({
    selector: 'es-workspace-main',
    templateUrl: 'workspace.component.html',
    styleUrls: ['workspace.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('fadeFast', UIAnimation.fade(UIAnimation.ANIMATION_TIME_FAST)),
        trigger('overlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST)),
        trigger('fromLeft', UIAnimation.fromLeft()),
        trigger('fromRight', UIAnimation.fromRight()),
    ],
})
export class WorkspaceMainComponent implements EventListener, OnInit, OnDestroy {
    private static VALID_ROOTS = [
        'MY_FILES',
        'SHARED_FILES',
        'MY_SHARED_FILES',
        'TO_ME_SHARED_FILES',
        'WORKFLOW_RECEIVE',
        'RECYCLE',
    ];
    private static VALID_ROOTS_NODES = [
        RestConstants.USERHOME,
        '-shared_files-',
        '-my_shared_files-',
        '-to_me_shared_files_personal-',
        '-to_me_shared_files-',
        '-workflow_receive-',
    ];

    @ViewChild('explorer') explorer: WorkspaceExplorerComponent;
    @ViewChild('actionbar') actionbarRef: ActionbarComponent;

    cardHasOpenModals$: Observable<boolean>;
    private isRootFolder: boolean;
    private sharedFolders: Node[] = [];
    path: Node[] = [];
    private parameterNode: Node;
    root: NodeRoot = 'MY_FILES';

    showSelectRoot = false;

    private allowBinarySubject = new BehaviorSubject(true);
    get allowBinary() {
        return this.allowBinarySubject.value;
    }
    set allowBinary(value) {
        this.allowBinarySubject.next(value);
    }
    private createAllowedSubject = new BehaviorSubject<boolean | 'EMIT_EVENT'>(null);
    get createAllowed(): boolean | 'EMIT_EVENT' {
        return this.createAllowedSubject.value;
    }
    set createAllowed(value: boolean | 'EMIT_EVENT') {
        this.createAllowedSubject.next(value);
    }
    private currentFolderSubject = new BehaviorSubject<Node>(null);
    get currentFolder(): Node {
        return this.currentFolderSubject.value;
    }
    set currentFolder(value: Node) {
        this.currentFolderSubject.next(value);
    }
    private searchQuerySubject = new BehaviorSubject<{ node: Node; query: string }>(null);
    get searchQuery(): { node: Node; query: string } {
        return this.searchQuerySubject.value;
    }
    set searchQuery(value: { node: Node; query: string }) {
        this.searchQuerySubject.next(value);
    }

    globalProgress = false;
    editNodeDeleteOnCancel = false;
    notAllowedReason: string;
    user: IamUser;
    isSafe = false;
    isLoggedIn = false;
    addNodesToCollection: Node[];
    addNodesStream: Node[];
    variantNode: Node;
    mainnav = true;
    isBlocked = false;

    customOptions: CustomOptions = {
        useDefaultOptions: true,
    };

    toMeSharedToggle: boolean;

    dataSource = new NodeDataSource<Node>();
    private reurl: string;
    showLtiTools = false;
    private oldParams: Params;
    selectedNodeTree: string;
    contributorNode: Node;
    shareLinkNode: Node;
    displayType: NodeEntriesDisplayType = null;
    reorderDialog: boolean;
    private readonly destroyed$ = new Subject<void>();
    private loadingTask = this.loadingScreen.addLoadingTask({ until: this.destroyed$ });

    constructor(
        private toast: Toast,
        private route: ActivatedRoute,
        private router: Router,
        private nodeHelper: NodeHelperService,
        private translate: TranslateService,
        private translations: TranslationsService,
        private storage: TemporaryStorageService,
        private config: ConfigurationService,
        private connectors: RestConnectorsService,
        private toolService: RestToolService,
        private session: SessionStorageService,
        private iam: RestIamService,
        private mds: RestMdsService,
        private node: RestNodeService,
        private ui: UIService,
        private event: FrameEventsService,
        private connector: RestConnectorService,
        private card: CardService,
        private ngZone: NgZone,
        private loadingScreen: LoadingScreenService,
        private mainNavService: MainNavService,
    ) {
        this.event.addListener(this, this.destroyed$);
        this.translations.waitForInit().subscribe(() => {
            void this.initialize();
        });
        this.connector.setRoute(this.route);
        this.globalProgress = true;
        this.cardHasOpenModals$ = this.card.hasOpenModals.pipe(delay(0));
    }

    ngOnInit(): void {
        this.registerScroll();
        this.registerUpdateMainNav();
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
        this.storage.remove('workspace_clipboard');
        if (this.currentFolder) {
            this.storage.set(this.getLastLocationStorageId(), this.currentFolder.ref.id);
        }
        // close sidebar, if open
        this.mainNavService.getDialogs().closeSidebar();
    }

    @HostListener('window:beforeunload', ['$event'])
    beforeunloadHandler(event: any) {
        if (this.isSafe) {
            void this.connector.logout().toPromise();
        }
    }

    private handleScroll(event: Event) {
        const scroll = window.pageYOffset || document.documentElement.scrollTop;
        if (scroll > 0) {
            this.storage.set('workspace_scroll', scroll);
        }
    }

    onEvent(event: string, data: any): void {
        if (event === FrameEventsService.EVENT_REFRESH) {
            this.refresh();
        }
    }

    /**
     * Needs the following member variables to be initialized:
     * - isSafe
     * - isBlocked
     * - mainnav
     */
    private initMainNav(): void {
        this.mainNavService.setMainNavConfig({
            title: this.isSafe ? 'WORKSPACE.TITLE_SAFE' : 'WORKSPACE.TITLE',
            currentScope: this.isSafe ? 'safe' : 'workspace',
            searchEnabled: !this.isBlocked,
            create: {
                allowed: this.createAllowed,
                allowBinary: this.allowBinary,
                parent: this.currentFolder,
                folder: true,
            },
            onCreate: (nodes) => this.explorer.nodeEntries.addVirtualNodes(nodes),
            onCreateNotAllowed: () => this.createNotAllowed(),
            searchPlaceholder: this.isSafe ? 'WORKSPACE.SAFE_SEARCH' : 'WORKSPACE.SEARCH',
            canOpen: this.mainnav,
            searchQuery: this.searchQuery?.query,
            onSearch: (query, cleared) => this.doSearch({ query, cleared }),
        });
    }

    private registerUpdateMainNav(): void {
        rxjs.combineLatest([
            this.createAllowedSubject,
            this.allowBinarySubject,
            this.currentFolderSubject,
        ]).subscribe(([createAllowed, allowBinary, currentFolder]) =>
            this.mainNavService.patchMainNavConfig({
                create: {
                    allowed: createAllowed,
                    allowBinary: allowBinary,
                    parent: currentFolder,
                    folder: true,
                },
            }),
        );
        this.searchQuerySubject.subscribe((searchQuery) =>
            this.mainNavService.patchMainNavConfig({
                searchQuery: searchQuery?.query,
            }),
        );
    }

    private registerScroll(): void {
        this.ngZone.runOutsideAngular(() => {
            const handleScroll = (event: Event) => this.handleScroll(event);
            window.addEventListener('scroll', handleScroll);
            this.destroyed$.subscribe(() => window.removeEventListener('scroll', handleScroll));
        });
    }

    private hideDialog(): void {
        this.toast.closeModalDialog();
    }

    private editConnector(
        node: Node = null,
        type: Filetype = null,
        win: any = null,
        connectorType: Connector = null,
    ) {
        UIHelper.openConnector(
            this.connectors,
            this.iam,
            this.event,
            this.toast,
            this.getNodeList(node)[0],
            type,
            win,
            connectorType,
        );
    }

    handleDrop(event: { target: DropTarget; source: DropSource<Node> }) {
        if (event.source.mode === 'copy') {
            this.copyNode(event.target, event.source.element);
        } else {
            this.moveNode(event.target, event.source.element);
        }
        /*
        this.dialogTitle="WORKSPACE.DRAG_DROP_TITLE";
        this.dialogCancelable=true;
        this.dialogMessage="WORKSPACE.DRAG_DROP_MESSAGE";
        this.dialogMessageParameters={source:event.source.name,target:event.target.name};
        this.dialogButtons=[
          new DialogButton("WORKSPACE.DRAG_DROP_COPY",{ color: 'primary' },()=>this.copyNode(event.target,event.source)),
          new DialogButton("WORKSPACE.DRAG_DROP_MOVE",{ color: 'primary' },()=>this.moveNode(event.target,event.source)),
        ]
        */
    }

    handleDropOnBreadcrumb(event: { target: Node | 'HOME'; source: DropSource<Node> }) {
        if (event.target === 'HOME') {
            this.handleDrop({ target: this.root, source: event.source });
        } else {
            this.handleDrop(event as { target: Node; source: DropSource<Node> });
        }
    }

    canDropOnBreadcrumb = (dragData: DragData<'HOME' | Node>): CanDrop => {
        if (dragData.target === 'HOME') {
            if (this.root === 'MY_FILES') {
                return canDragDrop(dragData);
            } else {
                return { accept: false, denyExplicit: false };
            }
        } else {
            return canDropOnNode(dragData as DragData<Node>);
        }
    };

    private moveNode(target: DropTarget, source: Node[], position = 0) {
        this.globalProgress = true;
        if (position >= source.length) {
            this.finishMoveCopy(target, source, false);
            this.globalProgress = false;
            return;
        }
        this.node
            .moveNode((target as Node).ref?.id || RestConstants.USERHOME, source[position].ref.id)
            .subscribe(
                (data: NodeWrapper) => {
                    this.moveNode(target, source, position + 1);
                },
                (error: any) => {
                    this.nodeHelper.handleNodeError(source[position].name, error);
                    source.splice(position, 1);
                    this.moveNode(target, source, position + 1);
                },
            );
    }

    private copyNode(target: DropTarget, source: Node[], position = 0) {
        this.globalProgress = true;
        if (position >= source.length) {
            this.finishMoveCopy(target, source, true);
            this.globalProgress = false;
            return;
        }
        this.node
            .copyNode((target as Node).ref?.id || RestConstants.USERHOME, source[position].ref.id)
            .subscribe(
                (data: NodeWrapper) => {
                    this.copyNode(target, source, position + 1);
                },
                (error: any) => {
                    this.nodeHelper.handleNodeError(source[position].name, error);
                    source.splice(position, 1);
                    this.copyNode(target, source, position + 1);
                },
            );
    }

    private finishMoveCopy(target: DropTarget, source: Node[], copy: boolean) {
        this.toast.closeModalDialog();
        const info: any = {
            to: (target as Node).name || this.translate.instant('WORKSPACE.MY_FILES'),
            count: source.length,
            mode: this.translate.instant('WORKSPACE.' + (copy ? 'PASTE_COPY' : 'PASTE_MOVE')),
        };
        if (source.length) {
            this.toast.toast('WORKSPACE.TOAST.PASTE_DRAG', info);
        }
        this.globalProgress = false;
        this.refresh();
    }

    private async initialize() {
        this.user = await this.iam.getCurrentUserAsync();
        this.route.params.subscribe((routeParams: Params) => this.handleParamsUpdate(routeParams));
        this.route.queryParams.subscribe((params: Params) => this.handleQueryParamsUpdate(params));
    }

    private async handleParamsUpdate(routeParams: Params) {
        this.isSafe = routeParams.mode === 'safe';
        const login = await this.connector.isLoggedIn().toPromise();
        if (login.statusCode !== RestConstants.STATUS_CODE_OK) {
            RestHelper.goToLogin(this.router, this.config);
            return;
        }
        await this.prepareActionbar();
        this.loadFolders(this.user);

        let valid = true;
        if (!login.isValidLogin || login.isGuest) {
            valid = false;
        }
        this.isBlocked = !this.connector.hasToolPermissionInstant(
            RestConstants.TOOLPERMISSION_WORKSPACE,
        );
        if (this.isSafe && login.currentScope !== RestConstants.SAFE_SCOPE) {
            valid = false;
        }
        if (!this.isSafe && login.currentScope != null) {
            this.connector.logout().subscribe(
                () => {
                    this.goToLogin();
                },
                (error: any) => {
                    this.toast.error(error);
                    this.goToLogin();
                },
            );
            return;
        }
        if (!valid) {
            this.goToLogin();
            return;
        }
        this.connector.scope = this.isSafe ? RestConstants.SAFE_SCOPE : null;
        this.isLoggedIn = true;
        this.globalProgress = false;
    }

    private handleQueryParamsUpdate(params: Params) {
        let needsUpdate = false;
        if (this.oldParams) {
            for (const key of Object.keys(this.oldParams).concat(Object.keys(params))) {
                if (params[key] === this.oldParams[key]) {
                    continue;
                }
                if (key === UIConstants.QUERY_PARAM_LIST_VIEW_TYPE) {
                    continue;
                }
                needsUpdate = true;
            }
        } else {
            needsUpdate = true;
        }
        if (params.displayType != null) {
            this.setDisplayType(
                parseInt(params[UIConstants.QUERY_PARAM_LIST_VIEW_TYPE], 10),
                false,
            );
        } else {
            this.setDisplayType(
                this.config.instant(
                    'workspaceViewType',
                    NodeEntriesDisplayType.Table,
                ) as NodeEntriesDisplayType,
                false,
            );
        }
        if (params.root && WorkspaceMainComponent.VALID_ROOTS.indexOf(params.root) !== -1) {
            this.root = params.root;
        } else {
            this.root = 'MY_FILES';
        }
        if (params.reurl) {
            this.reurl = params.reurl;
        }
        this.mainnav = params.mainnav === 'false' ? false : true;

        this.initMainNav();

        if (params.file && params.file !== this.oldParams?.file) {
            void this.showNodeInCurrentFolder(params.file);
        }

        if (!needsUpdate) {
            return;
        }
        this.createAllowed = this.root === 'MY_FILES';
        let lastLocation = this.storage.pop(this.getLastLocationStorageId(), null);
        if (this.isSafe) {
            // clear lastLocation, this is another folder than the safe
            lastLocation = null;
        }
        if (!params.id && !params.query && lastLocation) {
            this.openDirectory(lastLocation, { replaceUrl: true });
        } else {
            this.openDirectoryFromRoute(params);
        }
        if (params.showAlpha) {
            this.showAlpha();
        }
        this.oldParams = params;
    }

    private async showNodeInCurrentFolder(id: string) {
        // TODO: Consider moving this to `NodeDataSource`. We would need to make sure that the
        // dataSource is not replaced by explorer, however.
        const visibleNodes = await this.explorer.dataSourceSubject
            .pipe(
                filter(notNull),
                switchMap((dataSource) => dataSource.connect()),
                first((data) => data?.length > 1),
            )
            .toPromise();
        let node = visibleNodes.find((node) => node.ref.id === id);
        if (!node) {
            ({ node } = await this.node.getNodeMetadata(id, [RestConstants.ALL]).toPromise());
            if (node.parent?.id === this.currentFolder?.ref.id) {
                this.explorer.dataSource.appendData([node], 'before');
                // FIXME: The appended node will show up a second time when loading more data.
            } else {
                this.toast.error(null, 'WORKSPACE.TOAST.ELEMENT_NOT_IN_FOLDER');
                return;
            }
        }
        this.setSelection([node]);
        this.parameterNode = node;
        this.mainNavService.getDialogs().nodeSidebar = node;
    }

    resetWorkspace() {
        if (this.mainNavService.getDialogs().nodeSidebar && this.parameterNode) {
            this.setSelection([this.parameterNode]);
        }
    }

    doSearch(query: any) {
        const id = this.currentFolder
            ? this.currentFolder.ref.id
            : this.searchQuery && this.searchQuery.node
            ? this.searchQuery.node.ref.id
            : null;
        void this.routeTo(this.root, id, query.query);
        if (!query.cleared) {
            this.ui.hideKeyboardIfMobile();
        }
    }

    private doSearchFromRoute(params: any, node: Node | any) {
        node = this.isRootFolder ? null : node;
        this.searchQuery = {
            query: params.query,
            node,
        };
        if (node == null && this.root !== 'RECYCLE') {
            this.root = 'ALL_FILES';
        }
        this.createAllowed = 'EMIT_EVENT';
        this.notAllowedReason = 'WORKSPACE.CREATE_REASON.SEARCH';
        this.path = [];
        this.setSelection([]);
    }

    private deleteDone() {
        this.closeMetadata();
        this.refresh();
    }

    private displayNode(event: Node) {
        const list = this.getNodeList(event);
        this.closeMetadata();
        if (list[0].isDirectory || list[0].type === RestConstants.SYS_TYPE_CONTAINER) {
            if (list[0].collection) {
                UIHelper.goToCollection(this.router, list[0]);
            } else {
                this.openDirectory(list[0].ref.id);
            }
        } else {
            /*
            this.nodeDisplayed = event;
            this.nodeDisplayedVersion = event.version;
            */
            this.storage.set(
                TemporaryStorageService.NODE_RENDER_PARAMETER_DATA_SOURCE,
                this.dataSource,
            );
            this.router.navigate(
                [
                    UIConstants.ROUTER_PREFIX + 'render',
                    list[0].ref.id,
                    list[0].version ? list[0].version : '',
                ],
                {
                    state: {
                        scope: 'workspace',
                    },
                },
            );
        }
    }

    // returns either the passed node as list, or the current selection if the passed node is invalid (actionbar)
    private getNodeList(node: Node): Node[] {
        if (Array.isArray(node)) {
            return node;
        }
        let nodes = [node];
        if (node == null) {
            nodes = this.explorer.nodeEntries.getSelection().selected;
        }
        return nodes;
    }

    private loadFolders(user: IamUser) {
        for (const folder of user.person.sharedFolders) {
            this.node
                .getNodeMetadata(folder.id)
                .subscribe((node: NodeWrapper) => this.sharedFolders.push(node.node));
        }
    }

    setRoot(root: NodeRoot) {
        this.root = root;
        this.searchQuery = null;
        this.routeTo(root, null, null);
        this.actionbarRef.invalidate();
    }

    setSelection(nodes: Node[]) {
        this.explorer.nodeEntries.getSelection().clear();
        this.explorer.nodeEntries.getSelection().select(...nodes);
        this.setFixMobileNav();
    }

    private setFixMobileNav() {
        this.mainNavService
            .getMainNav()
            .setFixMobileElements(this.explorer.nodeEntries.getSelection().selected?.length > 0);
    }

    private updateLicense() {
        this.closeMetadata();
    }

    private closeMetadata() {
        this.mainNavService.getDialogs().closeSidebar();
    }

    private openDirectory(id: string, { replaceUrl = false } = {}) {
        this.routeTo(this.root, id, null, { replaceUrl });
    }

    searchGlobal(query: string) {
        this.routeTo(this.root, null, query);
    }

    private openDirectoryFromRoute(params: any = null) {
        let id = params?.id;
        this.closeMetadata();
        if (!id) {
            this.path = [];
            id = this.getRootFolderInternalId();
            if (this.root === 'RECYCLE') {
                this.createAllowed = false;
                // GlobalContainerComponent.finishPreloading();
                // return;
            }
        } else {
            this.selectedNodeTree = id;
            this.node.getNodeParents(id, false, [RestConstants.ALL]).subscribe(
                (data: NodeList) => {
                    if (this.root === 'RECYCLE') {
                        this.path = [];
                        this.createAllowed = false;
                    } else {
                        this.path = data.nodes.reverse();
                    }
                    this.selectedNodeTree = null;
                },
                (error: any) => {
                    this.selectedNodeTree = null;
                    this.path = [];
                },
            );
        }
        this.currentFolder = null;
        this.allowBinary = true;
        const root = !id || WorkspaceMainComponent.VALID_ROOTS_NODES.indexOf(id) !== -1;
        if (!root) {
            this.isRootFolder = false;
            this.node.getNodeMetadata(id).subscribe(
                (data: NodeWrapper) => {
                    this.mds
                        .getSet(
                            data.node.metadataset ? data.node.metadataset : RestConstants.DEFAULT,
                        )
                        .subscribe((mds: any) => {
                            if (mds.create) {
                                this.allowBinary = !mds.create.onlyMetadata;
                                if (!this.allowBinary) {
                                }
                            }
                        });
                    this.updateNodeByParams(params, data.node);
                    this.createAllowed =
                        !this.searchQuery &&
                        this.nodeHelper.getNodesRight(
                            [data.node],
                            RestConstants.ACCESS_ADD_CHILDREN,
                        )
                            ? true
                            : 'EMIT_EVENT';
                    this.notAllowedReason = 'WORKSPACE.CREATE_REASON.PERMISSIONS';
                    this.recoverScrollposition();
                },
                (error: any) => {
                    this.updateNodeByParams(params, { ref: { id } });
                },
            );
        } else {
            this.isRootFolder = true;
            if (
                id === '-my_shared_files-' ||
                id === '-to_me_shared_files_personal-' ||
                id === '-to_me_shared_files-'
            ) {
                this.isRootFolder = false;
            }
            if (id === RestConstants.USERHOME) {
                this.createAllowed = true;
            } else if (this.root === 'RECYCLE') {
                this.createAllowed = false;
            } else {
                this.createAllowed = 'EMIT_EVENT';
                this.notAllowedReason = 'WORKSPACE.CREATE_REASON.VIRTUAL';
            }
            const node: Node | any = {
                ref: {
                    id,
                },
                name: this.translate.instant('WORKSPACE.' + this.root),
            };
            if (this.root === 'MY_FILES') {
                node.access = [RestConstants.ACCESS_ADD_CHILDREN];
            }
            this.updateNodeByParams(params, node);
        }
    }

    openNode(node: Node, useConnector = true) {
        if (this.nodeHelper.isSavedSearchObject(node)) {
            UIHelper.routeToSearchNode(this.router, null, node);
        } else if (RestToolService.isLtiObject(node)) {
            this.toolService.openLtiObject(node);
        } else if (useConnector && this.connectors.connectorSupportsEdit(node)) {
            this.editConnector(node);
        } else {
            this.displayNode(node);
        }
    }

    openBreadcrumb(position: number) {
        console.log(position);
        this.searchQuery = null;
        if (position > 0) {
            // handled automatically via routing
        } else {
            // TODO: handle with homeRouterLink if possible.
            if (
                UIHelper.evaluateMediaQuery(
                    UIConstants.MEDIA_QUERY_MAX_WIDTH,
                    UIConstants.MOBILE_TAB_SWITCH_WIDTH,
                )
            ) {
                this.showSelectRoot = true;
            } else {
                this.routeTo(this.root);
            }
        }
    }

    private refresh(refreshPath = true, nodes: Node[] = null) {
        // only refresh properties in this case
        if (nodes && nodes.length) {
            this.updateNodes(nodes);
            return;
        }
        const search = this.searchQuery;
        const folder = this.currentFolder;
        this.currentFolder = null;
        this.searchQuery = null;
        const path = this.path;
        if (refreshPath) {
            this.path = [];
        }
        setTimeout(() => {
            this.path = path;
            this.currentFolder = folder;
            this.searchQuery = search;
        });
    }

    private async routeTo(
        root: string,
        node: string = null,
        search: string = null,
        { replaceUrl = false } = {},
    ) {
        const params = await UIHelper.getCommonParameters(this.route).toPromise();
        params.root = root;
        params.id = node;
        params.query = search;
        params.mainnav = this.mainnav;
        // tslint:disable-next-line:triple-equals
        if (this.displayType !== null) {
            params.displayType = this.displayType;
        }
        void this.router
            .navigate(['./'], { queryParams: params, relativeTo: this.route, replaceUrl })
            .then((result: boolean) => {
                if (!result) {
                    this.refresh(false);
                }
            });
    }

    private showAlpha() {
        this.toast.showModalDialog(
            'WORKSPACE.ALPHA_TITLE',
            'WORKSPACE.ALPHA_MESSAGE',
            DialogButton.getOk(() => this.hideDialog()),
            false,
        );
    }

    private addToCollection(node: Node) {
        const nodes = this.getNodeList(node);
        this.addNodesToCollection = nodes;
    }
    private addToStream(node: Node) {
        const nodes = this.getNodeList(node);
        this.addNodesStream = nodes;
    }
    private createVariant(node: Node) {
        const nodes = this.getNodeList(node);
        this.variantNode = nodes[0];
    }

    private goToLogin() {
        RestHelper.goToLogin(this.router, this.config, this.isSafe ? RestConstants.SAFE_SCOPE : '');
    }

    getRootFolderId() {
        if (this.root === 'MY_FILES') {
            return RestConstants.USERHOME;
        }
        if (this.root === 'SHARED_FILES') {
            return RestConstants.SHARED_FILES;
        }
        if (this.root === 'MY_SHARED_FILES') {
            return RestConstants.MY_SHARED_FILES;
        }
        if (this.root === 'TO_ME_SHARED_FILES') {
            return RestConstants.TO_ME_SHARED_FILES;
        }
        if (this.root === 'WORKFLOW_RECEIVE') {
            return RestConstants.WORKFLOW_RECEIVE;
        }
        return '';
    }

    getRootFolderInternalId() {
        if (this.root === 'TO_ME_SHARED_FILES') {
            if (this.toMeSharedToggle) {
                return RestConstants.TO_ME_SHARED_FILES;
            } else {
                return RestConstants.TO_ME_SHARED_FILES_PERSONAL;
            }
        }
        return this.getRootFolderId();
    }

    listLTI() {
        this.showLtiTools = true;
    }

    private recoverScrollposition() {
        window.scrollTo(0, this.storage.get('workspace_scroll', 0));
    }

    private applyNode(node: Node, force = false) {
        /*if(node.isDirectory && !force){
            this.dialogTitle='WORKSPACE.APPLY_NODE.DIRECTORY_TITLE';
            this.dialogCancelable=true;
            this.dialogMessage='WORKSPACE.APPLY_NODE.DIRECTORY_MESSAGE';
            this.dialogMessageParameters={name:node.name};
            this.dialogButtons=DialogButton.getYesNo(()=>{
                this.dialogTitle=null;
            },()=>{
                this.dialogTitle=null;
                this.applyNode(node,true);
            });
            return;
        }*/
        this.nodeHelper.addNodeToLms(node, this.reurl);
    }

    private updateNodeByParams(params: any, node: Node | any) {
        if (!this.loadingTask.isDone) {
            this.loadingTask.done();
        }
        if (params?.query) {
            this.doSearchFromRoute(params, node);
        } else {
            this.searchQuery = null;
            this.currentFolder = node;
            this.event.broadcastEvent(
                FrameEventsService.EVENT_NODE_FOLDER_OPENED,
                this.currentFolder,
            );
        }
    }

    private canPasteInCurrentLocation() {
        const clip = this.storage.get('workspace_clipboard') as ClipboardObject;
        return (
            this.currentFolder &&
            !this.searchQuery &&
            clip &&
            (!clip.sourceNode ||
                clip.sourceNode.ref.id !== this.currentFolder.ref.id ||
                clip.copy) &&
            this.createAllowed
        );
    }

    private updateNodes(nodes: Node[]) {
        for (let node of this.dataSource.getData()) {
            const hit = nodes.filter((n) => n.ref.id === node.ref.id);
            if (hit && hit.length === 1) {
                Helper.copyObjectProperties(node, hit[0]);
            }
        }
    }

    async prepareActionbar() {
        this.toMeSharedToggle = await this.session
            .get('toMeSharedGroup', this.config.instant('workspaceSharedToMeDefaultAll', false))
            .toPromise();
        const toggle = new OptionItem(
            'OPTIONS.TOGGLE_SHARED_TO_ME',
            this.toMeSharedToggle ? 'edu-content_shared_me_all' : 'edu-content_shared_me_private',
            () => {
                this.toMeSharedToggle = !this.toMeSharedToggle;
                toggle.icon = this.toMeSharedToggle
                    ? 'edu-content_shared_me_all'
                    : 'edu-content_shared_me_private';
                this.session.set('toMeSharedGroup', this.toMeSharedToggle);
                this.openDirectoryFromRoute();
                //this.treeComponent.reload = Boolean(true);
                this.toast.toast(
                    'WORKSPACE.TOAST.TO_ME_SHARED_' + (this.toMeSharedToggle ? 'ALL' : 'PERSONAL'),
                );
            },
        );
        toggle.isToggle = true;
        toggle.group = DefaultGroups.Toggles;
        toggle.elementType = [ElementType.Unknown];
        toggle.priority = 5;
        toggle.customShowCallback = () => {
            return this.root === 'TO_ME_SHARED_FILES';
        };
        this.customOptions.addOptions = [toggle];
    }

    private getLastLocationStorageId() {
        return (
            TemporaryStorageService.WORKSPACE_LAST_LOCATION +
            (this.isSafe ? RestConstants.SAFE_SCOPE : '')
        );
    }

    setDisplayType(displayType: NodeEntriesDisplayType, refreshRoute = true) {
        this.displayType = displayType;
        if (refreshRoute) {
            void this.router.navigate(['./'], {
                relativeTo: this.route,
                replaceUrl: true,
                queryParamsHandling: 'merge',
                queryParams: {
                    [UIConstants.QUERY_PARAM_LIST_VIEW_TYPE]: displayType,
                },
            });
        }
    }

    async createNotAllowed() {
        const message =
            (await this.translate.get(this.notAllowedReason).toPromise()) +
            '\n\n' +
            (await this.translate.get('WORKSPACE.CREATE_REASON.GENERAL').toPromise());
        this.toast.showConfigurableDialog({
            title: 'WORKSPACE.CREATE_REASON.TITLE',
            message,
            isCancelable: true,
            buttons: [
                new DialogButton(
                    'WORKSPACE.GO_TO_HOME',
                    { color: 'primary', position: 'opposite' },
                    () => {
                        this.openDirectory(RestConstants.USERHOME);
                        this.toast.closeModalDialog();
                    },
                ),
                new DialogButton('CLOSE', { color: 'standard' }, () =>
                    this.toast.closeModalDialog(),
                ),
            ],
        });
    }

    onDeleteNodes(nodes: Node[]): void {
        this.mainNavService.getDialogs().nodeDelete = nodes;
    }
}

function notNull<T>(value?: T): boolean {
    return value !== undefined && value !== null;
}
