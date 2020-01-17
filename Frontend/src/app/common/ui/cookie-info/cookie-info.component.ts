import {Component, Input, Output, EventEmitter, OnInit, SimpleChanges} from '@angular/core';
import {UIAnimation} from '../../../core-module/ui/ui-animation';
import {trigger} from '@angular/animations';
import {CordovaService} from "../../services/cordova.service";
import {DialogButton, SessionStorageService} from "../../../core-module/core.module";

@Component({
  selector: 'cookie-info',
  templateUrl: 'cookie-info.component.html',
  styleUrls: ['cookie-info.component.scss'],
    animations: [
        trigger('fromBottom', UIAnimation.fromBottom(UIAnimation.ANIMATION_TIME_SLOW)),
    ]
})


export class CookieInfoComponent{
  show=false;
  dialog=false;
  buttons : DialogButton[];
  constructor(private storage : SessionStorageService,private cordova : CordovaService) {
    this.show=!this.cordova.isRunningCordova() && !this.storage.getCookie("COOKIE_INFO_ACCEPTED",false);
    this.buttons=[new DialogButton('CLOSE',DialogButton.TYPE_PRIMARY,()=>{this.dialog=false;})];
  }
  accept(){
    this.storage.setCookie("COOKIE_INFO_ACCEPTED",true+"");
    this.show=false;
  }
}
