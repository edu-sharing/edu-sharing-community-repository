import {Component, Input, EventEmitter, Output} from '@angular/core';
import {RestNodeService} from "../../../common/rest/services/rest-node.service";
import {
  Node, NodeList, NodeWrapper, NodePermissions, NodeVersions, UsageList,
  Version, LoginResult, IamUser, Permission
} from "../../../common/rest/data-object";
import {RestConstants} from "../../../common/rest/rest-constants";
import {TranslateService} from "@ngx-translate/core";
import {NodeHelper} from "../../../common/ui/node-helper";
import {RestUsageService} from "../../../common/rest/services/rest-usage.service";
import {DialogButton} from "../../../common/ui/modal-dialog/modal-dialog.component";
import {Toast} from "../../../common/ui/toast";
import {RestConnectorService} from "../../../common/rest/services/rest-connector.service";
import {RestIamService} from "../../../common/rest/services/rest-iam.service";
import {ConfigurationService} from "../../../common/services/configuration.service";
import {VCard} from "../../../common/VCard";
import {RestHelper} from "../../../common/rest/rest-helper";
import {Router} from "@angular/router";
import {UIHelper} from "../../../common/ui/ui-helper";
import {UIConstants} from "../../../common/ui/ui-constants";
import {ListItem} from "../../../common/ui/list-item";

@Component({
  selector: 'workspace-metadata',
  templateUrl: 'metadata.component.html',
  styleUrls: ['metadata.component.scss']
})
export class WorkspaceMetadataComponent  {
  private _node : string;
  public loading=true;
  public data : any;
  private INFO="INFO";
  private PROPERTIES="PROPERTIES";
  private VERSIONS="VERSIONS";
  private tab=this.INFO;
  private permissions : any;
  private usageCount : number;
  private nodeObject : Node;
  private versions : Version[];
  private versionsLoading=false;
  @Input() isAdmin:boolean;
  @Input() set node(node : string){
    this._node=node;
    this.load();
  }
  @Output() onEditMetadata=new EventEmitter();
  @Output() onDownload=new EventEmitter();
  @Output() onDisplay=new EventEmitter();
  @Output() onClose=new EventEmitter();
  @Output() onRestore=new EventEmitter();
  private load(){
    this.versions=null;
    this.loading=true;
    this.versionsLoading=true;
    this.data=null;
    let currentNode=this._node;
    this.nodeApi.getNodeMetadata(this._node,[RestConstants.ALL]).subscribe((data : NodeWrapper) => {
      if(currentNode!=this._node)
        return;
      console.log(data);
      this.nodeObject=data.node;
      if(this.nodeObject.isDirectory)
        this.tab = this.INFO;


      this.data=this.format(data.node);
      this.loading=false;
    });

    this.nodeApi.getNodeVersions(this._node).subscribe((data : NodeVersions)=>{
      if(currentNode!=this._node)
        return;
      this.versions=data.versions.reverse();
      for(let version of this.versions) {
        if(version.comment){
          if(version.comment==RestConstants.COMMENT_MAIN_FILE_UPLOAD || version.comment.startsWith(RestConstants.COMMENT_EDITOR_UPLOAD)) {
            let parameters = version.comment.split(",");
            let editor = "";
            if (parameters.length > 1)
              editor = this.translate.instant("CONNECTOR." + parameters[1] + ".NAME");
            version.comment = this.translate.instant('WORKSPACE.METADATA.COMMENT.' + parameters[0], {editor: editor});
          }
        }
      }
        let i=0;
      for(let version of this.versions){
        if(this.isCurrentVersion(version)){
          this.versions.splice(i,1);
          this.versions.splice(0,0,version);
          break;
        }
        i++;
      }
      this.versionsLoading=false;
    });
    this.iamApi.getUser().subscribe((login:IamUser)=>{
      this.nodeApi.getNodePermissions(this._node).subscribe((data : NodePermissions) => {
        this.permissions=this.formatPermissions(login,data);
      });
    });

    this.usageApi.getNodeUsages(this._node).subscribe((data : UsageList) =>{
      this.usageCount=data.usages.length;
    });
  }
  private isCurrentVersion(version : Version) : boolean{
    if(!this.nodeObject)
      return false;
    let prop=this.nodeObject.properties[RestConstants.LOM_PROP_LIFECYCLE_VERSION];
    if(!prop)
      return false;

    return prop[0]==(version.version.major+"."+version.version.minor);
  }
  private setTab(tab : string){
    this.tab=tab;
  }
  private display(version:string=null){
    this.nodeObject.version=version;
    this.onDisplay.emit(this.nodeObject);
  }
  private openPermalink(){
    this.router.navigate([UIConstants.ROUTER_PREFIX+"render",this.nodeObject.ref.id]);
  }
  private displayVersion(version : Version){
    if(this.isCurrentVersion(version))
      this.display();
    else
      this.display(version.version.major+"."+version.version.minor);
  }
  private format(node: Node) : any{
    let data : any = {};
    data["name"]=node.name;
    data["title"]=node.title;
    data["isDirectory"]=node.isDirectory;
    data["isCollection"]=node.collection!=null;
    data["description"]=node.description;
    data["preview"]=node.preview.url;
    data["keywords"]=node.properties[RestConstants.LOM_PROP_GENERAL_KEYWORD];
    if(data["keywords"] && data["keywords"].length==1 && !data["keywords"][0])
      data["keywords"]=null;
    //data["creator"]=node.properties[RestConstants.CM_CREATOR];
    data["creator"]=RestHelper.getPersonWithConfigDisplayName(node.createdBy,this.config);
    data["createDate"]=NodeHelper.getNodeAttribute(this.translate,this.config,node,new ListItem("NODE",RestConstants.CM_PROP_C_CREATED));
    data["author"]=this.toVCards(node.properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR]).join(", ");
    data["author_freetext"]=node.properties[RestConstants.CCM_PROP_AUTHOR_FREETEXT] ? node.properties[RestConstants.CCM_PROP_AUTHOR_FREETEXT][0] : null;
    data["mediatype"]=node.mediatype=="file" ? node.mimetype : node.mediatype;
    data["mimetype"]=node.mimetype;
    data["size"]=node.size;
    if(node.properties[RestConstants.EXIF_PROP_DATE_TIME_ORIGINAL])
      data["exifDate"]=NodeHelper.getNodeAttribute(this.translate,this.config,node,new ListItem("NODE",RestConstants.EXIF_PROP_DATE_TIME_ORIGINAL));

