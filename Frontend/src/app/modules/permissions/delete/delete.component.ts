import {Component} from "@angular/core";
import {DeleteMode, DialogButton, Group, ListItem, Person, RestAdminService, RestConstants, RestIamService, SessionStorageService, User} from "../../../core-module/core.module";
import {Toast} from "../../../core-ui-module/toast";
import {TranslateService} from "@ngx-translate/core";
import {AuthorityNamePipe} from "../../../core-ui-module/pipes/authority-name.pipe";

@Component({
  selector: 'permissions-delete',
  templateUrl: 'delete.component.html',
  styleUrls: ['delete.component.scss'],

})
export class PermissionsDeleteComponent {
 deleteModes=[DeleteMode.none,DeleteMode.assign,DeleteMode.delete];
 options: any;
 receiver: User;
 receiverGroup: Group;
 users: User[];
 selectedUsers: User[]=[];
 loading = false;
 columns : ListItem[]=[];

 constructor(
     private iam : RestIamService,
     private admin : RestAdminService,
     private toast : Toast,
     private storage : SessionStorageService,
     private translate : TranslateService
 ){
     // send list of target users + options for these specific users
    this.options={
      homeFolder:{
        privateFiles:DeleteMode.none,
        ccFiles:DeleteMode.none,
        keepFolderStructure:false,
      },
      sharedFolders:{
        privateFiles:DeleteMode.none,
        ccFiles:DeleteMode.none,
      },
      collections:{
        privateCollections:DeleteMode.none,
        publicCollections:DeleteMode.none,
      },
      ratings: {
          delete: false
      },
      comments: {
          delete: true
      },
      statistics: {
          delete: false
      },
      stream: {
          delete: false
      },
          // change owner + (optional) invite a coordinator group
          // comments, ratings, feedback, stream, statistics
      receiver:'',
      receiverGroup:''
    };
     this.storage.get('delete_users_options', this.options).subscribe((data: any) => {
         this.options = data;
     });
     this.columns.push(new ListItem('USER', RestConstants.AUTHORITY_NAME));
     this.columns.push(new ListItem('USER', RestConstants.AUTHORITY_FIRSTNAME));
     this.columns.push(new ListItem('USER', RestConstants.AUTHORITY_LASTNAME));

     this.refresh();
 }

  /**
   * returns a code whether all selected modes seem to be data conform and all user-relevant data will be removed and all options match up
   */
  isValid() {
    return !this.anyModeMatches(DeleteMode.none);
  }

  hasAssigning() {
    return this.anyModeMatches(DeleteMode.assign)
  }

  private anyModeMatches(mode : DeleteMode) {
    return this.options.homeFolder.privateFiles==mode
        || this.options.homeFolder.ccFiles==mode
        || this.options.sharedFolders.privateFiles==mode
        || this.options.sharedFolders.ccFiles==mode
        || this.options.collections.privateCollections==mode
        || this.options.collections.publicCollections==mode;
  }

    private refresh() {
        this.selectedUsers=[];
        this.loading=true;
        let request={maxItems:RestConstants.COUNT_UNLIMITED};
        this.iam.searchUsers("*",true,'todelete',request).subscribe((users)=>{
            this.users=users.users;
            this.loading=false;
        },(error)=>{
            this.toast.error(error);
            this.loading=false;
        });
    }

    prepareStart() {
      let message=this.translate.instant('PERMISSIONS.DELETE.CONFIRM.USERS');
      for(let user of this.selectedUsers){
          message+="\n"+new AuthorityNamePipe(this.translate).transform(user,null);
      }
      if(this.hasAssigning()) {
          message += "\n\n" + this.translate.instant('PERMISSIONS.DELETE.CONFIRM.RECEIVER', {user: new AuthorityNamePipe(this.translate).transform(this.receiver, null)});
          message += "\n\n" + this.translate.instant('PERMISSIONS.DELETE.CONFIRM.RECEIVER_GROUP', {group: new AuthorityNamePipe(this.translate).transform(this.receiverGroup, null)});
      }
      message+="\n\n"+this.translate.instant('PERMISSIONS.DELETE.CONFIRM.FINAL');
      this.toast.showModalDialog('PERMISSIONS.DELETE.CONFIRM.CAPTION',message,[
          new DialogButton('CANCEL',DialogButton.TYPE_CANCEL,()=>this.toast.closeModalDialog()),
          new DialogButton('PERMISSIONS.DELETE.START',DialogButton.TYPE_PRIMARY,()=>this.start())
      ]);
    }

    start() {
      if(this.hasAssigning()) {
          this.options.receiver = this.receiver.authorityName;
          this.options.receiverGroup = this.receiverGroup.authorityName;
      }
      this.toast.showProgressDialog();
      this.storage.set('delete_users_options',this.options);
      this.admin.deletePersons(this.selectedUsers.map((u)=>u.authorityName),this.options).subscribe(()=>{
          this.toast.closeModalDialog();
      },(error)=>{
          this.toast.error(error);
          this.toast.closeModalDialog();
      });
    }

    missingAssigning() {
        return this.hasAssigning() && (this.receiver==null || this.receiverGroup==null)
    }

    canSubmit() {
        return this.selectedUsers.length && !this.missingAssigning();
    }
}
