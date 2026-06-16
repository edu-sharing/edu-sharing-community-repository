import { AfterViewInit, Component, ElementRef, HostListener, Input, signal } from '@angular/core';
import { RenderingModule } from '../../rendering.module';
import { RenderModule } from '../RenderModule';
import { Node } from 'ngx-edu-sharing-api';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RenderData, AssetStateItem } from '../../dto/RenderData';

@Component({
    selector: 'rs-module-image',
    imports: [RenderingModule, MatButtonModule, MatIconModule],
    templateUrl: './image.component.html',
    styleUrl: './image.component.scss',
})
export class ImageComponent implements RenderModule, AfterViewInit {
    @Input() data: RenderData | undefined;
    @Input() node: Node | undefined;
    activeObject = signal<AssetStateItem | undefined>(undefined);
    constructor(public element: ElementRef) {}
    ngAfterViewInit(): void {
        this.loadOptimalSize();
    }

    @HostListener('document:fullscreenchange')
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
