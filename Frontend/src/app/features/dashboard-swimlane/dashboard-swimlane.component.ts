import { CommonModule } from '@angular/common';
import {
    ApplicationRef,
    Component,
    computed,
    effect,
    input,
    signal,
    ViewChild,
    inject,
} from '@angular/core';
import {
    ColumnType,
    EduSharingUiCommonModule,
    ElementType,
    GridConfig,
    HideMode,
    InteractionType,
    MdsHelperService,
    NodeDataSource,
    NodeEntriesData,
    NodeEntriesDataType,
    NodeEntriesDisplayType,
    NodeEntriesModule,
    NodeEntriesService,
    NodeEntriesWrapperComponent,
    OptionItem,
    Scope,
    Target,
    UIAnimation,
    UIConstants,
    UIService,
} from 'ngx-edu-sharing-ui';
import { SwimlaneEntry } from '../../pages/landing-page/landing-page.component';
import { MatButtonModule } from '@angular/material/button';
import { BehaviorSubject, firstValueFrom, Observable } from 'rxjs';
import {
    AssignmentV1Service,
    AuthenticationService,
    DEFAULT,
    HOME_REPOSITORY,
    Node,
    NodeEvent,
    NodeService,
    NodeShare,
    ROOT,
    SearchResultGeneric,
    SearchResultNode,
    SearchService,
    SearchSortModifiers,
    SessionStorageService,
} from 'ngx-edu-sharing-api';
import { trigger } from '@angular/animations';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { RestConstants } from '../../core-module/rest/rest-constants';
import { Params, Router, RouterLink } from '@angular/router';
import { filter, first } from 'rxjs/operators';
import { DashboardInteractivityStreamComponent } from './dashboard-interactivity-stream/dashboard-interactivity-stream.component';
import { OptionsHelperService } from '../../services/options-helper.service';
import { RECENT_ACTIVITY_EVENT_TYPES } from '../../pages/editorial-page/editorial-page.service';

