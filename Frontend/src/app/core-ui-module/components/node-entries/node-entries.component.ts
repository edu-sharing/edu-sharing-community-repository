import {OPTIONS_HELPER_CONFIG, OptionsHelperService} from '../../options-helper.service';
import {
    AfterViewInit,
    Component,
    ContentChild, HostListener, Input,
    OnChanges,
    SimpleChanges,
    TemplateRef
} from '@angular/core';
import {NodeEntriesService} from '../../node-entries.service';
import {Node} from '../../../core-module/rest/data-object';
import {NodeEntriesDisplayType} from '../node-entries-wrapper/node-entries-wrapper.component';
import {KeyEvents} from '../../../core-module/ui/key-events';
import {UIService} from '../../../core-module/rest/services/ui.service';

@Component({
    selector: 'app-node-entries',
    templateUrl: 'node-entries.component.html',
    styleUrls: ['node-entries.component.scss'],
})
export class NodeEntriesComponent<T extends Node> implements OnChanges {
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    @ContentChild('title') titleRef: TemplateRef<any>;
    @ContentChild('empty') emptyRef: TemplateRef<any>;


    @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent): void {
        if (
            event.code === 'KeyA' &&
            (event.ctrlKey || this.uiService.isAppleCmd()) &&
            !KeyEvents.eventFromInputField(event)
        ) {
            if(this.entriesService.selection.isEmpty()) {
                this.entriesService.selection.select(...this.entriesService.dataSource.getData());
            } else {
                this.entriesService.selection.clear();
            }
            event.preventDefault();
            event.stopPropagation();
        }
    }


    constructor(
        private uiService: UIService,
        public entriesService: NodeEntriesService<T>,
    ) {

    }

    ngOnChanges(changes: SimpleChanges): void {
    }
}
