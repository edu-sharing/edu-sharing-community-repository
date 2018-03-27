import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {RestConstants} from "../../../common/rest/rest-constants";
import {Node, NodeList, NodeWrapper} from '../../../common/rest/data-object';
import {RestNodeService} from "../../../common/rest/services/rest-node.service";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../../../common/ui/ui-animation";
import {RestSearchService} from '../../../common/rest/services/rest-search.service';

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
  @Input() isFileOver=false;
  @Input() showPicker=false;
  private breadcrumbs: Node[];
  private ltiAllowed:boolean;
  private ltiActivated:boolean;
  private ltiTool: Node;
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
  public setState(event: any){
    let link=this.link.nativeElement.value.trim();
    this.disabled=!link;
    this.ltiAllowed=false;
    if(this.cleanupUrlForLti(link)) {
        this.searchService.search([{
            property: "url",
            values: [this.cleanupUrlForLti(link)]
        }], [], null, RestConstants.CONTENT_TYPE_ALL, RestConstants.HOME_REPOSITORY, RestConstants.DEFAULT, [], 'tool_instances')
            .subscribe((result: NodeList) => {
                this.ltiAllowed = result.nodes.length > 0;
                if(result.nodes.length){
                  this.nodeService.getNodeMetadata(result.nodes[0].parent.id,[],result.nodes[0].parent.repo).subscribe((data:NodeWrapper)=>{
                    console.log(data.node);
                    this.ltiTool=data.node;
                  })
                }
            });
    }
  }
  public parentChoosed(event:Node[]){
    this.parent=event[0].ref.id;
    this.parentChange.emit(this.parent);
    this.chooseParent=false;
  }
  public constructor(
    private nodeService:RestNodeService,
    private searchService:RestSearchService,
  ){
    this.parent=RestConstants.USERHOME;
  }

    private cleanupUrlForLti(link: string) {
        let start=link.indexOf("://");
        if(start==-1)
          return null;
        start+=3;
        let end=link.indexOf("/",start);
        if(end==-1)
          return null;
        return link.substr(start,end-start);
    }
}
