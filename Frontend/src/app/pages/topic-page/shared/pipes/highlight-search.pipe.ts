import { SecurityContext, Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';

@Pipe({
    name: 'highlightSearch',
})
export class HighlightSearchPipe implements PipeTransform {
    constructor(private sanitizer: DomSanitizer) {}

    transform(content: string, query: string): string {
        // return, if no content or search query is provided
        if (content == null || query == null || query.trim() === '') {
            return content ?? '';
        }

        // convert SafeHtml into string, otherwise [object Object] might be returned
        const html = this.sanitizer.sanitize(SecurityContext.HTML, content as any) ?? '';

        if (html === '') {
            return '';
        }

        // normalize query (simple case: one search phrase)
        const q = query.trim();
        const pattern = this.escapeRegExp(q);
        const regex = new RegExp(pattern, 'gi');

        // parse the HTML and only touch text nodes
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, 'text/html');
        this.walkTextNodes(doc.body, (node) => {
            const text = node.nodeValue ?? '';
            if (!regex.test(text)) {
                regex.lastIndex = 0; // reset for repeated tests
                return;
            }
            regex.lastIndex = 0;

            const frag = doc.createDocumentFragment();
            let lastIndex = 0;
            let match: RegExpExecArray | null;

            while ((match = regex.exec(text)) !== null) {
                const before = text.slice(lastIndex, match.index);
                if (before) frag.appendChild(doc.createTextNode(before));

                const span = doc.createElement('span');
                span.className = 'topic-page-search-highlight';
                span.textContent = match[0]; // original match; preserve case
                frag.appendChild(span);

                lastIndex = match.index + match[0].length;
            }
            const after = text.slice(lastIndex);
            if (after) frag.appendChild(doc.createTextNode(after));

            node.parentNode?.replaceChild(frag, node);
        });

        // return the string; Angular will sanitize again for [innerHTML]
        return doc.body.innerHTML;
    }

    private walkTextNodes(root: Node, visitor: (textNode: Text) => void) {
        const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, {
            acceptNode: (node: any) => {
                // ignore text in script/style
                const parentName = node.parentElement?.nodeName;
                if (parentName === 'SCRIPT' || parentName === 'STYLE')
                    return NodeFilter.FILTER_REJECT;
                return NodeFilter.FILTER_ACCEPT;
            },
        } as any);
        let n: Node | null;
        while ((n = walker.nextNode())) {
            visitor(n as Text);
        }
    }

    private escapeRegExp(s: string): string {
        // escape special characters
        return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    }
}
