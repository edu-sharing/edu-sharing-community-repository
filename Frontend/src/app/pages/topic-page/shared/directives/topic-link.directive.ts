import { Directive, ElementRef, HostListener, inject, input } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { TopicPageGlobalService } from '../services/topic-page-global.service';
import { TopicPageHelperService } from '../services/topic-page-helper.service';

/**
 * Marks a link to the topic page of the given collection node. A plain click lets the
 * surrounding topic page load the topic in place; otherwise the link is followed as usual.
 */
@Directive({
    selector: 'a[esTopicLink]',
    standalone: true,
})
export class TopicLinkDirective {
    private readonly elementRef = inject<ElementRef<HTMLAnchorElement>>(ElementRef);
    private readonly topicPageGlobalService = inject(TopicPageGlobalService);
    private readonly topicPageHelperService = inject(TopicPageHelperService);

    readonly node = input.required<Node>({ alias: 'esTopicLink' });
    /** For links in a preview: the previewed node, whose topic page loads the topic in place. */
    readonly previewOf = input<Node | null>(null, { alias: 'esTopicLinkPreviewOf' });

    @HostListener('click', ['$event'])
    onClick(event: MouseEvent): void {
        // modified clicks keep the browser's behavior, e.g. opening a new tab
        if (
            event.defaultPrevented ||
            event.button !== 0 ||
            event.ctrlKey ||
            event.metaKey ||
            event.shiftKey ||
            event.altKey
        ) {
            return;
        }
        const url: string | null = this.elementRef.nativeElement.getAttribute('href');
        const collectionId: string = this.node().ref.id;
        const previewOf: Node | null = this.previewOf();
        const handled: boolean = previewOf
            ? this.topicPageGlobalService.navigateInPlaceFromPreview(
                  previewOf.ref.id,
                  collectionId,
                  url,
              )
            : this.topicPageHelperService.navigateInPlace(collectionId, url);
        if (handled) {
            event.preventDefault();
        }
    }
}
