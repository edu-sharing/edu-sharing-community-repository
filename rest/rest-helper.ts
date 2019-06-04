/**
 * Different helper functions, may be used globally
 */

import {RestConstants} from "./rest-constants";
import {
    Authority,
    Collection,
    CollectionReference,
    LocalPermissions,
    Node,
    Permission,
    Permissions,
    User
} from "./data-object";
import {Router} from "@angular/router";
import {RestConnectorService} from './services/rest-connector.service';
import {RestIamService} from './services/rest-iam.service';
import {UIConstants} from "../ui/ui-constants";
import {ConfigurationService} from "./services/configuration.service";
import {BridgeService, MessageType} from "../../core-bridge-module/bridge.service";
import NumberFormat = Intl.NumberFormat;
import NumberFormatOptions = Intl.NumberFormatOptions;

export class RestHelper{
    private static SPACES_STORE_REF = "workspace://SpacesStore/";
  public static getNodeIds(nodes : Node[]|Collection[]|CollectionReference[]): Array<string>{
    let data=new Array<string>(nodes.length);
    for(let i=0;i<nodes.length;i++){
      data[i]=nodes[i].ref.id;
    }
    return data;
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

    /**
     * checks a rest error message and returns true if the string was found in the error or message text
     * @param error
     * @param needle
     */
  public static errorMatchesAny(error:any,needle:string){
    try{
      console.log(error);
      let json=JSON.parse(error.response);
      console.log(json);
      return json.error.indexOf(needle)!=-1 || json.message.indexOf(needle)!=-1;
    }catch(e){}
    return false;
  }
  public static errorMessageContains(error:any,needle:string){
    try{
      return error.error.message.indexOf(needle)!=-1;
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
  public static getQueryString(queryName : string,values : any[]) : string{
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

    public static getName(node:any):string {
        if(node.reference)
          node=node.reference;
        if (node.name) return node.name;
        return node.title;
    }
    public static getDurationInSeconds(node:any) : number {
        // PT1H5M23S
        // or 00:00:00
        // or 00:00
        let value = node.properties[RestConstants.LOM_PROP_TECHNICAL_DURATION];
        if (!value)
            return 0;
        try{
            let result=value[0].split(":");
            if(result.length==3) {
                let h = result[0] ? parseInt(result[0]) : 0;
                let m = result[1] ? parseInt(result[1]) : 0;
                let s = result[2] ? parseInt(result[2]) : 0;
                let time = h * 60 * 60 + m * 60 + s;
                return time;
            }
            if(result.length==2) {
                let m = result[0] ? parseInt(result[0]) : 0;
                let s = result[1] ? parseInt(result[1]) : 0;
                let time = m * 60 + s;
                return time;
            }
        }
        catch(e) {
            return value;
        }
        try {
            let regexp = new RegExp("PT(\\d+H)?(\\d+M)?(\\d+S)?");
            let result = regexp.exec(value[0]);
            let h = result[1] ? parseInt(result[1]) : 0;
            let m = result[2] ? parseInt(result[2]) : 0;
            let s = result[3] ? parseInt(result[3]) : 0;
            let time = h * 60 * 60 + m * 60 + s;
            return time;
        }catch(e){
            return value;
        }

    }
  public static getDurationFormatted(node:any) : string{
      let time=RestHelper.getDurationInSeconds(node);
      if(!time)
        return "";
      let h=Math.floor(time/60/60);
      let m=Math.floor(Math.floor(time/60)%60);
      let s=Math.floor(time%60);
      let options:NumberFormatOptions={
        minimumIntegerDigits:2,
        maximumFractionDigits:0
      };
      let format=new NumberFormat([],options);
      let str="";
      /*
      if(h>0) {
        str = format.format(h) + "h";
      }
      if(m>0) {
        if (str)
          str += " ";
        str += format.format(m) + "m";
      }
      if(s>0) {
        if (str)
          str += " ";
        str += format.format(s) + "s";
      }
      */
      str=format.format(h)+":"+format.format(m)+":"+format.format(s);
      return str;
    }
    public static getTitle(node:any):string {
      if(node.reference) // for collection references
        node=node.reference;
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

  static createSpacesStoreRef(node: Node) {
    return RestHelper.SPACES_STORE_REF+node.ref.id;
  }
    static removeSpacesStoreRef(id: string) {
        if(id.startsWith(RestHelper.SPACES_STORE_REF)){
            return id.substr(RestHelper.SPACES_STORE_REF.length);
        }
        return id;
    }

  static getAllAuthoritiesPermission() {
    let perm=new Permission();
    perm.authority={authorityName:RestConstants.AUTHORITY_EVERYONE,authorityType:RestConstants.AUTHORITY_TYPE_EVERYONE};
    return perm;
  }
    static addToStore(selection: Node[],bridge:BridgeService,iam:RestIamService,callback:Function,position=0,errors=0) {
        if(position==selection.length){
            if(errors==0)
                bridge.showTemporaryMessage(MessageType.info,'SEARCH.ADDED_TO_NODE_STORE',{count:position,errors:errors});
            callback();
            return;
        }
        iam.addNodeList(RestConstants.NODE_STORE_LIST,selection[position].ref.id).subscribe(()=>{
            RestHelper.addToStore(selection,bridge,iam,callback,position+1,errors);
        },(error)=>{
            console.log(error);
            if(RestHelper.errorMessageContains(error,'Node is already in list'))
                bridge.showTemporaryMessage(MessageType.error,'SEARCH.ADDED_TO_NODE_STORE_EXISTS',{name:RestHelper.getTitle(selection[position])});
            RestHelper.addToStore(selection,bridge,iam,callback,position+1,errors+1)
        });
    }
  public static goToLogin(router : Router,config:ConfigurationService,scope:string=null,next=window.location.href) {
    if(config.getLocator().getBridge().isRunningCordova()){
          config.getLocator().getBridge().getCordova().reinitStatus(config.getLocator().endpointUrl,true,next).subscribe(()=>{});
          return;
    }

    config.get("loginUrl").subscribe((url:string)=> {
      if(url && !scope && !config.instant("loginAllowLocal",false)){
        window.location.href=url;
        return;
      }
      router.navigate([UIConstants.ROUTER_PREFIX + "login"], {
        queryParams: {
          scope: scope,
          next: next
        }
      });
    });
  }

    /**
     * returns the (in some cases guessed) version for a given "about" object from the rest/_about endpoint
     * @param about
     */
    static getRepositoryVersionFromAbout(about: any) {
        if(about.version.major==1){
            if(about.version.minor==0){
                return "4.0";
            }
            if(about.version.minor==1){
                return "4.0";
            }
            if(about.version.minor==2){
                return "4.1";
            }
            if(about.version.minor==3){
                return "4.2";
            }
        }
        return null;
    }
    static guessMediatypeForFile(file: File){
        if(file.type.startsWith("image"))
            return "file-image";
        if(file.type.startsWith("video"))
            return "file-video";
        if(file.type.startsWith("audio"))
            return "file-audio";
        if(file.type=="text/xml")
            return "file-xml";
        if(file.type=="text/plain")
            return "file-txt";
        if(file.type=="application/zip" || file.type=="application/x-zip-compressed")
            return "file-zip";
        return "file";
    }
    static guessMediatypeIconForFile(connector:RestConnectorService,file:File){
        return connector.getThemeMimeIconSvg(this.guessMediatypeForFile(file)+'.svg');
    }

    static getRestObjectPositionInArray(search: any, haystack: any[]) {
        let i=0;
        for(let node of haystack) {
            if(node.ref) {
                if (node.ref.id == search.ref.id)
                    return i;
            }
            else if(node.authorityName){
                if(node.authorityName==search.authorityName)
                    return i;
            }
            i++;
        }
        return haystack.indexOf(search);
    }

}
export interface UrlReplace{
  search:string;
  replace:string;

}
