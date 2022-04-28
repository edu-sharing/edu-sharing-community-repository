import {Component, HostListener, NgZone, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import { TranslationsService } from '../../translations/translations.service';
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
    RestCollectionService,
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
    UIService
} from '../../core-module/core.module';
import {ActivatedRoute, Params, Router} from '@angular/router';
import {
    CustomOptions,
    DefaultGroups,
    ElementType,
    OptionItem
} from '../../core-ui-module/option-item';
import {Toast} from '../../core-ui-module/toast';
import {UIAnimation} from '../../core-module/ui/ui-animation';
import {NodeHelperService} from '../../core-ui-module/node-helper.service';
import {KeyEvents} from '../../core-module/ui/key-events';
import {UIHelper} from '../../core-ui-module/ui-helper';
import {trigger} from '@angular/animations';
import {UIConstants} from '../../core-module/ui/ui-constants';
import {ActionbarHelperService} from '../../common/services/actionbar-helper';
import {Helper} from '../../core-module/rest/helper';
import {CordovaService} from '../../common/services/cordova.service';
import {HttpClient} from '@angular/common/http';
import {MainNavComponent} from '../../common/ui/main-nav/main-nav.component';
import {ActionbarComponent} from '../../common/ui/actionbar/actionbar.component';
import {BridgeService} from '../../core-bridge-module/bridge.service';
import {WorkspaceExplorerComponent} from './explorer/explorer.component';
import {CardService} from '../../core-ui-module/card.service';
import {Observable, Subject} from 'rxjs';
import {delay} from 'rxjs/operators';
import {ListTableComponent} from '../../core-ui-module/components/list-table/list-table.component';
import {SkipTarget} from '../../common/ui/skip-nav/skip-nav.service';
import {DragNodeTarget} from '../../core-ui-module/directives/drag-nodes/drag-nodes';
import {NodeDataSource} from '../../core-ui-module/components/node-entries-wrapper/node-data-source';
import {
    DropSource, DropTarget, NodeEntriesDisplayType,
    NodeRoot
} from '../../core-ui-module/components/node-entries-wrapper/entries-model';
import { LoadingScreenService } from '../../main/loading-screen/loading-screen.service';

