import { Pipe, PipeTransform } from '@angular/core';
import { Assignment, Permission } from 'ngx-edu-sharing-api';

@Pipe({
    name: 'assignment',
    standalone: false,
})
export class AssignmentPipe implements PipeTransform {
    transform(
        assignment: Assignment,
        args: {
            mode: 'endTimePriority' | 'permissions' | 'submissionsDone' | 'submissionsTotal';
        } = {
            mode: 'permissions',
        },
    ): Permission['role'] | number | 'high' | 'low' {
        if (args.mode === 'permissions') {
            return assignment.isCoordinator ? 'COORDINATOR' : 'ASSIGNEE';
        }
        if (args.mode === 'endTimePriority') {
            const now = new Date().getTime();
            /*const permissions = new AssignmentPipe().transform(assignment, { mode: 'permissions' });
            if (permissions === 'COORDINATOR') {
                if (assignment.status !== 'INPROGRESS') {
                    return 'low';
                }
            } else if (permissions === 'ASSIGNEE') {
                if (
                    assignment.submissions?.[0]?.submissionStatus === 'FINISHED' ||
                    assignment.submissions?.[0]?.validationStatus === 'FINISHED'
                ) {
                    return 'low';
                }
            }*/
            const delayUntil =
                (Date.parse(assignment.endTime as string) ||
                    (assignment.endTime as unknown as number)) - now;
            // delayed / old
            if (delayUntil < 0) {
                return 'high';
            }
            return 'low';
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
