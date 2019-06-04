import {Component, Input, EventEmitter, Output, ViewChild, ElementRef, HostListener} from '@angular/core';
import {DialogButton, RestConnectorService} from "../../../core-module/core.module";
import {Toast} from "../../../common/ui/toast";
import {RestNodeService} from "../../../core-module/core.module";
import {
  NodeWrapper, Node, NodePermissions, LocalPermissionsResult, Permission,
  LoginResult, UserProfile, Comments, Comment, User
} from "../../../core-module/core.module";
import {ConfigurationService} from "../../../core-module/core.module";
import {UIHelper} from "../../../common/ui/ui-helper";
import {RestIamService} from "../../../core-module/core.module";
import {TranslateService} from "@ngx-translate/core";
import {RestCommentsService} from "../../../core-module/core.module";
import {OptionItem} from "../../../common/ui/actionbar/option-item";
import {RestConstants} from "../../../core-module/core.module";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../../../common/ui/ui-animation";

@Component({
  selector: 'node-comments',
  templateUrl: 'node-comments.component.html',
  styleUrls: ['node-comments.component.scss'],
  animations: [
    trigger('fade', UIAnimation.fade()),
    trigger('cardAnimation', UIAnimation.cardAnimation())
  ]
})
export class NodeCommentsComponent  {
  public _node: Node;
  public dialogTitle:string;
  public dialogMessage:string;
  public dialogButtons:DialogButton[];
  private isGuest: boolean;
  private loading: boolean;
  private user: User;
  private comments: Comment[];
  private edit: Comment[];
  private options: OptionItem[][];
  public newComment="";
  public editComment:Comment=null;
  public editCommentText:string;

  @Input() set node(node : Node){
    this._node=node;
    this.refresh();
  }
  @Input() readOnly=false;
  @Output() onCancel=new EventEmitter();
  @Output() onLoading=new EventEmitter();
    /**
     * Some data has changed, may be a new, removed or edited comment
     * @type {EventEmitter<any>}
     */
  @Output() onChange=new EventEmitter();

  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
      if(event.code=="Escape"){
          event.preventDefault();
          event.stopPropagation();
          this.cancel();
          return;
      }
  }
  constructor(
    private connector : RestConnectorService,
    private iam : RestIamService,
    private commentsApi : RestCommentsService,
    private toast : Toast,
    private nodeApi : RestNodeService) {
    this.loading=true;
    this.connector.isLoggedIn().subscribe((data:LoginResult)=>{
      this.loading=false;
      this.isGuest=data.isGuest;
      if(!data.isGuest){
        this.iam.getUser().subscribe((data)=>{
          this.user=data.person;
        });
      }
    });
  }
  private saveEditComment(){
    this.onLoading.emit(true);
    this.commentsApi.editComment(this.editComment.ref.id,this.editCommentText.trim()).subscribe(()=>{
      this.onLoading.emit(false);
      this.onChange.emit();
      this.editComment=null;
      this.refresh();
    },(error:any)=>{
      this.toast.error(error);
      this.onLoading.emit(false);
    })
  }
  private getOptions(comment:Comment){
    let options:OptionItem[]=[];
    let isAuthor=this.user && this.user.authorityName==comment.creator.authorityName;
    if(isAuthor){
      options.push(new OptionItem('NODE_COMMENTS.OPTION_EDIT','edit',()=>{
        this.editComment=comment;
        this.editCommentText=comment.comment;
      }));
    }
    if(isAuthor || this._node.access.indexOf(RestConstants.ACCESS_WRITE)!=-1){
      options.push(new OptionItem('NODE_COMMENTS.OPTION_DELETE','delete',()=>{
        this.dialogTitle='NODE_COMMENTS.DELETE_COMMENT';
        this.dialogMessage='NODE_COMMENTS.DELETE_COMMENT_MESSAGE';
        this.dialogButtons=DialogButton.getYesNo(()=>{
            this.dialogTitle=null;
          },()=>{
            this.onLoading.emit(true);
            this.dialogTitle=null;
            this.commentsApi.deleteComment(comment.ref.id).subscribe(()=>{
              this.refresh();
              this.onChange.emit();
              this.onLoading.emit(false);
            },(error:any)=>{
              this.toast.error(error);
              this.onLoading.emit(false);
            });
          }
        );
      }));
    }
    return options;
  }
  public canComment(){
    if(this.isGuest || !this.user || this.readOnly)
      return false;
    return this._node.access.indexOf(RestConstants.ACCESS_COMMENT)!=-1;
  }
  public addComment(){
    if(!this.newComment.trim()){
      this.toast.error(null,'NODE_COMMENTS.COMMENT_EMTPY');
      return;
    }
    this.onLoading.emit(true);
    this.commentsApi.addComment(this._node.ref.id,this.newComment.trim()).subscribe(()=>{
      this.onLoading.emit(false);
      this.onChange.emit();
      this.newComment="";
      this.refresh();
    },(error:any)=>{
      this.toast.error(error);
      this.onLoading.emit(false);
    })
  }
  public cancel(){
    this.onCancel.emit();
  }

  private refresh() {
    this.comments=null;
    if(!this._node)
      return;
    if(this.loading){
      setTimeout(()=>this.refresh(),100);
      return;
    }
    this.commentsApi.getComments(this._node.ref.id,this._node.ref.repo).subscribe((data:Comments)=>{
      this.comments=data.comments;
      this.options=[];
      for(let comment of this.comments){
        this.options.push(this.getOptions(comment));
      }
    },(error:any)=>{
      this.toast.error(error);
    });
  }
}
