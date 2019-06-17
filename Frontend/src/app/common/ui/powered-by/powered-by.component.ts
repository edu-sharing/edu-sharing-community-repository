/**
 * Created by Torsten on 13.01.2017.
 */

import {Component, EventEmitter, Input, Output} from '@angular/core';

@Component({
  selector: 'powered-by',
  templateUrl: 'powered-by.component.html',
  styleUrls: ['powered-by.component.scss'],
})
export class PoweredByComponent {
  /**
   * The mode do display
   * Either 'white' (like on login') or 'color'
   * @type {string}
   */
  @Input() mode='white';

  constructor() {
    /*
    this.config.getAll().subscribe(()=>{
      this.config = ConfigurationHelper.getBanner(this.config);
      this.onUpdate.emit(this.banner);
    });
    */
  }
  open(){
    window.open("https://edu-sharing.com/");
  }
}
