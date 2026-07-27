import { Component, computed, inject, input, model } from '@angular/core';
import { AssignmentFile, Node } from 'ngx-edu-sharing-api';
import { SharedModule } from '../../../shared/shared.module';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { NodeHelperService, NodesRightMode } from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { AssignmentBase } from '../manage-assignment/manage-assignment.component';

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
    nodeHelperService = inject(NodeHelperService);

    readonly ChangePermissions = RestConstants.ACCESS_CHANGE_PERMISSIONS;
    readonly = input<boolean>(false);
    loading = input<boolean>(false);
    role = input<Role>('SUBMITTABLE');
    assignment = model.required<AssignmentBase>();
    nodes = model.required<NodeWithRole[]>();

    /** Nodes belonging to this section's role (defaulting a missing role to SUBMITTABLE). */
    readonly roleNodes = computed(() =>
        (this.nodes() || []).filter((n) => (n.documentRole || 'SUBMITTABLE') === this.role()),
    );

    drop(event: CdkDragDrop<NodeWithRole[]>) {
        const ordered = this.roleNodes().slice();
        moveItemInArray(ordered, event.previousIndex, event.currentIndex);
        // Write the reordered role nodes back into the full list, keeping other roles in place.
        let i = 0;
        this.nodes.set(
            this.nodes().map((n) =>
                (n.documentRole || 'SUBMITTABLE') === this.role() ? ordered[i++] : n,
            ),
        );
    }

    remove(item: NodeWithRole) {
        this.nodes.set(this.nodes().filter((n) => n !== item));
    }

    protected readonly NodesRightMode = NodesRightMode;

    isLicenseMedia(item: NodeWithRole) {
        return item.properties?.[RestConstants.CCM_PROP_RESTRICTED_ACCESS]?.[0] === 'true';
    }
}
