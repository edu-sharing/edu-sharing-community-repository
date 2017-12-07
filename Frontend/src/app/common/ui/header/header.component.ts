/**
 * Created by Torsten on 13.01.2017.
 */

import { Component} from '@angular/core';
import {ConfigurationService} from "../../services/configuration.service";

@Component({
  selector: 'app-header',
  templateUrl: 'header.component.html',
})
export class SearchHeaderComponent {
  private header: string;
  constructor() {
  }
}
