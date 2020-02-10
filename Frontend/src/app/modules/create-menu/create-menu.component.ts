import { trigger } from '@angular/animations';
import { PlatformLocation } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import {Component, ElementRef, EventEmitter, HostListener, Input, OnInit, Output, ViewChild} from '@angular/core';
import { FormControl } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { map, startWith } from 'rxjs/operators';
import { GlobalContainerComponent } from '../../common/ui/global-container/global-container.component';
import { MainNavComponent } from '../../common/ui/main-nav/main-nav.component';
import { BridgeService } from '../../core-bridge-module/bridge.service';
import {AccessScope, ConfigurationService, Connector, DialogButton, Filetype, LoginResult, Node, NodeWrapper, RestConnectorService, RestConnectorsService, RestConstants, RestHelper, RestIamService, RestNodeService, SessionStorageService, TemporaryStorageService, FrameEventsService} from '../../core-module/core.module';
import { Helper } from '../../core-module/rest/helper';
import { UIAnimation } from '../../core-module/ui/ui-animation';
import { OPEN_URL_MODE, UIConstants } from '../../core-module/ui/ui-constants';
import { InputPasswordComponent } from '../../core-ui-module/components/input-password/input-password.component';
import { RouterHelper } from '../../core-ui-module/router.helper';
import { Toast } from '../../core-ui-module/toast';
import { Translation } from '../../core-ui-module/translation';
import { UIHelper } from '../../core-ui-module/ui-helper';
import {MatMenu} from '@angular/material/menu';
import {DateHelper} from '../../core-ui-module/DateHelper';
import {NodeHelper} from '../../core-ui-module/node-helper';
import {CardComponent} from '../../core-ui-module/components/card/card.component';


@Component({
    selector: 'app-create-menu',
    templateUrl: 'create-menu.component.html',
    styleUrls: ['create-menu.component.scss'],
    animations: [
        trigger('dialog', UIAnimation.switchDialog(UIAnimation.ANIMATION_TIME_FAST)),
    ]
})
export class CreateMenuComponent {
    @ViewChild('dropdown', {static: false}) dropdown : MatMenu;
    /**
     * Currently allowed to drop files?
     */
    @Input() allowed = true;
    /**
     * Current search query
     */
    @Input() searchQuery: string;
    /**
     * Allow upload of binary files
     */
    @Input() allowBinary = true;
    /**
     * Parent location. If null, the folder picker will be shown
     */
    @Input() set parent(parent: Node) {
        console.log(parent);
        if (parent == null) {
            this._parent = null;
            this.nodeService.getNodeMetadata(RestConstants.INBOX).subscribe((node) => {
                // check because of race condition
                if(this._parent == null) {
                    this._parent = node.node;
                }
            });
            this.showPicker = true;
        } else {
            this._parent = parent;
            this.showPicker = false;
        }
    }
    /**
     * can a folder be created
     */
    @Input() folder = true;
    /**
     * Fired when elements are created or uploaded
     */
    @Output() onCreate = new EventEmitter<Node[]>();

    _parent: Node = null;
    addFolderName: string = null;
    showUploadSelect = false;
    filesToUpload: FileList;
    connectorList: Connector[];
    fileIsOver = false;
    showPicker: boolean;
    createConnectorName: string;
    createConnectorType: Connector;

    private params: Params;

    constructor(
        private connectors: RestConnectorsService,
        private iamService: RestIamService,
        private nodeService: RestNodeService,
        private toast: Toast,
        private router: Router,
        private translate: TranslateService,
        private temporaryStorage: TemporaryStorageService,
        private route: ActivatedRoute,
        private bridge: BridgeService,
        private iam: RestIamService,
        private event: FrameEventsService,
    ) {
        this.route.queryParams.subscribe((params)=> {
            this.params = params;
        });
        this.connectors.list().subscribe(() => {
            this.connectorList = this.connectors.getConnectors();
        });
    }

