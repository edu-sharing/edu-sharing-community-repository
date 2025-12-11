import { Component, Input } from '@angular/core';
import { Target } from '../../types/option-item';
import { ClickSource, InteractionType } from '../entries-model';

import { NodeEntriesTemplatesService } from '../node-entries-templates.service';
import { NodeEntriesService } from '../../services/node-entries.service';
import { NodeHelperService } from '../../services/node-helper.service';
import { Assignment, Node } from 'ngx-edu-sharing-api';
import { DropdownComponent } from '../../dropdown/dropdown.component';

@Component({
    selector: 'es-node-entries-card-small',
    templateUrl: 'node-entries-card-small.component.html',
    styleUrls: ['node-entries-card-small.component.scss'],
    standalone: false,
})
export class NodeEntriesCardSmallComponent<T extends Node> {
    readonly ClickSource = ClickSource;
    readonly InteractionType = InteractionType;
    readonly Target = Target;
    @Input() node: T;
    @Input() dropdown: DropdownComponent;

    constructor(
        public entriesService: NodeEntriesService<T>,
        public nodeHelper: NodeHelperService,
        public templatesService: NodeEntriesTemplatesService,
    ) {}

    optionsOnCard() {
        const options = this.entriesService.options[Target.List];
        const always = options.filter((o) => o.showAlways);
        if (always.some((o) => o.showCallback([this.node]))) {
            return always;
        }
        return options.filter((o) => o.showAsAction && o.showCallback([this.node])).slice(0, 3);
    }
    async openMenu(event: MouseEvent, node: T) {
        event.stopPropagation();
        this.entriesService.openDropdown(this.dropdown, node);
    }
    openContextmenu(event: MouseEvent | Event) {
        event.preventDefault();
        event.stopPropagation();
    }

    readonly AssignmentStatusIcon: { [key in Assignment['status']]: string } = {
        DRAFT: 'news',
        INPROGRESS: 'schedule_send',
        CANCELED: 'cancel',
        FINISHED: 'done',
    };

    assignmentEndTimePriority(endTime: string) {
        const now = new Date().getTime();
        const delayUntil = Date.parse(endTime) - now;
        // > 5 days == low delay
        if (delayUntil > 3600 * 1000 * 24 * 5) {
            return 'low';
            // > 2 days == medium delay
        } else if (delayUntil > 3600 * 1000 * 24 * 2) {
            return 'medium';
        }
        return 'high';
    }
}
