import {Component, Input, Output, EventEmitter} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {ArchiveRestore,Node} from "../../../../common/rest/data-object";

@Component({
  selector: 'recycle-restore-info',
  templateUrl: 'restore.component.html',
  styleUrls : ['restore.component.css']
})

export class RecycleRestoreComponent {
  @Input() results: any;
  @Output() onClose=new EventEmitter();
  @Output() onRestoreFolder=new EventEmitter();
  public showFileChooser : Boolean;
  public confirm() : void{
    this.onClose.emit();
  }
  private cancel() : void{
    this.onClose.emit();
  }
  private chooseDirectory() : void{
    this.showFileChooser=new Boolean(true);
  }
  private closeFolder(){
    this.showFileChooser=false;
  }
  private folderSelected(event : Node[]){
    console.log(event);
    let nodes:any[]=[];
    for(let result of this.results.results){
      if((result.restoreStatus as any)==1)
        nodes.push({ref:{nId:result.nodeId}});
    }
    this.showFileChooser=false;
    //this.appComponent.restoreNodes(nodes,event.ref.id);
    this.onRestoreFolder.emit({nodes:nodes,parent:event[0].ref.id});
    this.cancel();
  }
  public static get STATUS_FINE(): string { return 'FINE'; }
  public static get STATUS_DUPLICATENAME(): string { return 'DUPLICATENAME'; }
  public static get STATUS_PARENT_FOLDER_MISSING(): string { return 'FALLBACK_PARENT_NOT_EXISTS'; }
  public static get STATUS_PARENT_FOLDER_NO_PERMISSION(): string { return 'FALLBACK_PARENT_NO_PERMISSION'; }

  public static prepareResults(translate : TranslateService,results : any){
    for(let result of results.results){
        if(result.restoreStatus==RecycleRestoreComponent.STATUS_FINE)
          continue;
        translate.get("RECYCLE.RESTORE."+result.restoreStatus).subscribe((text:any)=> result.message=text);
        if(result.restoreStatus==RecycleRestoreComponent.STATUS_DUPLICATENAME) {
          results.hasDuplicateNames = true;
          result.restoreStatus=0;
        }
        if(result.restoreStatus==RecycleRestoreComponent.STATUS_PARENT_FOLDER_MISSING || result.restoreStatus==RecycleRestoreComponent.STATUS_PARENT_FOLDER_NO_PERMISSION) {
          results.hasParentFolderMissing = true;
          result.restoreStatus=1;
        }

    }

  }
}
