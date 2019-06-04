import {Component, Input, Output, EventEmitter, OnInit, HostListener} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {Toast} from "../toast";
import {UIHelper} from '../ui-helper';
import {UserQuota} from '../../../core-module/core.module';

@Component({
  selector: 'user-quota',
  templateUrl: 'user-quota.component.html',
  styleUrls: ['user-quota.component.scss'],
})
/**
 * A quota info component
 */
export class UserQuotaComponent{
  @Input() quota : UserQuota;

    /**
     * returns the "health" of the available space
     * 0 = good
     * 1 = medium
     * 2 = bad
     */
  getHealth(){
    const fac=this.getPercentUsed();
    if(fac<0.75)
      return 0;
    if(fac<0.9)
      return 1;
    return 2;
  }

  getFree() {
    return Math.max((this.quota.sizeQuota-this.quota.sizeCurrent),0)
  }
  getPercentUsed() {
      return Math.min(this.quota.sizeCurrent / this.quota.sizeQuota,1);
  }
}
