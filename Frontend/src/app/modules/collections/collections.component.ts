import {Component, OnInit} from '@angular/core';

import {Router, Params, ActivatedRoute} from "@angular/router";
import {RouterComponent} from "../../router/router.component";

import {TranslateService, TranslatePipe} from 'ng2-translate/ng2-translate';
import {Translation} from './../../common/translation';


import * as EduData from "../../common/rest/data-object";

import {RestCollectionService} from "../../common/rest/services/rest-collection.service";
import {RestNodeService} from "../../common/rest/services/rest-node.service";
import {RestIamService} from "../../common/rest/services/rest-iam.service";
import {RestHelper} from "../../common/rest/rest-helper";
import {RestConstants} from "../../common/rest/rest-constants";

import {EduCardComponent} from './edu-card/edu-card.component';
import {GwtInterfaceService, GwtEventListener} from "../../common/services/gwt-interface.service";
import {Toast} from "../../common/ui/toast";
import {RestConnectorService} from "../../common/rest/services/rest-connector.service";
import {LoginResult} from "../../common/rest/data-object";
import {RestOrganizationService} from "../../common/rest/services/rest-organization.service";
import {OrganizationOrganizations} from "../../common/rest/data-object";
import {OptionItem} from "../../common/ui/actionbar/actionbar.component";
import {TemporaryStorageService} from "../../common/services/temporary-storage.service";
import {NodeRenderComponent} from "../../common/ui/node-render/node-render.component";
import {DialogButton} from "../../common/ui/modal-dialog/modal-dialog.component";
import {NodeWrapper} from "../../common/rest/data-object";
import {SearchComponent} from "../search/search.component";
import {ApplyToLmsComponent} from "../../common/ui/apply-to-lms/apply-to-lms.component";
import {FrameEventsService} from "../../common/services/frame-events.service";
import {UIHelper} from "../../common/ui/ui-helper";
import {Title} from "@angular/platform-browser";
import {ConfigurationService} from "../../common/services/configuration.service";
import {SessionStorageService} from "../../common/services/session-storage.service";
import {UIConstants} from "../../common/ui/ui-constants";

// data class for breadcrumbs
export class Breadcrumb {
    ref:EduData.Reference;
    name:string;
}

// component class
@Component({
  selector: 'app-collections',
  templateUrl: 'collections.component.html',
  styleUrls: ['collections.component.scss'],
  providers: [GwtInterfaceService]
})
export class CollectionsMainComponent implements GwtEventListener {
    public dialogTitle : string;
    private dialogCancelable = false;
    private dialogMessage : string;
    private dialogButtons : DialogButton[];

    public tabSelected:string = RestConstants.COLLECTIONSCOPE_MY;
    public isLoading:boolean = true;
    public isReady:boolean = false;
    private clearSearchOnNextStateChange:boolean = false;

    public collectionContent:EduData.CollectionContent;
    private filteredOutCollections:Array<EduData.Collection> = new Array<EduData.Collection>();
    private filteredOutReferences:Array<EduData.CollectionReference> = new Array<EduData.CollectionReference>();
    private collectionIdParamSubscription:any;
    public lastError:string = null;

    private contentDetailObject:any = null;

    private breadcrumbs:Array<Breadcrumb> = new Array<Breadcrumb>();

    // real parentCollectionId is only available, if user was browsing
    private parentCollectionId:EduData.Reference = new EduData.Reference(RestConstants.HOME_REPOSITORY,RestConstants.ROOT);

    private temp:string;
    private lastScrollY:number;

    private person : EduData.User;
    public mainnav = true;
    private path : EduData.Node[];
    private hasOrganizations = false;
    private nodeOptions: OptionItem[]=[];
    public isGuest = true;
    public addToOther:string;
    private showCollection=true;
  // default hides the tabs

