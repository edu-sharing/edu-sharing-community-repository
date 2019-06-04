import {Component, Input, ViewChild} from "@angular/core";
import {ListItem, RestArchiveService} from "../../../core-module/core.module";
import {RestConstants} from "../../../core-module/core.module";
import {OptionItem} from "../../../common/ui/actionbar/option-item";
import {RecycleRestoreComponent} from "./restore/restore.component";
import {TranslateService} from "@ngx-translate/core";
import {Toast} from "../../../common/ui/toast";
import {ArchiveRestore,Node} from "../../../core-module/core.module";
import {TemporaryStorageService} from "../../../core-module/core.module";

@Component({
  selector: 'recycle',
  templateUrl: 'recycle.component.html'
})
export class RecycleMainComponent {
  @ViewChild('list') list : NodeList;
  public toDelete:Node[] = [];
  public restoreResult:ArchiveRestore;

  @Input() isInsideWorkspace = false;
  @Input() searchWorkspace:string;
  public reload : Boolean;
  public sortBy = RestConstants.CM_ARCHIVED_DATE;
  public sortAscending = false;
  private selected:Node[] = [];

  public columns : ListItem[]=[];
  public options : OptionItem[]=[];
  public fullscreenLoading:boolean;
  loadData(currentQuery :string,offset : number,sortBy : string,sortAscending : boolean){
    return this.archive.search(currentQuery,"",{propertyFilter:[RestConstants.CM_ARCHIVED_DATE],offset:offset,sortBy:[sortBy],sortAscending:sortAscending})
  }
  constructor(private archive : RestArchiveService,private toast : Toast,private translate : TranslateService,private service : TemporaryStorageService){
    this.columns.push(new ListItem("NODE",RestConstants.CM_NAME));
    this.columns.push(new ListItem("NODE",RestConstants.CM_ARCHIVED_DATE));
    this.options.push(new OptionItem("RECYCLE.OPTION.RESTORE_SINGLE","undo", (node : Node) => this.restoreSingle(node)));
    this.options.push(new OptionItem("RECYCLE.OPTION.DELETE_SINGLE","delete", (node : Node) => this.deleteSingle(node)));

  }
  public onSelection(data:Node[]){
    this.selected=data;
  }
  private restoreFinished(list: Node[], restoreResult: any){
    this.fullscreenLoading=false;
    console.log(restoreResult);

    RecycleRestoreComponent.prepareResults(this.translate,restoreResult);
    if(restoreResult.hasDuplicateNames || restoreResult.hasParentFolderMissing)
      this.restoreResult=restoreResult;

    if(list.length==1){
      this.toast.toast("RECYCLE.TOAST.RESTORE_FINISHED_SINGLE");//,{link : 'TODO'},{enableHTML:true});
    }
    else
      this.toast.toast("RECYCLE.TOAST.RESTORE_FINISHED");
    this.reload=new Boolean(true);

  }
  private delete() : void{
    console.log(this.selected);
    this.deleteNodes(this.selected);
  }
  private deleteSingle(node : Node) : void{
    if(node==null){
      this.delete();
      return;
    }
    this.deleteNodes([node]);
  }

  public deleteNodesWithoutConfirmation(list = this.toDelete){
    this.fullscreenLoading=true;
    this.archive.delete(list).subscribe(
      (result) => this.deleteFinished(),
      error => this.handleErrors(error),
    );
  }

  private deleteFinished() {
    this.fullscreenLoading=false;
    this.toast.toast('RECYCLE.TOAST.DELETE_FINISHED');
    this.reload=new Boolean(true);

  }
  private deleteNodes(list : Node[]){
    if(this.service.get("recycleSkipDeleteConfirmation",false)){
      this.deleteNodesWithoutConfirmation(list);
      return;
    }

    this.toDelete=[];
    for(var i=0;i<list.length;i++){
      this.toDelete.push(list[i]);
    }
  }
  private restoreNodesEvent(event : any){
    this.restoreNodes(event.nodes,event.parent);
  }
  private finishRestore(){
    this.restoreResult=null;
  }
  private finishDelete(){
    this.toDelete=null;
  }
  public restoreNodes(list : Node[],toPath=""){
    // archiveRestore list
    this.fullscreenLoading=true;
    this.archive.restore(list,toPath)
      .subscribe(
        (result:ArchiveRestore) => this.restoreFinished(list,result),
        (error:any) => this.handleErrors(error),
      );

  }
  private handleErrors(error: any) {
    this.toast.error(error);
    this.fullscreenLoading=false;
  }

  private restoreSingle(node : Node) : void{
    if(node==null){
      this.restore();
      return;
    }

    this.restoreNodes([node]);
  }
  private restore() : void{
    this.restoreNodes(this.selected);
  }
}
