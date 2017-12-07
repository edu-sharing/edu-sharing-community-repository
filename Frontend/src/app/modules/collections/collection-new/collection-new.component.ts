import {Component, OnInit, NgZone, HostListener, ViewChild} from '@angular/core';


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
import {Group, IamGroups, IamUser, NodeRef, Permission} from "../../../common/rest/data-object";
import {User} from "../../../common/rest/data-object";
import {LocalPermissions} from "../../../common/rest/data-object";
import {Collection} from "../../../common/rest/data-object";
import {RestConnectorService} from "../../../common/rest/services/rest-connector.service";
import {ConfigurationService} from "../../../common/services/configuration.service";
import {SessionStorageService} from "../../../common/services/session-storage.service";
import {UIConstants} from "../../../common/ui/ui-constants";
import {MdsComponent} from "../../../common/ui/mds/mds.component";
import {ListItem} from "../../../common/ui/list-item";

// component class
@Component({
  selector: 'app-collection-new',
  templateUrl: 'collection-new.component.html',
  styleUrls: ['collection-new.component.scss'],
  providers: [GwtInterfaceService]
})
export class CollectionNewComponent {
  @ViewChild('mds') mds : MdsComponent;
  public hasCustomScope: boolean;
  public COLORS1=['#975B5D','#692426','#E6B247','#A89B39','#699761','#32662A'];
  public COLORS2=['#60998F','#29685C','#759CB7','#537997','#976097','#692869'];
  public isLoading:boolean = true;
  public showPermissions = false;
  private currentCollection:Collection;
  public newCollectionType:string;
  public properties:any;
  public reloadMds:Boolean;
  private hasUserAnyOrgasYet = false;
  private user : User;
  public mainnav = true;
  public editPermissionsId: string;
  private permissions: LocalPermissions = null;
  public canInvite: boolean;
  public shareToAll: boolean;
  public createEditorial = false;
  public createCurriculum = false;
  public parentId: any;
  public editId: any;
  public editorialGroups:Group[]=[];
  public editorialGroupsSelected:Group[]=[];
  public editorialColumns:ListItem[]=[new ListItem("GROUP",RestConstants.AUTHORITY_DISPLAYNAME)];
  private imageData:string = null;
  private imageFile:File = null;
  private STEP_NEW = 'NEW';
  private STEP_GENERAL = 'GENERAL';
  private STEP_METADATA = 'METADATA';
  private STEP_PERMISSIONS = 'PERMISSIONS';
  private STEP_SETTINGS = 'SETTINGS';
  private STEP_EDITORIAL_GROUPS = 'EDITORIAL_GROUPS';
  private STEP_ICONS={
    GENERAL:'edit',
    METADATA:'info_outline',
    PERMISSIONS:'group_add',
    SETTINGS:'settings',
    EDITORIAL_GROUPS:'star'
  };
  public newCollectionStep=this.STEP_NEW;
  public editPermissionsDummy: EduData.Node;
  private availableSteps: string[];


  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
    if(event.code=="Escape"){
      event.preventDefault();
      event.stopPropagation();
      this.goBack();
      return;
    }
  }

    constructor(
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
        Translation.initialize(this.translationService,this.config,this.storage,this.route).subscribe(()=>{
          this.connector.hasToolPermission(RestConstants.TOOLPERMISSION_INVITE).subscribe((has:boolean)=>this.canInvite=has);
          this.connector.hasToolPermission(RestConstants.TOOLPERMISSION_INVITE_ALLAUTHORITIES).subscribe((has)=>this.shareToAll=has);
          this.connector.hasToolPermission(RestConstants.TOOLPERMISSION_COLLECTION_EDITORIAL).subscribe((has)=>this.createEditorial=has);
          this.connector.hasToolPermission(RestConstants.TOOLPERMISSION_COLLECTION_CURRICULUM).subscribe((has)=>this.createCurriculum=has);
          this.iamService.getUser().subscribe((user : IamUser) => this.user=user.person);
          this.route.queryParams.subscribe(params => {
            this.mainnav=params['mainnav']=='true';
          });
          this.iamService.searchGroups("*",true,RestConstants.GROUP_TYPE_EDITORIAL,{count:RestConstants.COUNT_UNLIMITED}).subscribe((data:IamGroups)=>{
            this.editorialGroups=data.groups;
          });
          this.route.params.subscribe(params => {
            // get mode from route and validate input data
            let mode = params['mode'];
            let id = params['id'];
            if (mode=="edit") {
              this.collectionService.getCollection(id).subscribe((data:EduData.CollectionWrapper)=>{
                this.nodeService.getNodeMetadata(id,[RestConstants.ALL]).subscribe((node:EduData.NodeWrapper)=>{
                  this.editId=id;
                  this.currentCollection=data.collection;
                  this.properties=node.node.properties;
                  this.newCollectionType=this.getTypeForCollection(this.currentCollection);
                  this.hasCustomScope=false;
                  this.newCollectionStep = this.STEP_GENERAL;
                  this.updateAvailableSteps();
                  this.isLoading=false;
                });
              });
            } else {
              this.parentId = id;
              this.currentCollection=new Collection();
              this.currentCollection.title="";
              this.currentCollection.description="";
              this.currentCollection.color=this.COLORS1[0];
              this.updateAvailableSteps();
              this.isLoading=false;
            }
          });

        });
        // subscribe to paramter


    }
    private saveCollection(){
       this.collectionService.updateCollection(this.currentCollection).subscribe(()=>{
        this.navigateToCollectionId(this.currentCollection.ref.id);
      },(error:any)=>this.toast.error(error));
    }
    private updatePermissions(){
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
    private setPermissions(permissions : LocalPermissions){
      if(permissions) {
        this.permissions = permissions;
        this.permissions.inherited=false;
        if(this.permissions.permissions && this.permissions.permissions.length){
          this.currentCollection.scope=RestConstants.COLLECTIONSCOPE_CUSTOM;
        }
      }
      this.showPermissions=false;
    }
    private editPermissions(){
      if(this.permissions==null && !this.editId) {
        this.permissions = new LocalPermissions();
      }
      if(this.editId) {
        this.editPermissionsId = this.editId;
      }
      else{
        this.editPermissionsDummy=new EduData.Node();
        this.editPermissionsDummy.ref=new NodeRef();
        this.editPermissionsDummy.aspects=[RestConstants.CCM_ASPECT_COLLECTION];
        this.editPermissionsDummy.isDirectory=true;
      }
      this.showPermissions=true;
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
    setColor(color:string) : void {
        this.currentCollection.color = color;
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
    save() : void {
        // input data optimize
        this.currentCollection.title = this.currentCollection.title.trim();
        this.currentCollection.description = this.currentCollection.description.trim();

        if (this.isEditCollection()) {

            /*
             *  EDIT
             */

            this.isLoading = true;
            this.collectionService.updateCollection(this.currentCollection).subscribe(()=>{
              this.save2(this.currentCollection);
            }
            ,(error:any)=>{
              this.isLoading=false;
              this.handleError(error);
            });
        } else {

        /*
         *  CREATE
         */
          this.isLoading = true;
          this.collectionService.createCollection(
            this.currentCollection.title,
            this.currentCollection.description,
            this.currentCollection.color,
            this.currentCollection.scope,
            this.parentId

          ).subscribe((collection:EduData.CollectionWrapper) => {
            this.save2(collection.collection);
          },(error:any)=>{
            this.isLoading=false;
            this.handleError(error);
          });
        }
    }
    private saveImage(collection:EduData.Collection) : void {

       if ((this.imageData!=null) && (this.imageData).startsWith("data:")) {
           this.collectionService.uploadCollectionImage(collection.ref.id, this.imageFile, "image/png").subscribe(() => {
               this.navigateToCollectionId(collection.ref.id);
           });
       } else {
          this.navigateToCollectionId(collection.ref.id);
        }
    }
    setCollectionType(type:string){
      this.newCollectionType=type;
      if(type=='EDU_ALL'){
        this.currentCollection.scope=RestConstants.COLLECTIONSCOPE_ALL;
      }
      if(type=='CUSTOM'){
        this.currentCollection.scope=RestConstants.COLLECTIONSCOPE_CUSTOM;
      }
      this.updateAvailableSteps();
      this.goToNextStep();
    }
    public getAvailableSteps():string[]{
      let steps:string[]=[];
      console.log(this.newCollectionType);
      steps.push(this.STEP_GENERAL);
      if(this.newCollectionType=='EDITORIAL'){
        steps.push(this.STEP_METADATA);
      }
      if(this.newCollectionType=='CUSTOM'){
        steps.push(this.STEP_PERMISSIONS);
      }
      if(this.newCollectionType=='EDITORIAL'){
        //steps.push(this.STEP_SETTINGS);
      }
      if(this.newCollectionType=='EDITORIAL'){
        steps.push(this.STEP_EDITORIAL_GROUPS);
      }
      return steps;
    }
    public isLastStep(){
      let pos=this.currentStepPosition();
      return pos>=this.availableSteps.length-1;
    }
    public goToNextStep(){
      if(this.isLastStep()){
        this.save();
      }
      else{
        let pos=this.currentStepPosition();
        this.newCollectionStep=this.availableSteps[pos+1];
        this.reloadMds=new Boolean(true);
      }

    }
    setCollectionGeneral(){
      if(!this.currentCollection.title){
        this.toast.error(null,'COLLECTIONS.ENTER_NAME');
        return;
      }
      this.goToNextStep();
    }
    currentStepPosition(){
      return this.availableSteps.indexOf(this.newCollectionStep);
    }
    goBack(){
       let pos=this.currentStepPosition();
       if(pos==-1){
         this.navigateToCollectionId(this.parentId);
       }
       else if(pos==0){
         if(this.editId) {
           this.navigateToCollectionId(this.editId);
         }
         else {
           this.newCollectionStep = this.STEP_NEW;
         }
       }
       else{
         this.newCollectionStep = this.availableSteps[pos - 1];
         this.reloadMds=new Boolean(true);
       }
    }
    navigateToCollectionId(id:string) : void {
      this.isLoading = false;
      this.router.navigate([UIConstants.ROUTER_PREFIX+'collections'], {queryParams:{id:id,mainnav:this.mainnav}});
    }
  private syncMetadata(goToNext:boolean){
      this.properties=this.mds.getValues({},goToNext);
      if(goToNext && this.properties!=null){
        this.goToNextStep();
      }
      if(!goToNext){
        this.goBack();
      }
  }
  private save2(collection: Collection) {
    if(this.newCollectionType==RestConstants.GROUP_TYPE_EDITORIAL){
      this.nodeService.editNodeMetadata(collection.ref.id,this.properties).subscribe(()=>{
        this.save3(collection);
      });
    }
    else{
      this.save3(collection);
    }
  }
  private save3(collection:Collection){
    if(this.newCollectionType==RestConstants.GROUP_TYPE_EDITORIAL){
      this.permissions=this.getEditorialGroupPermissions();
    }
    if((this.newCollectionType==RestConstants.COLLECTIONSCOPE_CUSTOM || this.newCollectionType==RestConstants.GROUP_TYPE_EDITORIAL) && this.permissions && this.permissions.permissions && this.permissions.permissions.length){
      this.nodeService.setNodePermissions(collection.ref.id,this.permissions).subscribe(()=>{
        this.saveImage(collection);
      });
    }
    else {
      this.saveImage(collection);
    }
  }

  private getTypeForCollection(collection: Collection) {
    if(collection.scope==RestConstants.COLLECTIONSCOPE_MY || collection.scope==RestConstants.COLLECTIONSCOPE_ORGA || collection.scope==RestConstants.COLLECTIONSCOPE_ALL || collection.scope==RestConstants.COLLECTIONSCOPE_CUSTOM_PUBLIC)
      return RestConstants.COLLECTIONSCOPE_CUSTOM;
    return collection.scope;
  }

  private updateAvailableSteps() {
    this.availableSteps=this.getAvailableSteps();
  }

  private getEditorialGroupPermissions() {
    let permissions=new LocalPermissions();
    permissions.permissions=[];
    for(let group of this.editorialGroupsSelected){
      let perm=new Permission();
      perm.authority={authorityName:group.authorityName,authorityType:group.authorityType};
      perm.permissions=[RestConstants.PERMISSION_COORDINATOR];
      permissions.permissions.push(perm);
    }
    return permissions;
  }
}
