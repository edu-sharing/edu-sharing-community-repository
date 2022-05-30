import {Component, Input, EventEmitter, Output, ViewChild, ElementRef, HostListener} from '@angular/core';
import {Permission, NodePermissionsHistory, DialogButton} from '../../../../core-module/core.module';
import {Toast} from '../../../../core-ui-module/toast';
import {RestNodeService} from '../../../../core-module/core.module';
import {
  Node
} from '../../../../core-module/core.module';

import {TranslateService} from '@ngx-translate/core';
import {RestConstants} from '../../../../core-module/core.module';
import {Helper} from '../../../../core-module/rest/helper';
import {RestHelper} from '../../../../core-module/core.module';
import {DateHelper} from '../../../../core-ui-module/DateHelper';
import {UIAnimation} from '../../../../core-module/ui/ui-animation';
import {trigger} from '@angular/animations';
import {CollectionChooserComponent} from '../../../../core-ui-module/components/collection-chooser/collection-chooser.component';
import {NodeHelperService} from '../../../../core-ui-module/node-helper.service';

@Component({
  selector: 'es-workspace-share-history',
  templateUrl: 'history.component.html',
  styleUrls: ['history.component.scss'],
  animations: [
      trigger('fade', UIAnimation.fade()),
      trigger('cardAnimation', UIAnimation.cardAnimation())
  ]
})
export class WorkspaceHistoryComponent {
  public history: any;
  private STATUS_SAME= 0;
  private STATUS_ADD= 1;
  private STATUS_CHANGE= 2;
  public loading: boolean;
  _node: Node;
  @Input() set node(node: Node){
    this._node=node;
    this.loading = true;
    this.nodeApi.getNodePermissionsHistory(node.ref.id).subscribe((data: NodePermissionsHistory[]) => {
      this.processHistory(data);
      this.loading = false;
    }, (error: any) => {
      this.toast.error(error);
      this.close();
    });
  }
  @Output() onClose= new EventEmitter();
  buttons: DialogButton[];

  constructor(private nodeApi: RestNodeService,
              private toast: Toast,
              private nodeHelper: NodeHelperService,
              private translation: TranslateService) {
    this.buttons = DialogButton.getSingleButton('CLOSE', () => this.close(), 'standard');
  }

  public close(){
    this.onClose.emit();
  }
  private processHistory(history: NodePermissionsHistory[]) {
    this.history = [];
    let i = 0;
    for (const entry of history){
      const info: any = {};
      info.added = [];
      info.modified = [];

      if (i < history.length - 1) {
        info.removed = this.getRemoved(entry.permissions.permissions, history[i + 1].permissions.permissions);
      }else {
        info.removed = [];
      }

      info.date = DateHelper.formatDate(this.translation, entry.date);
      info.user = this.nodeHelper.getUserDisplayName(entry.user);
      for (const permission of entry.permissions.permissions){
        if (i < history.length - 1){
          const result = this.getPermissionStatus(permission, history[i + 1]);
          const status = result.status;
          if (status == this.STATUS_SAME){

          }
          else if (status == this.STATUS_CHANGE){
            info.modified.push(this.convertPermission(permission, result.permissions));
          }
          else if (status == this.STATUS_ADD){
            info.added.push(this.convertPermission(permission));
          }
        }
        else{
          info.added.push(this.convertPermission(permission));
        }
      }
      if (info.added.length || info.modified.length || info.removed.length) {
        this.history.push(info);
      }
      i++;
    }
  }
  private getRemoved(permissionNew: Permission[], permissionOld: Permission[]): Permission[] {
    const removed: Permission[] = [];
    for (const pOld of permissionOld){
      let isRemoved = true;
      for (const pNew of permissionNew){
        if (pNew.authority.authorityName === pOld.authority.authorityName){
          isRemoved = false;
        }
      }
      if (!isRemoved)
        continue;
      removed.push(this.convertPermission(pOld));
    }
    return removed;
  }
  private getPermissionStatus(permissionNew: Permission, permissionOld: NodePermissionsHistory): any{
    for (const permission of permissionOld.permissions.permissions){
      if (permission.authority.authorityName === permissionNew.authority.authorityName){
        if (!Helper.arrayEquals(permission.permissions, permissionNew.permissions)){
          return {status: this.STATUS_CHANGE, permissions: permission.permissions};
        }
        return {status: this.STATUS_SAME};
      }

    }
    return {status: this.STATUS_ADD};
  }

  private convertPermission(p: Permission, oldPermissions: string[]= null) {
    const object: any = {};
    if (p.user)
      object.name = (p.user.firstName + ' ' + p.user.lastName).trim();
    else if (p.group)
      object.name = p.group.displayName;
    else if (p.authority.authorityName === RestConstants.AUTHORITY_EVERYONE){
      object.name = this.translation.instant('GROUP_EVERYONE');
    }
    else{
      object.name = p.authority.authorityName;
    }
    p.permissions = this.cleanupPermissions(p.permissions);
    oldPermissions = this.cleanupPermissions(oldPermissions);
    const list: string[] = [];
    for (const perm of p.permissions){
      list.push(this.translation.instant('PERMISSION_TYPE.' + perm));
    }
    object.permissions = list.join(', ');
    if (oldPermissions) {
      const list: string[] = [];
      for (const perm of oldPermissions) {
        list.push(this.translation.instant('PERMISSION_TYPE.' + perm));
      }
      object.oldPermissions = list.join(', ');
    }
    return object;

  }

    private cleanupPermissions(permissions: string[]) {
        if (permissions == null)
          return permissions;
        const all = permissions.indexOf(RestConstants.PERMISSION_ALL);
        if (all != -1){
          permissions.splice(all, 1, RestConstants.PERMISSION_COORDINATOR);
        }
        const coord = permissions.indexOf(RestConstants.PERMISSION_COORDINATOR);
        const collab = permissions.indexOf(RestConstants.PERMISSION_COLLABORATOR);
        const consumer = permissions.indexOf(RestConstants.PERMISSION_CONSUMER);
        if (coord != -1){
          if (collab != -1)
            permissions.splice(collab, 1);
          if (consumer != -1)
            permissions.splice(consumer, 1);
        }
        else if (collab != -1){
          if (consumer != -1)
            permissions.splice(consumer, 1);
        }
        return permissions;
    }
}
