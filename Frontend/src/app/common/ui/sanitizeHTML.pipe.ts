import {Pipe, PipeTransform, SecurityContext} from '@angular/core';
import {DomSanitizer} from '@angular/platform-browser';

/**
 * Replace HTML characters by safe encoded values for usage inside innerHTML
 */
@Pipe({
    name: 'sanitizeHTML',
})
export class SanitizeHTMLPipe implements PipeTransform {
    constructor() {
    }
    transform(value: string): string {
        return value.replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }
}
