import {
    ColumnType,
    DateHelper,
    InteractionType,
    ListItem,
    NodeDataSource,
    NodeEntriesDisplayType,
    TranslationsService,
    UIConstants,
} from 'ngx-edu-sharing-ui';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { ActivatedRoute, Router } from '@angular/router';
import { Toast } from '../../services/toast';
import {
    Connector,
    ConnectorList,
    FrameEventsService,
    ParentList,
    RestCollectionService,
    RestConnectorService,
    RestConnectorsService,
    RestConstants,
    RestHelper,
    RestNodeService,
    RestUtilitiesService,
} from '../../core-module/core.module';
import { DomSanitizer } from '@angular/platform-browser';
import { TranslateService } from '@ngx-translate/core';
import {
    DEFAULT,
    HOME_REPOSITORY,
    Node,
    PROPERTY_FILTER_ALL,
    SearchService,
} from 'ngx-edu-sharing-api';
import { Component } from '@angular/core';
import { Helper } from '../../core-module/rest/helper';
import { CordovaService, OnBackBehaviour } from '../../services/cordova.service';
import { BridgeService } from '../../services/bridge.service';
import { NodeHelperService } from '../../services/node-helper.service';
import { MainNavService } from '../../main/navigation/main-nav.service';

@Component({
    selector: 'es-app-share-page',
    templateUrl: 'app-share-page.component.html',
    styleUrls: ['app-share-page.component.scss'],
    animations: [],
    standalone: false,
})
export class AppSharePageComponent {
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    globalProgress = true;
    uri: string;
    private type = 'LINK';
    title: string;
    description: string;
    previewUrl: any;
    private inboxPath: Node[];
    inbox: Node;
    columns: ColumnType;
    collections = new NodeDataSource<Node>();
    private cordovaType: string;
    private mimetype: string;
    private editorType: string;
    private file: File;
    private fileName: string;
    private text: string;
    constructor(
        private toast: Toast,
        private route: ActivatedRoute,
        private router: Router,
        private sanitizer: DomSanitizer,
        private node: RestNodeService,
        private connectors: RestConnectorsService,
        private events: FrameEventsService,
        private utilities: RestUtilitiesService,
        private translate: TranslateService,
        private translations: TranslationsService,
        private collectionApi: RestCollectionService,
        private cordova: CordovaService,
        private connector: RestConnectorService,
        private bridge: BridgeService,
        private nodeHelper: NodeHelperService,
        private mainNav: MainNavService,
        private searchService: SearchService,
    ) {
        this.mainNav.setMainNavConfig({ title: 'SHARE_APP.TITLE', currentScope: 'app-share' });
        // when the user finished sharing and navigates back he must return to the origin app
        this.cordova.setOnBackBehaviour(OnBackBehaviour.closeApp);
        this.columns = {
            Default: [
                new ListItem('COLLECTION', 'title'),
                new ListItem('COLLECTION', 'info'),
                new ListItem('COLLECTION', 'scope'),
            ],
        };
        if (this.cordova.isRunningCordova()) {
            this.cordova.subscribeServiceReady().subscribe(() => {
                this.init();
            });
        } else {
            this.init();
        }
    }
    getType() {
        if (this.isLink()) return 'link';
        if (this.isTextSnippet()) return 'file-txt';
        if (this.mimetype === 'application/pdf') {
            return 'file-pdf';
        }
        if (
            [
                'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
                'application/msword',
            ].includes(this.mimetype)
        ) {
            return 'file-word';
        }
        if (
            [
                'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
                'application/vnd.ms-excel',
            ].includes(this.mimetype)
        ) {
            return 'file-excel';
        }
        if (
            [
                'application/vnd.openxmlformats-officedocument.presentationml.presentation',
                'application/vnd.ms-powerpoint',
            ].includes(this.mimetype)
        ) {
            return 'file-powerpoint';
        }
        if (
            [
                'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
                'application/msword',
            ].includes(this.mimetype)
        ) {
            return 'file-word';
        }
        if (this.mimetype) {
            let type = this.mimetype.split('/')[0];
            if (this.translate.instant('MEDIATYPE.file-' + type) === 'MEDIATYPE.file-' + type) {
                type = null;
            }
            if (type != null) {
                return 'file-' + type;
            }
        }
        return 'file';
    }
    saveInternal(callback: Function) {
        this.globalProgress = true;
        if (this.isLink()) {
            let prop: any = {};
            prop[RestConstants.CCM_PROP_IO_WWWURL] = [this.getUri()];
            this.node
                .createNode(
                    this.inbox?.ref?.id || RestConstants.INBOX,
                    RestConstants.CCM_TYPE_IO,
                    [],
                    prop,
                    true,
                    RestConstants.COMMENT_MAIN_FILE_UPLOAD,
                )
                .subscribe(
                    (data) => {
                        callback(data.node);
                    },
                    (error) => {
                        this.toast.error(error);
                        this.globalProgress = false;
                    },
                );
        } else {
            let prop: any = RestHelper.createNameProperty(this.title);
            if (this.editorType) {
                prop[RestConstants.CCM_PROP_EDITOR_TYPE] = [this.editorType];
            }
            if (this.text && !this.isTextSnippet()) {
                prop[RestConstants.LOM_PROP_GENERAL_DESCRIPTION] = [this.text];
            }
            this.node
                .createNode(
                    this.inbox?.ref?.id || RestConstants.INBOX,
                    RestConstants.CCM_TYPE_IO,
                    [],
                    prop,
                    true,
                )
                .subscribe(
                    (data) => {
                        this.node
                            .uploadNodeContent(
                                data.node.ref.id,
                                this.file,
                                RestConstants.COMMENT_MAIN_FILE_UPLOAD,
                                this.mimetype,
                            )
                            .subscribe(
                                () => {
                                    callback(data.node);
                                },
                                (error) => {
                                    this.toast.error(error);
                                    this.globalProgress = false;
                                },
                            );
                    },
                    (error) => {
                        this.toast.error(error);
                        this.globalProgress = false;
                    },
                );
        }
    }
    saveFile() {
        this.saveInternal((node: Node) => {
            this.goToInbox(node);
            this.events.broadcastEvent(FrameEventsService.EVENT_SHARED, node);
        });
    }
    saveToCollection(collection: Node) {
        // the server takes a reference only into a collection the user may add children to
        if (!collection.access?.includes(RestConstants.ACCESS_ADD_CHILDREN)) {
            this.toast.error(null, 'COLLECTIONS.NO_ADD_PERMISSIONS');
            return;
        }
        this.saveInternal((node: Node) => {
            UIHelper.addToCollection(
                this.nodeHelper,
                this.collectionApi,
                this.router,
                this.bridge,
                collection,
                [node],
                false,
                (added) => {
                    this.events.broadcastEvent(FrameEventsService.EVENT_SHARED, node);
                    if (added.length > 0) {
                        UIHelper.goToCollection(this.router, collection, null, {
                            replaceUrl: true,
                        });
                    } else {
                        // the upload stays in the inbox when the collection does not take it
                        this.goToInbox(node);
                    }
                },
            );
        });
    }
    private isLink() {
        if (this.cordovaType == 'public.url')
            // ios
            return true;
        if (this.hasData()) return false;
        if (!this.uri) return false;
        if (this.uri.startsWith('content://') || this.uri.startsWith('file://')) return false;
        let pos = this.uri.indexOf('://');
        return pos > 0 && pos < 10;
    }
    private isTextSnippet() {
        if (this.cordovaType == 'public.text')
            // ios
            return true;
        if (this.hasData()) return false;
        if (this.isLink()) return false;
        if (!this.uri) return false;
        return !this.uri.startsWith('content://') && !this.uri.startsWith('file://');
    }
    /** Opens the inbox, or the folder the shared node was saved to while the inbox is not loaded. */
    private goToInbox(node: Node) {
        const folder = this.inbox?.ref?.id ?? node?.parent?.id;
        UIHelper.goToWorkspaceFolder(this.node, this.router, null, folder, {
            replaceUrl: true,
        });
    }
    private init() {
        this.translations.waitForInit().subscribe(() => {
            this.route.queryParams.subscribe((params: any) => {
                this.uri = params['uri'];
                this.mimetype = params['mimetype'];
                if (this.mimetype == 'public.image') {
                    // ios
                    this.mimetype = 'image/jpeg';
                } else if (!this.mimetype.includes('/')) {
                    // ios sends invalid mimetypes from certain apps
                    console.info('invalid mimetype from ios', this.mimetype);
                    this.mimetype = 'application/octet-stream';
                }
                this.cordovaType = params['mimetype'];
                this.fileName = params['file'];
                this.text = params['text']; // ios only: custom description
                this.description = null;
                this.collections.reset();
                this.collections.isLoading = true;
                this.searchService
                    .requestSearch({
                        repository: HOME_REPOSITORY,
                        metadataset: DEFAULT,
                        query: 'collections',
                        contentType: 'COLLECTIONS',
                        propertyFilter: [PROPERTY_FILTER_ALL],
                        sortProperties: [RestConstants.CM_MODIFIED_DATE],
                        sortAscending: [false],
                        skipCount: 0,
                        maxItems: 50,
                        // the index holds roles, Coordinator is not resolved into AddChildren
                        body: {
                            criteria: [],
                            permissions: [
                                RestConstants.PERMISSION_COORDINATOR,
                                RestConstants.PERMISSION_COLLABORATOR,
                            ],
                        },
                    })
                    .subscribe(
                        (data) => {
                            // sharing into a collection needs the right to add to it
                            const collections = data.nodes.filter((collection) =>
                                collection.access?.includes(RestConstants.ACCESS_ADD_CHILDREN),
                            );
                            this.collections.setData(collections, data.pagination);
                            this.collections.isLoading = false;
                        },
                        (error) => {
                            this.toast.error(error);
                            this.collections.isLoading = false;
                        },
                    );
                this.node.getNodeParents(RestConstants.INBOX, false).subscribe(
                    (data: ParentList) => {
                        this.inboxPath = data.nodes;
                        this.inbox = data.nodes[0];
                    },
                    // saving falls back to the inbox id, which the server resolves itself
                    () => {},
                );
                this.previewUrl = this.connector.getThemeMimePreview(this.getType() + '.svg');
                if (this.isLink()) {
                    this.utilities.getWebsiteInformation(this.getUri()).subscribe(
                        (data: any) => {
                            if (data.title) {
                                this.title = data.title + ' - ' + data.page;
                            } else {
                                this.title = this.getUri();
                            }
                            this.description = data.description;
                            this.globalProgress = false;
                        },
                        (error) => {
                            this.title = this.getUri();
                            this.globalProgress = false;
                        },
                    );
                } else if (this.isTextSnippet()) {
                    this.globalProgress = false;
                    this.connectors.list().subscribe(
                        (list: ConnectorList) => {
                            this.prepareTextSnippet(list.connectors);
                        },
                        (error: any) => {
                            this.prepareTextSnippet(null);
                        },
                    );
                } else {
                    if (
                        this.cordova.isRunningCordova() &&
                        this.cordova.getLastIntent() &&
                        this.cordova.getLastIntent().stream
                    ) {
                        let base64 = this.cordova.getLastIntent().stream;
                        if (this.mimetype.startsWith('image/')) {
                            this.previewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(
                                'data:' + this.mimetype + ';base64,' + base64,
                            );
                        }
                        this.file = Helper.base64toBlob(base64, this.mimetype) as any;
                        this.cordova.getFileAsBlob(this.getUri(), this.mimetype).subscribe(
                            (data: any) => {
                                this.globalProgress = false;
                                this.updateTitle();
                            },
                            (error: any) => {
                                console.warn(error);
                                this.globalProgress = false;
                                this.updateTitle();
                            },
                        );
                    } else {
                        void this.router.navigate([
                            UIConstants.ROUTER_PREFIX,
                            'messages',
                            'SHARING_ERROR',
                        ]);
                    }
                    /*
                    this.cordova.getFileAsBlob(this.getUri(),this.mimetype).subscribe((data:any)=>{
                        let split=this.fileName ? this.fileName.split("/") : this.getUri().split("/");
                        this.title=split[split.length-1];
                        this.file=data;
                        if(this.mimetype.startsWith("image/"))
                            this.previewUrl=this.sanitizer.bypassSecurityTrustUrl(data.localURL);
                        let request = new XMLHttpRequest();
                        let result=request.open('GET', data.localURL, true);
                        request.responseType = 'blob';
                        request.onload = ()=> {
                            if(request.response.size<=0){
                                this.router.navigate([UIConstants.ROUTER_PREFIX,'messages','CONTENT_NOT_READABLE']);
                            }
                            this.file=request.response;
                            (this.file as any).name=this.title;
                        };
                        request.onerror=(e)=>{
                            this.router.navigate([UIConstants.ROUTER_PREFIX,'messages','CONTENT_NOT_READABLE']);
                        }
                        request.send();
                    },(error:any)=>{
                        this.router.navigate([UIConstants.ROUTER_PREFIX,'messages','CONTENT_NOT_READABLE']);
                    });*/
                }
            });
        });
    }

