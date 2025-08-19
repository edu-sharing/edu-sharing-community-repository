import { Injectable } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
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
    private currentNode$ = new BehaviorSubject<Node | null>(null);
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
        // the currentNode might already be set as input
        this.currentNode$.next(instance.node);
        // listen to the closed event
        this.instance$.value.closed.subscribe(() => {
            this.currentNode$.next(null);
        });
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

    getCurrentNode() {
        return this.currentNode$;
    }

    handleNodeClick(node: Node) {
        if (!this.instance$.value) {
            return;
        }
        this.currentNode$.next(this.instance$.value.node !== node ? node : null);
        this.instance$.value.node = this.currentNode$.value;
    }
}
