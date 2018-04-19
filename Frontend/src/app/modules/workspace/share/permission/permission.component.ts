import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {Permission} from "../../../../common/rest/data-object";
import {RestConstants} from "../../../../common/rest/rest-constants";

@Component({
  selector: 'workspace-share-permission',
  templateUrl: 'permission.component.html',
  styleUrls: ['permission.component.scss']
})
export class WorkspacePermissionComponent  {
  public _permission : Permission;
  public invalidPermission = false;
  public isEveryone: boolean;
  @Input() set permission(permission:Permission){
    this._permission=permission;
    let coordinator=permission.permissions.indexOf(RestConstants.PERMISSION_COORDINATOR);
    let collaborator=permission.permissions.indexOf(RestConstants.PERMISSION_COLLABORATOR);
    if(coordinator!=-1){
      let i=permission.permissions.indexOf(RestConstants.PERMISSION_COLLABORATOR);
      if(i!=-1)
        permission.permissions.splice(i,1);
    }
    if(coordinator!=-1 || collaborator!=-1){
      let i=permission.permissions.indexOf(RestConstants.PERMISSION_CONSUMER);
      if(i!=-1)
        permission.permissions.splice(i,1);
    }
    this.isEveryone=permission.authority.authorityName==RestConstants.AUTHORITY_EVERYONE;
    let check=this._permission.permissions.slice();
    if(check.indexOf(RestConstants.ACCESS_CC_PUBLISH)!=-1){
      check.splice(check.indexOf(RestConstants.ACCESS_CC_PUBLISH),1);
    }
    this.invalidPermission=check.length!=1 ||
      (check[0]!=RestConstants.PERMISSION_OWNER && check[0]!=RestConstants.PERMISSION_CONSUMER &&
       check[0]!=RestConstants.PERMISSION_COLLABORATOR && check[0]!=RestConstants.PERMISSION_COORDINATOR);
  }
  @Input() inherit = false;
  @Input() added = false;
  @Input() readOnly = true;
  @Input() showDelete = true;
  @Input() isDirectory = false;
  @Input() canPublish = true;
  @Output() onRemove = new EventEmitter();
  @Output() onType = new EventEmitter();

  public showChooseType=false;


  public remove(){
    if(this.showDelete)
      this.onRemove.emit();
  }
  public chooseType(){
    if(this.readOnly || this.isEveryone)
      return;
    this.showChooseType=true;
  }
  private changeType(type : any){
    this.onType.emit(type);
    if(type.wasMain)
      this.showChooseType=false;
  }
}
