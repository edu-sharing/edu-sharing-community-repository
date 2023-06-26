/**
 * Created by Torsten on 13.01.2017.
 */

import { Component, Input, OnInit } from '@angular/core';
import { UIService } from '../../../core-module/rest/services/ui.service';
import { NotificationListComponent } from '../notification-list/notification-list.component';
import { Node } from '../../../core-module/rest/data-object';

@Component({
    selector: 'es-notification-list-entry',
    templateUrl: 'notification-list-entry.component.html',
    styleUrls: ['notification-list-entry.component.scss'],
})
export class NotificationListEntryComponent implements OnInit {
    static readonly icons = {
        AddToCollectionEvent: 'layers',
        CommentEvent: 'comment',
        InviteEvent: 'person_add',
        NodeIssueEvent: 'flag',
        RatingEvent: 'star',
        WorkflowEvent: 'swap_calls',
    };
    @Input() entry: any = {
        _class: Object.keys(NotificationListEntryComponent.icons)[Math.floor(Math.random() * 5)],
        status: Math.random() > 0.5 ? 'READ' : 'PENDING',
        title: 'Mein PDF.pdf',
        reason: 'Test-Problem',
        collection: 'Test-Sammlung',
        workflowStatus: 'Test-Status',
        creator: 'Max Muster',
        timestamp: new Date().getTime(),
    };
    constructor(private uiService: UIService) {}

    ngOnInit() {}

    getIcon() {
        return (NotificationListEntryComponent.icons as any)[this.entry._class];
    }

    markAsRead() {
        this.entry.status = 'READ';
    }

    getNode() {
        return new Node(this.entry.title);
    }
}
