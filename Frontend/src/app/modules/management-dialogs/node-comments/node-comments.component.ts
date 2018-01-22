import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {RestConnectorService} from "../../../common/rest/services/rest-connector.service";
import {Toast} from "../../../common/ui/toast";
import {RestNodeService} from "../../../common/rest/services/rest-node.service";
import {
  NodeWrapper, Node, NodePermissions, LocalPermissionsResult, Permission,
  LoginResult, UserProfile, Comments, Comment, User
} from "../../../common/rest/data-object";
import {ConfigurationService} from "../../../common/services/configuration.service";
import {UIHelper} from "../../../common/ui/ui-helper";
import {RestIamService} from "../../../common/rest/services/rest-iam.service";
import {TranslateService} from "@ngx-translate/core";
import {RestCommentsService} from "../../../common/rest/services/rest-comments.service";
import {OptionItem} from "../../../common/ui/actionbar/option-item";
import {RestConstants} from "../../../common/rest/rest-constants";
import {DialogButton} from "../../../common/ui/modal-dialog/modal-dialog.component";

@Component({
  selector: 'node-comments',
  templateUrl: 'node-comments.component.html',
  styleUrls: ['node-comments.component.scss']
})
export class NodeCommentsComponent  {
  public _node: Node;
  private dialogTitle:string;
  private dialogMessage:string;
  private dialogButtons:DialogButton[];
  private isGuest: boolean;
  private user: User;
  private comments: Comment[];
  private options: OptionItem[][];
  public newComment="";

  @Input() set node(node : Node){
    this._node=node;
    this.refresh();
  }
  @Output() onCancel=new EventEmitter();
  @Output() onLoading=new EventEmitter();
  constructor(
    private connector : RestConnectorService,
    private iam : RestIamService,
    private commentsApi : RestCommentsService,
    private toast : Toast,
    private nodeApi : RestNodeService) {
    this.connector.isLoggedIn().subscribe((data:LoginResult)=>{
      this.isGuest=data.isGuest;
      if(!data.isGuest){
        this.iam.getUser().subscribe((data)=>{
          this.user=data.person;
        });
      }
    });
  }
  private getOptions(comment:Comment){
    let options:OptionItem[]=[];
    let isAuthor=this.user.authorityName==comment.creator.authorityName;
    if(isAuthor){
      options.push(new OptionItem('NODE_COMMENTS.OPTION_EDIT','edit',()=>{

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
  public addComment(){
    if(!this.newComment.trim()){
      this.toast.error(null,'NODE_COMMENTS.COMMENT_EMTPY');
      return;
    }
    this.onLoading.emit(true);
    this.commentsApi.addComment(this._node.ref.id,this.newComment).subscribe(()=>{
      this.onLoading.emit(false);
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
    this.commentsApi.getComments(this._node.ref.id).subscribe((data:Comments)=>{
      this.comments=data.comments;
      this.options=[];
      for(let comment of this.comments){
        this.options.push(this.getOptions(comment));
      }
    });
  }
}
