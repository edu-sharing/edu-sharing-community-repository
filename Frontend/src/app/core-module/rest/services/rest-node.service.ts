import {Injectable} from '@angular/core';
import 'rxjs/add/operator/map'
import {Observable} from 'rxjs/Observable';
import {RestConnectorService} from "./rest-connector.service";
import {RestHelper} from "../rest-helper";
import {RestConstants} from "../rest-constants";
import {
    LocalPermissions,
    NodeList,
    NodeLock,
    NodePermissions,
    NodePermissionsHistory,
    NodeRef,
    NodeRemoteWrapper,
    NodeShare,
    NodeTemplate,
    NodeTextContent,
    NodeVersion,
    NodeVersions,
    NodeWrapper,
    ParentList,
    RenderDetails,
    WorkflowEntry
} from "../data-object";
import {RestIamService} from "./rest-iam.service";
import {AbstractRestService} from "./abstract-rest-service";
import {BridgeService, MessageType} from "../../../core-bridge-module/bridge.service";
import {FrameEventsService} from './frame-events.service';

@Injectable()
export class RestNodeService extends AbstractRestService{
  constructor(connector : RestConnectorService,private events:FrameEventsService, private iam : RestIamService, private bridge : BridgeService) {
    super(connector);
    events.addListener(this);
  }
  onEvent(event:string,data:any){
    if(event==FrameEventsService.EVENT_PARENT_ADD_NODE_URL) {
        this.createNodeUrl(data);
      }
   }


  /** Searches for nodes in the repositroy
   *
   * @param searchQuery A lucence query string, see https://community.alfresco.com/docs/DOC-4673-search
   * @param facettes A list of attributes which should be grouped for counting
   * @param request A common @RequestObject
   * @param repository
   * @returns {Observable<R>}
   */
  public searchNodes = (searchQuery : string,
                        facettes : string[] = [],
                        request : any = null,
                        repository = RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrlNoEscape("node/:version/nodes/:repository?query=:query&:facettes&:request",repository,
      [
        [":query",encodeURIComponent(searchQuery)],
        [":facettes",RestHelper.getQueryStringForList("facettes",facettes)],
        [":request",this.connector.createRequestString(request)]
      ]);
    return this.connector.post<NodeList>(query,"",this.connector.getRequestOptions());

  }

