import { Component, HostBinding, Input, inject } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { ClickSource } from '../entries-model';
import { NodeEntriesService } from '../../services/node-entries.service';

@Component({
    selector: 'es-node-stats-badges',
    templateUrl: './node-stats-badges.component.html',
    styleUrls: ['./node-stats-badges.component.scss'],
    standalone: false,
})
export class NodeStatsBadgesComponent {
    entriesService = inject<NodeEntriesService<Node>>(NodeEntriesService, { optional: true });

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

    // Angular types `$event` as plain `Event` for filtered bindings like `(keydown.enter)`.
    onCommentsActivated(event: Event): void {
        this.entriesService?.onClicked({
            event: event as MouseEvent | KeyboardEvent,
            element: this.node,
            source: ClickSource.Comments,
        });
    }

    private getChildObjectCount(node: Node): number {
        const value = node.properties?.['virtual:childobjectcount']?.[0];
        if (value) {
            return parseInt(value);
        } else {
            return 0;
        }
    }
}