    private updateTitle() {
        let split = this.fileName ? this.fileName.split('/') : this.uri.split('/');
        this.title = decodeURIComponent(split[split.length - 1]);
    }

    private hasData() {
        return (
            this.cordova.isRunningCordova() &&
            this.cordova.getLastIntent() &&
            this.cordova.getLastIntent().stream != null
        );
    }

    private prepareTextSnippet(connectors: Connector[]) {
        this.mimetype = 'text/plain';
        let filetype = 'txt';
        if (connectors && connectors.length) {
            let i = Helper.indexOfObjectArray(connectors, 'id', 'TINYMCE');
            if (i != -1) {
                let connector = connectors[i];
                this.mimetype = connector.filetypes[0].mimetype;
                filetype = connector.filetypes[0].filetype;
                this.editorType = connector.filetypes[0].editorType;
            }
        }
        this.title =
            this.translate.instant('SHARE_APP.TEXT_SNIPPET') +
            ' ' +
            DateHelper.getDateForNewFile() +
            '.' +
            filetype;
        let content = this.getTextContent();
        if (this.mimetype == 'text/html') {
            content = content.replace(/\n/g, '<br />');
        }
        this.file = new Blob([content], {
            type: this.mimetype,
        }) as any;
    }

    getTextContent(): string {
        return this.cordova.isAndroid() ? this.uri : this.text;
    }

    private getUri() {
        return this.cordova.isAndroid() ? this.uri : this.cordova.getLastIntent().base64;
    }
}
