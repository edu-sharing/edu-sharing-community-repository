import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { NodeService } from 'ngx-edu-sharing-api';
import { Observable, Observer, Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { BridgeService } from '../../../../../services/bridge.service';
import {
    ConfigurationService,
    DialogButton,
    UIConstants,
} from '../../../../../core-module/core.module';
import { Node, Permission } from '../../../../../core-module/rest/data-object';
import { Helper } from '../../../../../core-module/rest/helper';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';
import { RestHelper } from '../../../../../core-module/rest/rest-helper';
import { RestConnectorService } from '../../../../../core-module/rest/services/rest-connector.service';
import { RestNodeService } from '../../../../../core-module/rest/services/rest-node.service';
import { NodeHelperService } from '../../../../../services/node-helper.service';
import { Toast } from '../../../../../services/toast';
import { UIHelper } from '../../../../../core-ui-module/ui-helper';
import { MainNavService } from '../../../../../main/navigation/main-nav.service';
import {
    CompletionStatusEntry,
    MdsEditorInstanceService,
} from '../../../../mds/mds-editor/mds-editor-instance.service';
import { DialogsService } from '../../../dialogs.service';
import { OPEN_URL_MODE } from 'ngx-edu-sharing-ui';
import { YES_OR_NO } from '../../generic-dialog/generic-dialog-data';

class PublishedNode extends Node {
    status?: 'new' | 'update' | null; // flag if this node is manually added later and didn't came from the repo
}

@Component({
    selector: 'es-share-dialog-publish',
    templateUrl: 'publish.component.html',
    styleUrls: ['publish.component.scss'],
    providers: [MdsEditorInstanceService],
})
export class ShareDialogPublishComponent implements OnChanges, OnInit, OnDestroy {
    @Input() node: Node;
    @Input() permissions: Permission[];
    @Input() inherited: boolean;
    @Input() isAuthorEmpty: boolean;
    @Input() isLicenseEmpty: boolean;
    @Output() onDisableInherit = new EventEmitter<void>();
    @Output() onInitCompleted = new EventEmitter<void>();
    @ViewChild('shareModeCopyRef') shareModeCopyRef: any;
    @ViewChild('shareModeDirectRef') shareModeDirectRef: any;
    doiPermission: boolean;
    initialState: {
        copy: boolean;
        direct: boolean;
    };
    shareModeCopy: boolean;
    shareModeDirect: boolean;
    publishCopyPermission: boolean;
    doiActive: boolean;
    doiDisabled: boolean;
    isCopy: boolean;
    handleMode: 'distinct' | 'update' = 'distinct';
    republish: 'update' | 'new' | false = false;
    private publishedVersions: Node[] = [];
    allPublishedVersions: PublishedNode[];
    mdsCompletion: CompletionStatusEntry;
    private initHasStarted = false;
    private destroyed = new Subject<void>();

    constructor(
        private bridge: BridgeService,
        private config: ConfigurationService,
        private connector: RestConnectorService,
        private dialogs: DialogsService,
        private legacyNodeService: RestNodeService,
        private mdsService: MdsEditorInstanceService,
        private nodeHelper: NodeHelperService,
        private nodeService: NodeService,
        private router: Router,
        private toast: Toast,
        private translate: TranslateService,
    ) {
        this.doiPermission = this.connector.hasToolPermissionInstant(
            RestConstants.TOOLPERMISSION_HANDLESERVICE,
        );
        this.publishCopyPermission = this.connector.hasToolPermissionInstant(
            RestConstants.TOOLPERMISSION_PUBLISH_COPY,
        );
    }

    ngOnInit(): void {
        this.mdsService
            .observeCompletionStatus()
            .pipe(
                takeUntil(this.destroyed),
                filter((completion) => completion !== null),
            )
            .subscribe((completion) => {
                this.mdsCompletion = {
                    completed:
                        (completion.mandatory.completed || 0) +
                        (completion.mandatoryForPublish.completed || 0),
                    total:
                        (completion.mandatory.total || 0) +
                        (completion.mandatoryForPublish.total || 0),
                };
            });
    }

    async ngOnChanges(changes: SimpleChanges) {
        if (this.node && this.permissions && !this.initHasStarted) {
            this.initHasStarted = true;
            // refresh already for providing initial state
            this.refresh();
            this.node = (
                await this.legacyNodeService
                    .getNodeMetadata(this.node.ref.id, [RestConstants.ALL])
                    .toPromise()
            ).node;
            this.refresh();
            this.onInitCompleted.emit();
            this.onInitCompleted.complete();
        }
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    getLicense() {
        return this.node.properties[RestConstants.CCM_PROP_LICENSE]?.[0];
    }
    getLicenseText() {
        return this.translate.instant('LICENSE.NAMES.' + this.getLicense());
    }

    async openLicense() {
        const dialogRef = await this.dialogs.openLicenseDialog({
            kind: 'nodes',
            nodes: [this.node],
        });
        dialogRef.afterClosed().subscribe((updatedNodes) => {
            if (updatedNodes) {
                // We used to fetch the node again, but we should be fine just taking the updated
                // node from the dialog, right?
                //
                // this.node = (
                //     await this.legacyNodeService
                //         .getNodeMetadata(this.node.ref.id, [RestConstants.ALL])
                //         .toPromise()
                // ).node;
                this.node = updatedNodes[0];
                this.refresh();
            }
        });
    }
    async openMetadata() {
        const dialogRef = await this.dialogs.openMdsEditorDialogForNodes({
            nodes: [this.node],
            immediatelyShowMissingRequiredWidgets: true,
        });
        dialogRef.afterClosed().subscribe((nodes) => {
            if (nodes) {
                this.node = nodes[0];
                // this.node = (
                //     await this.legacyNodeService
                //         .getNodeMetadata(this.node.ref.id, [RestConstants.ALL])
                //         .toPromise()
                // ).node;
                this.refresh();
            }
        });
    }

    private refresh() {
        this.doiActive = this.nodeHelper.isDOIActive(this.node, this.permissions);
        this.doiDisabled = this.doiActive;
        const prop = this.node?.properties?.[RestConstants.CCM_PROP_PUBLISHED_MODE]?.[0];
        if (prop === ShareMode.Copy) {
            this.shareModeCopy = true;
            this.isCopy = true;
            this.legacyNodeService.getPublishedCopies(this.node.ref.id).subscribe(
                (nodes) => {
                    this.publishedVersions = nodes.nodes.reverse();
                    this.updatePublishedVersions();
                },
                (error) => {
                    this.toast.error(error);
                },
            );
        }
        if (prop !== ShareMode.Copy) {
            this.republish = 'new';
        }
        // if GROUP_EVERYONE is not yet invited -> reset to off
        this.shareModeDirect = this.permissions.some(
            (p: Permission) => p.authority?.authorityName === RestConstants.AUTHORITY_EVERYONE,
        );
        this.initialState = {
            copy: this.shareModeCopy,
            direct: this.shareModeDirect,
        };
        this.mdsService.initWithNodes([this.node]);
        this.updatePublishedVersions();
    }

    async updateShareMode(type: 'copy' | 'direct', force = false) {
        if ((this.shareModeCopy || this.shareModeDirect) && !force) {
            if (this.config.instant('publishingNotice', false)) {
                const dialogRef = await this.dialogs.openGenericDialog({
                    title: 'WORKSPACE.SHARE.PUBLISHING_WARNING_TITLE',
                    message: 'WORKSPACE.SHARE.PUBLISHING_WARNING_MESSAGE',
                    buttons: YES_OR_NO,
                });
                dialogRef.afterClosed().subscribe((response) => {
                    if (response === 'YES') {
                        void this.updateShareMode(type, true);
                    } else {
                        this.shareModeDirect = false;
                        this.shareModeCopy = false;
                    }
                });
                return;
            }
        }
        if (this.shareModeCopy && this.doiPermission && type === 'copy') {
            this.doiActive = true;
        }
        this.updatePublishedVersions();
    }

    updatePermissions(permissions: Permission[]) {
        permissions = permissions.filter(
            (p: Permission) => p.authority.authorityName !== RestConstants.AUTHORITY_EVERYONE,
        );
        if (this.shareModeDirect) {
            const permission = RestHelper.getAllAuthoritiesPermission();
            permission.permissions = [
                RestConstants.ACCESS_CONSUMER,
                RestConstants.ACCESS_CC_PUBLISH,
            ];
            permissions.push(permission);
        }
        return permissions;
    }

    save() {
        return new Observable((observer: Observer<Node | void>) => {
            if (
                this.shareModeCopy &&
                // republish and not yet published, or wasn't published before at all
                ((this.republish === 'new' && !this.currentVersionPublished()) || !this.isCopy)
            ) {
                this.legacyNodeService
                    .publishCopy(
                        this.node.ref.id,
                        this.doiPermission && !this.doiDisabled && this.doiActive
                            ? this.handleMode
                            : null,
                    )
                    .subscribe(
                        ({ node }) => {
                            observer.next(node);
                            observer.complete();
                        },
                        (error) => {
                            observer.error(error);
                            observer.complete();
                        },
                    );
            } else if (
                this.shareModeCopy &&
                // update most recent version
                this.republish === 'update'
            ) {
                this.nodeService
                    .copyMetadata(this.publishedVersions[0].ref.id, this.node.ref.id, {})
                    .subscribe(
                        ({ node }) => {
                            observer.next(node);
                            observer.complete();
                        },
                        (error) => {
                            observer.error(error);
                            observer.complete();
                        },
                    );
            } else {
                observer.next(null);
                observer.complete();
            }
        });
    }

    openVersion(node: Node) {
        const url =
            this.connector.getAbsoluteEdusharingUrl() +
            this.router.serializeUrl(
                this.router.createUrlTree([UIConstants.ROUTER_PREFIX, 'render', node.ref.id]),
            );
        UIHelper.openUrl(url, this.bridge, OPEN_URL_MODE.Blank);
    }

    currentVersionPublished() {
        return (
            this.publishedVersions?.filter(
                (p) =>
                    p.properties[RestConstants.LOM_PROP_LIFECYCLE_VERSION]?.[0] ===
                    this.node.properties[RestConstants.LOM_PROP_LIFECYCLE_VERSION]?.[0],
            ).length !== 0
        );
    }

    updatePublishedVersions() {
        if ((!this.isCopy && this.shareModeCopy) || this.republish === 'new') {
            if (this.node?.properties) {
                const virtual = Helper.deepCopy(this.node);
                virtual.properties[RestConstants.CCM_PROP_PUBLISHED_DATE + '_LONG'] = [
                    new Date().getTime(),
                ];
                if (this.doiActive && !this.doiDisabled && this.doiPermission) {
                    virtual.properties[RestConstants.CCM_PROP_PUBLISHED_HANDLE_ID] = [true];
                }
                virtual.status = 'new';
                this.allPublishedVersions = [virtual].concat(this.publishedVersions);
                this.handleMode = this.hasExactOneHandle() ? 'update' : 'distinct';
            }
        } else if (this.republish === 'update') {
            this.allPublishedVersions = Helper.deepCopy(this.publishedVersions);
            this.allPublishedVersions[0].status = 'update';
        } else {
            this.allPublishedVersions = this.publishedVersions;
        }
    }

    getType() {
        if (this.node?.isDirectory) {
            return this.node.collection ? 'COLLECTION' : 'DIRECTORY';
        } else {
            return 'DOCUMENT';
        }
    }

    copyAllowed() {
        return this.publishCopyPermission && !this.node?.isDirectory;
    }

    setRepublish() {
        this.doiActive = this.republish !== false && this.doiPermission;
        this.updatePublishedVersions();
    }

    hasExactOneHandle() {
        return (
            new Set(
                this.allPublishedVersions
                    .filter(
                        (v) =>
                            !v.status && v.properties[RestConstants.CCM_PROP_PUBLISHED_HANDLE_ID],
                    )
                    .map((v) => v.properties[RestConstants.CCM_PROP_PUBLISHED_HANDLE_ID][0]),
            ).size === 1
        );
    }
    isLicenseMissing() {
        return !this.getLicense() && this.isLicenseEmpty && !this.node.isDirectory;
    }
    isAuthorMissing() {
        return this.isAuthorEmpty && !this.node.isDirectory;
    }
}
export enum ShareMode {
    Direct = 'direct',
    Copy = 'copy',
}
