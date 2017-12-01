import {Component, OnInit, NgZone, HostListener} from '@angular/core';


import {Router, Params, ActivatedRoute} from "@angular/router";

import {TranslateService, TranslatePipe} from 'ng2-translate/ng2-translate';
import {Translation} from './../../../common/translation';

import * as EduData from "../../../common/rest/data-object";

import {RestCollectionService} from "../../../common/rest/services/rest-collection.service";
import {RestNodeService} from "../../../common/rest/services/rest-node.service";
import {RestConstants} from "../../../common/rest/rest-constants";
import {RestHelper} from "../../../common/rest/rest-helper";
import {GwtInterfaceService} from "../../../common/services/gwt-interface.service";
import {Toast} from "../../../common/ui/toast";
import {RestIamService} from "../../../common/rest/services/rest-iam.service";
import {IamUser} from "../../../common/rest/data-object";
import {User} from "../../../common/rest/data-object";
import {LocalPermissions} from "../../../common/rest/data-object";
import {Collection} from "../../../common/rest/data-object";
import {RestConnectorService} from "../../../common/rest/services/rest-connector.service";
import {ConfigurationService} from "../../../common/services/configuration.service";
import {SessionStorageService} from "../../../common/services/session-storage.service";
import {UIConstants} from "../../../common/ui/ui-constants";

// component class
@Component({
  selector: 'app-collection-new',
  templateUrl: 'collection-new.component.html',
  styleUrls: ['collection-new.component.scss'],
  providers: [GwtInterfaceService]
})
export class CollectionNewComponent {
  public hasCustomScope: boolean;
  public COLORS1=['#975B5D','#692426','#E6B247','#A89B39','#699761','#32662A'];
  public COLORS2=['#60998F','#29685C','#759CB7','#537997','#976097','#692869'];
    public static SCOPE_MYCOLLECTIONS:number = 0;
    public static SCOPE_MYORGANIZATIONS:number = 1;
    public static SCOPE_ALLCOLLECTIONS:number = 2;

    public isLoading:boolean = false;
    public isReady:boolean = false;

    private paramSubscription:any;
    public lastError:string = null;

    private parentId:string; // if not null --> new collection
    private editId:string; // if not null --> edit collection

    private currentCollection:Collection;
    // new collection as state of page until router confusion settles down
    public newCollectionStep:number;
    private newCollectionName:string;
    private newCollectionDescription:string;
    private newCollectionScope:string;
    private newCollectionColor:string;

    // on edit of icon of collection is already set
    private previewUrl:string;

    private imageShow:boolean = false;
    private imageData:string = null;
    private imageFile:File = null;

