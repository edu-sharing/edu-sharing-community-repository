import { Pipe, PipeTransform } from '@angular/core';

/**
 * Replaces HTML characters by encoded values for usage inside innerHTML.
 *
 * Use this pipe in combination with other pipes that introduce markup into text strings, e.g.,
 * ```html
 * <span [innerHTML]="value | escapeHtml | highlight: substring"></span>
 * ```
 * This will render `value` as it would have been when used in normal text interpolation like
 * `<span>{{ value }}</span>`, but allows us to use markup introduced by the `highlight` pipe.
 *
 * With current Angular versions, sanitizing input of `innerHTML` (as long as bound with an Angular
 * template) is actually not needed for security anymore since Angular sanitizes unsafe string input
 * automatically.
 *
 * This pipe still has two purposes:
 * - It escapes HTML characters, not for security but so they can be used in text.
 * - It removes control characters, which would be rendered visible through Angular's escaping of
 *   `innerHTML`.
 */
@Pipe({
    name: 'escapeHtml',
})
export class EscapeHtmlPipe implements PipeTransform {
    transform(value: string): string {
        return (
            value
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#039;')
                // Remove control characters
                .replace(/[\u0000-\u001F\u007F-\u009F]/g, '')
        );
    }
}
