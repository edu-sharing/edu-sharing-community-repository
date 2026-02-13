import { CommonModule } from '@angular/common';
import {
    AfterViewInit,
    Component,
    computed,
    CUSTOM_ELEMENTS_SCHEMA,
    EventEmitter,
    input,
    Input,
    InputSignal,
    OnDestroy,
    Output,
    Signal,
    signal,
    ViewEncapsulation,
    WritableSignal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MdsQueryCriteria, Node } from 'ngx-edu-sharing-api';
import {
    EduSharingUiCommonModule,
    OptionsHelperService as OptionsHelperServiceAbstract,
    SearchHelperService,
    Values,
} from 'ngx-edu-sharing-ui';
import { TopicPageHelperService } from '../../shared/services/topic-page-helper.service';
import { ConfigurationOption } from '../../shared/types/configuration-option';
import { StatisticNode } from '../../shared/types/statistic-node';
import { SwimlaneBackgroundShape } from '../../shared/types/swimlane-background-shape';
import { ContentTeaserConfig } from '../../shared/types/widget-config/content-teaser-config';
import { createQueryString } from '../../shared/utils/dom-util';
import { OptionsHelperService } from '../../shared/services/options-helper.service';
import { GenericNodeEntriesComponent } from './generic-node-entries/generic-node-entries.component';
import { WidgetConfigurationButtonsComponent } from '../shared/widget-configuration-buttons/widget-configuration-buttons.component';
import { GenericNodeEntriesDisplayType } from '../../shared/types/generic-node-entries-display-type';
import { ScrollHelperService } from '../../shared/services/scroll-helper.service';
import { DEFAULT_COLLECTION_ID_PROP } from '../../shared/types/custom-definitions';
import { ApplyFilterEvent } from '../../shared/types/apply-filter-event';
import { LayoutOption } from '../../shared/types/layout-option';
import { WidgetComponentInterface } from '../generic-widget/generic-widget.component';
import { GenericWidgetGlobalService } from '../generic-widget/generic-widget-global.service';

