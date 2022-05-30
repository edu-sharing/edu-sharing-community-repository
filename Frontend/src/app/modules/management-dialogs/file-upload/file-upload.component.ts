import {Component, Input, EventEmitter, Output} from '@angular/core';
import {RestNodeService} from "../../../core-module/core.module";
import {Node, NodeWrapper} from "../../../core-module/core.module";
import {RestConstants} from "../../../core-module/core.module";
import {RestHelper} from "../../../core-module/core.module";
import {TimePipe} from "../../../common/ui/time.pipe";
import {TranslateService} from "@ngx-translate/core";

@Component({
  selector: 'es-workspace-file-upload',
  templateUrl: 'file-upload.component.html',
  styleUrls: ['file-upload.component.scss']
})
export class WorkspaceFileUploadComponent  {
  public progress : any;
  private resultList : Node[];
  @Input() current:Node;
  private _files : FileList;
  private error = false;
  public showClose = false;
  processed = 0;
  @Input() set files(files : FileList){
    this._files=files;
    this.progress=[];
    this.error=false;
    this.showClose=false;
    this.resultList=[];
    for(let i=0;i<files.length;i++){
      this.progress.push({name:files.item(i).name,progress:{progress:0}});
    }
    this.upload(0);
  }
  @Output() onDone=new EventEmitter();
  close(){
    this.onDone.emit(this.error ? null : this.resultList);
  }
  private upload(number: number) {
    if(number>=this._files.length){
      if(this.error)
          this.showClose=true;
        else
          this.onDone.emit(this.resultList);
      return;
    }
    if(!this._files.item(number).type && !this._files.item(number).size){
      setTimeout(()=>{
        this.progress[number].progress.progress=-1;
        this.progress[number].error='FORMAT';
        this.error=true;
        this.upload(number+1);
      },50);
      return;
    }
    this.node.createNode(this.current ? this.current.ref.id : RestConstants.INBOX,RestConstants.CCM_TYPE_IO,[],RestHelper.createNameProperty(this._files.item(number).name),true).subscribe(
      (data : NodeWrapper) => {
        this.node.uploadNodeContent(data.node.ref.id,this._files.item(number),RestConstants.COMMENT_MAIN_FILE_UPLOAD,"auto",(progress:any)=>{
          progress.progress=Math.round(progress.progress*100);
          this.progress[number].progress=progress;
        }).subscribe(
          () => {
            this.resultList.push(data.node);
            this.progress[number].progress.progress=100;
            this.processed++;
            this.upload(number+1);
          },(error)=>{
                this.error=true;
                this.progress[number].error=this.mapError(error,data.node);
                this.progress[number].progress.progress=-1;
                this.upload(number+1);
            });
      }
      ,(error:any) => {
          this.error=true;
          this.progress[number].error=this.mapError(error);
          this.progress[number].progress.progress=-1;
          this.upload(number+1);
      });
  }
  formatTime(time:number){
    return new TimePipe(this.translate).transform(time,null);
  }
  constructor(private node : RestNodeService,
              private translate:TranslateService){}

    private mapError(error: any, node: Node = null) {
        // delete the now orphan node since it's empty
        if(node) {
            this.node.deleteNode(node.ref.id, false).subscribe(() => {});
        }
      if(RestHelper.errorMatchesAny(error,RestConstants.CONTENT_QUOTA_EXCEPTION)) {
          return 'QUOTA';
      }
      if(RestHelper.errorMatchesAny(error,RestConstants.CONTENT_VIRUS_EXCEPTION)){
            return 'VIRUS';
      }
      return 'UNKNOWN';
    }
}