    // inject services
    constructor(
      public gwtInterface:GwtInterfaceService,
      private frame : FrameEventsService,
      private temporaryStorageService : TemporaryStorageService,
        private collectionService : RestCollectionService,
        private nodeService : RestNodeService,
        private organizationService : RestOrganizationService,
        private iamService : RestIamService,
      private storage : SessionStorageService,
      private connector : RestConnectorService,
        private route:ActivatedRoute,
        private router : Router,
        private toast : Toast,
      private title:Title,
      private config:ConfigurationService,
        private translationService: TranslateService) {
            this.collectionContent = new EduData.CollectionContent();
            this.collectionContent.setCollectionID(RestConstants.ROOT);
            this.gwtInterface.addListenerOfGwtEvents(this);
            Translation.initialize(this.translationService,this.config,this.storage,this.route).subscribe(()=>{
              UIHelper.setTitle('COLLECTIONS.TITLE',title,translationService,config);
            });

      this.connector.isLoggedIn().subscribe((data:LoginResult)=>{
        if(data.isValidLogin && data.currentScope==null) {
          this.isGuest=data.isGuest;
          if(data.isValidLogin){
            this.organizationService.getOrganizations().subscribe((data:OrganizationOrganizations)=>{
              this.hasOrganizations=data.organizations.length>0;
            });
          }
          this.initialize();
        }else
          UIHelper.goToLogin(this.router,this.config);
      },(error:any)=> UIHelper.goToLogin(this.router,this.config));

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
    selectTab(tab:string){
      if ((this.tabSelected!=tab)
        || (this.collectionContent.getCollectionID()!=RestConstants.ROOT)) {

          this.tabSelected = tab;
          this.collectionContent = new EduData.CollectionContent();
          this.collectionContent.setCollectionID(RestConstants.ROOT);
          this.breadcrumbs = new Array<Breadcrumb>();
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

    selectBreadcrumb(crumb:Breadcrumb, doNavigation:boolean=true) : void {

        // make sure no content is n detail
        this.contentDetailObject = null;

        // cut breadcrumbs
        let valid:boolean = true;
        let cutCrumbs = Array<Breadcrumb>();
        this.breadcrumbs.forEach(oldCrumb => {
            if (oldCrumb.ref.id==crumb.ref.id) valid=false;
            if (valid) cutCrumbs.push(oldCrumb);
        });
        this.breadcrumbs = cutCrumbs;

        // set parent collection based in bread crumb
        if (this.breadcrumbs.length>0) {
            this.parentCollectionId = this.breadcrumbs[(this.breadcrumbs.length-1)].ref;
        } else {
            this.parentCollectionId = new EduData.Reference(RestConstants.HOME_REPOSITORY,RestConstants.ROOT);
        }

        // set thru router so that browser back button can work
        if (doNavigation) {
          this.navigate(crumb.ref.id);
        }

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

    getPrivacyScope(collection:EduData.Collection) : string {
      return collection.scope;
      //  return RestHelper.getPrivacyScope(collection);
    }

    navigateToSearch(){
      this.router.navigate([UIConstants.ROUTER_PREFIX+"search"],{queryParams:{mainnav:this.mainnav}});
    }
    switchToSearch() : void {
      if(this.frame.isRunningInFrame()) {
        console.log("gwtInterface: trying to send signal to GWT for changing to search view " + this.collectionContent.collection.ref.id);
        this.gwtInterface.sendEventSwitchToSearchView(this.collectionContent.collection.ref.id, JSON.stringify(this.breadcrumbs));
      }
      else{
        ApplyToLmsComponent.navigateToSearchUsingReurl(this.router,window.location.href);
      }
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
            this.gwtInterface.sendEvent("clearsearch", null);
        }

        // set correct scope
        let scope=this.tabSelected ? this.tabSelected : RestConstants.COLLECTIONSCOPE_ALL;

        this.collectionService.getCollectionContent(this.collectionContent.collection.ref.id,
          scope,
          [RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR],
          {sortBy:[RestConstants.CM_MODIFIED_DATE],sortAscending:false},
          this.collectionContent.collection.ref.repo
        ).subscribe((collection:EduData.CollectionContent) => {
            console.log(collection);
            this.lastError = null;

            // transfere sub collections and content
            this.collectionContent.collections = collection.collections;
            this.collectionContent.references = collection.references;

            // add an empty collection for the "add new colleciton" card
            if (this.isAllowedToEditCollection()) this.collectionContent.collections.unshift(new EduData.Collection());


            //this.sortCollectionContent();
            this.isLoading=false;
            if(callback)
              callback();
        });


        if ((this.collectionContent.getCollectionID()!="-root-") && (this.collectionContent.collection.permission==null)) {
            this.nodeService.getNodePermissions(this.collectionContent.getCollectionID()).subscribe( permission => {
                this.collectionContent.collection.permission = permission.permissions;
            });
        }

    }

    onCollectionsClick(collection:EduData.Collection) : void {

        // check if click was on new collections placeholder
        if (collection.ref==null) {
            this.router.navigate([UIConstants.ROUTER_PREFIX+"collections/collection", "new", this.collectionContent.collection.ref.id],{queryParams:{mainnav:this.mainnav}});
            return;
        }

        // remember actual collection as breadcrumb
        if (!this.isRootLevelCollection()) {

            let crumb = new Breadcrumb();
            crumb.ref = this.collectionContent.collection.ref;
            crumb.name =this.collectionContent.collection.title;
            this.parentCollectionId = crumb.ref;
            this.breadcrumbs.push(crumb);
        }

        // set thru router so that browser back button can work
      this.navigate(collection.ref.id);

    }
    deleteReference(content:EduData.CollectionReference){
      this.contentDetailObject=content;
      this.deleteFromCollection();
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
            NodeRenderComponent.close();
          })));
        }
        if(!this.isGuest && content.originalId)
          this.nodeOptions.push(new OptionItem("COLLECTIONS.DETAIL.ADD_TO_OTHER", "layers", () => {
            this.addToOtherCollection(data.node);
          }));
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

    onGwtEvent(command:string, message:any) : void {
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

                    // set breadcrumb
                    var crumb:Breadcrumb = new Breadcrumb();
                    crumb.name = collection.collection.title;
                    crumb.ref = collection.collection.ref;
                    this.selectBreadcrumb(crumb, false);

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
    this.nodeService.getNodeParents(this.collectionContent.collection.ref.id).subscribe((data: EduData.NodeList) => {
      console.log(data);
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



    // make sure the top area search field is clean
    this.gwtInterface.sendEvent("clearsearch", null);


    // load user profile
    this.iamService.getUser().subscribe( iamUser => {
      // WIN

      this.person = iamUser.person;

      // set app to ready state
      this.gwtInterface.addListenerOfGwtEvents(this);
      this.isReady = true;

      // subscribe to parameters of url
      this.collectionIdParamSubscription = this.route.queryParams.subscribe(params => {
        console.log(params);
        if(params['scope'])
          this.tabSelected=params['scope'];
        if(this.isGuest)
          this.tabSelected=RestConstants.COLLECTIONSCOPE_ALL;
        if(params['mainnav'])
          this.mainnav=params['mainnav']=='true';

        // get id from route and validate input data
        var id = params['id'] || '-root-';
        if (id==":id") id = "-root-";
        if (id=="") id = "-root-";
        if(params['addToOther']){
          this.addToOther=params['addToOther'];
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
      this.collectionService.removeFromCollection(this.contentDetailObject.ref.id,this.collectionContent.collection.ref.id).subscribe(()=>{
        this.toast.toast("COLLECTIONS.REMOVED_FROM_COLLECTION");
        this.refreshContent();
        if(callback) callback();
      },(error:any)=>this.toast.error(error));
  }

  private closeDialog() {
    this.dialogTitle=null;
  }

  private downloadMaterial() {
    window.open(this.contentDetailObject.downloadUrl);
  }
  private cancelAddToOther(){
      this.navigate(this.collectionContent.collection.ref.id);
      this.addToOther=null;
  }
  private storeToOtherCollection(collection:EduData.Node[]){
      this.collectionService.addNodeToCollection(collection[0].ref.id,this.addToOther).subscribe(()=>{
        this.toast.toast("COLLECTIONS.ADDED_TO_COLLECTION");
        this.addToOther=null;
        this.navigate(collection[0].ref.id);
      },(error:any)=>{
        this.handleError(error);
      })
  }
  private addToOtherCollection(node:EduData.Node) {
    //NodeRenderComponent.close();
    this.navigate(this.collectionContent.collection.ref.id,node.ref.id);
  }

  private handleError(error: any) {
    if(error.status==RestConstants.DUPLICATE_NODE_RESPONSE){
     this.toast.error(null,'COLLECTIONS.ERROR_NODE_EXISTS');
    }else {
      this.toast.error(error);
    }
  }
}
