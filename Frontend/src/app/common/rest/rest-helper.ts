/**
 * Different helper functions, may be used globally
 */

import {RestConstants} from "./rest-constants";
import {
  RestError, Node, Collection, Person, Permission, Permissions, User, MdsInfo,
  CollectionReference, LocalPermissions, Authority
} from "./data-object";
import {TranslateService} from "@ngx-translate/core";
import {DateHelper} from "../ui/DateHelper";
import {ConfigurationService} from "../services/configuration.service";
import {RestMdsService} from "./services/rest-mds.service";
import {ListItem} from "../ui/list-item";

export class RestHelper{
  public static getNodeIds(nodes : Node[]): Array<string>{
    let data=new Array<string>(nodes.length);
    for(let i=0;i<nodes.length;i++){
      data[i]=nodes[i].ref.id;
    }
    return data;
  }

  /**
   * Guess the mimetype for a HTML-File object
   * @param file
   * @returns {string}
   */
  public static guessMimeType(file: File) : string {
    let type=file.type;
    if(type=="application/x-zip-compressed")
      type="application/zip";
    return type;
  }

  public static errorClassContains(error:any,needle:string){
    try{
      let json=JSON.parse(error._body);
      return json.error.indexOf(needle)!=-1;
    }catch(e){}
    return false;
  }
  public static errorMessageContains(error:any,needle:string){
    try{
      let json=JSON.parse(error._body);
      return json.message.indexOf(needle)!=-1;
    }catch(e){}
    return false;
  }
  public static stringToNodes(data : string[]): Node[]{
    let nodes=new Array<Node>(data.length);
    for(let i=0;i<nodes.length;i++){
      nodes[i].ref.id=data[i];
    }
    return nodes;
  }
  public static getQueryString(queryName : string,values : string[]) : string{
    let query="";
    for(var i=0;i<values.length;i++){
      if(i)
        query+="&";
      query+=queryName+"="+encodeURIComponent(values[i]);
    }
    return query;
  }

  public static getQueryStringForList(queryName : string, nodes : Node[]|string[]) : string{
    if(nodes==null || nodes.length==0)
      return "";

    // TODO both types
    let list : string[];
    if(typeof nodes[0]=="string"){
      list=<string[]>nodes;
    }
    else{
      list=RestHelper.getNodeIds(<Node[]>nodes);
    }

    return this.getQueryString(queryName,list);

  }

  public static printError(error : any) : string {
    console.log(error);
    return error._body;
  }

  public static createNameProperty(name: string) : any{
    let property : any ={};
    property[RestConstants.CM_NAME]=[name];
    //property[RestConstants.CCM_FILENAME]=[name];
    return property;
  }

      // checks permission if its open to see for everybody
  public static  isSimplePermissionPublic(permissionObj:Permissions):boolean {

        var result:boolean = false;

        /* --> deactivated due to bug DESREPO-579
        // check inherited permissions (if active)
        if ((permissionObj.inheritedPermissions!=null) && (permissionObj.localPermissions.inherited)) {
            permissionObj.inheritedPermissions.forEach( permission => {
                if (permission.authority.authorityType == "EVERYONE") result=true;
            });
        }
        */

        // check local permissions
        if ((permissionObj.localPermissions!=null) && (permissionObj.localPermissions.permissions!=null)) {
            permissionObj.localPermissions.permissions.forEach( permission => {
                if (permission.authority.authorityType == "EVERYONE") result=true;
            });
        }

        return result;
   }

   // checks permission if its open to see by organizations
   public static isSimplePermissionOrganization(permissionObj:Permissions):boolean {

        var result:boolean = false;

        /* --> deactivated due to bug DESREPO-579
        // check inherited permissions (if active)
        if ((permissionObj.inheritedPermissions!=null) && (permissionObj.localPermissions.inherited)) {
            permissionObj.inheritedPermissions.forEach( permission => {
                if (permission.authority.authorityName.startsWith(RestConstants.GROUP_PREFIX)) result=true;
            });
        }
        */

        // check local permissions
       if ((permissionObj.localPermissions!=null) && (permissionObj.localPermissions.permissions!=null)) {
       permissionObj.localPermissions.permissions.forEach( permission => {
                if (permission.authority.authorityName.startsWith(RestConstants.GROUP_PREFIX)) result=true;
            } );
       }

        return result;
    }



    public static isUserAllowedToEdit(collection:Collection, person:User) : boolean {

        // if permissions missing default to false
        if (collection.permission==null) return false;

        // check access permissions on collection
        if (RestHelper.hasAccessPermission(collection, 'Write')) return true;

        // if nothing else matched - default to no right to edit
        return false;
    }

    public static hasAccessPermission(collection:Collection|CollectionReference, permission:string) : boolean {
        if (typeof collection.access == "undefined") return true;
        if (typeof collection.access == null) return true;
        return collection.access.indexOf(permission)!=-1;
    }


