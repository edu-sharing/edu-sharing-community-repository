import {Component, Input, EventEmitter, Output} from '@angular/core';
import {RestNodeService} from "../../../common/rest/services/rest-node.service";
import {Node, NodeList, NodeWrapper} from "../../../common/rest/data-object";
import {RestConstants} from "../../../common/rest/rest-constants";
import {RestHelper} from "../../../common/rest/rest-helper";

@Component({
  selector: 'workspace-file-upload',
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
  @Input() set files(files : FileList){
    console.log(files);
    this._files=files;
    this.progress=[];
    this.error=false;
    this.showClose=false;
    this.resultList=[]
    for(let i=0;i<files.length;i++){
      this.progress.push({name:files.item(i).name,progress:0});
    }
    this.upload(0);
  }
  @Output() onDone=new EventEmitter();
  private close(){
    this.onDone.emit(this.resultList);
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
        this.progress[number].progress=-1;
        this.error=true;
        this.upload(number+1);
      },50);
      return;
    }
    this.node.createNode(this.current.ref.id,RestConstants.CCM_TYPE_IO,[],RestHelper.createNameProperty(this._files.item(number).name),true).subscribe(
      (data : NodeWrapper) => {
        this.resultList.push(data.node);
        this.node.uploadNodeContent(data.node.ref.id,this._files.item(number),RestConstants.COMMENT_MAIN_FILE_UPLOAD).subscribe(
          () => {
            this.progress[number].progress=100;
            this.upload(number+1);
          }
        );
      }
    )
  }
  constructor(private node : RestNodeService){}
}
