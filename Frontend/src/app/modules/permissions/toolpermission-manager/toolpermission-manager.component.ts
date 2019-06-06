import {Component, EventEmitter, Input, Output} from "@angular/core";
import {
    RestAdminService,
    RestConstants,
    RestIamService,
    RestNodeService,
    ToolPermission
} from '../../../core-module/core.module';
import {Toast} from "../../../core-ui-module/toast";
import {TranslateService} from "@ngx-translate/core";
import {UIAnimation} from "../../../core-module/ui/ui-animation";
import {Helper} from "../../../core-module/rest/helper";
import {trigger} from "@angular/animations";
import {AuthorityNamePipe} from '../../../common/ui/authority-name.pipe';

@Component({
  selector: 'toolpermission-manager',
  templateUrl: 'toolpermission-manager.component.html',
  styleUrls: ['toolpermission-manager.component.scss'],
  animations: [
      trigger('fade', UIAnimation.fade()),
      trigger('cardAnimation', UIAnimation.cardAnimation())
  ]
})
export class ToolpermissionManagerComponent {
  isLoading=false;
  addName="";
  creatingToolpermission=false;
  static STATUS_ALLOWED="ALLOWED";
  static STATUS_DENIED="DENIED";
  static STATUS_UNDEFINED="UNDEFINED";
  static STATUS_UNKNOWN="UNKNOWN";
  static GROUPS:any=[{name:"SHARING",icon:"share",permissions:[
        RestConstants.TOOLPERMISSION_INVITE,
        RestConstants.TOOLPERMISSION_INVITE_STREAM,
        RestConstants.TOOLPERMISSION_INVITE_LINK,
        RestConstants.TOOLPERMISSION_INVITE_SHARE,
        RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH,
        RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE,
        RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_FUZZY,
        RestConstants.TOOLPERMISSION_INVITE_HISTORY,
    ]},
    {name:"LICENSING",icon:"copyright",permissions:[
        RestConstants.TOOLPERMISSION_INVITE_ALLAUTHORITIES,
        RestConstants.TOOLPERMISSION_LICENSE,
    ]},
    {name:"DATA_MANAGEMENT",icon:"folder",permissions:[
        RestConstants.TOOLPERMISSION_WORKSPACE,
        RestConstants.TOOLPERMISSION_UNCHECKEDCONTENT
    ]},
    {name:"SAFE",icon:"lock",permissions:[
        RestConstants.TOOLPERMISSION_CONFIDENTAL,
        RestConstants.TOOLPERMISSION_INVITE_SAFE,
        RestConstants.TOOLPERMISSION_INVITE_SHARE_SAFE,
        RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SAFE,
        RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE_SAFE,
    ]},
    {name:"COLLECTIONS",icon:"layers",permissions:[
      RestConstants.TOOLPERMISSION_COLLECTION_EDITORIAL,
        RestConstants.TOOLPERMISSION_COLLECTION_CURRICULUM,
        RestConstants.TOOLPERMISSION_COLLECTION_PINNING,
    ]},
    {name:"OTHER",icon:"help"}
  ];
  changing: string[]=[];
  getGroups(){
    return ToolpermissionManagerComponent.GROUPS;
  }
  getToolpermissionsForGroup(group:any){
    if(group.permissions){
      return group.permissions;
    }
    let permissions=Object.keys(this.permissions);
    for(let group of ToolpermissionManagerComponent.GROUPS){
      if(group.permissions){
        for(let tp of group.permissions){
          let pos=permissions.indexOf(tp);
          if(pos!=-1) {
              permissions.splice(pos, 1);
          }
        }
      }
    }
    return permissions;
  }
  private _authority: any;
  name:string;
  @Input() set authority(authority:any){
    if(authority==null)
      return;
    this._authority=authority;
    this.isLoading=true;
    this.name=new AuthorityNamePipe(this.translate).transform(authority,null);
    this.refresh();
  }
  @Output() onClose = new EventEmitter();
  permissions: ToolPermission|any;
  allow: any;
  allowInit: any;
  deny: any;
  denyInit: any;

