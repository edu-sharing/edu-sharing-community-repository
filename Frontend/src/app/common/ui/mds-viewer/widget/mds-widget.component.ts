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
  _data:string[];
  value:any;
  @Input() widget:any;
  @Input() set data(data:string[]){
    this._data=data; 
    this.value=this.getValue();
  }

  getBasicType() {
    switch(this.widget.type) {
      case 'text':
      case 'number':
      case 'email':
      case 'month':
      case 'color':
      case 'textarea':
      case 'singleoption':
        return 'text';
      case 'multivalueFixedBadges':
      case 'multivalueSuggestBadges':
      case 'multivalueBadges':
      case 'multivalueTree':
        return 'array';
      case 'slider':
        return 'slider';
    }
    return 'unknown';
  }

  getValue() {
    if(!this._data || !this._data[0]) {
      return null;
    }
    if(this.widget.values) {
      console.log(this._data, this.widget.values);
        const mapping=this.widget.values.filter((v:any) => this._data.filter((d) => d === v.id).length > 0).map((v:any) => v.caption);
        if(mapping){
          return mapping;
        }
    }
    return this._data;
  }

    click() {
        if(this.widget.link === '_BLANK') {
          window.open(this.value);
        } else {
          console.warn('Unsupported link type ' + this.widget.link);
        }
    }
}
