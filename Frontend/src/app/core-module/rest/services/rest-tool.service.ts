import { Injectable } from '@angular/core';
import 'rxjs/add/operator/map'
import { Observable } from 'rxjs/Observable';
import {RestConnectorService} from "./rest-connector.service";
import {RestHelper} from "../rest-helper";
import {RestConstants} from "../rest-constants";
import {
  NodeRef, NodeWrapper,Node, NodePermissions, LocalPermissions, NodeVersions, NodeVersion, NodeList, NodePermissionsHistory,
  NodeLock, NodeShare, WorkflowEntry, ParentList
} from "../data-object";
import {RestIamService} from "./rest-iam.service";
import {RequestObject} from "../request-object";
import {AbstractRestService} from "./abstract-rest-service";

@Injectable()
export class RestToolService extends AbstractRestService{
  constructor(connector : RestConnectorService) {
      super(connector);
  }

    /** Create a new tool definition object
   *
   * @param parent The parent id
   * @param properties properties of this node, each key of the array represents the property name
   * @param renameIfExists Auto-Rename if a file with same CM_NAME exists. If false, may returns an error
   * @param repository
   * @returns {Observable<R>}
   */
  public createToolDefinition = (properties : any,
                        renameIfExists = false,
                        versionComment = RestConstants.COMMENT_MAIN_FILE_UPLOAD,
                        repository=RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrlNoEscape("tool/:version/tools/:repository/tooldefinitions/?renameIfExists=:rename&versionComment=:versionComment",repository,
      [
        [":rename",encodeURIComponent(""+renameIfExists)],
        [":versionComment",encodeURIComponent(versionComment)],
      ]);
    return this.connector.post<NodeWrapper>(query,JSON.stringify(properties),this.connector.getRequestOptions());
  }
  /** Create a new tool instance (for a tool definition) object
   *
   * @param parent The id of the tool definition
   * @param properties properties of this node, each key of the array represents the property name
   * @param renameIfExists Auto-Rename if a file with same CM_NAME exists. If false, may returns an error
   * @param repository
   * @returns {Observable<R>}
   */
  public createToolInstance = (parent : string,
                                 properties : any,
                                 renameIfExists = false,
                                 versionComment = RestConstants.COMMENT_MAIN_FILE_UPLOAD,
                                 repository=RestConstants.HOME_REPOSITORY) => {
    let query=this.connector.createUrlNoEscape("tool/:version/tools/:repository/:parent/toolinstances/?renameIfExists=:rename&versionComment=:versionComment",repository,
      [
        [":parent",encodeURIComponent(parent)],
        [":rename",encodeURIComponent(""+renameIfExists)],
        [":versionComment",encodeURIComponent(versionComment)],
      ]);
    return this.connector.post<NodeWrapper>(query,JSON.stringify(properties),this.connector.getRequestOptions());
  }
  /** Get instances of a tool definition object
   *
   * @param tooldefinition the node id of the tool definition
   * @param repository
   * @returns {Observable<R>}
   */
  public getToolInstances = (tooldefinition:string,
                               repository=RestConstants.HOME_REPOSITORY) : Observable<NodeList> => {
    let query=this.connector.createUrl("tool/:version/tools/:repository/:tooldefinition/toolinstances",repository,      [
        [":tooldefinition",tooldefinition]
      ]);
    return this.connector.get(query,this.connector.getRequestOptions());
  }


  public static isLtiObject(node:Node){
    return node.aspects.indexOf(RestConstants.CCM_ASPECT_TOOL_OBJECT)!=-1;
  }
  public openLtiObject(node:Node,win:Window = null){
    let req = this.connector.getAbsoluteEndpointUrl()+"../eduservlet/connector?nodeId="+encodeURIComponent(node.ref.id);
    if(win==null)
      win=window.open("",'_blank');
    win.location.href = req;
  }
}
