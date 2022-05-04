import {Component, Input, Output, EventEmitter, OnInit, ViewChild, ElementRef, HostListener} from '@angular/core';
import {UIAnimation} from "../../../core-module/ui/ui-animation";
import {trigger} from "@angular/animations";
import {UIHelper} from "../../../core-ui-module/ui-helper"
import {Helper} from '../../../core-module/rest/helper';
import {OptionItem} from "../../../core-ui-module/option-item";
import {UIService} from "../../../core-module/core.module";
import { MatMenu, MatMenuTrigger } from '@angular/material/menu';

@Component({
  selector: 'es-mat-link',
  templateUrl: 'link.component.html',
  styleUrls: ['link.component.scss']
})
/**
 * A basic link that should be used whenever a button is not the best solution but rather a link is preferable
 * Will handle keyup.enter automatically for the click binding
 */
export class LinkComponent{
 @Output() click=new EventEmitter();
}
