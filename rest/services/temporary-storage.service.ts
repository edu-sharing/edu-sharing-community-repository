import {Injectable} from "@angular/core";
import {Node} from "../data-object"

/**
 Service to store any data temporary (lost after reloading page)
 Note that all components share the same data source. So uses prefixes for your name if applicable!
 */
@Injectable()
export class TemporaryStorageService {
  public static APPLY_TO_LMS_PARAMETER_NODE ="apply_to_lms_node";
  public static NODE_RENDER_PARAMETER_OPTIONS = "node_render_options";
  public static NODE_RENDER_PARAMETER_LIST = "node_render_list";
  public static NODE_RENDER_PARAMETER_ORIGIN = "node_render_origin";
  public static COLLECTION_ADD_NODES = "collection_add_nodes";
  public static SESSION_INFO = "SESSION_INFO";
  public static LIST_DRAG_DATA = "list_drag";
  public static MAIN_NAV_BUTTONS = "main_nav_buttons";

  private data : any={};

  constructor() {}
  public get(name : string,defaultValue:any = null){
    if(this.data[name]!=null)
      return this.data[name];
    return defaultValue;
  }
  public set(name : string, value : any){
    this.data[name]=value;
  }

    /**
     * same as get, but will remove the value after fetching it
     * @param {string} name
     * @param defaultValue
     * @returns {any}
     */
  public pop(name : string,defaultValue:any = null){
     let value=this.get(name,defaultValue);
     this.remove(name);
     return value;
  }
  public remove(name : string) {
    this.data[name]=null;
  }
}
export interface ClipboardObject {
    nodes: Node[];
    sourceNode: Node;
    copy: boolean;
}