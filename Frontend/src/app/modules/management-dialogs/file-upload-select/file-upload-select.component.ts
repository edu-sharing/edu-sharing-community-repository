import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {RestConstants} from "../../../common/rest/rest-constants";
import {Node,NodeList} from "../../../common/rest/data-object";
import {RestNodeService} from "../../../common/rest/services/rest-node.service";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../../../common/ui/ui-animation";

@Component({
  selector: 'workspace-file-upload-select',
  templateUrl: 'file-upload-select.component.html',
  styleUrls: ['file-upload-select.component.scss'],
  animations: [
    trigger('fade', UIAnimation.fade()),
    trigger('cardAnimation', UIAnimation.cardAnimation())
  ]
})
export class WorkspaceFileUploadSelectComponent  {
  public disabled=true;
  public chooseParent=false;
  @ViewChild('fileSelect') file : ElementRef;
  @ViewChild('link') link : ElementRef;
  /**
   * priority, useful if the dialog seems not to be in the foreground
   * Values greater 0 will raise the z-index
   */
  @Input() priority = 0;
  /**
   * Allow multiple files uploaded
   * @type {boolean}
   */
  @Input() multiple = true;
  /**
   * Should this widget display that it supports dropping
   * @type {boolean}
   */
  @Input() supportsDrop = true;
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