    private openCamera() {
        this.bridge.getCordova().getPhotoFromCamera((data: any) => {
            console.log(data);
            const name = this.translate.instant('SHARE_APP.IMAGE')
                + ' '
                + DateHelper.formatDate(this.translate, new Date().getTime(), { showAlwaysTime: true, useRelativeLabels: false })
                + '.jpg';
            const blob: any = Helper.base64toBlob(data, 'image/jpeg');
            blob.name = name;
            const list: any = {};
            list.item = (i: number) => {
                return blob;
            };
            list.length = 1;
            this.filesToUpload = list;
        }, (error: any) => {
            console.warn(error);
            // this.toast.error(error);
        });
    }

    addFolder(folder: any) {
        this.addFolderName = null;
        this.toast.showProgressDialog();
        const properties = RestHelper.createNameProperty(folder.name);
        if (folder.metadataset) {
            properties[RestConstants.CM_PROP_METADATASET_EDU_METADATASET] = [folder.metadataset];
            properties[RestConstants.CM_PROP_METADATASET_EDU_FORCEMETADATASET] = ['true'];
        }
        this.nodeService.createNode(this._parent.ref.id, RestConstants.CM_TYPE_FOLDER, [], properties).subscribe(
            (data: NodeWrapper) => {
                this.toast.closeModalDialog();
                this.onCreate.emit([data.node]);
                // this.refresh();
                this.toast.toast('WORKSPACE.TOAST.FOLDER_ADDED');
            },
            (error: any) => {
                this.toast.closeModalDialog();
                if (NodeHelper.handleNodeError(this.toast, folder.name, error) === RestConstants.DUPLICATE_NODE_RESPONSE) {
                    this.addFolderName = folder.name;
                }
            }
        );
    }
    private uploadFiles(files: FileList) {
        this.onFileDrop(files);
    }
    public onFileDrop(files: FileList) {
        if (!this.allowed) {
            if (this.searchQuery) {
                this.toast.error(null, 'WORKSPACE.TOAST.NOT_POSSIBLE_IN_SEARCH');
            } else {
                this.toast.error(null, 'WORKSPACE.TOAST.NOT_POSSIBLE_GENERAL');
            }
            return;
        }
        if (this.filesToUpload) {
            this.toast.error(null, 'WORKSPACE.TOAST.ONGOING_UPLOAD');
            return;
        }
        this.showUploadSelect = false;
        this.filesToUpload = files;
    }

    hasOpenWindows() {
        return CardComponent.getNumberOfOpenCards() !== 0;
    }
    private afterUpload(nodes: Node[]) {
        if (this.params.reurl) {
            NodeHelper.addNodeToLms(this.router, this.temporaryStorage, nodes[0], this.params.reurl);
        }
        this.onCreate.emit(nodes);
    }
    showCreateConnector(connector: Connector) {
        this.createConnectorName = '';
        this.createConnectorType = connector;
        this.iamService.getUser().subscribe((user) => {
            if (user.person.quota.enabled && user.person.quota.sizeCurrent >= user.person.quota.sizeQuota) {
                this.toast.showModalDialog('CONNECTOR_QUOTA_REACHED_TITLE', 'CONNECTOR_QUOTA_REACHED_MESSAGE', DialogButton.getOk(() => {
                    this.toast.closeModalDialog();
                }), true);
                this.createConnectorName = null;
            }
        });
    }
    private editConnector(node: Node = null, type: Filetype = null, win: any = null, connectorType: Connector = null) {
        UIHelper.openConnector(this.connectors, this.iam, this.event, this.toast, node, type, win, connectorType);
    }
    private createConnector(event: any) {
        const name = event.name + '.' + event.type.filetype;
        this.createConnectorName = null;
        const prop = NodeHelper.propertiesFromConnector(event);
        let win: any;
        if (!this.bridge.isRunningCordova()) {
            win = window.open('');
        }
        this.nodeService.createNode(this._parent.ref.id, RestConstants.CCM_TYPE_IO, [], prop, false).subscribe(
            (data: NodeWrapper) => {
                this.editConnector(data.node, event.type, win, this.createConnectorType);
                this.onCreate.emit([data.node]);
            },
            (error: any) => {
                win.close();
                if (NodeHelper.handleNodeError(this.toast, event.name, error) === RestConstants.DUPLICATE_NODE_RESPONSE) {
                    this.createConnectorName = event.name;
                }
            }
        );
    }
}
