import { Component, effect, ElementRef, inject, input, output } from '@angular/core';
import { LocationStrategy } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { Node } from 'ngx-edu-sharing-api';
import { TemporaryStorageService, UIConstants } from 'ngx-edu-sharing-ui';
import { filter } from 'rxjs/operators';
import { TopicChangedEvent } from '../../../features/topic-page/shared/types/topic-changed-event';
import { TopicPageGlobalService } from '../../../features/topic-page/shared/services/topic-page-global.service';
import { TOPIC_PAGE_VIEW_TYPE } from '../../../features/topic-page/shared/utils/collection-topic-page-util';
import { GlobalCollectionsPageService } from '../../../pages/collections-page/global-collections-page.service';
import { GlobalTitleService } from '../../../shared/directives/global-title.service';
import { WebComponentLocationStrategy } from '../../../main/web-component.utils';

/**
 * The topic page of a collection as web component `edu-sharing-topic-page`: the collections page
 * in its topic view, with breadcrumb and sidebar, routed inside the component. Takes the
 * attributes, properties and events of topic page embeddings, e.g. `collection-id`.
 */
@Component({
    selector: 'es-topic-page-wrapper',
    templateUrl: './topic-page-wrapper.component.html',
    styleUrls: ['./topic-page-wrapper.component.scss'],
    standalone: false,
})
export class TopicPageWrapperComponent {
    private readonly router = inject(Router);
    private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
    private readonly topicPageGlobalService = inject(TopicPageGlobalService);

    /** The collection whose topic page is shown; follows topics opened inside the page. */
    readonly collectionId = input<string>();
    readonly variantId = input<string>();
    /** The link of a topic, e.g. for new tabs and copied links. */
    readonly customUrl = input<(node: Node) => string>();
    readonly customUrlTarget = input<'_self' | '_blank'>();

    /** Emitted after the page switched to another topic. */
    readonly topicChanged = output<TopicChangedEvent>();

    /** the collection the page shows, which may be one opened inside it */
    private shownCollectionId: string | undefined;

    constructor() {
        const locationStrategy = inject(LocationStrategy);
        if (locationStrategy instanceof WebComponentLocationStrategy) {
            locationStrategy.showInside('collections');
        }
        // the box shows the page without the app's main navigation, whichever view registers it
        inject(TemporaryStorageService).set(TemporaryStorageService.OPTION_HIDE_MAINNAV, true);
        // the landing page is not shown within the box
        inject(GlobalCollectionsPageService).setShowLanding(false);
        // a box within the embedding page leaves the page's title as it is
        const embeddingPageTitle: string = document.title;
        inject(GlobalTitleService).titleOverrideCallback.next(() => embeddingPageTitle);

        // hooks up the routing inside the component to its location strategy
        this.router.initialNavigation();
        effect(() => this.openTopicPage(this.collectionId(), this.variantId()));
        effect(() => {
            const customUrl = this.customUrl();
            if (customUrl) {
                this.topicPageGlobalService.setCustomUrlFunction(customUrl);
            }
        });
        effect(() => {
            const customUrlTarget = this.customUrlTarget();
            if (customUrlTarget) {
                this.topicPageGlobalService.setCustomUrlTarget(customUrlTarget);
            }
        });
        this.router.events
            .pipe(
                filter((event) => event instanceof NavigationEnd),
                takeUntilDestroyed(),
            )
            .subscribe(() => this.followOpenedCollection());
    }

    private openTopicPage(collectionId: string | undefined, variantId: string | undefined): void {
        if (!collectionId || (collectionId === this.shownCollectionId && !variantId)) {
            return;
        }
        this.shownCollectionId = collectionId;
        void this.router.navigate([UIConstants.ROUTER_PREFIX + 'collections'], {
            queryParams: { id: collectionId, viewType: TOPIC_PAGE_VIEW_TYPE, variantId },
        });
    }

    /** Reflects a collection opened inside the page, so the embedding page can read and reset it. */
    private followOpenedCollection(): void {
        const id: string | undefined = this.router.parseUrl(this.router.url).queryParams['id'];
        if (!id || id === this.shownCollectionId) {
            return;
        }
        this.shownCollectionId = id;
        this.host.nativeElement.setAttribute('collection-id', id);
        this.host.nativeElement.removeAttribute('variant-id');
        const url = this.topicPageGlobalService.getCustomUrlFunction()?.({ ref: { id } } as Node);
        this.topicChanged.emit({ collectionId: id, url: url ?? null });
    }
}
