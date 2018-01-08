
import {Component} from "@angular/core";
import {Translation} from "../../common/translation";
import {UIHelper} from "../../common/ui/ui-helper";
import {SessionStorageService} from "../../common/services/session-storage.service";
import {TranslateService} from "@ngx-translate/core";
import {Title} from "@angular/platform-browser";
import {ActivatedRoute} from "@angular/router";
import {Toast} from "../../common/ui/toast";
import {RestConnectorService} from "../../common/rest/services/rest-connector.service";
import {ConfigurationService} from "../../common/services/configuration.service";
import {RestIamService} from "../../common/rest/services/rest-iam.service";
import {IamUser, User} from "../../common/rest/data-object";
import {AuthorityNamePipe} from "../../common/ui/authority-name.pipe";

@Component({
  selector: 'app-profiles',
  templateUrl: 'profiles.component.html',
  styleUrls: ['profiles.component.scss'],
  animations: [

  ]
})
export class ProfilesComponent {
  public user: User;
  public userEdit: User;
  public globalProgress = true;
  public isMe: boolean;
  public edit: boolean;
  public avatarFile: any;

  constructor(private toast: Toast,
              private route: ActivatedRoute,
              private title: Title,
              private translate: TranslateService,
              private config: ConfigurationService,
              private storage : SessionStorageService,
              private iamService: RestIamService) {
      Translation.initialize(translate, this.config, this.storage, this.route).subscribe(() => {
        route.params.subscribe((params)=>{
          this.loadUser(params['authority']);
        });
      });
  }
  public loadUser(authority:string){
    this.globalProgress=true;
    this.iamService.getUser(authority).subscribe((profile:IamUser)=>{
      this.user=profile.person;
      console.log(this.user);
      let name=new AuthorityNamePipe().transform(this.user,null);
      UIHelper.setTitle('PROFILES.TITLE', this.title, this.translate, this.config,{name:name});
      this.globalProgress=false;
      this.iamService.getUser().subscribe((profile:IamUser)=>{
        this.isMe=profile.person.authorityName==this.user.authorityName;
      })
    },(error:any)=>{
      this.globalProgress=false;
      this.toast.error(null,'PROFILES.LOAD_ERROR');
    });
  }
  public updateAvatar(event:any){
    console.log(event);
    if(event.srcElement.files && event.srcElement.files.length){
      this.avatarFile=event.srcElement.files[0];
    }
  }
  public beginEdit(){
    this.userEdit=JSON.parse(JSON.stringify(this.user));
    this.edit=true;
    this.avatarFile=null;
  }
  public clearAvatar(){
    this.avatarFile=null;
    this.userEdit.profile.avatar=null;
  }
  public hasAvatar(){
    return this.userEdit.profile.avatar || this.avatarFile;
  }
  public saveEdits(){
    this.globalProgress=true;
    this.iamService.editUser(this.user.authorityName,this.userEdit.profile).subscribe(()=>{
      if(!this.userEdit.profile.avatar && !this.avatarFile){
        this.iamService.removeUserAvatar(this.user.authorityName).subscribe(()=>{
          this.edit=false;
          this.loadUser(this.user.authorityName);
        });
      }
      else if(this.avatarFile){
        this.iamService.setUserAvatar(this.avatarFile,this.user.authorityName).subscribe(()=>{
          this.edit=false;
          this.loadUser(this.user.authorityName);
        });
      }
      else{
        this.globalProgress=false;
        this.edit=false;
        this.loadUser(this.user.authorityName);
      }

    },(error:any)=>{
      this.globalProgress=false;
      this.toast.error(error);
    });
  }
}

