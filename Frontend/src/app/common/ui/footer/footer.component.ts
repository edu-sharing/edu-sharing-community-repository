/**
 * Created by Torsten on 13.01.2017.
 */

import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ConfigurationService} from "../../services/configuration.service";
import {ConfigurationHelper} from "../../rest/configuration-helper";

@Component({
  selector: 'app-footer',
  templateUrl: 'footer.component.html',
  styleUrls: ['footer.component.scss']
})
export class FooterComponent {
    @Input() scope:string;
    public show: boolean;
    constructor(private config:ConfigurationService) {
        this.config.getAll().subscribe(()=>{
            let banner = ConfigurationHelper.getBanner(this.config);
            this.show=banner && banner.components.indexOf(this.scope)!=-1
        });
    }
}
