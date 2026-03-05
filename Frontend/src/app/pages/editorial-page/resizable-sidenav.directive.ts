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
import { SessionStorageService } from 'ngx-edu-sharing-api';

@Directive({
    selector: '[esResizableSidenav]',
})
export class ResizableSidenavDirective implements OnInit, OnDestroy {
    @Input() storageKey: string;
    @Input() position: 'start' | 'end' = 'start';
    @Input() minWidth = 0.2;
    /**
     * default width (is calculated based on the full screen with [0...1]
     */
    @Input() defaultWidth = 0.3;
    @Input() maxWidth = 0.7;
    private resizer!: HTMLElement;
    private dragging = false;

    constructor(
        private el: ElementRef,
        private renderer: Renderer2,
        private storage: SessionStorageService,
        @Optional() private sidenavContainer: MatSidenavContainer, // inject container
    ) {}

    async ngOnInit(): Promise<void> {
        this.addResizer();
        await this.setInitialWidth();
    }

    private async setInitialWidth() {
        const defaultWidth = window.innerWidth * this.defaultWidth;
        if (this.storageKey) {
            const lastValue = this.applyWidthConstrains(
                await this.storage.get<number>(this.storageKey, defaultWidth),
            );
            this.renderer.setStyle(this.el.nativeElement, 'width', `${lastValue}px`);
            this.sidenavContainer.updateContentMargins();
        } else {
            this.renderer.setStyle(this.el.nativeElement, 'width', `${defaultWidth}px`);
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
        this.renderer.appendChild(this.el.nativeElement, this.resizer);

        this.resizer.addEventListener('mousedown', this.startResize);
        document.addEventListener('mousemove', this.onMouseMove);
        document.addEventListener('mouseup', this.stopResize);
        this.resizer.addEventListener('dblclick', this.resetToDefault);
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
        this.renderer.setStyle(this.el.nativeElement, 'width', `${newWidth}px`);
        if (this.storageKey) {
            void this.storage.set(this.storageKey, newWidth);
        }
    };

    private applyWidthConstrains(newWidth: number) {
        return Math.max(
            this.minWidth * window.innerWidth,
            Math.min(newWidth, this.maxWidth * window.innerWidth),
        );
    }

    private stopResize = () => {
        if (this.dragging && this.sidenavContainer) {
            this.sidenavContainer.updateContentMargins();
        }
        this.dragging = false;
    };

    private resetToDefault = async () => {
        void this.storage.delete(this.storageKey);
        const defaultWidth = window.innerWidth * this.defaultWidth;
        this.renderer.setStyle(this.el.nativeElement, 'width', `${defaultWidth}px`);
        this.sidenavContainer.updateContentMargins();
    };
}
