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
        args: { mode: 'permissions' | 'submissionsDone' | 'submissionsTotal' } = {
            mode: 'permissions',
        },
    ): Permission['role'] | number {
        if (args.mode === 'permissions') {
            return assignment.permissions?.some((p) => p.role === 'COORDINATOR')
                ? 'COORDINATOR'
                : 'ASSIGNEE';
        }
        if (args.mode === 'submissionsTotal') {
            return assignment.permissions?.filter((p) => p.role === 'ASSIGNEE').length || 0;
        }
        if (args.mode === 'submissionsDone') {
            return (
                assignment.submissions?.filter((p) => p.submissionStatus === 'FINISHED').length || 0
            );
        }
        return null;
    }
}
