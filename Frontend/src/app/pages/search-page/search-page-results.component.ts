import {
    Component,
    computed,
    inject,
    OnDestroy,
    OnInit,
    ViewChild,
    viewChild,
} from '@angular/core';
import {
    ActionbarComponent,
    CustomOptions,
    DefaultGroups,
    ElementType,
    InteractionType,
    ListSortConfig,
    NodeClickEvent,
    NodeEntriesDataType,
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
import { distinctUntilChanged, skip, switchMap, takeUntil } from 'rxjs/operators';
import { FrameEventsService } from '../../core-module/rest/services/frame-events.service';
import { Values } from '../../features/mds/types/types';
import { ConfigService, Node } from 'ngx-edu-sharing-api';
import { EditorialSidebarService } from '../../features/editorial-sidebar/editorial-sidebar.service';
import { SelectionChange } from '@angular/cdk/collections';
import { SearchFieldInternalService } from '../../main/navigation/search-field/search-field-internal.service';

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
    private globalSearchPageInternal = inject(GlobalSearchPageServiceInternal);
    private results = inject(SearchPageResultsService);
    private configService = inject(ConfigService);
    searchPage = inject(SearchPageService);
    private temporaryStorageService = inject(TemporaryStorageService);
    private editorialSidebarService = inject(EditorialSidebarService);
    private frameEventsService = inject(FrameEventsService);
    private announcer = inject(LiveAnnouncer);
    private translate = inject(TranslateService);
    private searchFieldInternalService = inject(SearchFieldInternalService);

    readonly InteractionType = InteractionType;
    readonly Scope = Scope;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    private destroyed = new Subject<void>();
    previewMode: string | 'Sidebar' | 'RenderingPage';
    primaryActionOptions: CustomOptions;

    @ViewChild('nodeEntriesResults')
    nodeEntriesResults: NodeEntriesWrapperComponent<Node>;

    private readonly actionbarToggles = viewChild<ActionbarComponent>('actionbarToggles');
    private readonly actionbarActions = viewChild<ActionbarComponent>('actionbarActions');
    private readonly actionbarAddToCollection = viewChild<ActionbarComponent>(
        'actionbarAddToCollection',
    );
    private readonly actionbarPrimaryBanner =
        viewChild<ActionbarComponent>('actionbarPrimaryBanner');
    /**
     * All actionbars driven by the same computed options: the toggles-only title bar, the actions-only
     * sticky selection bar, and the banner actionbars shown in add-to-collection / primary-action modes
     * (these are mutually exclusive in the template; absent ones resolve to undefined and are filtered).
     */
    readonly actionbars = computed(() =>
        [
            this.actionbarToggles(),
            this.actionbarActions(),
            this.actionbarAddToCollection(),
            this.actionbarPrimaryBanner(),
        ].filter((bar): bar is ActionbarComponent => !!bar),
    );

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

    constructor() {
        const results = this.results;

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
            // Reset any selection carried over from the "all repositories" view we navigated from.
            this.clearSelection();
        });
        // Clear the selection when the active repository changes (the component is reused across repos).
        this.searchPage.activeRepository
            .observeValue()
            .pipe(distinctUntilChanged(), skip(1), takeUntil(this.destroyed))
            .subscribe(() => this.clearSelection());
        this.previewMode = await this.configService.get('searchPreviewMode', 'Sidebar');
    }

    onClick(event: NodeClickEvent<Node>) {
        this.editorialSidebarService.handleSelect(this.nodeEntriesResults, event, Scope.Search);
        this.results.onClick(event.element);
    }

    toggleFilters(): void {
        this.searchFieldInternalService.filtersButtonClicked.next();
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

    selectionChange(selection: SelectionChange<NodeEntriesDataType>) {
        this.searchPage.selection.next(selection.source.selected as Node[]);
        this.editorialSidebarService.handleSelection(selection);
    }

    clearSelection() {
        this.nodeEntriesResults?.getSelection().clear();
        // Reset the shared subject directly: clearing an already-empty wrapper emits no change event,
        // so a stale selection (e.g. carried over from the "all repositories" view) wouldn't reset.
        this.searchPage.selection.next([]);
    }
}
