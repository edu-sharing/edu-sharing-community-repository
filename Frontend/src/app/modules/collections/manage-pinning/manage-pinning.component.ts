import {Component, Input, Output, EventEmitter} from "@angular/core";
import {DialogButton, RestConstants} from "../../../core-module/core.module";
import {Node,NodeList,NodeWrapper} from "../../../core-module/core.module";
import {Toast} from "../../../core-ui-module/toast";
import {RestSearchService} from "../../../core-module/core.module";
import {RestHelper} from "../../../core-module/core.module";
import {RestNodeService} from "../../../core-module/core.module";
import {Helper} from "../../../core-module/rest/helper";
import {UIService} from "../../../core-module/core.module";
import {RestCollectionService} from "../../../core-module/core.module";
import {TranslateService} from "@ngx-translate/core";

@Component({
  selector: 'es-collections-manage-pinning',
  templateUrl: 'manage-pinning.component.html',
  styleUrls: ['manage-pinning.component.scss']
})
export class CollectionManagePinningComponent {
  public pinnedCollections: Node[];
  public currentDragColumn:Node;
  public isMobile: boolean;
  public checked:string[]=[];
  public loading = false;
  buttons: DialogButton[];
  @Input() set addCollection (addCollection : Node) {
    this.loading = true;
    this.toast.showProgressDialog();
    this.search.searchByProperties([RestConstants.CCM_PROP_COLLECTION_PINNED_STATUS],["true"],["="],
      RestConstants.COMBINE_MODE_AND,RestConstants.CONTENT_TYPE_COLLECTIONS,{sortBy:[RestConstants.CCM_PROP_COLLECTION_PINNED_ORDER],sortAscending:true,count:RestConstants.COUNT_UNLIMITED}).subscribe((data:NodeList)=>{
      this.pinnedCollections=data.nodes;
      for(let collection of this.pinnedCollections){
        // collection is already pinned, don't add it
        if(collection.ref.id === addCollection.ref.id){
          this.setAllChecked();
          this.toast.closeModalDialog();
          this.loading = false;
          return;
        }
      }
      this.node.getNodeMetadata(addCollection.ref.id).subscribe((add:NodeWrapper)=>{
        this.pinnedCollections.splice(0,0,add.node);
        this.toast.closeModalDialog();
        this.setAllChecked();
        this.loading = false;
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
  public moveUp(pos:number, event?: Event){
    Helper.arraySwap(this.pinnedCollections,pos,pos-1);
    if (event instanceof KeyboardEvent) {
      setTimeout(() => {
        (event.target as HTMLElement).focus();
      });
    }
  }
  public moveDown(pos:number){
    Helper.arraySwap(this.pinnedCollections,pos,pos+1);
  }
  public getName(collection:Node) : string {
    return RestHelper.getTitle(collection);
  }
  dragStartColumn(event:any,index:number,node : Node){
    event.dataTransfer.effectAllowed = 'all';
    event.dataTransfer.setData("text",index);
    this.currentDragColumn=node;
  }
  allowDragColumn(event:any,index:number,target:Node){
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
  dropColumn(event:any,index:number,target:Node){
    this.currentDragColumn=null;
    event.preventDefault();
    event.stopPropagation();
  }

  public apply(){
    this.toast.showProgressDialog();
    this.loading = true;
    let collections:string[]=[];
    for(let collection of this.pinnedCollections){
      if(this.isChecked(collection))
        collections.push(collection.ref.id);
    }
    this.collection.setPinning(collections).subscribe(()=>{
      this.onClose.emit();
      this.toast.toast('COLLECTIONS.PINNING.UPDATED');
      this.toast.closeModalDialog();
    },(error)=>{
      this.toast.error(error);
      this.toast.closeModalDialog();
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
  }
  private setAllChecked() {
    for(let collection of this.pinnedCollections){
      this.checked.push(collection.ref.id);
    }
  }
}
