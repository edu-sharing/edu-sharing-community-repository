import {NodeHelper} from "../node-helper";
import { Node} from "../../rest/data-object";
import {RestConstants} from "../../rest/rest-constants";
import {OptionItem} from "./option-item";
import {RestConnectorService} from "../../rest/services/rest-connector.service";
export class ActionbarHelper{
  /**
   * Add a given option for a specified type and checks the rights if possible
   * returns the option if it could be created, null otherwise
   * @param {string} type
   * @param {Node[]} nodes
   * @param {Function} callback
   * @returns {any}
   */
  static createOptionIfPossible(type:string,nodes:Node[],connector:RestConnectorService,callback:Function){
    let option:OptionItem=null;
    if(type=='DOWNLOAD') {
      console.log(nodes);
      if (NodeHelper.allFiles(nodes)) {
        option = new OptionItem("WORKSPACE.OPTION.DOWNLOAD", "cloud_download", callback);
        option.enabledCallback = (node: Node) => {
          let list:any=ActionbarHelper.getNodes(nodes, node);
          if(!list || !list.length)
            return false;
          if(list[0].reference)
            list[0]=list[0].reference;
          return list && list[0].downloadUrl && list[0].properties && !list[0].properties[RestConstants.CCM_PROP_IO_WWWURL];
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
      if (NodeHelper.allFiles(nodes)) {
          option = new OptionItem("WORKSPACE.OPTION.VARIANT", "layers", callback);
          /*
          option.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CC_PUBLISH);
          option.enabledCallback = (node: Node) => {
              let list = ActionbarHelper.getNodes(nodes, node);
              return NodeHelper.getNodesRight(list,RestConstants.ACCESS_CC_PUBLISH);
          }
          */
      }
  }
    if(type=='ADD_TO_COLLECTION') {
      if (NodeHelper.allFiles(nodes)) {
        option = new OptionItem("WORKSPACE.OPTION.COLLECTION", "layers", callback);
        option.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CC_PUBLISH);
        option.showAsAction = true;
        option.enabledCallback = (node: Node) => {
          let list = ActionbarHelper.getNodes(nodes, node);
          return NodeHelper.getNodesRight(list,RestConstants.ACCESS_CC_PUBLISH);
        }
        option.disabledCallback = () =>{
          connector.getToastService().error(null,'WORKSPACE.TOAST.ADD_TO_COLLECTION_DISABLED');
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
        option.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CHANGE_PERMISSIONS) && connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_INVITE);
      }
    }
    return option;
  }
  static getNodes(nodes:Node[],node:Node):Node[] {
    return node ? [node] : nodes;
  }
}
