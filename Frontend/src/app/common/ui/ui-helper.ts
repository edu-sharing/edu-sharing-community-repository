import {TranslateService} from "@ngx-translate/core";
import {Title} from "@angular/platform-browser";
import {ConfigurationService} from "../services/configuration.service";
import {Collection, Connector, ConnectorList, Filetype, MdsInfo, Node, NodeLock} from "../rest/data-object";
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
import {RestConnectorService} from "../rest/services/rest-connector.service";
import {RestConnectorsService} from "../rest/services/rest-connectors.service";
import {FrameEventsService} from "../services/frame-events.service";
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
    let type=node.collection ? node.collection.type : node.type;
    if(scope==RestConstants.COLLECTIONSCOPE_MY){
      scope='MY';
    }
    else if(scope==RestConstants.COLLECTIONSCOPE_ORGA || scope==RestConstants.COLLECTIONSCOPE_CUSTOM){
      scope='SHARED';
    }
    else if(scope==RestConstants.COLLECTIONSCOPE_ALL || scope==RestConstants.COLLECTIONSCOPE_CUSTOM_PUBLIC){
      scope='PUBLIC';
    }
    else if(type==RestConstants.COLLECTIONTYPE_EDITORIAL){
      scope='PUBLIC';
    }
    toast.toast("WORKSPACE.TOAST.ADDED_TO_COLLECTION_"+scope, {count: count, collection: RestHelper.getTitle(node)});
  }


  static handleAllowDragEvent(storage:TemporaryStorageService,ui:UIService,event: any, target: Node, canDrop: Function) {
    let source=storage.get(TemporaryStorageService.LIST_DRAG_DATA);
    if(!source)
      return false;
    if(!canDrop({source:source.nodes,target:target,event:event}))
      return false;
    /*
    if(event.altKey)
      event.dataTransfer.dropEffect='link';
      */
    if(event.ctrlKey || ui.isShiftCmd())
      event.dataTransfer.dropEffect='copy';
    //if(event.dataTransfer.getData("text"))
    event.preventDefault();
    event.stopPropagation();
    return true;
  }

  static handleDropEvent(storage: TemporaryStorageService, ui: UIService, event: any, target: Node, onDrop: EventEmitter<any>) {
    storage.remove(TemporaryStorageService.LIST_DRAG_DATA);
    if(!event.dataTransfer.getData("text"))
      return;
    let data=(JSON.parse(event.dataTransfer.getData("text")) as Node[]);
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
  static prepareMetadatasets(translate:TranslateService,mdsSets: MdsInfo[]) {
    for(let i=0;i<mdsSets.length;i++){
      if(mdsSets[i].id=="mds")
        mdsSets[i].name=translate.instant('DEFAULT_METADATASET');
    }
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
  static openConnector(connector:RestConnectorsService,events:FrameEventsService,toast:Toast,connectorList:ConnectorList,node : Node,type : Filetype=null,win : any = null,connectorType : Connector = null,newWindow=true){
    if(connectorType==null){
      connectorType=RestConnectorsService.connectorSupportsEdit(connectorList,node);
    }
    if(win==null && newWindow)
      win=window.open("",'_blank');

    connector.nodeApi.isLocked(node.ref.id).subscribe((result:NodeLock)=>{
      if(result.isLocked) {
        toast.error(null, "TOAST.NODE_LOCKED");
        win.close();
        return;
      }
      connector.generateToolUrl(connectorList,connectorType,type,node).subscribe((url:string)=>{
          if(newWindow)
            win.location.href=url;
          else
            window.location.replace(url);

          events.addWindow(win);
        },
        (error:string)=>{
          toast.error(null,error);
          if(win)
            win.close();
        });
    },(error:any)=> {
      toast.error(error);
      if(win)
        win.close();
    });
  }

  /**
   * smoothly scroll to the given y offset
   * @param {y} number
   * @param {smoothness} lower numbers indicate less smoothness, higher more smoothness
   */
  static scrollSmooth(y: number=0,smoothness=1) {
    let mode=window.scrollY>y;
    let divider=3*smoothness;
    let minSpeed=7/smoothness;
    let lastY=y;
    let interval=setInterval(()=>{
      let yDiff=window.scrollY-lastY;
      lastY=window.scrollY;
      if(window.scrollY>y && mode && yDiff){
          window.scrollBy(0, -Math.max((window.scrollY-y)/divider,minSpeed));
      }
      else if(window.scrollY<y && !mode && yDiff){
          window.scrollBy(0, Math.max((y-window.scrollY)/divider,minSpeed));
      }
      else {
        clearInterval(interval);
      }
    },16);
  }
    /**
     * smoothly scroll to the given y offset inside an element (use offsetTop on the child to determine this position)
     * @param {y} number
     * @param {smoothness} lower numbers indicate less smoothness, higher more smoothness
     */
    static scrollSmoothElement(y: number=0,element:Element,smoothness=1) {
        let mode=element.scrollTop>y;
        let divider=3*smoothness;
        let minSpeed=7/smoothness;
        let lastY=y;
        let interval=setInterval(()=>{
            let yDiff=element.scrollTop-lastY;
            lastY=element.scrollTop;
            if(element.scrollTop>y && mode && yDiff){
                element.scrollTop-=Math.max((element.scrollTop-y)/divider,minSpeed);
            }
            else if(element.scrollTop<y && !mode && yDiff){
                element.scrollTop+=Math.max((y-element.scrollTop)/divider,minSpeed);
            }
            else {
                clearInterval(interval);
            }
        },16);
    }
  static setFocusOnCard() {
    let elements=document.getElementsByClassName("card")[0].getElementsByTagName("*");
    this.focusElements(elements);
  }
  static setFocusOnDropdown(ref: ElementRef) {
    // the first element(s) might be currently invisible, so try to focus from bottom to top
    if(ref && ref.nativeElement) {
      let elements = ref.nativeElement.getElementsByTagName("a");
      this.focusElements(elements);
    }
  }

  private static focusElements(elements: any) {
    for(let i=elements.length-1;i>=0;i--){
      elements[i].focus();
    }
  }

  static addHttpIfRequired(link: string) {
      if(link.indexOf("://")==-1){
        return "http://"+link;
      }
      return link;
  }

  static goToDefaultLocation(router: Router,configService : ConfigurationService) {
      return router.navigate([UIConstants.ROUTER_PREFIX + configService.instant("loginDefaultLocation","workspace")]);
  }
}
