import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {RestConstants} from "../../../common/rest/rest-constants";
import {Node, NodeList, NodeWrapper} from '../../../common/rest/data-object';
import {RestNodeService} from "../../../common/rest/services/rest-node.service";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../../../common/ui/ui-animation";
import {RestSearchService} from '../../../common/rest/services/rest-search.service';
import {Toast} from '../../../common/ui/toast';

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
  @Input() isFileOver=false;
  @Input() showPicker=false;
  /**
   * Show the lti option and support generation of lti files?
   * @type {boolean}
   */
  @Input() showLti=true;
  private breadcrumbs: Node[];
  private ltiAllowed:boolean;
  private ltiActivated:boolean;
  private ltiConsumerKey:string;
  private ltiSharedSecret:string;
  private ltiTool: Node;
    private _link: string;
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
    if(this.ltiActivated && (!this.ltiConsumerKey || !this.ltiSharedSecret)){
      let params={
        link:{
          caption:'WORKSPACE.TOAST.LTI_FIELDS_REQUIRED_LINK',
          callback:()=>{
            this.ltiActivated=false;
            this.setLink();
          }
        }
      };
      this.toast.error(null,'WORKSPACE.TOAST.LTI_FIELDS_REQUIRED',null,null,null,params);
      return;
    }
    this.onLinkSelected.emit({link:this._link,lti:this.ltiActivated,consumerKey:this.ltiConsumerKey,sharedSecret:this.ltiSharedSecret});
  }
  public get link(){
    return this._link;
  }
  public set link(link:string){
    this._link=link;
    this.setState(link);
  }
  public setState(link: string){
    link=link.trim();
    this.disabled=!link;
    this.ltiAllowed=true;
    if(this.cleanupUrlForLti(link)) {
        this.searchService.search([{
            property: "url",
            values: [this.cleanupUrlForLti(link)]
        }], [], null, RestConstants.CONTENT_TYPE_ALL, RestConstants.HOME_REPOSITORY, RestConstants.DEFAULT, [], 'tool_instances')
            .subscribe((result: NodeList) => {
                // for now, always allow
                this.ltiAllowed = result.nodes.length > 0 || true;
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
    private toast:Toast,
  ){
    this.parent=RestConstants.USERHOME;
    this.setState("");
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
