import { Component, HostBinding, Input, Optional } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { NodeEntriesService } from '../../../core-ui-module/node-entries.service';
import { ClickSource } from '../entries-model';

@Component({
    selector: 'es-node-stats-badges',
    templateUrl: './node-stats-badges.component.html',
    styleUrls: ['./node-stats-badges.component.scss'],
})
export class NodeStatsBadgesComponent {
    readonly ClickSource = ClickSource;

    childObjectCount = 0;

    private _node: Node;
    @Input()
    get node(): Node {
        return this._node;
    }
    set node(node: Node) {
        this._node = node;
        this.childObjectCount = this.getChildObjectCount(node);
    }

    @HostBinding('attr.backgroundStyle')
    @Input()
    backgroundStyle: 'darken' | 'lighten' = 'lighten';

    constructor(
        @Optional()
        public entriesService: NodeEntriesService<Node>,
    ) {}

    private getChildObjectCount(node: Node): number {
        const value = node.properties?.['virtual:childobjectcount']?.[0];
        if (value) {
            return parseInt(value);
        } else {
            return 0;
        }
    }
}
