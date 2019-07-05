import {Component, Input, Output, EventEmitter, OnInit, HostListener} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {Toast} from "../../toast";
import {Router} from "@angular/router";
import {
    Collection,
    Node,
    ListItem,
    RestCollectionService, RestConnectorService,
    RestConstants,
    UIConstants,
    RestIamService,
    RestNodeService
} from "../../../core-module/core.module";
import {AddElement} from "../../add-element";

@Component({
  selector: 'collection-chooser',
  templateUrl: 'collection-chooser.component.html',
  styleUrls: ['collection-chooser.component.scss'],
})
/**
 * An edu-sharing sidebar dialog for adding data to a collection
 */
export class CollectionChooserComponent implements OnInit{
  public createCollectionElement = new AddElement("COLLECTIONS.CREATE_COLLECTION");
  private hasMoreToLoad : boolean;
  public searchQuery = "";
  public lastSearchQuery = "";
  private columns:ListItem[]=ListItem.getCollectionDefaults();
  private sortBy: string[];
  private sortAscending = false;
  isLoadingLatest=true;
  isLoadingMy=true;
  currentRoot: Collection;
  breadcrumbs: Node[];

  ngOnInit(): void {
    this.loadLatest(true);
    this.loadMy();
  }
  public listLatest : Collection[];
  public listMy : Collection[];
  /**
   * The caption of the dialog, will be translated automatically
   */
  @Input() title : string;
  /**
   * Fired when an element is choosen, a (collection) Node will be send as a result
   * @type {EventEmitter}
   */
  @Output() onChoose = new EventEmitter();
  @Output() onCreateCollection = new EventEmitter();
  /**
   * Fired when a list of nodes is dropped on a collection item
   * @type {EventEmitter}
   */
  @Output() onDrop = new EventEmitter();
  /**
   * Fired when the dialog should be closed
   * @type {EventEmitter}
   */
  @Output() onCancel = new EventEmitter();

  private drop(event:any){
    if(!this.checkPermissions(event.target)) {
      return;
    }
    this.onDrop.emit(event);
  }

  private checkPermissions(node:Collection) {
    if(node.access.indexOf(RestConstants.ACCESS_WRITE)==-1){
      this.toast.error(null,"NO_WRITE_PERMISSIONS");
      return false;
    }
    return true;
  }
  public createCollection(){
    this.onCreateCollection.emit(this.currentRoot ? this.currentRoot : null);
  }
  private hasWritePermissions(node:any){
      if(node.access.indexOf(RestConstants.ACCESS_WRITE)==-1){
          return {status:false,message:'NO_WRITE_PERMISSIONS'};
      }
      return {status:true};
  }
  private goIntoCollection(node:Collection){
    this.currentRoot=node;
    this.loadMy();

  }
  private clickCollection(node:Collection){
    if(!this.checkPermissions(node)){
      return;
    }
    this.onChoose.emit(node);
  }
  constructor(private connector : RestConnectorService,
              private router : Router,
              private iam : RestIamService,
              private collectionApi : RestCollectionService,
              private node : RestNodeService,
              private toast : Toast,
              private translate : TranslateService) {
    // http://plnkr.co/edit/btpW3l0jr5beJVjohy1Q?p=preview
    this.sortBy=[RestConstants.CM_MODIFIED_DATE];
  }
  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
    if(event.code=="Escape"){
      event.preventDefault();
      event.stopPropagation();
      this.cancel();
      return;
    }
  }
  public loadLatest(reset=false) {
    if(reset){
      this.listLatest=[];
      this.lastSearchQuery=this.searchQuery;
    }
    else if(!this.hasMoreToLoad){
      return;
    }
    this.isLoadingLatest=true;
    this.collectionApi.search(this.lastSearchQuery,{
      sortBy:this.sortBy,
      offset:this.listLatest.length,
      sortAscending:false,
    }).subscribe((data)=>{
      this.isLoadingLatest=false;
      this.hasMoreToLoad=data.collections.length>0;
      this.listLatest=this.listLatest.concat(data.collections);
    });
  }

  public cancel() {
    this.onCancel.emit();
  }

  private loadMy() {
    this.listMy=[];
    this.breadcrumbs=null;
    this.isLoadingMy=true;
    this.collectionApi.getCollectionSubcollections(this.currentRoot ? this.currentRoot.ref.id : RestConstants.ROOT,RestConstants.COLLECTIONSCOPE_MY,[RestConstants.ALL],{
      sortBy:this.sortBy,
      sortAscending:false,
      count: RestConstants.COUNT_UNLIMITED,
    }).subscribe((data)=>{
      this.isLoadingMy=false;
      this.listMy=this.listMy.concat(data.collections);
    });
    if(this.currentRoot) {
      this.node.getNodeParents(this.currentRoot.ref.id,false).subscribe((list)=>this.breadcrumbs=list.nodes.reverse());
    }
  }

  navigateBack() {
    if(this.breadcrumbs.length>1)
      this.currentRoot=(this.breadcrumbs[this.breadcrumbs.length-2] as any);
    else
      this.currentRoot=null;
    console.log(this.currentRoot);
    this.loadMy();
  }
}
