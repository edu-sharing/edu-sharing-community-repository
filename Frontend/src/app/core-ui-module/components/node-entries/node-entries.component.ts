import {OPTIONS_HELPER_CONFIG, OptionsHelperService} from '../../options-helper.service';
import {
    AfterViewInit,
    Component,
    ContentChild, Input,
    OnChanges,
    SimpleChanges,
    TemplateRef
} from '@angular/core';
import {NodeEntriesService} from '../../node-entries.service';
import {Node} from '../../../core-module/rest/data-object';
import {NodeEntriesDisplayType} from '../node-entries-wrapper/node-entries-wrapper.component';

@Component({
    selector: 'app-node-entries',
    templateUrl: 'node-entries.component.html',
    styleUrls: ['node-entries.component.scss'],
})
export class NodeEntriesComponent<T extends Node> implements OnChanges {
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    @ContentChild('empty') emptyRef: TemplateRef<any>;

    constructor(
        private optionsHelper: OptionsHelperService,
        public entriesService: NodeEntriesService<T>,
    ) {

    }

    ngOnChanges(changes: SimpleChanges): void {
    }
}
