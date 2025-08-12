import { CommonModule } from '@angular/common';
import {
    ApplicationRef,
    Component,
    computed,
    effect,
    input,
    signal,
    ViewChild,
} from '@angular/core';
import {
    EduSharingUiCommonModule,
    GridConfig,
    InteractionType,
    MdsHelperService,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeEntriesModule,
    NodeEntriesService,
    NodeEntriesWrapperComponent,
    OptionItem,
    Scope,
    UIAnimation,
    UIConstants,
} from 'ngx-edu-sharing-ui';
import { SwimlaneEntry } from '../../pages/landing-page/landing-page.component';
import { MatButtonModule } from '@angular/material/button';
import { BehaviorSubject, firstValueFrom, Observable } from 'rxjs';
import {
    DEFAULT,
    HOME_REPOSITORY,
    Node,
    NodeEntries,
    NodeService,
    ROOT,
    SearchResultEvent,
    SearchResultNode,
    SearchService,
    SearchServiceUnwrapped,
    SearchSortModifiers,
    SessionStorageService,
    UserEvent,
} from 'ngx-edu-sharing-api';
import { trigger } from '@angular/animations';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { RestConstants } from '../../core-module/rest/rest-constants';
import { Params, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter, first } from 'rxjs/operators';
import { DashboardInteractivityStreamComponent } from './dashboard-interactivity-stream/dashboard-interactivity-stream.component';

