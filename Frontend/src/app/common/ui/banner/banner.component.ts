/**
 * Created by Torsten on 13.01.2017.
 */

import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ConfigurationService} from "../../services/configuration.service";
import {ConfigurationHelper} from "../../rest/configuration-helper";

@Component({
  selector: 'app-banner',
  templateUrl: 'banner.component.html',
  styleUrls: ['banner.component.scss'],
})
export class BannerComponent {
  @Input() scope:string;
  @Output() onUpdate = new EventEmitter();
  public banner: any;
  constructor(private config:ConfigurationService) {
    this.banner = ConfigurationHelper.getBanner(this.config);
    this.config.getAll().subscribe(()=>{
      this.banner = ConfigurationHelper.getBanner(this.config);
      this.onUpdate.emit(this.banner);
    });
  }
}
