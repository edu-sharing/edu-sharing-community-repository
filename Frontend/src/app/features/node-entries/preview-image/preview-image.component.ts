import { Component, ElementRef, Input, ViewChild } from '@angular/core';
import { Node } from '../../../core-module/core.module';

/**
 * Displays the preview image of `node`.
 *
 * When `node` is a video, the animated preview image is replaced with a static canvas unless
 * `playAnimation` is set to `true`.
 */
@Component({
    selector: 'es-preview-image',
    templateUrl: './preview-image.component.html',
    styleUrls: ['./preview-image.component.scss'],
})
export class PreviewImageComponent<T extends Node> {
    @Input() node: T;
    @Input() playAnimation = false;

    @ViewChild('image') imageRef: ElementRef<HTMLImageElement>;
    @ViewChild('canvas') canvasRef: ElementRef<HTMLCanvasElement>;
    // @ViewChild('backdropCanvas') backdropCanvasRef: ElementRef<HTMLCanvasElement>;

    showCanvas: boolean = false;
    replacedWithStatic: boolean = false;

    constructor() {}

    onImageLoad(event: Event): void {
        if (this.node.mimetype?.startsWith('video')) {
            const image = event.target as HTMLImageElement;
            this.showCanvas = true;
            setTimeout(() => {
                this.initCanvas(image, this.canvasRef.nativeElement);
                // this.initCanvas(image, this.backdropCanvasRef.nativeElement);
                this.replacedWithStatic = true;
            });
        }
    }

    private initCanvas(image: HTMLImageElement, canvas: HTMLCanvasElement): void {
        var width = image.naturalWidth;
        var height = image.naturalHeight;
        canvas.width = width;
        canvas.height = height;
        canvas.getContext('2d').drawImage(image, 0, 0, width, height);
    }
}
