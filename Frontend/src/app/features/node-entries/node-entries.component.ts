import {
    AfterViewInit,
    Component,
    ContentChild, HostListener, Input,
    OnChanges,
    SimpleChanges,
    TemplateRef
} from '@angular/core';
import { UIService, GenericAuthority, Node } from '../../core-module/core.module';
import { KeyEvents } from '../../core-module/ui/key-events';
import { NodeEntriesService } from '../../core-ui-module/node-entries.service';
import { NodeEntriesDisplayType } from './entries-model';

import {NodeEntriesTemplatesService} from './node-entries-templates.service';

@Component({
    selector: 'es-node-entries',
    templateUrl: 'node-entries.component.html',
    styleUrls: ['node-entries.component.scss'],

})
export class NodeEntriesComponent<T extends NodeEntriesDataType> implements OnChanges {
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;

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
        public templatesService: NodeEntriesTemplatesService,
    ) {

    }

    ngOnChanges(changes: SimpleChanges): void {
    }
}
export type NodeEntriesDataType = Node | GenericAuthority;
