import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {ConfigurationService, DialogButton, RestConstants} from '../../../core-module/core.module';
import {IamUser, Node, NodeList, NodeWrapper} from '../../../core-module/core.module';
import {RestNodeService} from '../../../core-module/core.module';
import {trigger} from '@angular/animations';
import {UIAnimation} from '../../../core-module/ui/ui-animation';
import {RestSearchService} from '../../../core-module/core.module';
import {Toast} from '../../../core-ui-module/toast';
import {RestIamService} from '../../../core-module/core.module';
import {LinkData} from '../../../core-ui-module/node-helper.service';

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
  public disabled= true;
  public chooseParent= false;
  @ViewChild('fileSelect') file: ElementRef;
  @ViewChild('link') linkRef: ElementRef;
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
  @Input() isFileOver= false;
    /**
     * Allow the user to use a file picker to choose the parent?
     */
    @Input() showPicker= false;
  /**
   * Show the lti option and support generation of lti files?
   * @type {boolean}
   */
  @Input() showLti=true;
  breadcrumbs: Node[];
  ltiAllowed: boolean;
  ltiActivated: boolean;
  ltiConsumerKey: string;
  ltiSharedSecret: string;
  private ltiTool: Node;
  private _link: string;
  _parent: Node;
  buttons: DialogButton[];
  user: IamUser;
  @Input() set parent(parent: Node){
    this.breadcrumbs = null;
    this._parent = parent;
    if (parent) {
        this.nodeService.getNodeParents(parent.ref.id).subscribe((data: NodeList) => {
            this.breadcrumbs = data.nodes.reverse();
        });
    }
  }
  @Output() parentChange = new EventEmitter();
  @Output() onCancel= new EventEmitter();
  @Output() onFileSelected= new EventEmitter<FileList>();
  @Output() onLinkSelected= new EventEmitter<LinkData>();

  public cancel(){
    this.onCancel.emit();
  }
  public selectFile(){
    this.file.nativeElement.click();
  }
  public onDrop(fileList: FileList){
      this.onFileSelected.emit(fileList);
  }
  public filesSelected(event: any): void {
    this.onFileSelected.emit(event.target.files as FileList);
  }
  public setLink(){
    if (this.ltiActivated && (!this.ltiConsumerKey || !this.ltiSharedSecret)){
      const params = {
        link: {
          caption: 'WORKSPACE.TOAST.LTI_FIELDS_REQUIRED_LINK',
          callback: () => {
            this.ltiActivated = false;
            this.setLink();
          }
        }
      };
      this.toast.error(null, 'WORKSPACE.TOAST.LTI_FIELDS_REQUIRED', null, null, null, params);
      return;
    }
    this.onLinkSelected.emit({link: this._link, lti: this.ltiActivated, consumerKey: this.ltiConsumerKey, sharedSecret: this.ltiSharedSecret});
  }
  public get link(){
    return this._link;
  }
  public set link(link: string){
    this._link = link;
    this.setState(link);
  }
  public setState(link: string){
    link = link.trim();
    this.disabled = !link;
    this.ltiAllowed = true;
    this.updateButtons();
    /*
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
                    this.ltiTool=data.node;
                  })
                }
            });
    }
    */
  }
  public parentChoosed(event: Node[]) {
    this._parent = event[0];
    this.parentChange.emit(this._parent);
    this.chooseParent = false;
  }
  public constructor(
    private nodeService: RestNodeService,
    private iamService: RestIamService,
    private searchService: RestSearchService,
    public configService: ConfigurationService,
    private toast: Toast,
  ){
    this.setState('');
    this.iamService.getCurrentUserAsync().then((user) => {
      this.user = user;
    });
  }
  updateButtons() {
    const ok = new DialogButton('OK', DialogButton.TYPE_PRIMARY, () => this.setLink());
    ok.disabled = this.disabled || (this.showPicker && !this._parent);
    this.buttons = [
        new DialogButton('CANCEL', DialogButton.TYPE_CANCEL, () => this.cancel()),
        ok
    ];
  }
    private cleanupUrlForLti(link: string) {
        let start = link.indexOf('://');
        if (start == -1)
          return null;
        start += 3;
        const end = link.indexOf('/', start);
        if (end == -1)
          return null;
        return link.substr(start, end - start);
    }
}
