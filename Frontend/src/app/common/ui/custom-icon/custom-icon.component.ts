/**
 * Created by Torsten on 13.01.2017.
 */

import {Component, Input} from '@angular/core';
import {ConfigurationService} from "../../services/configuration.service";

@Component({
  selector: 'custom-icon',
  templateUrl: 'custom-icon.component.html',
})
export class CustomIconComponent {
  @Input() iconId:string;
  constructor() {
  }
}
