import { Injectable, signal } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Node } from 'ngx-edu-sharing-api';
import { CanDrop, DragData, DropSource } from 'ngx-edu-sharing-ui';

/**
 * Drop support for breadcrumbs that are not rendered by the page itself (e.g. global/custom
 * breadcrumbs of an extension). Mirrors the `canDropNodes` / `dropElement` bindings of
 * `es-breadcrumbs`.
 */
export interface BreadcrumbsDropHandler {
    canDropNodes: (dragData: DragData<Node | 'HOME'>) => CanDrop;
    dropElement: (target: Node | 'HOME', source: DropSource<Node>) => void;
}

@Injectable({
    providedIn: 'root',
})
export class BreadcrumbsService {
    breadcrumbs$ = new BehaviorSubject<Node[]>([]);
    /**
     * Drop handler of the currently active page, or `null` if dropping on the breadcrumbs is not
     * supported in the current context.
     */
    readonly dropHandler = signal<BreadcrumbsDropHandler | null>(null);

    setNodePath(path: Node[]) {
        this.breadcrumbs$.next(path);
    }

    setDropHandler(handler: BreadcrumbsDropHandler) {
        this.dropHandler.set(handler);
    }

    /**
     * Removes the given handler, unless another one was registered in the meantime.
     */
    clearDropHandler(handler: BreadcrumbsDropHandler) {
        if (this.dropHandler() === handler) {
            this.dropHandler.set(null);
        }
    }
}
