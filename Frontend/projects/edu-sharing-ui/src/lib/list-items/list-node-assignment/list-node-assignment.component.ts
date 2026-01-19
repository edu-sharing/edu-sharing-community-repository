import { Component } from '@angular/core';
import { ListWidget } from '../list-widget';
import { ListItem } from '../../types/list-item';
import { NodeHelperService } from '../../services/node-helper.service';
import { Assignment, Node, RestConstants, Submission } from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-list-node-assignment',
    templateUrl: './list-node-assignment.component.html',
    styleUrls: ['./list-node-assignment.component.scss'],
    standalone: false,
})
export class ListNodeAssignmentComponent extends ListWidget {
    static supportedItems = [new ListItem('ASSIGNMENT', '*'), new ListItem('SUBMISSION', '*')];

    get assignment() {
        return this.node as Assignment;
    }
    get submission() {
        return this.node as Submission;
    }

    constructor(private nodeHelper: NodeHelperService) {
        super();
    }
}
