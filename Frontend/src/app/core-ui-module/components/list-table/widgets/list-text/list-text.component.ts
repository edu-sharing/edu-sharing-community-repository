import { Component, Input, OnInit } from '@angular/core';
import { ListWidget } from '../list-widget';
import { ListItem } from '../../../../../core-module/ui/list-item';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';
import {ProposalNode} from '../../../../../core-module/rest/data-object';

@Component({
    selector: 'app-list-text',
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

    constructor() {
        super();
    }
    getNode() {
        if(this.item.type === 'NODE_PROPOSAL') {
            return (this.node as ProposalNode).proposal;
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

    getI18n(item: ListItem) {
        return (item.type === 'NODE_PROPOSAL' ? 'NODE_PROPOSAL' : 'NODE') + '.' + item.name;
    }
}
