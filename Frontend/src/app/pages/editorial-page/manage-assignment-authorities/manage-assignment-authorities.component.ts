import { Component, input, model } from '@angular/core';
import { SharedModule } from '../../../shared/shared.module';
import { NodeHelperService, NodesRightMode } from 'ngx-edu-sharing-ui';
import { MatSelectChange } from '@angular/material/select';
import { AssignmentBase } from '../manage-assignment/manage-assignment.component';
import { Permission } from 'ngx-edu-sharing-api';

type Role = 'ASSIGNEE' | 'COORDINATOR';

@Component({
    selector: 'es-manage-assignment-authorities',
    templateUrl: 'manage-assignment-authorities.component.html',
    styleUrls: ['manage-assignment-authorities.component.scss'],
    imports: [SharedModule],
})
export class ManageAssignmentAuthoritiesComponent {
    assignment = input.required<AssignmentBase>();
    authorities = model.required<Permission[]>();

    constructor(public nodeHelperService: NodeHelperService) {}

    remove(item: Permission) {
        this.authorities().splice(this.authorities().indexOf(item), 1);
        this.authorities.set(this.authorities());
    }

    protected readonly NodesRightMode = NodesRightMode;

    setRole(item: Permission, $event: MatSelectChange<Role>) {
        item.role = $event.value;
        this.authorities.set(this.authorities());
    }
}
