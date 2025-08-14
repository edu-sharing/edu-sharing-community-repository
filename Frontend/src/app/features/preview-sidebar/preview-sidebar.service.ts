import { AfterViewInit, Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { PreviewSidebarComponent } from './preview-sidebar.component';

/**
 * Sidebar component that previews an element with preview image and some metadata.
 *
 * The sidebar will collapse and display as overlay dialog instead on small screens.
 */
@Injectable({
    providedIn: 'root',
})
export class PreviewSidebarService {
    private instance$ = new BehaviorSubject<PreviewSidebarComponent>(null);

    registerInstance(instance: PreviewSidebarComponent) {
        if (this.instance$.value) {
            console.warn(
                'Multiple sidebar registration detected! Check your DOM',
                this.instance$.value,
                instance,
            );
        }
        this.instance$.next(instance);
    }
    unregisterInstance(instance: PreviewSidebarComponent) {
        if (this.instance$.value !== instance) {
            console.error('This sidebar is not registered!', this.instance$.value, instance);
        } else {
            this.instance$.next(null);
        }
    }

    getInstance() {
        return this.instance$;
    }
}
