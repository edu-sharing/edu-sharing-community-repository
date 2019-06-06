import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {DialogButton, RestConnectorService} from "../../../core-module/core.module";
import {Toast} from "../../../core-ui-module/toast";
import {RestNodeService} from "../../../core-module/core.module";
import {RestConstants} from "../../../core-module/core.module";
import {NodeWrapper, Node, NodePermissions, LocalPermissionsResult, Permission} from "../../../core-module/core.module";
import {TranslateService} from "@ngx-translate/core";
import {NodeHelper} from "../../../core-ui-module/node-helper";
import {ConfigurationService} from "../../../core-module/core.module";
import {RestHelper} from "../../../core-module/core.module";
import {VCard} from "../../../core-module/ui/VCard";
import {UIHelper} from "../../../core-ui-module/ui-helper";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../../../core-module/ui/ui-animation";
import {UIService} from '../../../core-module/core.module';

@Component({
  selector: 'workspace-license',
  templateUrl: 'license.component.html',
  styleUrls: ['license.component.scss'],
  animations: [
    trigger('fade', UIAnimation.fade()),
    trigger('cardAnimation', UIAnimation.cardAnimation())
  ]
})
export class WorkspaceLicenseComponent  {
  @ViewChild('contactCheckbox') contactCheckbox : ElementRef;
  @ViewChild('releaseCheckbox') releaseCheckbox : ElementRef;
  @ViewChild('selectLicense') selectLicense : ElementRef;

  /**
   * priority, useful if the dialog seems not to be in the foreground
   * Values greater 0 will raise the z-index
   * Default is 1 for mds
   */
  @Input() priority = 1;
  _type="";
  _properties: any;
  private doiPermission: boolean;
  private doiActive: boolean;
  private doiDisabled: boolean;
  public set type(type:string){
    this._type=type;
    if(this._type=='CC_0' && !this.cc0Type)
      this.cc0Type='CC_0';
  }
  public get type(){
    return this._type;
  }
  public ccShare="";
  private ccCommercial="";
  private ccTitleOfWork="";
  private ccSourceUrl="";
  private ccVersion="4.0";
  private ccLocale="";
  private cc0Type="";
  private ccProfileUrl="";
  private copyrightType="COPYRIGHT_FREE";
  private eduType="P_NR";
  private rightsDescription="";
  private showCcAuthor=false;
  private contact=true;
  private release=false;
  private eduDownload=true;
  dialogTitle : string;
  dialogMessage : string;
  dialogCancel : Function;
  dialogButtons : DialogButton[];

