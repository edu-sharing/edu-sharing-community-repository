import {
    Directive,
    ElementRef,
    EventEmitter,
    Input,
    NgZone,
    OnDestroy,
    Output,
} from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { CanDrop, DragData, DropTargetState } from '../../types/drag-drop';
import { NodesDragDropService } from '../../services/nodes-drag-drop.service';

const ACTIVE_DROP_TARGET_ACCEPT_CLASS = 'es-nodes-active-drop-target-accept';
const ACTIVE_DROP_TARGET_DENY_CLASS = 'es-nodes-active-drop-target-deny';

@Directive({
    selector: '[esNodesDropTarget]',
    exportAs: 'esNodesDropTarget',
})
export class NodesDropTargetDirective<T = unknown> implements OnDestroy {
    @Input('esNodesDropTarget') target: T;
    @Input() canDropNodes: (dragData: DragData<T>) => CanDrop;
    @Output() nodeDropped = new EventEmitter<DragData<T>>();

    get active() {
        return this.activeDropTargetSubject.value;
    }

    private activeDropTargetSubject = new BehaviorSubject<DropTargetState | null>(null);
    private destroyed = new Subject<void>();

    constructor(
        private ngZone: NgZone,
        private elementRef: ElementRef<HTMLElement>,
        private nodesDragDrop: NodesDragDropService,
    ) {
        this.registerMouseEnterLeave();
        this.registerActiveDropTarget();
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    _setActiveDropTarget(value: DropTargetState | null) {
        this.activeDropTargetSubject.next(value);
    }

    private registerMouseEnterLeave() {
        this.ngZone.runOutsideAngular(() => {
            this.elementRef.nativeElement.addEventListener('mouseenter', () =>
                this.nodesDragDrop.onMouseEnter(this),
            );
            this.elementRef.nativeElement.addEventListener('mouseleave', () =>
                this.nodesDragDrop.onMouseLeave(this),
            );
            // Firefox does not fire a mouseleave event when the element is removed from the DOM
            // while being hovered. When an element is dragged, it will be replaced with a
            // placeholder. If the element was also a drop target, we would think that we are still
            // hovering the element.
            observeRemovedFromParent(this.elementRef.nativeElement)
                .pipe(takeUntil(this.destroyed))
                .subscribe(() => this.nodesDragDrop.onMouseLeave(this));
        });
    }

    private registerActiveDropTarget() {
        this.activeDropTargetSubject
            .pipe(takeUntil(this.destroyed))
            .subscribe((dropTargetState) => {
                const canDrop = dropTargetState?.canDrop;
                const classList = this.elementRef.nativeElement.classList;
                classList.remove(ACTIVE_DROP_TARGET_ACCEPT_CLASS, ACTIVE_DROP_TARGET_DENY_CLASS);
                if (canDrop?.accept) {
                    classList.add(ACTIVE_DROP_TARGET_ACCEPT_CLASS);
                } else if (canDrop?.denyExplicit) {
                    classList.add(ACTIVE_DROP_TARGET_DENY_CLASS);
                }
            });
    }
}

function observeRemovedFromParent(element: HTMLElement): Observable<void> {
    return new Observable((subscriber) => {
        const observer = new MutationObserver((event) => {
            for (const mutation of event) {
                // @ts-ignore
                if ([...mutation.removedNodes].includes(element)) {
                    subscriber.next();
                }
            }
        });
        // Wait for `element` to be attached to the DOM.
        let timeout = setTimeout(() => {
            timeout = null;
            observer.observe(element.parentNode, { childList: true, subtree: false });
        });
        return () => {
            // In case the element was destroyed before we attached the mutation observer, we cancel
            // the observable returned by this function and don't attach the mutation observer.
            if (timeout !== null) {
                // TODO: Investigate elements that trigger the following warning.
                //
                // console.warn(
                //     'Possible performance leak: the element got destroyed before it could be added to the DOM.',
                //     element,
                // );
                clearTimeout(timeout);
            }
            observer.disconnect();
        };
    });
}
