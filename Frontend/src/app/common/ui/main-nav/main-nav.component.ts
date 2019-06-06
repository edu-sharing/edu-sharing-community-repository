import {
    Component, Input, Output, EventEmitter, OnInit, ElementRef, ViewChild,
    HostListener, Renderer, ChangeDetectorRef, AfterViewInit
} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {UIAnimation} from "../../../core-module/ui/ui-animation";
import {Router, Params, ActivatedRoute} from "@angular/router";
import {style, transition, trigger, animate, keyframes} from "@angular/animations";
import {UIHelper} from "../../../core-ui-module/ui-helper";
import {OPEN_URL_MODE, UIConstants} from "../../../core-module/ui/ui-constants";
import {Toast} from "../../../core-ui-module/toast";
import {Translation} from "../../../core-ui-module/translation";
import {OptionItem} from "../../../core-ui-module/option-item";
import {HttpClient} from '@angular/common/http';
import {
    AccessScope,
    ConfigurationHelper,
    ConfigurationService,
    DialogButton,
    FrameEventsService,
    IamUser,
    LoginResult,
    Node,
    NodeList,
    NodeTextContent,
    NodeWrapper,
    OrganizationOrganizations,
    RestConnectorService,
    RestConstants, RestHelper,
    RestIamService,
    RestNodeService,
    RestOrganizationService,
    SessionStorageService,
    TemporaryStorageService,
    UIService
} from '../../../core-module/core.module';
import {CordovaService} from "../../services/cordova.service";
import {BridgeService} from "../../../core-bridge-module/bridge.service";

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
export class MainNavComponent implements AfterViewInit{
  private static bannerPositionInterval: any;
  private static preloading=true;
  private static ID_ATTRIBUTE_NAME='data-banner-id';

  @ViewChild('search') search : ElementRef;
  @ViewChild('sidebar') sidebar:ElementRef;
  @ViewChild('topbar') topbar:ElementRef;
  @ViewChild('nodeStoreRef') nodeStoreRef:ElementRef;
  @ViewChild('scrolltotop') scrolltotop:ElementRef;
  @ViewChild('userRef') userRef:ElementRef;
  @ViewChild('tabNav') tabNav:ElementRef;
    dialogTitle : string;
    dialogCancelable = false;
    dialogMessage : string;
    dialogMessageParameters : any;
    dialogButtons : DialogButton[];
    timeout: string;
    timeIsValid = false;
  public config: any={};
  private editUrl: string;
  public nodeStoreAnimation=0;
  public showNodeStore=false;
  private nodeStoreCount = 0;
  acceptLicenseAgreement: boolean;
  licenseAgreement: boolean;
  licenseAgreementHTML: string;
  canEditProfile: boolean;
  private licenseAgreementNode: Node;
  userMenuOptions: OptionItem[];
  helpOptions: OptionItem[]=[];
  tutorialElement: ElementRef;
  globalProgress = false;

  public showEditProfile: boolean;
  public showProfile: boolean;

  private toolpermissions: string[];
  public canAccessWorkspace = true;
  private scrollInitialPositions : any[]=[];

  private touchStart : any;


