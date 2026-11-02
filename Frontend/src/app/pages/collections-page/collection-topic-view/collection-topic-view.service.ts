import { computed, effect, inject, Injectable, signal, Type } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { Node, ParentEntries } from 'ngx-edu-sharing-api';
import { ParentList, RestConstants, RestHelper } from '../../../core-module/core.module';
import { CollectionDetailsService } from '../../../features/collection-info/collection-details.service';
import {
    hasTopicPage,
    OPEN_MANAGE_SIDEBAR_PARAM,
    OPEN_MENU_QUERY_PARAM,
    TOPIC_PAGE_VIEW_TYPE,
    VIEW_TYPE_QUERY_PARAM,
} from '../../../features/topic-page/shared/utils/collection-topic-page-util';
import { ViewToggleItem } from '../../../shared/components/view-toggle-group/view-toggle-group.component';
import { CollectionViewStateService, CollectionViewType } from '../collection-view-state.service';

// query params only the topic view reads; they are dropped when another view is selected
const TOPIC_STATE_QUERY_PARAMS = ['variantId', OPEN_MENU_QUERY_PARAM, 'q', 'filters'];

/**
 * The topic view of the collections page: whether the open collection offers it, when it is shown
 * and how the URL keeps it. Only the topic view is kept in the URL, so a copied link opens it,
 * while every other view falls back to the default.
 */
@Injectable()
export class CollectionTopicViewService {
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly details = inject(CollectionDetailsService);
    private readonly viewState = inject(CollectionViewStateService);

    /** Query params the topic view applies itself, without the page loading anything. */
    readonly queryParamKeys: readonly string[] = [
        VIEW_TYPE_QUERY_PARAM,
        ...TOPIC_STATE_QUERY_PARAMS,
    ];

    /** whether a topic page is configured for the open collection */
    private readonly configured = signal(false);
    private readonly writable = computed(() => {
        const collection = this.details.collection();
        return (
            !!collection &&
            RestHelper.hasAccessPermission(collection, RestConstants.PERMISSION_WRITE)
        );
    });
    /** viewers only get a configured topic page, writers can also set one up */
    readonly available = computed(() => this.configured() || this.writable());
    /** whether the topic view is the selected view type */
    readonly selected = computed(() => this.viewState.viewType() === 'topicPage');
    /** the entry of the topic view in the page's view toggle */
    readonly toggleItem = computed(
        (): ViewToggleItem<CollectionViewType> => ({
            id: 'topicPage',
            icon: 'menu_book',
            label: this.available()
                ? 'EDITORIAL.OPTIONS.NODES_SELECTOR.COLLECTIONS.DISPLAY_TYPE.TOPIC'
                : 'EDITORIAL.OPTIONS.NODES_SELECTOR.COLLECTIONS.DISPLAY_TYPE.TOPIC_UNAVAILABLE',
            disabled: !this.available(),
        }),
    );
    /** the topic view, loaded the first time it is selected */
    readonly component = signal<Type<unknown> | null>(null);
    /** ancestors of the open collection, the collection first, as the topic view reads them */
    readonly parents = signal<ParentEntries | null>(null);
    private params: Params | null = null;
    /** the collection whose default view was applied last */
    private defaultViewAppliedFor: string | null = null;

    constructor() {
        effect(() => {
            if (this.selected()) {
                void import('./collection-topic-view.component').then((m) =>
                    this.component.set(m.CollectionTopicViewComponent),
                );
            }
        });
    }

    /**
     * Applies the view type of new query params, before the page loads anything for them. A
     * collection opened from the topic view chooses its view itself.
     */
    applyParams(params: Params): void {
        const openedFromTopicView =
            params.id !== this.params?.id &&
            this.params?.[VIEW_TYPE_QUERY_PARAM] === TOPIC_PAGE_VIEW_TYPE;
        this.params = params;
        if (params[VIEW_TYPE_QUERY_PARAM] === TOPIC_PAGE_VIEW_TYPE && !openedFromTopicView) {
            this.viewState.viewType.set('topicPage');
        } else if (this.selected()) {
            this.viewState.viewType.set('grid');
        }
    }

    /** Forgets the collection before while the next one loads. */
    reset(): void {
        this.configured.set(false);
        this.parents.set(null);
    }

    /**
     * Once the collection and its ancestors are known, applies its default view, falls back from a
     * topic view it does not offer and keeps the URL in line with the view. The topic view opens
     * with the ancestors afterwards, so it reads this collection's state from the URL.
     */
    async resolveCollectionViewType(
        collection: Node,
        parentList: ParentList | null,
    ): Promise<void> {
        // the topic page works with the ancestors as the node API returns them, the collection first
        const parents = parentList
            ? ({
                  nodes: [...parentList.nodes],
                  pagination: parentList.pagination,
                  scope: parentList.scope,
              } as unknown as ParentEntries)
            : null;
        const isRoot = collection.ref.id === RestConstants.ROOT;
        this.configured.set(!isRoot && hasTopicPage(collection, parents?.nodes ?? []));
        const openedByDefault = this.applyCollectionDefaultView(collection);
        if (this.selected() && (isRoot || !this.available())) {
            this.viewState.viewType.set('grid');
        }
        const selected = this.selected();
        if (
            openedByDefault ||
            selected !== (this.params?.[VIEW_TYPE_QUERY_PARAM] === TOPIC_PAGE_VIEW_TYPE)
        ) {
            await this.setTopicViewParam(selected, true, openedByDefault);
        }
        this.parents.set(parents);
    }

    /** Applies a view type the user selected and keeps the URL in line with it. */
    select(viewType: CollectionViewType): void {
        const leavesTopicView = this.selected() && viewType !== 'topicPage';
        this.viewState.viewType.set(viewType);
        if (viewType === 'topicPage') {
            // switched to by the user, the topic page starts with the list of its pages
            void this.setTopicViewParam(true, false, false, {
                [OPEN_MENU_QUERY_PARAM]: OPEN_MANAGE_SIDEBAR_PARAM,
            });
        } else if (leavesTopicView) {
            void this.setTopicViewParam(false, false);
        }
    }

    /**
     * Opens the topic view of a collection that is set to open as a topic page, the first time
     * the collection is shown after another one. Returns whether it opened the topic view.
     */
    private applyCollectionDefaultView(collection: Node): boolean {
        const id = collection.ref.id;
        if (id === RestConstants.ROOT) {
            this.defaultViewAppliedFor = null;
            return false;
        }
        if (id === this.defaultViewAppliedFor) {
            return false;
        }
        this.defaultViewAppliedFor = id;
        if (
            collection.collection?.viewtype === TOPIC_PAGE_VIEW_TYPE &&
            this.available() &&
            !this.selected()
        ) {
            this.viewState.viewType.set('topicPage');
            return true;
        }
        return false;
    }

    /**
     * @param resetTopicState drops the topic view's state, e.g. one of a collection before
     * @param topicParams further state the topic view opens with
     */
    private async setTopicViewParam(
        topicView: boolean,
        replaceUrl: boolean,
        resetTopicState = false,
        topicParams: Params = {},
    ): Promise<void> {
        const queryParams: Params = {
            [VIEW_TYPE_QUERY_PARAM]: topicView ? TOPIC_PAGE_VIEW_TYPE : null,
            ...topicParams,
        };
        if (!topicView || resetTopicState) {
            TOPIC_STATE_QUERY_PARAMS.forEach((key) => (queryParams[key] = null));
        }
        await this.router.navigate([], {
            relativeTo: this.route,
            queryParams,
            queryParamsHandling: 'merge',
            replaceUrl,
        });
    }
}