@Component({
    selector: 'es-workspace-main',
    templateUrl: 'workspace.component.html',
    styleUrls: ['workspace.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('fadeFast', UIAnimation.fade(UIAnimation.ANIMATION_TIME_FAST)),
        trigger('overlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST)),
        trigger('fromLeft', UIAnimation.fromLeft()),
        trigger('fromRight', UIAnimation.fromRight())
    ]
})
export class WorkspaceMainComponent implements EventListener, OnInit, OnDestroy {
    @ViewChild('explorer') explorer: WorkspaceExplorerComponent;
    @ViewChild('actionbar') actionbarRef: ActionbarComponent;
    private static VALID_ROOTS = ['MY_FILES', 'SHARED_FILES', 'MY_SHARED_FILES', 'TO_ME_SHARED_FILES', 'WORKFLOW_RECEIVE', 'RECYCLE'];
    private static VALID_ROOTS_NODES = [RestConstants.USERHOME, '-shared_files-', '-my_shared_files-', '-to_me_shared_files_personal-', '-to_me_shared_files-', '-workflow_receive-'];


    cardHasOpenModals$: Observable<boolean>;
    private isRootFolder: boolean;
    private sharedFolders: Node[] = [];
    path: Node[] = [];
    private parameterNode: Node;
    root: NodeRoot = 'MY_FILES';

    showSelectRoot = false;

    public allowBinary = true;
    public globalProgress = false;
    public editNodeDeleteOnCancel = false;
    private createMds: string;
    private nodeDisplayedVersion: string;
    createAllowed: boolean | 'EMIT_EVENT';
    notAllowedReason: string;
    currentFolder: Node;
    user: IamUser;
    public searchQuery: any;
    public isSafe = false;
    isLoggedIn = false;
    public addNodesToCollection: Node[];
    public addNodesStream: Node[];
    public variantNode: Node;
    @ViewChild('mainNav') mainNavRef: MainNavComponent;
    private connectorList: Connector[];
    private currentNode: Node;
    public mainnav = true;
    private timeout: string;
    private timeIsValid = false;
    private viewToggle: OptionItem;
    private isAdmin = false;
    public isBlocked = false;
    private isGuest: boolean;

    customOptions: CustomOptions = {
        useDefaultOptions: true,
    };

    toMeSharedToggle: boolean;

    dataSource = new NodeDataSource<Node>();
    private appleCmd = false;
    private reurl: string;
    private mdsParentNode: Node;
    public showLtiTools = false;
    private oldParams: Params;
    selectedNodeTree: string;
    public contributorNode: Node;
    public shareLinkNode: Node;
    displayType: NodeEntriesDisplayType  = null;
    private reurlDirectories: boolean;
    reorderDialog: boolean;
    private readonly destroyed$ = new Subject<void>();
    private loadingTask = this.loadingScreen.addLoadingTask();
    @HostListener('window:beforeunload', ['$event'])
    beforeunloadHandler(event: any) {
        if (this.isSafe) {
            this.connector.logout().toPromise();
        }
    }
    private handleScroll(event: Event) {
        const scroll = (window.pageYOffset || document.documentElement.scrollTop);
        if (scroll > 0) {
            this.storage.set('workspace_scroll', scroll);
        }
    }
    @HostListener('document:keyup', ['$event'])
    handleKeyboardEventUp(event: KeyboardEvent) {
        if (event.keyCode === 91 || event.keyCode === 93) {
            this.appleCmd = false;
        }
    }
    @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent) {
        if (event.keyCode === 91 || event.keyCode === 93) {
            this.appleCmd = true;
            event.preventDefault();
            event.stopPropagation();
            return;
        }
        const clip = (this.storage.get('workspace_clipboard') as ClipboardObject);
        const fromInputField = KeyEvents.eventFromInputField(event);

    }
    onEvent(event: string, data: any): void {
        if (event === FrameEventsService.EVENT_REFRESH) {
            this.refresh();
        }
    }

    ngOnInit(): void {
        this.registerScroll();
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
        this.storage.remove('workspace_clipboard');
        if(this.currentFolder) {
            this.storage.set(this.getLastLocationStorageId(), this.currentFolder.ref.id);
        }
        // close sidebar, if open
        this.mainNavRef.management.closeSidebar();
    }
    constructor(
        private toast: Toast,
        private bridge: BridgeService,
        private route: ActivatedRoute,
        private router: Router,
        private http: HttpClient,
        private nodeHelper: NodeHelperService,
        private translate: TranslateService,
        private translations: TranslationsService,
        private storage: TemporaryStorageService,
        private config: ConfigurationService,
        private connectors: RestConnectorsService,
        private actionbar: ActionbarHelperService,
        private collectionApi: RestCollectionService,
        private toolService: RestToolService,
        private session: SessionStorageService,
        private iam: RestIamService,
        private mds: RestMdsService,
        private node: RestNodeService,
        private ui: UIService,
        private event: FrameEventsService,
        private connector: RestConnectorService,
        private cordova: CordovaService,
        private card: CardService,
        private ngZone: NgZone,
        private loadingScreen: LoadingScreenService,
    ) {
        this.event.addListener(this);
        this.translations.waitForInit().subscribe(() => {
            this.initialize();
        });
        this.connector.setRoute(this.route);
        this.globalProgress = true;
        this.cardHasOpenModals$ = card.hasOpenModals.pipe(delay(0));
    }

    private registerScroll(): void {
        this.ngZone.runOutsideAngular(() => {
            const handleScroll = (event: Event) => this.handleScroll(event);
            window.addEventListener('scroll', handleScroll);
            this.destroyed$.subscribe(() => window.removeEventListener('scroll', handleScroll));
        })
    }

    private hideDialog(): void {
        this.toast.closeModalDialog();
    }

    private editConnector(node: Node = null, type: Filetype = null, win: any = null, connectorType: Connector = null) {
        UIHelper.openConnector(this.connectors, this.iam, this.event, this.toast, this.getNodeList(node)[0], type, win, connectorType);
    }
    handleDrop(event: {target: DropTarget, source: DropSource<Node>}) {
        for (const s of event.source.element) {
            if ((event.target as Node).ref?.id === s.ref.id || (event.target as Node).ref?.id === s.parent.id) {
                this.toast.error(null, 'WORKSPACE.SOURCE_TARGET_IDENTICAL');
                return;
            }
        }
        if (event.target !== 'MY_FILES' && !(event.target as Node).isDirectory) {
            this.toast.error(null, 'WORKSPACE.TARGET_NO_DIRECTORY');
            return;
        }
        if (event.source.mode === 'link') {
            this.toast.error(null, 'WORKSPACE.FEATURE_NOT_IMPLEMENTED');
        }
        else if (event.source.mode === 'copy') {
            this.copyNode(event.target, event.source.element);
        }
        else {
            this.moveNode(event.target, event.source.element);
        }
        /*
        this.dialogTitle="WORKSPACE.DRAG_DROP_TITLE";
        this.dialogCancelable=true;
        this.dialogMessage="WORKSPACE.DRAG_DROP_MESSAGE";
        this.dialogMessageParameters={source:event.source.name,target:event.target.name};
        this.dialogButtons=[
          new DialogButton("WORKSPACE.DRAG_DROP_COPY",DialogButton.TYPE_PRIMARY,()=>this.copyNode(event.target,event.source)),
          new DialogButton("WORKSPACE.DRAG_DROP_MOVE",DialogButton.TYPE_PRIMARY,()=>this.moveNode(event.target,event.source)),
        ]
        */
    }
    canDropBreadcrumbs = (event: any) => {
        return event.target === 'HOME' ? this.root === 'MY_FILES' :
            event.target?.ref?.id !== this.currentFolder.ref.id;
    };
    private moveNode(target: DropTarget, source: Node[], position = 0) {
        this.globalProgress = true;
        if (position >= source.length) {
            this.finishMoveCopy(target, source, false);
            this.globalProgress = false;
            return;
        }
        this.node.moveNode((target as Node).ref?.id || RestConstants.USERHOME, source[position].ref.id).subscribe((data: NodeWrapper) => {
                this.moveNode(target, source, position + 1);
            },
            (error: any) => {
                this.nodeHelper.handleNodeError(source[position].name, error);
                source.splice(position, 1);
                this.moveNode(target, source, position + 1);
            });
    }
    private copyNode(target: DropTarget, source: Node[], position = 0) {
        this.globalProgress = true;
        if (position >= source.length) {
            this.finishMoveCopy(target, source, true);
            this.globalProgress = false;
            return;
        }
        this.node.copyNode((target as Node).ref?.id || RestConstants.USERHOME, source[position].ref.id).subscribe((data: NodeWrapper) => {
                this.copyNode(target, source, position + 1);
            },
            (error: any) => {
                this.nodeHelper.handleNodeError(source[position].name, error);
                source.splice(position, 1);
                this.copyNode(target, source, position + 1);
            });
    }
    private finishMoveCopy(target: DropTarget, source: Node[], copy: boolean) {
        this.toast.closeModalDialog();
        const info: any = {
            to: (target as Node).name || this.translate.instant('WORKSPACE.MY_FILES'),
            count: source.length,
            mode: this.translate.instant('WORKSPACE.' + (copy ? 'PASTE_COPY' : 'PASTE_MOVE'))
        };
        if (source.length) {
            this.toast.toast('WORKSPACE.TOAST.PASTE_DRAG', info);
        }
        this.globalProgress = false;
        this.refresh();
    }
    private async initialize() {
        this.user = await (this.iam.getCurrentUserAsync());
        this.route.params.subscribe(async (routeParams: Params) => {
            this.isSafe = routeParams.mode === 'safe';
            const login = await this.connector.isLoggedIn().toPromise();
            if (login.statusCode !== RestConstants.STATUS_CODE_OK) {
                RestHelper.goToLogin(this.router, this.config);
                return;
            }
            await this.prepareActionbar();
            this.loadFolders(this.user);

            let valid = true;
            this.isGuest = login.isGuest;
            if (!login.isValidLogin || login.isGuest) {
                valid = false;
            }
            this.isBlocked = !this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_WORKSPACE);
            this.isAdmin = login.isAdmin;
            if (this.isSafe && login.currentScope !== RestConstants.SAFE_SCOPE) {
                valid = false;
            }
            if (!this.isSafe && login.currentScope != null) {
                this.connector.logout().subscribe(() => {
                    this.goToLogin();
                }, (error: any) => {
                    this.toast.error(error);
                    this.goToLogin();
                })
                return;
            }
            if (!valid) {
                this.goToLogin();
                return;
            }
            this.connector.scope = this.isSafe ? RestConstants.SAFE_SCOPE : null;
            this.isLoggedIn = true;
            try {
                this.connectorList = (await this.connectors.list().toPromise()).connectors;
            } catch (e) {
                console.warn('no connectors found: ' + e.toString());
            }
            this.globalProgress = false;
            this.route.queryParams.subscribe((params: Params) => {
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
                this.oldParams = params;
                if (params.displayType != null) {
                    this.setDisplayType(parseInt(params[UIConstants.QUERY_PARAM_LIST_VIEW_TYPE], 10), false);
                } else {
                    this.setDisplayType(this.config.instant('workspaceViewType', NodeEntriesDisplayType.Table) as NodeEntriesDisplayType, false);
                }
                if (params.root && WorkspaceMainComponent.VALID_ROOTS.indexOf(params.root) !== -1) {
                    this.root = params.root;
                } else {
                    this.root = 'MY_FILES';
                }
                if (params.reurl) {
                    this.reurl = params.reurl;
                }
                this.reurlDirectories = params.applyDirectories === 'true';
                this.mainnav = params.mainnav === 'false' ? false : true;

                if (params.file) {
                    this.node.getNodeMetadata(params.file, [RestConstants.ALL]).subscribe((paramNode) => {
                        this.setSelection([paramNode.node]);
                        this.parameterNode = paramNode.node;
                        this.mainNavRef.management.nodeSidebar = paramNode.node;
                    });
                }

                if (!needsUpdate) {
                    return;
                }
                this.createAllowed = this.root === 'MY_FILES';
                let lastLocation = this.storage.pop(this.getLastLocationStorageId(), null);
                if(this.isSafe) {
                    // clear lastLocation, this is another folder than the safe
                    lastLocation = null;
                }
                if (!params.id && !params.query && lastLocation) {
                    this.openDirectory(lastLocation);
                } else {
                    this.openDirectoryFromRoute(params);
                }
                if (params.showAlpha) {
                    this.showAlpha();
                }
            });
        });
    }
    public resetWorkspace() {
        if (this.mainNavRef.management.nodeSidebar && this.parameterNode) {
            this.setSelection([this.parameterNode]);
        }
    }

    public doSearch(query: any) {
        const id = this.currentFolder ? this.currentFolder.ref.id :
            this.searchQuery && this.searchQuery.node ? this.searchQuery.node.ref.id : null;
        this.routeTo(this.root, id, query.query);
        if (!query.cleared) {
            this.ui.hideKeyboardIfMobile();
        }
    }
    private doSearchFromRoute(params: any, node: Node | any) {
        node = this.isRootFolder ? null : node;
        this.searchQuery = {
            query: params.query,
            node
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
            if(list[0].collection) {
                UIHelper.goToCollection(this.router,list[0]);
            } else {
                this.openDirectory(list[0].ref.id);
            }
        }
        else {
            /*
            this.nodeDisplayed = event;
            this.nodeDisplayedVersion = event.version;
            */
            this.storage.set(TemporaryStorageService.NODE_RENDER_PARAMETER_DATA_SOURCE, this.dataSource);
            this.currentNode = list[0];
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'render', list[0].ref.id, list[0].version ? list[0].version : ''],
                {
                    state: {
                        scope: 'workspace'
                    }
                }
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
            this.node.getNodeMetadata(folder.id).subscribe((node: NodeWrapper) => this.sharedFolders.push(node.node));
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
        this.mainNavRef.setFixMobileElements(this.explorer.nodeEntries.getSelection().selected?.length > 0);
    }
    private updateLicense() {
        this.closeMetadata();
    }
    private closeMetadata() {
        this.mainNavRef.management.closeSidebar();
    }
    private openDirectory(id: string) {
        this.routeTo(this.root, id);
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
        }
        else {
            this.selectedNodeTree = id;
            this.node.getNodeParents(id, false, [RestConstants.ALL]).subscribe((data: NodeList) => {
                if (this.root === 'RECYCLE') {
                    this.path = [];
                    this.createAllowed = false;
                }
                else {
                    this.path = data.nodes.reverse();
                }
                this.selectedNodeTree = null;
            }, (error: any) => {
                this.selectedNodeTree = null;
                this.path = [];
            });
        }
        this.currentFolder = null;
        this.allowBinary = true;
        const root = !id || WorkspaceMainComponent.VALID_ROOTS_NODES.indexOf(id) !== -1;
        if (!root) {
            this.isRootFolder = false;
            this.node.getNodeMetadata(id).subscribe((data: NodeWrapper) => {
                this.mds.getSet(data.node.metadataset ? data.node.metadataset : RestConstants.DEFAULT).subscribe((mds: any) => {
                    if (mds.create) {
                        this.allowBinary = !mds.create.onlyMetadata;
                        if (!this.allowBinary) {
                        }
                    }
                });
                this.updateNodeByParams(params, data.node);
                this.createAllowed = !this.searchQuery && this.nodeHelper.getNodesRight([data.node], RestConstants.ACCESS_ADD_CHILDREN) ? true : 'EMIT_EVENT';
                this.notAllowedReason = 'WORKSPACE.CREATE_REASON.PERMISSIONS';
                this.recoverScrollposition();
            }, (error: any) => {
                this.updateNodeByParams(params, { ref: { id } });
            });
        }
        else {
            this.isRootFolder = true;
            if(id === '-my_shared_files-'
                || id === '-to_me_shared_files_personal-'
                || id === '-to_me_shared_files-') {
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
            const node: Node|any = {
                ref: {
                    id
                },
                name: this.translate.instant('WORKSPACE.' + this.root)
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
        }
        else if (RestToolService.isLtiObject(node)) {
            this.toolService.openLtiObject(node);
        }
        else if (useConnector && this.connectors.connectorSupportsEdit(node)) {
            this.editConnector(node);
        }
        else {
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
                    UIConstants.MOBILE_TAB_SWITCH_WIDTH
                )
            ) {
                this.showSelectRoot = true;
            } else {
                this.routeTo(this.root);
            }
        }
    }

    private refresh(refreshPath = true,nodes: Node[] = null) {
        // only refresh properties in this case
        if(nodes && nodes.length) {
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

    private async routeTo(root: string, node: string = null, search: string = null) {
        const params = await UIHelper.getCommonParameters(this.route).toPromise();
        params.root = root;
        params.id = node;
        params.query = search;
        params.mainnav = this.mainnav;
        // tslint:disable-next-line:triple-equals
        if (this.displayType !== null) {
            params.displayType = this.displayType;
        }
        this.router.navigate(['./'], {queryParams: params, relativeTo: this.route})
            .then((result: boolean) => {
                if (!result) {
                    this.refresh(false);
                }
            });
    }

    private showAlpha() {
        this.toast.showModalDialog('WORKSPACE.ALPHA_TITLE',
            'WORKSPACE.ALPHA_MESSAGE',
            DialogButton.getOk(() => this.hideDialog()),
            false
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

    getRootFolderInternalId(){
        if(this.root === 'TO_ME_SHARED_FILES') {
            if (this.toMeSharedToggle) {
                return RestConstants.TO_ME_SHARED_FILES;
            } else {
                return RestConstants.TO_ME_SHARED_FILES_PERSONAL;
            }
        }
        return this.getRootFolderId();
    }

    public listLTI() {
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
        }
        else {
            this.searchQuery = null;
            this.currentFolder = node;
            this.event.broadcastEvent(FrameEventsService.EVENT_NODE_FOLDER_OPENED, this.currentFolder);
        }
    }

    private canPasteInCurrentLocation() {
        const clip = (this.storage.get('workspace_clipboard') as ClipboardObject);
        return this.currentFolder
            && !this.searchQuery
            && clip
            && ((!clip.sourceNode || clip.sourceNode.ref.id !== this.currentFolder.ref.id) || clip.copy)
            && this.createAllowed;
    }

    private updateNodes(nodes: Node[]) {
        for(let node of this.dataSource.getData()){
            const hit = nodes.filter((n) => n.ref.id === node.ref.id);
            if (hit && hit.length === 1) {
                Helper.copyObjectProperties(node, hit[0]);
            }
        }
    }

    async prepareActionbar() {
        this.toMeSharedToggle = await this.session.get('toMeSharedGroup',
            this.config.instant('workspaceSharedToMeDefaultAll', false)
            ).toPromise();
        const toggle = new OptionItem('OPTIONS.TOGGLE_SHARED_TO_ME',
            this.toMeSharedToggle ? 'edu-content_shared_me_all' : 'edu-content_shared_me_private',
            () => {
                this.toMeSharedToggle = !this.toMeSharedToggle;
                toggle.icon = this.toMeSharedToggle ? 'edu-content_shared_me_all' : 'edu-content_shared_me_private';
                this.session.set('toMeSharedGroup', this.toMeSharedToggle);
                this.openDirectoryFromRoute();
                //this.treeComponent.reload = Boolean(true);
                this.toast.toast('WORKSPACE.TOAST.TO_ME_SHARED_' + (this.toMeSharedToggle ? 'ALL' : 'PERSONAL'));
            });
        toggle.isToggle = true;
        toggle.group = DefaultGroups.Toggles;
        toggle.elementType = [ElementType.Unknown];
        toggle.priority = 5;
        toggle.customShowCallback = () => {
            return this.root === 'TO_ME_SHARED_FILES';
        }
        this.customOptions.addOptions = [toggle];
    }

    private getLastLocationStorageId() {
        return TemporaryStorageService.WORKSPACE_LAST_LOCATION + (this.isSafe ? RestConstants.SAFE_SCOPE : '');
    }

    setDisplayType(displayType: NodeEntriesDisplayType, refreshRoute = true) {
        this.displayType = displayType;
        if(refreshRoute) {
            this.router.navigate(['./'], {
                relativeTo: this.route,
                replaceUrl: true,
                queryParamsHandling: 'merge',
                queryParams: {
                    [UIConstants.QUERY_PARAM_LIST_VIEW_TYPE]: displayType
                }
            });
        }
    }

    async createNotAllowed() {
        const message = (await this.translate.get(this.notAllowedReason).toPromise()) + '\n\n' +
                (await this.translate.get('WORKSPACE.CREATE_REASON.GENERAL').toPromise());
            this.toast.showConfigurableDialog({
                title: 'WORKSPACE.CREATE_REASON.TITLE',
                message,
                isCancelable: true,
                buttons: [
                    new DialogButton('WORKSPACE.GO_TO_HOME', DialogButton.TYPE_PRIMARY + DialogButton.TYPE_SECONDARY, () => {
                        this.openDirectory(RestConstants.USERHOME);
                        this.toast.closeModalDialog();
                    }),
                    new DialogButton('CLOSE', DialogButton.TYPE_CANCEL, () => this.toast.closeModalDialog()),
                ]
            });
    }
}