type StreamDetails = { key: string; result: SearchResultEvent; params: Params };
@Component({
    selector: 'es-dashboard-swimlane',
    providers: [NodeEntriesService],
    standalone: true,
    templateUrl: './dashboard-swimlane.component.html',
    styleUrls: ['./dashboard-swimlane.component.scss'],
    animations: [trigger('openOverlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST))],
    imports: [
        CommonModule,
        DashboardInteractivityStreamComponent,
        EduSharingUiCommonModule,
        TranslateModule,
        MatButtonModule,
        NodeEntriesModule,
        RouterLink,
    ],
})
export class DashboardSwimlaneComponent {
    /**
     * @param {SwimlaneEntry} swimlane - The required SwimlaneEntry.
     * @description
     * Represents a swimlane on the start page
     */
    readonly swimlane = input.required<SwimlaneEntry>();
    readonly type = computed(() => {
        return this.swimlane().id === 'recent-activities' ? 'interactivity-stream' : 'nodes';
    });
    @ViewChild(NodeEntriesWrapperComponent)
    nodeNodeEntriesWrapperComponent: NodeEntriesWrapperComponent<Node>;
    readonly Scope = Scope;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    readonly dataSource = new NodeDataSource();
    /**
     * max items per swimlane
     */
    readonly maxItems = 25;
    readonly maxItemsEvents = 6;
    columns = signal([]);
    streamEvents = signal(null as StreamDetails[]);
    displayType = signal(NodeEntriesDisplayType.Grid);
    globalOptions = signal<OptionItem[]>([]);
    routerLink = signal('');
    routerQueryParams = signal<Params>({});
    open = new BehaviorSubject(false);
    gridConfig: GridConfig = {
        layout: 'scroll',
        maxRows: 1,
    };
    private nodes = signal<NodeEntries>(null);
    constructor(
        private storage: SessionStorageService,
        private translate: TranslateService,
        private ref: ApplicationRef,
        private router: Router,
        private searchService: SearchService,
        private searchServiceUnwrapped: SearchServiceUnwrapped,
        private nodeService: NodeService,
        private mdsHelperService: MdsHelperService,
    ) {
        this.open
            .pipe(
                filter((o) => !!o),
                first(),
            )
            .subscribe(() => void this.initSwimlane());
        effect(() => {
            this.storage
                .get(this.getStorageKey(), this.swimlane().defaultExpanded)
                .subscribe((v) => this.open.next(v));

            this.dataSource.isLoading = this.nodes() == null || this.columns().length === 0;
            if (!this.dataSource.isLoading) {
                this.dataSource.setData(this.nodes().nodes, this.nodes().pagination);
            }
        });
    }
    toggleVisibility() {
        this.open.next(!this.open.value);
        void this.storage.set(this.getStorageKey(), this.open.value);
        setTimeout(() => this.nodeNodeEntriesWrapperComponent?.initOptionsGenerator({}));
    }

    private getStorageKey() {
        return 'frontpage_swimlane_' + this.swimlane().id + '_expanded';
    }

    /**
     * Initializes the swimlane by resetting the data source, setting loading state,
     * fetching columns based on the swimlane ID
     *
     * @returns {Promise<void>} - A promise that resolves when the initialization is complete.
     */
    private async initSwimlane(): Promise<void> {
        this.dataSource.reset();
        this.dataSource.isLoading = true;
        void this.mdsHelperService
            .getColumns('swimlane_' + this.swimlane().id, {
                repository: HOME_REPOSITORY,
                metadataSet: DEFAULT,
            })
            .then((columns) => {
                this.columns.set(columns);
            });
        if (this.swimlane().id === 'featured-media') {
            this.displayType.set(NodeEntriesDisplayType.Grid);
            this.routerLink.set('/' + UIConstants.ROUTER_PREFIX + 'search');
            this.routerQueryParams.set({});
            void this.fetch(
                this.nodeService.getChildren(RestConstants.NODES_FRONTPAGE, {
                    maxItems: this.maxItems,
                }),
            );
        } else if (this.swimlane().id === 'collections') {
            this.displayType.set(NodeEntriesDisplayType.SmallGrid);
            this.routerLink.set('/' + UIConstants.ROUTER_PREFIX + 'collections');
            this.routerQueryParams.set({ scope: 'MY' });
            void this.fetch(
                this.searchService.search<SearchResultNode>({
                    query: 'dashboard_my_collections',
                    repository: HOME_REPOSITORY,
                    metadataset: DEFAULT,
                    contentType: 'ALL',
                    body: {
                        criteria: [],
                    },
                    sortProperties: [
                        // sort grouped by current day
                        RestConstants.CM_MODIFIED_DATE + '|' + SearchSortModifiers.GranularityDate,
                        // then, sort based on permissions
                        RestConstants.LUCENE_SCORE,
                        // then again, sort by date including time
                        RestConstants.CM_MODIFIED_DATE,
                    ],
                    sortAscending: [false, false, false],
                    maxItems: this.maxItems,
                }),
            );
            this.globalOptions.set([
                new OptionItem('OPTIONS.NEW_COLLECTION', 'add', () => {
                    void this.router.navigate([
                        UIConstants.ROUTER_PREFIX + 'collections/collection',
                        'new',
                        ROOT,
                    ]);
                }),
            ]);
        } else if (this.swimlane().id === 'recent-activities') {
            const events = [];
            events.push({
                key: 'files',
                result: await firstValueFrom(
                    this.searchServiceUnwrapped.getRecentUserEvents({
                        repository: HOME_REPOSITORY,
                        contentType: 'FILES',
                        maxItems: this.maxItemsEvents,
                    }),
                ),
                params: { contentType: 'FILES' },
            });

            events.push({
                key: 'collections',
                result: await firstValueFrom(
                    this.searchServiceUnwrapped.getRecentUserEvents({
                        repository: HOME_REPOSITORY,
                        contentType: 'COLLECTIONS',
                        maxItems: this.maxItemsEvents,
                    }),
                ),
                params: { contentType: 'COLLECTIONS' },
            });
            events.push({
                key: 'folders',
                result: await firstValueFrom(
                    this.searchServiceUnwrapped.getRecentUserEvents({
                        repository: HOME_REPOSITORY,
                        contentType: 'FOLDERS',
                        maxItems: this.maxItemsEvents,
                    }),
                ),
                params: { contentType: 'FOLDERS' },
            });
            this.streamEvents.set(events);
        }
    }

    private async fetch(observable: Observable<NodeEntries>) {
        this.nodes.set(await firstValueFrom(observable));
        // this.nodes.set({nodes: [], pagination: {} as any});
        void this.nodeNodeEntriesWrapperComponent?.initOptionsGenerator({});
    }
}