  private _oerMode=true;
  public set oerMode(oerMode:boolean){
    this._oerMode=oerMode;
    this.showCcAuthor=false;
  }
  public get oerMode(){
    return this._oerMode;
  }
  private ccLocales=["ar","be","bg","de","fi","fr",
                     "in","it","ca","hr","mt","mk","nl",
                     "no","pl","pt","ro","es","th",
                     "uk","hu"];
  public ALL_LICENSE_TYPES=["NONE","CC_0","CC_BY","SCHULFUNK","UNTERRICHTS_UND_LEHRMEDIEN","COPYRIGHT","CUSTOM"];
  public licenseMainTypes:string[];
  _nodes:Node[];
  private permissions: LocalPermissionsResult;
  public loading=true;
  private allowedLicenses: string[];
  private releaseMulti: string;
  public authorTab=0;
  public authorVCard:VCard;
  public authorFreetext:string;
  private allowRelease = true;
  public isAllowedLicense(license:string){
    return this.allowedLicenses==null || this.allowedLicenses.indexOf(license)!=-1;
  }
  public isOerLicense(){
    return this.getLicenseProperty()=="CC_0" || this.getLicenseProperty()=="PDM"
      || this.getLicenseProperty()=="CC_BY" || this.getLicenseProperty()=="CC_BY_SA";
  }
  @Input() set properties(properties : any){
    this.loadConfig();
    this._properties = properties;
    this.readLicense();
    this.loading=false;
  }
  public loadNodes(nodes:Node[],callback:Function,pos=0){
    if(pos==nodes.length){
      callback();
      return;
    }
    this.nodeApi.getNodeMetadata(nodes[pos].ref.id,[RestConstants.ALL]).subscribe((node)=>{
      this._nodes.push(node.node);
      this.loadNodes(nodes,callback,pos+1);
    },(error)=>{
      this.toast.error(error);
      this.cancel();
    });
  }
  @Input() set nodes(nodes : Node[]){
      this._nodes=[];
      this.loadNodes(nodes,()=>{
          this.loadConfig();
          this.checkAllowRelease();
          this.readLicense();
          this.loading=false;
          this.releaseMulti=null;
          let i=0;
          for(let node of this._nodes) {
              i++;
              this.nodeApi.getNodePermissions(node.ref.id).subscribe((permissions: NodePermissions) => {
                  this.permissions = permissions.permissions.localPermissions;
                  this.readPermissions(i==this._nodes.length);
                  if(this._nodes.length==1) {
                      this.doiActive = NodeHelper.isDOIActive(node, permissions.permissions);
                      this.doiDisabled = this.doiActive;
                  }
              });
          }
      });
  }
    private loadConfig() {
        this.config.get("allowedLicenses").subscribe((data: string[]) => {
            if (!data) {
                this.licenseMainTypes = this.ALL_LICENSE_TYPES;
                this.allowedLicenses = null;
            }
            else {
                this.licenseMainTypes = [];
                this.allowedLicenses = data;
                for (let entry of data) {
                    if (entry.startsWith("CC_BY")) {
                        if (this.licenseMainTypes.indexOf("CC_BY") == -1)
                            this.licenseMainTypes.push("CC_BY");
                    }
                    else if (entry == "CC_0" || entry == "PDM") {
                        if (this.licenseMainTypes.indexOf("CC_0") == -1)
                            this.licenseMainTypes.push("CC_0");
                    }
                    else if (entry.startsWith("COPYRIGHT")) {
                        this.licenseMainTypes.push("COPYRIGHT");
                        if (data.indexOf(this.copyrightType) == -1)
                            this.copyrightType = entry;
                    }
                    else if (this.ALL_LICENSE_TYPES.indexOf(entry) != -1) {
                        this.licenseMainTypes.push(entry);
                    }
                }
            }
            for(let license of this.config.instant("customLicenses",[])){
                this.licenseMainTypes.splice(license.position>=0 ? license.position : this.licenseMainTypes.length-license.position,0,license.id);
            }
        });
    }
  @Output() onCancel=new EventEmitter();
  @Output() onLoading=new EventEmitter();
  @Output() onDone=new EventEmitter();
  @Output() openContributor=new EventEmitter();
  constructor(
    private connector : RestConnectorService,
    private translate : TranslateService,
    private config : ConfigurationService,
    private ui : UIService,
    private toast : Toast,
    private nodeApi : RestNodeService) {
      this.connector.hasToolPermission(RestConstants.TOOLPERMISSION_HANDLESERVICE).subscribe((has:boolean)=>this.doiPermission=has);
  }
  public cancel(){
    this.onCancel.emit();
  }

