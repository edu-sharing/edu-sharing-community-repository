/**
 * Created by Torsten on 13.01.2017.
 */

import { Component, HostBinding, Input } from '@angular/core';

@Component({
    selector: 'es-powered-by',
    templateUrl: 'powered-by.component.html',
    styleUrls: ['powered-by.component.scss'],
})
export class PoweredByComponent {
    /**
     * The mode do display
     * Either 'white' or 'color'
     * @type {string}
     */
    @Input() mode = 'white';
    @HostBinding('attr.role') readonly role = 'contentinfo';

    constructor() {
        /*
    this.config.getAll().subscribe(()=>{
      this.config = ConfigurationHelper.getBanner(this.config);
      this.onUpdate.emit(this.banner);
    });
    */
    }
}
