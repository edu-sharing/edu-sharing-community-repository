import {
  Component, Input, Output, EventEmitter, OnInit, ElementRef, ViewChild,
  HostListener, Renderer, ChangeDetectorRef
} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {UIAnimation} from "../ui-animation";
import {RestIamService} from "../../rest/services/rest-iam.service";
import {
    IamUser, AccessScope, LoginResult, Organizations, OrganizationOrganizations, NodeList,
    NodeTextContent, NodeWrapper, Node
} from '../../rest/data-object';
import {Router, Params, ActivatedRoute} from "@angular/router";
import {RouterComponent} from "../../../router/router.component";
import {RestConnectorService} from "../../rest/services/rest-connector.service";
import {RestConstants} from "../../rest/rest-constants";
import {RestOrganizationService} from "../../rest/services/rest-organization.service";
import {FrameEventsService} from "../../services/frame-events.service";
import {ConfigurationService} from "../../services/configuration.service";
import {style, transition, trigger, animate, keyframes} from "@angular/animations";
import {SearchNodeStoreComponent} from "../../../modules/search/node-store/node-store.component";
import {UIHelper} from "../ui-helper";
import {UIConstants} from "../ui-constants";
import {RestHelper} from "../../rest/rest-helper";
import {Http} from "@angular/http";
import {Toast} from "../toast";
import {TemporaryStorageService} from "../../services/temporary-storage.service";
import {ConfigurationHelper} from "../../rest/configuration-helper";
import {CordovaService} from '../../services/cordova.service';
import {SessionStorageService} from "../../services/session-storage.service";
import {RestNodeService} from "../../rest/services/rest-node.service";
import {Translation} from "../../translation";

@Component({
  selector: 'main-nav',
  templateUrl: 'main-nav.component.html',
  styleUrls: ['main-nav.component.scss'],
  animations: [
    trigger('fromLeft', UIAnimation.fromLeft()),
    trigger('overlay', UIAnimation.openOverlay()),
    trigger('cardAnimation', UIAnimation.cardAnimation()),
    trigger('fade', UIAnimation.fade()),
    trigger('nodeStore', [
      transition(':enter', [
        animate(UIAnimation.ANIMATION_TIME_SLOW+'ms ease-in', keyframes([
          style({opacity: 0, top:'0', transform: 'scale(0.25)', offset: 0}),
          style({opacity: 1, top:'10px', transform: 'scale(1)', offset: 1}),
          //style({opacity:0,offset:0}),
          //style({opacity:1,offset:1}),

        ]))
      ]),
      transition(':leave', [
        animate(UIAnimation.ANIMATION_TIME_SLOW+'ms ease-in', keyframes([
          style({opacity: 1, transform: 'scale(1)', offset: 0}),
          style({opacity: 0, transform: 'scale(10)', offset: 1}),
          /*
          style({offset:0}),
          style({transform:'scale(1)',
            left:this.nodeStoreRef ? this.nodeStoreRef.nativeElement.getBoundingClientRect().left : '100%',
            top:this.nodeStoreRef ? this.nodeStoreRef.nativeElement.getBoundingClientRect().top : 0,offset:1})
          */
        ]))

      ])]),
  ]
})
/**
 * An edu-sharing file-picker modal dialog
 */
export class MainNavComponent {
  @ViewChild('search') search : ElementRef;
  @ViewChild('sidebar') sidebar:ElementRef;
  @ViewChild('topbar') topbar:ElementRef;
  @ViewChild('nodeStoreRef') nodeStoreRef:ElementRef;
  @ViewChild('scrolltotop') scrolltotop:ElementRef;
  public config: any={};
  private editUrl: string;
  public nodeStoreAnimation=0;
  public showNodeStore=false;
  private nodeStoreCount = 0;
  private static bannerPositionInterval: any;
  acceptLicenseAgreement: boolean;
  licenseAgreement: boolean;
  licenseAgreementHTML: string;
  canEditProfile: boolean;
  private licenseAgreementNode: Node;
  public setNodeStore(value:boolean){
    UIHelper.changeQueryParameter(this.router,this.route,"nodeStore",value);
  }
  public showEditProfile: boolean;
  public showProfile: boolean;

