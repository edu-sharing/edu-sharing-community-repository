import {
    AfterViewInit,
    Component,
    computed,
    CUSTOM_ELEMENTS_SCHEMA,
    EventEmitter,
    input,
    Input,
    InputSignal,
    model,
    ModelSignal,
    OnDestroy,
    Output,
    Signal,
    signal,
    ViewEncapsulation,
    WritableSignal,
    inject,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { NavigationExtras } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule, NodeTitlePipe } from 'ngx-edu-sharing-ui';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { SharedModule } from '../../../../shared/shared.module';
import { RenderWrapperComponent } from '../../../render2-page/render-wrapper-component/render-wrapper.component';
import { HighlightSearchPipe } from '../../shared/pipes/highlight-search.pipe';
import { TopicPageGlobalService } from '../../shared/services/topic-page-global.service';
import { TopicPageHelperService } from '../../shared/services/topic-page-helper.service';
import { ConfigurationOption } from '../../shared/types/configuration-option';
import { DEFAULT_COLLECTION_ID_PROP } from '../../shared/types/custom-definitions';
import { LayoutOption } from '../../shared/types/layout-option';
import { MediaRenderingDisplayType } from '../../shared/types/media-rendering-display-type';
import { MediaRenderingConfig } from '../../shared/types/widget-config/media-rendering-config';
import { WidgetComponentInterface } from '../generic-widget/generic-widget.component';
import { WidgetConfigurationButtonsComponent } from '../shared/widget-configuration-buttons/widget-configuration-buttons.component';
import { PreviewSidebarService } from '../../../../features/editorial-sidebar/preview-sidebar/preview-sidebar.service';

@Component({
    selector: 'es-media-rendering',
    encapsulation: ViewEncapsulation.Emulated,
    imports: [
        EduSharingUiCommonModule,
        FormsModule,
        MatButtonModule,
        MatButtonToggleModule,
        MatFormFieldModule,
        MatInputModule,
        RenderWrapperComponent,
        SharedModule,
        TranslateModule,
        WidgetConfigurationButtonsComponent,
    ],
    providers: [HighlightSearchPipe, NodeTitlePipe],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    templateUrl: './media-rendering.component.html',
    styleUrls: ['./media-rendering.component.scss'],
})
export class MediaRenderingComponent implements AfterViewInit, OnDestroy, WidgetComponentInterface {
    private highlightSearch = inject(HighlightSearchPipe);
    private nodeTitlePipe = inject(NodeTitlePipe);
    private previewSidebarService = inject(PreviewSidebarService);
    private topicPageGlobalService = inject(TopicPageGlobalService);
    private topicPageHelperService = inject(TopicPageHelperService);

    // CONSTANTS
    readonly i18nPrefix: string = 'TOPIC_PAGE.WIDGET.MEDIA_RENDERING.';

    // INPUTS + OUTPUTS
    @Input() contextNodeId: string;
    editMode: InputSignal<boolean> = input<boolean>(false);
    @Input() embedConfigurationOption?: ConfigurationOption;
    @Input() gridIndex: number = -1;
    headline: InputSignal<string> = input<string>('');
    @Input() nodeId?: string;
    @Input() pageVariantNode?: Node;
    private _searchInput: string;
    @Input() get searchInput(): string {
        return this._searchInput;
    }
    set searchInput(value: string) {
        this._searchInput = value;
        this.computeSelectedNodeTitle();
    }
    @Input() swimlaneIndex: number = -1;

    @Output() configChanged: EventEmitter<void> = new EventEmitter<void>();
    @Output() embedWidgetClicked: EventEmitter<void> = new EventEmitter<void>();
    @Output() internalSearchResultCountChanged: EventEmitter<number> = new EventEmitter<number>();
    @Output() itemClickedEvent: EventEmitter<Node> = new EventEmitter<Node>();
    @Output() totalSearchResultCountChanged: EventEmitter<number> = new EventEmitter<number>();
    @Output() visibleNodesChanged: EventEmitter<Node[]> = new EventEmitter<Node[]>();

    private destroy$ = new Subject<void>();
    initialized: WritableSignal<boolean> = signal(false);
    layout: ModelSignal<MediaRenderingDisplayType> = model(MediaRenderingDisplayType.Preview);
    layoutOptions: LayoutOption[] = [
        {
            ariaLabel: 'PREVIEW_ARIA',
            icon: 'looks_one',
            value: MediaRenderingDisplayType.Preview,
            viewValue: 'PREVIEW',
        },
        {
            ariaLabel: 'PREVIEW_TITLE_ARIA',
            icon: 'looks_two',
            value: MediaRenderingDisplayType.TitlePreview,
            viewValue: 'PREVIEW_TITLE',
        },
        {
            ariaLabel: 'PREVIEW_TITLE_BUTTONS_ARIA',
            icon: 'looks_3',
            value: MediaRenderingDisplayType.TitlePreviewButtons,
            viewValue: 'PREVIEW_TITLE_BUTTONS',
        },
    ];
    layoutWithTitle: Signal<boolean> = computed((): boolean =>
        [
            this.MediaRenderingLayout.TitlePreview,
            this.MediaRenderingLayout.TitlePreviewButtons,
        ].includes(this.layout()),
    );
    selectedNode: Node;
    selectedNodeTitle: WritableSignal<string> = signal('');
    sidebarOpen: WritableSignal<boolean> = signal(false);
    updateInProgress: WritableSignal<boolean> = signal(false);
    private windowRef: Window | null = null;

