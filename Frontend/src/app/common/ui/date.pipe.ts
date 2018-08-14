import {PipeTransform, Pipe} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {DateHelper} from "./DateHelper";

@Pipe({name: 'formatDate'})
export class NodeDatePipe implements PipeTransform {
  transform(value : any,args:any): string {
    let time=false;
    if(args && args.time)
      time=args.time;
    let relative=true;
    if(args && args.relative!==null)
        relative=args.relative;
    return DateHelper.formatDate(this.translate,value,time,relative);
  }
  constructor(private translate : TranslateService){}
}
