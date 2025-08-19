import { CommonModule } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import {
    EduSharingUiCommonModule,
    NodeEntriesModule,
    NodeEntriesService,
    UIAnimation,
} from 'ngx-edu-sharing-ui';
import { InviteEvent, UserEvent } from 'ngx-edu-sharing-api';
import { MatButtonModule } from '@angular/material/button';
import { trigger } from '@angular/animations';
import { TranslateModule } from '@ngx-translate/core';
import { SharedModule } from '../../../shared/shared.module';

enum TimeGroups {
    Today = 'Today',
    Yesterday = 'Yesterday',
    Last7Days = 'Last7Days',
    Last30Days = 'Last30Days',
    Older = 'Older',
}
const TimeGroupsSort = [
    TimeGroups.Today,
    TimeGroups.Yesterday,
    TimeGroups.Last7Days,
    TimeGroups.Last30Days,
    TimeGroups.Older,
];
type EventsGrouped = { [key in TimeGroups]?: UserEvent[] };

@Component({
    selector: 'es-dashboard-interactivity-stream',
    providers: [NodeEntriesService],
    standalone: true,
    templateUrl: './dashboard-interactivity-stream.component.html',
    styleUrls: ['./dashboard-interactivity-stream.component.scss'],
    animations: [trigger('openOverlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST))],
    imports: [
        CommonModule,
        EduSharingUiCommonModule,
        TranslateModule,
        MatButtonModule,
        NodeEntriesModule,
        SharedModule,
    ],
})
export class DashboardInteractivityStreamComponent {
    // readonly TimeGroups = TimeGroups;
    readonly events = input.required<(UserEvent | InviteEvent)[]>();
    readonly type = input.required<'activity' | 'share'>();
    readonly oldestEvent = computed(() => {
        return this.events()[this.events().length - 1];
    });
    /*readonly eventsGrouped = computed(() => {
        const result: EventsGrouped = {};
        this.events()?.forEach((e) => {
            const group = this.getTimeGroup(e);
            if (result[group] == null) {
                result[group] = [];
            }
            result[group].push(e);
            result[group].sort((a, b) => (a.timestamp < b.timestamp ? 1 : -1));
        });

        const keys = (Object.keys(result) as TimeGroups[]).sort((a, b) =>
            TimeGroupsSort.indexOf(a) > TimeGroupsSort.indexOf(b) ? 1 : -1,
        );
        const sorted: {
            key: TimeGroups;
            events: UserEvent[];
        }[] = [];
        for (let key of keys) {
            sorted.push({
                key: key,
                events: result[key],
            });
        }
        return sorted;
    });
    getTimeGroup(e: UserEvent) {
        const today = new Date();
        const yesterday = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 1);
        const last7Days = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 7);
        const last30Days = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 30);
        const eventTime = new Date(e.timestamp);
        if (
            eventTime.getFullYear() === today.getFullYear() &&
            eventTime.getMonth() === today.getMonth() &&
            eventTime.getDate() === today.getDate()
        ) {
            return TimeGroups.Today;
        } else if (
            eventTime.getFullYear() === yesterday.getFullYear() &&
            eventTime.getMonth() === yesterday.getMonth() &&
            eventTime.getDate() === yesterday.getDate()
        ) {
            return TimeGroups.Yesterday;
        } else if (eventTime.getTime() > last7Days.getTime()) {
            return TimeGroups.Last7Days;
        } else if (eventTime.getTime() > last30Days.getTime()) {
            return TimeGroups.Last30Days;
        }
        return TimeGroups.Older;
    }
     */
}
