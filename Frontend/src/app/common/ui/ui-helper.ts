import {TranslateService} from "ng2-translate";
import {Title} from "@angular/platform-browser";
import {ConfigurationService} from "../services/configuration.service";
import {Collection, Node} from "../rest/data-object";
import {ActivatedRoute, Router} from "@angular/router";
import {UIConstants} from "./ui-constants";
import {ElementRef, EventEmitter, HostListener} from "@angular/core";
import {RestConstants} from "../rest/rest-constants";
import {RestHelper} from "../rest/rest-helper";
import {Toast} from "./toast";
import {TemporaryStorageService} from "../services/temporary-storage.service";
import {UIService} from "../services/ui.service";
import {RestCollectionService} from "../rest/services/rest-collection.service";
import {NodeHelper} from "./node-helper";
export class UIHelper{
  static MOBILE_WIDTH = 600;

  public static setTitleNoTranslation(name:string,title:Title,config:ConfigurationService) {
    config.get("branding").subscribe((data:any)=>{
      let t=name;
      if(data==true){
        t+=" - edu-sharing";
      }
      title.setTitle(t);
    });
  }
  public static setTitle(name:string,title:Title,translate:TranslateService,config:ConfigurationService){
    translate.get(name).subscribe((name:string)=>{
      this.setTitleNoTranslation(name,title,config);
    });
  }
  public static goToLogin(router : Router,config:ConfigurationService,scope="",next=window.location.href) {
    config.get("loginUrl").subscribe((url:string)=> {
      if(url && !scope){
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

  static changeQueryParameter(router: Router,route:ActivatedRoute, name: string, value: any) {
    route.queryParams.subscribe((data:any)=>{
      let queryParams:any={};
      for(let key in data){
        queryParams[key]=data[key];
      }
      queryParams[name]=value;
      router.navigate([],{queryParams:queryParams});
    });
  }

  /**
   * The materialize textarea should auto-adjust height based on content
   * however, ngmodel does not refresh the element. This method will simulate a keyboard event to refresh the state
   * It will wait for the element to come active and send a keyboard event
   * Using ng's ElementRef will not work :/ We have to use a global dom id
   * @param {ElementRef} element
   */
  static invalidateMaterializeTextarea(id:string,timeout=10) {
    setTimeout(()=> {
      if (document.getElementById(id) == null) {
        UIHelper.invalidateMaterializeTextarea(id,100);
        return;
      }
      let event = new KeyboardEvent('keyup', {
        'view': window,
        'bubbles': true,
        'cancelable': true
      });
      document.getElementById(id).dispatchEvent(event);
    },timeout);
  }

  /**
   * returns true if the given string seems to be an email
   * @param {string} email
   */
  static isEmail(email: string) {
    if(!email)
      return false;
    if(email.indexOf("@")==-1)
      return false;
    if(email.indexOf(".")==-1)
      return false;
    return true;
  }

  static routeToSearchNode(router: Router, node: Node) {
    let converted=UIHelper.convertSearchParameters(node);
    router.navigate([UIConstants.ROUTER_PREFIX+'search'],{queryParams:{query:converted.query,savedQuery:node.ref.id,repository:node.properties[RestConstants.CCM_PROP_SAVED_SEARCH_REPOSITORY],mds:node.properties[RestConstants.CCM_PROP_SAVED_SEARCH_MDS],parameters:JSON.stringify(converted.parameters)}});
  }
  static convertSearchParameters(node: Node) {
    let parameters=JSON.parse(node.properties[RestConstants.CCM_PROP_SAVED_SEARCH_PARAMETERS]);
    let result:any={parameters:{},query:null};
    for(let parameter of parameters){
      if(parameter.property==RestConstants.PRIMARY_SEARCH_CRITERIA){
        if(parameter.values[0]=='*')
          parameter.values[0]='';
        result.query=parameter.values[0];
        continue;
      }
      console.log(parameter);
      result.parameters[parameter.property]=parameter.values;
    }
    return result;
  }

  static materializeSelect() {
    eval("$('select').css('display','none');$('select').material_select()");
  }

  static showAddedToCollectionToast(toast:Toast,node: any,count:number) {
    let scope=node.collection ? node.collection.scope : node.scope;
    if(scope==RestConstants.COLLECTIONSCOPE_MY){
      scope='MY';
    }
    if(scope==RestConstants.COLLECTIONSCOPE_ORGA || scope==RestConstants.COLLECTIONSCOPE_CUSTOM){
      scope='SHARED';
    }
    else if(scope==RestConstants.COLLECTIONSCOPE_ALL || scope==RestConstants.COLLECTIONSCOPE_CUSTOM_PUBLIC){
      scope='PUBLIC';
    }
    else{
      scope='SHARED';
    }
    toast.toast("WORKSPACE.TOAST.ADDED_TO_COLLECTION_"+scope, {count: count, collection: RestHelper.getTitle(node)});
  }


  static handleAllowDragEvent(storage:TemporaryStorageService,ui:UIService,event: any, target: Node, canDrop: Function) {
    let source=storage.get(TemporaryStorageService.LIST_DRAG_DATA);
    if(!source)
      return false;
    if(!canDrop({source:source,target:target,event:event}))
      return false;
    /*
    if(event.altKey)
      event.dataTransfer.dropEffect='link';
      */
    if(event.ctrlKey || ui.isShiftCmd())
      event.dataTransfer.dropEffect='copy';
    //if(event.dataTransfer.getData("node"))
    event.preventDefault();
    event.stopPropagation();
    return true;
  }

  static handleDropEvent(storage: TemporaryStorageService, ui: UIService, event: any, target: Node, onDrop: EventEmitter<any>) {
    storage.remove(TemporaryStorageService.LIST_DRAG_DATA);
    if(!event.dataTransfer.getData("node"))
      return;
    let data=(JSON.parse(event.dataTransfer.getData("node")) as Node[]);
    event.preventDefault();
    event.stopPropagation();
    if(!data) {
      return;
    }
    let type='default';
    /*
    if(event.altKey)
      type='link';
     */
    if(event.ctrlKey || ui.isAppleCmd())
      type='copy';
    onDrop.emit({target:target,source:data,event:event,type:type});
  }
  static addToCollection(collectionService:RestCollectionService,toast:Toast,collection:Node|Collection,nodes:Node[],callback:Function=null,position=0,error=false){
    if(position>=nodes.length){
      if(!error)
        UIHelper.showAddedToCollectionToast(toast,collection,nodes.length);
      if(callback)
        callback(error);
      return;
    }

    collectionService.addNodeToCollection(collection.ref.id,nodes[position].ref.id).subscribe(()=>{
        UIHelper.addToCollection(collectionService,toast,collection,nodes,callback,position+1,error);
      },
      (error:any)=>{
        if(error.status==RestConstants.DUPLICATE_NODE_RESPONSE){
          toast.error(null,"WORKSPACE.TOAST.NODE_EXISTS_IN_COLLECTION",{name:RestHelper.getTitle(nodes[position])});
        }
        else
          NodeHelper.handleNodeError(toast,RestHelper.getTitle(nodes[position]),error);
        UIHelper.addToCollection(collectionService,toast,collection,nodes,callback,position+1,true);
      });
  }
}
