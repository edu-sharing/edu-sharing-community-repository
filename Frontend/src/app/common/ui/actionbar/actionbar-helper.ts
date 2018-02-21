import {NodeHelper} from "../node-helper";
import { Node} from "../../rest/data-object";
import {RestConstants} from "../../rest/rest-constants";
import {OptionItem} from "./option-item";
export class ActionbarHelper{
  /**
   * Add a given option for a specified type and checks the rights if possible
   * returns the option if it could be created, null otherwise
   * @param {string} type
   * @param {Node[]} nodes
   * @param {Function} callback
   * @returns {any}
   */
  static createOptionIfPossible(type:string,nodes:Node[],callback:Function){
    let option:OptionItem=null;
    if(type=='DOWNLOAD') {
      if (nodes && nodes.length && NodeHelper.allFiles(nodes)) {
        option = new OptionItem("WORKSPACE.OPTION.DOWNLOAD", "cloud_download", callback);
        option.enabledCallback = (node: Node) => {
          let list:any=ActionbarHelper.getNodes(nodes, node);
          if(list[0].reference)
            list[0]=list[0].reference;
          return list && list[0].downloadUrl && list[0].properties && !list[0].properties[RestConstants.CCM_PROP_IO_WWWURL];
        }
        option.isEnabled=option.enabledCallback(null);
      }
    }
    if(type=='ADD_TO_COLLECTION') {
      if (nodes && nodes.length && NodeHelper.allFiles(nodes)) {
        option = new OptionItem("WORKSPACE.OPTION.COLLECTION", "layers", callback);
        option.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CC_PUBLISH);
        option.showAsAction = true;
        option.enabledCallback = (node: Node) => {
          let list = ActionbarHelper.getNodes(nodes, node);
          return NodeHelper.getNodesRight(list,RestConstants.ACCESS_CC_PUBLISH);
        }
      }
    }
    if(type=='ADD_TO_STREAM') {
      if (nodes && nodes.length && NodeHelper.allFiles(nodes)) {
        option = new OptionItem("WORKSPACE.OPTION.STREAM", "line_weight", callback);
        option.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CC_PUBLISH);
        option.enabledCallback = (node: Node) => {
          let list = ActionbarHelper.getNodes(nodes, node);
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
    if(type=='SHARE_LINK'){
      if(nodes && !nodes[0].isDirectory) {
        option = new OptionItem("WORKSPACE.OPTION.SHARE_LINK", "link", callback);
        option.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_WRITE);
      }
    }
    return option;
  }
  static getNodes(nodes:Node[],node:Node):Node[] {
    return node ? [node] : nodes;
  }
}