  private sidebarButtons : any=[];
  public displaySidebar=false;
  public user : IamUser;
  public userName : string;
  public userOpen = false;
  public helpOpen = false;
  public _currentScope:string;

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
    /**
     * Called when a search event happened, emits the search string and additional event info
     * {query:string,cleared:boolean}
     * @type {EventEmitter}
     */
    @Output() onSearch=new EventEmitter();
    public isGuest = false;
    private isAdmin = false;
    public _showUser = false;
  @Input() searchQuery:string;
  @Output() searchQueryChange = new EventEmitter<string>();
    private lastScroll = -1;
    private elementsTopY = 0;
    private elementsBottomY = 0;
    private fixScrollElements = false;
    private isSafe = false;
    licenseDialog: boolean;
    private licenseDetails: string;
  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
      if(event.code=="Escape" && this.canOpen && this.displaySidebar){
          event.preventDefault();
          event.stopPropagation();
          this.displaySidebar=false;
          return;
      }
  }
    @HostListener('window:scroll', ['$event'])
    @HostListener('window:touchmove', ['$event'])
    handleScroll(event: any) {
        let elementsScroll=document.getElementsByClassName('scrollWithBanner');
        let elementsAlign=document.getElementsByClassName('alignWithBanner');
        let elements:any=[];
        for(let i=0;i<elementsScroll.length;i++) {
            elements.push(elementsScroll[i]);
        }
        for(let i=0;i<elementsAlign.length;i++) {
            elements.push(elementsAlign[i]);
        }
        if(event==null) {
            // re-init the positions, reset the elements
            this.scrollInitialPositions=[];
            for(let i=0;i<elements.length;i++) {
                let element: any = elements[i];
                element.style.position = null;
                element.style.top = null;
                // disable transition for instant refreshes
                element.style.transition="none"
            }
            // give the browser layout engine some time to remove the values, otherwise the elements will have not their initial positions
            setTimeout(()=> {
                for (let i = 0; i < elements.length; i++) {
                    let element: any = elements[i];
                    element.style.transition=null;
                    if (!element.getAttribute(MainNavComponent.ID_ATTRIBUTE_NAME)) {
                        element.setAttribute(MainNavComponent.ID_ATTRIBUTE_NAME, Math.random());
                    }
                    if (this.scrollInitialPositions[element.getAttribute(MainNavComponent.ID_ATTRIBUTE_NAME)])
                        continue;
                    // getComputedStyle does report wrong values in search sidenav
                    this.scrollInitialPositions[element.getAttribute(MainNavComponent.ID_ATTRIBUTE_NAME)] = window.getComputedStyle(element).getPropertyValue('top');
                    //this.scrollInitialPositions[element.getAttribute(ATTRIBUTE_NAME)]=element.getBoundingClientRect().top;
                }
                console.log(this.scrollInitialPositions);
                this.posScrollElements(event,elements);
            });
        }
        else{
            this.handleScrollHide();
            this.posScrollElements(event,elements);
        }
    }
    posScrollElements(event:Event, elements: any[]){
        let y=0;
        try {
            let rect=document.getElementsByTagName("header")[0].getBoundingClientRect();
            y = rect.bottom-rect.top;
        }catch(e){
        }
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
                element.style.top = this.scrollInitialPositions[element.getAttribute(MainNavComponent.ID_ATTRIBUTE_NAME)];
            } else {
                element.style.position = 'absolute';
                element.style.top = Number.parseInt(this.scrollInitialPositions[element.getAttribute(MainNavComponent.ID_ATTRIBUTE_NAME)])+y + 'px';
            }
        }
        if((window.pageYOffset || document.documentElement.scrollTop) > 400) {
            this.scrolltotop.nativeElement.style.display = 'flex';
        } else {
            this.scrolltotop.nativeElement.style.display = 'none';
        }
    }
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
  public setNodeStore(value:boolean){
      UIHelper.changeQueryParameter(this.router,this.route,"nodeStore",value);
  }
  @Input() set currentScope(currentScope:string){
    this._currentScope=currentScope;
    this.event.broadcastEvent(FrameEventsService.EVENT_VIEW_OPENED,currentScope);
  }
  public refreshNodeStore(){
      this.iam.getNodeList(RestConstants.NODE_STORE_LIST).subscribe((data:NodeList)=>{
          if(data.nodes.length-this.nodeStoreCount>0 && this.nodeStoreAnimation==-1)
              this.nodeStoreAnimation=data.nodes.length-this.nodeStoreCount;
          this.nodeStoreCount=data.nodes.length;
          setTimeout(()=>{
              this.nodeStoreAnimation=-1;
          },1500);
      });
  }

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
    setTimeout(()=>this.handleScroll(null));
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
              private bridge : BridgeService,
              private ui : UIService,
              private changeDetector :  ChangeDetectorRef,
              private event : FrameEventsService,
              private nodeService : RestNodeService,
              private configService : ConfigurationService,
              private storage : TemporaryStorageService,
              private session : SessionStorageService,
              private http : HttpClient,
              private org : RestOrganizationService,
              private router : Router,
              private route : ActivatedRoute,
              private toast : Toast,
              private renderer: Renderer
  ){
    // get last buttons from cache for faster app navigation
    this.sidebarButtons=this.storage.get(TemporaryStorageService.MAIN_NAV_BUTTONS,[]);
    this.connector.isLoggedIn().subscribe((data:LoginResult)=>{
      if(!data.isValidLogin) {
        this.canOpen=data.isGuest;
        this.checkConfig([]);
        return;
      }
      this.isSafe=data.currentScope==RestConstants.SAFE_SCOPE;
      setInterval(()=>this.updateTimeout(),1000);
      this.toolpermissions=data.toolPermissions;
      this.canAccessWorkspace=this.toolpermissions && this.toolpermissions.indexOf(RestConstants.TOOLPERMISSION_WORKSPACE)!=-1;

      this.route.queryParams.subscribe((params: Params) => {
        let buttons:any=[];
        if(params["noNavigation"]=="true")
          this.canOpen=false;

        let reurl=null;
        if(params["reurl"])
          reurl={reurl:params["reurl"],applyDirectories:params["applyDirectories"]};
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
        buttons.push({path:'collections',scope:'collections',icon:"layers",name:"SIDEBAR.COLLECTIONS",queryParams:reurl});
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
        this.refreshNodeStore();
        this.connector.hasAccessToScope(RestConstants.SAFE_SCOPE).subscribe((data:AccessScope)=>{
          // safe needs access and not be app (oauth not supported)
          if(data.hasAccess && !this.bridge.isRunningCordova())
            buttons.push({path:'workspace/safe',scope:'safe',icon:"lock",name:"SIDEBAR.SECURE",onlyDesktop:true});
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
    if(this.bridge.isRunningCordova()){
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
    this.updateUserOptions();
    this.userOpen=true;
  }
  public showHelpMenu(){
    this.updateHelpOptions();
    this.helpOpen=true;
  }
  public showHelp(url:string){
    this.helpOpen=false;
    UIHelper.openUrl(url,this.bridge,OPEN_URL_MODE.BlankSystemBrowser);
  }
  private logout(){
    this.globalProgress=true;
    if(this.bridge.isRunningCordova()){
      this.connector.logout().subscribe(()=> {
          this.bridge.getCordova().restartCordova();
      });
      return;
    }
    if(this.config.logout) {
      let sessionData=this.connector.getCurrentLogin();
      if(this.config.logout.ajax){
        this.http.get(this.config.logout.url).subscribe(()=>{
            if(this.config.logout.destroySession){
                this.connector.logout().subscribe((response) => {
                    this.finishLogout();
                });
                return;
            }
          this.finishLogout();
        },(error:any)=>{
          this.toast.error(error);
        });
      }
      else {
        if(this.config.logout.destroySession){
          this.connector.logout().subscribe((response) => {
            if(sessionData.currentScope==RestConstants.SAFE_SCOPE){
              this.finishLogout();
            }
            else {
              window.location.href = this.config.logout.url;
            }
          });
        }
        else {
          if(sessionData.currentScope==RestConstants.SAFE_SCOPE){
              this.finishLogout();
          }
          else {
              window.location.href = this.config.logout.url;
          }
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
    // if(button.scope==this._currentScope){
    //   return;
    // }
    this.event.broadcastEvent(FrameEventsService.EVENT_VIEW_SWITCHED,button.scope);
    if(button.url){
      UIHelper.openUrl(button.url,this.bridge,OPEN_URL_MODE.BlankSystemBrowser);
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
        buttons.push({path: 'permissions', scope: 'permissions', icon: "group_add", name: "SIDEBAR.PERMISSIONS",onlyDesktop: true});
      }
      if(this.isAdmin){
        buttons.push({path:'admin',scope:'admin',icon:"settings",name:"SIDEBAR.ADMIN",onlyDesktop: true});
      }
      this.checkConfig(buttons);
    },(error:any)=>this.checkConfig(buttons));
  }
  private openImprint(){
    UIHelper.openUrl(this.config.imprintUrl,this.bridge,OPEN_URL_MODE.BlankSystemBrowser);
  }
  private openPrivacy(){
    UIHelper.openUrl(this.config.privacyInformationUrl,this.bridge,OPEN_URL_MODE.BlankSystemBrowser);
  }
  private checkConfig(buttons: any[]) {
    this.configService.getAll().subscribe((data:any)=>{
      this.config=data;
      this.updateHelpOptions();
      this.editUrl=data["editProfileUrl"];
      this.showEditProfile=data["editProfile"];
      this.hideButtons(buttons);
      this.addButtons(buttons);
      this.filterButtons();
      this.storage.set(TemporaryStorageService.MAIN_NAV_BUTTONS,this.sidebarButtons);
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
      button.isCustom=true;
      this.sidebarButtons.splice(pos,0,button);
    }
    console.log(this.sidebarButtons);
  }

  private finishLogout() {
    if(this.config.logout && this.config.logout.next)
      window.location.href=this.config.logout.next;
    else
      this.login(false);
    this.globalProgress=false;
  }
  getIconSource() {
    return this.configService.instant('mainnav.icon.url','assets/images/edu-white-alpha.svg');
  }
  saveLicenseAgreement(){
    this.licenseAgreement=false;
    if(this.licenseAgreementNode)
      this.session.set('licenseAgreement',this.licenseAgreementNode.contentVersion);
    else
      this.session.set('licenseAgreement','0.0');
    this.startTutorial();
  }
  startTutorial(){
      if(this.connector.getCurrentLogin().statusCode=='OK') {
          UIHelper.waitForComponent(this, 'userRef').subscribe(() => {
              this.tutorialElement = this.userRef;
          });
      }
  }
  private showLicenseAgreement() {
    if(!this.config.licenseAgreement || this.isGuest || !this.connector.getCurrentLogin().isValidLogin) {
        this.startTutorial();
        return;
    }
    this.session.get('licenseAgreement',false).subscribe((version:string)=>{
      console.log("user accepted agreement at version "+version);
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
        if(version==data.node.contentVersion) {
            this.startTutorial();
            return;
        }
        this.licenseAgreement=true;
        this.nodeService.getNodeTextContent(nodeId).subscribe((data: NodeTextContent) => {
            this.licenseAgreementHTML = data.html ? data.html : data.raw ? data.raw : data.text;
        }, (error: any) => {
            this.licenseAgreementHTML = "Error loading content for license agreement node '" + nodeId + "'";
        });
      },(error:any)=>{
          if(version==='0.0') {
              this.startTutorial();
              return;
          }
          this.licenseAgreement=true;
          this.licenseAgreementHTML = "Error loading metadata for license agreement node '" + nodeId + "'";
      })

    });

  }

    private updateUserOptions() {
      this.userMenuOptions=[];
        //<a *ngIf="isGuest && !config.loginOptions" class="collection-item" (click)="showAddDesktop=false;login(true)" (keyup.enter)="showAddDesktop=false;login(true)" tabindex="0" title="{{ 'SIDEBAR.LOGIN' | translate}}"><i class="material-icons">person</i> {{ 'SIDEBAR.LOGIN' | translate}}</a>
        //<a *ngFor="let loginOption of isGuest?config.loginOptions:null" class="collection-item" tabindex="0" title="{{loginOption.name}}" href="{{loginOption.url}}">{{loginOption.name}}</a>
        if(!this.isGuest){
            this.userMenuOptions.push(new OptionItem('EDIT_ACCOUNT','assignment_ind',()=>this.openProfile()));
        }
        if(this.isGuest){
          if(this.config.loginOptions){
            for(let login of this.config.loginOptions){
              this.userMenuOptions.push(new OptionItem(login.name,'',()=>window.location.href=login.url));
            }
          }
          else{
              this.userMenuOptions.push(new OptionItem('SIDEBAR.LOGIN','person',()=>this.login(true)));
          }
      }
      if(this._currentScope=='search') {
        let option=new OptionItem('SEARCH.NODE_STORE.TITLE','bookmark_border',()=>this.setNodeStore(true));
          option.mediaQueryType=UIConstants.MEDIA_QUERY_MAX_WIDTH;
          option.mediaQueryValue=UIConstants.MOBILE_TAB_SWITCH_WIDTH;
          option.isSeperateBottom=true;
          this.userMenuOptions.push(option);
      }for(let option of this.getConfigMenuHelpOptions()){
          option.mediaQueryType=UIConstants.MEDIA_QUERY_MAX_WIDTH;
          option.mediaQueryValue=UIConstants.MOBILE_TAB_SWITCH_WIDTH;
          this.userMenuOptions.push(option);
      }
      if(this.config.imprintUrl){
          let option=new OptionItem('IMPRINT','info_outline',()=>this.openImprint());
          option.mediaQueryType=UIConstants.MEDIA_QUERY_MAX_WIDTH;
          option.mediaQueryValue=UIConstants.MOBILE_TAB_SWITCH_WIDTH;
          option.isSeperateBottom=!this.config.privacyInformationUrl;
          this.userMenuOptions.push(option);
      }
        if(this.config.privacyInformationUrl){
            let option=new OptionItem('PRIVACY_INFORMATION','verified_user',()=>this.openPrivacy());
            option.mediaQueryType=UIConstants.MEDIA_QUERY_MAX_WIDTH;
            option.mediaQueryValue=UIConstants.MOBILE_TAB_SWITCH_WIDTH;
            option.isSeperateBottom=true;
            this.userMenuOptions.push(option);
        }
        let option=new OptionItem('LICENSE_INFORMATION','lightbulb_outline',()=>this.showLicenses());
        option.mediaQueryType=UIConstants.MEDIA_QUERY_MAX_WIDTH;
        option.mediaQueryValue=UIConstants.MOBILE_TAB_SWITCH_WIDTH;
        option.isSeperateBottom=true;
        this.userMenuOptions.push(option);


      if(!this.isGuest){
        this.userMenuOptions.push(new OptionItem('LOGOUT','undo',()=>this.logout()));
      }
    }

    private updateHelpOptions() {
      this.helpOptions=this.getConfigMenuHelpOptions();
    }

    private getConfigMenuHelpOptions() {
      if(!this.config.helpMenuOptions){
          console.warn("config does not contain helpMenuOptions, will not display any options");
          return [];
      }
        let options:OptionItem[]=[];
        for(let entry of this.config.helpMenuOptions){
            options.push(new OptionItem(entry.key,entry.icon,()=>window.open(entry.url)));
        }
        return options;
    }

    /**
     * Method to dynamically hide objects when scrolling on mobile
     * Add css class mobile-move-top or mobile-move-bottom for specific items
     */
    private handleScrollHide() {
      if(this.tabNav==null || this.tabNav.nativeElement==null)
          return;
      if(this.lastScroll==-1) {
          this.lastScroll=window.scrollY;
          return;
      }
      let elementsTop:any=document.getElementsByClassName("mobile-move-top");
      let elementsBottom:any=document.getElementsByClassName("mobile-move-bottom");
      let top=-1,bottom=-1;
      for(let i=0;i<elementsTop.length;i++) {
          let rect=elementsTop.item(i).getBoundingClientRect();
          if(bottom==-1 || bottom<rect.bottom){
              bottom=rect.bottom;
          }
      }
        for(let i=0;i<elementsBottom.length;i++) {
            let rect=elementsBottom.item(i).getBoundingClientRect();
            if(top==-1 || top>rect.top){
                top=rect.top;
            }
        }
      let diffTop=window.scrollY-this.lastScroll;
      let diffBottom=window.scrollY-this.lastScroll;
      if(diffTop<0) diffTop*=2;
      if(diffBottom<0) diffBottom*=2;

      if(diffTop>0 && bottom<0){
          diffTop=0;
      }
        if(diffBottom>0 && top>window.innerHeight){
            diffBottom=0;
        }
      this.elementsTopY+=diffTop;
      this.elementsTopY=Math.max(0,this.elementsTopY);
      this.elementsBottomY+=diffBottom;
      this.elementsBottomY=Math.max(0,this.elementsBottomY);
      // for ios elastic scroll
        if(window.scrollY<=0 || this.fixScrollElements || !UIHelper.evaluateMediaQuery(UIConstants.MEDIA_QUERY_MAX_WIDTH,UIConstants.MOBILE_TAB_SWITCH_WIDTH)){
            this.elementsTopY=0;
            this.elementsBottomY=0;
        }
      //this.navbarOffsetY=Math.min(this.navbarOffsetY,bottom-top);
        for(let i=0;i<elementsTop.length;i++) {
            elementsTop.item(i).style.position="relative";
            elementsTop.item(i).style.top=-this.elementsTopY+"px";
        }
      for(let i=0;i<elementsBottom.length;i++) {
          elementsBottom.item(i).style.position="relative";
          elementsBottom.item(i).style.top=this.elementsBottomY+"px";
      }
        this.lastScroll=window.scrollY;
        //console.log(event);
    }
    public setFixMobileElements(fix:boolean){
        this.fixScrollElements=fix;
        this.handleScrollHide();
    }
    private showTimeout(){
        return !this.bridge.isRunningCordova() && !this.isGuest && this.timeIsValid && this.dialogTitle!='WORKSPACE.AUTOLOGOUT' &&
            (this.isSafe || !this.isSafe && this.configService.instant('sessionExpiredDialog',{show:true}).show);
    }
    private updateTimeout(){
        let time=this.connector.logoutTimeout - Math.floor((new Date().getTime()-this.connector.lastActionTime)/1000);
        let min=Math.floor(time/60);
        let sec=time%60;
        this.event.broadcastEvent(FrameEventsService.EVENT_SESSION_TIMEOUT,time);
        if(time>=0) {
            this.timeout = this.formatTimeout(min, 2) + ":" + this.formatTimeout(sec, 2);
            this.timeIsValid=true;
        }
        else if(this.showTimeout()){
            this.dialogTitle='WORKSPACE.AUTOLOGOUT';
            this.dialogMessage='WORKSPACE.AUTOLOGOUT_INFO';
            this.dialogCancelable=false;
            this.dialogMessageParameters={minutes:Math.round(this.connector.logoutTimeout/60)};
            this.dialogButtons=[];
            this.dialogButtons.push(new DialogButton("WORKSPACE.RELOGIN",DialogButton.TYPE_PRIMARY,()=>RestHelper.goToLogin(this.router,this.configService)));
        }
        else
            this.timeout="";
    }
    private formatTimeout(num:number, size:number) {
        let s = num+"";
        while (s.length < size) s = "0" + s;
        return s;
    }
    hideDialog() : void{
        this.dialogTitle=null;
    }

    private filterButtons() {
        console.log(this.sidebarButtons);
        for (let i=0;i<this.sidebarButtons.length;i++) {
            if (this.sidebarButtons[i].onlyDesktop && this.ui.isMobile()) {
                this.sidebarButtons.splice(i,1);
                i--;
            }
        }
    }
    getPreloading(){
        return MainNavComponent.preloading;
    }
    public finishPreloading(){
        MainNavComponent.preloading=false;
    }

    showLicenses() {
        this.licenseDialog=true;
        this.displaySidebar=false;
        /*this.http.get('assets/licenses/'+Translation.getLanguage()+'.html',{responseType:'text'}).subscribe((text)=>{
            this.licenseDetails=(text as any);
        },(error)=>{
            console.info("Could not load license data for "+Translation.getLanguage()+", using default en");
            */
            this.http.get('assets/licenses/en.html',{responseType:'text'}).subscribe((text)=>{
                console.log(text);
                this.licenseDetails=(text as any);
            },(error)=> {
                console.error(error);
            });
        //});
    }
}
