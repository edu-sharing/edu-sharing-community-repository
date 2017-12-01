import {PipeTransform, Pipe} from '@angular/core';
import {TranslateService} from "ng2-translate";
import {RestHelper} from "../rest/rest-helper";
import {Node} from "../rest/data-object";

@Pipe({name: 'nodeTitle'})
export class NodeTitlePipe implements PipeTransform {
  transform(node : Node,args:string[]): string {
    return RestHelper.getTitle(node);
  }
  constructor(private translate : TranslateService){}
}
