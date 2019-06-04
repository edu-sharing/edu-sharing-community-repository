import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {RestConstants} from "../../../core-module/core.module";
import {Node, NodeWrapper} from "../../../core-module/core.module";
import {RestNodeService} from "../../../core-module/core.module";
import {Toast} from "../../../common/ui/toast";

@Component({
  selector: 'workspace-editor',
  templateUrl: 'editor.component.html',
  styleUrls: ['editor.component.scss']
})
export class WorkspaceEditorComponent  {
  @ViewChild('file') fileDialog : ElementRef;
  @ViewChild('title') title : ElementRef;

  public _node : Node;
  private asVersion = false;
  private versionComment="";
  private keyword : string;
  private selectedFile : File;
  private disabled=false;
  @Input() allowReplacing=true;
  @Input() set node(node : Node){
    this.nodeApi.getNodeMetadata(node.ref.id,[RestConstants.ALL]).subscribe((data : NodeWrapper)=>{
      let node=data.node;
      if(!node.properties[RestConstants.LOM_PROP_GENERAL_KEYWORD])
        node.properties[RestConstants.LOM_PROP_GENERAL_KEYWORD]=[];

      if(!node.properties[RestConstants.LOM_PROP_GENERAL_DESCRIPTION])
        node.properties[RestConstants.LOM_PROP_GENERAL_DESCRIPTION]=[""];
      if(node.properties[RestConstants.LOM_PROP_GENERAL_KEYWORD]
        && node.properties[RestConstants.LOM_PROP_GENERAL_KEYWORD].length==1
        && node.properties[RestConstants.LOM_PROP_GENERAL_KEYWORD][0]==""
      )
        node.properties[RestConstants.LOM_PROP_GENERAL_KEYWORD]=[];

      this._node=node;
      setTimeout(()=>this.checkPreview(),2000);
    });
  }

  private checkPreview() {
    console.log(this._node.preview);
    if(!this._node.preview.isIcon)
      return;
    this.nodeApi.getNodeMetadata(this._node.ref.id).subscribe((data : NodeWrapper)=>{
      this._node.preview=data.node.preview;
      this._node.preview.url+="&cache="+new Date().getTime();
      setTimeout(()=>this.checkPreview(),2000);
    });

    }
  @Output() onCancel = new EventEmitter();
  @Output() onDone = new EventEmitter();
  @Output() onWorking = new EventEmitter();
  private cancel(){
    this.onCancel.emit();
  }
  private updateState(state : any = null){
    let version=this.asVersion;
    if(state)
      version=state.srcElement.checked;
    if(this.selectedFile)
      version=true;
    this.disabled=version && !this.versionComment.trim();
    this.disabled=this.disabled || !this.title.nativeElement.value;
  }
  public fileSelected(event : any){
    this.selectedFile=event.target.files.item(0);
    this.updateState();
  }
  private addKeyword(){
    if(!this.keyword.trim())
      return;
    if(this._node.properties[RestConstants.LOM_PROP_GENERAL_KEYWORD].indexOf(this.keyword)==-1)
      this._node.properties[RestConstants.LOM_PROP_GENERAL_KEYWORD].push(this.keyword);
    this.keyword="";
  }
  private removeKeyword(keyword : string){
    let pos=this._node.properties[RestConstants.LOM_PROP_GENERAL_KEYWORD].indexOf(keyword);
    this._node.properties[RestConstants.LOM_PROP_GENERAL_KEYWORD].splice(pos,1);
  }
  private save(){
    this.onWorking.emit(true);
    if(this.selectedFile){
      this.nodeApi.uploadNodeContent(this._node.ref.id,this.selectedFile,this.versionComment).subscribe(()=>{
        this.saveMetadata(false);
        },
        (error:any)=>this.toast.error(error)
      )
    }
    else{
      this.saveMetadata(this.asVersion);
    }
  }
  constructor(private nodeApi : RestNodeService,private toast : Toast){}

  private saveMetadata(version: boolean) {
    if(version){
      this.nodeApi.editNodeMetadataNewVersion(this._node.ref.id,this.versionComment,this._node.properties).subscribe(()=>{
          this.onDone.emit();
          this.toast.toast("WORKSPACE.EDITOR.UPDATED");
          this.onWorking.emit(false);
        },
        (error:any)=> {
          this.toast.error(error);
          this.onWorking.emit(false);
        }
          );
    }
    else{
      this.nodeApi.editNodeMetadata(this._node.ref.id,this._node.properties).subscribe(()=>{
          this.onDone.emit();
          this.toast.toast("WORKSPACE.EDITOR.UPDATED");
          this.onWorking.emit(false);

        },
        (error:any)=> {
          this.toast.error(error);
          this.onWorking.emit(false);
        }
    );
    }

  }
}
