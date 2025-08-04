import { Component, computed, OnDestroy, OnInit, signal } from '@angular/core';
import {
    AuthenticationService,
    ConfigService,
    DEFAULT,
    HOME_REPOSITORY,
    MdsDefinition,
    MdsService,
    SearchService,
} from 'ngx-edu-sharing-api';
import { BehaviorSubject, combineLatest, filter, firstValueFrom, Observable, Subject } from 'rxjs';
import { RestConstants } from '../../core-module/rest/rest-constants';
import { ActivatedRoute, Params, Router } from '@angular/router';
import {
    MdsHelperService,
    MdsViewerService,
    NodeDataSource,
    SearchHelperService,
    UIService,
    Values,
} from 'ngx-edu-sharing-ui';
import { MainNavService } from '../../main/navigation/main-nav.service';
import {
    SearchEvent,
    SearchFieldService,
} from '../../main/navigation/search-field/search-field.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EditorialPageService } from './editorial-page.service';
import { MdsHelper } from '../../core-module/rest/mds-helper';
import { debounce, debounceTime, distinctUntilChanged, skip, startWith } from 'rxjs/operators';

export type PrimaryMode = 'activity';
type RouteConfig = {
    primaryMode: PrimaryMode;
};

@Component({
    selector: 'es-editorial-page',
    templateUrl: 'editorial-page.component.html',
    styleUrls: ['editorial-page.component.scss'],
    standalone: false,
})
export class EditorialPageComponent implements OnInit, OnDestroy {
    readonly HOME_REPOSITORY = HOME_REPOSITORY;
    readonly TabWidgetActivities = 'virtual:activityType';
    private destroyed$ = new Subject<void>();
    sidenavLeft = signal(true);
    sidenavRight = signal(true);
    /**
     * mds group, used to fetch the template group AND search query id!
     */
    mdsGroup = signal<string>(null);
    params$: Observable<RouteConfig>;
    tabSelection$ = new BehaviorSubject<number>(0);
    searchValues$ = new BehaviorSubject<Values>({});
    searchEvent$: Observable<SearchEvent>;
    mdsDefinition$ = new BehaviorSubject<MdsDefinition>(null);
    dataSource$ = new NodeDataSource();

    constructor(
        private router: Router,
        private route: ActivatedRoute,
        private mdsService: MdsService,
        private mainNav: MainNavService,
        private searchFieldService: SearchFieldService,
        private searchService: SearchService,
        private configService: ConfigService,
        private searchHelperService: SearchHelperService,
        private ui: UIService,
        private authenticationService: AuthenticationService,
        public editorialPageService: EditorialPageService,
    ) {
        this.mainNav.setMainNavConfig({
            showUser: true,
            showScope: true,
            currentScope: 'EDITORIAL',
            title: 'EDITORIAL.LANDING',
            show: true,
            hideSearchField: false,
            create: {
                allowed: true,
                allowBinary: true,
            },
            showNavigation: true,
        });
        this.searchFieldService.enable(
            {
                showFiltersButton: true,
                enableFiltersAndSuggestions: false,
            },
            this.destroyed$,
        );
        this.searchFieldService
            .observeCurrentInstance()
            .pipe(
                takeUntilDestroyed(),
                filter((i) => !!i),
            )
            .subscribe((instance) => {
                instance
                    .onFiltersButtonClicked()
                    .subscribe(() => this.sidenavLeft.set(!this.sidenavLeft()));
                this.searchEvent$ = instance.onSearchTriggered();
                this.initSubscription();
            });

        this.tabSelection$.subscribe((t) => console.log('tab ,', t));
    }

    async ngOnInit(): Promise<void> {
        this.params$ = this.route.params as Observable<RouteConfig>;
        this.registerMode();
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    private registerMode() {
        this.params$.subscribe(async (p) => {
            console.log(p);
            if (p.primaryMode === 'activity') {
                this.mdsDefinition$.next(
                    await firstValueFrom(
                        this.mdsService.getMetadataSet({ repository: HOME_REPOSITORY }),
                    ),
                );
                const widget = MdsHelperService.getWidget(
                    this.TabWidgetActivities,
                    null,
                    this.mdsDefinition$.value.widgets,
                );
                if (widget == null) {
                    console.warn(
                        'Can not register tabs since widget definition was not found',
                        this.TabWidgetActivities,
                    );
                } else {
                    this.mdsGroup.set('editorial_activity');
                    this.editorialPageService.registerTabsFromWidget(widget);
                }
            }
        });
    }

    private initSubscription() {
        this.route.queryParams.subscribe((params) => {
            this.fetchResults(params);
        });
        combineLatest([
            this.searchEvent$.pipe(startWith(null)),
            this.tabSelection$,
            // first one will be the init of the set
            this.searchValues$.pipe(skip(1)),
        ])
            .pipe(distinctUntilChanged(), debounceTime(50))
            .subscribe(([search, tab, values]) => {
                console.log(search, tab, values);
                const queryParams = {
                    q: search?.searchString,
                    filters: JSON.stringify({
                        ...values,
                        ...this.editorialPageService.buildSearchCriteria(tab),
                    }),
                };
                console.log(this.editorialPageService.buildSearchCriteria(tab));
                void this.router.navigate(['./'], {
                    relativeTo: this.route,
                    replaceUrl: false,
                    queryParams,
                });
            });
    }

    private fetchResults(params: Params) {
        const criteria = JSON.parse(params.filters || {});
        if (params.q) {
            criteria[RestConstants.PRIMARY_SEARCH_CRITERIA] = [params.q];
        }
        const searchCriteria = this.searchHelperService.convertCritieria(
            criteria,
            this.mdsDefinition$.value.widgets,
            true,
        );

        this.searchService
            .search({
                repository: HOME_REPOSITORY,
                metadataset: DEFAULT,
                query: this.mdsGroup(),
                body: {
                    criteria: searchCriteria,
                    resolveCollections: false,
                    resolveUsernames: true,
                },
            })
            .subscribe((result) => {
                this.dataSource$.setData(result.nodes, result.pagination);
            });
    }
}
