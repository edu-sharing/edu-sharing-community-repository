import {TranslateService} from "@ngx-translate/core";
import {Title} from "@angular/platform-browser";
import {ConfigurationService} from "../services/configuration.service";
import {
    Collection, Connector, ConnectorList, Filetype, LoginResult, MdsInfo, Node,
    NodeLock, OAuthResult, ParentList
} from "../rest/data-object";
import {ActivatedRoute, NavigationExtras, Router} from "@angular/router";
import {UIConstants} from "./ui-constants";
import {ElementRef, EventEmitter, HostListener} from "@angular/core";
import {RestConstants} from "../rest/rest-constants";
import {RestHelper} from "../rest/rest-helper";
import {Toast} from "./toast";
import {TemporaryStorageService} from "../services/temporary-storage.service";
import {UIService} from "../services/ui.service";
import {RestCollectionService} from "../rest/services/rest-collection.service";
import {NodeHelper} from "./node-helper";
import {RestConnectorsService} from "../rest/services/rest-connectors.service";
import {FrameEventsService} from "../services/frame-events.service";
import {RestNodeService} from "../rest/services/rest-node.service";
import {PlatformLocation} from "@angular/common";
import {ListItem} from './list-item';
import {CordovaService} from "../services/cordova.service";
import {SearchService} from "../../modules/search/search.service";
import {OptionItem} from "./actionbar/option-item";
import {RestConnectorService} from "../rest/services/rest-connector.service";
import {Observable, Observer} from "rxjs";
export class UIHelper{

  public static evaluateMediaQuery(type:string,value:number){
    if(type==UIConstants.MEDIA_QUERY_MAX_WIDTH)
      return value>window.innerWidth;
    if(type==UIConstants.MEDIA_QUERY_MIN_WIDTH)
        return value<window.innerWidth;
    console.warn("Unsupported media query "+type);
    return true;
  }

