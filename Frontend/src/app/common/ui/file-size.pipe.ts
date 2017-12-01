import {PipeTransform, Pipe} from '@angular/core';
import {Translation} from "../translation";

@Pipe({name: 'formatSize'})
export class FormatSizePipe implements PipeTransform {
  transform(value : any,args:string[]): string {
    let names=["bytes","KB","MB","GB","TB"];
    let i=0;
    if(isNaN(value))
      return value;
    if(value==null)
      value=0;
    while(value>1024 && i<names.length){
      value/=1024;
      i++;
    }
    //return value+" "+names[i];
    return value.toLocaleString(Translation.getLanguage(),{maximumFractionDigits:1})+" "+names[i];
  }
}
