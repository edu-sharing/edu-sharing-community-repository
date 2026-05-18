import {
    Directive,
    ElementRef,
    Input,
    OnDestroy,
    OnInit,
    Optional,
    Renderer2,
} from '@angular/core';
import { MatSidenavContainer } from '@angular/material/sidenav';
import { SessionStorageService, Store } from 'ngx-edu-sharing-api';
import { BehaviorSubject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';

@Directive({
    selector: '[esResizableSidenav]',
})
export class ResizableSidenavDirective implements OnInit, OnDestroy {
    @Input() storageKey: string;
    @Input() position: 'start' | 'end' = 'start';
    @Input() minWidth = 0.2;
    @Input() minWidthPx = 400;
    /**
     * default width (is calculated based on the full screen with [0...1]
     */
    @Input() defaultWidth = 0.3;
    @Input() maxWidth = 0.5;
    @Input() maxWidthPx = 800;
    private resizer!: HTMLElement;
    private dragging = false;
    private width$ = new BehaviorSubject<number>(0);

    constructor(
        private el: ElementRef,
        private renderer: Renderer2,
        private storage: SessionStorageService,
        @Optional() private sidenavContainer: MatSidenavContainer, // inject container
    ) {
        this.width$.pipe(debounceTime(10)).subscribe((width) => {
            if (this.storageKey) {
                void this.storage.set(this.storageKey, width, Store.LocalStorage);
            }
        });
    }

    async ngOnInit(): Promise<void> {
        this.addResizer();
        await this.setInitialWidth();
    }

    private async setInitialWidth() {
        const defaultWidth = window.innerWidth * this.defaultWidth;
        if (this.storageKey) {
            const lastValue = this.applyWidthConstrains(
                await this.storage.get<number>(this.storageKey, defaultWidth, Store.LocalStorage),
            );
            this.renderer.setStyle(this.el.nativeElement, 'width', `${lastValue}px`);
            this.renderer.setAttribute(
                this.resizer,
                'aria-valuenow',
                String(Math.round(lastValue)),
            );
            this.sidenavContainer.updateContentMargins();
        } else {
            this.renderer.setStyle(this.el.nativeElement, 'width', `${defaultWidth}px`);
            this.renderer.setAttribute(
                this.resizer,
                'aria-valuenow',
                String(Math.round(defaultWidth)),
            );
            this.sidenavContainer.updateContentMargins();
        }
    }

    ngOnDestroy(): void {
        this.resizer.remove();
    }

    private addResizer() {
        this.resizer = this.renderer.createElement('div');
        this.renderer.addClass(this.resizer, 'es-sidenav-resizer');
        this.renderer.addClass(this.resizer, this.position);
        this.renderer.setAttribute(this.resizer, 'tabindex', '0');
        this.renderer.setAttribute(this.resizer, 'role', 'separator');
        this.renderer.setAttribute(this.resizer, 'aria-orientation', 'vertical');
        const calculatedMin = Math.round(
            Math.max(this.minWidthPx, window.innerWidth * this.minWidth),
        );
        this.renderer.setAttribute(this.resizer, 'aria-valuemin', String(calculatedMin));
        const calculatedMax = Math.round(
            Math.min(this.maxWidthPx, window.innerWidth * this.maxWidth),
        );
        this.renderer.setAttribute(this.resizer, 'aria-valuemax', String(calculatedMax));
        this.renderer.insertBefore(
            this.el.nativeElement,
            this.resizer,
            this.el.nativeElement.firstChild,
        );

        this.resizer.addEventListener('mousedown', this.startResize);
        document.addEventListener('mousemove', this.onMouseMove);
        document.addEventListener('mouseup', this.stopResize);
        this.resizer.addEventListener('dblclick', this.resetToDefault);
        this.resizer.addEventListener('keydown', this.onKeyDown);
    }

    private startResize = (event: MouseEvent) => {
        this.dragging = true;
        event.preventDefault();
        event.stopPropagation();
    };

    private onMouseMove = (event: MouseEvent) => {
        if (!this.dragging) return;

        const containerWidth = window.innerWidth;

        let newWidth;
        if (this.position === 'start') {
            newWidth = event.clientX;
        } else {
            newWidth = containerWidth - event.clientX;
        }

        newWidth = this.applyWidthConstrains(newWidth);
        this.width$.next(newWidth);
        this.renderer.setStyle(this.el.nativeElement, 'width', `${newWidth}px`);
    };

    private applyWidthConstrains(newWidth: number) {
        return Math.max(
            this.minWidthPx,
            this.minWidth * window.innerWidth,
            Math.min(newWidth, this.maxWidth * window.innerWidth, this.maxWidthPx),
        );
    }

    private stopResize = () => {
        if (this.dragging && this.sidenavContainer) {
            this.sidenavContainer.updateContentMargins();
        }
        this.dragging = false;
    };

    private onKeyDown = (event: KeyboardEvent) => {
        const currentWidth = this.el.nativeElement.offsetWidth as number;
        const step = event.shiftKey ? 50 : 10;
        const growKey = this.position === 'start' ? 'ArrowRight' : 'ArrowLeft';
        const shrinkKey = this.position === 'start' ? 'ArrowLeft' : 'ArrowRight';

        let newWidth: number | null = null;
        if (event.key === growKey) {
            newWidth = currentWidth + step;
        } else if (event.key === shrinkKey) {
            newWidth = currentWidth - step;
        } else if (event.key === 'Home') {
            newWidth = this.applyWidthConstrains(0);
        } else if (event.key === 'End') {
            newWidth = this.applyWidthConstrains(Infinity);
        } else if (event.key === 'Enter') {
            void this.resetToDefault();
            return;
        } else {
            return;
        }

        event.preventDefault();
        newWidth = this.applyWidthConstrains(newWidth);
        this.width$.next(newWidth);
        this.renderer.setStyle(this.el.nativeElement, 'width', `${newWidth}px`);
        this.sidenavContainer?.updateContentMargins();
        this.renderer.setAttribute(this.resizer, 'aria-valuenow', String(Math.round(newWidth)));
    };

    private resetToDefault = async () => {
        void this.storage.delete(this.storageKey, Store.LocalStorage);
        const defaultWidth = window.innerWidth * this.defaultWidth;
        this.renderer.setStyle(this.el.nativeElement, 'width', `${defaultWidth}px`);
        this.sidenavContainer.updateContentMargins();
    };
}
