import { Component, Input, OnInit } from '@angular/core';
import { ListWidget } from '../list-widget';
import { ListItem } from '../../../../../core-module/ui/list-item';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';
import {Statistics} from "../../../../../core-module/rest/data-object";

@Component({
    selector: 'app-list-counts',
    templateUrl: './list-counts.component.html',
})
export class ListCountsComponent extends ListWidget {
    static supportedItems = [
        new ListItem('NODE', 'counts.OVERALL'),
        new ListItem('NODE', 'counts.VIEW_MATERIAL'),
        new ListItem('NODE', 'counts.VIEW_MATERIAL_EMBEDDED'),
        new ListItem('NODE', 'counts.DOWNLOAD_MATERIAL'),
    ];

    getCount() {
        const counts = (this.node as Statistics).counts;
        if(this.item.name === 'counts.OVERALL') {
            return Object.keys(counts).map((c) => counts[c]).reduce((a, b) =>
                a + b
            );
        }
        return counts[this.item.name.split('.')[1]];
    }
}