    private hasUserAnyOrgasYet = false;
    private user : User;
    public mainnav = false;
    public editPermissionsId: string;
    private permissions: LocalPermissions = null;
    private shareToAll: boolean;

  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
    if(event.code=="Escape"           ){
      event.preventDefault();
      event.stopPropagation();
      this.newCollectionCancel();
      return;
    }
  }
    // inject services
    constructor(
        private gwtInterface:GwtInterfaceService,
        private collectionService : RestCollectionService,
        private nodeService : RestNodeService,
        private connector : RestConnectorService,
        private iamService : RestIamService,
        private route:ActivatedRoute,
        private router: Router,
        private toast : Toast,
        private storage : SessionStorageService,
        private zone: NgZone,
        private config : ConfigurationService,
        private translationService:TranslateService) {

        Translation.initialize(this.translationService,this.config,this.storage,this.route).subscribe(()=>{});
        this.iamService.getUser().subscribe((user : IamUser) => this.user=user.person);
        // subscribe to paramter
      this.paramSubscription = this.route.queryParams.subscribe(params => {
        this.mainnav=params['mainnav']=='true';
      });
        this.paramSubscription = this.route.params.subscribe(params => {

            // get id from route and validate input data
            var id = params['id'];
            if (typeof id == "undefined") id = RestConstants.ROOT;
            if (id=="") id = RestConstants.ROOT;
            if (id==":id") id = RestConstants.ROOT;

            // get mode from route and validate input data
            var mode = params['mode'];
            if (typeof mode == "undefined") mode = "new";
            if (mode=="") mode = "new";
            if (mode==":mode") mode = "new";

            if (mode=="edit") {
                this.editId = id;
                this.parentId = null;
            } else {
                this.editId = null;
                this.parentId = id;
            }

            this.newCollectionStep = 1;
            this.newCollectionName = "";
            this.newCollectionDescription = "";
            this.newCollectionScope = RestConstants.COLLECTIONSCOPE_ALL;
            this.newCollectionColor = "#975B5D";


            this.collectionService.getOrganizations().subscribe((orgaList) => {
              console.log(orgaList);
              if (orgaList.organizations.length>0) this.hasUserAnyOrgasYet = true;
              this.isLoading = false;
              this.isReady = true;
           });
            // on edit case load values of collection
            if (this.editId!=null) {
                this.isLoading = true;

                this.collectionService.getCollection(this.editId).subscribe((collection) => {
                    console.log(collection);
                    this.newCollectionName = collection.collection.title;
                    this.newCollectionDescription = collection.collection.description;
                    this.previewUrl = collection.collection.preview.isIcon ? null :  collection.collection.preview.url;
                    if (collection.collection.color!=null) this.newCollectionColor = collection.collection.color;
                  this.newCollectionScope = collection.collection.scope;

                  this.isLoading = false;
                  this.isReady = true;
                });

            } else {
                this.isLoading = false;
                this.isReady = true;
            }

        });


    }
    private saveCollection(){
       this.collectionService.updateCollection(this.currentCollection).subscribe(()=>{
        this.navigateToCollectionId(this.currentCollection.ref.id);
      },(error:any)=>this.toast.error(error));
    }
    private updatePermissions(){
      this.currentCollection.scope=this.newCollectionScope;
      this.isLoading=true;
      if(this.permissions){
        this.nodeService.setNodePermissions(this.currentCollection.ref.id,this.permissions).subscribe(()=>{
          this.permissions=null;
          this.saveCollection();
        },(error:any)=>{
          this.toast.error(error);
          this.isLoading=false;
        });
        return;
      }
      this.saveCollection();

    }
    private permissionsSave(permissions : LocalPermissions){
      this.permissions=permissions;
      this.editPermissionsId=null;
    }
    private editPermissions(){
      if(!this.hasCustomScope && this.permissions==null)
        this.permissions=new LocalPermissions();
      this.newCollectionScope=RestConstants.COLLECTIONSCOPE_CUSTOM;
      this.editPermissionsId=this.currentCollection.ref.id;
    }
    isNewCollection() : boolean {
        return this.editId==null;
    }

    isEditCollection() : boolean {
        return !this.isNewCollection();
    }

    newCollectionCancel() : void {
        var id = this.parentId;
        if (id==null) id = this.editId;
        this.navigateToCollectionId(id);
    }

    newCollectionContinue() : void {



        // prepare image data
        this.imageShow = false;
        this.imageData = null;

        // show next stepp
        this.newCollectionStep=2;
    }
    newCollectionGoBack(){
      this.newCollectionStep=1;
    }

    setColor(color:string) : void {
        this.newCollectionColor = color;
    }

    imageDataChanged(event:any) : void {
        // get files and check if available
        var files = event.target.files;
        if (typeof files == "undefined") {
            console.log("files = undefined -> ignoring");
            return;
        }
        if (files.length<=0) {
           console.log("files.length = 0 -> ignoring");
            return;
        }

        // get first file
        var file:File = files[0];

        // check if file type is correct
        var validType = false;
        if (file.type.startsWith("image")) validType = true;
        //if (file.type=="image/jpeg") validType = true;
        //if (file.type=="image/gif") validType = true;
        if (!validType) {
            return;
        }


        // remember file for upload
        this.imageFile = file;
        // read file base64
        var reader  = new FileReader();
        reader.addEventListener("load", () => {
            this.imageData = reader.result;
            this.imageShow = true;
        });
        reader.readAsDataURL(file);

    }
    handleError(error:any){
      if(error.status==RestConstants.DUPLICATE_NODE_RESPONSE){
        this.toast.error(null,"COLLECTIONS.TOAST.DUPLICATE_NAME");
        return;
      }
      if(RestHelper.errorMessageContains(error,"Invalid property value")){
        this.toast.error(null,"COLLECTIONS.TOAST.INVALID_NAME");
        return;
      }
      this.toast.error(error);
    }
    newCollectionCreate() : void {
        // input data optimize
        this.newCollectionName = this.newCollectionName.trim();
        this.newCollectionDescription = this.newCollectionDescription.trim();

        // validate input --> TODO: Dialogs or micro interaction later
        if (this.newCollectionName.length==0) {
          this.toast.error(null,"collectionNew_askName");
          return;
        }
        if (this.isEditCollection()) {

            /*
             *  EDIT
             */

            this.isLoading = true;

            this.collectionService.getCollection(this.editId).subscribe((collectionWrapper) => {

                // update with user set data
                collectionWrapper.collection.color = this.newCollectionColor;
                collectionWrapper.collection.title = this.newCollectionName;
                collectionWrapper.collection.description = this.newCollectionDescription;
                collectionWrapper.collection.scope=this.newCollectionScope;
                // null fields that should ne ignored
                collectionWrapper.collection.owner = null;

                this.collectionService.updateCollection(collectionWrapper.collection).subscribe( result => {

                    // update image if needed
                    this.uploadImageIfSetOrChanged(collectionWrapper.collection, () => {

                        // finally UPDATE PERMISSIONS and than it will navigate to collection
                        //this.updateLocalPermissions(collectionWrapper.collection);
                        this.showPermissions(collectionWrapper.collection);
                    });

                },(error:any)=>{
                  this.handleError(error);
                  this.isLoading=false;
                });

            });


            return;

        } else {

            /*
             *  CREATE
             */
            this.connector.hasToolPermission(RestConstants.TOOLPERMISSION_INVITE_ALLAUTHORITIES).subscribe((has:boolean)=>{
              if(!has)
                this.newCollectionScope=RestConstants.COLLECTIONSCOPE_MY;
              this.isLoading = true;

              this.collectionService.createCollection(
                this.newCollectionName,
                this.newCollectionDescription,
                this.newCollectionColor,
                this.newCollectionScope,
                this.parentId

              ).subscribe((collection:EduData.CollectionWrapper) => {

                // update image if set
                this.uploadImageIfSetOrChanged(collection.collection, () => {
                  // finally UPDATE PERMISSIONS and than it will navigate to collection
                  //this.updateLocalPermissions(collection.collection);
                  this.showPermissions(collection.collection);

                });

              },(error:any)=>{
                this.isLoading=false;
                this.handleError(error);
              });
            });


        }

    }

    // uploads image if needed and calls callback with ...
    // null --> if no image or not changed
    // image url --> if image new or changed
    private uploadImageIfSetOrChanged(collection:EduData.Collection, callbackFunction:any) : void {

             if ((this.imageShow) && (this.imageData!=null) && (this.imageData).startsWith("data:")) {
                 // image new or changed
                 this.collectionService.uploadCollectionImage(collection.ref.id, this.imageFile, "image/png").subscribe(() => {
                     callbackFunction();
                 });
             } else {

                 // no image or not changed
                callbackFunction();

             }

    }

    showPermissions(id:Collection){
      if(this.isNewCollection()){
        this.toast.toast('COLLECTIONS.TOAST.CREATED');
      }
      this.connector.hasToolPermission(RestConstants.TOOLPERMISSION_INVITE).subscribe((has:boolean)=>{
        if(has){
          this.newCollectionStep=2;
          this.hasCustomScope=this.newCollectionScope==RestConstants.COLLECTIONSCOPE_CUSTOM;
          this.currentCollection=id;
          this.shareToAll=this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_INVITE_ALLAUTHORITIES);
          this.isLoading=false;
        }
        else{
          this.navigateToCollectionId(id.ref.id);
        }
      });

    }
    navigateToCollectionId(id:string) : void {
      this.isLoading = false;
      this.router.navigate([UIConstants.ROUTER_PREFIX+'collections'], {queryParams:{id:id,mainnav:this.mainnav}});
    }

}
