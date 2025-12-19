import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Assignment, Group, Permission, RestConstants, User } from 'ngx-edu-sharing-api';
import { VCard } from '../util/VCard';

@Pipe({
    name: 'assignment',
    standalone: false,
})
export class AssignmentPipe implements PipeTransform {
    constructor() {}

    transform(
        assignment: Assignment,
        args: { mode: 'permissions' } = { mode: 'permissions' },
    ): Permission['role'] {
        if (args.mode === 'permissions') {
            return assignment.permissions?.some((p) => p.role === 'COORDINATOR')
                ? 'COORDINATOR'
                : 'ASSIGNEE';
        }
        return null;
    }
}
