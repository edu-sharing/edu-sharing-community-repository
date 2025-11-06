import { Component, Input, OnChanges, Optional } from '@angular/core';
import { Assignment, GenericAuthority, Node } from 'ngx-edu-sharing-api';
import { CustomFieldSpecialType, NodeEntriesGlobalService } from '../node-entries-global.service';
import { NodeHelperService } from '../../services/node-helper.service';
import { NodeEntriesService } from '../../services/node-entries.service';
import { NodeEntriesDataType } from '../data-type';

/**
 * A small circular badge that depicts the node's type.
 *
 * To be used in the top bar of a card or similar places.
 */
@Component({
    selector: 'es-node-type-badge',
    templateUrl: './node-type-badge.component.html',
    styleUrls: ['./node-type-badge.component.scss'],
    standalone: false,
})
export class NodeTypeBadgeComponent implements OnChanges {
    @Input() node: NodeEntriesDataType;
    /**
     * when true, collection icons will resolve based on their type (editorial, private...)
     * When false, the generic svg image is used
     */
    @Input() collectionIcons = true;

    isCollection: boolean;

    constructor(
        public nodeHelper: NodeHelperService,
        private nodeEntriesGlobalService: NodeEntriesGlobalService,
        @Optional() public nodeEntriesService: NodeEntriesService<Node>,
    ) {}

    ngOnChanges(): void {
        this.isCollection = this.nodeHelper.isNodeCollection(this.node as Node);
    }

    getCustomTemplate() {
        return this.nodeEntriesGlobalService.getCustomFieldTemplate(
            { type: 'NODE', name: CustomFieldSpecialType.type },
            this.node as Node,
        );
    }

    materialIcon() {
        if ((this.node as GenericAuthority).authorityType === 'USER') {
            return 'person';
        } else if ((this.node as GenericAuthority).authorityType === 'GROUP') {
            return 'group';
        } else if ((this.node as Assignment).allowAdditionalDocumentSubmissions !== undefined) {
            return 'task';
        }

        return null;
    }
}