  public helpUrl = 'http://docs.edu-sharing.com/confluence/edp/';
  public whatsNewUrl = 'http://docs.edu-sharing.com/confluence/edp/de/was-ist-neu-in-edu-sharing';
  private toolpermissions: string[];
  public canAccessWorkspace = true;
  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
    if(event.code=="Escape" && this.canOpen && this.displaySidebar){
      event.preventDefault();
      event.stopPropagation();
      this.displaySidebar=false;
      return;
    }
  }
  private scrollInitialPositions : any[]=[];
  @HostListener('window:scroll', ['$event'])
  handleScroll(event: Event) {
    let y=0;
    try {
      let rect=document.getElementsByTagName("header")[0].getBoundingClientRect();
      y = rect.bottom-rect.top;
    }catch(e){
    }
    let elementsScroll=document.getElementsByClassName('scrollWithBanner');
    let elementsAlign=document.getElementsByClassName('alignWithBanner');
    let elements=[];
    for(let i=0;i<elementsScroll.length;i++) {
      elements.push(elementsScroll[i]);
    }
    for(let i=0;i<elementsAlign.length;i++) {
      elements.push(elementsAlign[i]);
    }
    let ATTRIBUTE_NAME='data-banner-id';
    if(event==null) {
      /*this.scrollInitialPositions=[];*/
      for(let i=0;i<elements.length;i++) {
        let element: any = elements[i];
        element.style.position = null;
        element.style.top = null;
        if(!element.getAttribute(ATTRIBUTE_NAME)){
            element.setAttribute(ATTRIBUTE_NAME,Math.random());
        }
        if(this.scrollInitialPositions[element.getAttribute(ATTRIBUTE_NAME)])
          continue;
        this.scrollInitialPositions[element.getAttribute(ATTRIBUTE_NAME)]=window.getComputedStyle(element).getPropertyValue('top');
      }
    }
    if(/*this.topbar.nativeElement.classList.contains('topBar-search')*/ true) {
      for(let i=0;i<elements.length;i++) {
        let element:any=elements[i];
        if(y==0){
          element.style.position=null;
          element.style.top=null;
          continue;
        }
        if(element.className.indexOf('alignWithBanner')!=-1){
          element.style.position = 'relative';
          if(event==null) {
            element.style.top = y + 'px';
          }
        }
        else if ((window.pageYOffset || document.documentElement.scrollTop) > y) {
          element.style.position = 'fixed';
          element.style.top = this.scrollInitialPositions[element.getAttribute(ATTRIBUTE_NAME)];
        } else {
          element.style.position = 'absolute';
          element.style.top = Number.parseInt(this.scrollInitialPositions[element.getAttribute(ATTRIBUTE_NAME)])+y + 'px';
        }
      }
    }
    if((window.pageYOffset || document.documentElement.scrollTop) > 400) {
      this.scrolltotop.nativeElement.style.display = 'block';
    } else {
      this.scrolltotop.nativeElement.style.display = 'none';
    }
  }
  private touchStart : any;
  @HostListener('document:touchstart',['$event']) onTouchStart(event:any) {
    this.touchStart=event;
  }
  @HostListener('document:touchend',['$event']) onTouchEnd(event:any) {
    let horizontal=event.changedTouches[0].clientX-this.touchStart.changedTouches[0].clientX;
    let vertical=event.changedTouches[0].clientY-this.touchStart.changedTouches[0].clientY;
    let horizontalRelative=horizontal/window.innerWidth;
    if(Math.abs(horizontal)/Math.abs(vertical)<5)
      return;
    if(this._currentScope=='render')
      return;
    if(this.touchStart.changedTouches[0].clientX<window.innerWidth/7){
      if(horizontalRelative>0.2){
        this.displaySidebar=true;
      }
    }
    if(this.touchStart.changedTouches[0].clientX>window.innerWidth/7){
      if(horizontalRelative<-0.2){
        this.displaySidebar=false;
      }
    }
  }

  private sidebarButtons : any=[];
  public displaySidebar=false;
  public user : IamUser;
  public userName : string;
  public userOpen = false;
  public helpOpen = false;
  /**
   * Show and enables the search field
   */
  @Input() searchEnabled : boolean;
  /**
   * Shows the current location
   */
  @Input() showScope=true;
  /**
   * Shows and enables the user menu
   */
  @Input() showUser=true;
  /**
   * The placeholder text for the search field, will be translated
   */
  @Input() searchPlaceholder : string;
  /**
   * When true, the sidebar can be clicked to open the menu
   * @type {boolean}
   */
  @Input() canOpen = true;
  /**
   * The title on the left side, will be translated
   */
  @Input() title : string;
  /**
   * The current scope identifier, to mark correct element in the menu as active
   */
  public _currentScope:string;
  @Input() set currentScope(currentScope:string){
    this._currentScope=currentScope;
    this.event.broadcastEvent(FrameEventsService.EVENT_VIEW_OPENED,currentScope);
  }
  /**
   * The current search query, will be inserted in the search field
   */
  @Input() searchQuery:string;
  @Output() searchQueryChange = new EventEmitter<string>();
  @Input() set onInvalidNodeStore(data:Boolean){
    this.iam.getNodeList(SearchNodeStoreComponent.LIST).subscribe((data:NodeList)=>{
      if(data.nodes.length-this.nodeStoreCount>0 && this.nodeStoreAnimation==-1)
        this.nodeStoreAnimation=data.nodes.length-this.nodeStoreCount;
      this.nodeStoreCount=data.nodes.length;
      setTimeout(()=>{
        this.nodeStoreAnimation=-1;
      },1500);
    });
  };

  /**
   * Called when a search event happened, emits the search string and additional event info
   * {query:string,cleared:boolean}
   * @type {EventEmitter}
   */
  @Output() onSearch=new EventEmitter();
  public isGuest = false;
  private isAdmin = false;
  public _showUser = false;
  onEvent(event:string,data:any){
    if(event==FrameEventsService.EVENT_PARENT_SEARCH){
      this.doSearch(data,false);
    }
  }
  public openProfileDialog(){
    this.userOpen=false;
    this.showProfile=true;
  }
  public openProfile(){
    this.router.navigate([UIConstants.ROUTER_PREFIX+"profiles",RestConstants.ME]);
    this.displaySidebar=false;
    this.userOpen=false;
  }
  public getCurrentScopeIcon(){
    if(this._currentScope=='login' || this._currentScope=='profiles')
      return 'person';
    if(this._currentScope=='oer')
        return 'public'
    for(let button of this.sidebarButtons){
      if(button.scope==this._currentScope)
        return button.icon;
    }
    return null;
  }
  refreshBanner(){
    this.handleScroll(null);
    setTimeout(()=>this.handleScroll(null),10);
  }
  ngAfterViewInit() {
    this.refreshBanner();
    /*
    for(let i=0;i<200;i++) {
      setTimeout(() => this.handleScroll(null), i * 50);
    }
    */

    // too slow and buggy
    /*
    if(MainNavComponent.bannerPositionInterval){
      clearInterval(MainNavComponent.bannerPositionInterval);
    }
    MainNavComponent.bannerPositionInterval=setInterval(()=>this.handleScroll(null),100);
    */
    }
  private clearSearch(){
    this.searchQuery="";
    this.searchQueryChange.emit("");
    this.onSearch.emit({query:"",cleared:true});
  }
  constructor(private iam : RestIamService,
              private connector : RestConnectorService,
              private cordova : CordovaService,
              private changeDetector :  ChangeDetectorRef,
              private event : FrameEventsService,
              private nodeService : RestNodeService,
              private configService : ConfigurationService,
              private storage : TemporaryStorageService,
              private session : SessionStorageService,
              private http : Http,
              private org : RestOrganizationService,
              private router : Router,
              private route : ActivatedRoute,
              private toast : Toast,
              private renderer: Renderer
  ){

    this.connector.isLoggedIn().subscribe((data:LoginResult)=>{
      if(!data.isValidLogin) {
        this.canOpen=false;
        this.checkConfig([]);
        return;
      }
      this.toolpermissions=data.toolPermissions;
      this.canAccessWorkspace=this.toolpermissions.indexOf(RestConstants.TOOLPERMISSION_WORKSPACE)!=-1;

      this.route.queryParams.subscribe((params: Params) => {
        let buttons:any=[];
        if(params["noNavigation"]=="true")
          this.canOpen=false;

        let reurl=null;
        if(params["reurl"])
          reurl={reurl:params["reurl"]};
        this.showNodeStore=params['nodeStore']=="true";
        if(!data.isGuest && this.canAccessWorkspace) {
          //buttons.push({url:this.connector.getAbsoluteEndpointUrl()+"../classic.html",scope:'workspace_old',icon:"cloud",name:"SIDEBAR.WORKSPACE_OLD"});
          buttons.push({
            //isSeperate:true,
            path: 'workspace/files',
            scope: 'workspace',
            icon: "cloud",
            name: "SIDEBAR.WORKSPACE",
            queryParams:reurl
          });
        }
        buttons.push({path:'search',scope:'search',icon:"search",name:"SIDEBAR.SEARCH",queryParams:reurl});
        buttons.push({path:'collections',scope:'collections',icon:"layers",name:"SIDEBAR.COLLECTIONS"});
        buttons.push({path:'stream',scope:'stream',icon:"event",name:"SIDEBAR.STREAM"});
        if(data.isGuest){
          buttons.push({path:'login',scope:'login',icon:"person",name:"SIDEBAR.LOGIN"});
        }
        this.isGuest=data.isGuest;
        this.isAdmin=data.isAdmin;
        this._showUser=this.currentScope!='login' && this.showUser;
        this.iam.getUser().subscribe((user : IamUser) => {
          this.user=user;
          this.canEditProfile=user.editProfile;
          this.configService.getAll().subscribe(()=>{
            this.userName=ConfigurationHelper.getPersonWithConfigDisplayName(this.user.person,this.configService);
          });
        });
        this.onInvalidNodeStore=new Boolean(true);
        this.connector.hasAccessToScope(RestConstants.SAFE_SCOPE).subscribe((data:AccessScope)=>{
          if(data.hasAccess)
            buttons.push({path:'workspace/safe',scope:'safe',icon:"lock",name:"SIDEBAR.SECURE"});
          this.addMoreButtons(buttons);
        },(error:any)=>this.addMoreButtons(buttons));
      });

    });

    event.addListener(this);
  }

  scrollToTop() {
    UIHelper.scrollSmooth(0);
    //window.scrollTo(0,0);
  }
  editProfile(){
    if(this.cordova.isRunningCordova()){
      window.open(this.editUrl,'_system');
    }
    else {
      window.location.href = this.editUrl;
    }
  }
  openSidenav() {
    if(this.canOpen) {
      this.displaySidebar=!this.displaySidebar;
      setTimeout(() => {
        this.renderer.invokeElementMethod(this.sidebar.nativeElement,'focus');
      }, 100);
    }
  }

  private showUserMenu(){
    if(this._currentScope=='login')
      return;
    this.userOpen=true;
  }
  public showHelpMenu(){
    this.helpOpen=true;
  }
  public showHelp(url:string){
    this.helpOpen=false;
    UIHelper.openBlankWindow(url,this.cordova);
  }
  private logout(){
    if(this.cordova.isRunningCordova()){
      this.cordova.clearAllCookies();
      this.cordova.setPermanentStorage(CordovaService.STORAGE_OAUTHTOKENS,null);
      this.cordova.restartCordova();
      return;
    }
    if(this.config.logout) {
      if(this.config.logout.ajax){
        this.http.get(this.config.logout.url).subscribe(()=>{
          this.finishLogout();
        },(error:any)=>{
          this.toast.error(error);
        });
      }
      else {
        if(this.config.logout.destroySession){
          this.connector.logout().subscribe((response) => {
            window.location.href = this.config.logout.url;
          });
        }
        else {
          window.location.href = this.config.logout.url;
        }
      }
    }
    else {
      this.connector.logout().subscribe((response) => {
        this.finishLogout();
      });
    }
  }
  private login(reurl=false){
    RestHelper.goToLogin(this.router,this.configService,"",reurl?window.location.href:"")
  }
  private doSearch(value=this.search.nativeElement.value,broadcast=true){
    if(broadcast)
      this.event.broadcastEvent(FrameEventsService.EVENT_GLOBAL_SEARCH,value);
    this.onSearch.emit({query:value,cleared:false});
  }
  private openButton(button : any){
    if(button.isDisabled)
      return;
    this.displaySidebar=false;
    if(button.scope==this._currentScope){
      return;
    }
    this.event.broadcastEvent(FrameEventsService.EVENT_VIEW_SWITCHED,button.scope);
    if(button.url){
      UIHelper.openBlankWindow(button.url,this.cordova);
    }
    else {
      let queryParams=button.queryParams?button.queryParams:{};
      queryParams.mainnav=true;
      this.router.navigate([UIConstants.ROUTER_PREFIX + button.path], {queryParams:queryParams});
    }
  }

  private hideButtons(buttons:any[]) {
    let hideMainMenu:string[]=null;
    if(this.config) hideMainMenu=this.config.hideMainMenu;
    this.sidebarButtons=buttons;
    if(hideMainMenu) {
      for (let i=0;i<this.sidebarButtons.length;i++) {
        let pos=hideMainMenu.indexOf(this.sidebarButtons[i].scope);
        if (pos != -1) {
          this.sidebarButtons.splice(i,1);
          i--;
        }
      }
    }
  }

  private addMoreButtons(buttons:any[]) {
    this.org.getOrganizations().subscribe((data:OrganizationOrganizations)=>{
      let add=data.canCreate;
      for(let orga of data.organizations){
        if(orga.administrationAccess){
          add=true;
          break;
        }
      }
      if(add) {
        buttons.push({path: 'permissions', scope: 'permissions', icon: "group_add", name: "SIDEBAR.PERMISSIONS"});
      }
      if(this.isAdmin && this.connector.getApiVersion()>=RestConstants.API_VERSION_4_0){
        buttons.push({path:'admin',scope:'admin',icon:"settings",name:"SIDEBAR.ADMIN"});
      }
      this.checkConfig(buttons);
    },(error:any)=>this.checkConfig(buttons));
  }
  private openImprint(){
    window.document.location.href=this.config.imprintUrl;
  }
  private checkConfig(buttons: any[]) {
    this.configService.getAll().subscribe((data:any)=>{
      this.config=data;
      this.editUrl=data["editProfileUrl"];
      this.showEditProfile=data["editProfile"];
      this.helpUrl=this.configService.instant("helpUrl",this.helpUrl);
      this.whatsNewUrl=this.configService.instant("whatsNewUrl",this.whatsNewUrl);
      this.hideButtons(buttons);
      this.addButtons(buttons);
      this.showLicenseAgreement();
    },(error:any)=>this.hideButtons(buttons));
  }

  private addButtons(buttons: any[]) {
    if(!this.config.menuEntries)
      return;

    for(let button of this.config.menuEntries) {
      let pos=button.position;
      if(pos<0)
        pos=this.sidebarButtons.length-pos;
      this.sidebarButtons.splice(pos,0,button);
    }
  }

  private finishLogout() {
    if(this.config.logout && this.config.logout.next)
      window.location.href=this.config.logout.next;
    else
      this.login(false);
  }
  getIconSource() {
    return this.configService.instant('mainnav.icon.url','assets/images/edu-white-alpha.svg');
  }
  saveLicenseAgreement(){
    this.licenseAgreement=false;
    if(this.licenseAgreementNode)
      this.session.set('licenseAgreement',this.licenseAgreementNode.contentVersion);
  }
  private showLicenseAgreement() {
    if(!this.config.licenseAgreement || this.isGuest)
      return;
    this.session.get('licenseAgreement',false).subscribe((version:string)=>{
      this.licenseAgreementHTML=null;
      let nodeId:string=null;
      for(let node of this.config.licenseAgreement.nodeId) {
        if(node.language==null)
          nodeId=node.value;
        if(node.language==Translation.getLanguage()){
          nodeId=node.value;
          break;
        }
      }
      this.nodeService.getNodeMetadata(nodeId).subscribe((data:NodeWrapper)=>{
        this.licenseAgreementNode=data.node;
        console.log(data.node);
        if(version==data.node.contentVersion)
          return;
        this.licenseAgreement=true;
        this.nodeService.getNodeTextContent(nodeId).subscribe((data: NodeTextContent) => {
            this.licenseAgreementHTML = data.html ? data.html : data.raw ? data.raw : data.text;
        }, (error: any) => {
            this.licenseAgreementHTML = "Error loading content for license agreement node '" + nodeId + "'";
        });
      },(error:any)=>{
          this.licenseAgreement=true;
          this.licenseAgreementHTML = "Error loading metadata for license agreement node '" + nodeId + "'";
      })

    });

  }
}
