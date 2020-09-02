import {Component, EventEmitter, Input, OnChanges, Output, SimpleChanges} from '@angular/core';
import {UIHelper} from "../../../../core-ui-module/ui-helper";
import {Node, Permission} from '../../../../core-module/rest/data-object';
import {RestConstants} from '../../../../core-module/rest/rest-constants';
import {RestConnectorService} from '../../../../core-module/rest/services/rest-connector.service';
import {NodeHelper} from '../../../../core-ui-module/node-helper';
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

@Component({
  selector: 'app-share-publish',
  templateUrl: 'share-publish.component.html',
  styleUrls: ['share-publish.component.scss']
})
export class SharePublishComponent implements OnChanges {
  @Input() node: Node;
  @Input() permissions: Permission[];
  @Input() inherited: boolean;
  @Output() onDisableInherit = new EventEmitter<void>();
  @Output() onInitCompleted = new EventEmitter<void>();
  doiPermission: boolean;
  initialShareMode: ShareMode;
  shareMode: ShareMode;
  publishCopyPermission: boolean;
  doiActive: boolean;
  doiDisabled: boolean;
  isCopy: boolean;
  republish = false;
  private publishedVersions: Node[] = [];
  allPublishedVersions: Node[];
  constructor(
      private connector: RestConnectorService,
      private nodeService: RestNodeService,
      private config: ConfigurationService,
      private toast: Toast,
      private router: Router,
      private bridge: BridgeService,
      private mainNavService: MainNavService
  ) {
    this.doiPermission = this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_HANDLESERVICE);
    this.publishCopyPermission = this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_PUBLISH_COPY);
  }

  async ngOnChanges(changes: SimpleChanges) {
    if (this.node && this.permissions) {
      this.node = (await this.nodeService.getNodeMetadata(this.node.ref.id, [RestConstants.ALL]).toPromise()).node;
      this.refresh();
      this.onInitCompleted.emit();
      this.onInitCompleted.complete();
    }
  }

  getLicense() {
    return this. node.properties[RestConstants.CCM_PROP_LICENSE]?.[0];
  }

  openLicense() {
    this.mainNavService.getDialogs().nodeLicense = [this.node];
    this.mainNavService.getDialogs().nodeLicenseChange.subscribe(async () => {
      this.node = (await this.nodeService.getNodeMetadata(this.node.ref.id, [RestConstants.ALL]).toPromise()).node;
      this.refresh();
    });
  }

  private refresh() {
    this.doiActive = NodeHelper.isDOIActive(this.node, this.permissions);
    this.doiDisabled = this.doiActive;
    const prop = this.node.properties[RestConstants.CCM_PROP_PUBLISHED_MODE]?.[0];
    if(prop === ShareMode.Copy) {
      this.shareMode = ShareMode.Copy;
      this.isCopy = true;
      this.nodeService.getPublishedCopies(this.node.ref.id).subscribe((nodes) => {
        this.publishedVersions = nodes.nodes.reverse();
        this.updatePublishedVersions();
      })
    } else if(prop === ShareMode.Direct || !prop) {
      this.shareMode = ShareMode.Direct;
    }
    if(prop !== ShareMode.Copy) {
      this.republish = true;
    }
    if(this.shareMode === ShareMode.Direct) {
      // if GROUP_EVERYONE is not yet invited -> reset to off
      if(this.permissions.filter(
          (p: Permission) => p.authority?.authorityName === RestConstants.AUTHORITY_EVERYONE).length === 0
      ) {
        this.shareMode = null;
      }
    }
    this.initialShareMode = this.shareMode;
    this.updatePublishedVersions();
  }



  updateShareMode(shareMode: ShareMode, force = false) {
    this.shareMode = shareMode;
    if(this.shareMode != null && !force) {
      if (this.config.instant('publishingNotice', false)) {
        let cancel = () => {
          this.updateShareMode(null, true);
          this.toast.closeModalDialog();
        };
        this.toast.showModalDialog(
            'WORKSPACE.SHARE.PUBLISHING_WARNING_TITLE',
            'WORKSPACE.SHARE.PUBLISHING_WARNING_MESSAGE',
            DialogButton.getYesNo(cancel, () => {
              this.updateShareMode(shareMode);
              this.toast.closeModalDialog();
            }),
            true,
            cancel,
        );
        return;
      }
    }
    if(this.shareMode != null && this.doiPermission) {
      this.doiActive = true;
    }
    this.updatePublishedVersions();
  }

  updatePermissions(permissions: Permission[]) {
    permissions = permissions.filter(
        (p: Permission) => p.authority.authorityName !== RestConstants.AUTHORITY_EVERYONE
    );
    if(this.shareMode === ShareMode.Direct) {
      const permission = RestHelper.getAllAuthoritiesPermission()
      permission.permissions = [RestConstants.ACCESS_CONSUMER, RestConstants.ACCESS_CC_PUBLISH];
      permissions.push(permission);
    }
    return permissions;
  }

  save() {
    return new Observable((observer: Observer<Node|void>) => {
      if (this.shareMode === ShareMode.Copy &&
          // republish and not yet published, or wasn't published before at all
          (this.republish && !this.currentVersionPublished() || !this.isCopy)) {
        this.nodeService.publishCopy(this.node.ref.id).subscribe(({node}) => {
          if (this.doiPermission && !this.doiDisabled && this.doiActive) {
            console.log('create handle');
            this.nodeService.setNodePermissions(node.ref.id,
                null, false, '', false, true
            ).subscribe(() => {
              observer.next(node);
              observer.complete();
            },error => {
              observer.error(error);
              observer.complete();
            });
          } else {
            observer.next(node);
            observer.complete();
          }
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
    const url = '/' + this.router.serializeUrl(this.router.createUrlTree([UIConstants.ROUTER_PREFIX, 'render' ,node.ref.id] ));
    UIHelper.openUrl(url, this.bridge, OPEN_URL_MODE.Blank);
  }

  currentVersionPublished() {
    return this.publishedVersions?.filter((p) =>
        p.properties[RestConstants.LOM_PROP_LIFECYCLE_VERSION]?.[0] ===
        this.node.properties[RestConstants.LOM_PROP_LIFECYCLE_VERSION]?.[0]
    ).length !== 0;
  }

  updatePublishedVersions() {
    if(!this.isCopy && this.shareMode === ShareMode.Copy
        || this.republish) {
      console.log('update true')
      const virtual = Helper.deepCopy(this.node);
      virtual.properties[RestConstants.CCM_PROP_PUBLISHED_DATE + '_LONG'] = [new Date().getTime()];
      if(this.doiActive) {
        virtual.properties[RestConstants.CCM_PROP_PUBLISHED_HANDLE_ID] = [true];
      }
      virtual.virtual = true;
      this.allPublishedVersions = [virtual].concat(this.publishedVersions);
    } else {
      console.log('update false')
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
}
export enum ShareMode {
  Direct = 'direct',
  Copy = 'copy'
}