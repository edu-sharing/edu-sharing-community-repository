import { Pipe, PipeTransform, SecurityContext, inject } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { marked, MarkedOptions } from 'marked';

@Pipe({
    name: 'markdown',
    standalone: true,
})
export class MarkdownPipe implements PipeTransform {
    private sanitizer = inject(DomSanitizer);

    // note: disabling the following options seems not to have any effect,
    //       so keep them to be sure.
    // * br
    // * checkbox
    // * space
    // note: essential properties to render text blocks:
    // * paragraph
    // * text
    private readonly blacklistedMarkdownFunctions: string[] = [
        // avoid including HTML directly
        'html',
        // avoid including Markdown tables
        'tablecell',
        'tablerow',
    ];
    md: typeof marked;

    constructor() {
        const renderer = new marked.Renderer();
        // handle custom renderings
        // parse heading as plain text (forEach due to link breaks)
        renderer.heading = ({ tokens }) => {
            // <div> is necessary to properly break lines similar to heading elements
            let result = '<div>';
            tokens.forEach((token) => {
                if ('text' in token) {
                    result += token.text;
                } else {
                    result += token.raw;
                }
            });
            result += '</div>';
            return result;
        };
        // parse link as link opening in a separate tab
        renderer.link = ({ href, text }) => `<a href="${href}" target="_blank">${text}</a>`;
        // parse image as link
        renderer.image = ({ href, text }) => `<a href="${href}" target="_blank">${text}</a>`;
        // parse table as plain text
        renderer.table = (token) => {
            return token.raw;
        };

        // handle blacklisted functions
        this.blacklistedMarkdownFunctions.forEach((functionName: string): void => {
            // @ts-ignore
            renderer[functionName] = () => '';
        });

        // gfm: use approved GitHub flavored Markdown specification
        const options: MarkedOptions = {
            gfm: true,
            renderer: renderer,
        };
        this.md = marked.setOptions(options);
    }

    /**
     * Converts a given Markdown string into an HTML string.
     *
     * @param value
     */
    transform(value: string): string {
        return this.sanitizer.sanitize(SecurityContext.HTML, this.md.parse(value));
    }
}
