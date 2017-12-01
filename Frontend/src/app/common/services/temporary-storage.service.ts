

import {Injectable} from "@angular/core";
/**
 Service to store any data temporary (lost after reloading page)
 Note that all components share the same data source. So uses prefixes for your name if applicable!
 */
@Injectable()
export class TemporaryStorageService {
  public static APPLY_TO_LMS_PARAMETER_NODE ="apply_to_lms_node";
  public static NODE_RENDER_PARAMETER_OPTIONS = "node_render_options";
  public static NODE_RENDER_PARAMETER_LIST = "node_render_list";

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

  public remove(name : string) {
    this.data[name]=null;
  }
}
