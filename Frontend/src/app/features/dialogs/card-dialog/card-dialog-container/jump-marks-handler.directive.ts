import {
    Directive,
    ElementRef,
    EventEmitter,
    Input,
    NgZone,
    OnDestroy,
    Output,
} from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { UIService } from '../../../../core-module/core.module';
import { JumpMark, JumpMarksService } from '../../../../services/jump-marks.service';

/**
 * Container that contains jump-mark anchors.
 */
@Directive({
    selector: '[esJumpMarksHandler]',
    exportAs: 'esJumpMarksHandler',
})
export class JumpMarksHandlerDirective implements OnDestroy {
    private jumpMarksSubject = new BehaviorSubject<JumpMark[]>(null);
    @Input('esJumpMarksHandler')
    get jumpMarks(): JumpMark[] {
        return this.jumpMarksSubject.value;
    }
    set jumpMarks(value: JumpMark[]) {
        this.jumpMarksSubject.next(value);
    }

    @Output() activeJumpMarkChanged = new EventEmitter<JumpMark | null>();

    private shouldUpdateJumpMarkOnScroll = true;
    private activeJumpMarkSubject = new BehaviorSubject<JumpMark | null>(null);
    private destroyed = new Subject<void>();

    constructor(
        private elementRef: ElementRef<HTMLElement>,
        private uiService: UIService,
        private ngZone: NgZone,
        private jumpMarksService: JumpMarksService,
    ) {
        this.activeJumpMarkSubject.subscribe(this.activeJumpMarkChanged);
        this.registerUpdateActiveJumpMark();
        this.registerJumpMarksService();
    }

    private registerJumpMarksService() {
        this.jumpMarksService.triggerScrollToJumpMark
            .pipe(takeUntil(this.destroyed))
            .subscribe((jumpMark) => {
                if (typeof jumpMark === 'string') {
                    jumpMark = this.jumpMarks.find((j) => j.id === jumpMark);
                }
                void this.scrollToJumpMark(jumpMark);
            });
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    async scrollToJumpMark(jumpMark: JumpMark): Promise<void> {
        this.jumpMarksService.beforeScrollToJumpMark.next(jumpMark);
        this.activeJumpMarkSubject.next(jumpMark);
        this.shouldUpdateJumpMarkOnScroll = false;
        const headingElement = document.getElementById(jumpMark.id);
        // Since our heading elements are sticky, we cannot scroll those into view. Instead, we
        // require the heading to be the first child of its parent and scroll the parent into view.
        const top = headingElement.parentElement.offsetTop;
        await this.uiService.scrollSmoothElement(top, this.elementRef.nativeElement, 0.5);
        // Leave a little time for the last scroll event to propagate before enabling updates
        // again.
        this.ngZone.runOutsideAngular(() =>
            window.setTimeout(() => (this.shouldUpdateJumpMarkOnScroll = true), 20),
        );
    }

    private registerUpdateActiveJumpMark(): void {
        const updateActiveJumpMark = () => {
            if (this.shouldUpdateJumpMarkOnScroll) {
                const activeJumpMark = this.getActiveJumpMark();
                if (activeJumpMark !== this.activeJumpMarkSubject.value) {
                    this.ngZone.run(() => this.activeJumpMarkSubject.next(activeJumpMark));
                }
            }
        };
        this.jumpMarksSubject.subscribe(() => updateActiveJumpMark());
        this.ngZone.runOutsideAngular(() => {
            this.elementRef.nativeElement.addEventListener('scroll', updateActiveJumpMark);
        });
        this.destroyed.subscribe(() =>
            this.elementRef.nativeElement.removeEventListener('scroll', updateActiveJumpMark),
        );
    }

    private getActiveJumpMark(): JumpMark | null {
        // The heading currently sticking to the top of the card is always active.
        const cardTop = this.elementRef.nativeElement.getBoundingClientRect().top;
        for (const jumpMark of this.jumpMarks ?? []) {
            const headingElement = document.getElementById(jumpMark.id);
            const sectionTop = headingElement.getBoundingClientRect().top;
            if (sectionTop >= cardTop) {
                return jumpMark;
            }
        }
        return null;
    }
}
