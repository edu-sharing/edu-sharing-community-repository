/**
 * Created by Torsten on 13.01.2017.
 */

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { UIService } from '../../../core-module/rest/services/ui.service';
import { Node } from '../../../core-module/rest/data-object';
import { Notification } from 'ngx-edu-sharing-api';
import { RestConstants } from '../../../core-module/rest/rest-constants';

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
    @Input() entry: Notification;

    /*: any = {
        _class: Object.keys(NotificationListEntryComponent.icons)[Math.floor(Math.random() * 5)],
        status: Math.random() > 0.5 ? 'READ' : 'PENDING',
        title: 'Mein PDF.pdf',
        reason: 'Test-Problem',
        collection: 'Test-Sammlung',
        workflowStatus: 'Test-Status',
        creator: 'Max Muster',
        timestamp: new Date().getTime(),
    };*/
    @Output() statusChange = new EventEmitter<'PENDING' | 'SENT' | 'READ'>();
    constructor(private uiService: UIService) {}

    ngOnInit() {}

    getIcon() {
        return (NotificationListEntryComponent.icons as any)[this.entry._class];
    }

    markAsRead() {
        this.statusChange.emit('READ');
    }

    getNode() {
        const props = (this.entry as any).node.properties;
        const node = new Node(props[RestConstants.SYS_NODE_UUID]);
        node.isDirectory = props['virtual:type'] === RestConstants.CCM_TYPE_MAP;
        return node;
    }
}
