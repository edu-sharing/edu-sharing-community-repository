import {Directive, HostListener, Inject, Input, NgZone, OnDestroy, OnInit} from '@angular/core';
import {CdkDrag, CdkDropList} from '@angular/cdk/drag-drop';
import {Subject} from 'rxjs';
import {takeUntil} from 'rxjs/operators';
import {WindowRefService} from '../../modules/search/window-ref.service';
import {DropAction} from './drag-nodes/drag-nodes';
import {Node} from '../../core-module/rest/data-object';

export type DragDropState<T extends Node> = {
    element: T,
    dropAllowed: boolean,
    mode?: DropAction,
};
@Directive({
    selector: '[appDragCursor]',
})
/**
 * directive to control the drag/drop behaviour based on the current state
 * Must be used in cominbation with cdkDrag
 */
export class DragCursorDirective implements OnInit, OnDestroy {
    private unsubscribe$: Subject<void> = new Subject();
    private dragging = false;
    @Input() dragState: DragDropState<Node>;
    private interval: number;
    constructor(
        private cdkDrag: CdkDrag,
        private ngZone: NgZone,
    ) {}

    keyDownListener = (e: KeyboardEvent) => {
        if (e.key === 'Control' || e.keyCode == 91 || e.keyCode == 93) {
            this.dragState.mode = 'copy';
            this.updateCursor();
        }
    }
    keyUpListener = (e: KeyboardEvent) => {
        if (e.key === 'Control' || e.keyCode == 91 || e.keyCode == 93) {
            this.dragState.mode = 'move';
            this.updateCursor();
        }
    }

    public ngOnInit(): void {
        this.cdkDrag.started.pipe(takeUntil(this.unsubscribe$)).subscribe(() => {
            this.dragging = true;
            this.ngZone.runOutsideAngular(() => {
                document.addEventListener('keydown', this.keyDownListener);
                document.addEventListener('keyup', this.keyUpListener);
                this.updateCursor();
                this.interval = setInterval(() => this.updateCursor(), 1000 / 60);
            });
        });

        this.cdkDrag.ended.pipe(takeUntil(this.unsubscribe$)).subscribe(() => {
            this.dragging = false;
            this.ngZone.runOutsideAngular(() => {
                document.removeEventListener('keydown', this.keyDownListener);
                document.removeEventListener('keyup', this.keyUpListener);
                this.updateCursor();
                clearInterval(this.interval);
            });
        });
    }

    public ngOnDestroy(): void {
        this.unsubscribe$.next();
        this.unsubscribe$.complete();
    }

    private updateCursor() {
        if (this.dragging) {
            document.body.style.cursor = this.dragState?.dropAllowed ?
                this.dragState?.element ?
                    this.dragState?.mode === 'copy' ? 'copy' : 'default'
                    : null
                : 'no-drop';
        } else {
            document.body.style.cursor = null;
        }
    }
}
