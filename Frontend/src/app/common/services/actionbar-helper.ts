import {NodeHelper} from "../ui/node-helper";
import {Node, Repository} from '../rest/data-object';
import {RestConstants} from "../rest/rest-constants";
import {OptionItem} from "../ui/actionbar/option-item";
import {RestConnectorService} from "../rest/services/rest-connector.service";
import {RestConnectorsService} from "../rest/services/rest-connectors.service";
import {Injectable} from "@angular/core";
import {RestNetworkService} from '../rest/services/rest-network.service';
@Injectable()
export class ActionbarHelperService{
  private repositories: Repository[];
  public static getNodes(nodes:Node[],node:Node):Node[] {
      return NodeHelper.getActionbarNodes(nodes,node);
  }
  constructor(
    private connector : RestConnectorService,
    private networkService : RestNetworkService,
    private connectors : RestConnectorsService
  ){
      this.networkService.getRepositories().subscribe((repositories)=>this.repositories=repositories.repositories);
  }
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
    if(type=='ADD_NODE_STORE'){
        option=new OptionItem('SEARCH.ADD_NODE_STORE', 'bookmark_border',callback);
        option.showCallback=(node:Node)=>{
            let n=ActionbarHelperService.getNodes(nodes,node);
            if(n==null)
                return false;
            return n.length && RestNetworkService.allFromHomeRepo(n,this.repositories);
        };
    }
    if(type=='NODE_TEMPLATE') {
      if (nodes && nodes.length==1 && NodeHelper.allFolders(nodes)) {
          option = new OptionItem("WORKSPACE.OPTION.TEMPLATE", "assignment_turned_in", callback);
          option.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_WRITE);
          option.onlyDesktop = true;
      }
    }
    if(type=='CREATE_VARIANT') {
        option = new OptionItem("WORKSPACE.OPTION.VARIANT", "call_split", callback);
        option.showCallback = (node : Node) =>{
            let n=ActionbarHelperService.getNodes(nodes,node);
            if(n==null)
                return false;
            option.name="WORKSPACE.OPTION.VARIANT" + (this.connectors.connectorSupportsEdit(n[0]) ? "_OPEN" : "");
            return NodeHelper.allFiles(n) && n && n.length==1 && RestNetworkService.allFromHomeRepo(n) && !this.connector.getCurrentLogin().isGuest;
        };
        option.enabledCallback = (node: Node) => {
            return node.size > 0 && node.downloadUrl;
        };
    }

    if(type=='ADD_TO_COLLECTION') {
      if (this.connector.getCurrentLogin() && !this.connector.getCurrentLogin().isGuest) {
        option = new OptionItem("WORKSPACE.OPTION.COLLECTION", "layers", callback);
        option.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CC_PUBLISH,true) && RestNetworkService.allFromHomeRepo(nodes,this.repositories);
        option.showAsAction = true;
        option.showCallback = (node: Node) => {
            let n=ActionbarHelperService.getNodes(nodes,node);
            if(n==null)
                return false;
            return NodeHelper.allFiles(nodes) && n.length>0;
        }
        option.enabledCallback = (node: Node) => {
          let list = ActionbarHelperService.getNodes(nodes, node);
          return NodeHelper.getNodesRight(list,RestConstants.ACCESS_CC_PUBLISH,true) && RestNetworkService.allFromHomeRepo(list,this.repositories);
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
        option.enabledCallback = (node: Node) => {
          let n = ActionbarHelperService.getNodes(nodes, node);
          if(n==null)
              return false;
          return NodeHelper.getNodesRight(n,RestConstants.ACCESS_CC_PUBLISH) &&
              this.connectors.getRestConnector().hasToolPermissionInstant(RestConstants.TOOLPERMISSION_INVITE_STREAM) &&
              RestNetworkService.allFromHomeRepo(n,this.repositories);
        };
          option.showCallback = (node: Node) => {
              let n=ActionbarHelperService.getNodes(nodes,node);
              if(n==null)
                  return false;
              return NodeHelper.allFiles(nodes) && RestNetworkService.allFromHomeRepo(n,this.repositories) && n.length==1;
          }
        option.isEnabled = option.enabledCallback(null);
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
    // when there are already nodes (action bar), and the option has a show callback, check if it is valid
    if(option && nodes && nodes.length && option.showCallback && !option.showCallback(null)){
        return null;
    }
    return option;
  }
}
