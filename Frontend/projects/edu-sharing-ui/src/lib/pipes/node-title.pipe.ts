import { PipeTransform, Pipe } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { RestHelper } from '../util/rest-helper';

@Pipe({ name: 'nodeTitle' })
export class NodeTitlePipe implements PipeTransform {
    transform(node: Node, args: string[] = null): string {
        return RestHelper.getTitle(node);
    }
    constructor(private translate: TranslateService) {}
}
