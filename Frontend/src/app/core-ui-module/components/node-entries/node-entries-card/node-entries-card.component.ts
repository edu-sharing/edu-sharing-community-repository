import {OPTIONS_HELPER_CONFIG, OptionsHelperService} from '../../../options-helper.service';
import {
    AfterViewInit,
    Component,
    ContentChild, Input,
    OnChanges,
    SimpleChanges,
    TemplateRef
} from '@angular/core';
import {NodeEntriesService} from '../../../node-entries.service';
import {Node} from '../../../../core-module/rest/data-object';

@Component({
    selector: 'app-node-entries-card',
    templateUrl: 'node-entries-card.component.html',
    styleUrls: ['node-entries-card.component.scss']
})
export class NodeEntriesCardComponent<T extends Node> implements OnChanges {
    @Input() node: T;
    constructor(
        public entriesService: NodeEntriesService<T>,
    ) {
    }

    ngOnChanges(changes: SimpleChanges): void {
    }
}