    constructor() {
        // subscribe to changes on the selected node
        this.previewSidebarService
            .getCurrentNode()
            .pipe(takeUntil(this.destroy$))
            .subscribe((node: Node | null): void => {
                const selectedNode: Node = node;
                if (selectedNode?.ref.id && selectedNode.ref.id === this.selectedNode?.ref.id) {
                    this.sidebarOpen.set(true);
                } else {
                    this.sidebarOpen.set(false);
                }
            });
    }

    /**
     * Initializes a custom event listener and the media rendering component itself.
     */
    async ngAfterViewInit(): Promise<void> {
        this.initializeCustomEventListeners();
    }

    /**
     * On destruction, remove all event listeners previously added.
     */
    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
        window.removeEventListener('message', this.handleApplyNode, false);
    }

    /**
     * Handles the initialization of custom event listeners, e.g., to react to node selections.
     */
    private initializeCustomEventListeners(): void {
        // note: the arrow function is necessary to correctly access "this."
        window.addEventListener('message', this.handleApplyNode, false);
    }

    /**
     * Handles the receiving of the APPLY_NODE event.
     *
     * @param event
     */
    private handleApplyNode = async (event: MessageEvent<any>): Promise<void> => {
        // APPLY_NODE event was received and windowRef must exist and not being closed already (as multiple listeners might exist)
        if (event.data.event === 'APPLY_NODE' && !!this.windowRef && !this.windowRef.closed) {
            this.windowRef.close();
            // reset the reference to be not called twice
            this.windowRef = null;
            const selectedNode: Node = event.data.data;
            // check whether the node can be read
            const node: Node = await this.topicPageHelperService.getNode(selectedNode.ref.id);
            if (!node) {
                return;
            }
            // run any registered selection validator
            // a validator may cancel the selection silently and inform the user itself
            const isValid: boolean = await this.topicPageGlobalService.validateNodeSelection(node);
            if (!isValid) {
                return;
            }
            // workaround to properly update the selected node
            this.selectedNode = null;
            setTimeout((): void => {
                this.selectedNode = node;
                this.computeSelectedNodeTitle();
                this.configChanged.emit();
                this.emitVisibleNode();
            });
        }
    };

    /**
     * Opens a new window with the Re-URL parameter set.
     */
    openReurlLink(): void {
        // set filter to collection ID
        const mediaRenderingExtra: NavigationExtras = {
            queryParams: {
                filters: {
                    [DEFAULT_COLLECTION_ID_PROP]: [this.contextNodeId],
                },
            },
        };
        this.windowRef = this.topicPageHelperService.openReurlLink(mediaRenderingExtra);
    }

    /**
     * Handles the embedding of the widget by emitting an embed widget clicked event.
     */
    embedWidget(): void {
        this.embedWidgetClicked.emit();
    }

    /**
     * Changes the layout by persisting it and updating the search hits.
     */
    async layoutChanged(): Promise<void> {
        this.configChanged.emit();
        this.computeSelectedNodeTitle();
    }

    /**
     * Emits the click event for the selected node.
     */
    itemClicked(): void {
        this.itemClickedEvent.emit(this.selectedNode);
        this.previewSidebarService.handleNodeClick(this.selectedNode);
    }

    // noinspection JSUnusedGlobalSymbols
    /**
     * Postload action emitting the statistics.
     */
    async postLoadAction(): Promise<void> {
        this.emitVisibleNode();
    }

    // noinspection JSUnusedGlobalSymbols
    /**
     * Called by generic-widget component to retrieve a widget config from the currently set variables in the component.
     */
    retrieveWidgetConfig(): MediaRenderingConfig {
        let widgetConfig: MediaRenderingConfig = {
            mediaRenderingLayout: this.layout(),
        };
        if (this.selectedNode?.ref.id) {
            widgetConfig.selectedNodeId = this.selectedNode.ref.id;
        }
        return widgetConfig;
    }

    // noinspection JSUnusedGlobalSymbols
    /**
     * Called by generic-widget component to set widget-specific values.
     *
     * @param config
     */
    async setWidgetValues(config: MediaRenderingConfig): Promise<void> {
        if (config.mediaRenderingLayout !== undefined) {
            this.layout.set(config.mediaRenderingLayout);
        }
        if (config.selectedNodeId) {
            this.selectedNode = await this.topicPageHelperService.getNode(config.selectedNodeId);
            this.computeSelectedNodeTitle();
        }
    }

    // HELPERS
    /**
     * Helper function to compute the title of the selected node and update the number of search hits.
     */
    private computeSelectedNodeTitle(): void {
        if (!this.selectedNode) {
            this.internalSearchResultCountChanged.emit(0);
            return;
        }
        let outputTitle: string = this.nodeTitlePipe.transform(this.selectedNode);
        // search, if necessary
        if (
            this.searchInput &&
            [
                this.MediaRenderingLayout.TitlePreview,
                this.MediaRenderingLayout.TitlePreviewButtons,
            ].includes(this.layout())
        ) {
            outputTitle = this.highlightSearch.transform(outputTitle, this.searchInput);
        }
        // emit number of hits
        const numberOfHits: number = (
            outputTitle.match(new RegExp('class="topic-page-search-highlight"', 'g')) || []
        ).length;
        this.internalSearchResultCountChanged.emit(numberOfHits);
        this.selectedNodeTitle.set(outputTitle);
    }

    /**
     * Helper function to emit the statistics of the selected node.
     */
    private emitVisibleNode(): void {
        if (this.selectedNode) {
            this.visibleNodesChanged.emit([this.selectedNode]);
            this.totalSearchResultCountChanged.emit(1);
        } else {
            this.visibleNodesChanged.emit([]);
            this.totalSearchResultCountChanged.emit(0);
        }
    }

    protected readonly MediaRenderingLayout = MediaRenderingDisplayType;
}
