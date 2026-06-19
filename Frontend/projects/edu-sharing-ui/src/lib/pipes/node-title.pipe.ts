import { Injectable, Pipe, PipeTransform, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Assignment, AssignmentFile, Node, RestConstants } from 'ngx-edu-sharing-api';
import { RestHelper } from '../util/rest-helper';
import { NodeRoot } from '../node-entries/entries-model';
import { isString } from 'lodash-es';

@Injectable({ providedIn: 'root' })
@Pipe({
    name: 'nodeTitle',
    standalone: false,
})
export class NodeTitlePipe implements PipeTransform {
    private translate = inject(TranslateService);

    transform(
        node: Node | Assignment | AssignmentFile | NodeRoot | 'HOME',
        args?: { type: 'name' | 'title' },
    ): string {
        if ((node as AssignmentFile)?.referNode) {
            node = (node as AssignmentFile)?.referNode;
        }
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
}
