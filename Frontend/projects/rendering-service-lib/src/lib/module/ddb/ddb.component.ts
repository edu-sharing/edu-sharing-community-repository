import { AfterViewInit, Component, ElementRef, HostListener, Input, signal } from '@angular/core';
import { RenderingModule } from '../../rendering.module';
import { RenderModule } from '../RenderModule';
import { Node } from 'ngx-edu-sharing-api';
import { MatIcon } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { FormsModule } from '@angular/forms';
import { RenderData } from '../../dto/RenderData';

@Component({
    selector: 'rs-module-ddb',
    imports: [RenderingModule, MatButtonModule, FormsModule, MatIcon],
    templateUrl: './ddb.component.html',
    styleUrl: './ddb.component.scss',
})
export class DdbComponent implements RenderModule, AfterViewInit {
    @Input() data: RenderData | undefined;
    @Input() node: Node | undefined;
    activeObject = signal<string | undefined>(undefined);

    constructor(public element: ElementRef) {}

    ngAfterViewInit(): void {
        this.loadOptimalSize();
    }

    @HostListener('document:fullscreenchange')
    protected loadOptimalSize() {
        const size = this.element.nativeElement.getBoundingClientRect();
        if (this.data?.items !== undefined && this.data?.items[0].additionalData !== undefined) {
            const additionalData = this.data?.items[0].additionalData;
            const keys = Object.keys(additionalData);
            const sizeKeys = keys.filter((key) => key.startsWith('size_'));
            const sizes = sizeKeys.map((key) => additionalData[key]);
            const matchingSize = sizes.sort((a, b) => {
                return Math.abs(parseInt(a.split(',')[0]) - size.width) >
                    Math.abs(parseInt(b.split(',')[0]) - size.width)
                    ? 1
                    : -1;
            })[0];
            const width = matchingSize.split(',')[0];
            const height = matchingSize.split(',')[1];
            const widthPlaceHolder = additionalData['widthPlaceHolder'] ?? '';
            const heightPlaceHolder = additionalData['heightPlaceHolder'] ?? '';
            const linkTemplate = additionalData['linkTemplate'] ?? '';
            const link = linkTemplate
                .replace(widthPlaceHolder, width)
                .replace(heightPlaceHolder, height);
            this.activeObject.set(link);
        }
    }

    toggleFullscreen() {
        if (document.fullscreenElement) {
            void document.exitFullscreen();
        } else {
            this.element.nativeElement.requestFullscreen();
        }
    }
}
