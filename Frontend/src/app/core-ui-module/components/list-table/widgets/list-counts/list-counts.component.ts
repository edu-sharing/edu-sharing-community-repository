import { Component, Input, OnInit } from '@angular/core';
import { ListWidget } from '../list-widget';
import { ListItem } from '../../../../../core-module/ui/list-item';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';
import {Statistics} from "../../../../../core-module/rest/data-object";

@Component({
    selector: 'app-list-counts',
    templateUrl: './list-counts.component.html',
    styleUrls: ['./list-counts.component.scss'],
})
export class ListCountsComponent extends ListWidget {
    static supportedItems = [
        new ListItem('NODE', 'counts.OVERALL'),
        new ListItem('NODE', 'counts.VIEW_MATERIAL'),
        new ListItem('NODE', 'counts.VIEW_MATERIAL_EMBEDDED'),
        new ListItem('NODE', 'counts.DOWNLOAD_MATERIAL'),
    ];

    getId() {
        return this.item.name.split('.')[1];
    }
    getCountSingle(group: number = null) {
        const counts = (this.node as Statistics).counts;
        if(this.item.name === 'counts.OVERALL') {
            return Object.keys(counts.counts).map((c) => (group ? group : counts.counts[c])).reduce((a, b) =>
                a + b
            );
        }
        return (group ? group : counts.counts[this.getId()]) || 0;
    }

    getCount() {
        const counts = (this.node as Statistics).counts;
        let result = this.getCountSingle();
        if(Object.keys(counts.groups)?.length > 0) {
            const i1= this.getId();
            if(counts.groups[i1]) {
                const i2 = Object.keys(counts.groups[i1])[0];
                if (counts.groups?.[i1]?.[i2]) {
                    result = Object.keys(counts.groups?.[i1]?.[i2]).map((group) =>
                        (group || '-') + ': ' + this.getCountSingle(counts.groups?.[i1]?.[i2][group])
                    ).join('\n').trim();
                }
            }
        }
        return result;
    }
}
