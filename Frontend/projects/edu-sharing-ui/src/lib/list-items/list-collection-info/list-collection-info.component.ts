import { Component } from '@angular/core';
import { ListItem } from '../../types/list-item';
import { ListWidget } from '../list-widget';
import { NodeHelperService } from '../../services/node-helper.service';
import { AccessibilityService } from '../../services/accessibility.service';

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

    readonly indicatorIcons$;

    constructor(private accessibility: AccessibilityService, public nodeHelper: NodeHelperService) {
        super();
        this.indicatorIcons$ = this.accessibility.observe('indicatorIcons');
    }
}
