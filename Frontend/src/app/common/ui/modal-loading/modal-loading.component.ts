import {Component, Input, Output, EventEmitter, OnInit, HostListener} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";

@Component({
  selector: 'modal-loading',
  templateUrl: 'modal-loading.component.html',
  styleUrls: ['modal-loading.component.scss'],
})
/**
 * An edu-sharing file-picker modal dialog
 */
export class ModalLoadingComponent{
  /**
   * The title, will be translated automatically
   */
  @Input() title : string;
  /**
   * The message, will be translated automatically
   */
  @Input() message : string;
  /**
   * Additional dynamic content for your language string, use an object, e.g.
   * Language String: Hello {{ name }}
   * And use messageParameters={name:'World'}
   */
  @Input() messageParameters : any;

}