  /** Get the home directory for the current user (a ref to the folder)
   *
   * @returns {Observable<R>}
   */
  public getHomeDirectory(){
    let query=this.connector.createUrl("iam/:version/people/:repository/:user",RestConstants.HOME_REPOSITORY,[[":user",RestConstants.ME]]);
    return this.connector.get<NodeRef>(query,this.connector.getRequestOptions());
  }
  /**
   * Prepare a remote object (an object from a foreign repository) local cache and return the cached instance
   * @param node the id of the node
   * @param repository the repository id where the original node is located
   * @returns {Observable<R>}
   */
  public prepareUsage = (node : string,
                        repository=RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrl("node/:version/nodes/:repository/:node/prepareUsage",repository,
      [
        [":node",node]
      ]);
    return this.connector.post<NodeRemoteWrapper>(query,null,this.connector.getRequestOptions());
  }
  /**
   * Get all children of a particular parent
   * @param parent the id, or one of the constants USERHOME, SHARED_FILES, MY_SHARED_FILES or TO_ME_SHARED_FILES
   * @param filter by FILTER_FILES, FILTER_FOLDERS or mime:any
   * @param request A common @RequestObject
   * @param repository
   * @returns {Observable<R>}
   */
  public getChildren = (parent : string,
                        filter : string[]=[],
                        request : any = null,
                        assocName : string = "",
                        repository=RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrlNoEscape("node/:version/nodes/:repository/:parent/children/?:filter&assocName=:assocName&:request",repository,
      [
        [":parent",encodeURIComponent(parent)],
        [":filter",RestHelper.getQueryStringForList("filter",filter)],
        [":assocName",encodeURIComponent(assocName)],
        [":request",this.connector.createRequestString(request)],
      ]);
    return this.connector.get<NodeList>(query,this.connector.getRequestOptions());
  }
  /**
   * Report abuse for a node
   */
  public reportNode = (node : string,
                        reason: string,
                        userEmail: string,
                        userComment : string = "",
                        repository=RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrl("node/:version/nodes/:repository/:node/report?reason=:reason&userEmail=:userEmail&userComment=:userComment",repository,
      [
        [":node",node],
        [":reason",reason],
        [":userEmail",userEmail],
        [":userComment",userComment],
      ]);
    return this.connector.post(query,null,this.connector.getRequestOptions());
  }
  /**
   * import a node from a remote repository
   */
  public importNode = (repository:string,
                        node:string,
                        localParent : string
                        ) => {
    let query=this.connector.createUrl("node/:version/nodes/:repository/:node/import/?parent=:parent",repository,
      [
        [":node",node],
        [":parent",localParent]
      ]);
    return this.connector.post<NodeWrapper>(query,null,this.connector.getRequestOptions());
  }
  /** Create a new node
   *
   * @param parent The parent id
   * @param type The type of node, use CCM_TYPE_IO (file) or CCM_TYPE_MAP (folder)
   * @param aspects aspects of this node, not required
   * @param properties properties of this node, each key of the array represents the property name
   * @param renameIfExists Auto-Rename if a file with same CM_NAME exists. If false, may returns an error
   * @param repository
   * @returns {Observable<R>}
   */
  public createNode = (parent : string,
                        type : string,
                        aspects : string[] = [],
                        properties : any[],
                        renameIfExists = false,
                        versionComment = "",
                        assocType = "",
                        repository=RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrlNoEscape("node/:version/nodes/:repository/:parent/children/?type=:type&renameIfExists=:rename&assocType=:assocType&versionComment=:versionComment&:aspects",repository,
      [
        [":parent",encodeURIComponent(parent)],
        [":type",encodeURIComponent(type)],
        [":rename",encodeURIComponent(""+renameIfExists)],
        [":versionComment",encodeURIComponent(versionComment)],
        [":assocType",encodeURIComponent(assocType)],
        [":aspects",RestHelper.getQueryStringForList("aspects",aspects)]
      ]);
    return this.connector.post<NodeWrapper>(query,JSON.stringify(properties),this.connector.getRequestOptions());
  }
  /**
   * Copy a node to a target
   * @param target the target (parent) where the copy should be saved
   * @param toCopy The element to copy
   * @param recursive Should the folder content be copied
   * @param repository
   * @returns {Observable<R>}
   */
  public copyNode = (target : string,
                       toCopy : string,
                       recursive=true,
                       repository=RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrl("node/:version/nodes/:repository/:target/children/_copy?source=:toCopy&withChildren=:recursive",repository,
      [
        [":target",target],
        [":toCopy",toCopy],
        [":recursive",recursive+""]
      ]);
    return this.connector.post<NodeWrapper>(query,"",this.connector.getRequestOptions());
  }
  /**
   * Move a node to an other parent
   * @param target the target (parent) where the element should be moved to
   * @param toCopy The element to move
   * @param repository
   * @returns {Observable<R>}
   */
  public moveNode = (target : string,
                     toMove : string,
                     repository=RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrl("node/:version/nodes/:repository/:target/children/_move?source=:toCopy",repository,
      [
        [":target",target],
        [":toCopy",toMove]
      ]);
    return this.connector.post<NodeWrapper>(query,"",this.connector.getRequestOptions());
  }
    /**
     * Create a fork (variant) of a node
     * @param target the target (parent) where the fork should be saved
     * @param toCopy The element to fork
     * @param repository
     * @returns {Observable<R>}
     */
    public forkNode = (target : string,
                       toFork : string,
                       repository=RestConstants.HOME_REPOSITORY) => {
        let query=this.connector.createUrl("node/:version/nodes/:repository/:target/children/_fork?source=:toFork",repository,
            [
                [":target",target],
                [":toFork",toFork],
            ]);
        return this.connector.post<NodeWrapper>(query,"",this.connector.getRequestOptions());
    }
  public getNodeShares = (node : string,
                     email : string=null,
                     repository=RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrl("node/:version/nodes/:repository/:node/shares?email=:email",repository,
      [
        [":node",node],
        [":email",email],
      ]);;
    return this.connector.get<NodeShare[]>(query,this.connector.getRequestOptions())
  }
  public createNodeShare = (node : string,
                            expiryDate=RestConstants.SHARE_EXPIRY_UNLIMITED,
                            password="",
                            repository=RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrl("node/:version/nodes/:repository/:node/shares/?expiryDate=:expiryDate&password=:password",repository,
    [
      [":node",node],
      [":expiryDate",""+expiryDate],
      [":password",password]
    ]);
    return this.connector.put<NodeShare>(query,"",this.connector.getRequestOptions());
  }
    public deleteNodeShare = (node : string,
                              share:string,
                              repository=RestConstants.HOME_REPOSITORY) : Observable<Response> => {
        let query=this.connector.createUrl("node/:version/nodes/:repository/:node/shares/:share",repository,
            [
                [":node",node],
                [":share",share]
            ]);
        return this.connector.delete(query,this.connector.getRequestOptions());
    }
  public updateNodeShare = (node : string,
                             shareId : string,
                             expiryDate=RestConstants.SHARE_EXPIRY_UNLIMITED,
                             password="",
                             repository=RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrl("node/:version/nodes/:repository/:node/shares/:shareId?expiryDate=:expiryDate&password=:password",repository,
      [
        [":node",node],
        [":shareId",shareId],
        [":expiryDate",""+expiryDate],
        [":password",password]
      ]);
    return this.connector.post<NodeShare>(query,"",this.connector.getRequestOptions());
  }
  /**
   * Get the metadata (properties) for a node
   * @param node
   * @param propertyFilter Only show given properties, or use the constant ALL for all properties
   * @param repository
   * @returns {Observable<R>}
   */
  public getNodeMetadata = (node : string,propertyFilter : string[]=[],repository=RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrlNoEscape("node/:version/nodes/:repository/:node/metadata/?:propertyFilter",repository,[
      [":node",encodeURIComponent(node)],
      [":propertyFilter",RestHelper.getQueryString("propertyFilter",propertyFilter)]
      ]);
    return this.connector.get<NodeWrapper>(query,this.connector.getRequestOptions());
  }
  /**
   * Get all parents of the node (including the node itself)
   * @param node The node to start from
   * @param fullPath If true, show the actual path (including company home), false only shows the actual path the user will see the node at
   * @param propertyFilter Only show given properties, or use the constant ALL for all properties
   * @param repository
   * @returns {Observable<R>}
   */
  public getNodeParents = (node : string,
                           fullPath=this.connector.getCurrentLogin() ? this.connector.getCurrentLogin().isAdmin : false,
                           //fullPath=false,
                           propertyFilter : string[]=[],
                           repository=RestConstants.HOME_REPOSITORY) : Observable<ParentList> => {
    let query=this.connector.createUrlNoEscape("node/:version/nodes/:repository/:node/parents/?fullPath=:fullPath&:propertyFilter",repository,[
      [":node",encodeURIComponent(node)],
      [":fullPath",encodeURIComponent(fullPath+"")],
      [":propertyFilter",RestHelper.getQueryString("propertyFilter",propertyFilter)]
    ]);
    return this.connector.get<ParentList>(query,this.connector.getRequestOptions());
  }
  /**
   * Remove the node from the repository (Move to archive)
   * @param node The id of the node
   * @param repository
   * @returns {Observable<Response>}
   */
  public deleteNode = (node : string,recycle=true,repository=RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrl("node/:version/nodes/:repository/:node?recycle=:recycle",repository,[
      [":node",node],
      [":recycle",""+recycle],
    ]);
    return this.connector.delete(query,this.connector.getRequestOptions());
  }
  /**
   * Like @getNodeMetadata, but for a particular node version
   * @param node The node id
   * @param major Major version number
   * @param minor Minor version number
   * @param repository
   * @returns {Observable<R>}
   */
  public getNodeMetadataForVersion = (node : string,major : number,minor : number,repository=RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrl("node/:version/nodes/:repository/:node/versions/:major/:minor/metadata",repository,
      [
        [":node",node],
        [":major",major+""],
        [":minor",minor+""],
      ]);
    return this.connector.get<NodeVersion>(query,this.connector.getRequestOptions());
  }
  /**
   * Get permissions for the node
   * @param node The node id
   * @param repository
   * @returns {Observable<R>}
   */
  public getNodePermissions = (node : string,repository=RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrl("node/:version/nodes/:repository/:node/permissions",repository,[[":node",node]]);
    return this.connector.get<NodePermissions>(query,this.connector.getRequestOptions())
  }
  /**
   * Get permissions for the node that a specific user has
   * @param node The node id
   * @param repository
   * @returns {Observable<R>}
   */
  public getNodePermissionsForUser = (node : string,authority:string,repository=RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrl("node/:version/nodes/:repository/:node/permissions/:authority",repository,[
      [":node",node],
      [":authority",authority]
    ]);
    return this.connector.get<string[]>(query,this.connector.getRequestOptions());
  }
  public getNodePermissionsHistory(node: string,repository=RestConstants.HOME_REPOSITORY) {
    let query=this.connector.createUrl("node/:version/nodes/:repository/:node/notifys",repository,[[":node",node]]);
    return this.connector.get<NodePermissionsHistory[]>(query,this.connector.getRequestOptions());
  }
  /**
   * List all versions of this node
   * @param node The node id
   * @param repository
   * @returns {Observable<R>}
   */
  public getNodeVersions = (node : string,repository=RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrl("node/:version/nodes/:repository/:node/versions",repository,[[":node",node]]);
    return this.connector.get<NodeVersions>(query,this.connector.getRequestOptions());
  }
  /**
   * Revet node to a particular version
   * @param node The node id
   * @param major Major version number
   * @param minor Minor version number
   * @param repository
   * @returns {Observable<R>}
   */
  public revertNodeToVersion = (node : string,major : number,minor : number,repository=RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrl("node/:version/nodes/:repository/:node/versions/:major/:minor/_revert",repository,
      [
        [":node",node],
        [":major",major+""],
        [":minor",minor+""],
      ]);
    return this.connector.put<NodeVersions>(query,"",this.connector.getRequestOptions());
  }
  /**
   * Update permissions for a node
   * @param node The node id
   * @param permissions A @LocalPermissions object, should be fetched before via @getNodePermissions
   * @param sendMail if true, send a mail to new invited users
   * @param mailText Additional mail text
   * @param sendCopy if true, send a copy of this mail to the user who is sharing this node
   * @param repository
   * @returns {Observable<Response>}
   */
  public setNodePermissions = (node : string,permissions:LocalPermissions,sendMail=false,mailText="",sendCopy=false,createHandle=false,repository=RestConstants.HOME_REPOSITORY) : Observable<Response> => {
    let query=this.connector.createUrl("node/:version/nodes/:repository/:node/permissions?mailtext=:mailText&sendMail=:sendMail&sendCopy=:sendCopy&createHandle=:createHandle",repository,[
      [":node",node],
      [":mailText",mailText],
      [":sendMail",""+sendMail],
      [":sendCopy",""+sendCopy],
      [":createHandle",""+createHandle]
    ]);
    return this.connector.post(query,JSON.stringify(permissions),this.connector.getRequestOptions());
  }
  /**
   * Change metadata of the node and create a new version
   * @param node The node id
   * @param versionComment The version comment string
   * @param properties The new node properties
   * @param repository
   * @returns {Observable<R>}
   */
  public editNodeMetadataNewVersion = (node : string,versionComment:string,properties : any,repository=RestConstants.HOME_REPOSITORY) => {
    let query = this.connector.createUrl("node/:version/nodes/:repository/:node/metadata?versionComment=:comment", repository,
      [
        [":node", node],
        [":comment", versionComment]
      ]);
    return this.connector.post(query, JSON.stringify(properties), this.connector.getRequestOptions());
  }
  /**
   * Add one or more aspects to an existing
   * @param node The node id
   * @param aspects The new aspects to add
   * @returns {Observable<R>}
   */
  public AddNodeAspects = (node : string,aspects : string[],repository=RestConstants.HOME_REPOSITORY) => {
    let query = this.connector.createUrl("node/:version/nodes/:repository/:node/aspects", repository,
      [
        [":node", encodeURIComponent(node)],
      ]);
    return this.connector.put(query, JSON.stringify(aspects), this.connector.getRequestOptions());
  }
  /**
   * Like @editNodeMetadataNewVersion, but no versioning
   * @param node
   * @param node The node id
   * @param properties The new node properties
   * @returns {Observable<R>}
   */
  public editNodeMetadata = (node : string,properties : any,repository=RestConstants.HOME_REPOSITORY) => {
    let query = this.connector.createUrl("node/:version/nodes/:repository/:node/metadata", repository,
      [
        [":node", node],
      ]);
    return this.connector.put<NodeWrapper>(query, JSON.stringify(properties), this.connector.getRequestOptions());
  }
  /**
   * Upload binary data to a node and create a new version
   * @param node The node id
   * @param file The @File to upload
   * @param versionComment The version comment string
   * @param mimetype when default "auto" is used, the mimetype is guessed from the content type
   * @param repository
   * @returns {Observable<void>}
   */
  public uploadNodeContent = (node : string,
                              file : File,
                     versionComment : string,
                     mimetype="auto",
                     onProgress:Function=null,
                     repository=RestConstants.HOME_REPOSITORY) : Observable<XMLHttpRequest> => {
    if(mimetype=="auto")
      mimetype=RestHelper.guessMimeType(file);
    let query=this.connector.createUrl("node/:version/nodes/:repository/:node/content?versionComment=:comment&mimetype=:mime",repository,
      [
        [":node",node],
        [":comment",versionComment],
        [":mime",mimetype]
      ]);
    let options=this.connector.getRequestOptions();

    return this.connector.sendDataViaXHR(query,file,'POST','file',onProgress);
    /*
    return this.http.post(query,"",this.connector.getRequestOptions())
      .map((response: Response) => response.json());
      */
  }
    public setNodeTextContent = (node : string,
                                text : string,
                                versionComment : string = "",
                                mimetype="text/plain",
                                repository=RestConstants.HOME_REPOSITORY) => {
        let query=this.connector.createUrl("node/:version/nodes/:repository/:node/content?versionComment=:comment&mimetype=:mimetype",repository,
            [
                [":node",node],
                [":mimetype",mimetype],
                [":comment",versionComment],
            ]);
        let options=this.connector.getRequestOptions('multipart/form-data');
        return this.connector.post<NodeWrapper>(query,text,options);

        /*
        return this.http.post(query,"",this.connector.getRequestOptions())
          .map((response: Response) => response.json());
          */
    }
  public addWorkflow = (node : string,workflow:WorkflowEntry,repository=RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrl("node/:version/nodes/:repository/:node/workflow",repository,[
      [":node",node],
    ]);
    return this.connector.put(query,JSON.stringify(workflow),this.connector.getRequestOptions());
  }
  public getWorkflowHistory = (node : string,repository=RestConstants.HOME_REPOSITORY) : Observable<WorkflowEntry[]> => {
    let query=this.connector.createUrl("node/:version/nodes/:repository/:node/workflow",repository,[
      [":node",node],
    ]);
    return this.connector.get<WorkflowEntry[]>(query,this.connector.getRequestOptions());
  }
  /**
   * Returns the current lock status (e.g. an other user edits this node) of this node
   * @returns {Observable<R>}
   */
  public isLocked = (node : string,repository=RestConstants.HOME_REPOSITORY) : Observable<NodeLock> => {
    let query = this.connector.createUrl("node/:version/nodes/:repository/:node/lock/status", repository,
      [
        [":node", node],
      ]);
    return this.connector.get<NodeLock>(query,this.connector.getRequestOptions());
  }
  /**
   * Upload binary data to a node and create a new version
   * @param node The node id
   * @param file The @File to upload
   * @param versionComment The version comment string
   * @param mimetype when default "auto" is used, the mimetype is guessed from the content type
   * @param repository
   * @returns {Observable<void>}
   */
  public uploadNodePreview = (node : string,
                              file : File,
                              mimetype="auto",
                              repository=RestConstants.HOME_REPOSITORY) : Observable<XMLHttpRequest> => {
    if(mimetype=="auto")
      mimetype=RestHelper.guessMimeType(file);
    let query=this.connector.createUrl("node/:version/nodes/:repository/:node/preview?mimetype=:mime",repository,
      [
        [":node",node],
        [":mime",mimetype]
      ]);
    let options=this.connector.getRequestOptions();

    return this.connector.sendDataViaXHR(query,file,'POST','image');
    /*
     return this.http.post(query,"",this.connector.getRequestOptions())
     .map((response: Response) => response.json());
     */
  }
    public deleteNodePreview = (node : string,
                                repository=RestConstants.HOME_REPOSITORY) : Observable<XMLHttpRequest> => {
        let query=this.connector.createUrl("node/:version/nodes/:repository/:node/preview",repository,
            [
                [":node",node],
            ]);
        let options=this.connector.getRequestOptions();
        return this.connector.delete(query,options);
    }
  private createNodeUrl(data: any) {
    let prop=RestHelper.createNameProperty(data.name);
    prop[RestConstants.CCM_PROP_IO_WWWURL]=[data.url];
    prop[RestConstants.CCM_PROP_LINKTYPE]=[RestConstants.LINKTYPE_USER_GENERATED];
    this.createNode(RestConstants.USERHOME,RestConstants.CCM_TYPE_IO,[],prop,true).subscribe((data:NodeWrapper)=>{
      this.bridge.showTemporaryMessage(MessageType.info,"NODE_CREATED_USERHOME",{name:data.node.name});
      this.events.broadcastEvent(FrameEventsService.EVENT_NODE_SAVED,data.node);
    },(error:any)=>this.bridge.showTemporaryMessage(MessageType.error,error));
  }

