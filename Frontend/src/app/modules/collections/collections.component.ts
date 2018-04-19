import {Component, OnInit, ViewChild} from '@angular/core';

import {Router, Params, ActivatedRoute} from "@angular/router";
import {RouterComponent} from "../../router/router.component";

import {Translation} from './../../common/translation';


import * as EduData from "../../common/rest/data-object";

import {RestCollectionService} from "../../common/rest/services/rest-collection.service";
import {RestNodeService} from "../../common/rest/services/rest-node.service";
import {RestIamService} from "../../common/rest/services/rest-iam.service";
import {RestHelper} from "../../common/rest/rest-helper";
import {RestConstants} from "../../common/rest/rest-constants";

import {Toast} from "../../common/ui/toast";
import {RestConnectorService} from "../../common/rest/services/rest-connector.service";
import {CollectionContent, LoginResult, MdsMetadataset} from "../../common/rest/data-object";
import {RestOrganizationService} from "../../common/rest/services/rest-organization.service";
import {OrganizationOrganizations} from "../../common/rest/data-object";
import {OptionItem} from "../../common/ui/actionbar/option-item";
import {TemporaryStorageService} from "../../common/services/temporary-storage.service";
import {NodeRenderComponent} from "../../common/ui/node-render/node-render.component";
import {DialogButton} from "../../common/ui/modal-dialog/modal-dialog.component";
import {NodeWrapper,Node} from "../../common/rest/data-object";
import {FrameEventsService} from "../../common/services/frame-events.service";
import {UIHelper} from "../../common/ui/ui-helper";
import {Title} from "@angular/platform-browser";
import {ConfigurationService} from "../../common/services/configuration.service";
import {SessionStorageService} from "../../common/services/session-storage.service";
import {UIConstants} from "../../common/ui/ui-constants";
import {ListItem} from "../../common/ui/list-item";
import {AddElement} from "../../common/ui/list-table/list-table.component";
import {RestMdsService} from "../../common/rest/services/rest-mds.service";
import {ActionbarHelper} from "../../common/ui/actionbar/actionbar-helper";
import {NodeHelper} from "../../common/ui/node-helper";
import {TranslateService} from "@ngx-translate/core";
import {MdsHelper} from "../../common/rest/mds-helper";
import {UIAnimation} from "../../common/ui/ui-animation";
import {trigger} from "@angular/animations";
import {Location} from "@angular/common";
import {Helper} from "../../common/helper";
import {UIService} from "../../common/services/ui.service";
import {MainNavComponent} from "../../common/ui/main-nav/main-nav.component";
import {ColorHelper} from '../../common/ui/color-helper';

// component class
@Component({
  selector: 'app-collections',
  templateUrl: 'collections.component.html',
  styleUrls: ['collections.component.scss'],
})
export class CollectionsMainComponent {
  @ViewChild('mainNav') mainNavRef: MainNavComponent;
  public dialogTitle : string;
  public globalProgress=false;
  public dialogCancelable = false;
  public dialogMessage : string;
  public dialogButtons : DialogButton[];

  public tabSelected:string = RestConstants.COLLECTIONSCOPE_MY;
  public isLoading:boolean = true;
  public isReady:boolean = false;
  private clearSearchOnNextStateChange:boolean = false;

  public collectionContent:EduData.CollectionContent;
  private collectionContentOriginal: EduData.CollectionContent;
  private filteredOutCollections:Array<EduData.Collection> = new Array<EduData.Collection>();
  private filteredOutReferences:Array<EduData.CollectionReference> = new Array<EduData.CollectionReference>();
  private collectionIdParamSubscription:any;
  public lastError:string = null;

  private contentDetailObject:any = null;

  // real parentCollectionId is only available, if user was browsing
  private parentCollectionId:EduData.Reference = new EduData.Reference(RestConstants.HOME_REPOSITORY,RestConstants.ROOT);

  private temp:string;
  private lastScrollY:number;

  private person : EduData.User;
  public mainnav = true;
  private path : EduData.Node[];
  private hasOrganizations = false;
  private hasEditorial = false;
  private nodeOptions: OptionItem[]=[];
  public isGuest = true;
  public addToOther:EduData.Node[];
  private showCollection=true;
  private pinningAllowed = false;
  public addPinning: string;
  public infoTitle: string;
  public infoMessage: string;
  public infoButtons: DialogButton[];
  public infoClose: Function;
  public nodeReport: Node;
  public collectionsColumns : ListItem[]=[];
  public referencesColumns : ListItem[]=[];
  public createCollectionElement = new AddElement("COLLECTIONS.CREATE_COLLECTION");
  public createCollectionReference = new AddElement("COLLECTIONS.ADD_MATERIAL","redo");
  private listOptions: OptionItem[];
  private _orderActive: boolean;
  optionsMaterials:OptionItem[];
    // default hides the tabs

