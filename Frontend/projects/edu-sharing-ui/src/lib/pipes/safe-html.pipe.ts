import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import DOMPurify from 'dompurify';

@Pipe({
    name: 'safeHtml',
    standalone: false,
})
export class SafeHtmlPipe implements PipeTransform {
    constructor(protected sanitizer: DomSanitizer) {}

    static readonly DEFAULT_ALLOWED_TAGS = ['p', 'br', 'div', 'a', 'span', 'strong', 'em'];

    /**
     * @param args.purify sanitize the html via DOMPurify before trusting it
     * @param args.allowedTags tags kept when purifying, defaults to `DEFAULT_ALLOWED_TAGS`
     */
    transform(
        value: any,
        args: { purify: boolean; allowedTags?: string[] } = { purify: false },
    ): SafeHtml {
        if (args?.purify) {
            value = DOMPurify.sanitize(value, {
                ALLOWED_TAGS: args.allowedTags ?? SafeHtmlPipe.DEFAULT_ALLOWED_TAGS,
                ALLOWED_ATTR: ['href', 'target'],
            });
        }
        return this.sanitizer.bypassSecurityTrustHtml(value);
    }
}
