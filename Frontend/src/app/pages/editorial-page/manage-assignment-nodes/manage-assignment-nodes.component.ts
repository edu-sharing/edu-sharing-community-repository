import { Component, input } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { SharedModule } from '../../../shared/shared.module';

@Component({
    selector: 'es-manage-assignment-nodes',
    templateUrl: 'manage-assignment-nodes.component.html',
    styleUrls: ['manage-assignment-nodes.component.scss'],
    imports: [SharedModule],
})
export class ManageAssignmentNodesComponent {
    readonly nodes = input.required<Node[]>();
}