  // inject services
  constructor(
    private frame : FrameEventsService,
    private temporaryStorageService : TemporaryStorageService,
    private collectionService : RestCollectionService,
    private nodeService : RestNodeService,
    private organizationService : RestOrganizationService,
    private iamService : RestIamService,
    private mdsService : RestMdsService,
    private storage : SessionStorageService,
    private connector : RestConnectorService,
    private route:ActivatedRoute,
    private uiService:UIService,
    private router : Router,
    private toast : Toast,
    private title:Title,
    private location:Location,
    private config:ConfigurationService,
    private translationService: TranslateService) {
    this.collectionsColumns.push(new ListItem("COLLECTION", 'title'));
    this.collectionsColumns.push(new ListItem("COLLECTION", 'info'));
    this.collectionsColumns.push(new ListItem("COLLECTION",'scope'));
    this.collectionContent = new EduData.CollectionContent();
    this.collectionContent.setCollectionID(RestConstants.ROOT);
    Translation.initialize(this.translationService,this.config,this.storage,this.route).subscribe(()=>{
      UIHelper.setTitle('COLLECTIONS.TITLE',title,translationService,config);
      this.mdsService.getSet().subscribe((data:MdsMetadataset)=>{
        this.referencesColumns=MdsHelper.getColumns(data,'collectionReferences');
      })
    });

    this.connector.isLoggedIn().subscribe((data:LoginResult)=>{
      if(data.isValidLogin && data.currentScope==null) {
        this.pinningAllowed=this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_COLLECTION_PINNING);
        this.isGuest=data.isGuest;
        if(data.isValidLogin){
          this.organizationService.getOrganizations().subscribe((data:OrganizationOrganizations)=>{
            this.hasOrganizations=data.organizations.length>0;
          });
        }
        this.collectionService.getCollectionContent(RestConstants.ROOT,RestConstants.COLLECTIONSCOPE_TYPE_EDITORIAL).subscribe((data:CollectionContent)=>{
          console.log(data);
          this.hasEditorial=data.collections.length>0;
        });
        this.initialize();
      }else
        RestHelper.goToLogin(this.router,this.config);
    },(error:any)=> RestHelper.goToLogin(this.router,this.config));

  }
  public isMobile(){
    return this.uiService.isMobile();
  }
  public setCustomOrder(event:any){
    let checked=event.target.checked;
    this.collectionContent.collection.orderMode=checked ? RestConstants.COLLECTION_ORDER_MODE_CUSTOM : null;
    if(checked){
      this.orderActive=true;
    }
    else{
      this.globalProgress = true;
      this.collectionService.setOrder(this.collectionContent.collection.ref.id).subscribe(()=>{
        this.globalProgress = false;
        this.orderActive=false;
        //this.refreshContent(()=>this.globalProgress=false);
      });
    }

    }

    public set orderActive(orderActive:boolean){
      this._orderActive=orderActive;
      this.collectionContent.collection.orderMode=orderActive ? RestConstants.COLLECTION_ORDER_MODE_CUSTOM : null;

    if(this._orderActive){
      this.infoTitle='COLLECTIONS.ORDER_ELEMENTS';
      this.infoMessage='COLLECTIONS.ORDER_ELEMENTS_INFO';
      this.infoButtons=DialogButton.getSingleButton("SAVE",()=>{
        this.changeOrder();
      });
      this.infoClose=()=>{
        this.orderActive=false;
      }
    }
    else{
      this.infoTitle=null;
      //this.collectionContent.references=Helper.deepCopy(this.collectionContentOriginal.references);
      this.refreshAll();
    }
  }
  public get orderActive(){
    return this._orderActive;
  }
  navigate(id:string="",addToOther=""){
    let params:any={};
    params.scope=this.tabSelected;
    params.id=id;
    params.mainnav=this.mainnav;
    if(addToOther)
      params.addToOther=addToOther;
    this.router.navigate([UIConstants.ROUTER_PREFIX+"collections"],{queryParams:params});
  }
  closeAddToOther(){
    this.navigate(this.collectionContent.collection.ref.id);
  }
  selectTab(tab:string){
    if ((this.tabSelected!=tab)
      || (this.collectionContent.getCollectionID()!=RestConstants.ROOT)) {

      this.tabSelected = tab;
      this.collectionContent = new EduData.CollectionContent();
      this.collectionContent.setCollectionID(RestConstants.ROOT);
      this.parentCollectionId = new EduData.Reference(RestConstants.HOME_REPOSITORY, RestConstants.ROOT);
      this.contentDetailObject = null;
      this.navigate();

      this.refreshContent();

    }
  }
  selectTabMyCollections():void {
    this.selectTab(RestConstants.COLLECTIONSCOPE_MY);

  }

  selectTabMyOrganizations():void {
    this.selectTab(RestConstants.COLLECTIONSCOPE_ORGA);

  }

  selectTabAllCollections():void {
    this.selectTab(RestConstants.COLLECTIONSCOPE_ALL);
  }


  // sorting collections and references
  /*
  sortCollectionContent() : void {
      this.collectionContent.collections = this.collectionContent.collections.sort(
          function(a:EduData.Collection,b:EduData.Collection):number {
              // first sort by number of sub collections
              if (a.childCollectionsCount!=b.childCollectionsCount) return  b.childCollectionsCount-a.childCollectionsCount;
              // second sort by number of references
              if (a.childReferencesCount!=b.childReferencesCount) return  b.childReferencesCount-a.childReferencesCount;
              // third sort by date of creation
              return 0;
          }
      );
  }
  */

  // just show content (collections & references) that
  // match the keyword in title ot description
  /*
    filterCollectionContent(keyword:string):void {

       // put back all previous filtered out

       this.collectionContent.references = this.collectionContent.references.concat(this.filteredOutReferences);
       this.collectionContent.collections = this.collectionContent.collections.concat(this.filteredOutCollections);
       this.filteredOutReferences = new Array<EduData.CollectionReference>();
       this.filteredOutCollections = new Array<EduData.Collection>();

       // filter collections
       var filteredInCollections:Array<EduData.Collection> = new Array<EduData.Collection>();
       this.collectionContent.collections.forEach((collection) => {
           var isMatch:boolean = false;
           if ((typeof collection.title != "undefined") && (collection.title.toLowerCase().indexOf(keyword.toLowerCase())>=0)) isMatch = true;
           if ((typeof collection.description != "undefined") && (collection.description.toLowerCase().indexOf(keyword.toLowerCase())>=0)) isMatch = true;
           if (isMatch) {
               filteredInCollections.push(collection);
           } else {
               this.filteredOutCollections.push(collection);
           }
       });
       this.collectionContent.collections = filteredInCollections;

       // filter references
       var filteredInReferences:Array<EduData.CollectionReference> = new Array<EduData.CollectionReference>();
       this.collectionContent.references.forEach((reference) => {
           var isMatch:boolean = false;
           if ((typeof reference.reference.title != "undefined") && (reference.reference.title.toLowerCase().indexOf(keyword.toLowerCase())>=0)) isMatch = true;
           if ((typeof reference.reference.description != "undefined") && (reference.reference.description.toLowerCase().indexOf(keyword.toLowerCase())>=0)) isMatch = true;
           if (isMatch) {
               filteredInReferences.push(reference);
           } else {
               this.filteredOutReferences.push(reference);
           }
       });
       this.collectionContent.references = filteredInReferences;

       this.sortCollectionContent();
    }
    */

  isRootLevelCollection():boolean {
    return !this.showCollection;
    /*
    if (this.collectionContent==null) return false;
    return this.collectionContent.getCollectionID()=='-root-';
    */
  }

  isAllowedToEditCollection() : boolean {
    // This seems to be wrong: He may has created a public collection and wants to edit it
    if ((this.isRootLevelCollection()) && (this.tabSelected!=RestConstants.COLLECTIONSCOPE_MY)) return false;

    if (RestHelper.hasAccessPermission(this.collectionContent.collection,'Delete')) return true;
    return false;
  }

  isAllowedToDeleteCollection() : boolean {
    if (this.isRootLevelCollection()) return false;
    if (RestHelper.hasAccessPermission(this.collectionContent.collection,'Delete')) return true;
    return false;
  }

  isUserAllowedToEdit(collection:EduData.Collection) : boolean {
    return RestHelper.isUserAllowedToEdit(collection, this.person);
  }
  pinCollection(){
    this.addPinning=this.collectionContent.collection.ref.id;
  }

  getPrivacyScope(collection:EduData.Collection) : string {
    return collection.scope;
    //  return RestHelper.getPrivacyScope(collection);
  }

  navigateToSearch(){
    this.router.navigate([UIConstants.ROUTER_PREFIX+"search"],{queryParams:{mainnav:this.mainnav}});
  }
  switchToSearch() : void {
    if(this.frame.isRunningInFrame()) {
    }
    else{
      //ApplyToLmsComponent.navigateToSearchUsingReurl(this.router);
      this.router.navigate([UIConstants.ROUTER_PREFIX+"search"],{queryParams:{addToCollection:this.collectionContent.collection.ref.id}});
    }
  }
    public isBrightColor(){
        return ColorHelper.getColorBrightness(this.collectionContent.collection.color)>ColorHelper.BRIGHTNESS_THRESHOLD_COLLECTIONS;
    }
    getScopeInfo(){
      return NodeHelper.getCollectionScopeInfo(this.collectionContent.collection);
    }
    public onSelection(nodes:EduData.Node[]){
        this.optionsMaterials=this.getOptions(nodes,false);
    }
    getOptions(nodes:Node[]=null,fromList:boolean) {
      if(fromList && (!nodes || !nodes.length)){
        nodes=[new Node()];
      }
      let options:OptionItem[]=[];
      if(!fromList){
          if(nodes && nodes.length) {
              if (NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CC_PUBLISH)) {
                  let collection = ActionbarHelper.createOptionIfPossible('ADD_TO_COLLECTION', nodes,this.connector, (node: Node) => this.addToOther = ActionbarHelper.getNodes(nodes, node));
                  options.push(collection);
              }
              if (NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_DELETE)) {
                  let remove = new OptionItem('COLLECTIONS.DETAIL.REMOVE','remove_circle_outline',(node: Node)=>{
                      this.deleteMultiple(ActionbarHelper.getNodes(nodes,node));
                  });
                  options.push(remove);
              }
          }
      }
      if(fromList) {
          let collection = ActionbarHelper.createOptionIfPossible('ADD_TO_COLLECTION', nodes,this.connector,
              (node: Node) => this.addToOtherCollection(node));
          if (collection) {
              collection.name = 'COLLECTIONS.DETAIL.ADD_TO_OTHER';
              options.push(collection);
          }
      }
      let download = ActionbarHelper.createOptionIfPossible('DOWNLOAD',nodes,this.connector,
        (node:Node)=>NodeHelper.downloadNodes(this.toast,this.connector,ActionbarHelper.getNodes(nodes,node)));
      if (download)
        options.push(download);
      if(fromList) {
          let remove = new OptionItem("COLLECTIONS.DETAIL.REMOVE", "remove_circle_outline", (node: Node) => this.deleteReference(ActionbarHelper.getNodes(nodes, node)[0]));
          remove.showCallback = (node: Node) => {
              return NodeHelper.getNodesRight(ActionbarHelper.getNodes(nodes, node), RestConstants.ACCESS_DELETE);
          };
          options.push(remove);
      }
      if(fromList || nodes && nodes.length==1) {
          if (this.config.instant("nodeReport", false)) {
              let report = new OptionItem("NODE_REPORT.OPTION", "flag", (node: Node) => this.nodeReport = ActionbarHelper.getNodes(nodes, node)[0]);
              options.push(report);
          }
      }

    return options;
  }
  dropOnCollection(event:any){
    let target=event.target;
    let source=event.source[0];
    this.globalProgress=true;
    console.log(source);
    if(source.hasOwnProperty('childCollectionsCount')){
      if(event.type=='copy'){
        this.toast.error(null,'INVALID_OPERATION');
        this.globalProgress=false;
        return;
      }
      this.nodeService.moveNode(target.ref.id,source.ref.id).subscribe(()=>{
        this.globalProgress = false;
        this.refreshContent();
      },(error:any)=>{
        this.toast.error(error);
        this.globalProgress = false;
      });
    }
    else {
      this.collectionService.addNodeToCollection(target.ref.id, source.ref.id).subscribe(() => {
        UIHelper.showAddedToCollectionToast(this.toast,this.router, target, 1);
        if (event.type == 'copy') {
          this.globalProgress = false;
          this.refreshContent();
          return;
        }
        this.collectionService.removeFromCollection(source.ref.id, this.collectionContent.collection.ref.id).subscribe(() => {
          this.globalProgress = false;
          this.refreshContent();
        }, (error: any) => {
          this.toast.error(error);
          this.globalProgress = false;
        });
      }, (error: any) => {
        this.toast.error(error);
        this.globalProgress = false;
      });
    }
  }
  canDropOnCollection = (event:any)=>{
    if(event.source[0].ref.id==event.target.ref.id)
      return false;
    if(event.target.ref.id==this.collectionContent.collection.ref.id)
      return false;
    if(event.source.reference && event.source[0].access && event.source[0].access.indexOf(RestConstants.ACCESS_CC_PUBLISH)==-1)
      return false;
    if(!event.source.reference && event.source[0].access && event.source[0].access.indexOf(RestConstants.ACCESS_WRITE)==-1)
      return false;
    if(event.target.access && event.target.access.indexOf(RestConstants.ACCESS_WRITE)==-1)
      return false;

    return true;
  }
  canDropOnRef(){
    // do not allow to drop here
    return false;
  }

  buttonCollectionDelete() : void {
    this.dialogTitle="COLLECTIONS.CONFIRM_DELETE";
    this.dialogMessage="COLLECTIONS.CONFIRM_DELETE_INFO";
    this.dialogCancelable=true;
    this.dialogButtons=DialogButton.getYesNo(()=>this.closeDialog(),()=>{
      this.isLoading = true;
      this.closeDialog();
      this.collectionService.deleteCollection(this.collectionContent.collection.ref.id, this.collectionContent.collection.ref.repo).subscribe( result => {
        this.isLoading = false;
        this.navigate(this.parentCollectionId.id);
      }, error => {
        this.isLoading = false;
        this.toast.error(null,'COLLECTIONS.ERROR_DELETE');
      });

    })
  }

  buttonCollectionEdit() : void {
    this.router.navigate([UIConstants.ROUTER_PREFIX+'collections/collection','edit',this.collectionContent.collection.ref.id],{queryParams:{mainnav:this.mainnav}});
    return;
  }

  // gets called by user if something went wrong to start fresh from beginning
  resetCollections() : void {
    var url = window.location.href;
    url = url.substring(0,url.indexOf("collections")+11);
    window.location.href = url;
    return;
  }

  refreshContent(callback:Function=null) : void {
    if (!this.isReady) return;
    this.isLoading=true;
    // clear search field in GWT top area
    if (this.clearSearchOnNextStateChange) {
      this.clearSearchOnNextStateChange=false;
    }

    // set correct scope
    let scope=this.tabSelected ? this.tabSelected : RestConstants.COLLECTIONSCOPE_ALL;

    this.collectionService.getCollectionContent(this.collectionContent.collection.ref.id,
      scope,
      [RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR],
      {sortBy: [
          RestConstants.CCM_PROP_COLLECTION_PINNED_STATUS,
          RestConstants.CCM_PROP_COLLECTION_PINNED_ORDER,
          RestConstants.CM_MODIFIED_DATE
        ],
        sortAscending: [false,true,false]
      },
      this.collectionContent.collection.ref.repo
    ).subscribe((collection:EduData.CollectionContent) => {
      console.log(collection);
      this.lastError = null;
      // transfere sub collections and content
      this.collectionContent.collections = collection.collections;
      this.collectionContent.references = collection.references;
      this.collectionContentOriginal=Helper.deepCopy(this.collectionContent);
      // add an empty collection for the "add new colleciton" card
      //if (this.isAllowedToEditCollection()) this.collectionContent.collections.unshift(new EduData.Collection());


      //this.sortCollectionContent();
      this.isLoading=false;
      this.mainNavRef.refreshBanner();
      if(callback)
        callback();
    });


    if ((this.collectionContent.getCollectionID()!="-root-") && (this.collectionContent.collection.permission==null)) {
      this.nodeService.getNodePermissions(this.collectionContent.getCollectionID()).subscribe( permission => {
        this.collectionContent.collection.permission = permission.permissions;
      });
    }

  }
  onCreateCollection(){
    this.router.navigate([UIConstants.ROUTER_PREFIX+"collections/collection", "new", this.collectionContent.collection.ref.id],{queryParams:{mainnav:this.mainnav}});
  }
  onCollectionsClick(collection:EduData.Collection) : void {

        // remember actual collection as breadcrumb
        if (!this.isRootLevelCollection()) {
            this.parentCollectionId = this.collectionContent.collection.ref;
        }

    // set thru router so that browser back button can work
    this.navigate(collection.ref.id);

  }
  deleteReference(content:EduData.CollectionReference|EduData.Node){
    this.contentDetailObject=content;
    this.deleteFromCollection();
  }
  canDelete(node:EduData.CollectionReference){
    return RestHelper.hasAccessPermission(node,'Delete');
  }
  onContentClick(content:EduData.CollectionReference,force=false) : void {
    this.contentDetailObject=content;
    if (content.originalId==null && !force) {
      this.dialogTitle="COLLECTIONS.ORIGINAL_MISSING";
      this.dialogMessage="COLLECTIONS.ORIGINAL_MISSING_INFO";
      this.dialogCancelable=true;
      this.dialogButtons=[];
      if (this.isAllowedToDeleteCollection()) {
        this.dialogButtons.push(new DialogButton('COLLECTIONS.DETAIL.REMOVE',DialogButton.TYPE_CANCEL,()=>this.deleteFromCollection(()=>this.closeDialog())));
      }
      this.dialogButtons.push(new DialogButton('COLLECTIONS.OPEN_MISSING',DialogButton.TYPE_PRIMARY,()=>this.onContentClick(content,true)));
      return;
    }
    this.nodeService.getNodeMetadata(content.ref.id).subscribe((data:NodeWrapper)=>{
      this.contentDetailObject=data.node;

        // remember the scroll Y before displaying content
        this.lastScrollY = window.scrollY;
        /*if(data.node.downloadUrl)
          this.nodeOptions.push(new OptionItem("DOWNLOAD", "cloud_download", () => this.downloadMaterial()));
         */
        if(data.node.access.indexOf(RestConstants.ACCESS_DELETE)!=-1) {
          this.nodeOptions.push(new OptionItem("COLLECTIONS.DETAIL.REMOVE", "remove_circle_outline", () => this.deleteFromCollection(() => {
            NodeRenderComponent.close(this.location);
          })));
        }
        // set content for being displayed in detail
        this.temporaryStorageService.set(TemporaryStorageService.NODE_RENDER_PARAMETER_OPTIONS,this.nodeOptions);
        this.temporaryStorageService.set(TemporaryStorageService.NODE_RENDER_PARAMETER_LIST,this.collectionContent.references);
        this.router.navigate([UIConstants.ROUTER_PREFIX+"render",  content.ref.id]);
        //this.navigate(this.collectionContent.collection.ref.id,content.ref.id);
        // add breadcrumb


    });


  }

  contentDetailBack(event:any) : void {

    // scroll to last Y
    window.scrollTo(0,this.lastScrollY);

    this.navigate(this.collectionContent.collection.ref.id);
    // refresh content if signaled
    if (event.refresh) this.refreshContent();
  }


  public refreshAll(){
    this.displayCollectionById(this.collectionContent.collection.ref.id);
  }
  displayCollectionById(id:string,callback:Function=null) : void {
    if (id==null) id=RestConstants.ROOT;
    if (id=="-root-") {

      // display root collections with tabs
      this.collectionContent = new EduData.CollectionContent();
      this.collectionContent.setCollectionID(RestConstants.ROOT);
      this.refreshContent(callback);
    } else {

      // load metadata of collection
      this.isLoading=true;

      this.collectionService.getCollection(id).subscribe( collection => {
        // set the collection and load content data by refresh
        this.collectionContent = new EduData.CollectionContent();
        this.collectionContent.collection = collection.collection;

        this.renderBreadcrumbs();

        this.refreshContent(callback);
      }, error => {
        if(id!='-root-'){
          this.navigate();
        }
        if(error.status==404){
          this.toast.error(null,"COLLECTIONS.ERROR_NOT_FOUND");
        }
        else {
          this.toast.error(error);
        }
        this.isLoading=false;
      });

    }
  }

  private renderBreadcrumbs() {
    this.path=[];
    this.nodeService.getNodeParents(this.collectionContent.collection.ref.id,false).subscribe((data: EduData.NodeList) => {
      this.path = data.nodes.reverse();
    });
  }
  private openBreadcrumb(position : number){
    if(position==0) {
      this.selectTab(this.tabSelected);
      return;
    }
    this.navigate(this.path[position-1].ref.id);

  }

  private initialize() {

    this.listOptions = this.getOptions(null,true);

    // load user profile
    this.iamService.getUser().subscribe( iamUser => {
      // WIN

      this.person = iamUser.person;

      this.isReady = true;
      // subscribe to parameters of url
      this.collectionIdParamSubscription = this.route.queryParams.subscribe(params => {
        console.log(params);
        if(params['scope'])
          this.tabSelected=params['scope'];
        if(this.isGuest)
          this.tabSelected=RestConstants.COLLECTIONSCOPE_ALL;
        if(params['mainnav'])
          this.mainnav=params['mainnav']!='false';

        this._orderActive = false;
        this.infoTitle = null;
        // get id from route and validate input data
        let id = params['id'] || '-root-';
        if (id==":id") id = "-root-";
        if (id=="") id = "-root-";
        if(params['addToOther']){
          this.nodeService.getNodeMetadata(params['addToOther']).subscribe((data:EduData.NodeWrapper)=>{
            this.addToOther=[data.node];
          });
        }
        if(params['nodeId']){
          let node=params['nodeId'].split("/");
          node=node[node.length-1];
          console.log("node: "+node);
          this.collectionService.addNodeToCollection(id,node).subscribe(()=> this.navigate(id),(error:any)=>{
            this.handleError(error);
            this.navigate(id);
            //this.displayCollectionById(id)
          });
        }
        else {
          this.showCollection=id!='-root-';
          this.displayCollectionById(id,(collection:EduData.Collection)=>{
            if(params['content']){
              console.log("search content");
              for(let content of this.collectionContent.references) {
                console.log(content);
                if(content.ref.id==params['content']){
                  console.log("match");
                  this.contentDetailObject = content;
                  break;
                }
              }
            }
            this.frame.broadcastEvent(FrameEventsService.EVENT_INVALIDATE_HEIGHT);
          });
        }

      });

    }, error => {
      // FAIL
      this.toast.error(error);
      this.isReady = true;
    });
  }


  private deleteFromCollection(callback:Function=null) {
    this.globalProgress=true;
    this.collectionService.removeFromCollection(this.contentDetailObject.ref.id,this.collectionContent.collection.ref.id).subscribe(()=>{
      this.toast.toast("COLLECTIONS.REMOVED_FROM_COLLECTION");
      this.globalProgress=false;
      this.refreshContent();
      if(callback) callback();
    },(error:any)=>{
      this.globalProgress=false;
      this.toast.error(error);
    });
  }
    private deleteMultiple(nodes:Node[],position=0,error=false){
            if(position==nodes.length){
                if(!error) {
                    this.toast.toast("COLLECTIONS.REMOVED_FROM_COLLECTION");
                }
                this.globalProgress=false;
                this.refreshContent();
                return;
            }
        this.globalProgress=true;
        this.collectionService.removeFromCollection(nodes[position].ref.id,this.collectionContent.collection.ref.id).subscribe(()=>{
            this.deleteMultiple(nodes,position+1,error);

        },(error:any)=>{
            this.toast.error(error);
            this.deleteMultiple(nodes,position+1,true);
        });
    }
  public closeDialog() {
    this.dialogTitle=null;
  }

  private downloadMaterial() {
    window.open(this.contentDetailObject.downloadUrl);
  }

  private addToOtherCollection(node:EduData.Node) {
    console.log("add to other");
    this.navigate(this.collectionContent.collection.ref.id,node.ref.id);
  }

  private handleError(error: any) {
    if(error.status==RestConstants.DUPLICATE_NODE_RESPONSE){
      this.toast.error(null,'COLLECTIONS.ERROR_NODE_EXISTS');
    }else {
      this.toast.error(error);
    }
  }

  private changeOrder() {
    this.globalProgress=true;
    this.collectionService.setOrder(this.collectionContent.collection.ref.id,RestHelper.getNodeIds(this.collectionContent.references)).subscribe(()=>{
      this.collectionContentOriginal=Helper.deepCopy(this.collectionContent);
      this._orderActive=false;
      this.infoTitle=null;
      this.toast.toast('COLLECTIONS.ORDER_SAVED');
      this.globalProgress=false;
    },(error:any)=>{
      this.globalProgress=false;
      this.toast.error(error);
    });
  }
}
