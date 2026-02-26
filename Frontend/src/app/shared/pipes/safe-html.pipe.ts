import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import DOMPurify from 'dompurify';

@Pipe({
    name: 'safeHtml',
    standalone: false,
})
export class SafeHtmlPipe implements PipeTransform {
    constructor(protected sanitizer: DomSanitizer) {}

    transform(value: any, args: { purify: boolean } = { purify: false }): SafeHtml {
        if (args?.purify) {
            value = DOMPurify.sanitize(value, {
                ALLOWED_TAGS: ['p', 'br', 'div', 'a', 'span', 'strong', 'em'],
                ALLOWED_ATTR: ['style', 'href'],
            });
        }
        return this.sanitizer.bypassSecurityTrustHtml(value);
    }
}
