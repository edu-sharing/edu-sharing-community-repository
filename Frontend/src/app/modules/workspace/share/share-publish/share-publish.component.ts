import {Component, Input, EventEmitter, Output, ViewChild, ElementRef, AfterViewInit, OnChanges, SimpleChanges} from '@angular/core';
import {UIHelper} from "../../../../core-ui-module/ui-helper";
import { MatSlideToggle } from "@angular/material/slide-toggle";
import {LocalPermissions, Node, Permission, Permissions} from '../../../../core-module/rest/data-object';
import {RestConstants} from '../../../../core-module/rest/rest-constants';
import {RestConnectorService} from '../../../../core-module/rest/services/rest-connector.service';
import {NodeHelper} from '../../../../core-ui-module/node-helper';
import {RestHelper} from '../../../../core-module/rest/rest-helper';
import {MainNavService} from '../../../../common/services/main-nav.service';
import {RestNodeService} from '../../../../core-module/rest/services/rest-node.service';

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
  doiPermission: boolean;
  shareMode: ShareMode;
  publishCopyPermission: boolean;
  doiActive: boolean;
  doiDisabled: boolean;
  isCopy: boolean;
  constructor(
      private connector: RestConnectorService,
      private nodeService: RestNodeService,
      private mainNavService: MainNavService
  ) {
    this.doiPermission = this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_HANDLESERVICE);
    this.publishCopyPermission = this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_PUBLISH_COPY);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if(this.node && this.permissions) {
      this.refresh();
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
    const prop = this.node.properties[RestConstants.CCM_PROP_PUBLISH_MODE]?.[0];
    if(prop === ShareMode.Copy) {
      this.shareMode = ShareMode.Copy;
      this.isCopy = true;
    } else if(prop === ShareMode.Direct || !prop) {
      this.shareMode = ShareMode.Direct;
    }
    if(this.shareMode === ShareMode.Direct) {
      // if GROUP_EVERYONE is not yet invited -> reset to off
      if(this.permissions.filter(
          (p: Permission) => p.authority?.authorityName === RestConstants.AUTHORITY_EVERYONE).length === 0
      ) {
        this.shareMode = null;
      }
    }
  }

  updateShareMode() {
    if(this.shareMode != null && this.doiPermission) {
      this.doiActive = true;
    }
  }

  updatePermissions(permissions: Permission[]) {
    const isPublished = this.permissions.filter(
        (p: Permission) => p.authority.authorityName === RestConstants.AUTHORITY_EVERYONE
    ).length !== 0;
    if(this.shareMode === ShareMode.Direct && !isPublished) {
      const permission = RestHelper.getAllAuthoritiesPermission()
      permission.permissions = [RestConstants.ACCESS_CONSUMER, RestConstants.ACCESS_CC_PUBLISH];
      permissions.push(permission);
    } else if(this.shareMode !== ShareMode.Direct && isPublished) {
      permissions = permissions.filter((p: Permission) => p.authority.authorityName !== RestConstants.AUTHORITY_EVERYONE);
    }
    console.log(permissions)
    return permissions;
  }
}
export enum ShareMode {
  Direct = 'direct',
  Copy = 'copy'
}