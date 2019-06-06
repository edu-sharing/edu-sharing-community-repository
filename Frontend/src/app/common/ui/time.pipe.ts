import {PipeTransform, Pipe} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {DateHelper} from "../../core-ui-module/DateHelper";
import {isNumeric} from "rxjs/util/isNumeric";

/**
 * Format a given value of time in seconds to a readable time span
 * e.g. 2m 5s
 */
@Pipe({name: 'formatTime'})
export class TimePipe implements PipeTransform {
  transform(value : number,args:any): string {
    if(!isNumeric(value))
      return "invalid value";

    let hours=Math.floor(value/3600);
    let minutes=Math.floor((value/60)%60);
    let seconds=Math.floor(value%60);
    let str="";
    if(hours>0){
      str+=hours+" "+this.translate.instant("HOUR"+(hours!=1 ? 'S' : ''))+" ";
    }
    if(minutes>0){
      str+=minutes+" "+this.translate.instant("MINUTE"+(minutes!=1 ? 'S' : ''))+" ";
    }
    if(seconds>0){
      str+=seconds+" "+this.translate.instant("SECOND"+(seconds!=1 ? 'S' : ''))+" ";
    }
    return str.trim();
  }
  constructor(private translate : TranslateService){}
}
