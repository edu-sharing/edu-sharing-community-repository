import { Injectable, signal } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { MdsWidget, UserEvent } from 'ngx-edu-sharing-api';
import { NodeEntriesDataType, NodeEntriesDisplayType, Values } from 'ngx-edu-sharing-ui';
import { PrimaryMode } from '../../features/editorial-sidebar/editorial-sidebar.component';

export const RECENT_ACTIVITY_EVENT_TYPES: Record<string, NonNullable<UserEvent['eventType']>[]> = {
    viewed: [
        'VIEW_MATERIAL',
        'VIEW_COLLECTION',
        'VIEW_MATERIAL_EMBEDDED',
        'VIEW_MATERIAL_PLAY_MEDIA',
        'OPEN_EXTERNAL_LINK',
    ],
    created: ['CREATE_MATERIAL', 'EDIT_MATERIAL_RELATION'],
    edited: [
        'EDIT_MATERIAL',
        'MOVE_MATERIAL',
        'COPY_MATERIAL',
        'ARCHIVE_MATERIAL',
        'DELETE_MATERIAL',
    ],
};

export type EditorialTab = {
    id: string;
    caption?: string;
    icon: string;
};
export type CloseConfig = {
    show: boolean;
    callback?: () => void;
};
@Injectable()
export class EditorialPageService {
    readonly displayType = signal(NodeEntriesDisplayType.Table);
    /**
     * info for close button state (next to breadcrumbs)
     */
    readonly close = signal<CloseConfig>(null);
    private virtualNodes$ = new BehaviorSubject<{ [key: string]: NodeEntriesDataType[] }>({});
    private tabs$ = new BehaviorSubject<EditorialTab[]>(null);
    private tabWidgetId$ = new BehaviorSubject<string>(null);

    buildSearchCriteria(tab: number) {
        if (this.tabWidgetId$.value) {
            return { [this.tabWidgetId$.value]: [this.tabs$.value[tab].id] };
        }
        return {};
    }

    resolveTabForCriteria(criteria: Values) {
        if (this.tabWidgetId$.value) {
            const tab = this.tabs$.value.findIndex(
                (t) => t.id === criteria[this.tabWidgetId$.value]?.[0],
            );
            delete criteria[this.tabWidgetId$.value];
            return tab === -1 ? 0 : tab;
        }
        return 0;
    }

    observeTabs() {
        return this.tabs$.asObservable();
    }

    registerTabs(tab: EditorialTab[]) {
        this.tabs$.next(tab);
    }
    registerTabsFromWidget(widget: MdsWidget): EditorialTab[] {
        this.tabs$.next(this.mapWidgetToTabs(widget));
        return this.tabs$.value;
    }
    mapWidgetToTabs(widget: MdsWidget) {
        this.tabWidgetId$.next(widget.id);
        return widget.values.map((v) => {
            return {
                id: v.id,
                caption: v.caption,
                icon: v.icon,
            };
        });
    }
    getVirtualNodes(mode: PrimaryMode) {
        return this.virtualNodes$.value[mode];
    }
    addVirtualNodes(nodes: NodeEntriesDataType[], mode: PrimaryMode) {
        if (!this.virtualNodes$.value[mode]) {
            this.virtualNodes$.value[mode] = [];
        }
        this.virtualNodes$.value[mode] = [...this.virtualNodes$.value[mode], ...nodes];
        this.virtualNodes$.next(this.virtualNodes$.value);
    }
}
