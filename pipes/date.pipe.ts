import {PipeTransform, Pipe} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {DateHelper, FormatOptions} from '../DateHelper';

@Pipe({name: 'formatDate'})
export class NodeDatePipe implements PipeTransform {
  transform(value : any,args:any): string {
    let options=new FormatOptions();
    if(args && args.time!=null)
        options.showAlwaysTime=args.time;
      if(args && args.date!=null)
          options.showDate=args.date;
    if(args && args.relative!==null)
        options.useRelativeLabels=args.relative;
    return DateHelper.formatDate(this.translate,value,options);
  }
  constructor(private translate : TranslateService){}
}
