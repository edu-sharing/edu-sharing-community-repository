import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  NgZone,
  HostListener,
  ViewChild,
  ElementRef,
  Sanitizer
} from '@angular/core';

@Component({
  selector: 'mds-widget',
  templateUrl: 'mds-widget.component.html',
  styleUrls: ['mds-widget.component.scss'],
})
export class MdsWidgetComponent{
  @Input() widget:any;
  @Input() data:any;

  getBasicType(){
    switch(this.widget.type){
      case 'text':
      case 'number':
      case 'email':
      case 'month':
      case 'color':
      case 'textarea':
        return 'text';
      case 'slider':
        return 'slider';
    }
    return 'unknown';
  }

  getValue() {
    if(this.widget.valuespace){

    }
    return this.data;
  }
}
