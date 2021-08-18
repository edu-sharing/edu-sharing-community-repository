import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges,
    ViewChild
} from '@angular/core';
import {UIHelper} from "../../../../core-ui-module/ui-helper";
import {Node, Permission} from '../../../../core-module/rest/data-object';
import {RestConstants} from '../../../../core-module/rest/rest-constants';
import {RestConnectorService} from '../../../../core-module/rest/services/rest-connector.service';

import {RestHelper} from '../../../../core-module/rest/rest-helper';
import {MainNavService} from '../../../../common/services/main-nav.service';
import {RestNodeService} from '../../../../core-module/rest/services/rest-node.service';
import {Observable, Observer} from 'rxjs';
import {Router} from '@angular/router';
import {ConfigurationService, DialogButton, UIConstants} from '../../../../core-module/core.module';
import {OPEN_URL_MODE} from '../../../../core-module/ui/ui-constants';
import {BridgeService} from '../../../../core-bridge-module/bridge.service';
import {Helper} from '../../../../core-module/rest/helper';
import {Toast} from '../../../../core-ui-module/toast';
import {TranslateService} from '@ngx-translate/core';
import {MdsEditorInstanceService, CompletionStatusEntry} from '../../../../common/ui/mds-editor/mds-editor-instance.service';
import {NodeHelperService} from '../../../../core-ui-module/node-helper.service';

