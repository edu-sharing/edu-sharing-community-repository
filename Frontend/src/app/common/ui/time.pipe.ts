import {PipeTransform, Pipe} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {DateHelper} from "./DateHelper";
import {isNumeric} from "rxjs/util/isNumeric";

/**
 * Format a given value of time in seconds to a readable time span
 * e.g. 2m 5s
 */
@Pipe({name: 'formatTime'})
export class TimePipe implements PipeTransform {
  transform(value : any,args:any): string {
    if(!isNumeric(value))
      return "invalid value";

    let hours=Math.floor(value/3600);
    let minutes=Math.floor((value/60)%60);
    let seconds=Math.floor(value%60);
    let str="";
    if(hours>0){
      str+=hours+" "+this.translate.instant("HOURS")+" ";
    }
    if(minutes>0){
      str+=minutes+" "+this.translate.instant("MINUTES")+" ";
    }
    if(seconds>0){
      str+=seconds+" "+this.translate.instant("SECONDS")+" ";
    }
    return str.trim();
  }
  constructor(private translate : TranslateService){}
}