    public static isContentItem(node:Node):boolean {
        return ((node.type != null) && ((node.type == "ccm:io") || (node.type == "{http://www.campuscontent.de/model/1.0}io")));
    }

    public static isFolder(node:Node):boolean {
        return ((node.type != null) && ((node.type == "ccm:map") || (node.type == "{http://www.campuscontent.de/model/1.0}map")));
    }

    public static getName(node:Node):string {
        if (node.name) return node.name;
        return node.title;
    }
    public static getTitle(node:Node):string {
      if (node.title) return node.title;
      return node.name;
    }
    public static getCreatorName(node:Node):string {
        let result:string = "";
        if (node.createdBy!=null) {
            if (node.createdBy.firstName!=null) result = node.createdBy.firstName;
            if (node.createdBy.lastName!=null) result += " "+node.createdBy.lastName;
        }
        return result.trim();
    }

    public static getCreateTime(translation : TranslateService,node:Node):string {
      return DateHelper.formatDate(translation,DateHelper.convertDate(node.createdAt));
    }
    public static getModifiedTime(translation : TranslateService,node:Node):string {
      return DateHelper.formatDate(translation,DateHelper.convertDate(node.modifiedAt));
    }
    public static getPreviewUrl(node:Node):string {
        if ((node.preview!=null) && (node.preview.url!=null) && (node.preview.url.length>0)) {
            return node.preview.url;
        }
        return null;
    }

    public static gotProperty(node:Node, name:string):boolean {
        if (node.properties==null) return false;
        return (node.properties.hasOwnProperty(name));
    }

    public static getProperty(node:Node, name:string):string[] {
        if (node.properties==null) return null;
        if (!RestHelper.gotProperty(node,name)) return null;
        return node.properties[name];
    }


  static prepareMetadatasets(translate:TranslateService,mdsSets: MdsInfo[]) {
    for(let i=0;i<mdsSets.length;i++){
      if(mdsSets[i].id=="mds")
        mdsSets[i].name=translate.instant('DEFAULT_METADATASET');
    }
  }

  static getPersonWithConfigDisplayName(person: any, config: ConfigurationService) {
    let field=config.instant("userDisplayName","fullName");
    if(field=="authorityName"){
      if(person.authorityName==null)
        field="fullName";
      else
        return person.authorityName;
    }
    if(field=="fullName"){
      if(person.profile){
        return ((person.profile.firstName ? person.profile.firstName : "")+" "+(person.profile.lastName ? person.profile.lastName : "")).trim();
      }
      return ((person.firstName ? person.firstName : "")+" "+(person.lastName ? person.lastName : "")).trim();
    }
    if(field=="firstName" || field=="lastName"){
      if(person.profile){
        return person.profile[field];
      }
      return person[field];
    }
    if(field=="email"){
      if(person.profile && person.profile.email)
        return person.profile.email;
      if(person.email==null)
        return person.mailbox;
      return person.email;
    }
    return person[field];
  }

  static createSpacesStoreRef(node: Node) {
    return "workspace://SpacesStore/"+node.ref.id;
  }

  static getAllAuthoritiesPermission() {
    let perm=new Permission();
    perm.authority={authorityName:RestConstants.AUTHORITY_EVERYONE,authorityType:RestConstants.AUTHORITY_TYPE_EVERYONE};
    return perm;
  }

  static getColumns(mdsSet: any, name: string) {
    let columns=[];
    for(let list of mdsSet.lists){
      if(list.id=="search"){
        console.log(list);
        for(let column of list.columns){
          columns.push(new ListItem("NODE",column.id));
        }
        return columns;
      }
    }
    if(name=='search') {
      columns.push(new ListItem("NODE", RestConstants.CM_PROP_TITLE));
      columns.push(new ListItem("NODE", RestConstants.CM_MODIFIED_DATE));
      columns.push(new ListItem("NODE", RestConstants.CCM_PROP_LICENSE));
      columns.push(new ListItem("NODE", RestConstants.CCM_PROP_REPLICATIONSOURCE));
    }
    return columns;
  }

  static copyAndCleanPermissions(permissionsIn: Permission[],inherited=true) {
    let permissions: LocalPermissions = new LocalPermissions();
    permissions.inherited=inherited;
    permissions.permissions=[];
    for(let perm of permissionsIn) {
      let permClean=new Permission();
      permClean.authority=new Authority();
      permClean.authority.authorityName=perm.authority.authorityName;
      permClean.authority.authorityType=perm.authority.authorityType;
      permClean.permissions=perm.permissions;
      permissions.permissions.push(permClean);
    }
    return permissions;
  }
}
export interface UrlReplace{
  search:string;
  replace:string;

}