type StreamDetails = { key: string; result: SearchResultGeneric<NodeEvent>; params: Params };
type ShareDetails = { key: string; result: SearchResultGeneric<NodeShare>; params: Params };
@Component({
    selector: 'es-dashboard-swimlane',
    providers: [NodeEntriesService, OptionsHelperService],
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
    private storage = inject(SessionStorageService);
    private translate = inject(TranslateService);
    private ref = inject(ApplicationRef);
    private router = inject(Router);
    private authenticationService = inject(AuthenticationService);
    private searchService = inject(SearchService);
    private uiService = inject(UIService);
    private optionsHelperService = inject(OptionsHelperService);
    private nodeService = inject(NodeService);
    private assignmentService = inject(AssignmentV1Service);
    private mdsHelperService = inject(MdsHelperService);

    /**
     * @param {SwimlaneEntry} swimlane - The required SwimlaneEntry.
     * @description
     * Represents a swimlane on the start page
     */
    readonly swimlane = input.required<SwimlaneEntry>();
    readonly type = computed(() => {
        return 'recent-activities' === this.swimlane().id
            ? 'interactivity-stream-activities'
            : 'shares' === this.swimlane().id
            ? 'interactivity-stream-shares'
            : 'nodes';
    });
    @ViewChild(NodeEntriesWrapperComponent)
    nodeNodeEntriesWrapperComponent: NodeEntriesWrapperComponent<Node>;
    readonly Scope = Scope;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    readonly dataSource = new NodeDataSource<NodeEntriesDataType>();
    /**
     * max items per swimlane
     */
    readonly maxItems = 25;
    readonly maxItemsEvents = 4;
    columns = signal({} as ColumnType);
    streamEvents = signal(null as StreamDetails[]);
    sharesEvents = signal(null as ShareDetails[]);
    displayType = signal(NodeEntriesDisplayType.Grid);
    globalOptions = signal<OptionItem[]>([]);
    routerLink = signal('');
    routerQueryParams = signal<Params>({});
    open = new BehaviorSubject(false);
    gridConfig: GridConfig = {
        layout: 'scroll',
        maxRows: 1,
    };
    private nodes = signal<NodeEntriesData>(null);
    constructor() {
        this.open
            .pipe(
                filter((o) => !!o),
                first(),
            )
            .subscribe(() => void this.initSwimlane());
        effect(() => {
            void this.storage
                .get<boolean>(this.getStorageKey(), this.swimlane().defaultExpanded)
                .then((v) => this.open.next(v));

            this.dataSource.isLoading = this.nodes() == null || !this.columns();
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
            .getColumnsByMdsId('swimlane_' + this.swimlane().id, {
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
        } else if (this.swimlane().id === 'assignments') {
            this.displayType.set(NodeEntriesDisplayType.SmallGrid);
            this.routerLink.set('/' + UIConstants.ROUTER_PREFIX + 'editorial/assignment');
            this.routerQueryParams.set({});
            const createAssignment = new OptionItem('OPTIONS.NEW_ASSIGNMENT', 'add', () => {
                void this.router.navigate([UIConstants.ROUTER_PREFIX + 'editorial/assignment'], {
                    queryParams: {
                        mainComponent: 'manageAssignment',
                    },
                    queryParamsHandling: 'replace',
                });
            });
            createAssignment.elementType = [ElementType.NoneOrUnknown];
            createAssignment.toolpermissions = [
                RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_ASSIGNMENTS,
            ];
            createAssignment.toolpermissionsMode = HideMode.Hide;
            void this.setGlobalOptions([createAssignment]);

            void this.fetch(
                this.assignmentService.searchAssignments({
                    body: {
                        criteria: [
                            { property: 'virtual:assignmentType', values: ['swimlane_landing'] },
                        ],
                    },
                    sortProperties: [RestConstants.CM_PROP_C_CREATED],
                    sortAscending: [false],
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
            const newCollection = new OptionItem('OPTIONS.NEW_COLLECTION', 'add', () => {
                void this.router.navigate([
                    UIConstants.ROUTER_PREFIX + 'collections/collection',
                    'new',
                    ROOT,
                ]);
            });
            newCollection.elementType = [ElementType.NoneOrUnknown];
            void this.setGlobalOptions([newCollection]);
        } else if (this.swimlane().id === 'recent-activities') {
            this.routerLink.set('/' + UIConstants.ROUTER_PREFIX + 'editorial/activity');
            const events = [] as StreamDetails[];
            let i = 0;
            for (const [key, types] of Object.entries(RECENT_ACTIVITY_EVENT_TYPES)) {
                events.splice(i++, 0, {
                    key,
                    result: await firstValueFrom(
                        this.searchService.search({
                            metadataset: DEFAULT,
                            query: null,
                            searchMode: 'recentActivity',
                            repository: HOME_REPOSITORY,
                            contentType: 'ALL',
                            eventType: types,
                            maxItems: this.maxItemsEvents,
                            body: {
                                criteria: [],
                            },
                        }),
                    ),
                    params: {
                        filters: JSON.stringify({ 'virtual:activityType': [key] }),
                    },
                });
            }
            this.streamEvents.set(events);
        } else if (this.swimlane().id === 'shares') {
            this.routerLink.set('/' + UIConstants.ROUTER_PREFIX + 'editorial/share');
            const events = [] as ShareDetails[];
            [
                ['toUser', 'toUser'],
                ['toUserGroups', 'toUserGroups'],
                ['fromUser', 'fromUser'],
            ].forEach(async (k, i) => {
                events.splice(i, 0, {
                    key: k[0],
                    result: await firstValueFrom(
                        this.searchService.search({
                            metadataset: DEFAULT,
                            query: null,
                            searchMode: 'shares',
                            direction: k[1] as any,
                            repository: HOME_REPOSITORY,
                            contentType: 'ALL',
                            maxItems: this.maxItemsEvents,
                            body: {
                                criteria: [],
                            },
                        }),
                    ),
                    params: {
                        filters: JSON.stringify({ 'virtual:shareDirection': [k[1]] }),
                    },
                });
            });
            this.sharesEvents.set(events);
        }
    }

    private async fetch(observable: Observable<NodeEntriesData>) {
        this.nodes.set(await firstValueFrom(observable));
        // this.nodes.set({nodes: [], pagination: {} as any});
        void this.nodeNodeEntriesWrapperComponent?.initOptionsGenerator({});
    }

    private async setGlobalOptions(optionItems: OptionItem[]) {
        this.globalOptions.set(
            await this.optionsHelperService.filterOptions(optionItems, Target.ListGlobalOption),
        );
    }
}
