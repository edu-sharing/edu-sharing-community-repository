
import {Component, ElementRef, ViewChild} from '@angular/core';
import {Translation} from "../../common/translation";
import {UIHelper} from "../../common/ui/ui-helper";
import {SessionStorageService} from "../../common/services/session-storage.service";
import {TranslateService} from "@ngx-translate/core";
import {DomSanitizer, Title} from "@angular/platform-browser";
import {ActivatedRoute, Router} from '@angular/router';
import {Toast} from "../../common/ui/toast";
import {RestConnectorService} from "../../common/rest/services/rest-connector.service";
import {ConfigurationService} from "../../common/services/configuration.service";
import {RestIamService} from "../../common/rest/services/rest-iam.service";
import {IamUser, User} from "../../common/rest/data-object";
import {AuthorityNamePipe} from "../../common/ui/authority-name.pipe";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../../common/ui/ui-animation";
import {UserProfileComponent} from "../../common/ui/user-profile/user-profile.component";
import {RestConstants} from "../../common/rest/rest-constants";
import {RestHelper} from "../../common/rest/rest-helper";
import {MainNavComponent} from '../../common/ui/main-nav/main-nav.component';
import {Helper} from "../../common/helper";

@Component({
  selector: 'app-profiles',
  templateUrl: 'profiles.component.html',
  styleUrls: ['profiles.component.scss'],
  animations: [
    trigger('overlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST))
  ]
})
export class ProfilesComponent {
  public user: User;
  public userEdit: User;
  public globalProgress = true;
  public isMe: boolean;
  public edit: boolean;
  public avatarFile: any;
  public changePassword: boolean;
  public editAbout = false;
  public oldPassword="";
  public password="";
  private static PASSWORD_MIN_LENGTH = 5;
  private editProfile: boolean;
  private editProfileUrl: string;
  private avatarImage: any;
  @ViewChild('mainNav') mainNavRef: MainNavComponent;
  @ViewChild('avatar') avatarElement : ElementRef;
  constructor(private toast: Toast,
              private route: ActivatedRoute,
              private title: Title,
              private connector: RestConnectorService,
              private translate: TranslateService,
              private router: Router,
              private config: ConfigurationService,
              private sanitizer: DomSanitizer,
              private storage : SessionStorageService,
              private iamService: RestIamService) {
      Translation.initialize(translate, this.config, this.storage, this.route).subscribe(() => {
        route.params.subscribe((params)=>{
          this.editProfileUrl=this.config.instant("editProfileUrl");
          this.editProfile=this.config.instant("editProfile",true);
          this.loadUser(params['authority']);
        });
      });
  }
  public loadUser(authority:string){
    this.globalProgress=true;
    this.connector.isLoggedIn().subscribe((login)=> {
        this.iamService.getUser(authority).subscribe((profile: IamUser) => {
            this.user = profile.person;
            let name = new AuthorityNamePipe(this.translate).transform(this.user, null);
            UIHelper.setTitle('PROFILES.TITLE', this.title, this.translate, this.config, {name: name});
            this.globalProgress = false;
            this.mainNavRef.finishPreloading();
            this.iamService.getUser().subscribe((me)=>{
                this.isMe = profile.person.authorityName == me.person.authorityName;
                if(this.isMe && login.isGuest){
                    RestHelper.goToLogin(this.router,this.config);
                }
            });
        }, (error: any) => {
            this.globalProgress = false;
            this.mainNavRef.finishPreloading();
            this.toast.error(null, 'PROFILES.LOAD_ERROR');
        });
    });
  }
  public updateAvatar(event:any){
    if(this.avatarElement.nativeElement.files && this.avatarElement.nativeElement.files.length){
      this.avatarFile=this.avatarElement.nativeElement.files[0];
      this.avatarImage=this.sanitizer.bypassSecurityTrustUrl(URL.createObjectURL(this.avatarFile));
    }
  }
  public beginEdit(){
    if(this.editProfileUrl){
      window.location.href=this.editProfileUrl;
      return;
    }
    this.userEdit=Helper.deepCopy(this.user);
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
  public savePassword(){
    if(this.changePassword){
      if(this.password.length<ProfilesComponent.PASSWORD_MIN_LENGTH){
        this.toast.error(null,'PASSWORD_MIN_LENGTH',{length:ProfilesComponent.PASSWORD_MIN_LENGTH});
        this.globalProgress=false;
        return;
      }
      let credentials={oldPassword:this.oldPassword,newPassword:this.password};
      this.iamService.editUserCredentials(this.user.authorityName,credentials).subscribe(()=>{
        this.saveAvatar();
      },(error:any)=>{
        if(RestHelper.errorMessageContains(error,"BadCredentialsException")){
          this.toast.error(null,"WRONG_PASSWORD");
          this.globalProgress=false;
        }
        else {
          this.toast.error(error);
          this.saveAvatar();
        }
      });
    }
    else{
      this.saveAvatar();
    }
  }
  public saveEdits(){
    if(!this.userEdit.profile.firstName.trim()){
      this.toast.error(null,'PROFILES.ERROR.FIRST_NAME');
      return;
    }
    if(!this.userEdit.profile.lastName.trim()){
      this.toast.error(null,'PROFILES.ERROR.LAST_NAME');
      return;
    }
    if(!this.userEdit.profile.email.trim()){
      this.toast.error(null,'PROFILES.ERROR.EMAIL');
      return;
    }
    this.globalProgress=true;
    this.iamService.editUser(this.user.authorityName,this.userEdit.profile).subscribe(()=>{
      this.savePassword();
    },(error:any)=>{
      this.globalProgress=false;
      this.toast.error(error);
    });
  }

  private saveAvatar() {
    this.user=null;
    if(!this.userEdit.profile.avatar && !this.avatarFile){
      this.iamService.removeUserAvatar(this.userEdit.authorityName).subscribe(()=>{
        this.edit=false;
        this.loadUser(this.userEdit.authorityName);
      });
    }
    else if(this.avatarFile){
      this.iamService.setUserAvatar(this.avatarFile,this.userEdit.authorityName).subscribe(()=>{
        this.edit=false;
        this.loadUser(this.userEdit.authorityName);
      });
    }
    else{
      this.globalProgress=false;
      this.edit=false;
      this.loadUser(this.userEdit.authorityName);
    }
  }

  public aboutEdit() {
    this.userEdit=Helper.deepCopy(this.user);
    this.editAbout = true;
  }

  public editPassword() {
    this.changePassword =! this.changePassword;
    this.password = "";
    this.oldPassword = "";
  }
}

