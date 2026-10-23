import { Component, DestroyRef, effect, inject, input, viewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TemplateComponent } from '../../topic-page/editor/template.component';
import { TopicChangedEvent } from '../../topic-page/shared/types/topic-changed-event';
import { CollectionViewSlotsService } from '../collection-view-slots.service';

/**
 * The topic page of a collection, shown as a view of the collections page below its breadcrumb.
 * Loaded on demand, so the collections page does not carry the topic page code.
 */
@Component({
    selector: 'es-collection-topic-view',
    imports: [TemplateComponent],
    templateUrl: './collection-topic-view.component.html',
    styleUrls: ['./collection-topic-view.component.scss'],
})
export class CollectionTopicViewComponent {
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly slots = inject(CollectionViewSlotsService);

    readonly collectionId = input.required<string>();
    /** the variant the URL selects when the view opens; later selections are kept by the page */
    readonly initialVariantId: string | undefined =
        this.route.snapshot.queryParamMap.get('variantId') ?? undefined;
    private readonly template = viewChild.required(TemplateComponent);

    constructor() {
        effect(() => this.slots.breadcrumbEnd.set(this.template().variantSelectTemplate()));
        inject(DestroyRef).onDestroy(() => this.slots.breadcrumbEnd.set(null));
    }

    /** Opens the collection of a topic linked inside the page, staying in the topic view. */
    onTopicChanged(event: TopicChangedEvent): void {
        void this.router.navigate([], {
            relativeTo: this.route,
            queryParams: { id: event.collectionId, variantId: null },
            queryParamsHandling: 'merge',
        });
    }
}
