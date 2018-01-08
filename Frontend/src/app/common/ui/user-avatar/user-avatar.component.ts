/**
 * Created by Torsten on 13.01.2017.
 */

import {Component, Input} from '@angular/core';
import {ConfigurationService} from "../../services/configuration.service";
import {Person} from "../../rest/data-object";

@Component({
  selector: 'user-avatar',
  templateUrl: 'user-avatar.component.html',
  styleUrls: ['user-avatar.component.scss'],
})
export class UserAvatarComponent {
  @Input() user : Person;
  /**
   * either small, medium or large
   */
  @Input() size = 'large';
  constructor() {
  }
}
