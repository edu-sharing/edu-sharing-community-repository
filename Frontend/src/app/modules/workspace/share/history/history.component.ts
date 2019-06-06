import {Component, Input, EventEmitter, Output, ViewChild, ElementRef, HostListener} from '@angular/core';
import {Permission, NodePermissionsHistory} from "../../../../core-module/core.module";
import {Toast} from "../../../../core-ui-module/toast";
import {RestNodeService} from "../../../../core-module/core.module";
import {
  Node
} from "../../../../core-module/core.module";
import {NodeHelper} from "../../../../core-ui-module/node-helper";
import {TranslateService} from "@ngx-translate/core";
import {RestConstants} from "../../../../core-module/core.module";
import {Helper} from "../../../../core-module/rest/helper";
import {RestHelper} from "../../../../core-module/core.module";
import {DateHelper} from "../../../../core-ui-module/DateHelper";
import {UIAnimation} from '../../../../core-module/ui/ui-animation';
import {trigger} from '@angular/animations';
import {CollectionChooserComponent} from '../../../../core-ui-module/components/collection-chooser/collection-chooser.component';

@Component({
  selector: 'workspace-share-history',
  templateUrl: 'history.component.html',
  styleUrls: ['history.component.scss'],
  animations: [
      trigger('fade', UIAnimation.fade()),
      trigger('cardAnimation', UIAnimation.cardAnimation())
  ]
})
export class WorkspaceHistoryComponent {
  public history:any;
  private STATUS_SAME=0;
  private STATUS_ADD=1;
  private STATUS_CHANGE=2;
  public loading: boolean;
  @Input() set node (node : Node){
    this.loading=true;
    this.nodeApi.getNodePermissionsHistory(node.ref.id).subscribe((data:NodePermissionsHistory[])=>{
      this.processHistory(data);
      this.loading=false;
    },(error:any)=>{
      this.toast.error(error);
      this.close();
    });
  }
  @Output() onClose=new EventEmitter();

  constructor(private nodeApi : RestNodeService,
              private toast : Toast,
              private translation : TranslateService
  ){
  }

  public close(){
    this.onClose.emit();
  }
  private processHistory(history:NodePermissionsHistory[]) {
    console.log(history);
    this.history=[];
    let i=0;
    for(let entry of history){
      let info:any={};
      info.added=[];
      info.modified=[];

      if(i<history.length-1)
        info.removed=this.getRemoved(entry.permissions.permissions,history[i+1].permissions.permissions);
      else
        info.removed=[];

      info.date=DateHelper.formatDate(this.translation,entry.date);
      info.user=NodeHelper.getUserDisplayName(entry.user);
      for(let permission of entry.permissions.permissions){
        if(i<history.length-1){
          let result=this.getPermissionStatus(permission,history[i+1]);
          let status=result.status;
          if(status==this.STATUS_SAME){

          }
          else if(status==this.STATUS_CHANGE){
            info.modified.push(this.convertPermission(permission,result.permissions));
          }
          else if(status==this.STATUS_ADD){
            info.added.push(this.convertPermission(permission));
          }
        }
        else{
          info.added.push(this.convertPermission(permission));
        }
      }
      if(info.added.length || info.modified.length || info.removed.length) {
        this.history.push(info);
      }
      i++;
    }
  }
  private getRemoved(permissionNew:Permission[],permissionOld: Permission[]) : Permission[] {
    let removed:Permission[]=[];
    for(let pOld of permissionOld){
      let isRemoved=true;
      for(let pNew of permissionNew){
        if(pNew.authority.authorityName==pOld.authority.authorityName){
          isRemoved=false;
        }
      }
      if(!isRemoved)
        continue;
      removed.push(this.convertPermission(pOld));
    }
    return removed;
  }
  private getPermissionStatus(permissionNew:Permission,permissionOld: NodePermissionsHistory) : any{
    for(let permission of permissionOld.permissions.permissions){
      if(permission.authority.authorityName==permissionNew.authority.authorityName){
        if(!Helper.arrayEquals(permission.permissions,permissionNew.permissions)){
          return {status:this.STATUS_CHANGE,permissions:permission.permissions};
        }
        return {status:this.STATUS_SAME};
      }

    }
    return {status:this.STATUS_ADD};
  }

  private convertPermission(p: Permission,oldPermissions:string[]=null) {
    let object:any={};
    if(p.user)
      object.name=(p.user.firstName+" "+p.user.lastName).trim();
    else if(p.group)
      object.name=p.group.displayName;
    else if(p.authority.authorityName==RestConstants.AUTHORITY_EVERYONE){
      object.name=this.translation.instant('GROUP_EVERYONE');
    }
    else{
      object.name=p.authority.authorityName;
    }
    p.permissions=this.cleanupPermissions(p.permissions);
    oldPermissions=this.cleanupPermissions(oldPermissions);
    let list:string[]=[];
    for(let perm of p.permissions){
      list.push(this.translation.instant("PERMISSION_TYPE."+perm));
    }
    object.permissions=list.join(", ");
    if(oldPermissions) {
      let list:string[]=[];
      for (let perm of oldPermissions) {
        list.push(this.translation.instant("PERMISSION_TYPE." + perm));
      }
      object.oldPermissions=list.join(", ");
    }
    return object;

  }

    private cleanupPermissions(permissions: string[]) {
        if(permissions==null)
          return permissions;
        let all=permissions.indexOf(RestConstants.PERMISSION_ALL);
        if(all!=-1){
          permissions.splice(all,1,RestConstants.PERMISSION_COORDINATOR);
        }
        let coord=permissions.indexOf(RestConstants.PERMISSION_COORDINATOR);
        let collab=permissions.indexOf(RestConstants.PERMISSION_COLLABORATOR);
        let consumer=permissions.indexOf(RestConstants.PERMISSION_CONSUMER);
        if(coord!=-1){
          if(collab!=-1)
            permissions.splice(collab,1);
          if(consumer!=-1)
            permissions.splice(consumer,1);
        }
        else if(collab!=-1){
          if(consumer!=-1)
            permissions.splice(consumer,1);
        }
        return permissions;
    }
}