@Component({
    selector: 'es-content-teaser',
    encapsulation: ViewEncapsulation.Emulated,
    imports: [
        CommonModule,
        EduSharingUiCommonModule,
        FormsModule,
        GenericNodeEntriesComponent,
        MatButtonModule,
        MatButtonToggleModule,
        MatFormFieldModule,
        MatInputModule,
        TranslateModule,
        WidgetConfigurationButtonsComponent,
    ],
    providers: [
        {
            provide: OptionsHelperServiceAbstract,
            useClass: OptionsHelperService,
        },
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    templateUrl: './content-teaser.component.html',
    styleUrls: ['./content-teaser.component.scss'],
})
export class ContentTeaserComponent implements AfterViewInit, OnDestroy, WidgetComponentInterface {
    // INPUTS + OUTPUTS
    @Input() contextNodeId!: string;
    @Input() defaultNodeId: string = '';
    editMode: InputSignal<boolean> = input<boolean>(false);
    @Input() embedConfigurationOption?: ConfigurationOption;
    @Input() gridIndex: number = -1;
    @Input() layout: GenericNodeEntriesDisplayType = GenericNodeEntriesDisplayType.StandardView;
    @Input() nodeId?: string;
    @Input() pageVariantNode?: Node;
    @Input() propagatedNodeId?: string;
    searchInput: InputSignal<string> = input<string>(null);
    @Input() searchText: string;
    swimlaneColor: InputSignal<string> = input<string>(null);
    @Input() swimlaneIndex: number = -1;
    swimlaneShape: InputSignal<SwimlaneBackgroundShape> = input<SwimlaneBackgroundShape>(
        SwimlaneBackgroundShape.None,
    );
    swimlaneShapeSelected = computed((): boolean => {
        return (
            this.swimlaneShape() !== undefined &&
            this.swimlaneShape() !== SwimlaneBackgroundShape.None
        );
    });

    @Output() configChanged: EventEmitter<void> = new EventEmitter<void>();
    @Output() embedWidgetClicked: EventEmitter<void> = new EventEmitter<void>();
    @Output() itemClickedEvent: EventEmitter<Node> = new EventEmitter<Node>();
    @Output() nodeStatisticsChanged: EventEmitter<StatisticNode[]> = new EventEmitter<
        StatisticNode[]
    >();
    @Output() totalSearchResultCountChanged: EventEmitter<number> = new EventEmitter<number>();

    // VARIABLES
    blacklistedNodeIds: string[] = [];
    criteria: Signal<MdsQueryCriteria[]> = computed((): MdsQueryCriteria[] => {
        const criteriaArray: MdsQueryCriteria[] = [];
        // if no propertyFilters were defined yet, set collectionIdKey to [COLLECTION_ID] to search for the collection
        if (!Object.keys(this.propertyFilters())?.length) {
            criteriaArray.push({
                property: DEFAULT_COLLECTION_ID_PROP,
                values: [this.contextNodeId],
            });
            // also push the search or input text if it exists
            const text: string = (this.searchText ?? '').trim();
            const inputVal: string = (this.searchInput() ?? '').trim();
            const value: string = text ? (inputVal ? `${text} ${inputVal}` : text) : inputVal;
            if (value) {
                criteriaArray.push({
                    property: this.searchMode(),
                    values: [value],
                });
            }
        }
        // include search if a search term or input is defined (must be a non-empty string -> check for a truthy value)
        // reference: https://stackoverflow.com/a/154068
        // note: the search term is later not included in the propertyFilters
        else if (this.searchMode() === 'ngsearchword' && (this.searchText || this.searchInput())) {
            const text: string = (this.searchText ?? '').trim();
            const inputVal: string = (this.searchInput() ?? '').trim();

            const value: string = text ? (inputVal ? `${text} ${inputVal}` : text) : inputVal;

            criteriaArray.push({
                property: this.searchMode(),
                values: [value],
            });
        }
        // special cases for propagating parent: replace the collectionId
        const propagatedWidget: boolean = this.propagatedNodeId && !this.nodeId;
        // check if the propertyFilter contains the collectionId and replace it
        const containsCollectionId: boolean =
            !!this.propertyFilters()?.[DEFAULT_COLLECTION_ID_PROP];
        if (propagatedWidget && containsCollectionId) {
            this.propertyFilters()[DEFAULT_COLLECTION_ID_PROP] = [this.contextNodeId];
        }
        // filter and convert the propertyFilters object and push the result into the criteriaArray
        criteriaArray.push(
            ...this.searchHelperService.convertCritieria(this.propertyFilters(), []),
        );
        return criteriaArray;
    });
    initialized: WritableSignal<boolean> = signal(false);
    layoutOptions: LayoutOption[] = [
        {
            ariaLabel: 'SINGLE_VIEW_ARIA',
            icon: 'svg-view_carousel',
            value: GenericNodeEntriesDisplayType.SingleView,
            viewValue: 'SINGLE_VIEW',
        },
        {
            ariaLabel: 'SPLIT_VIEW_ARIA',
            icon: 'svg-view_carousel_split',
            value: GenericNodeEntriesDisplayType.SplitView,
            viewValue: 'SPLIT_VIEW',
        },
        {
            ariaLabel: 'STANDARD_VIEW_ARIA',
            icon: 'svg-view_standard',
            value: GenericNodeEntriesDisplayType.StandardView,
            viewValue: 'STANDARD_VIEW',
        },
        {
            ariaLabel: 'COMPACT_VIEW_ARIA',
            icon: 'svg-view_compact',
            value: GenericNodeEntriesDisplayType.CompactView,
            viewValue: 'COMPACT_VIEW',
        },
        {
            ariaLabel: 'LIST_VIEW_ARIA',
            icon: 'svg-view_list',
            value: GenericNodeEntriesDisplayType.ListView,
            viewValue: 'LIST_VIEW',
        },
    ];
    private propertyFilters: WritableSignal<Values> = signal({});
    queryId: Signal<string> = computed((): string =>
        this.searchMode() === 'ngsearchword' ? 'ngsearch' : 'wlo_collection',
    );
    private searchMode: WritableSignal<'ngsearchword' | 'collection'> = signal('ngsearchword');
    private sortActive: WritableSignal<string> = signal<string>('cm:created');
    private sortDirection: WritableSignal<string> = signal<string>('asc');
    totalSearchResultCount: number = -1;
    updateInProgress: WritableSignal<boolean> = signal(false);
    private windowRef: Window | null = null;

    constructor(
        private genericWidgetGlobalService: GenericWidgetGlobalService,
        private scrollHelperService: ScrollHelperService,
        private searchHelperService: SearchHelperService,
        private topicPageHelperService: TopicPageHelperService,
        private translate: TranslateService,
    ) {
        if (
            this.genericWidgetGlobalService.hasCustomDisplayType(
                GenericNodeEntriesDisplayType.MapView,
            )
        ) {
            this.layoutOptions.push({
                ariaLabel: 'MAP_VIEW_ARIA',
                icon: 'svg-view_map',
                value: GenericNodeEntriesDisplayType.MapView,
                viewValue: 'MAP_VIEW',
            });
        }
    }

    /**
     * Initializes event listener (the APPLY_FILTER event is returned by the editorial desk).
     */
    ngAfterViewInit(): void {
        // note: the arrow function is necessary to correctly access "this."
        window.addEventListener('message', this.handleApplyFilter, false);
    }

    /**
     * On destruction, remove the event listener previously added.
     */
    ngOnDestroy(): void {
        window.removeEventListener('message', this.handleApplyFilter, false);
    }

    /**
     * Adds or removes a given nodeId from the blacklist and persists it to the node.
     */
    changedBlacklist(nodeId: string): void {
        if (!this.blacklistedNodeIds.includes(nodeId)) {
            // workaround to ensure that ngOnChanges in triggered
            // previous solution: this.blacklistedNodeIds.push(nodeId);
            this.blacklistedNodeIds = [...this.blacklistedNodeIds, nodeId];
        } else {
            // workaround to ensure that ngOnChanges in triggered
            // previous solution: this.blacklistedNodeIds.splice(this.blacklistedNodeIds.indexOf(nodeId), 1);
            this.blacklistedNodeIds = this.blacklistedNodeIds.filter(
                (id: string): boolean => id !== nodeId,
            );
        }
        this.configChanged.emit();
    }

    /**
     * @Deprecated
     * @TODO
     * Opens the repository content URL using the previously set propertyFilters and searchText.
     *
     */
    openRepoContentUrl(): void {
        const propertyFilters: Values = this.propertyFilters();
        // if no propertyFilters were defined yet, set collectionIdKey to [COLLECTION_ID] to filter for the collection
        if (!Object.keys(propertyFilters)?.length) {
            propertyFilters[DEFAULT_COLLECTION_ID_PROP] = [this.contextNodeId];
        }
        // 'search' opens the search buffet
        propertyFilters['virtual:audit_filter'] = ['search'];
        const stringifiedFilters: string = JSON.stringify(propertyFilters);
        const params = {
            applyFilter: true,
            filters: stringifiedFilters,
            fromMds: true,
            mode: 'audit',
            q: this.searchMode() === 'ngsearchword' ? this.searchText : '',
            resetGroup: true,
            sortActive: this.sortActive(),
            sortDirection: this.sortDirection(),
            title: 'Inhalte-Buffets',
        };
        this.windowRef = window.open(
            this.topicPageHelperService.getBaseHref() +
                'components/editorial-desk?' +
                createQueryString(params),
            '_blank',
        );
        if (!this.windowRef) {
            console.warn(this.translate.instant('TOPIC_PAGE.WIDGET.CONTENT_TEASER.POPUP_BLOCKED'));
        }
    }

    /**
     * Handles the embedding of the widget by emitting an embed widget clicked event.
     */
    embedWidget(): void {
        this.embedWidgetClicked.emit();
    }

    /**
     * Handles the change of the layout by emitting a config changed event.
     */
    changeLayout(): void {
        this.configChanged.emit();
    }

    /**
     * Restores the scroll position (relative to the bottom) after changing the layout.
     */
    layoutChangeAction(): void {
        this.scrollHelperService.restoreScrollPosition();
    }

    /**
     * Reacts to wlo-node-entries (itemClicked) event and emits it.
     *
     * @param node
     */
    itemClicked(node: Node): void {
        this.itemClickedEvent.emit(node);
    }

    /**
     * Reacts to wlo-node-entries (nodeStatisticsChanged) event and emits it.
     *
     * @param statistics
     */
    changeNodeStatistics(statistics: StatisticNode[]): void {
        this.nodeStatisticsChanged.emit(statistics);
    }

    /**
     * Reacts to wlo-node-entries (totalSearchResultCountChanged) event and emits it.
     *
     * @param count
     */
    changeTotalSearchResultCount(count: number): void {
        this.totalSearchResultCount = count;
        this.totalSearchResultCountChanged.emit(count);
    }

    // noinspection JSUnusedGlobalSymbols
    /**
     * Called by generic-widget component to retrieve a widget config from the currently set variables in the component.
     */
    retrieveWidgetConfig(): ContentTeaserConfig {
        return {
            blacklistedNodeIds: this.blacklistedNodeIds,
            contentTeaserLayout: this.layout,
            propertyFilters: this.propertyFilters(),
            searchMode: this.searchMode(),
            searchText: this.searchText ?? '',
            sortActive: this.sortActive(),
            sortDirection: this.sortDirection(),
        };
    }

    // noinspection JSUnusedGlobalSymbols
    /**
     * Called by generic-widget component to set widget-specific values.
     *
     * @param config
     */
    setWidgetValues(config: ContentTeaserConfig): void {
        if (config.blacklistedNodeIds) {
            this.blacklistedNodeIds = config.blacklistedNodeIds;
        }
        // 0 is a valid enum value, so check for undefined
        if (config.contentTeaserLayout !== undefined) {
            this.layout = config.contentTeaserLayout;
        }
        if (config.propertyFilters) {
            this.propertyFilters.set(config.propertyFilters);
        }
        if (config.searchMode) {
            this.searchMode.set(config.searchMode);
        }
        // an empty string is also valid (i.e., overwrite search text if it is somehow defined)
        if (config.searchText !== undefined) {
            this.searchText = config.searchText;
        }
        if (config.sortActive) {
            this.sortActive.set(config.sortActive);
        }
        if (config.sortDirection) {
            this.sortDirection.set(config.sortDirection);
        }
    }

    // HELPERS
    /**
     * Helper function to handle the receiving of the APPLY_FILTER event.
     *
     * @param event
     */
    private handleApplyFilter = async (event: MessageEvent<any>): Promise<void> => {
        // APPLY_FILTER event was received, and windowRef must exist and not being closed yet (as multiple listeners might exist)
        if (event.data.event === 'APPLY_FILTER' && !!this.windowRef && !this.windowRef.closed) {
            // close the separate tab
            this.windowRef.close();
            // reset the reference to be not called twice
            this.windowRef = null;
            // parse the event data
            const resultData: ApplyFilterEvent = JSON.parse(event.data.data);
            console.info(
                this.translate.instant('TOPIC_PAGE.WIDGET.CONTENT_TEASER.FILTER_EVENT_RECEIVED'),
                resultData,
            );
            // set searchText and propertyFilters
            this.searchText = resultData.searchString;
            this.propertyFilters.set(resultData.propertyFilters);
            // emit config changed event
            this.configChanged.emit();
        }
    };
}
