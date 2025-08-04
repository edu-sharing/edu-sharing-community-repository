import { Injectable, NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { EditorialPageComponent } from './editorial-page.component';
import { BehaviorSubject } from 'rxjs';
import { MdsWidget } from 'ngx-edu-sharing-api';

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
