import { Component } from '@angular/core';
import { ListWidget } from '../list-widget';
import { ListItem } from '../../types/list-item';
import { StatisticsGroup } from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-list-counts',
    templateUrl: './list-counts.component.html',
    styleUrls: ['./list-counts.component.scss'],
})
export class ListCountsComponent extends ListWidget {
    static supportedItems = [
        new ListItem('NODE', 'counts.OVERALL'),
        new ListItem('NODE', 'counts.VIEW_MATERIAL'),
        new ListItem('NODE', 'counts.VIEW_MATERIAL_EMBEDDED'),
        new ListItem('NODE', 'counts.VIEW_MATERIAL_PLAY_MEDIA'),
        new ListItem('NODE', 'counts.DOWNLOAD_MATERIAL'),
    ];

    static getCountSingle(node: StatisticsGroup, id: string, group: number = null) {
        const counts = (node as any).counts;
        if (id === 'OVERALL') {
            return Object.keys(counts.counts)
                .map((c) => (group ? group : counts.counts[c]))
                .reduce((a, b) => a + b);
        }
        return (group ? group : counts.counts[id]) || 0;
    }
    static getCount(node: StatisticsGroup, id: string) {
        const counts = (node as any).counts;
        let result = this.getCountSingle(node, id);
        if (Object.keys(counts.groups)?.length > 0) {
            const i1 = id;
            if (counts.groups[i1]) {
                const i2 = Object.keys(counts.groups[i1])[0];
                if (counts.groups?.[i1]?.[i2]) {
                    result = Object.keys(counts.groups?.[i1]?.[i2])
                        .map(
                            (group) =>
                                (group || '-') +
                                ': ' +
                                this.getCountSingle(node, id, counts.groups?.[i1]?.[i2][group]),
                        )
                        .join('\n')
                        .trim();
                }
            }
        }
        return result;
    }

    getId() {
        return this.item.name.split('.')[1];
    }

    getCount() {
        return ListCountsComponent.getCount(this.node as StatisticsGroup, this.getId());
    }
}
