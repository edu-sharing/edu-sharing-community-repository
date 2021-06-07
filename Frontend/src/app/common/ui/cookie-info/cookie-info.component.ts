import {Component, Input, Output, EventEmitter, OnInit, SimpleChanges} from '@angular/core';
import {UIAnimation} from '../../../core-module/ui/ui-animation';
import {trigger} from '@angular/animations';
import {CordovaService} from '../../services/cordova.service';
import {
    ConfigurationService,
    DialogButton,
    SessionStorageService
} from '../../../core-module/core.module';

@Component({
  selector: 'cookie-info',
  templateUrl: 'cookie-info.component.html',
  styleUrls: ['cookie-info.component.scss'],
    animations: [
        trigger('fromBottom', UIAnimation.fromBottom(UIAnimation.ANIMATION_TIME_SLOW)),
        trigger('overlay', UIAnimation.openOverlay()),
    ]
})


export class CookieInfoComponent implements OnInit {
  show=false;
  buttons = [
    new DialogButton('COOKIE_INFO.DECLINE',DialogButton.TYPE_CANCEL,()=>
        window.history.back()
    ),
    new DialogButton('COOKIE_INFO.ACCEPT', DialogButton.TYPE_PRIMARY, () =>
          this.accept()
    )];
    details = false;
  constructor(private storage : SessionStorageService,private cordova : CordovaService, private config: ConfigurationService) {
  }
  async ngOnInit() {
      this.show=!this.cordova.isRunningCordova() &&
          !this.storage.getCookie('COOKIE_INFO_ACCEPTED',false) &&
          (await this.config.get('privacy.cookieDisclaimer', false).toPromise());

  }
  accept() {
    this.storage.setCookie('COOKIE_INFO_ACCEPTED',true+'');
    this.show=false;
  }
}
