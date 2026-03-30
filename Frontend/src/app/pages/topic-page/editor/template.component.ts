import { CdkAccordionItem, CdkAccordionModule } from '@angular/cdk/accordion';
import { Clipboard } from '@angular/cdk/clipboard';
import { moveItemInArray } from '@angular/cdk/drag-drop';
import { PlatformLocation } from '@angular/common';
import {
    AfterViewInit,
    Component,
    computed,
    CUSTOM_ELEMENTS_SCHEMA,
    ElementRef,
    HostBinding,
    Input,
    OnDestroy,
    OnInit,
    QueryList,
    Signal,
    signal,
    TemplateRef,
    ViewChild,
    ViewChildren,
    WritableSignal,
} from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { ActivatedRoute, Params, Router, UrlTree } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import {
    CONTENT_TYPE_ALL,
    DEFAULT,
    HOME_REPOSITORY,
    MdsService,
    MdsWidget,
    Node,
    NodeEntries,
    ParentEntries,
    PROPERTY_FILTER_ALL,
    SearchResults,
    SearchService,
} from 'ngx-edu-sharing-api';
import { ChatCompletionResult, NodeConfig } from 'ngx-edu-sharing-b-api';
import {
    Constrain,
    CustomOptions,
    DefaultGroups,
    ElementType,
    Helper,
    OptionItem,
    OptionsHelperDataService,
    Scope,
    TranslationsService,
    UIConstants,
    Values,
} from 'ngx-edu-sharing-ui';
import { firstValueFrom, Observable, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, startWith, takeUntil } from 'rxjs/operators';
import { v4 as uuidv4 } from 'uuid';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { RestConnectorService } from '../../../core-module/rest/services/rest-connector.service';
import { Closable } from '../../../features/dialogs/card-dialog/card-dialog-config';
import { CardDialogRef } from '../../../features/dialogs/card-dialog/card-dialog-ref';
import {
    DELETE_OR_CANCEL,
    USE_OR_CANCEL,
    YES_OR_NO,
} from '../../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { QrDialogModule } from '../../../features/dialogs/dialog-modules/qr-dialog/qr-dialog.module';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { PreviewSidebarModule } from '../../../features/preview-sidebar/preview-sidebar.module';
import { PreviewSidebarService } from '../../../features/preview-sidebar/preview-sidebar.service';
import { MainNavService, TemplateSlot } from '../../../main/navigation/main-nav.service';
import {
    SearchEvent,
    SearchFieldService,
} from '../../../main/navigation/search-field/search-field.service';
import { SharedModule } from '../../../shared/shared.module';
import { VarDirective } from '../shared/directives/ng-var.directive';
import { FilterSwimlaneTypePipe } from '../shared/pipes/filter-swimlane-type.pipe';
import { AiTextPromptPipe } from '../shared/pipes/ai-text-prompt.pipe';
import { SwimlaneSearchCountPipe } from '../shared/pipes/swimlane-search-count.pipe';
import { FilterVisibleSwimlanePipe } from '../shared/pipes/filter-swimlane-hits.pipe';
import { AiHelperService } from '../shared/services/ai-helper.service';
import { TopicPageEventsService } from '../shared/services/topic-page-events.service';
import {
    CustomSideMenuItem,
    TopicPageGlobalService,
} from '../shared/services/topic-page-global.service';
import { TopicPageHelperService } from '../shared/services/topic-page-helper.service';
import {
    DEFAULT_AI_CONFIG_PROP,
    DEFAULT_ICON_PATH,
    DEFAULT_PAGE_CONFIG_ASPECT,
    DEFAULT_PAGE_CONFIG_PROP,
    DEFAULT_PAGE_CONFIG_PROPAGATE_REF_PROP,
    DEFAULT_PAGE_CONFIG_REF_PROP,
    DEFAULT_PAGE_NAME_PREFIX,
    DEFAULT_PAGE_VARIANT_CONFIG_ASPECT,
    DEFAULT_PAGE_VARIANT_CONFIG_PROP,
    DEFAULT_PAGE_VARIANT_IS_TEMPLATE_PROP,
    DEFAULT_PAGE_VARIANT_NAME_PREFIX,
    DEFAULT_PAGE_VARIANT_QUERY_ID,
    DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION,
    DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION_PROP,
    DEFAULT_WIDGET_CONFIG_PROP,
    DEFAULT_WIDGET_NAME_PREFIX,
    SWIMLANE_SHAPE_TO_IMAGE_MAPPING,
    SWIMLANE_TYPE_OPTIONS,
    WIDGET_TYPE,
    WIDGETS,
} from '../shared/types/custom-definitions';
import { BapiConfigObject } from '../shared/types/bapi-config-object';
import { ColorChangeEvent } from '../shared/types/color-change-event';
import { GridTile } from '../shared/types/grid-tile';
import { GridTileToHitsMapping } from '../shared/types/grid-tile-to-hits-mapping';
import { GridTileToSearchResultsMapping } from '../shared/types/grid-tile-to-search-results-mapping';
import { PageConfig } from '../shared/types/page-config';
import { PageVariantConfig } from '../shared/types/page-variant-config';
import { PromptToTextMapping } from '../shared/types/prompt-to-text-mapping';
import { Swimlane } from '../shared/types/swimlane';
import { SwimlaneBackgroundShape } from '../shared/types/swimlane-background-shape';
import { WidgetConfigObject } from '../shared/types/widget-config-object';
import { WidgetNodeAddedEvent } from '../shared/types/widget-node-added-event';
import {
    containsAiTags,
    retrieveChatCompletionObject,
    retrieveResultString,
} from '../shared/utils/ai-util';
import { checkUserAccess } from '../shared/utils/node-util';
import {
    addNodeIdToPageVariantConfig,
    convertNodeRefIntoNodeId,
    preparePageVariantConfig,
    prependWorkspacePrefix,
    retrieveAiConfigFromNode,
    retrieveNodeId,
    retrievePageConfig,
    retrievePageConfigPropagateRef,
    retrievePageConfigRef,
    retrievePageVariantConfig,
    retrievePromptFromAiConfig,
    retrieveTopicColor,
} from '../shared/utils/template-util';
import { BreadcrumbComponent } from '../widgets/breadcrumb/breadcrumb.component';
import { GenericWidgetComponent } from '../widgets/generic-widget/generic-widget.component';
import { GenericWidgetGlobalService } from '../widgets/generic-widget/generic-widget-global.service';
import { ProfilingComponent } from '../widgets/profiling/profiling.component';
import { ColorPickerComponent } from '../widgets/shared/color-picker/color-picker.component';
import { EditableTextComponent } from '../widgets/shared/editable-text/editable-text.component';
import { SideMenuItemComponent } from '../widgets/side-menu-wrapper/side-menu-item/side-menu-item.component';
import { SideMenuWrapperComponent } from '../widgets/side-menu-wrapper/side-menu-wrapper.component';
import { TopicHeaderComponent } from '../widgets/topic-header/topic-header.component';
import {
    AddPageVariantDialogComponent,
    CopyOption,
} from './add-page-variant-dialog/add-page-variant-dialog.component';
import { AddSwimlaneBorderButtonComponent } from './add-swimlane-button/add-swimlane-border-button.component';
import { ConfigurePageVariantComponent } from './configure-page-variant/configure-page-variant.component';
import { SwimlaneComponent } from './swimlane/swimlane.component';
import { SwimlaneSettingsDialogComponent } from './swimlane/swimlane-settings-dialog/swimlane-settings-dialog.component';
import { SwimlaneConfigurationButtonsComponent } from './swimlane-configuration-buttons/swimlane-configuration-buttons.component';
import { TopicPageFiltersSidebarComponent } from './topic-page-filters-sidebar/topic-page-filters-sidebar.component';

