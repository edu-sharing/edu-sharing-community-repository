import {PipeTransform, Pipe} from '@angular/core';
import {NodeHelper} from "./node-helper";
import {TranslateService} from "@ngx-translate/core";
import {ConfigurationService} from "../services/configuration.service";

@Pipe({name: 'replaceChars'})
export class ReplaceCharsPipe implements PipeTransform {
  transform(value : string,args:any): string {

    let i=0;
    if(!Array.isArray(args.search)){
        args.search=[args.search];
    }
    console.log(args);
    if(args.replace && !Array.isArray(args.replace)){
        args.replace=[args.replace];
    }
    for(let arg of args.search){
        value=value.split(arg).join(args.replace ? args.replace[i] : "");
        i++;
    }
    return value;
  }
  constructor(private translate : TranslateService,private config:ConfigurationService){}
}
