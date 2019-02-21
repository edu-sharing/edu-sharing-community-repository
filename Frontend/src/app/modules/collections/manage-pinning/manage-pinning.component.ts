import {Component, Input, Output, EventEmitter} from "@angular/core";
import {RestConstants} from "../../../common/rest/rest-constants";
import {Node,NodeList,NodeWrapper} from "../../../common/rest/data-object";
import {Toast} from "../../../common/ui/toast";
import {RestSearchService} from "../../../common/rest/services/rest-search.service";
import {RestHelper} from "../../../common/rest/rest-helper";
import {RestNodeService} from "../../../common/rest/services/rest-node.service";
import {Helper} from "../../../common/helper";
import {UIService} from "../../../common/services/ui.service";
import {RestCollectionService} from "../../../common/rest/services/rest-collection.service";
import {TranslateService} from "@ngx-translate/core";
import {DialogButton} from '../../../common/ui/modal-dialog/modal-dialog.component';

@Component({
  selector: 'collections-manage-pinning',
  templateUrl: 'manage-pinning.component.html',
  styleUrls: ['manage-pinning.component.scss']
})
export class CollectionManagePinningComponent {
  public pinnedCollections: Node[];
  public currentDragColumn:Node;
  public isMobile: boolean;
  public globalProgress: boolean;
  public checked:string[]=[];
  buttons: DialogButton[];
  @Input() set addCollection (addCollection : string){
    this.globalProgress=true;
    this.search.searchByProperties([RestConstants.CCM_PROP_COLLECTION_PINNED_STATUS],["true"],["="],
      RestConstants.COMBINE_MODE_AND,RestConstants.CONTENT_TYPE_COLLECTIONS,{sortBy:[RestConstants.CCM_PROP_COLLECTION_PINNED_ORDER],sortAscending:true}).subscribe((data:NodeList)=>{
      this.pinnedCollections=data.nodes;
      console.log(data.nodes);
      for(let collection of this.pinnedCollections){
        // collection is already pinned, don't add it
        if(collection.ref.id==addCollection){
          this.setAllChecked();
          this.globalProgress=false;
          return;
        }
      }
      this.node.getNodeMetadata(addCollection).subscribe((add:NodeWrapper)=>{
        this.pinnedCollections.splice(0,0,add.node);
        this.globalProgress=false;
        this.setAllChecked();
      });
    });
  }
  @Output() onClose=new EventEmitter();
  constructor(private search : RestSearchService,
              private toast : Toast,
              private ui: UIService,
              private node : RestNodeService,
              private collection : RestCollectionService,
              private translate : TranslateService
             ){
    this.isMobile=ui.isMobile();
    this.buttons=[
        new DialogButton('CANCEL',DialogButton.TYPE_CANCEL,()=>this.cancel()),
        new DialogButton('APPLY',DialogButton.TYPE_PRIMARY,()=>this.apply())
    ]
  }
  public isChecked(collection:Node){
    return this.checked.indexOf(collection.ref.id)!=-1;
  }
  public moveUp(pos:number){
    Helper.arraySwap(this.pinnedCollections,pos,pos-1);
  }
  public moveDown(pos:number){
    Helper.arraySwap(this.pinnedCollections,pos,pos+1);
  }
  public getName(collection:Node) : string {
    return RestHelper.getTitle(collection);
  }
  private dragStartColumn(event:any,index:number,node : Node){
    event.dataTransfer.effectAllowed = 'all';
    event.dataTransfer.setData("text",index);
    this.currentDragColumn=node;
  }
  private allowDragColumn(event:any,index:number,target:Node){
    console.log(event);
    if(!this.currentDragColumn)
      return;
    event.preventDefault();
    event.stopPropagation();
    if(this.currentDragColumn==target) {
      return;
    }
    let posOld=this.pinnedCollections.indexOf(this.currentDragColumn);
    Helper.arraySwap(this.pinnedCollections,posOld,index);
  }
  private dropColumn(event:any,index:number,target:Node){
    this.currentDragColumn=null;
    event.preventDefault();
    event.stopPropagation();
  }

  public apply(){
    this.globalProgress=true;
    let collections:string[]=[];
    for(let collection of this.pinnedCollections){
      if(this.isChecked(collection))
        collections.push(collection.ref.id);
      this.toast.toast('COLLECTIONS.PINNING.UPDATED');
    }
    this.collection.setPinning(collections).subscribe(()=>{
      this.onClose.emit();
      this.globalProgress=false;
    },(error:any)=>{
      this.toast.error(error);
      this.globalProgress=false;
    });
  }
  public cancel(){
    this.onClose.emit();
  }
  public setChecked(collection:Node,event:any){
    if(this.isChecked(collection)){
      this.checked.splice(this.checked.indexOf(collection.ref.id),1);
    }
    else{
      this.checked.push(collection.ref.id);
    }
    console.log(this.checked);
  }
  private setAllChecked() {
    for(let collection of this.pinnedCollections){
      this.checked.push(collection.ref.id);
    }
  }
}