@Component({
    imports: [
        AddPageVariantDialogComponent,
        AddSwimlaneBorderButtonComponent,
        AiTextPromptPipe,
        BreadcrumbComponent,
        CdkAccordionModule,
        ColorPickerComponent,
        ConfigurePageVariantComponent,
        EditableTextComponent,
        FilterSwimlaneTypePipe,
        FilterVisibleSwimlanePipe,
        GenericWidgetComponent,
        PreviewSidebarModule,
        ProfilingComponent,
        QrDialogModule,
        SharedModule,
        SideMenuItemComponent,
        SideMenuWrapperComponent,
        SwimlaneComponent,
        SwimlaneConfigurationButtonsComponent,
        SwimlaneSearchCountPipe,
        SwimlaneSettingsDialogComponent,
        TopicHeaderComponent,
        TopicPageFiltersSidebarComponent,
        TranslateModule,
        VarDirective,
    ],
    providers: [
        OptionsHelperDataService,
        SearchService,
        TopicPageHelperService,
        TopicPageEventsService,
    ],
    selector: 'es-template-page',
    templateUrl: './template.component.html',
    styleUrls: ['./template.component.scss'],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class TemplateComponent implements AfterViewInit, OnDestroy, OnInit {
    readonly ACCORDION_TYPE: string = SWIMLANE_TYPE_OPTIONS.find(
        (o) => o.viewValue === 'ACCORDION_ELEMENT',
    )?.value;
    private readonly ANCHOR_ITEM_CSS_PROPERTY: string = '--anchor-item-bg-color';
    readonly ANCHOR_TYPE: string = SWIMLANE_TYPE_OPTIONS.find((o) => o.viewValue === 'ANCHOR_MENU')
        ?.value;
    readonly CONTAINER_TYPE: string = SWIMLANE_TYPE_OPTIONS.find(
        (o) => o.viewValue === 'CONTAINER_ELEMENT',
    )?.value;
    readonly i18nPrefix: string = 'TOPIC_PAGE.';
    private readonly createPageVariantTitle: string =
        this.i18nPrefix + 'NAVIGATION.NEW_PAGE_VARIANT';
    readonly SWIMLANE_ID_PREFIX: string = 'swimlane-';
    private readonly TOPIC_COLOR_CSS_PROPERTY: string = '--topic-color';

    constructor(
        private aiHelperService: AiHelperService,
        private clipboard: Clipboard,
        private connector: RestConnectorService,
        private dialogs: DialogsService,
        private elementRef: ElementRef,
        private genericWidgetGlobalService: GenericWidgetGlobalService,
        private mainNavService: MainNavService,
        private mdsService: MdsService,
        private optionsHelperService: OptionsHelperDataService,
        private platformLocation: PlatformLocation,
        private previewSidebarService: PreviewSidebarService,
        private route: ActivatedRoute,
        private router: Router,
        private searchFieldService: SearchFieldService,
        private searchService: SearchService,
        private topicPageEventsService: TopicPageEventsService,
        private topicPageGlobalService: TopicPageGlobalService,
        private topicPageHelperService: TopicPageHelperService,
        private translate: TranslateService,
        private translationsService: TranslationsService,
    ) {
        // wait for the login to be ready
        this.connector.isLoggedIn().subscribe((): void => {
            // check for administration privileges
            this.isAdmin.set(this.connector.getCurrentLogin()?.isAdmin ?? false);
        });
        // subscribe to changes on the search input + search filters
        this.searchInputSubject
            .pipe(debounceTime(200), distinctUntilChanged(), takeUntil(this.destroyed$))
            .subscribe((searchInput: string) => {
                this.searchInput.set(searchInput);
                this.previewSidebarService.handleNodeClick(null);
            });
        this.searchFiltersSubject
            .pipe(debounceTime(500), distinctUntilChanged(), takeUntil(this.destroyed$))
            .subscribe((searchFilters: Values) => {
                this.searchFilters.set(searchFilters);
                this.previewSidebarService.handleNodeClick(null);
            });
        // subscribe to changes on the sidebar opening state
        this.previewSidebarService
            .getOpenState()
            .pipe(takeUntil(this.destroyed$))
            .subscribe((isOpen: boolean): void => {
                this.sidebarOpen.set(isOpen);
            });
        if (this.topicPageGlobalService.getCustomSideMenuItems()) {
            this.customSideMenuItems.set(this.topicPageGlobalService.getCustomSideMenuItems());
        }
        this.hasCustomBreadcrumbExtension.set(
            this.topicPageGlobalService.hasCustomBreadcrumbExtension(),
        );
        // the sidebar should be hidden when it is configured and a touch event is detected
        // TODO: this only works if a touch event is detected
        // if (this.topicPageGlobalService.getSidebarMobileHidden()) {
        //     this.uiService.isTouchSubject
        //         .pipe(takeUntil(this.destroyed$))
        //         .subscribe((isTouch: boolean): void => {
        //             this.sidebarMobileHidden.set(isTouch);
        //         });
        // }
        this.sidebarMobileHidden.set(this.topicPageGlobalService.getSidebarMobileHidden());
        // listen to changes in the selected variables and reload the pageVariants, if necessary
        this.topicPageHelperService
            .getSelectedVariables$()
            .pipe(takeUntil(this.destroyed$))
            .subscribe((): void => {
                if (this.initialLoadSuccessfully() && this.latestParams) {
                    try {
                        void this.retrievePageConfigAndSelectVariant(
                            this.latestParams.variantId,
                            true,
                        );
                    } catch (err) {
                        console.error(err);
                        this.topicPageHelperService.displayErrorToast();
                    }
                }
            });
    }

    @Input() collectionId: string;
    @Input() variantId: string;
    initialTopicColor: string;
    @HostBinding('style.--topic-color') topicColor: string;
    @ViewChild('addPageVariantDialog') addPageVariantRef: TemplateRef<undefined>;
    @ViewChild('editModeToggle') editModeToggle: TemplateRef<any>;
    @ViewChild('editSwimlaneDialog') editSwimlaneRef: TemplateRef<undefined>;
    @ViewChild('showQrCodeDialog') showQrCodeDialogRef: TemplateRef<undefined>;
    @ViewChildren('accordionItem') accordions: QueryList<CdkAccordionItem>;

    initialLoadSuccessfully: WritableSignal<boolean> = signal(false);
    requestInProgress: WritableSignal<boolean> = signal(false);
    private initializedWithParams: boolean = false;
    private readonly destroyed$ = new Subject<void>();

    userHasEditRights: WritableSignal<boolean> = signal(false);
    isAdmin: WritableSignal<boolean> = signal(false);
    editMode: WritableSignal<boolean> = signal(false);
    filterPanelOpen: WritableSignal<boolean> = signal(false);
    sidebarOpen: WritableSignal<boolean> = signal(false);
    customSidebarOptions: CustomOptions = {
        useDefaultOptions: true,
        supportedOptions: ['OPTIONS.DEBUG', 'OPTIONS.DOWNLOAD'],
        addOptions: [],
    };
    templateMode: WritableSignal<boolean> = signal(false);

    topic: WritableSignal<string> = signal('');
    topicCollectionID: WritableSignal<string> = signal(null);
    aiSupported: WritableSignal<boolean> = signal(false);

    collectionNode: Node;
    collectionNodePageConfigRef: string;
    collectionNodePagePropagateConfigRef: string;
    parentEntries: WritableSignal<ParentEntries> = signal(null);
    propagatingParentNode: Node;
    convertedBreadcrumbNodeId: Signal<string> = computed((): string =>
        convertNodeRefIntoNodeId(this.breadcrumbNodeId()),
    );
    convertedHeaderNodeId: Signal<string> = computed((): string =>
        convertNodeRefIntoNodeId(this.headerNodeId()),
    );
    breadcrumbNodeId: WritableSignal<string> = signal(null);
    headerNodeId: WritableSignal<string> = signal(null);
    pageConfigNode: Node;
    pageConfigCheckFailed: WritableSignal<boolean> = signal(false);
    defaultPageVariantNodes: Node[] | Partial<Node>[];
    selectedDefaultConfigNode: Node;
    createCustomConfigInProgress: WritableSignal<boolean> = signal(false);
    pageVariantConfigs: NodeEntries;
    private pageVariantDefaultPosition: number = -1;
    pageVariantNode: Node;
    pageVariantNodeIndex: number = 0;
    pageVariantSettingsValid: WritableSignal<boolean> = signal(true);
    pageVariantCreateDialogRef: CardDialogRef;
    pageVariantCreateDialogSelectedNode: Node;
    pageVariantCreateDialogCopyOption: CopyOption;
    pageVariantReloadNecessary: boolean = false;
    qrCodeDialogRef: CardDialogRef;
    qrCodeUrl: WritableSignal<string> = signal('');
    selectedVariantPosition: number = -1;
    showLoadingScreen: Signal<boolean> = computed(
        (): boolean =>
            !this.pageConfigCheckFailed() &&
            (!this.initialLoadSuccessfully() || this.requestInProgress()),
    );
    anchorTrigger: number = 1;
    @HostBinding('style.--anchor-item-bg-color') anchorItemColor: string;
    initialAnchorItemColor: string;
    swimlanes: Swimlane[] = [];
    swimlaneToEditForm: UntypedFormGroup;
    swimlaneIdToPromptTextMapping: Map<string, PromptToTextMapping> = new Map<
        string,
        PromptToTextMapping
    >();
    topicWidgets: NodeEntries;

    latestParams: Params;
    private initialFragmentScrollPerformed: boolean = false;
    private latestUrlFragment: string;
    selectDimensions: Map<string, MdsWidget> = new Map<string, MdsWidget>();

    hasCustomBreadcrumbExtension: WritableSignal<boolean> = signal(false);
    customSideMenuItems = signal<CustomSideMenuItem[]>([]);
    customSideMenuItemsBefore = computed(() =>
        this.customSideMenuItems().filter((item) => item.position === 'before'),
    );
    customSideMenuItemsAfter = computed(() =>
        this.customSideMenuItems().filter((item) => item.position === 'after'),
    );
    sidebarMobileHidden: WritableSignal<boolean> = signal(false);
    selectedMenuItem: string = '';

    searchEvent$: Observable<SearchEvent>;
    searchCountTrigger: number = 1;
    computedSearchInput: Signal<string> = computed((): string =>
        this.editMode() ? '' : this.searchInput(),
    );
    computedSearchFilters: Signal<Values> = computed(
        (): Values => (this.editMode() ? {} : this.searchFilters()),
    );
    searchInputOrFiltersDefined: Signal<boolean> = computed(() => {
        return (
            this.computedSearchInput() !== '' ||
            (this.computedSearchFilters() && Object.keys(this.computedSearchFilters())?.length > 0)
        );
    });
    private searchInput: WritableSignal<string> = signal('');
    private searchFilters: WritableSignal<Values> = signal({});
    private searchInputSubject: Subject<string> = new Subject<string>();
    private searchFiltersSubject: Subject<Values> = new Subject<Values>();
    searchUrl: string = '';
    swimlaneIdToHitMatching: Map<string, boolean> = new Map<string, boolean>();
    swimlaneTitleIdToHitMatching: Map<string, boolean> = new Map<string, boolean>();
    firstMatchingSwimlaneId: string;
    lastMatchingSwimlaneId: string;

    /**
     * Initializes the component by setting general defaults and retrieving the collection node,
     * page config and statistics.
     */
    async ngOnInit(): Promise<void> {
        // @TODO
        // retrieve the custom sidebar options (note: options with the same name are filtered out)
        // note:
        // * copyLink and qrCode should be added twice, so different names have to be used
        // * the (default) qr code and report problem options cannot be used, as specific positioning is required
        this.customSidebarOptions.addOptions = [
            this.topicPageHelperService.retrieveCustomOption(
                'copyLink',
                DefaultGroups.Primary,
                0,
                'TOPIC_PAGE.WIDGET.SHARE_OPTIONS.COPY_LINK_TO_CONTENT',
                'share',
            ),
            this.topicPageHelperService.retrieveCustomOption(
                'qrCode',
                DefaultGroups.Primary,
                1,
                'TOPIC_PAGE.SWIMLANE.QR_CODE.TOOLTIP',
            ),
            this.topicPageHelperService.retrieveCustomOption('copyLink', DefaultGroups.Primary, 2),
            this.topicPageHelperService.retrieveCustomOption('writeMail', DefaultGroups.Primary, 3),
            this.topicPageHelperService.retrieveCustomOption('qrCode', DefaultGroups.Primary, 4),
            this.topicPageHelperService.retrieveCustomOption(
                'reportProblem',
                DefaultGroups.FileOperations,
            ),
        ];
        // retrieve the search URL
        this.searchUrl = this.retrieveSearchUrl();
        // retrieve the AI support state
        this.aiSupported.set(await this.aiHelperService.hasAISupport());
        // check if the collectionId input is set
        if (this.collectionId) {
            // set the collection ID
            this.topicCollectionID.set(this.collectionId);
            // initialize the component
            void this.initializeComponent(this.variantId);
        }
        // set the topic based on the query param "collectionID"
        this.route.queryParams
            .pipe(filter((params: Params) => params.collectionId))
            .subscribe(async (params: Params): Promise<void> => {
                this.latestParams = params;
                // due to reload with queryParams, this might be called twice, thus, initializedWithParams is important
                if (params.collectionId && !this.initializedWithParams) {
                    // set the topicCollectionID
                    this.topicCollectionID.set(params.collectionId);
                    // initialize the component
                    await this.initializeComponent(params.variantId);
                }
            });
    }

    /**
     * After the view has been initialized, update the nav bars and register a search field.
     */
    async ngAfterViewInit(): Promise<void> {
        // read the default background color of anchor items
        const hostElement = this.elementRef.nativeElement;
        const computedStyle = getComputedStyle(hostElement);
        this.initialAnchorItemColor = computedStyle
            .getPropertyValue(this.ANCHOR_ITEM_CSS_PROPERTY)
            .trim();
        this.initialTopicColor = computedStyle
            .getPropertyValue(this.TOPIC_COLOR_CSS_PROPERTY)
            .trim();
        this.mainNavService.setMainNavConfig({
            currentScope: Scope.TopicPage,
            title: 'TOPIC_PAGE.NAVIGATION.TITLE',
            create: { allowed: true, allowBinary: true },
        });
        void this.addCustomMainNavOptions();
        // register the edit mode toggle button next to the create button
        this.mainNavService.registerCustomTemplateSlot(
            TemplateSlot.AfterCreateMenu,
            this.editModeToggle,
        );
        // enable the search field and observe the search event
        const mds = await firstValueFrom(
            this.mdsService.getMetadataSet({
                repository: HOME_REPOSITORY,
                metadataSet: this.genericWidgetGlobalService.getDefaultMds(),
            }),
        );
        this.searchFieldService.enable(
            {
                enableFiltersAndSuggestions: false,
                showFiltersButton: !!mds.views.find((v) => v.id === 'search_topic_page'),
                placeholder: 'TOPIC_PAGE.NAVIGATION.SEARCH_PLACEHOLDER',
            },
            this.destroyed$,
        );
        this.searchFieldService
            .observeCurrentInstance()
            .pipe(
                takeUntil(this.destroyed$),
                filter((i) => !!i),
            )
            .subscribe((instance) => {
                instance
                    .onFiltersButtonClicked()
                    .subscribe(() => this.filterPanelOpen.set(!this.filterPanelOpen()));
                this.searchEvent$ = instance.onSearchTriggered();
                this.searchEvent$
                    .pipe(
                        takeUntil(this.destroyed$),
                        startWith({
                            searchString: this.searchFieldService
                                .getCurrentInstance()
                                ?.getSearchString(),
                            cleared: false,
                        }),
                        distinctUntilChanged(),
                    )
                    .subscribe((event) => {
                        this.searchInputSubject.next(event.searchString);
                    });
            });
    }

    /**
     * On destruction, complete the subjects.
     */
    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
        this.mainNavService.unregisterCustomTemplateSlot(TemplateSlot.AfterCreateMenu);
    }

    /**
     * Initializes the component with an optionally given variant ID.
     *
     * @param variantId
     */
    private async initializeComponent(variantId?: string): Promise<void> {
        this.initializedWithParams = true;
        this.translationsService.initialize().subscribe(() => {});
        try {
            // fetch the collection node to set the topic name, color and check the user access
            this.collectionNode = await this.topicPageHelperService.getNode(
                this.topicCollectionID(),
            );
            this.topic.set(this.collectionNode.title ?? 'No topic defined');
            // retrieve parent entries
            const parentEntries: ParentEntries = await this.topicPageHelperService.getNodeParents(
                this.topicCollectionID(),
            );
            this.parentEntries.set(parentEntries);
            // check the user privileges for the collection node and initialize custom listeners
            this.userHasEditRights.set(checkUserAccess(this.collectionNode));
            if (this.userHasEditRights()) {
                this.initializeCustomEventListeners();
            }
            // retrieve the page config node and select the proper variant to define the breadcrumbNodeId, headerNodeId + swimlanes
            await this.retrievePageConfigAndSelectVariant(variantId);

            // initial load finished (page structure loaded)
            this.initialLoadSuccessfully.set(true);

            // listen to fragment changes to scroll given swimlane ID into view
            // note: setTimeout is necessary for view being loaded first
            this.initializeFragmentListener();

            // void this.addCustomMainNavOptions();
        } catch (err) {
            console.error(err);
            this.topicPageHelperService.displayErrorToast();
        }
    }

    /**
     * Initializes custom event listeners to react to events sent by the widgets (color change, widget node added).
     */
    private initializeCustomEventListeners(): void {
        // listen to swimlaneColorChanged event
        this.topicPageEventsService.swimlaneColorChanged
            .pipe(takeUntil(this.destroyed$))
            .subscribe(async (event: ColorChangeEvent): Promise<void> => {
                const color: string = event?.color ?? '';
                const pageVariantNode: Node = event?.pageVariantNode ?? null;
                const swimlaneIndex: number = event?.swimlaneIndex ?? -1;

                if (
                    color !== '' &&
                    pageVariantNode &&
                    retrieveNodeId(pageVariantNode) === retrieveNodeId(this.pageVariantNode) &&
                    swimlaneIndex > -1
                ) {
                    let changesNecessary: boolean =
                        this.swimlanes[swimlaneIndex]?.backgroundColor !== color;
                    if (changesNecessary) {
                        try {
                            // if necessary, create a new page config node
                            await this.checkForCustomPageNodeExistence();
                            const pageVariant: PageVariantConfig = this.retrievePageVariant();
                            this.swimlanes[swimlaneIndex].backgroundColor = color;
                            pageVariant.structure.swimlanes = this.swimlanes;
                            this.pageVariantNode =
                                await this.topicPageHelperService.setPropertyAndRetrieveUpdatedNode(
                                    retrieveNodeId(this.pageVariantNode),
                                    DEFAULT_PAGE_VARIANT_CONFIG_PROP,
                                    JSON.stringify(pageVariant),
                                );
                        } catch (err) {
                            console.error(err);
                            this.topicPageHelperService.displayErrorToast();
                        }
                    }
                }
            });

        // listen to widgetNodeAdded event
        this.topicPageEventsService.widgetNodeAdded
            .pipe(takeUntil(this.destroyed$))
            .subscribe(async (event: WidgetNodeAddedEvent): Promise<void> => {
                const pageVariantNode: Node = event?.pageVariantNode ?? null;
                const swimlaneIndex: number = event?.swimlaneIndex ?? -1;
                const gridIndex: number = event?.gridIndex ?? -1;
                const isBreadcrumbNode: boolean = event?.isBreadcrumbNode ?? false;
                const isHeaderNode: boolean = event?.isHeaderNode ?? false;
                const widget: WidgetConfigObject = event?.widget ?? null;

                const validParentVariant: boolean =
                    pageVariantNode &&
                    retrieveNodeId(pageVariantNode) === retrieveNodeId(this.pageVariantNode);
                const validSwimlaneIndex: boolean = swimlaneIndex > -1;
                const validGridIndex: boolean = gridIndex > -1;
                const validWidgetOrAiConfig: boolean =
                    (widget.widgetConfig && Object.keys(widget.widgetConfig)?.length > 0) ||
                    (widget.aiConfig && Object.keys(widget.aiConfig)?.length > 0);

                const validInputs: boolean = validGridIndex && validSwimlaneIndex;

                if (
                    validParentVariant &&
                    validWidgetOrAiConfig &&
                    (validInputs || isBreadcrumbNode || isHeaderNode)
                ) {
                    // if no page configuration exists yet, a config has to be created and a reload of the page is necessary
                    // this also creates the widget node, as it has to be added as children of the page variant node
                    const addedSuccessfully = await this.checkForCustomPageNodeExistence(
                        pageVariantNode,
                        swimlaneIndex,
                        gridIndex,
                        widget,
                        isHeaderNode,
                        isBreadcrumbNode,
                    );
                    // if no page node was created, the adding is not yet successfully, so updating is necessary
                    if (!addedSuccessfully) {
                        const pageVariant: PageVariantConfig = this.retrievePageVariant();
                        // create the widget node
                        const properties: { [key: string]: string } = {
                            [DEFAULT_WIDGET_CONFIG_PROP]: JSON.stringify(widget.widgetConfig),
                        };
                        if (widget.aiConfig && Object.keys(widget.aiConfig)?.length) {
                            properties[DEFAULT_AI_CONFIG_PROP] = JSON.stringify(widget.aiConfig);
                        }
                        let widgetNode: Node = await this.topicPageHelperService.createChild(
                            retrieveNodeId(this.pageVariantNode),
                            RestConstants.CCM_TYPE_MAP,
                            DEFAULT_WIDGET_NAME_PREFIX + uuidv4(),
                            null,
                            properties,
                        );
                        const convertedWidgetNodeId: string = prependWorkspacePrefix(
                            retrieveNodeId(widgetNode),
                        );
                        // modify breadcrumb nodeId
                        if (isBreadcrumbNode) {
                            pageVariant.structure.breadcrumbNodeId = convertedWidgetNodeId;
                            this.breadcrumbNodeId.set(pageVariant.structure.breadcrumbNodeId);
                            this.pageVariantNode =
                                await this.topicPageHelperService.setPropertyAndRetrieveUpdatedNode(
                                    retrieveNodeId(this.pageVariantNode),
                                    DEFAULT_PAGE_VARIANT_CONFIG_PROP,
                                    JSON.stringify(pageVariant),
                                );
                        }
                        // modify header nodeId
                        else if (isHeaderNode) {
                            pageVariant.structure.headerNodeId = convertedWidgetNodeId;
                            this.headerNodeId.set(pageVariant.structure.headerNodeId);
                            this.pageVariantNode =
                                await this.topicPageHelperService.setPropertyAndRetrieveUpdatedNode(
                                    retrieveNodeId(this.pageVariantNode),
                                    DEFAULT_PAGE_VARIANT_CONFIG_PROP,
                                    JSON.stringify(pageVariant),
                                );
                        }
                        // modify nodeId of swimlane grid tile
                        else if (
                            this.swimlanes?.[swimlaneIndex]?.grid?.[gridIndex] &&
                            pageVariant
                        ) {
                            this.swimlanes[swimlaneIndex].grid[gridIndex].nodeId =
                                convertedWidgetNodeId;
                            pageVariant.structure.swimlanes = this.swimlanes;
                            this.pageVariantNode =
                                await this.topicPageHelperService.setPropertyAndRetrieveUpdatedNode(
                                    retrieveNodeId(this.pageVariantNode),
                                    DEFAULT_PAGE_VARIANT_CONFIG_PROP,
                                    JSON.stringify(pageVariant),
                                );
                        }
                    }
                }
            });
    }

    /**
     * Initializes a listener for the fragment part of the current route.
     * If the fragment part changes, the corresponding element is scrolled into view.
     */
    private initializeFragmentListener(): void {
        this.route.fragment.subscribe((urlFragment: string): void => {
            this.latestUrlFragment = urlFragment;
            // avoid attempting to scroll when no fragment is specified
            if (!this.latestUrlFragment) {
                this.initialFragmentScrollPerformed = true;
                return;
            }
            // directly scroll into view if the page is already loaded
            if (this.initialFragmentScrollPerformed) {
                this.scrollElementIntoView();
            }
            // otherwise, wait 3 seconds before trying to scroll into view
            // TODO: this should be replaced by waiting for all widgets being loaded
            else {
                setTimeout((): void => {
                    this.performScrollAttempts();
                }, 3000);
            }
        });
    }

    /**
     * Performs up to 10 scroll attempts (1 per second) until scrollElementIntoView returns true.
     */
    private performScrollAttempts(): void {
        let attemptCount = 0;
        const maxAttempts = 10;

        const scrollInterval = setInterval(() => {
            attemptCount++;

            // attempt to scroll
            const scrollSuccessully = this.scrollElementIntoView();

            // when successful, stop the interval
            if (scrollSuccessully) {
                this.initialFragmentScrollPerformed = true;
                clearInterval(scrollInterval);
            }
            // also stop after reaching the maximum of attempts
            else if (attemptCount >= maxAttempts) {
                clearInterval(scrollInterval);
            }
        }, 1000);
    }

    /**
     * Scrolls the latest URL fragment into view if it exists.
     */
    private scrollElementIntoView(): boolean {
        if (!this.latestUrlFragment) {
            return false;
        }
        const element: HTMLElement = document.getElementById(this.latestUrlFragment);
        if (element) {
            const topBarElement = document.querySelector('.topBar') as HTMLElement;
            const navbarHeight = topBarElement ? topBarElement.offsetHeight : 100;
            const elementPosition = element.getBoundingClientRect().top + window.pageYOffset;
            const offsetPosition = elementPosition - navbarHeight;

            window.scrollTo({
                top: offsetPosition,
                behavior: 'smooth',
            });
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets the search filters to a given value.
     *
     * @param filters
     */
    applySearchFilters(filters: Values): void {
        this.searchFiltersSubject.next(filters);
    }

    // PAGE CONFIG + VARIANT SPECIFIC FUNCTIONS
    /**
     * Retrieves the page config node, selects the proper variant and defines the breadcrumbNodeId, headerNodeId + swimlanes.
     *
     * @param variantId
     * @param forceReload
     */
    private async retrievePageConfigAndSelectVariant(
        variantId: string = '',
        forceReload: boolean = false,
    ): Promise<void> {
        // idea: if "collectionNode" already has a page config, there is no need to search further
        if (this.collectionNode.properties[DEFAULT_PAGE_CONFIG_PROP] && !this.templateMode()) {
            this.pageConfigNode = this.collectionNode;
            this.collectionNodePageConfigRef =
                this.collectionNode.properties[DEFAULT_PAGE_CONFIG_PROP]?.[0];
        }
        // retrieve the page config node either by checking the node itself or by iterating the parents of the collectionNode
        else if (!retrieveNodeId(this.pageConfigNode)) {
            this.pageConfigNode = await this.retrievePageConfigNode(this.collectionNode);
            if (!this.pageConfigNode) {
                return;
            }
        }
        // parse the page config from the properties
        const pageConfig: PageConfig = retrievePageConfig(this.pageConfigNode);
        if (!pageConfig.variants) {
            console.error('pageConfig does not include variants', pageConfig);
            return;
        }
        // check whether the collection node has a page propagate config
        const pageConfigPropagateRef: string = retrievePageConfigPropagateRef(this.collectionNode);
        if (pageConfigPropagateRef) {
            this.collectionNodePagePropagateConfigRef = pageConfigPropagateRef;
        }
        // page config propagate ref does not yet exist
        else {
            // add a create page template option to the "new" button
            const createTemplateName: string = 'TOPIC_PAGE.NAVIGATION.NEW_PAGE_TEMPLATE';
            const createPageTemplate = new OptionItem(
                createTemplateName,
                'edu-add_page_template',
                async () => {
                    const dialogRef = await this.dialogs.openGenericDialog({
                        title: this.i18nPrefix + 'ADD_PAGE_TEMPLATE.LABEL',
                        message: this.i18nPrefix + 'ADD_PAGE_TEMPLATE.MESSAGE',
                        buttons: YES_OR_NO,
                        closable: Closable.Casual,
                    });
                    const response = await firstValueFrom(dialogRef.afterClosed());
                    if (response === 'YES') {
                        void this.createAndVisitEmptyPageTemplate();
                    }
                },
            );
            createPageTemplate.elementType = [ElementType.Unknown];
            createPageTemplate.group = DefaultGroups.Toggles;
            createPageTemplate.priority = 11;
            createPageTemplate.constrains = [Constrain.Admin];
            // retrieve existing options from main nav config to extend them
            const currentConfig = (await firstValueFrom(this.mainNavService.observeMainNavConfig()))
                .customCreateOptions;
            const existingCreateOptions = currentConfig?.addOptions || [];
            if (!existingCreateOptions.find((o) => o.name === createTemplateName)) {
                this.mainNavService.patchMainNavConfig({
                    customCreateOptions: {
                        ...currentConfig,
                        addOptions: existingCreateOptions.concat([createPageTemplate]),
                    },
                });
            }
        }
        // retrieve the (potentially updated) page variant configs
        await this.updatePageVariantConfigs(true);
        // default the ID with the default or the first occurrence
        this.pageVariantDefaultPosition = pageConfig.variants.indexOf(pageConfig.default);
        // select the proper variant (initialize with default or first variant)
        let selectedVariantId: string = pageConfig.default ?? pageConfig.variants[0];
        selectedVariantId = convertNodeRefIntoNodeId(selectedVariantId);
        // if a variantId is provided, override it
        if (variantId) {
            selectedVariantId = variantId;
        }
        // otherwise, iterate the variant configs and select the one with the most matching variables
        else {
            // retrieve the best matching index
            const bestMatchIndex = this.retrieveBestMatchingVariantIndex(
                this.pageVariantConfigs.nodes,
                this.topicPageHelperService.getSelectedVariables(),
            );
            if (bestMatchIndex !== -1 && this.pageVariantConfigs.nodes) {
                const matchedVariantNode = this.pageVariantConfigs.nodes[bestMatchIndex];
                selectedVariantId = retrieveNodeId(matchedVariantNode);
            }
        }
        // hold the position of the selected variant for later retrieval
        let newSelectedVariantPosition: number = pageConfig.variants.indexOf(
            prependWorkspacePrefix(selectedVariantId),
        );
        // workaround for the case that the provided variantId is not part of the variants array
        if (newSelectedVariantPosition === -1) {
            selectedVariantId = convertNodeRefIntoNodeId(
                pageConfig.default ?? pageConfig.variants[0],
            );
            newSelectedVariantPosition = pageConfig.variants.indexOf(selectedVariantId);
        }
        const initialLoad: boolean = this.selectedVariantPosition === -1;
        const pageVariantChanged: boolean =
            !initialLoad && newSelectedVariantPosition !== this.selectedVariantPosition;
        // if the variant was not selected yet or was changed, reload the page structure
        if (initialLoad || pageVariantChanged) {
            this.selectedVariantPosition = newSelectedVariantPosition;
            // retrieve the variant config node of the page
            this.pageVariantNode = this.pageVariantConfigs.nodes?.find(
                (node: Node): boolean => retrieveNodeId(node) === selectedVariantId,
            );
            if (!this.pageVariantNode) {
                console.error(
                    this.translate.instant('TOPIC_PAGE.NO_PAGE_VARIANT.DEFAULT_MESSAGE'),
                    pageConfig,
                    selectedVariantId,
                    this.pageVariantConfigs.nodes,
                );
                return;
            }
            this.pageVariantNodeIndex = this.pageVariantConfigs.nodes.findIndex(
                (n) => retrieveNodeId(n) === retrieveNodeId(this.pageVariantNode),
            );
            this.pageVariantSettingsValid.set(true);
            const pageVariant: PageVariantConfig = retrievePageVariantConfig(this.pageVariantNode);
            if (!pageVariant || !pageVariant.structure) {
                console.error(
                    this.translate.instant(
                        'TOPIC_PAGE.NO_PAGE_VARIANT.VARIANT_OR_STRUCTURE_MISSING',
                    ),
                    pageVariant,
                );
                return;
            }
            // if page config was retrieved from parent,
            // remove possible existing nodeIds from swimlane grids
            if (!retrievePageConfigRef(this.collectionNode)) {
                preparePageVariantConfig(pageVariant, true);
            }
            // set the anchorItemColor, topicColor, breadcrumbNodeId, headerNodeId and swimlanes
            if (pageVariant.structure.anchorItemColor) {
                this.anchorItemColor = pageVariant.structure.anchorItemColor;
            }
            this.topicColor = retrieveTopicColor(pageVariant, this.collectionNode, this.topic());
            this.breadcrumbNodeId.set(pageVariant.structure.breadcrumbNodeId);
            this.headerNodeId.set(pageVariant.structure.headerNodeId);
            this.swimlanes = pageVariant.structure.swimlanes ?? [];
        }
        // update the swimlane ID to prompt text mapping
        if (initialLoad || pageVariantChanged || forceReload) {
            await this.updateSwimlaneIdToPromptTextMapping();
        }
        // wait for the page variant to be loaded before checking whether all accordions are opened in edit mode
        setTimeout((): void => {
            this.checkAccordionExpansionState();
        });
    }

    /**
     * Allows creating a new page variant by opening a new dialog.
     */
    async createPageVariant(): Promise<void> {
        this.pageVariantCreateDialogSelectedNode = null;
        this.pageVariantCreateDialogRef = await this.dialogs.openGenericDialog({
            title: 'TOPIC_PAGE.CREATE_PAGE_VARIANT.HEADING',
            minWidth: '700px',
            maxWidth: '100%',
            contentTemplate: this.addPageVariantRef,
            closable: Closable.Casual,
            buttons: USE_OR_CANCEL,
        });
        const response = await firstValueFrom(this.pageVariantCreateDialogRef.afterClosed());
        // the goal is to add another page variant to the existing (propagated) page config
        if (response === 'USE') {
            if (!this.pageVariantCreateDialogSelectedNode) {
                return;
            }
            // create a child for the variant node
            this.startEditing(this.i18nPrefix + 'CREATE_PAGE_VARIANT.PENDING_MESSAGE');
            // check for custom page node existence and create it if necessary
            await this.checkForCustomPageNodeExistence();
            // check for pageConfigNode existence
            if (!retrieveNodeId(this.pageConfigNode)) {
                return;
            }
            // parse the page config from the properties
            const pageConfig: PageConfig = retrievePageConfig(this.pageConfigNode);
            // check for pageConfig variant existence
            if (!pageConfig.variants) {
                console.error(
                    this.translate.instant('TOPIC_PAGE.NO_PAGE_VARIANT.MISSING_IN_PAGE_CONFIG'),
                    pageConfig,
                );
                return;
            }
            try {
                this.closeSideMenus();
                // retrieve page variant config and remove node IDs
                const variantConfig: PageVariantConfig = retrievePageVariantConfig(
                    this.pageVariantCreateDialogSelectedNode,
                );
                // delete the nodeIds but keep them as propagatedNodeIds + remove certain variables
                preparePageVariantConfig(variantConfig, true, true);
                // retrieve the page variant properties
                const properties: { [key: string]: string | string[] } =
                    await this.topicPageHelperService.retrievePageVariantProperties(
                        this.pageVariantCreateDialogSelectedNode,
                        '_' +
                            this.translate.instant(
                                this.i18nPrefix + 'CREATE_PAGE_VARIANT.COPY_SUFFIX',
                            ),
                        variantConfig,
                    );
                let pageConfigVariantNode: Node = await this.topicPageHelperService.createChild(
                    retrieveNodeId(this.pageConfigNode),
                    RestConstants.CCM_TYPE_MAP,
                    this.pageVariantCreateDialogSelectedNode.name + '_' + uuidv4(),
                    DEFAULT_PAGE_VARIANT_CONFIG_ASPECT,
                    properties,
                );
                // depending on the copy option, certain variables should be added
                // workaround: this currently has to be done separately as it crashes otherwise
                const educationContextProp: string = 'ccm:educationalcontext';
                if (
                    this.pageVariantCreateDialogCopyOption === CopyOption.TopicPage &&
                    this.pageVariantCreateDialogSelectedNode.properties?.[educationContextProp]
                        ?.length
                ) {
                    pageConfigVariantNode =
                        await this.topicPageHelperService.setPropertyAndRetrieveUpdatedNode(
                            retrieveNodeId(pageConfigVariantNode),
                            educationContextProp,
                            this.pageVariantCreateDialogSelectedNode.properties[
                                educationContextProp
                            ],
                        );
                }
                // push it to the existing variants
                pageConfig.variants.push(
                    prependWorkspacePrefix(retrieveNodeId(pageConfigVariantNode)),
                );
                // update the ccm:page_config of page config node
                this.pageConfigNode =
                    await this.topicPageHelperService.setPropertyAndRetrieveUpdatedNode(
                        retrieveNodeId(this.pageConfigNode),
                        DEFAULT_PAGE_CONFIG_PROP,
                        JSON.stringify(pageConfig),
                    );
                // reload page variant configs
                await this.updatePageVariantConfigs(true);
                // TODO: there seems to be a race condition here, so slightly delay the navigate call
                setTimeout(async () => {
                    // switch into edit mode
                    this.editMode.set(true);
                    // end visual editing
                    this.endEditing();
                    this.topicPageHelperService.openSaveConfigToast(
                        this.i18nPrefix + 'CREATE_PAGE_VARIANT.SUCCESS_MESSAGE',
                    );
                    // navigate to the newly created variant
                    await this.navigateToVariant(retrieveNodeId(pageConfigVariantNode));
                    // wait for the variant load and automatically open the settings menu
                    setTimeout(async () => {
                        const queryParamsToAddOrOverwrite: Params = {
                            openMenu: 'settings',
                        };
                        // on variant change, do not keep the fragments
                        await this.router.navigate([], {
                            relativeTo: this.route,
                            queryParams: queryParamsToAddOrOverwrite,
                            queryParamsHandling: 'merge',
                        });
                    }, 500);
                }, 500);
            } catch (err) {
                console.error(err);
                this.endEditing();
                this.topicPageHelperService.displayErrorToast();
            }
        }
    }

    /**
     * Applies the changes made to the currently selected page variant.
     *
     * @param changesMap
     */
    async applyPageVariant(changesMap: Map<string, string | string[]>): Promise<void> {
        // reset validity variable
        this.pageVariantSettingsValid.set(true);
        this.startEditing();
        if (!changesMap.size) {
            this.closeSideMenus();
            this.endEditing();
            return;
        }
        try {
            await this.checkForCustomPageNodeExistence();
            const pageVariant: PageVariantConfig = this.retrievePageVariant();
            if (!pageVariant) {
                this.endEditing();
                return;
            }
            // iterate changes map and persist them
            let index: number = 0;
            for (const [key, value] of changesMap.entries()) {
                // more than one change exist
                // persist change without reloading the page variant
                if (changesMap.size > index + 1) {
                    await this.topicPageHelperService.setProperty(
                        retrieveNodeId(this.pageVariantNode),
                        key,
                        value,
                    );
                }
                // persist change with reloading the page variant and update the configs accordingly
                else {
                    this.pageVariantNode =
                        await this.topicPageHelperService.setPropertyAndRetrieveUpdatedNode(
                            retrieveNodeId(this.pageVariantNode),
                            key,
                            value,
                        );
                    await this.updatePageVariantConfigs(false);
                }
                index++;
            }
            this.closeSideMenus();
            this.endEditing();
        } catch (err) {
            console.error(err);
            this.endEditing();
            this.topicPageHelperService.displayErrorToast();
        }
    }

    /**
     * Deletes the currently selected page variant, adjusts the page config properties and
     * deletes the page config and its link as well if it is the last item.
     */
    async deletePageVariant(): Promise<void> {
        if (!this.collectionNodePageConfigRef || !this.userHasEditRights()) {
            return;
        }
        const deleteString: string = this.templateMode()
            ? 'DELETE_PAGE_TEMPLATE.'
            : 'DELETE_PAGE_VARIANT.';
        const dialogRef = await this.dialogs.openGenericDialog({
            title: this.i18nPrefix + deleteString + 'HEADING',
            message: this.i18nPrefix + deleteString + 'MESSAGE',
            buttons: DELETE_OR_CANCEL,
            closable: Closable.Casual,
        });
        dialogRef.afterClosed().subscribe(async (response) => {
            if (response === 'YES_DELETE') {
                // parse the page config from the properties
                const pageConfig: PageConfig = retrievePageConfig(this.pageConfigNode);
                // check for pageConfig variant existence
                if (!pageConfig.variants || !this.pageVariantNode) {
                    console.error(
                        this.translate.instant(
                            'TOPIC_PAGE.NO_PAGE_CONFIG.MISSING_CONFIG_OR_VARIANTS',
                        ),
                        pageConfig,
                        this.pageVariantNode,
                    );
                    return;
                }
                try {
                    // start deleting page variant
                    this.startEditing(this.i18nPrefix + deleteString + 'PENDING_MESSAGE');
                    // remove from variants first to ensure that no inconsistency occurs
                    pageConfig.variants = pageConfig.variants.filter(
                        (v) => !v.includes(this.pageVariantNode.ref.id),
                    );
                    const atLeastOneRemainingVariant = pageConfig.variants.length > 0;
                    if (atLeastOneRemainingVariant) {
                        // check if the default is set correctly
                        if (pageConfig.default?.includes(this.pageVariantNode.ref.id)) {
                            pageConfig.default = pageConfig.variants[0];
                        }
                    }
                    // update the pageConfig
                    if (atLeastOneRemainingVariant) {
                        this.pageConfigNode =
                            await this.topicPageHelperService.setPropertyAndRetrieveUpdatedNode(
                                retrieveNodeId(this.pageConfigNode),
                                DEFAULT_PAGE_CONFIG_PROP,
                                JSON.stringify(pageConfig),
                            );
                        // delete the page variant node along with all its children
                        await this.topicPageHelperService.deleteNode(
                            retrieveNodeId(this.pageVariantNode),
                        );
                        // reload the page without parameters
                        setTimeout(() => {
                            void this.reloadWithoutParameters();
                        }, 1000);
                        // TODO: comment back in, if a solution without page reload is found
                        // await this.reloadPageVariantConfigs();
                        // // navigate to default variant
                        // await this.navigateToVariant(convertNodeRefIntoNodeId(pageConfig.default));
                    } else {
                        // remove the page config ref from the collectionNode
                        if (!this.templateMode()) {
                            await this.topicPageHelperService.resetProperty(
                                retrieveNodeId(this.collectionNode),
                                DEFAULT_PAGE_CONFIG_REF_PROP,
                            );
                        } else {
                            await this.topicPageHelperService.resetProperty(
                                retrieveNodeId(this.collectionNode),
                                DEFAULT_PAGE_CONFIG_PROPAGATE_REF_PROP,
                            );
                        }
                        // delete the pageConfig node as well
                        await this.topicPageHelperService.deleteNode(
                            retrieveNodeId(this.pageConfigNode),
                        );
                        // reload the page without parameters
                        setTimeout(() => {
                            void this.reloadWithoutParameters();
                        }, 1000);
                    }
                    this.endEditing();
                } catch (err) {
                    console.error(err);
                    this.topicPageHelperService.displayErrorToast();
                }
            }
        });
    }

    /**
     * Updates the topic or anchor item color of the page variant.
     *
     * @param color
     * @param isTopicColor
     */
    async updateTopicOrAnchorItemColor(
        color: string,
        isTopicColor: boolean = false,
    ): Promise<void> {
        const initialColor: string = isTopicColor
            ? this.initialTopicColor
            : this.initialAnchorItemColor;
        if (color !== initialColor) {
            if (isTopicColor) {
                this.topicColor = color;
            } else {
                this.anchorItemColor = color;
            }
            this.startEditing();
            try {
                await this.checkForCustomPageNodeExistence();
                const pageVariant: PageVariantConfig = this.retrievePageVariant();
                if (!pageVariant) {
                    this.endEditing();
                    return;
                }
                const propertyName = isTopicColor ? 'topicColor' : 'anchorItemColor';
                pageVariant.structure[propertyName] = color;
                this.pageVariantNode =
                    await this.topicPageHelperService.setPropertyAndRetrieveUpdatedNode(
                        retrieveNodeId(this.pageVariantNode),
                        DEFAULT_PAGE_VARIANT_CONFIG_PROP,
                        JSON.stringify(pageVariant),
                    );
                await this.updatePageVariantConfigs(this.pageVariantReloadNecessary);
                this.pageVariantReloadNecessary = false;
                this.endEditing();
            } catch (err) {
                console.error(err);
                this.endEditing();
                this.topicPageHelperService.displayErrorToast();
            }
        }
    }

    /**
     * Reloads the page without parameters.
     */
    private async reloadWithoutParameters(): Promise<void> {
        // navigate to default variant
        await this.router.navigate([], {
            queryParams: { collectionId: this.latestParams.collectionId },
            replaceUrl: true,
        });
        // TODO: remove, if a solution without page reload is found
        window.location.reload();
    }

    /**
     * Navigates to a given variantId and reloads the page structure accordingly.
     *
     * @param variantId
     */
    async navigateToVariant(variantId: string): Promise<void> {
        this.closeSideMenus();
        const queryParamsToAddOrOverwrite: Params = {
            variantId: variantId,
        };
        // on variant change, do not keep the fragments
        await this.router.navigate([], {
            relativeTo: this.route,
            queryParams: queryParamsToAddOrOverwrite,
            queryParamsHandling: 'merge',
        });
        // retrieve the page config node and select the proper variant to define the breadcrumbNodeId, headerNodeId + swimlanes
        try {
            await this.retrievePageConfigAndSelectVariant(variantId);
        } catch (err) {
            console.error(err);
            this.topicPageHelperService.displayErrorToast();
        }
    }

    /**
     * Navigates to a given fragment by appending it to the url.
     *
     * @param fragment
     */
    async navigateToFragment(fragment: string): Promise<void> {
        // use history over router to avoid directly navigating to wrong fragment position
        history.pushState(null, '', this.retrieveFragmentUrl(fragment));
        // manually scroll into view
        this.latestUrlFragment = fragment;
        this.scrollElementIntoView();
    }

    /**
     * Helper function to retrieve the URL to a given fragment.
     */
    retrieveFragmentUrl(fragment: string): string {
        const url = new URL(window.location.href);
        url.hash = fragment;
        return url.toString();
    }

    // SWIMLANE SPECIFIC FUNCTIONS
    /**
     * Adds a new swimlane to the page and persists it in the config.
     */
    async addSwimlane(newSwimlane: Swimlane, positionToAdd: number): Promise<void> {
        this.startEditing();
        try {
            await this.checkForCustomPageNodeExistence();
            const pageVariant: PageVariantConfig = this.retrievePageVariant();
            if (!pageVariant) {
                this.endEditing();
                return;
            }
            const swimlanesCopy = Helper.deepCopy(this.swimlanes ?? []);
            swimlanesCopy.splice(positionToAdd, 0, newSwimlane);
            pageVariant.structure.swimlanes = swimlanesCopy;
            this.pageVariantNode =
                await this.topicPageHelperService.setPropertyAndRetrieveUpdatedNode(
                    retrieveNodeId(this.pageVariantNode),
                    DEFAULT_PAGE_VARIANT_CONFIG_PROP,
                    JSON.stringify(pageVariant),
                );
            await this.updatePageVariantConfigs(this.pageVariantReloadNecessary);
            this.pageVariantReloadNecessary = false;
            // add swimlane visually as soon as the requests are done
            this.swimlanes.splice(positionToAdd, 0, newSwimlane);
            this.endEditing();
        } catch (err) {
            console.error(err);
            this.endEditing();
            this.topicPageHelperService.displayErrorToast();
        }
    }

    /**
     * Moves the position of a swimlane on the page and persists it in the config.
     */
    async moveSwimlanePosition(oldIndex: number, newIndex: number): Promise<void> {
        if (newIndex >= 0 && newIndex <= this.swimlanes.length - 1) {
            this.startEditing();
            try {
                await this.checkForCustomPageNodeExistence();
                const pageVariant: PageVariantConfig = this.retrievePageVariant();
                if (!pageVariant) {
                    this.endEditing();
                    return;
                }
                const swimlanesCopy = Helper.deepCopy(this.swimlanes ?? []);
                moveItemInArray(swimlanesCopy, oldIndex, newIndex);
                pageVariant.structure.swimlanes = swimlanesCopy;
                this.pageVariantNode =
                    await this.topicPageHelperService.setPropertyAndRetrieveUpdatedNode(
                        retrieveNodeId(this.pageVariantNode),
                        DEFAULT_PAGE_VARIANT_CONFIG_PROP,
                        JSON.stringify(pageVariant),
                    );
                await this.updatePageVariantConfigs(this.pageVariantReloadNecessary);
                this.pageVariantReloadNecessary = false;
                // move swimlane position visually as soon as the requests are done
                moveItemInArray(this.swimlanes, oldIndex, newIndex);
                this.endEditing();
            } catch (err) {
                console.error(err);
                this.endEditing();
                this.topicPageHelperService.displayErrorToast();
            }
        }
    }

    /**
     * Updates the shape of a swimlane at a given index.
     *
     * @param swimlaneShape
     * @param mirror
     * @param index
     */
    async updateSwimlaneShape(
        swimlaneShape: SwimlaneBackgroundShape,
        mirror: boolean = false,
        index: number,
    ): Promise<void> {
        this.startEditing();
        try {
            await this.checkForCustomPageNodeExistence();
            const pageVariant: PageVariantConfig = this.retrievePageVariant();
            if (!pageVariant) {
                return;
            }
            const applySwimlaneChanges = (swimlane: Swimlane) => {
                if (swimlaneShape != null) {
                    swimlane.backgroundShape = swimlaneShape;
                }
                if (mirror) {
                    swimlane.backgroundShapeMirrored = !swimlane.backgroundShapeMirrored;
                }
            };

            const swimlanesCopy = Helper.deepCopy(this.swimlanes ?? []);
            applySwimlaneChanges(swimlanesCopy[index]);
            pageVariant.structure.swimlanes = swimlanesCopy;
            this.pageVariantNode =
                await this.topicPageHelperService.setPropertyAndRetrieveUpdatedNode(
                    retrieveNodeId(this.pageVariantNode),
                    DEFAULT_PAGE_VARIANT_CONFIG_PROP,
                    JSON.stringify(pageVariant),
                );
            await this.updatePageVariantConfigs(this.pageVariantReloadNecessary);
            this.pageVariantReloadNecessary = false;
            applySwimlaneChanges(this.swimlanes[index]);
        } catch (err) {
            console.error(err);
            this.topicPageHelperService.displayErrorToast();
        } finally {
            this.endEditing();
        }
    }

    /**
     * Opens a dialog that displays a QR code for the swimlane link.
     *
     * @param swimlaneIndex
     */
    async openQrCodeDialog(swimlaneIndex: number): Promise<void> {
        this.qrCodeUrl.set(
            this.retrieveFragmentUrl(this.SWIMLANE_ID_PREFIX + this.swimlanes[swimlaneIndex].id),
        );
        this.qrCodeDialogRef = await this.dialogs.openGenericDialog({
            title: 'TOPIC_PAGE.SWIMLANE.QR_CODE.HEADING',
            contentPadding: 0,
            contentTemplate: this.showQrCodeDialogRef,
            closable: Closable.Casual,
            buttons: [{ label: 'CANCEL', config: { color: 'standard' } }],
        });
    }

    /**
     * Copies the link to the swimlane into the clipboard.
     *
     * @param swimlaneIndex
     */
    async copySwimlaneLink(swimlaneIndex: number): Promise<void> {
        // similar to copying links in GitHub issues, the target is first set to the URL and then copied
        await this.navigateToFragment(this.SWIMLANE_ID_PREFIX + this.swimlanes[swimlaneIndex].id);
        // workaround: setTimeout is necessary, as navigateToFragment includes a delay
        setTimeout((): void => {
            this.clipboard.copy(window.location.href);
            // inform user about URL being copied successfully
            this.topicPageHelperService.openSaveConfigToast(
                'TOPIC_PAGE.SWIMLANE.SHARE_CONTENT.COPY_LINK_SUCCESS',
            );
        }, 300);
    }

    /**
     * Writes a mail with a link to a given swimlane.
     *
     * @param swimlaneIndex
     */
    writeMail(swimlaneIndex: number): void {
        const swimlane = this.swimlanes[swimlaneIndex];
        const i18nPrefix = 'TOPIC_PAGE.SWIMLANE.SHARE_CONTENT.WRITE_MAIL.';
        if (swimlane) {
            // TODO: Move into shared service
            const subjectText = this.translate.instant(i18nPrefix + 'SUBJECT_SHARED_CONTENT', {
                title: swimlane.heading || this.translate.instant(i18nPrefix + 'NO_TITLE'),
            });
            const bodyText = this.translate.instant(i18nPrefix + 'BODY_SHARED_CONTENT', {
                url: this.retrieveFragmentUrl(this.SWIMLANE_ID_PREFIX + swimlane.id),
            });

            const subject = encodeURIComponent(subjectText);
            const body = encodeURIComponent(bodyText);

            const mailtoLink = `mailto:?subject=${subject}&body=${body}`;
            window.open(mailtoLink, '_self');
        } else {
            console.warn(this.translate.instant(i18nPrefix + 'ERROR_NO_URL'));
        }
    }

    /**
     * Reacts to es-editable-text textChange output event by persisting the changes in the page config.
     *
     * @param title
     * @param index
     */
    async swimlaneTitleChanged(title: string, index: number): Promise<void> {
        this.startEditing();
        try {
            await this.checkForCustomPageNodeExistence();
            const pageVariant: PageVariantConfig = this.retrievePageVariant();
            if (!pageVariant || !this.pageVariantNode) {
                this.endEditing();
                return;
            }
            // update swimlane heading
            this.swimlanes[index].heading = title;
            pageVariant.structure.swimlanes = this.swimlanes;
            this.pageVariantNode =
                await this.topicPageHelperService.setPropertyAndRetrieveUpdatedNode(
                    retrieveNodeId(this.pageVariantNode),
                    DEFAULT_PAGE_VARIANT_CONFIG_PROP,
                    JSON.stringify(pageVariant),
                );
            // retrieve existing AI config
            const aiConfig: BapiConfigObject = retrieveAiConfigFromNode(this.pageVariantNode);
            let aiUpdateNecessary: boolean = false;
            if (containsAiTags(title)) {
                aiConfig[this.swimlanes[index].id] = retrieveChatCompletionObject(title);
                aiUpdateNecessary = true;
            } else if (aiConfig.hasOwnProperty(this.swimlanes[index].id)) {
                delete aiConfig[this.swimlanes[index].id];
                aiUpdateNecessary = true;
            }
            // update AI config, if necessary (either AI tags are present or were deleted)
            if (aiUpdateNecessary) {
                this.pageVariantNode =
                    await this.topicPageHelperService.setPropertyAndRetrieveUpdatedNode(
                        retrieveNodeId(this.pageVariantNode),
                        DEFAULT_AI_CONFIG_PROP,
                        JSON.stringify(aiConfig),
                    );
                // reload the page variant configs and set the updated pageVariantNode
                await this.updatePageVariantConfigs(this.pageVariantReloadNecessary);
                this.pageVariantReloadNecessary = false;
                this.retrievePageVariant();
                // use this pageVariantNode to update the mapping
                await this.updateSwimlaneIdToPromptTextMapping();
            }
            // update the page variant config in any case
            else {
                await this.updatePageVariantConfigs(this.pageVariantReloadNecessary);
                this.pageVariantReloadNecessary = false;
            }
            this.endEditing();
        } catch (err) {
            console.error(err);
            this.endEditing();
            this.topicPageHelperService.displayErrorToast();
        }
    }

    /**
     * Starts editing a swimlane by opening a dialog and handling the result status.
     *
     * @param swimlane
     * @param index
     */
    async editSwimlane(swimlane: Swimlane, index: number): Promise<void> {
        this.swimlaneToEditForm = new UntypedFormGroup({
            type: new UntypedFormControl(swimlane.type),
            heading: new UntypedFormControl(swimlane.heading),
            grid: new UntypedFormControl(JSON.stringify(swimlane.grid ?? '[]')),
        });
        const dialogRef = await this.dialogs.openGenericDialog({
            title: this.translate.instant('TOPIC_PAGE.SWIMLANE.EDIT.HEADING', {
                heading: swimlane.heading
                    ? '"' + swimlane.heading + '"'
                    : this.translate.instant('TOPIC_PAGE.SWIMLANE.EDIT.SWIMLANE_DEFAULT'),
            }),
            minWidth: '700px',
            maxWidth: '100%',
            contentTemplate: this.editSwimlaneRef,
            closable: Closable.Casual,
            buttons: [
                { label: 'CANCEL', config: { color: 'standard' } },
                { label: 'APPLY', config: { color: 'primary' } },
            ],
        });
        const result = await firstValueFrom(dialogRef.afterClosed());
        if (result === 'APPLY') {
            const editedSwimlane: any = this.swimlaneToEditForm.value;
            if (!editedSwimlane) {
                return;
            }
            // restore swimlane ID + background color
            editedSwimlane.id = swimlane.id;
            if (swimlane.backgroundColor) {
                editedSwimlane.backgroundColor = swimlane.backgroundColor;
            }
            // parse grid string
            if (editedSwimlane.grid) {
                editedSwimlane.grid = JSON.parse(editedSwimlane.grid);
            }
            if (JSON.stringify(editedSwimlane) === JSON.stringify(swimlane)) {
                return;
            }
            // detect whether a structural change has been made (type or grid was changed)
            const structuralChange: boolean =
                editedSwimlane.type !== swimlane.type ||
                JSON.stringify(editedSwimlane.grid) !== JSON.stringify(swimlane.grid);
            this.startEditing();
            try {
                await this.checkForCustomPageNodeExistence();
                const pageVariant: PageVariantConfig = this.retrievePageVariant();
                if (!pageVariant) {
                    this.endEditing();
                    return;
                }
                // create a copy of the swimlanes
                const stringifiedSwimlanes: string = JSON.stringify(this.swimlanes ?? []);
                const swimlanesCopy = JSON.parse(stringifiedSwimlanes);
                // retrieve deleted widget node IDs (previously existing node IDs must still exist)
                const deletedWidgetNodeIds: string[] = [];
                const stringifiedEditedSwimlane: string = JSON.stringify(editedSwimlane);
                // iterate swimlane and detect potentially deleted node IDs
                swimlane?.grid?.forEach((gridItem: GridTile): void => {
                    // nodeId exists but is no longer included in the edited swimlane
                    if (
                        !!gridItem.nodeId &&
                        gridItem.nodeId !== '' &&
                        !stringifiedEditedSwimlane.includes(gridItem.nodeId)
                    ) {
                        deletedWidgetNodeIds.push(gridItem.nodeId);
                    }
                });
                // store updated swimlane in config
                swimlanesCopy[index] = editedSwimlane;
                // overwrite swimlanes
                pageVariant.structure.swimlanes = swimlanesCopy;
                let reloadNecessary: boolean = false;
                if (structuralChange) {
                    reloadNecessary = this.pageVariantReloadNecessary;
                }
                this.pageVariantNode =
                    await this.topicPageHelperService.setPropertyAndRetrieveUpdatedNode(
                        retrieveNodeId(this.pageVariantNode),
                        DEFAULT_PAGE_VARIANT_CONFIG_PROP,
                        JSON.stringify(pageVariant),
                    );
                if (structuralChange) {
                    await this.updatePageVariantConfigs(reloadNecessary);
                    this.pageVariantReloadNecessary = false;
                }
                // afterward, delete config nodes of removed widgets
                for (const nodeId of deletedWidgetNodeIds) {
                    // retrieve correct nodeId
                    const widgetNodeId: string = convertNodeRefIntoNodeId(nodeId);
                    await this.topicPageHelperService.deleteNode(widgetNodeId);
                }
                // sync with the visible nodes (reset map, as the outputs are triggered again)
                this.topicPageGlobalService.deleteVisibleNodesMap();
                // visually change swimlanes
                this.swimlanes = pageVariant.structure.swimlanes;
                this.endEditing();
                // fix accordions are closed on edit
                setTimeout((): void => {
                    this.accordions?.forEach((accordion: CdkAccordionItem): void => {
                        accordion.open();
                    });
                });
            } catch (err) {
                console.error(err);
                this.endEditing();
                this.topicPageHelperService.displayErrorToast();
            }
        }
    }

    /**
     * Deletes a swimlane from the page with possible widget nodes and persists it in the config.
     *
     * @param index
     */
    async deleteSwimlane(index: number): Promise<void> {
        if (!this.swimlanes?.[index]) {
            return;
        }
        const dialogRef = await this.dialogs.openGenericDialog({
            title: this.i18nPrefix + 'SWIMLANE.DELETE.LABEL',
            message: this.i18nPrefix + 'SWIMLANE.DELETE.MESSAGE',
            buttons: YES_OR_NO,
            closable: Closable.Casual,
        });
        dialogRef.afterClosed().subscribe(async (response) => {
            if (response === 'YES') {
                this.startEditing();
                await this.checkForCustomPageNodeExistence();
                const pageVariant: PageVariantConfig = this.retrievePageVariant();
                if (!pageVariant) {
                    this.endEditing();
                    return;
                }
                // hold deleted widget node IDs to delete them afterwards
                const deletedWidgetNodeIds: string[] = [];
                this.swimlanes[index].grid?.forEach((gridItem: GridTile): void => {
                    if (gridItem?.nodeId) {
                        deletedWidgetNodeIds.push(gridItem.nodeId);
                    }
                });
                const swimlanesCopy = Helper.deepCopy(this.swimlanes ?? []);
                swimlanesCopy.splice(index, 1);
                pageVariant.structure.swimlanes = swimlanesCopy;
                // update page variant first to ensure that no inconsistency occurs
                this.pageVariantNode =
                    await this.topicPageHelperService.setPropertyAndRetrieveUpdatedNode(
                        retrieveNodeId(this.pageVariantNode),
                        DEFAULT_PAGE_VARIANT_CONFIG_PROP,
                        JSON.stringify(pageVariant),
                    );
                await this.updatePageVariantConfigs(this.pageVariantReloadNecessary);
                this.pageVariantReloadNecessary = false;
                // delete config nodes of removed widgets
                for (const nodeId of deletedWidgetNodeIds) {
                    const widgetNodeId: string = convertNodeRefIntoNodeId(nodeId);
                    await this.topicPageHelperService.deleteNode(widgetNodeId);
                }
                // sync with the visible nodes (reset swimlane nodes)
                this.topicPageGlobalService.deleteVisibleNodesBySwimlane(index);
                // delete swimlane visually as soon as the requests are done
                this.swimlanes.splice(index, 1);
                this.endEditing();
            }
        });
    }

    // REACT TO FURTHER OUTPUT EVENTS
    /**
     * Called by es-swimlane gridUpdated output event.
     * Handles the initial grid update of a given swimlane.
     *
     * @param grid
     * @param swimlaneIndex
     */
    async handleGridUpdate(grid: GridTile[], swimlaneIndex: number): Promise<void> {
        try {
            // overwrite swimlane grid
            this.swimlanes[swimlaneIndex].grid = grid;

            // persist the state afterward
            await this.checkForCustomPageNodeExistence();
            const pageVariant: PageVariantConfig = this.retrievePageVariant();
            if (!pageVariant) {
                // TODO: rollback necessary
            }
            pageVariant.structure.swimlanes = this.swimlanes;
            this.pageVariantNode =
                await this.topicPageHelperService.setPropertyAndRetrieveUpdatedNode(
                    retrieveNodeId(this.pageVariantNode),
                    DEFAULT_PAGE_VARIANT_CONFIG_PROP,
                    JSON.stringify(pageVariant),
                );
            await this.updatePageVariantConfigs(this.pageVariantReloadNecessary);
            this.pageVariantReloadNecessary = false;
            // TODO: rollback necessary, if the request is not successful
        } catch (err) {
            console.error(err);
            this.topicPageHelperService.displayErrorToast();
        }
    }

    /**
     * Called by es-editable-text searchResultsUpdated output event.
     *
     * @param event
     * @param swimlaneId
     */
    updateTitleSearchResults(event: number, swimlaneId: string): void {
        // update the title matching
        this.swimlaneTitleIdToHitMatching.set(swimlaneId, event > 0);
        // update the hit matching
        this.updateSwimlaneIdToHitMatching();
    }

    /**
     * Called by es-swimlane searchHitsChanged output event.
     *
     * @param event
     * @param swimlaneIndex
     */
    updateGridItemSearchInputHits(event: GridTileToHitsMapping, swimlaneIndex: number): void {
        // update the hasHits value of the grid tile
        this.swimlanes[swimlaneIndex].grid[event.gridIndex].hasHits = event.hasHits;
        // update the hit matching
        this.updateSwimlaneIdToHitMatching();
    }

    /**
     * Called by es-swimlane visibleNodesChanged output event.
     * Sets the visible nodes in the global service and triggers the search count update and hit matching.
     *
     * @param event
     * @param swimlaneIndex
     */
    updateVisibleNodes(event: GridTileToSearchResultsMapping, swimlaneIndex: number): void {
        this.topicPageGlobalService.updateVisibleNodes(swimlaneIndex, event.gridIndex, event.nodes);
        this.swimlanes[swimlaneIndex].grid[event.gridIndex].searchCount = event.nodes?.length || 0;
        this.searchCountTrigger++;
        this.updateSwimlaneIdToHitMatching();
    }

    /**
     * Called by es-profiling selectDimensionsChanged output event.
     * Retrieves a mapping between the dimension ID and the mds widgets from the mds service.
     *
     * @param event
     */
    selectDimensionsChanged(event: Map<string, MdsWidget>): void {
        if (!event) {
            return;
        }
        // iterate incoming select dimensions and insert new or overwrite existing ones
        event.forEach((value: MdsWidget, key: string): void => {
            this.selectDimensions.set(key, value);
        });
    }

    /**
     * Called by right es-side-menu-item itemClicked output event.
     */
    collapsibleItemClicked(item: string): void {
        if (this.selectedMenuItem === item) {
            this.closeSideMenus();
        } else {
            this.selectedMenuItem = item;
        }
    }

    /**
     * Called by edit mode toggle button located in the top navbar.
     * Opens all accordions when the view is switched into edit mode.
     */
    async toggleEditMode(): Promise<void> {
        this.editMode.set(!this.editMode());
        this.checkAccordionExpansionState();
        this.closeSideMenus();
    }

    /**
     * Leaves the template mode by setting the according variable and reloading the page.
     */
    leaveTemplateMode(): void {
        this.templateMode.set(false);
        window.location.reload();
    }

    /**
     * Called by es-topic-page-breadcrumb navigateToTemplate output event.
     *
     * @param reloadNecessary
     */
    async switchIntoTemplateMode(reloadNecessary: boolean = false): Promise<void> {
        this.templateMode.set(true);
        if (reloadNecessary) {
            this.collectionNode = await this.topicPageHelperService.getNode(
                retrieveNodeId(this.collectionNode),
            );
        }
        this.pageConfigNode = null;
        this.selectedVariantPosition = -1;
        this.closeSideMenus();
        // remove create button for new page variant
        // retrieve existing options from main nav config to extend them
        const currentConfig = (await firstValueFrom(this.mainNavService.observeMainNavConfig()))
            .customCreateOptions;
        const existingCreateOptions = currentConfig?.addOptions || [];
        this.mainNavService.patchMainNavConfig({
            customCreateOptions: {
                ...currentConfig,
                addOptions: existingCreateOptions.filter(
                    (o) => o.name !== this.createPageVariantTitle,
                ),
            },
        });
        await this.retrievePageConfigAndSelectVariant();
    }

    // HELPER FUNCTIONS
    /**
     * Helper function to add custom options to the main nav config.
     */
    private async addCustomMainNavOptions() {
        // @TODO: setting scope does not work yet
        this.optionsHelperService.setData({
            scope: Scope.TopicPage,
        });

        // add a create page variant option to the "new" button
        const createPageVariant = new OptionItem(
            this.createPageVariantTitle,
            'edu-page_variant',
            async () => {
                await this.createPageVariant();
            },
        );
        createPageVariant.elementType = [ElementType.Unknown];
        createPageVariant.group = DefaultGroups.Toggles;
        createPageVariant.priority = 10;
        createPageVariant.constrains = [Constrain.User];

        // retrieve existing options from main nav config to extend them
        const currentConfig = (await firstValueFrom(this.mainNavService.observeMainNavConfig()))
            .customCreateOptions;
        const existingCreateOptions = currentConfig?.addOptions || [];
        this.mainNavService.patchMainNavConfig({
            customCreateOptions: {
                ...currentConfig,
                addOptions: existingCreateOptions.concat([createPageVariant]),
            },
        });
    }

    /**
     * Helper function to retrieve the search URL.
     */
    private retrieveSearchUrl(): string {
        const tree: UrlTree = this.router.createUrlTree([UIConstants.ROUTER_PREFIX, 'search']);
        return (
            this.platformLocation.getBaseHrefFromDOM() + this.router.serializeUrl(tree).substring(1)
        );
    }

    /**
     * Helper function to parse the page config ref of the collection and return the proper page config node.
     * If no page config ref is set, check whether a parent propagates one.
     *
     * @param node
     */
    private async retrievePageConfigNode(node: Node): Promise<Node> {
        // check whether the node itself has a pageConfigRef
        let pageRef: string = retrievePageConfigRef(node);
        // in template mode, overwrite the page ref with the propagate page config ref
        if (this.templateMode() && retrievePageConfigPropagateRef(node)) {
            pageRef = retrievePageConfigPropagateRef(node);
        }
        this.collectionNodePageConfigRef = pageRef;
        // otherwise, iterate the parents to retrieve the pageConfigPropagateRef if set
        if (!pageRef) {
            const parents: ParentEntries = await this.topicPageHelperService.getNodeParents(
                retrieveNodeId(node),
            );
            this.propagatingParentNode = parents.nodes.find(
                (parent: Node) =>
                    retrieveNodeId(node) !== retrieveNodeId(parent) &&
                    !!retrievePageConfigPropagateRef(parent),
            );
            if (this.propagatingParentNode) {
                pageRef = retrievePageConfigPropagateRef(this.propagatingParentNode);
            }
            this.pageVariantReloadNecessary = true;
        }
        if (pageRef) {
            const pageNodeId: string = convertNodeRefIntoNodeId(pageRef);
            if (pageNodeId) {
                this.pageConfigCheckFailed.set(false);
                return await this.topicPageHelperService.getNode(pageNodeId);
            }
        }
        this.pageConfigCheckFailed.set(true);
        // if no page config exists, retrieve page variant templates to be chosen by the user
        const searchResult: SearchResults = await firstValueFrom(
            this.searchService.search({
                query: DEFAULT_PAGE_VARIANT_QUERY_ID,
                repository: HOME_REPOSITORY,
                maxItems: 10,
                propertyFilter: [PROPERTY_FILTER_ALL],
                contentType: CONTENT_TYPE_ALL,
                metadataset: DEFAULT,
                body: {
                    criteria: [
                        {
                            property: DEFAULT_PAGE_VARIANT_IS_TEMPLATE_PROP,
                            values: ['true'],
                        },
                    ],
                },
            }),
        );
        this.defaultPageVariantNodes =
            searchResult.nodes?.length > 0
                ? searchResult.nodes
                : ([
                      {
                          ref: {
                              archived: false,
                              id: uuidv4(),
                              repo: HOME_REPOSITORY,
                          },
                          name: 'DEFAULT TEMPLATE',
                          title: 'DEFAULT TEMPLATE',
                          type: RestConstants.CCM_TYPE_MAP,
                          properties: {
                              [DEFAULT_PAGE_VARIANT_CONFIG_PROP]: [
                                  '{"variables":{},"template":{},"structure":{"swimlanes":[]}}',
                              ],
                              [DEFAULT_PAGE_VARIANT_IS_TEMPLATE_PROP]: ['true'],
                          },
                      },
                  ] as Partial<Node>[]);
        return null;
    }

    /**
     * Helper function to update the swimlane ID to search hit matching.
     */
    private updateSwimlaneIdToHitMatching(): void {
        const swimlaneIdToHitMatching: Map<string, boolean> = new Map<string, boolean>();
        this.swimlanes.forEach((swimlane: Swimlane): void => {
            // check for direct title match
            let hasMatchingHits: boolean =
                this.swimlaneTitleIdToHitMatching.get(swimlane.id) ?? false;
            // if no title match was found, check for matching in the grid
            if (!hasMatchingHits) {
                swimlane.grid?.forEach((tile: GridTile): void => {
                    const hasHits: boolean = tile.hasHits;
                    const visibleWidgets: WIDGET_TYPE[] = [
                        WIDGETS.COLLECTION_CHIPS,
                        WIDGETS.TOPICS_COLUMN_BROWSER,
                    ];
                    const alwaysVisibleWidget: boolean = visibleWidgets.includes(tile.item);
                    if (hasHits || alwaysVisibleWidget) {
                        hasMatchingHits = true;
                    }
                });
            }
            swimlaneIdToHitMatching.set(swimlane.id, hasMatchingHits);
        });
        this.swimlaneIdToHitMatching = swimlaneIdToHitMatching;
        this.firstMatchingSwimlaneId = this.swimlanes.find((swimlane) =>
            this.swimlaneIdToHitMatching.get(swimlane.id),
        )?.id;
        this.lastMatchingSwimlaneId = [...this.swimlanes]
            .reverse()
            .find((swimlane) => this.swimlaneIdToHitMatching.get(swimlane.id))?.id;
    }

    /**
     * Helper function to update the swimlane ID to prompt text mapping.
     */
    private async updateSwimlaneIdToPromptTextMapping(): Promise<void> {
        if (!this.pageVariantNode) {
            console.warn(this.translate.instant('TOPIC_PAGE.NO_PAGE_VARIANT.DEFAULT_MESSAGE'));
            return;
        }
        if (!this.aiSupported()) {
            return;
        }
        const aiConfig: BapiConfigObject = retrieveAiConfigFromNode(this.pageVariantNode);
        if (!aiConfig || !Object.keys(aiConfig)?.length) {
            return;
        }
        try {
            // reset map to delete existing entries
            this.swimlaneIdToPromptTextMapping = new Map<string, PromptToTextMapping>();
            for (const swimlane of this.swimlanes) {
                if (aiConfig.hasOwnProperty(swimlane.id)) {
                    const prompt: string = retrievePromptFromAiConfig(aiConfig, swimlane.id);
                    if (prompt) {
                        const config: NodeConfig = {
                            type: 'node',
                            nodeId: convertNodeRefIntoNodeId(retrieveNodeId(this.pageVariantNode)),
                            configName: swimlane.id,
                        };
                        const result: ChatCompletionResult =
                            await this.aiHelperService.generateFromPrompt(
                                config,
                                this.topicPageHelperService.getSelectedVariables() || {},
                                this.topicCollectionID(),
                            );
                        const promptToTextMapping = new PromptToTextMapping(
                            prompt,
                            retrieveResultString(result) ||
                                this.translate.instant('TOPIC_PAGE.AI.INVALID_PROMPT'),
                        );
                        this.swimlaneIdToPromptTextMapping.set(swimlane.id, promptToTextMapping);
                    }
                }
            }
        } catch (e) {
            console.error(e);
        }
    }

    /**
     * Helper function to create a custom page config from the selected default config node.
     */
    async createCustomConfig(): Promise<void> {
        try {
            this.createCustomConfigInProgress.set(true);
            // fake page variant config nodes
            this.pageVariantConfigs = {
                nodes: [],
                pagination: {
                    count: 1,
                    from: 1,
                    total: 1,
                },
            };
            this.selectedVariantPosition = 0;
            this.pageVariantConfigs.nodes = [this.selectedDefaultConfigNode];
            // create config node + link
            await this.checkForCustomPageNodeExistence();
            // reset values + reinitialize the component
            this.pageConfigCheckFailed.set(false);
            await this.initializeComponent();
        } catch (err) {
            console.error(err);
            this.topicPageHelperService.displayErrorToast();
        } finally {
            this.createCustomConfigInProgress.set(false);
        }
    }

    /**
     * Creates, links and visits an empty page template.
     */
    async createAndVisitEmptyPageTemplate(): Promise<void> {
        try {
            this.startEditing();
            // page ccm:map for page config node
            const pageConfigNode: Node = await this.topicPageHelperService.createChild(
                this.collectionId,
                RestConstants.CCM_TYPE_MAP,
                DEFAULT_PAGE_NAME_PREFIX + uuidv4(),
                DEFAULT_PAGE_CONFIG_ASPECT,
            );
            // create a page variant as a template and add it to the list of variants
            const pageVariants: string[] = [];
            const variantConfig: PageVariantConfig = {
                structure: {
                    swimlanes: [],
                },
            };
            const properties: { [key: string]: string } = {
                [DEFAULT_PAGE_VARIANT_CONFIG_PROP]: JSON.stringify(variantConfig),
                [DEFAULT_PAGE_VARIANT_IS_TEMPLATE_PROP]: 'true',
                [DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION_PROP]: DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION,
                [RestConstants.LOM_PROP_TITLE]: this.translate.instant(
                    this.i18nPrefix + 'DEFAULT_PAGE_TEMPLATE_NAME',
                ),
            };
            let pageConfigVariantNode: Node = await this.topicPageHelperService.createChild(
                retrieveNodeId(pageConfigNode),
                RestConstants.CCM_TYPE_MAP,
                DEFAULT_PAGE_VARIANT_NAME_PREFIX + uuidv4(),
                DEFAULT_PAGE_VARIANT_CONFIG_ASPECT,
                properties,
            );
            pageVariants.push(prependWorkspacePrefix(retrieveNodeId(pageConfigVariantNode)));
            // update specific node-related properties
            await this.topicPageHelperService.setProperty(
                retrieveNodeId(pageConfigVariantNode),
                DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION_PROP,
                DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION,
            );
            // update page config node
            const pageConfig: PageConfig = {
                default: pageVariants[0],
                variants: pageVariants,
            };
            await this.topicPageHelperService.setProperty(
                retrieveNodeId(pageConfigNode),
                DEFAULT_PAGE_CONFIG_PROP,
                JSON.stringify(pageConfig),
            );
            // update propagate ref
            await this.topicPageHelperService.setProperty(
                this.collectionId,
                DEFAULT_PAGE_CONFIG_PROPAGATE_REF_PROP,
                prependWorkspacePrefix(retrieveNodeId(pageConfigNode)),
            );
            await this.switchIntoTemplateMode(true);
        } catch (err) {
            console.error(err);
            this.topicPageHelperService.displayErrorToast();
        } finally {
            this.endEditing();
        }
    }

    /**
     * Helper function to add a possible non-existing page config node.
     */
    private async checkForCustomPageNodeExistence(
        pageVariantNode?: Node,
        swimlaneIndex?: number,
        gridIndex?: number,
        widget?: WidgetConfigObject,
        isHeaderNode?: boolean,
        isBreadcrumbNode?: boolean,
    ): Promise<boolean> {
        if (!this.collectionNodePageConfigRef) {
            // before creating another page config node, check which variant is currently selected
            const pageConfig: PageConfig = retrievePageConfig(this.pageConfigNode);
            const selectedVariantRefId: string =
                pageConfig.variants?.[this.selectedVariantPosition] || '';
            // page ccm:map for page config node
            this.pageConfigNode = await this.topicPageHelperService.createChild(
                this.collectionNode.ref.id,
                RestConstants.CCM_TYPE_MAP,
                DEFAULT_PAGE_NAME_PREFIX + uuidv4(),
                DEFAULT_PAGE_CONFIG_ASPECT,
            );
            const pageVariants: string[] = [];
            // iterate variant config nodes (of template) and create ccm:map children nodes for it
            // those children later contain the widget nodes
            let updatedVariantRefId: string;
            if (this.pageVariantConfigs.nodes?.length) {
                for (const variantNode of this.pageVariantConfigs.nodes) {
                    const properties: { [key: string]: string | string[] } =
                        await this.topicPageHelperService.retrievePageVariantProperties(
                            variantNode,
                        );
                    let pageConfigVariantNode: Node = await this.topicPageHelperService.createChild(
                        retrieveNodeId(this.pageConfigNode),
                        RestConstants.CCM_TYPE_MAP,
                        DEFAULT_PAGE_VARIANT_NAME_PREFIX + uuidv4(),
                        DEFAULT_PAGE_VARIANT_CONFIG_ASPECT,
                        properties,
                    );
                    pageVariants.push(
                        prependWorkspacePrefix(retrieveNodeId(pageConfigVariantNode)),
                    );
                    // hold the updated variant to set it as default later
                    if (selectedVariantRefId.includes(retrieveNodeId(variantNode))) {
                        updatedVariantRefId = prependWorkspacePrefix(
                            retrieveNodeId(pageConfigVariantNode),
                        );
                    }
                    // retrieve page variant config and remove node IDs
                    const variantConfig: PageVariantConfig = retrievePageVariantConfig(variantNode);
                    preparePageVariantConfig(variantConfig, true);
                    // check whether the function is called from the widget added event
                    // and select the correct variant to be adjusted
                    if (retrieveNodeId(pageVariantNode) === retrieveNodeId(variantNode)) {
                        // create the widget node (including properties) as children of the page variant
                        const properties: { [key: string]: string } = {
                            [DEFAULT_WIDGET_CONFIG_PROP]: JSON.stringify(widget.widgetConfig),
                        };
                        if (widget.aiConfig && Object.keys(widget.aiConfig)?.length) {
                            properties[DEFAULT_AI_CONFIG_PROP] = JSON.stringify(widget.aiConfig);
                        }
                        let widgetNode: Node = await this.topicPageHelperService.createChild(
                            retrieveNodeId(pageConfigVariantNode),
                            RestConstants.CCM_TYPE_MAP,
                            DEFAULT_WIDGET_NAME_PREFIX + uuidv4(),
                            null,
                            properties,
                        );
                        // add the widget node ID to the page variant config
                        addNodeIdToPageVariantConfig(
                            variantConfig,
                            swimlaneIndex,
                            gridIndex,
                            retrieveNodeId(widgetNode),
                            isHeaderNode,
                            isBreadcrumbNode,
                        );
                    }
                    // update the page variant config
                    await this.topicPageHelperService.setProperty(
                        retrieveNodeId(pageConfigVariantNode),
                        DEFAULT_PAGE_VARIANT_CONFIG_PROP,
                        JSON.stringify(variantConfig),
                    );
                }
            }
            let defaultVariant: string =
                this.pageVariantDefaultPosition >= 0
                    ? pageVariants?.[this.pageVariantDefaultPosition] ?? pageVariants[0]
                    : undefined;
            // override the default variant with the updated variant ref (if set)
            // and also update the selected variant position to ensure loading the correct variant
            if (updatedVariantRefId) {
                defaultVariant = updatedVariantRefId;
                this.selectedVariantPosition = pageVariants.indexOf(updatedVariantRefId);
            }
            const updatedPageConfig: PageConfig = {
                variants: pageVariants,
            };
            if (defaultVariant) {
                updatedPageConfig.default = defaultVariant;
            }
            // update ccm:page_config of page config node
            this.pageConfigNode =
                await this.topicPageHelperService.setPropertyAndRetrieveUpdatedNode(
                    retrieveNodeId(this.pageConfigNode),
                    DEFAULT_PAGE_CONFIG_PROP,
                    JSON.stringify(updatedPageConfig),
                );
            // retrieve the updated page variant configs
            await this.updatePageVariantConfigs(true);
            // parse the page config ref again
            const pageVariant: PageVariantConfig = this.retrievePageVariant();
            if (pageVariant.structure.anchorItemColor) {
                this.anchorItemColor = pageVariant.structure.anchorItemColor;
            }
            this.topicColor = retrieveTopicColor(pageVariant, this.collectionNode, this.topic());
            this.breadcrumbNodeId.set(pageVariant.structure.breadcrumbNodeId);
            this.headerNodeId.set(pageVariant.structure.headerNodeId);
            this.swimlanes = pageVariant.structure.swimlanes ?? [];
            // update the ccm:page_config_ref in the collection
            await this.topicPageHelperService.setProperty(
                retrieveNodeId(this.collectionNode),
                DEFAULT_PAGE_CONFIG_REF_PROP,
                prependWorkspacePrefix(retrieveNodeId(this.pageConfigNode)),
            );
            this.collectionNodePageConfigRef = prependWorkspacePrefix(
                retrieveNodeId(this.pageConfigNode),
            );
            return true;
        }
        return false;
    }

    /**
     * Helper function to retrieve the page variant of an existing page config node.
     */
    private retrievePageVariant(): PageVariantConfig {
        // parse the page config from the properties
        const pageConfig: PageConfig = retrievePageConfig(this.pageConfigNode);
        if (!pageConfig.variants) {
            console.error(
                this.translate.instant('TOPIC_PAGE.NO_PAGE_CONFIG.MISSING_VARIANTS'),
                pageConfig,
            );
            return null;
        }
        let selectedVariantId: string = pageConfig.variants?.[this.selectedVariantPosition];
        if (!selectedVariantId) {
            console.error(
                this.translate.instant('TOPIC_PAGE.NO_PAGE_VARIANT.NO_SELECTED_VARIANT'),
                pageConfig.variants,
                this.selectedVariantPosition,
            );
            return null;
        }
        selectedVariantId = convertNodeRefIntoNodeId(selectedVariantId);
        this.pageVariantNode = this.pageVariantConfigs.nodes?.find(
            (node: Node) => retrieveNodeId(node) === selectedVariantId,
        );
        if (!this.pageVariantNode) {
            console.error(
                this.translate.instant('TOPIC_PAGE.NO_PAGE_VARIANT.NO_SELECTED_VARIANT_NODE'),
                selectedVariantId,
                this.pageVariantConfigs.nodes,
            );
            return null;
        }
        this.pageVariantNodeIndex = this.pageVariantConfigs.nodes.findIndex(
            (n) => retrieveNodeId(n) === retrieveNodeId(this.pageVariantNode),
        );
        this.pageVariantSettingsValid.set(true);
        const pageVariant: PageVariantConfig = retrievePageVariantConfig(this.pageVariantNode);
        if (!pageVariant || !pageVariant.structure) {
            console.error(
                this.translate.instant('TOPIC_PAGE.NO_PAGE_VARIANT.VARIANT_OR_STRUCTURE_MISSING'),
                pageVariant,
            );
            return null;
        }
        return pageVariant;
    }

    /**
     * Helper function to reload the page variant configs.
     *
     * @param reloadNecessary
     */
    private async updatePageVariantConfigs(reloadNecessary: boolean = false): Promise<void> {
        if (reloadNecessary) {
            this.pageVariantConfigs = await this.topicPageHelperService.getNodeChildren(
                retrieveNodeId(this.pageConfigNode),
            );
            return;
        }
        // update the page variant node inside the page variant configs
        const index = this.pageVariantConfigs.nodes.findIndex(
            (n) => retrieveNodeId(n) === retrieveNodeId(this.pageVariantNode),
        );
        this.pageVariantConfigs.nodes[index] = this.pageVariantNode;
    }

    /**
     * Helper function to start an editing process by setting requestInProgress to true and opening a toast to inform the user about it.
     *
     * @param msg
     */
    private startEditing(msg?: string): void {
        this.requestInProgress.set(true);
        this.topicPageHelperService.openSaveConfigToast(msg);
    }

    /**
     * Helper function to end an editing process by triggering an anchor update, setting requestInProgress to false and checking the accordion expansion state.
     */
    private endEditing(): void {
        this.anchorTrigger++;
        this.requestInProgress.set(false);
        // wait for the swimlane to be loaded before checking whether all accordions are opened in edit mode
        setTimeout((): void => {
            this.checkAccordionExpansionState();
        });
    }

    /**
     * Helper function to check whether all accordions are opened when in edit mode.
     */
    private checkAccordionExpansionState(): void {
        if (this.editMode()) {
            this.accordions?.forEach((accordion: CdkAccordionItem): void => {
                accordion.open();
            });
        }
    }

    /**
     * Helper function to close potentially opened side menus.
     */
    private closeSideMenus(): void {
        this.selectedMenuItem = '';
        this.pageVariantSettingsValid.set(true);
    }

    /**
     * Helper function to retrieve the best matching between given page variant nodes and selected values.
     */
    private retrieveBestMatchingVariantIndex(
        pageVariants: Node[],
        parameterSelection: { [key: string]: string[] },
    ): number {
        // early return, if no selection is available
        if (!Object.keys(parameterSelection).length) {
            return -1;
        }
        const numberOfUserSelectionVariables = Object.values(parameterSelection).reduce(
            (sum, values) => sum + values.length,
            0,
        );
        let highestNumberOfMatches = 0;
        let bestMatchIndex = -1;
        // prefer fewer variables
        let bestMatchNumberOfVariables = Number.MAX_VALUE;

        for (let index = 0; index < pageVariants.length; index++) {
            const pageVariantNode: Node = pageVariants[index];

            // count the number of selected values from the page variant
            let variantNumberOfVariables: number = 0;
            Array.from(this.selectDimensions.keys()).forEach((dimension: string) => {
                variantNumberOfVariables += pageVariantNode.properties[dimension]?.length ?? 0;
            });
            let totalMatches: number = 0;

            // count matches per dimension
            for (const [dimensionKey, selectedValues] of Object.entries(parameterSelection)) {
                // retrieve the selected values directly from the according property
                const storedValues: string[] = pageVariantNode.properties[dimensionKey];
                if (storedValues?.length) {
                    selectedValues.forEach((val) => {
                        if (storedValues.includes(val)) {
                            totalMatches++;
                        }
                    });
                }
            }

            if (totalMatches > 0 && totalMatches >= highestNumberOfMatches) {
                if (totalMatches > highestNumberOfMatches) {
                    highestNumberOfMatches = totalMatches;
                    bestMatchIndex = index;
                    bestMatchNumberOfVariables = variantNumberOfVariables;
                } else {
                    // prefer variant with fewer variables
                    if (variantNumberOfVariables < bestMatchNumberOfVariables) {
                        highestNumberOfMatches = totalMatches;
                        bestMatchIndex = index;
                        bestMatchNumberOfVariables = variantNumberOfVariables;
                    }
                }

                // an exact match is found
                if (
                    totalMatches === numberOfUserSelectionVariables &&
                    variantNumberOfVariables === numberOfUserSelectionVariables
                ) {
                    return index;
                }
            }
        }
        return bestMatchIndex;
    }

    protected readonly iconPath: string = DEFAULT_ICON_PATH;
    protected readonly pageVariantConfigPrefix = DEFAULT_PAGE_VARIANT_NAME_PREFIX;
    protected readonly SwimlaneBackgroundShape = SwimlaneBackgroundShape;
    protected readonly swimlaneShapeToImageMapping = SWIMLANE_SHAPE_TO_IMAGE_MAPPING;
    protected readonly WIDGETS = WIDGETS;
}