@Component({
    selector: 'app-share-publish',
    templateUrl: 'share-publish.component.html',
    styleUrls: ['share-publish.component.scss'],
    providers: [MdsEditorInstanceService],
})
export class SharePublishComponent implements OnChanges {
    @Input() node: Node;
    @Input() permissions: Permission[];
    @Input() inherited: boolean;
    @Input() isAuthorEmtpy: boolean;
    @Input() isLicenseEmtpy: boolean;
    @Output() onDisableInherit = new EventEmitter<void>();
    @Output() onInitCompleted = new EventEmitter<void>();
    @ViewChild('shareModeCopyRef') shareModeCopyRef: any;
    @ViewChild('shareModeDirectRef') shareModeDirectRef: any;
    doiPermission: boolean;
    initialState: {
        copy: boolean,
        direct: boolean
    };
    shareModeCopy: boolean;
    shareModeDirect: boolean;
    publishCopyPermission: boolean;
    doiActive: boolean;
    doiDisabled: boolean;
    isCopy: boolean;
    handleMode: 'distinct' | 'update' = 'distinct';
    republish = false;
    private publishedVersions: Node[] = [];
    allPublishedVersions: Node[];
    mdsCompletion: CompletionStatusEntry;
    private initHasStarted = false;
    constructor(
        private connector: RestConnectorService,
        private translate: TranslateService,
        private nodeHelper: NodeHelperService,
        private nodeService: RestNodeService,
        private config: ConfigurationService,
        private mdsService: MdsEditorInstanceService,
        private toast: Toast,
        private router: Router,
        private bridge: BridgeService,
        private mainNavService: MainNavService
    ) {
        this.doiPermission = this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_HANDLESERVICE);
        this.publishCopyPermission = this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_PUBLISH_COPY);
    }

    async ngOnChanges(changes: SimpleChanges) {
        if (this.node && this.permissions && !this.initHasStarted) {
            this.initHasStarted = true;
            // refresh already for providing initial state
            this.refresh();
            this.node = (await this.nodeService.getNodeMetadata(this.node.ref.id, [RestConstants.ALL]).toPromise()).node;
            this.refresh();
            this.onInitCompleted.emit();
            this.onInitCompleted.complete();
        }
    }
    getLicense() {
        return this.node.properties[RestConstants.CCM_PROP_LICENSE]?.[0];
    }
    getLicenseText() {
        return this.translate.instant('LICENSE.NAMES.' + this.getLicense());
    }

    openLicense() {
        this.mainNavService.getDialogs().nodeLicense = [this.node];
        this.mainNavService.getDialogs().nodeLicenseChange.subscribe(async () => {
            this.node = (await this.nodeService.getNodeMetadata(this.node.ref.id, [RestConstants.ALL]).toPromise()).node;
            this.refresh();
        });
    }
    openMetadata() {
        this.mainNavService.getDialogs().nodeMetadata = [this.node];
        setTimeout(() => { // Wait for `mdsEditorWrapperRef`
            this.mainNavService.getDialogs().mdsEditorWrapperRef.mdsEditorInstance.mdsInflated.subscribe(() =>
                this.mainNavService.getDialogs().mdsEditorWrapperRef.mdsEditorInstance.showMissingRequiredWidgets(false)
            );
        })
        this.mainNavService.getDialogs().nodeMetadataChange.subscribe(async () => {
            this.node = (await this.nodeService.getNodeMetadata(this.node.ref.id, [RestConstants.ALL]).toPromise()).node;
            this.refresh();
        });
    }

    private refresh() {
        this.doiActive = this.nodeHelper.isDOIActive(this.node, this.permissions);
        this.doiDisabled = this.doiActive;
        const prop = this.node?.properties?.[RestConstants.CCM_PROP_PUBLISHED_MODE]?.[0];
        if(prop === ShareMode.Copy) {
            this.shareModeCopy = true;
            this.isCopy = true;
            this.nodeService.getPublishedCopies(this.node.ref.id).subscribe((nodes) => {
                this.publishedVersions = nodes.nodes.reverse();
                this.updatePublishedVersions();
            }, error => {
                this.toast.error(error);
            });
        }
        if(prop !== ShareMode.Copy) {
            this.republish = true;
        }
        // if GROUP_EVERYONE is not yet invited -> reset to off
        this.shareModeDirect = this.permissions.some(
            (p: Permission) => p.authority?.authorityName === RestConstants.AUTHORITY_EVERYONE
        );
        this.initialState = {
            copy: this.shareModeCopy,
            direct: this.shareModeDirect
        };
        this.mdsService.observeCompletionStatus().subscribe((completion) => {
            this.mdsCompletion = {
                completed: (completion.mandatory.completed || 0) + (completion.mandatoryForPublish.completed || 0),
                total: (completion.mandatory.total || 0) + (completion.mandatoryForPublish.total || 0),
            };
        });
        this.mdsService.initWithNodes([this.node]);
        this.updatePublishedVersions();
    }



    updateShareMode(force = false) {
        if((this.shareModeCopy  || this.shareModeDirect) && !force) {
            if (this.config.instant('publishingNotice', false)) {
                let cancel = () => {
                    this.shareModeDirect = false;
                    this.shareModeCopy = false;
                    this.toast.closeModalDialog();
                };
                this.toast.showModalDialog(
                    'WORKSPACE.SHARE.PUBLISHING_WARNING_TITLE',
                    'WORKSPACE.SHARE.PUBLISHING_WARNING_MESSAGE',
                    DialogButton.getYesNo(cancel, () => {
                        this.updateShareMode(true);
                        this.toast.closeModalDialog();
                    }),
                    true,
                    cancel,
                );
                return;
            }
        }
        if(this.shareModeCopy && this.doiPermission) {
            this.doiActive = true;
        }
        this.updatePublishedVersions();
    }

    updatePermissions(permissions: Permission[]) {
        permissions = permissions.filter(
            (p: Permission) => p.authority.authorityName !== RestConstants.AUTHORITY_EVERYONE
        );
        if(this.shareModeDirect) {
            const permission = RestHelper.getAllAuthoritiesPermission()
            permission.permissions = [RestConstants.ACCESS_CONSUMER, RestConstants.ACCESS_CC_PUBLISH];
            permissions.push(permission);
        }
        return permissions;
    }

    save() {
        return new Observable((observer: Observer<Node|void>) => {
            if (this.shareModeCopy &&
                // republish and not yet published, or wasn't published before at all
                (this.republish && !this.currentVersionPublished() || !this.isCopy)) {
                this.nodeService.publishCopy(this.node.ref.id,
                    (this.doiPermission && !this.doiDisabled && this.doiActive) ? this.handleMode : null
                ).subscribe(({node}) => {
                    observer.next(node);
                    observer.complete();
                }, error => {
                    observer.error(error);
                    observer.complete();
                });
            } else {
                observer.next(null);
                observer.complete();
            }
        });
    }

    openVersion(node: Node) {
        const url = this.connector.getAbsoluteEdusharingUrl() +
            this.router.serializeUrl(this.router.createUrlTree([UIConstants.ROUTER_PREFIX, 'render' ,node.ref.id] ));
        UIHelper.openUrl(url, this.bridge, OPEN_URL_MODE.Blank);
    }

    currentVersionPublished() {
        return this.publishedVersions?.filter((p) =>
            p.properties[RestConstants.LOM_PROP_LIFECYCLE_VERSION]?.[0] ===
            this.node.properties[RestConstants.LOM_PROP_LIFECYCLE_VERSION]?.[0]
        ).length !== 0;
    }

    updatePublishedVersions() {
        if(!this.isCopy && this.shareModeCopy
            || this.republish) {
            if(this.node?.properties) {
                const virtual = Helper.deepCopy(this.node);
                virtual.properties[RestConstants.CCM_PROP_PUBLISHED_DATE + '_LONG'] = [new Date().getTime()];
                if (this.doiActive && !this.doiDisabled && this.doiPermission) {
                    virtual.properties[RestConstants.CCM_PROP_PUBLISHED_HANDLE_ID] = [true];
                }
                virtual.virtual = true;
                this.allPublishedVersions = [virtual].concat(this.publishedVersions);
                this.handleMode = this.hasExactOneHandle() ? 'update' : 'distinct';
            }
        } else {
            this.allPublishedVersions = this.publishedVersions;
        }
    }

    getType() {
        if(this.node?.isDirectory) {
            return this.node.collection ? 'COLLECTION' : 'DIRECTORY';
        } else {
            return 'DOCUMENT';
        }
    }

    copyAllowed() {
        return this.publishCopyPermission && !this.node?.isDirectory;
    }

    setRepublish() {
        this.doiActive = this.republish && this.doiPermission;
        this.updatePublishedVersions();
    }

    hasExactOneHandle() {
        return new Set(this.allPublishedVersions.filter(
            (v) => !v.virtual && v.properties[RestConstants.CCM_PROP_PUBLISHED_HANDLE_ID]
        ).map((v) => v.properties[RestConstants.CCM_PROP_PUBLISHED_HANDLE_ID][0])).size === 1;
    }

    onShareModeClick(event: any): void {
        if (!event._checked) {
            if (this.isLicenseEmtpy && !this.node.isDirectory) {
                this.toast.error(null, this.translate.instant('WORKSPACE.LICENSE.RELEASE_WITHOUT_LICENSE'));
                event.preventDefaultEvent();
            }
            if (this.isAuthorEmtpy && !this.node.isDirectory) {
                this.toast.error(null, this.translate.instant('WORKSPACE.LICENSE.RELEASE_WITHOUT_AUTHOR'));
                event.preventDefaultEvent();
            }
        }
    }
}
export enum ShareMode {
    Direct = 'direct',
    Copy = 'copy'
}
