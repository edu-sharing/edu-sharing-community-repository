import {Component, Input} from "@angular/core";
import {RestArchiveService} from "../../../common/rest/services/rest-archive.service";
import {RestConstants} from "../../../common/rest/rest-constants";
import {OptionItem} from "../../../common/ui/actionbar/actionbar.component";
import {RecycleRestoreComponent} from "./restore/restore.component";
import {TranslateService} from "@ngx-translate/core";
import {Toast} from "../../../common/ui/toast";
import {ArchiveRestore,Node} from "../../../common/rest/data-object";
import {TemporaryStorageService} from "../../../common/services/temporary-storage.service";
import {RestConnectorService} from "../../../common/rest/services/rest-connector.service";
import {ListItem} from "../../../common/ui/list-item";

@Component({
  selector: 'recycle',
  templateUrl: 'recycle.component.html'
})
export class RecycleMainComponent {
  public toDelete:Node[] = [];
  public restoreResult:ArchiveRestore;

  @Input() isInsideWorkspace = false;
  @Input() searchWorkspace:string;
  public reload : Boolean;
  private selected:Node[] = [];

  public columns : ListItem[]=[];
  public options : OptionItem[]=[];
  public fullscreenLoading:boolean;
  private loadData(currentQuery :string,offset : number,sortBy : string,sortAscending : boolean){
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