  public saveLicense(callback:Function=null){
    if(this._properties){
        this.onDone.emit(this.getProperties(this._properties));
        return;
    }
    if(!this.getLicenseProperty() && this.release){
      //this.toast.error(null,'WORKSPACE.LICENSE.RELEASE_WITHOUT_LICENSE');
      //return;
    }
    let prop:any={};

    prop=this.getProperties(prop);
    let i=0;
    this.onLoading.emit(true);
    for(let node of this._nodes) {
      let authors=this._nodes[i].properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR];
      if(!authors)
          authors=[];
      authors[0]=this.authorVCard.toVCardString();
      prop[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR]=authors;
      i++;
      this.nodeApi.editNodeMetadataNewVersion(node.ref.id,RestConstants.COMMENT_LICENSE_UPDATE, prop).subscribe(() => {
        this.savePermissions(node);
        if(i==this._nodes.length){
          this.toast.toast('WORKSPACE.TOAST.LICENSE_UPDATED');
          this.onLoading.emit(false);
          this.onDone.emit(prop);
          if(callback)
            callback();
        }
      }, (error: any) => {
        this.onLoading.emit(false);
        this.toast.error(error);
      });
    }
  }
  private getValueForAll(prop:string,fallbackNotIdentical:any="",fallbackIsEmpty=fallbackNotIdentical,asArray=false){
    let found=null;
    let foundAny=false;
    if(this._properties){
      return this._properties[prop] ? this._properties[prop][0] : fallbackIsEmpty;
    }
    for(let node of this._nodes){
      let v=node.properties[prop];
      let value=v ? asArray ? v : v[0] : fallbackIsEmpty;
      if(foundAny && found!=value)
        return fallbackNotIdentical;
      found=value;
      foundAny=true;
    }
    if(!foundAny)
      return fallbackIsEmpty;
    return found;
  }
  private readLicense() {
    let license=this.getValueForAll(RestConstants.CCM_PROP_LICENSE,"MULTI","NONE");
    if(!license)
      license="NONE";
    this.type=license;
    if(license.startsWith("CC_BY")){
      this.type="CC_BY";
      if(license.indexOf("SA")!=-1)
        this.ccShare="SA";
      if(license.indexOf("ND")!=-1)
        this.ccShare="ND";
      if(license.indexOf("NC")!=-1)
        this.ccCommercial="NC";

      this.ccTitleOfWork=this.getValueForAll(RestConstants.CCM_PROP_LICENSE_TITLE_OF_WORK);
      this.ccSourceUrl=this.getValueForAll(RestConstants.CCM_PROP_LICENSE_SOURCE_URL);
      this.ccProfileUrl=this.getValueForAll(RestConstants.CCM_PROP_LICENSE_PROFILE_URL);
      this.ccVersion=this.getValueForAll(RestConstants.CCM_PROP_LICENSE_CC_VERSION,this.ccVersion);
      this.ccLocale=this.getValueForAll(RestConstants.CCM_PROP_LICENSE_CC_LOCALE);
    }
    if(license=='CC_0'){
      this.cc0Type='CC_0';
      this.type='CC_0';
    }
    if(license=='PDM'){
      this.cc0Type='PDM';
      this.type='CC_0';
    }
    if(license.startsWith("COPYRIGHT")){
      this.type="COPYRIGHT";
      this.copyrightType=license;
    }
    if(license=='SCHULFUNK'){
      this.type=license;
    }
    if(license.startsWith("EDU")){
      this.type="EDU";
      if(license.indexOf("P_NR")!=-1)
        this.eduType='P_NR';
        if(license.indexOf("NC")!=-1)
          this.eduType='NC';
        this.eduDownload=license.indexOf("ND")==-1;
    }
    if(license=='CUSTOM')
      this.type=license;

    this.rightsDescription=this.getValueForAll(RestConstants.LOM_PROP_RIGHTS_DESCRIPTION);
    let contactState=this.getValueForAll(RestConstants.CCM_PROP_QUESTIONSALLOWED,"multi","true");
    this.contact=contactState=='true' || contactState==true;
    this.oerMode=this.isOerLicense() || this.type=='NONE';
    this.authorVCard=new VCard(this.getValueForAll(RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR));
    this.authorFreetext=this.getValueForAll(RestConstants.CCM_PROP_AUTHOR_FREETEXT);
    UIHelper.invalidateMaterializeTextarea('authorFreetext');
    UIHelper.invalidateMaterializeTextarea('licenseRights');
    if(this.authorVCard.isValid())
      this.authorTab=1;
    setTimeout(()=>{
      if(contactState=='multi')
        this.contactCheckbox.nativeElement.indeterminate=true;
    },10);
  }

  private getLicenseProperty(){
    let name=this.type;
    if(this.type=="NONE")
      return "";
    if(this.type=="CC_BY"){
      if(this.ccCommercial)
        name+="_"+this.ccCommercial;
      if(this.ccShare)
        name+="_"+this.ccShare;

      return name;
    }
    if(this.type=="CC_0"){
      return this.cc0Type;
    }
    if(this.type=='COPYRIGHT'){
      return this.copyrightType;
    }
    if(this.type=="EDU"){
      name+="_"+this.eduType;
      if(!this.eduDownload)
        name+="_ND";
    }

    return name;
  }
  private getLicenseName(){
    return NodeHelper.getLicenseNameByString(this.getLicenseProperty(),this.translate);
  }
  private getLicenseUrl(){
    return NodeHelper.getLicenseUrlByString(this.getLicenseProperty(),this.ccVersion);
  }
  private getLicenseUrlVersion(type:string){
      return NodeHelper.getLicenseUrlByString(type,this.ccVersion);
  }
  private getLicenseIcon(){
    return NodeHelper.getLicenseIconByString(this.getLicenseProperty(),this.connector);
  }
  private savePermissions(node:Node){
    if(this.releaseCheckbox.nativeElement.indeterminate){
      return;
    }
    let add=true;
    let index=0;
    for(let perm of this.permissions.permissions){
      if(perm.authority.authorityName==RestConstants.AUTHORITY_EVERYONE){
        add=false;
        if(perm.permissions.indexOf(RestConstants.ACCESS_CC_PUBLISH)==-1 && this.release){
          perm.permissions.push(RestConstants.ACCESS_CC_PUBLISH);
        } if(perm.permissions.indexOf(RestConstants.ACCESS_CONSUMER)==-1 && this.release){
          perm.permissions.push(RestConstants.ACCESS_CONSUMER);
        }
        /*if(perm.permissions.indexOf(RestConstants.ACCESS_CC_PUBLISH)!=-1 && !this.release){
          perm.permissions.splice(perm.permissions.indexOf(RestConstants.ACCESS_CC_PUBLISH),1);
        }
        if(perm.permissions.indexOf(RestConstants.ACCESS_CONSUMER)!=-1 && !this.release){
          perm.permissions.splice(perm.permissions.indexOf(RestConstants.ACCESS_CONSUMER),1);
        }
        */
        break;
      }
      index++;
    }
    // remove all_authorities
    if(!add && !this.release){
      this.permissions.permissions.splice(index,1);
    }
    // add all_authorities
    if(add && this.release){
      let perm=RestHelper.getAllAuthoritiesPermission();
      perm.permissions=[RestConstants.ACCESS_CC_PUBLISH,RestConstants.ACCESS_CONSUMER];
      this.permissions.permissions.push(perm);
    }
    let permissions=RestHelper.copyAndCleanPermissions(this.permissions.permissions,this.permissions.inherited);
    this.nodeApi.setNodePermissions(node.ref.id,permissions,false,"",false,this.doiPermission && this.doiActive && this.release).subscribe(()=>{
    },(error:any)=>this.toast.error(error));
  }
  private readPermissions(last:boolean) {
    this.release=false;
    console.log(this.permissions);
    if(this)
    for(let perm of this.permissions.permissions){
      if(perm.authority.authorityType==RestConstants.AUTHORITY_TYPE_EVERYONE && perm.permissions.indexOf(RestConstants.ACCESS_CC_PUBLISH)!=-1){
        if(this.releaseMulti!=null && this.releaseMulti!='true')
          this.releaseMulti='multi';
        else
          this.releaseMulti='true';
        if(last)
          this.setPermissionState();
        return;
      }
    }
    if(this.releaseMulti!=null && this.releaseMulti!='false')
      this.releaseMulti='multi';
    else
      this.releaseMulti='false';
    if(last)
      this.setPermissionState();
  }

  private setPermissionState() {
    if(this.releaseMulti=='true')
      this.release=true;
    if(this.releaseMulti==null || this.releaseMulti=='false')
      this.release=false;

    if(this.releaseMulti=='multi'){
      this.releaseCheckbox.nativeElement.indeterminate=true;
    }
  }

  private checkAllowRelease() {
    this.connector.hasToolPermission(RestConstants.TOOLPERMISSION_INVITE_ALLAUTHORITIES).subscribe((data:boolean)=>{
      if(!data) {
        this.allowRelease = false;
        return;
      }
      for(let node of this._nodes){
        if(node.access.indexOf(RestConstants.ACCESS_CHANGE_PERMISSIONS)==-1){
          this.allowRelease=false;
          return;
        }
      }
    });
  }
  allowDOI(){
      if(!this._nodes || this._nodes.length!=1)
          return false;
      return this.doiPermission && this.release;
  }
  setCCBy() {
    this.type='CC_BY';
    this.ccShare='';
    this.ccCommercial='';
  }
  setCC0() {
    this.type='CC_0';
    this.cc0Type='CC_0';
  }

    private getProperties(prop: any) {
        prop[RestConstants.CCM_PROP_LICENSE]=[this.getLicenseProperty()];
        if(!this.contactCheckbox.nativeElement.indeterminate)
            prop[RestConstants.CCM_PROP_QUESTIONSALLOWED]=[this.contact];
        if(this.type=='CC_BY'){
            if(this.ccTitleOfWork)
                prop[RestConstants.CCM_PROP_LICENSE_TITLE_OF_WORK]=[this.ccTitleOfWork];
            if(this.ccSourceUrl)
                prop[RestConstants.CCM_PROP_LICENSE_SOURCE_URL]=[this.ccSourceUrl];
            if(this.ccProfileUrl)
                prop[RestConstants.CCM_PROP_LICENSE_PROFILE_URL]=[this.ccProfileUrl];
            if(this.ccVersion)
                prop[RestConstants.CCM_PROP_LICENSE_CC_VERSION]=[this.ccVersion];
            if(this.ccLocale)
                prop[RestConstants.CCM_PROP_LICENSE_CC_LOCALE]=[this.ccLocale];
        }
        prop[RestConstants.CCM_PROP_AUTHOR_FREETEXT]=[this.authorFreetext];

        if(this.type=='CUSTOM') {
            prop[RestConstants.LOM_PROP_RIGHTS_DESCRIPTION] = [this.rightsDescription];
        }
        return prop;
    }

    openContributorDialog() {
        let nodes=this._nodes;
        this.saveLicense(()=>{
          console.log("open contr");
            this.openContributor.emit(nodes);
        });
    }

    changeRelease(release:boolean) {
        if(release){
            if(this.config.instant('publishingNotice',false)){
                this.dialogTitle='WORKSPACE.SHARE.PUBLISHING_WARNING_TITLE';
                this.dialogMessage='WORKSPACE.SHARE.PUBLISHING_WARNING_MESSAGE';
                this.dialogCancel=()=>{
                    this.dialogTitle=null;
                    this.release=false;
                };
                this.dialogButtons=DialogButton.getYesNo(()=>{
                    this.dialogCancel();
                }, ()=>{
                    this.release=true;
                    this.doiActive=true;
                    this.dialogTitle=null;
                });
                return;
            }
          this.doiActive=true;
        }
    }
}
