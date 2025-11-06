import { Component, input, model } from '@angular/core';
import { AssignmentFile, Node } from 'ngx-edu-sharing-api';
import { SharedModule } from '../../../shared/shared.module';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { NodeHelperService, NodesRightMode } from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { MatSelectChange } from '@angular/material/select';
import { AssignmentBase } from '../manage-assignment/manage-assignment.component';
import { TranslateService } from '@ngx-translate/core';
import { map } from 'rxjs/operators';
import { BehaviorSubject } from 'rxjs';

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
    readonly = input<boolean>(false);
    assignment = model.required<AssignmentBase>();
    nodes = model.required<NodeWithRole[]>();
    readonly translateReady$ = new BehaviorSubject<boolean>(false);

    drop(event: CdkDragDrop<NodeWithRole[]>) {
        moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
        this.nodes.set(this.nodes());
    }

    constructor(private translate: TranslateService, public nodeHelperService: NodeHelperService) {
        // dirty hack for https://github.com/angular/components/issues/7923
        this.translate
            .get('ANY')
            .pipe(map(() => true))
            .subscribe(this.translateReady$);
    }

    remove(item: NodeWithRole) {
        this.nodes().splice(this.nodes().indexOf(item), 1);
        this.nodes.set(this.nodes());
    }

    protected readonly NodesRightMode = NodesRightMode;
    compare(o1: any, o2: any) {
        return false;
    }

    setRole(item: NodeWithRole, $event: MatSelectChange<Role>) {
        item.documentRole = $event.value;
        this.nodes.set(this.nodes());
    }

    isLicenseMedia(item: NodeWithRole) {
        return item.properties?.[RestConstants.CCM_PROP_RESTRICTED_ACCESS]?.[0] === 'true';
    }
}
