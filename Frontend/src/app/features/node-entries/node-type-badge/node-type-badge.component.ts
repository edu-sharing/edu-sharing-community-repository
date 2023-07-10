import { Component, Input, OnChanges } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { NodeHelperService } from '../../../core-ui-module/node-helper.service';
import { CustomFieldSpecialType, NodeEntriesGlobalService } from '../node-entries-global.service';

/**
 * A small circular badge that depicts the node's type.
 *
 * To be used in the top bar of a card or similar places.
 */
@Component({
    selector: 'es-node-type-badge',
    templateUrl: './node-type-badge.component.html',
    styleUrls: ['./node-type-badge.component.scss'],
})
export class NodeTypeBadgeComponent implements OnChanges {
    @Input() node: Node;

    isCollection: boolean;

    constructor(
        public nodeHelper: NodeHelperService,
        private nodeEntriesGlobalService: NodeEntriesGlobalService,
    ) {}

    ngOnChanges(): void {
        this.isCollection = this.nodeHelper.isNodeCollection(this.node);
    }

    getCustomTemplate() {
        return this.nodeEntriesGlobalService.getCustomFieldTemplate(
            { type: 'NODE', name: CustomFieldSpecialType.type },
            this.node,
        );
    }
}