    data["dimensions"]=NodeHelper.getNodeAttribute(this.translate,this.config,node,new ListItem("NODE",RestConstants.DIMENSIONS));

    data["license"]=NodeHelper.getLicenseIcon(node);
    data["licenseName"]=NodeHelper.getLicenseName(node,this.translate);

    data["properties"]=[];
    for(let k in node.properties) {
      data["properties"].push([k, node.properties[k].join(", ")]);
    }
    return data;
  }
  public close(){
    this.onClose.emit();
  }
  private edit(){
    this.onEditMetadata.emit(this.nodeObject);
  }
  constructor(private translate : TranslateService,
              private config : ConfigurationService,
              private router: Router,
              private iamApi : RestIamService,
              private nodeApi : RestNodeService,
              private usageApi : RestUsageService,
              private toast : Toast) {
  }
  private restoreVersion(restore : Version){
    this.onRestore.emit(restore);
  }
  private formatPermissions(login:IamUser,permissions: NodePermissions) : any{
    let data : any = {};
    data["users"]=[];
    data["groups"]=[];
    if(!permissions.permissions)
      return data;
    for(let permission of permissions.permissions.inheritedPermissions){
      if(permission.authority.authorityName==login.person.authorityName || permission.authority.authorityType==RestConstants.AUTHORITY_TYPE_OWNER){

      }
      else if(permission.authority.authorityType==RestConstants.AUTHORITY_TYPE_USER){
        data["users"].push(permission);
      }
      else{
        data["groups"].push(permission);
      }
    }
    for(let permission of permissions.permissions.localPermissions.permissions){
      if(permission.authority.authorityName==login.person.authorityName || permission.authority.authorityType==RestConstants.AUTHORITY_TYPE_OWNER){

      }
      else if(permission.authority.authorityType==RestConstants.AUTHORITY_TYPE_USER){
        if(!this.containsPermission(data["groups"],permission))
          data["users"].push(permission);
      }
      else{
        if(!this.containsPermission(data["groups"],permission))
          data["groups"].push(permission);
      }
    }
    return data;
  }

  private toVCards(properties: any[]) {
    let vcards:string[]=[];
    if(properties) {
      for (let p of properties) {
        vcards.push(new VCard(p).getDisplayName());
      }
    }
    return vcards;

  }

  private containsPermission(permissions: Permission[], permission: Permission) {
    for(let perm of permissions){
      if(perm.authority.authorityName==permission.authority.authorityName)
        return true;
    }
    return false;
  }
}
