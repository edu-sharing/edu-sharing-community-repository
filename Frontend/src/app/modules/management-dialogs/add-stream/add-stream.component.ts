import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {RestConnectorService} from "../../../common/rest/services/rest-connector.service";
import {Toast} from "../../../common/ui/toast";
import {RestNodeService} from "../../../common/rest/services/rest-node.service";
import {
  NodeWrapper, Node, NodePermissions, LocalPermissionsResult, Permission,
  LoginResult, View, STREAM_STATUS, IamUser, AuthorityProfile
} from "../../../common/rest/data-object";
import {ConfigurationService} from "../../../common/services/configuration.service";
import {UIHelper} from "../../../common/ui/ui-helper";
import {RestIamService} from "../../../common/rest/services/rest-iam.service";
import {TranslateService} from "@ngx-translate/core";
import {MdsComponent} from "../../../common/ui/mds/mds.component";
import {RestConstants} from "../../../common/rest/rest-constants";
import {UIAnimation} from "../../../common/ui/ui-animation";
import {trigger} from "@angular/animations";
import {RestStreamService} from "../../../common/rest/services/rest-stream.service";
import {RestHelper} from "../../../common/rest/rest-helper";
import {Helper} from "../../../common/helper";

@Component({
  selector: 'add-stream',
  templateUrl: 'add-stream.component.html',
  styleUrls: ['add-stream.component.scss'],
  animations: [
    trigger('overlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST)),
    trigger('fade', UIAnimation.fade()),
    trigger('cardAnimation', UIAnimation.cardAnimation())
  ]
})
export class AddStreamComponent  {
  @ViewChild('mds') mdsRef : MdsComponent;
  private streamEntry:any={};
  private reloadMds = new Boolean(true);
  private AUDIENCE_MODE_EVERYONE="0";
  private AUDIENCE_MODE_CUSTOM="1";
  private audienceMode=this.AUDIENCE_MODE_EVERYONE;
  private _nodes: any;
  private invite: AuthorityProfile[]=[];
  @Input() set nodes(nodes : Node[]){
    this._nodes=nodes;
  }
  @Output() onCancel=new EventEmitter();
  @Output() onLoading=new EventEmitter();
  @Output() onDone=new EventEmitter();
  constructor(
    private connector : RestConnectorService,
    private iam : RestIamService,
    private streamApi : RestStreamService,
    private config : ConfigurationService,
    private toast : Toast,
    private nodeApi : RestNodeService) {
    this.connector.isLoggedIn().subscribe((data:LoginResult)=>{

    });
  }
  public cancel(){
    this.onCancel.emit();
  }
  public done(){
    this.onDone.emit();
  }
  public addInvite(event:AuthorityProfile){
    if(Helper.indexOfObjectArray(this.invite,'authorityName',event.authorityName)==-1)
      this.invite.push(event);
  }
  public removeInvite(event:AuthorityProfile){
    this.invite.splice(this.invite.indexOf(event),1);
  }
  public save(){
    let values=this.mdsRef.getValues();
    if(!values) {
      return;
    }
    if(this.audienceMode==this.AUDIENCE_MODE_CUSTOM && this.invite.length==0){
      this.toast.error(null,'ADD_TO_STREAM.ERROR.NO_PERSON_INVITED');
      return;
    }
    this.onLoading.emit(true);
    this.streamEntry.title=values['add_to_stream_title'][0];
    this.streamEntry.priority=values['add_to_stream_priority'][0];
    this.streamEntry.description=values['add_to_stream_description'] ? values['add_to_stream_description'][0] : null;
    this.streamEntry.properties=values;
    this.streamEntry.nodes=RestHelper.getNodeIds(this._nodes);
    console.log(this.streamEntry);
    this.streamApi.addEntry(this.streamEntry).subscribe((data:any)=>{
      let id=data.id;
      console.log(data);
      if(this.audienceMode==this.AUDIENCE_MODE_EVERYONE) {
        this.streamApi.updateStatus(id,RestConstants.AUTHORITY_EVERYONE,STREAM_STATUS.OPEN).subscribe(()=>{
          this.onLoading.emit(false);
          this.onDone.emit();
          this.toast.toast('ADD_TO_STREAM.SUCCESSFUL');
        });
      }
      else{
        this.invitePersons(id);
      }
    },(error:any)=>{
      this.onLoading.emit(false);
      this.toast.error(error);
    });
  }

  private invitePersons(id: string,position = 0) {
    if(position==this.invite.length){
      this.onLoading.emit(false);
      this.onDone.emit();
      return;
    }
    this.streamApi.updateStatus(id,this.invite[position].authorityName,STREAM_STATUS.OPEN).subscribe(()=>{
      this.invitePersons(id,position+1);
    });
  }
}
