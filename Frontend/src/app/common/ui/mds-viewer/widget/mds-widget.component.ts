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
  _data:any;
  value:any;
  @Input() widget:any;
  @Input() set data(data:any){
    this._data=data; 
    this.value=this.getValue();
  }

  getBasicType(){
    switch(this.widget.type){
      case 'text':
      case 'number':
      case 'email':
      case 'month':
      case 'color':
      case 'textarea':
      case 'singleoption':
        return 'text';
      case 'slider':
        return 'slider';
    }
    return 'unknown';
  }

  getValue() {
    if(!this._data){
      return '-';
    }
    if(this.widget.values){
        const mapping=this.widget.values.filter((v:any) => v.id.indexOf(this._data)!=-1).map((v:any) => v.caption);
        if(mapping){
          return mapping;
        }
    }
    return this._data;
  }
}
