import {Component, Input, Output, EventEmitter, OnInit, HostListener} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {RestIamService} from "../../rest/services/rest-iam.service";
import {RestNodeService} from "../../rest/services/rest-node.service";
import {RestConnectorService} from "../../rest/services/rest-connector.service";
import {Node, NodeList, IamUsers, IamUser, CollectionContent, Collection} from "../../rest/data-object";
import {RestConstants} from "../../rest/rest-constants";
import {RestCollectionService} from "../../rest/services/rest-collection.service";
import {Toast} from "../toast";
import {ListItem} from "../list-item";

@Component({
  selector: 'collection-chooser',
  templateUrl: 'collection-chooser.component.html',
  styleUrls: ['collection-chooser.component.scss'],
})
/**
 * An edu-sharing sidebar dialog for adding data to a collection
 */
export class CollectionChooserComponent implements OnInit{
  private hasMoreToLoad : boolean;
  private searchQuery : string;
  private columns:ListItem[]=[];
  private sortBy: string[];
  private isLoading=true;
  ngOnInit(): void {
    this.loadData(true);
  }
  public list : Collection[];
  /**
   * The caption of the dialog, will be translated automatically
   */
  @Input() title : string;
  /**
   * Fired when an element is choosen, a (collection) Node will be send as a result
   * @type {EventEmitter}
   */
  @Output() onChoose = new EventEmitter();
  /**
   * Fired when the dialog should be closed
   * @type {EventEmitter}
   */
  @Output() onCancel = new EventEmitter();


  private clickCollection(node:Node){
    if(node.access.indexOf(RestConstants.ACCESS_WRITE)==-1){
      this.toast.error(null,"NO_WRITE_PERMISSIONS");
      return;
    }
    this.onChoose.emit(node);
  }
  constructor(private connector : RestConnectorService,
              private iam : RestIamService,
              private collectionApi : RestCollectionService,
              private node : RestNodeService,
              private toast : Toast,
              private translate : TranslateService) {
    // http://plnkr.co/edit/btpW3l0jr5beJVjohy1Q?p=preview
    this.columns.push(new ListItem("NODE",RestConstants.CM_NAME));
    this.columns.push(new ListItem("COLLECTION","info"));
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
  private loadData(reset=false) {
    this.isLoading=true;
    if(reset){
      this.list=[];
    }
    this.collectionApi.search(this.searchQuery,{
      sortBy:this.sortBy,
      sortAscending:false,
    }).subscribe((data:CollectionContent)=>{
      this.isLoading=false;
      this.hasMoreToLoad=data.collections.length>0;
      this.list=this.list.concat(data.collections);
      console.log(this.list);
    });
  }

  private cancel() {
    this.onCancel.emit();
  }
}
