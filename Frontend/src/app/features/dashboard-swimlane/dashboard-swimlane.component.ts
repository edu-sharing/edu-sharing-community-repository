import { CommonModule } from '@angular/common';
import { Component, input } from '@angular/core';
import { EduSharingUiCommonModule, NodeEntriesService } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'es-dashboard-swimlane',
    providers: [NodeEntriesService],
    standalone: true,
    templateUrl: './dashboard-swimlane.component.html',
    styleUrls: ['./dashboard-swimlane.component.scss'],
    imports: [CommonModule, EduSharingUiCommonModule],
})
export class DashboardSwimlaneComponent {
    readonly type = input.required<string>();
}
