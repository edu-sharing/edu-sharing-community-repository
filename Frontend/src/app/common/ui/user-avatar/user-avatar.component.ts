/**
 * Created by Torsten on 13.01.2017.
 */

import {Component, Input} from '@angular/core';
import {ConfigurationService} from "../../services/configuration.service";
import {Person, UserProfile} from "../../rest/data-object";

@Component({
  selector: 'user-avatar',
  templateUrl: 'user-avatar.component.html',
  styleUrls: ['user-avatar.component.scss'],
})
export class UserAvatarComponent {
  @Input() profile : UserProfile;
  /**
   * either xsmall, small, medium or large
   */
  @Input() size = 'large';

  public _customImage:any;
  @Input() set customImage(customImage:File){
    if(customImage==null){
      this._customImage=null;
      return;
    }
    var reader = new FileReader();
    reader.onload = (e:any) => {
      this._customImage=e.target.result;
    }
    reader.readAsDataURL(customImage);
  };
  constructor() {
  }
}
