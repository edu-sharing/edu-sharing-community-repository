import {
    AfterViewInit,
    Component,
    ElementRef,
    HostListener,
    Input,
    signal,
    inject,
} from '@angular/core';
import { RenderingModule } from '../../rendering.module';
import { RenderModule } from '../RenderModule';
import { Node } from 'ngx-edu-sharing-api';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RenderData, AssetStateItem } from '../../dto/RenderData';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'rs-module-image',
    imports: [RenderingModule, MatButtonModule, MatIconModule, TranslateModule],
    templateUrl: './image.component.html',
    styleUrl: './image.component.scss',
})
export class ImageComponent implements RenderModule, AfterViewInit {
    element = inject(ElementRef);

    @Input() data: RenderData | undefined;
    @Input() node: Node | undefined;
    activeObject = signal<AssetStateItem | undefined>(undefined);
    /** Whether this module's host element is currently shown in fullscreen. */
    isFullscreen = signal(false);
    ngAfterViewInit(): void {
        this.loadOptimalSize();
    }

    /**
     * Tracks the fullscreen state on every change, including exits via Esc or browser controls,
     * so the toggle button can expose the matching name and icon.
     */
    @HostListener('document:fullscreenchange')
    protected onFullscreenChange() {
        this.isFullscreen.set(document.fullscreenElement === this.element.nativeElement);
        this.loadOptimalSize();
    }

    protected loadOptimalSize() {
        const size = this.element.nativeElement.getBoundingClientRect();
        const allFinishedItems = this.data?.items?.filter((item) => item.link !== '');
        const matchingItem = allFinishedItems?.sort((a, b) => {
            // sort by the image size closest to the viewport
            return Math.abs(a.width - size.width) > Math.abs(b.width - size.width) ? 1 : -1;
        })[0];
        this.activeObject.set(matchingItem);
    }

    toggleFullscreen() {
        if (document.fullscreenElement) {
            void document.exitFullscreen();
        } else {
            this.element.nativeElement.requestFullscreen();
        }
    }
}
