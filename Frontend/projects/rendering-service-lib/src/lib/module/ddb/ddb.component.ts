import {
    AfterViewInit,
    Component,
    ElementRef,
    HostListener,
    Input,
    signal,
    inject,
    ChangeDetectionStrategy,
} from '@angular/core';
import { RenderingModule } from '../../rendering.module';
import { RenderModule } from '../RenderModule';
import { Node } from 'ngx-edu-sharing-api';
import { MatIcon } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { FormsModule } from '@angular/forms';
import { RenderData } from '../../dto/RenderData';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'rs-module-ddb',
    imports: [RenderingModule, MatButtonModule, FormsModule, MatIcon, TranslatePipe],
    templateUrl: './ddb.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrl: './ddb.component.scss',
})
export class DdbComponent implements RenderModule, AfterViewInit {
    element = inject(ElementRef);

    @Input() data: RenderData | undefined;
    @Input() node: Node | undefined;
    activeObject = signal<string | undefined>(undefined);
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
