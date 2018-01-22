import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {RestConnectorService} from "../../../common/rest/services/rest-connector.service";
import {Toast} from "../../../common/ui/toast";
import {RestNodeService} from "../../../common/rest/services/rest-node.service";
import {
  NodeWrapper, Node, NodePermissions, LocalPermissionsResult, Permission,
  LoginResult, UserProfile, Comments, Comment
} from "../../../common/rest/data-object";
import {ConfigurationService} from "../../../common/services/configuration.service";
import {UIHelper} from "../../../common/ui/ui-helper";
import {RestIamService} from "../../../common/rest/services/rest-iam.service";
import {TranslateService} from "@ngx-translate/core";
import {RestCommentsService} from "../../../common/rest/services/rest-comments.service";

@Component({
  selector: 'node-comments',
  templateUrl: 'node-comments.component.html',
  styleUrls: ['node-comments.component.scss']
})
export class NodeCommentsComponent  {
  public _node: Node;
  private isGuest: boolean;
  private profile: UserProfile;
  private comments: Comment[];

  @Input() set node(node : Node){
    this._node=node;
    this.commentsApi.getComments(node.ref.id).subscribe((data:Comments)=>{
      this.comments=data.comments;
    });
  }
  @Output() onCancel=new EventEmitter();
  @Output() onLoading=new EventEmitter();
  constructor(
    private connector : RestConnectorService,
    private iam : RestIamService,
    private commentsApi : RestCommentsService,
    private nodeApi : RestNodeService) {
    this.connector.isLoggedIn().subscribe((data:LoginResult)=>{
      this.isGuest=data.isGuest;
      if(!data.isGuest){
        this.iam.getUser().subscribe((data)=>{
          this.profile=data.person.profile;
        });
      }
    });
  }
  public cancel(){
    this.onCancel.emit();
  }
}
