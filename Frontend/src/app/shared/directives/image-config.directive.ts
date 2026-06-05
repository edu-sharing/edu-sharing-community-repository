/**
 * Created by Torsten on 13.01.2017.
 */

import { Directive, ElementRef, inject } from '@angular/core';
import { ConfigurationService } from '../../core-module/core.module';

@Directive({
    selector: '[esImageConfig]',
    standalone: false,
})
export class ImageConfigDirective {
    private config = inject(ConfigurationService);

    private element: ElementRef;
    constructor() {
        const element = inject(ElementRef);

        this.element = element;
        this.replace();
    }
    replace() {
        if (this.element.nativeElement && this.element.nativeElement.src) {
            this.config
                .get('images', [])
                .subscribe((images: { src: string; replace: string }[]) => {
                    for (const img of images) {
                        // src contains the absolute url, so we need to verify via endsWith
                        if (this.element.nativeElement.src.endsWith(img.src)) {
                            this.element.nativeElement.src = img.replace;
                        }
                    }
                });
        } else {
            setTimeout(() => this.replace(), 16);
        }
    }
}