  public getNodeRenderSnippetUrl(node:string,version:string="-1",repository=RestConstants.HOME_REPOSITORY){
    return this.connector.createUrl("rendering/:version/details/:repository/:node?version=:nodeVersion",repository,[
      [":node",node],
      [":nodeVersion",version ? version : "-1"]
    ]);
  }
  public getNodeRenderSnippet(node:string,version:string="-1",parameters:any=null,repository=RestConstants.HOME_REPOSITORY){

    return this.connector.post<RenderDetails>(this.getNodeRenderSnippetUrl(node,version,repository),JSON.stringify(parameters),this.connector.getRequestOptions());
  }

  public getNodeTextContent(node:string,repository=RestConstants.HOME_REPOSITORY){
      let query=this.connector.createUrl("node/:version/nodes/:repository/:node/textContent",repository,[
          [":node",node],
      ]);
      return this.connector.get<NodeTextContent>(query,this.connector.getRequestOptions());
  }

  public getNodeTemplate(node: string,repository=RestConstants.HOME_REPOSITORY){
        let query=this.connector.createUrl("node/:version/nodes/:repository/:node/metadata/template",repository,[
            [":node",node],
        ]);
        return this.connector.get<NodeTemplate>(query,this.connector.getRequestOptions());
  }
    public setNodeTemplate(node: string,enable:boolean,properties:any={},repository=RestConstants.HOME_REPOSITORY){
        let query=this.connector.createUrl("node/:version/nodes/:repository/:node/metadata/template?enable=:enable",repository,[
            [":node",node],
            [":enable",""+enable],
        ]);
        return this.connector.put<NodeTemplate>(query,JSON.stringify(properties),this.connector.getRequestOptions());
    }

    /**
     * Helper function to retrieve all childobjects of an io
     * @param {string} nodeId
     * @param {string} repository
     * @returns {Observable<NodeList>}
     */
  public getNodeChildobjects(nodeId:string,repository=RestConstants.HOME_REPOSITORY){
      return this.getChildren(nodeId,[],{count:RestConstants.COUNT_UNLIMITED,propertyFilter:[RestConstants.ALL],sortBy:[RestConstants.CCM_PROP_CHILDOBJECT_ORDER],sortAscending:[true]},RestConstants.CCM_ASSOC_CHILDIO,repository);
      /*
      return new Observable<NodeList>((observer : Observer<NodeList>)=>{
          this.getChildren(nodeId,[],{count:RestConstants.COUNT_UNLIMITED,propertyFilter:[RestConstants.ALL],sortBy:[RestConstants.CCM_PROP_CHILDOBJECT_ORDER],sortAscending:[true]},repository).subscribe((childs:NodeList)=>{
          childs.nodes = Helper.filterArray(childs.nodes,'type',RestConstants.CCM_TYPE_IO);
          observer.next(childs);
          observer.complete();
      },(error:any)=>{
          observer.error(error);
          observer.complete();
          });
      });
      */
  }
}
