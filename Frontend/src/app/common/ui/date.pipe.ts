import {PipeTransform, Pipe} from '@angular/core';
import {TranslateService} from "ng2-translate";
import {DateHelper} from "./DateHelper";

@Pipe({name: 'formatDate'})
export class NodeDatePipe implements PipeTransform {
  transform(value : any,args:any): string {
    let time=false;
    if(args && args.time)
      time=args.time;
    return DateHelper.formatDate(this.translate,value,time);
  }
  constructor(private translate : TranslateService){}
}
