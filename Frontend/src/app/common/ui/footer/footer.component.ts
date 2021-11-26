/**
 * Created by Torsten on 13.01.2017.
 */

import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ConfigurationHelper, ConfigurationService} from '../../../core-module/core.module';

@Component({
  selector: 'es-footer',
  templateUrl: 'footer.component.html',
  styleUrls: ['footer.component.scss']
})
export class FooterComponent {
    banner: any;
    _scope:string;
    public show: boolean;
    @Input() set scope(scope:string){
        this._scope=scope;
        this.config.getAll().subscribe(()=>{
            this.banner = ConfigurationHelper.getBanner(this.config);
            this.show=this.banner && this.banner.components.indexOf(this._scope)!=-1
        });
    }
    constructor(private config:ConfigurationService) {
    }
}
