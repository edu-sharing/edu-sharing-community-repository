import {Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import {NodeEntriesService} from '../../../node-entries.service';
import {Node} from '../../../../core-module/rest/data-object';
import {NodeHelperService} from '../../../node-helper.service';
import {ColorHelper, PreferredColor} from '../../../../core-module/ui/color-helper';
import {InteractionType} from '../../node-entries-wrapper/node-entries-wrapper.component';

@Component({
    selector: 'app-node-entries-card',
    templateUrl: 'node-entries-card.component.html',
    styleUrls: ['node-entries-card.component.scss']
})
export class NodeEntriesCardComponent<T extends Node> implements OnChanges {
    readonly InteractionType = InteractionType;
    @Input() node: T;
    constructor(
        public entriesService: NodeEntriesService<T>,
        public nodeHelper: NodeHelperService,
    ) {
    }

    ngOnChanges(changes: SimpleChanges): void {
    }

    getTextColor() {
        return ColorHelper.getPreferredColor(this.node.collection.color) === PreferredColor.Black ?
            '#000' : '#fff';
    }
}
