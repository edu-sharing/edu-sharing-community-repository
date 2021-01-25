import { Component, Input, OnInit } from '@angular/core';
import { ListWidget } from '../list-widget';
import { ListItem } from '../../../../../core-module/ui/list-item';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';

@Component({
    selector: 'app-list-text',
    templateUrl: './list-text.component.html',
})
export class ListTextComponent extends ListWidget {
    static supportedItems = [
        new ListItem('NODE', '*'),
        new ListItem('COLLECTION', '*'),
        new ListItem('ORG', '*'),
        new ListItem('GROUP', '*'),
        new ListItem('USER', '*'),
    ];
    readonly DATE_FIELDS = RestConstants.DATE_FIELDS;

    constructor() {
        super();
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
}
