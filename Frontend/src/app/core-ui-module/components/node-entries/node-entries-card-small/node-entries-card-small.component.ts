import {Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import {NodeEntriesService} from '../../../node-entries.service';
import {Node} from '../../../../core-module/rest/data-object';
import {NodeHelperService} from '../../../node-helper.service';
import {ColorHelper, PreferredColor} from '../../../../core-module/ui/color-helper';
import {InteractionType} from '../../node-entries-wrapper/node-entries-wrapper.component';
import {OptionItem, Target} from '../../../option-item';

@Component({
    selector: 'app-node-entries-card-small',
    templateUrl: 'node-entries-card-small.component.html',
    styleUrls: ['node-entries-card-small.component.scss']
})
export class NodeEntriesCardSmallComponent<T extends Node> implements OnChanges {
    readonly InteractionType = InteractionType;
    readonly Target = Target;
    @Input() node: T;
    constructor(
        public entriesService: NodeEntriesService<T>,
        public nodeHelper: NodeHelperService,
    ) {
    }

    ngOnChanges(changes: SimpleChanges): void {
    }
    optionsOnCard() {
        const options = this.entriesService.options[Target.List];
        const always = options.filter((o) => o.showAlways);
        if (always.some((o) => o.showCallback(this.node))) {
           return always;
        }
        return options.filter((o) => o.showAsAction && o.showCallback(this.node)).slice(0, 3);
    }
}