  public static setTitleNoTranslation(name:string,title:Title,config:ConfigurationService) {
    config.get("branding").subscribe((branding:boolean)=>{
      let t=name;
      if(branding==true){
          config.get("siteTitle","edu-sharing").subscribe((name:string)=>{
              t+=" - "+name;
              title.setTitle(t);
          });
      }
      else{
        title.setTitle(t);
      }
    });
  }
  public static setTitle(name:string,title:Title,translate:TranslateService,config:ConfigurationService,languageParams:any=null){
    translate.get(name,languageParams).subscribe((name:string)=>{
      this.setTitleNoTranslation(name,title,config);
    });
  }
  public static getBlackWhiteContrast(color:string){

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
  static isEmail(mail: string) {
      if(!mail)
        return false;
      if (mail.trim()){
          const EMAIL_REGEXP = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
          if (mail && !EMAIL_REGEXP.test(mail)) {
              return false;
          } else {
              return true;
          }
      } else {
          return false;
      }
  }

    /**
     * returns an factor indicating the strength of a password
     * Higher values mean better password strength
     * @param password
     */
    private static getPasswordStrength(password:string){
        let strength: number;
        // These are weighting factors
        let flc = 1.0;  // lowercase factor
        let fuc = 1.0;  // uppercase factor
        let fnm = 1.3;  // number factor
        let fsc = 1.5;  // special char factor
        let spc_chars = '^`?()[]{/}+-=Â¦|~!@#$%&*_';

        let regex_sc = new RegExp('['+spc_chars+']', 'g');

        let lcase_count: any = password.match(/[a-z]/g);
        lcase_count = (lcase_count) ? lcase_count.length : 0;
        let ucase_count: any = password.match(/[A-Z]/g);
        ucase_count = (ucase_count) ? ucase_count.length : 0;
        let num_count: any = password.match(/[0-9]/g);
        num_count = (num_count) ? num_count.length : 0;
        let schar_count: any = password.match(regex_sc);
        schar_count = (schar_count) ? schar_count.length : 0;
        let avg: any = password.length / 2;

        strength = ((lcase_count * flc + 1) * (ucase_count * fuc + 1) * (num_count * fnm + 1) * (schar_count * fsc + 1)) / (avg + 1);

        // console.log('Strengt: '+strength);
        return strength;
    }
    /**
     * returns an factor indicating the repeat of signd in a password
     * Higher values mean better password strength
     * @param password
     */
    private static detectPW(password:string){
        let pw_parts = password.split('');
        let i;
        let ords = new Array();
        for (i in pw_parts){
            ords[i] = pw_parts[i].charCodeAt(0);
        }
        let accum = 0;
        let lasti = ords.length-1;

        for (let i=0; i < lasti; ++i){
            accum += Math.abs(ords[i] - ords[i+1]);
        }
        // console.log('detect: '+accum/lasti);
        return accum/lasti;
    }

    /**
     * returns the password strength as a string value
     * weak, accept, medium, strong
     * @param password
     */
    public static getPasswordStrengthString(password: string){
        let min_length = 5;
        // console.log("strength: "+this.getPasswordStrength(password));
        if (password.length >= min_length){
            if (this.getPasswordStrength(password) > 10){
                if (this.getPasswordStrength(password) > 15){
                    return 'strong';
                } else {
                    return 'medium';
                }
            } else {
                return 'accept';
            }
        } else {
            return'weak';
        }
    }

  static routeToSearchNode(router: Router,searchService:SearchService, node: Node) {
    let converted=UIHelper.convertSearchParameters(node);
    router.navigate([UIConstants.ROUTER_PREFIX+'search'],{queryParams:{
      query:converted.query,
      reurl:searchService ? searchService.reurl : null,
      repository:node.properties[RestConstants.CCM_PROP_SAVED_SEARCH_REPOSITORY],
      mds:node.properties[RestConstants.CCM_PROP_SAVED_SEARCH_MDS],
      parameters:JSON.stringify(converted.parameters)}
    });
  }
    public static goToCollection(router:Router,node:Node,extras:NavigationExtras={}) {
        extras.queryParams={id:node.ref.id};
        router.navigate([UIConstants.ROUTER_PREFIX+"collections"],extras);
    }
    /**
     * Navigate to the workspace
     * @param nodeService instance of NodeService
     * @param router instance of Router
     * @param login a result of the isValidLogin method
     * @param node The node to open and show
     */
    public static goToWorkspace(nodeService:RestNodeService,router:Router,login:LoginResult,node:Node,extras:NavigationExtras={}) {
        nodeService.getNodeParents(node.ref.id).subscribe((data:ParentList)=>{
            extras.queryParams={id:node.parent.id,file:node.ref.id,root:data.scope};
            router.navigate([UIConstants.ROUTER_PREFIX+"workspace/"+(login.currentScope ? login.currentScope : "files")],
                extras);
        });
    }
    /**
     * Navigate to the workspace
     * @param nodeService instance of NodeService
     * @param router instance of Router
     * @param login a result of the isValidLogin method
     * @param folder The folder id to open
     */
    public static goToWorkspaceFolder(nodeService:RestNodeService,router:Router,login:LoginResult,folder:string,extras:NavigationExtras={}) {
      extras.queryParams={id:folder};
      router.navigate([UIConstants.ROUTER_PREFIX+"workspace/"+(login && login.currentScope ? login.currentScope : "files")],
          extras);
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

  static showAddedToCollectionToast(toast:Toast,router:Router,node: any,count:number) {
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
    toast.toast("WORKSPACE.TOAST.ADDED_TO_COLLECTION_"+scope, {count: count, collection: RestHelper.getTitle(node)},null,null,{
      link:{
        caption:'WORKSPACE.TOAST.VIEW_COLLECTION',
        callback:()=>UIHelper.goToCollection(router,node)
      }
    });
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
        mdsSets[i].name=translate.instant('DEFAULT_METADATASET',{name:mdsSets[i].name});
    }
  }
  static addToCollection(collectionService:RestCollectionService,router:Router,toast:Toast,collection:Node|Collection,nodes:Node[],callback:Function=null,position=0,error=false){
    if(position>=nodes.length){
      if(!error)
        UIHelper.showAddedToCollectionToast(toast,router,collection,nodes.length);
      if(callback)
        callback(error);
      return;
    }

    collectionService.addNodeToCollection(collection.ref.id,nodes[position].ref.id).subscribe(()=>{
        UIHelper.addToCollection(collectionService,router,toast,collection,nodes,callback,position+1,error);
      },
      (error:any)=>{
        if(error.status==RestConstants.DUPLICATE_NODE_RESPONSE){
          toast.error(null,"WORKSPACE.TOAST.NODE_EXISTS_IN_COLLECTION",{name:RestHelper.getTitle(nodes[position])});
        }
        else
          NodeHelper.handleNodeError(toast,RestHelper.getTitle(nodes[position]),error);
        UIHelper.addToCollection(collectionService,router,toast,collection,nodes,callback,position+1,true);
      });
  }
  static openConnector(connector:RestConnectorsService,events:FrameEventsService,toast:Toast,node : Node,connectorList:ConnectorList=connector.getCurrentList(),type : Filetype=null,win : any = null,connectorType : Connector = null,newWindow=true){
    if(connectorType==null){
      connectorType=connector.connectorSupportsEdit(node,connectorList);
    }
    console.log(connectorList);
    let isCordova=connector.getRestConnector().getCordovaService().isRunningCordova();
    if(win==null && newWindow) {
        win = UIHelper.getNewWindow(connector.getRestConnector());
    }

      connector.nodeApi.isLocked(node.ref.id).subscribe((result:NodeLock)=>{
      if(result.isLocked) {
        toast.error(null, "TOAST.NODE_LOCKED");
        win.close();
        return;
      }
      connector.generateToolUrl(connectorList,connectorType,type,node).subscribe((url:string)=>{
          if(win) {
              win.location.href = url;
              console.log(win);
          }
          else if(isCordova){
            UIHelper.openBlankWindow(url,connector.getRestConnector().getCordovaService());
          }
          else {
              window.location.replace(url);
          }
          if(win) {
              events.addWindow(win);
          }
        },
        (error)=>{
          console.warn(error);
          toast.error(null,error);
          if(win)
            win.close();
        });
    },(error:any)=> {
      console.warn(error);
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
    let mode=window.scrollY>=y;
    console.log(mode);
    console.log(y);
    console.log(window.scrollY);
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

  static getDefaultCollectionColumns() {
      let columns=[];
      columns.push(new ListItem("COLLECTION","title"));
      columns.push(new ListItem("COLLECTION","info"));
      columns.push(new ListItem("COLLECTION","scope"));
      return columns;
  }

  static addHttpIfRequired(link: string) {
      if(link.indexOf("://")==-1){
        return "http://"+link;
      }
      return link;
  }

  static goToDefaultLocation(router: Router,configService : ConfigurationService,extras:NavigationExtras={}) {
      return router.navigate([UIConstants.ROUTER_PREFIX + configService.instant("loginDefaultLocation","workspace")],extras);
  }

    static openBlankWindow(url:string, cordova: CordovaService) {
      if(cordova.isRunningCordova()){
        return cordova.openInAppBrowser(url);
      }
      else {
        return window.open(url, '_blank',);
      }
    }

    static filterValidOptions(ui: UIService, options: OptionItem[]) {
        if(options==null)
            return null;
        options = options.filter((value)=>value!=null);
        let optionsFiltered:OptionItem[]=[];
        for(let option of options){
            if((!option.onlyMobile || option.onlyMobile && ui.isMobile()) &&
                (!option.onlyDesktop || option.onlyDesktop && !ui.isMobile()) &&
                (!option.mediaQueryType || option.mediaQueryType && UIHelper.evaluateMediaQuery(option.mediaQueryType,option.mediaQueryValue)))
                optionsFiltered.push(option);
        }
        return optionsFiltered;
    }
    static filterToggleOptions(options: OptionItem[],toggle:boolean) {
        let result:OptionItem[]=[];
        for(let option of options){
            if(option.isToggle==toggle)
                result.push(option);
        }
        return result;
    }

    /**
     * open a window (blank) to prevent popup blocking
     * @param {RestConnectorService} connector
     * @returns {any}
     */
    public static getNewWindow(connector:RestConnectorService) {
        if(connector.getCordovaService().isRunningCordova())
            return null;
        return window.open("");
    }

    /**
     * returns true if the error message includes the given string
     * @param error
     * @param {string} data
     * @returns {boolean}
     */
    static errorContains(error: any, data: string) {
        try{
            return error.error.message.indexOf(data)!=-1;
        }catch(e){}
        return false;
    }

    /**
     * waits until the given component/object is not null and available
     * @param clz the class where the component is attached (usually "this")
     * @param componentName The name of the property
     */
    static waitForComponent(clz:any, componentName: string) {
        return new Observable((observer : Observer<any>) => {
            let interval=setInterval(()=> {
                if (clz[componentName]) {
                    observer.next(clz[componentName]);
                    observer.complete();
                    clearInterval(interval);
                }
            },1000/60);
        });
    }
}
