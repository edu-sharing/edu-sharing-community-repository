import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { MdsWidget } from 'ngx-edu-sharing-api';
import { Values } from 'ngx-edu-sharing-ui';

export type EditorialTab = {
    id: string;
    caption?: string;
    icon: string;
};
@Injectable()
export class EditorialPageService {
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
    registerTabsFromWidget(widget: MdsWidget) {
        this.tabWidgetId$.next(widget.id);
        this.tabs$.next(
            widget.values.map((v) => {
                return {
                    id: v.id,
                    caption: v.caption,
                    icon: v.icon,
                };
            }),
        );
    }
}