  constructor(private toast: Toast,
              private admin : RestAdminService,
              private node : RestNodeService,
              private translate : TranslateService,
              private iam: RestIamService) {

  }
  close(){
    this.onClose.emit();
  }
  change(key:string){
    this.changing.push(key);
    this.admin.setToolpermissions(this._authority.authorityName,this.getPermissions()).subscribe(()=>{
        /*this.toast.toast('PERMISSIONS.TOOLPERMISSIONS.SAVED');
        this.close();*/
        this.refresh(()=>{
            let i=this.changing.indexOf(key);
            if(i!=-1){
                this.changing.splice(i,1);
            }
        });
    },(error:any)=>{
        this.toast.error(error);
    });
  }
  getEffective(key:string) {
    if(this.deny[key]){
      return ToolpermissionManagerComponent.STATUS_DENIED;
    }
    if(this.allow[key] && this.permissions[key].effective!=ToolpermissionManagerComponent.STATUS_DENIED){
      return ToolpermissionManagerComponent.STATUS_ALLOWED;
    }
    if(!this.denyInit[key] && this.permissions[key].effective==ToolpermissionManagerComponent.STATUS_DENIED){
      return ToolpermissionManagerComponent.STATUS_DENIED;
    }
    if(this.allow[key]!=this.allowInit[key] || this.deny[key]!=this.denyInit[key]) {
      return ToolpermissionManagerComponent.STATUS_UNKNOWN;
    }
    return this.permissions[key].effective;
  }
  isImplicit(key:string) {
    if(this._authority.authorityType==RestConstants.AUTHORITY_TYPE_EVERYONE){
        return false;
    }
    if(this.getEffective(key)==ToolpermissionManagerComponent.STATUS_UNDEFINED)
        return false;
    if(this.deny[key]){
        return false;
    }
    if(this.allow[key] && this.permissions[key].effective==ToolpermissionManagerComponent.STATUS_DENIED){
        return true;
    }
    return !this.allow[key];
  }
  getImplicitDetail(key:string){
      let names=[];
      for(let group of this.permissions[key].effectiveSource){
          if(group.authorityType==RestConstants.AUTHORITY_TYPE_EVERYONE){
              names.push(this.translate.instant("PERMISSIONS.TOOLPERMISSIONS.EVERYONE_ALLOWED"));
          }
          else{
              names.push(new AuthorityNamePipe(this.translate).transform(group, null));
          }
      }
      return this.translate.instant("PERMISSIONS.TOOLPERMISSIONS.INHERIT_DETAIL",{memberships:names.join(", ")});
  }

  private getPermissions() {
      let result:any={};
      for(let key in this.permissions){
        if(this.allow[key]){
          result[key]=ToolpermissionManagerComponent.STATUS_ALLOWED;
        }
        else if(this.deny[key]){
          result[key]=ToolpermissionManagerComponent.STATUS_DENIED;
        }
      }
      return result;
  }

    private refresh(callback:Function=null) {
        this.admin.getToolpermissions(this._authority.authorityName).subscribe((data:any)=>{
            this.isLoading=false;
            this.permissions=data;
            this.allow={};
            this.deny={};
            for(let key in this.permissions){
                let value=this.permissions[key].explicit;
                this.allow[key]=value==ToolpermissionManagerComponent.STATUS_ALLOWED;
                this.deny[key]=value==ToolpermissionManagerComponent.STATUS_DENIED;
            }
            this.allowInit=Helper.deepCopy(this.allow);
            this.denyInit=Helper.deepCopy(this.deny);
            if(callback) callback();
        },(error:any)=>{
            this.toast.error(error);
            this.close();
        });
    }
    createToolpermission(){
        this.creatingToolpermission=true;
        this.admin.addToolpermission(this.addName).subscribe(()=>{
            this.toast.toast("PERMISSIONS.TOOLPERMISSIONS.ADDED",{name:this.addName});
            this.addName="";
            this.creatingToolpermission=false;
            this.refresh();
        },(error)=>{
            this.creatingToolpermission=false;
            this.toast.error(error);
        })
    }
}
