import { PipeTransform, Pipe } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { RestConstants } from '../../core-module/rest/rest-constants';
import { Node } from '../../core-module/rest/data-object';
import { VCard } from '../../core-module/ui/VCard';

/**
 * Format the version label and checking constants if required
 */
@Pipe({ name: 'nodeAuthorName' })
export class NodeAuthorNamePipe implements PipeTransform {
    transform(node: Node, args: any = null): string {
        if (node.properties[RestConstants.CCM_PROP_AUTHOR_FREETEXT]?.[0]) {
            return node.properties[RestConstants.CCM_PROP_AUTHOR_FREETEXT]?.[0];
        } else if (node.properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR]?.[0]) {
            return new VCard(
                node.properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR]?.[0],
            ).getDisplayName();
        } else {
            return this.translate.instant('MDS.AUTHOR_UNSET');
        }
    }
    constructor(private translate: TranslateService) {}
}
