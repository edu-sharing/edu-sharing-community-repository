import { Component } from '@angular/core';
import { AccessibilityService } from '../../../services/accessibility.service';
import { ListItem } from '../../../core-module/core.module';
import { NodeHelperService } from '../../../core-ui-module/node-helper.service';

import { ListWidget } from '../list-widget';

@Component({
    selector: 'es-list-collection-info',
    templateUrl: './list-collection-info.component.html',
    styleUrls: ['./list-collection-info.component.scss'],
})
export class ListCollectionInfoComponent extends ListWidget {
    static supportedItems = [
        new ListItem('COLLECTION', 'info'),
        new ListItem('COLLECTION', 'scope'),
    ];

    readonly indicatorIcons$ = this.accessibility.observe('indicatorIcons');

    constructor(private accessibility: AccessibilityService, public nodeHelper: NodeHelperService) {
        super();
    }
}
