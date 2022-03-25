import {
  Component, Input, Output, EventEmitter, OnInit, HostListener, ViewChild, ElementRef,
  QueryList
} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {UIAnimation} from "../../../core-module/ui/ui-animation";
import {trigger} from "@angular/animations";
import {Toast} from "../../../core-ui-module/toast";
import {IamUser, RestConstants, RestHelper, RestIamService, User} from "../../../core-module/core.module";

@Component({
  selector: 'user-profile',
  templateUrl: 'user-profile.component.html',
  styleUrls: ['user-profile.component.scss'],
  animations: [
    trigger('overlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST))
  ]
})


export class UserProfileComponent{
  @ViewChild('form') form : ElementRef;
  public user: User;
  public changePassword = false;
  public oldPassword="";
  public password="";
  public passwordRepeat="";
  private static PASSWORD_MIN_LENGTH = 5;
  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
    if(event.code=="Escape"){
      event.preventDefault();
      event.stopPropagation();
      this.cancel();
      return;
    }
  }

  /**
   * Will be emitted when the users cancels the dialog
   * @type {EventEmitter}
   */
  @Output() onCancel = new EventEmitter();

  constructor(private iam : RestIamService,private toast:Toast) {
    this.iam.getCurrentUserAsync().then((data:IamUser)=>{
      this.user=data.person;
    });
  }
  public cancel(){
    this.onCancel.emit();
  }
  public save(){
    this.form.nativeElement.reportValidity();
    if(!this.form.nativeElement.checkValidity())
      return;
    if(this.changePassword){
      if(this.password.length<UserProfileComponent.PASSWORD_MIN_LENGTH){
        this.toast.error(null,'PASSWORD_MIN_LENGTH',{length:UserProfileComponent.PASSWORD_MIN_LENGTH});
        return;
      }
      if(this.password!=this.passwordRepeat){
        this.toast.error(null,'PASSWORD_NOT_MATCH');
        return;
      }
      let credentials={oldPassword:this.oldPassword,newPassword:this.password};
      this.iam.editUserCredentials(RestConstants.ME,credentials).subscribe(()=>{
        this.saveProfile();
      },(error:any)=>{
        if(RestHelper.errorMessageContains(error,"BadCredentialsException")){
          this.toast.error(null,"WRONG_PASSWORD");
        }else {
          this.toast.error(error);
        }
      });
    }
    else{
      this.saveProfile();
    }
  }

  private saveProfile() {
    this.iam.editUser(RestConstants.ME,this.user.profile).subscribe(()=>{
      this.cancel();
      this.toast.toast('PROFILE_UPDATED');
    },(error:any)=>{
      this.toast.error(error);
    });
  }
}
