import { Component, input, model } from '@angular/core';
import { AssignmentFile, Node } from 'ngx-edu-sharing-api';
import { SharedModule } from '../../../shared/shared.module';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { NodeHelperService, NodesRightMode } from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { MatSelectChange } from '@angular/material/select';

type Role = 'SUPPLEMENTARY' | 'SUBMITTABLE';
export type NodeWithRole = Node &
    Pick<AssignmentFile, 'documentRole' | 'isDone'> & {
        refId?: string;
    };

@Component({
    selector: 'es-manage-assignment-nodes',
    templateUrl: 'manage-assignment-nodes.component.html',
    styleUrls: ['manage-assignment-nodes.component.scss'],
    imports: [SharedModule],
})
export class ManageAssignmentNodesComponent {
    readonly ChangePermissions = RestConstants.ACCESS_CHANGE_PERMISSIONS;
    nodes = model.required<NodeWithRole[]>();
    drop(event: CdkDragDrop<NodeWithRole[]>) {
        moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
        this.nodes.set(this.nodes());
    }

    constructor(public nodeHelperService: NodeHelperService) {}

    remove(item: NodeWithRole) {
        this.nodes().splice(this.nodes().indexOf(item), 1);
        this.nodes.set(this.nodes());
    }

    protected readonly NodesRightMode = NodesRightMode;

    setRole(item: NodeWithRole, $event: MatSelectChange<Role>) {
        item.documentRole = $event.value;
        this.nodes.set(this.nodes());
    }
}
