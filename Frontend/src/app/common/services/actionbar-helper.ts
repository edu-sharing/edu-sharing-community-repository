import {NodeHelper} from "../ui/node-helper";
import { Node} from "../rest/data-object";
import {RestConstants} from "../rest/rest-constants";
import {OptionItem} from "../ui/actionbar/option-item";
import {RestConnectorService} from "../rest/services/rest-connector.service";
import {RestConnectorsService} from "../rest/services/rest-connectors.service";
import {Injectable} from "@angular/core";
@Injectable()
export class ActionbarHelperService{
  public static getNodes(nodes:Node[],node:Node):Node[] {
      return node ? [node] : nodes;
  }
  constructor(
    private connector : RestConnectorService,
    private connectors : RestConnectorsService
  ){}
  /**
   * Add a given option for a specified type and checks the rights if possible
   * returns the option if it could be created, null otherwise
   * @param {string} type
   * @param {Node[]} nodes
   * @param {Function} callback
   * @returns {any}
   */
  public createOptionIfPossible(type: string, nodes: Node[], callback: Function){
    let option:OptionItem=null;
    if(type=='DOWNLOAD') {
      if (NodeHelper.allFiles(nodes)) {
        option = new OptionItem("WORKSPACE.OPTION.DOWNLOAD", "cloud_download", callback);
        option.enabledCallback = (node: Node) => {
          let list:any=ActionbarHelperService.getNodes(nodes, node);
          if(!list || !list.length)
            return false;
            let isAllowed=false;
            for(let item of list) {
                if(item.reference)
                    item = item.reference;
                // if at least one is allowed -> allow download (download servlet will later filter invalid files)
                isAllowed=isAllowed || list && item.downloadUrl && item.properties && !item.properties[RestConstants.CCM_PROP_IO_WWWURL];
            }
            return isAllowed;
        }
        option.isEnabled=option.enabledCallback(null);
      }
    }
    if(type=='NODE_TEMPLATE') {
      if (nodes && nodes.length==1 && NodeHelper.allFolders(nodes)) {
          option = new OptionItem("WORKSPACE.OPTION.TEMPLATE", "assignment_turned_in", callback);
          option.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_WRITE);
      }
    }
    if(type=='CREATE_VARIANT') {
      if (NodeHelper.allFiles(nodes) && nodes && nodes.length==1 && !this.connector.getCurrentLogin().isGuest) {
        if(nodes && nodes.length && this.connectors.connectorSupportsEdit(nodes[0])){
            option = new OptionItem("WORKSPACE.OPTION.VARIANT_OPEN", "call_split", callback);
        }
        else {
            option = new OptionItem("WORKSPACE.OPTION.VARIANT", "call_split", callback);

            option.enabledCallback = (node: Node) => {
                return node.size > 0 && node.downloadUrl;
            }
            if(nodes && nodes.length) {
                option.isEnabled = option.enabledCallback(nodes[0]);
            }
        }
      }
    }
    if(type=='ADD_TO_COLLECTION') {
      if (NodeHelper.allFiles(nodes) && !this.connector.getCurrentLogin().isGuest) {
        option = new OptionItem("WORKSPACE.OPTION.COLLECTION", "layers", callback);
        option.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CC_PUBLISH,true);
        option.showAsAction = true;
        option.enabledCallback = (node: Node) => {
          let list = ActionbarHelperService.getNodes(nodes, node);
          return NodeHelper.getNodesRight(list,RestConstants.ACCESS_CC_PUBLISH,true);
        }
        option.disabledCallback = () =>{
          this.connectors.getRestConnector().getToastService().error(null,'WORKSPACE.TOAST.ADD_TO_COLLECTION_DISABLED');
        };
      }
    }
    if(type=='DELETE'){
      if(nodes && nodes.length){
          option=new OptionItem("WORKSPACE.OPTION.DELETE","delete", callback);
          option.isEnabled=NodeHelper.getNodesRight(nodes,RestConstants.ACCESS_DELETE);
          option.isSeperate=true;

      }
    }
    if(type=='ADD_TO_STREAM') {
      if (NodeHelper.allFiles(nodes)) {
        option = new OptionItem("WORKSPACE.OPTION.STREAM", "event", callback);
        option.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CC_PUBLISH);
        option.enabledCallback = (node: Node) => {
          let list = ActionbarHelperService.getNodes(nodes, node);
          return NodeHelper.getNodesRight(list,RestConstants.ACCESS_CC_PUBLISH);
        }
      }
    }
    if(type=='INVITE'){
      if(nodes && nodes.length==1) {
        option = new OptionItem("WORKSPACE.OPTION.INVITE", "group_add", callback);
        option.isSeperate = NodeHelper.allFiles(nodes);
        option.showAsAction = true;
        option.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CHANGE_PERMISSIONS);
      }
    }
    if(type=='WORKFLOW'){
        if (nodes && nodes.length==1 && !nodes[0].isDirectory  && nodes[0].type!=RestConstants.CCM_TYPE_SAVED_SEARCH) {
            option = new OptionItem("WORKSPACE.OPTION.WORKFLOW", "swap_calls", callback);
            option.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CHANGE_PERMISSIONS);
        }
    }
    if(type=='SHARE_LINK'){
      if(nodes && !nodes[0].isDirectory && nodes[0].type!=RestConstants.CCM_TYPE_SAVED_SEARCH) {
        option = new OptionItem("WORKSPACE.OPTION.SHARE_LINK", "link", callback);
        option.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CHANGE_PERMISSIONS) && this.connectors.getRestConnector().hasToolPermissionInstant(RestConstants.TOOLPERMISSION_INVITE);
      }
    }
    return option;
  }
}
