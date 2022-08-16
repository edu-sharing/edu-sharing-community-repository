import { Component } from '@angular/core';
import { ListItem, RestConstants, ProposalNode, Node } from 'src/app/core-module/core.module';
import { NodeHelperService } from 'src/app/core-ui-module/node-helper.service';
import { ListWidget } from '../list-widget';

@Component({
    selector: 'es-list-text',
    templateUrl: './list-text.component.html',
})
export class ListTextComponent extends ListWidget {
    static supportedItems = [
        new ListItem('NODE', '*'),
        new ListItem('NODE_PROPOSAL', '*'),
        new ListItem('COLLECTION', '*'),
        new ListItem('ORG', '*'),
        new ListItem('GROUP', '*'),
        new ListItem('USER', '*'),
    ];
    readonly DATE_FIELDS = RestConstants.DATE_FIELDS;
    readonly VCARD_FIELDS = RestConstants.getAllVCardFields();

    constructor(private nodeHelper: NodeHelperService) {
        super();
    }
    getNode() {
        if (this.item.type === 'NODE_PROPOSAL') {
            return (this.node as ProposalNode).proposal || this.node;
        } else if ((this.node as Node).type === RestConstants.CCM_TYPE_COLLECTION_PROPOSAL) {
            return (this.node as Node).relations?.Original ?? this.node;
        }
        return this.node;
    }

    isUserProfileAttribute(attribute: string) {
        return (
            [
                RestConstants.AUTHORITY_FIRSTNAME,
                RestConstants.AUTHORITY_LASTNAME,
                RestConstants.AUTHORITY_EMAIL,
            ].indexOf(attribute) !== -1
        );
    }
    getWorkflowStatus() {
        return this.nodeHelper.getWorkflowStatus(this.node as Node).current;
    }

    getI18n(item: ListItem) {
        return (item.type === 'NODE_PROPOSAL' ? 'NODE_PROPOSAL' : 'NODE') + '.' + item.name;
    }
}
