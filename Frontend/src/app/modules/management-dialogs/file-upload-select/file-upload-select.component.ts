import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {RestConstants} from "../../../common/rest/rest-constants";
import {Node,NodeList} from "../../../common/rest/data-object";
import {RestNodeService} from "../../../common/rest/services/rest-node.service";

@Component({
  selector: 'workspace-file-upload-select',
  templateUrl: 'file-upload-select.component.html',
  styleUrls: ['file-upload-select.component.scss']
})
export class WorkspaceFileUploadSelectComponent  {
  public disabled=true;
  public chooseParent=false;
  @ViewChild('fileSelect') file : ElementRef;
  @ViewChild('link') link : ElementRef;
  @Input() isFileOver=false;
  @Input() showPicker=false;
  private breadcrumbs: Node[];
  @Input() set parent(parent:string){
    if(parent==RestConstants.USERHOME){
      this.breadcrumbs=[];
      return;
    }
    this.nodeService.getNodeParents(parent).subscribe((data:NodeList)=>{
      this.breadcrumbs=data.nodes;
    })
  }
  @Output() parentChange = new EventEmitter();
  @Output() onCancel=new EventEmitter();
  @Output() onFileSelected=new EventEmitter();
  @Output() onLinkSelected=new EventEmitter();

  public cancel(){
    this.onCancel.emit();
  }
  public selectFile(){
    this.file.nativeElement.click();
  }
  public filesSelected(event:any) : void {
    this.onFileSelected.emit(event.target.files);
  }
  public setLink(){
    this.onLinkSelected.emit(this.link.nativeElement.value);
  }
  public setState(event : any){
    this.disabled=!this.link.nativeElement.value.trim();
  }
  public parentChoosed(event:Node[]){
    this.parent=event[0].ref.id;
    this.parentChange.emit(this.parent);
    this.chooseParent=false;
  }
  public constructor(
    private nodeService:RestNodeService,
  ){
    this.parent=RestConstants.USERHOME;
  }
}
