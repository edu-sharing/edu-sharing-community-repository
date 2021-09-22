import {Directive, HostListener, Inject, OnDestroy, OnInit} from '@angular/core';
import {CdkDrag, CdkDropList} from '@angular/cdk/drag-drop';
import {Subject} from 'rxjs';
import {takeUntil} from 'rxjs/operators';
import {WindowRefService} from '../../modules/search/window-ref.service';
import {DropAction} from './drag-nodes/drag-nodes';

@Directive({
    selector: '[appDragCursor]',
})
/**
 * directive to control the drag/drop behaviour based on the current state
 * Must be used in cominbation with cdkDrag
 */
export class DragCursorDirective implements OnInit, OnDestroy {
    private unsubscribe$: Subject<void> = new Subject();
    private mode: DropAction = 'move';
    private dragging = false
    constructor( private cdkDrag: CdkDrag) {}

    @HostListener('document:keydown', ['$event'])
    onKeyDown(e: KeyboardEvent) {
        if(!e.repeat && e.key === 'Control') {
            this.mode = 'copy';
            this.updateCursor();
        }
    }
    @HostListener('document:keyup', ['$event'])
    onKeyUp(e: KeyboardEvent) {
        if(e.key === 'Control') {
            this.mode = 'move';
            this.updateCursor();
        }
    }

    public ngOnInit(): void {
        this.cdkDrag.started.pipe(takeUntil(this.unsubscribe$)).subscribe(() => {
            this.dragging = true;
            this.updateCursor();
        });

        this.cdkDrag.ended.pipe(takeUntil(this.unsubscribe$)).subscribe(() => {
            this.dragging = false;
            document.body.style.cursor = null;
        });
    }

    public ngOnDestroy(): void {
        this.unsubscribe$.next();
        this.unsubscribe$.complete();
    }

    private updateCursor() {
        if(this.dragging) {
            document.body.style.cursor = this.mode === 'copy' ? 'copy' : 'move';
        }
    }
}
