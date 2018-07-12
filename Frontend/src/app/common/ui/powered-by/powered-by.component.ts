/**
 * Created by Torsten on 13.01.2017.
 */

import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ConfigurationService} from "../../services/configuration.service";
import {ConfigurationHelper} from "../../rest/configuration-helper";

@Component({
  selector: 'powered-by',
  templateUrl: 'powered-by.component.html',
  styleUrls: ['powered-by.component.scss'],
})
export class PoweredByComponent {
  /**
   * The mode do display
   * Either 'white' (like on login') or 'colored'
   * @type {string}
   */
  @Input() mode='white';

  constructor(private config:ConfigurationService) {
    /*
    this.config.getAll().subscribe(()=>{
      this.config = ConfigurationHelper.getBanner(this.config);
      this.onUpdate.emit(this.banner);
    });
    */
  }
  open(){
    window.open("https://www.edu-sharing.com");
  }
}
