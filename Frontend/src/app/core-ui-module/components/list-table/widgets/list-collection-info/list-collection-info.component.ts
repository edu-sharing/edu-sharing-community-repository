import { Component } from '@angular/core';
import { AccessibilityService } from 'src/app/common/ui/accessibility/accessibility.service';
import { ListItem } from '../../../../../core-module/ui/list-item';
import { NodeHelperService } from '../../../../node-helper.service';
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
