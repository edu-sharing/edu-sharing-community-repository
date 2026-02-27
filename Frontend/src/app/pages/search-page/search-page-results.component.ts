import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import {
    ActionbarComponent,
    CustomOptions,
    DefaultGroups,
    ElementType,
    InteractionType,
    ListSortConfig,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
    OptionItem,
    OptionItemToggle,
    Scope,
    TemporaryStorageService,
} from 'ngx-edu-sharing-ui';
import { SearchPageResultsService } from './search-page-results.service';
import { SearchPageService } from './search-page.service';
import { GlobalSearchPageServiceInternal } from './global-search-page.service';
import { Subject } from 'rxjs';
import { LiveAnnouncer } from '@angular/cdk/a11y';
import { TranslateService } from '@ngx-translate/core';
import { switchMap, takeUntil } from 'rxjs/operators';
import { FrameEventsService } from '../../core-module/rest/services/frame-events.service';
import { Values } from '../../features/mds/types/types';
import { ConfigService, Node } from 'ngx-edu-sharing-api';

export type SearchFilter = {
    propertyFilters: Values;
    searchString: string;
};

@Component({
    selector: 'es-search-page-results',
    templateUrl: './search-page-results.component.html',
    styleUrls: ['./search-page-results.component.scss'],
    providers: [SearchPageResultsService],
    standalone: false,
})
export class SearchPageResultsComponent implements OnInit, OnDestroy {
    readonly InteractionType = InteractionType;
    readonly Scope = Scope;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    private destroyed = new Subject<void>();
    previewMode: string | 'Sidebar' | 'RenderingPage';
    primaryActionOptions: CustomOptions;

    @ViewChild('nodeEntriesResults')
    nodeEntriesResults: NodeEntriesWrapperComponent<Node>;

    @ViewChild(ActionbarComponent)
    set _actionbar(value: ActionbarComponent) {
        // Avoid changed-after-checked error.
        setTimeout(() => (this.actionbar = value));
    }
    actionbar: ActionbarComponent;

    readonly resultsDataSource = this.results.resultsDataSource;
    readonly collectionsDataSource = this.results.collectionsDataSource;
    readonly resultColumns = this.results.resultColumns;
    readonly collectionColumns = this.results.collectionColumns;
    readonly state = this.results.state;
    readonly onDblClick = this.results.onDblClick;
    readonly addToCollectionMode = this.searchPage.addToCollectionMode;
    readonly primaryAction = this.searchPage.primaryAction;
    readonly customTemplates = this.globalSearchPageInternal.customTemplates;
    defaultCustomOptions: CustomOptions;
    constructor(
        private globalSearchPageInternal: GlobalSearchPageServiceInternal,
        private results: SearchPageResultsService,
        private configService: ConfigService,
        public searchPage: SearchPageService,
        private temporaryStorageService: TemporaryStorageService,
        private frameEventsService: FrameEventsService,
        private announcer: LiveAnnouncer,
        private translate: TranslateService,
    ) {
        this.registerPrimaryActionOptions();
        // announce newly loaded elements to users using screen readers
        results.diffCount
            .pipe(
                takeUntil(this.destroyed), // FIXME: replace with takeUntilDestroyed in Angular 16+
                switchMap((newlyLoadedElements) =>
                    this.translate.get('SEARCH.LOADED_RESULTS', { count: newlyLoadedElements }),
                ),
            )
            .subscribe((elementsLoadedTranslation) => {
                void this.announcer.announce(elementsLoadedTranslation);
            });

        const toggleSearchFilter = new OptionItemToggle(
            { enabled: 'SEARCH.FILTERS', disabled: 'SEARCH.FILTERS' },
            { enabled: 'filter_list', disabled: 'filter_list' },
            false,
            () => this.toggleFilters(),
        );
        toggleSearchFilter.scopes = [Scope.Search];
        toggleSearchFilter.constrains = [];
        toggleSearchFilter.group = DefaultGroups.Toggles;
        toggleSearchFilter.elementType = [];
        toggleSearchFilter.priority = 30;
        toggleSearchFilter.toggleType = 'primary';
        toggleSearchFilter.togglePosition = 'before';
        this.defaultCustomOptions = {
            useDefaultOptions: true,
            addOptions: [toggleSearchFilter],
        };
    }

    async ngOnInit() {
        setTimeout(() => {
            this.searchPage.results = this.results;
            this.searchPage.showingAllRepositories.next(false);
        });
        this.previewMode = await this.configService.get('searchPreviewMode', 'Sidebar');
    }

    onClick(event: Node) {
        this.nodeEntriesResults.getSelection().setSelection(event);
        this.results.onClick(event);
    }

    toggleFilters(): void {
        const filterBarIsVisible = this.searchPage.filterBarIsVisible;
        filterBarIsVisible.setUserValue(!filterBarIsVisible.getValue());
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
        this.temporaryStorageService.set(
            TemporaryStorageService.NODE_RENDER_PARAMETER_DATA_SOURCE,
            this.resultsDataSource,
        );
    }
    getContainerClass() {
        if (this.searchPage.searchString.getValue()) {
            return '-ngsearchword';
        }
        return '';
    }

    setDisplayType(displayType: NodeEntriesDisplayType) {
        this.results.patchState({ displayType });
    }

    updateSort(sort: ListSortConfig) {
        this.results.searchSort.setUserValue({
            active: sort.active,
            direction: sort.direction,
        });
    }

    registerPrimaryActionOptions() {
        this.primaryAction.subscribe((action) => {
            if (action === 'applyFilter') {
                const applyFilter = new OptionItem('OPTIONS.APPLY_FILTER', 'redo', () => {
                    const filters = {
                        propertyFilters: this.searchPage.searchFilters.getValue() as Values,
                        searchString: this.searchPage.searchString.getValue(),
                    } as SearchFilter;
                    const data = JSON.stringify(filters);
                    console.info(data);
                    this.frameEventsService.broadcastEvent(
                        FrameEventsService.EVENT_APPLY_FILTER,
                        data,
                    );
                    window.close();
                });
                applyFilter.group = DefaultGroups.Primary;
                applyFilter.elementType = [ElementType.NoneOrUnknown];
                this.primaryActionOptions = {
                    useDefaultOptions: false,
                    addOptions: [applyFilter],
                };
            } else {
                this.primaryActionOptions = null;
            }
        });
    }
}
