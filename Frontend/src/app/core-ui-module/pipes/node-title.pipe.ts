import {PipeTransform, Pipe} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {RestHelper} from '../../core-module/core.module';
import {UniversalNode} from '../../common/definitions';

@Pipe({name: 'nodeTitle'})
export class NodeTitlePipe implements PipeTransform {
  transform(node : UniversalNode,args:string[] = null): string {
    return RestHelper.getTitle(node);
  }
  constructor(private translate : TranslateService){}
}
