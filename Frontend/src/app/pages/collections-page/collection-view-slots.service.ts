import { Injectable, signal, TemplateRef } from '@angular/core';

/**
 * Places in the collections page's chrome a view can fill with content of its own, so the page
 * does not need to know the views rendered below its breadcrumb.
 */
@Injectable()
export class CollectionViewSlotsService {
    /** Rendered at the end of the breadcrumb row, before the view toggle. */
    readonly breadcrumbEnd = signal<TemplateRef<unknown> | null>(null);
}
