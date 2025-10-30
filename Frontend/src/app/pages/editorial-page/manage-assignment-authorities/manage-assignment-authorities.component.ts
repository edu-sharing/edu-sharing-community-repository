import { Component, input, model } from '@angular/core';
import { Authority } from 'ngx-edu-sharing-api';
import { SharedModule } from '../../../shared/shared.module';
import { Assignment } from 'ngx-edu-sharing-api';
import { NodeHelperService, NodesRightMode } from 'ngx-edu-sharing-ui';
import { MatSelectChange } from '@angular/material/select';

type Role = 'ASSIGNEE' | 'COORDINATOR';
export type AuthorityWithSubmission = Authority & {
    role?: Role;
};

@Component({
    selector: 'es-manage-assignment-authorities',
    templateUrl: 'manage-assignment-authorities.component.html',
    styleUrls: ['manage-assignment-authorities.component.scss'],
    imports: [SharedModule],
})
export class ManageAssignmentAuthoritiesComponent {
    assignment = input.required<Assignment>();
    authorities = model.required<AuthorityWithSubmission[]>();

    constructor(public nodeHelperService: NodeHelperService) {}

    remove(item: AuthorityWithSubmission) {
        this.authorities().splice(this.authorities().indexOf(item), 1);
        this.authorities.set(this.authorities());
    }

    protected readonly NodesRightMode = NodesRightMode;

    setRole(item: AuthorityWithSubmission, $event: MatSelectChange<Role>) {
        item.role = $event.value;
        this.authorities.set(this.authorities());
    }
}
