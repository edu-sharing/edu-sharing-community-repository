import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Node, RestConstants } from 'ngx-edu-sharing-api';
import { RestHelper } from '../util/rest-helper';
import { NodeRoot } from '../node-entries/entries-model';
import { isString } from 'lodash-es';

@Pipe({ name: 'nodeTitle' })
export class NodeTitlePipe implements PipeTransform {
    transform(node: Node | NodeRoot | 'HOME', args?: { type: 'name' | 'title' }): string {
        if (!(node as Node)?.name && isString(node)) {
            if (node === 'HOME') {
                return this.translate.instant('WORKSPACE.' + node);
            }
            return this.translate.instant('WORKSPACE.' + node);
        }
        const value =
            args?.type === 'name'
                ? RestHelper.getName(node as Node)
                : RestHelper.getTitle(node as Node);
        if ((node as Node)?.properties?.[RestConstants.CCM_PROP_MAPTYPE]) {
            return this.translate.instant(
                'MAPTYPE.' + (node as Node)?.properties?.[RestConstants.CCM_PROP_MAPTYPE][0],
                {
                    fallback: value,
                },
            );
        }
        return value;
    }
    constructor(private translate: TranslateService) {}
}
