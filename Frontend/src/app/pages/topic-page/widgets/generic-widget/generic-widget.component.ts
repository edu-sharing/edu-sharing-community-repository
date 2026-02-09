import { Clipboard } from '@angular/cdk/clipboard';
import { PlatformLocation } from '@angular/common';
import {
    AfterViewInit,
    Component,
    ComponentRef,
    DestroyRef,
    ElementRef,
    EventEmitter,
    inject,
    Injector,
    Input,
    input,
    InputSignal,
    OnChanges,
    OnDestroy,
    Output,
    signal,
    SimpleChanges,
    TemplateRef,
    ViewChild,
    ViewContainerRef,
    ViewEncapsulation,
    WritableSignal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslateService } from '@ngx-translate/core';
import { MdsValue, MdsWidget, Node, SearchService } from 'ngx-edu-sharing-api';
import { ChatCompletionResult, NodeConfig } from 'ngx-edu-sharing-b-api';
import { UIService } from 'ngx-edu-sharing-ui';
import { Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { ConfigureWidgetEmbeddingDialogComponent } from './configure-widget-embedding-dialog/configure-widget-embedding-dialog.component';
import { WidgetHeaderComponent } from './generic-widget-header/generic-widget-header.component';
import { WidgetConfig } from '../../shared/types/widget-config/widget-config';
import { BapiConfigObject } from '../../shared/types/bapi-config-object';
import { ConfigurationOption } from '../../shared/types/configuration-option';
import { DEFAULT_BG_COLOR, WIDGET_TYPE, WIDGETS } from '../../shared/types/custom-definitions';
import { SwimlaneBackgroundShape } from '../../shared/types/swimlane-background-shape';
import { StatisticNode } from '../../shared/types/statistic-node';
import { CardDialogRef } from '../../../../features/dialogs/card-dialog/card-dialog-ref';
import { PromptToTextMapping } from '../../shared/types/prompt-to-text-mapping';
import { SharedModule } from '../../../../shared/shared.module';
import { AiHelperService } from '../../shared/services/ai-helper.service';
import { DialogsService } from '../../../../features/dialogs/dialogs.service';
import { GlobalWidgetConfigService } from '../../shared/services/global-widget-config.service';
import { Toast, ToastType } from '../../../../services/toast';
import {
    convertNodeRefIntoNodeId,
    retrieveAiConfigFromNode,
    retrieveCustomUrl,
    retrievePromptFromAiConfig,
    retrieveWidgetConfigFromNode,
} from '../../shared/utils/template-util';
import { TopicPageHelperService } from '../../shared/services/topic-page-helper.service';
import {
    containsAiTags,
    retrieveBapiConfigObject,
    retrieveResultString,
} from '../../shared/utils/ai-util';
import { BaseWidgetConfig } from '../../shared/types/widget-config/base-widget-config';
import { getNodeOrDefaultNodeId } from '../../shared/utils/node-util';
import { Closable } from '../../../../features/dialogs/card-dialog/card-dialog-config';
import { GenericWidgetGlobalService } from './generic-widget-global.service';

export interface WidgetComponentInterface {
    // inputs
    contextNodeId: string;
    editMode: InputSignal<boolean>;
    embedConfigurationOption?: ConfigurationOption;
    gridIndex: number;
    pageVariantNode?: Node;
    swimlaneIndex: number;

    // outputs
    configChanged: EventEmitter<void>;
    embedWidgetClicked: EventEmitter<void>;

    // properties
    initialized: WritableSignal<boolean>;
    updateInProgress: WritableSignal<boolean>;

    // methods
    preLoadAction?(): Promise<void>;
    retrieveWidgetConfig?(): WidgetConfig;
    setWidgetValues?(widgetConfig: WidgetConfig, aiConfig?: BapiConfigObject): Promise<void> | void;
    retrieveCustomAiKeyValuePairs?(): { [key: string]: string };
    postLoadAction?(): Promise<void>;
}

@Component({
    selector: 'es-generic-widget',
    encapsulation: ViewEncapsulation.Emulated,
    imports: [SharedModule, WidgetHeaderComponent, ConfigureWidgetEmbeddingDialogComponent],
    providers: [SearchService],
    templateUrl: './generic-widget.component.html',
    styleUrls: ['./generic-widget.component.scss'],
})
export class GenericWidgetComponent implements AfterViewInit, OnChanges, OnDestroy {
    @ViewChild('widgetContainer', { read: ViewContainerRef, static: true })
    widgetContainer!: ViewContainerRef;
    @ViewChild('widgetContainer') widgetContainerElement!: ElementRef<HTMLElement>;

    // INPUTS
    // if configOverwrite is set, it replaces the fetched config, allowing to directly control
    // the widget, which will be important, when embedding the widget.
    @Input() configOverwrite: string;
    @Input() contextNodeId: string;
    editMode: InputSignal<boolean> = input<boolean>(false);
    @Input() gridIndex: number = -1;
    @Input() nodeId: string = '';
    @Input() pageVariantNode?: Node;
    @Input() propagatedNodeId: string;
    searchInput: InputSignal<string> = input<string>(null);
    @Input() swimlaneColor: string = DEFAULT_BG_COLOR;
    @Input() swimlaneIndex: number = -1;
    @Input() swimlaneShape: SwimlaneBackgroundShape = SwimlaneBackgroundShape.None;
    @Input() widgetType: WIDGET_TYPE | string = WIDGETS.CONTENT_TEASER;

    // Additional inputs that might be specific to certain widgets
    @Input() customUrl?: (collection: Node) => string;
    @Input() defaultNodeId: string = '';
    @Input() height?: string;
    @Input() hideDescription: boolean = false;
    @Input() isEmbedMode: boolean = false;
    @Input() searchText: string = '';
    @Input() selectDimensions: Map<string, MdsWidget> = new Map<string, MdsWidget>();
    @Input() selectedDimensionValues: MdsValue[] = [];
    @Input() sidebarEmbedding: boolean = false;

    // OUTPUTS
    @Output() itemClickedEvent: EventEmitter<Node> = new EventEmitter<Node>();
    @Output() nodeStatisticsChanged: EventEmitter<StatisticNode[]> = new EventEmitter<
        StatisticNode[]
    >();
    @Output() searchInputHitsChanged: EventEmitter<boolean> = new EventEmitter<boolean>();
    @Output() totalSearchResultCountChanged: EventEmitter<number> = new EventEmitter<number>();

    @ViewChild('configureWidgetEmbeddingTemplate')
    configureWidgetEmbeddingTemplateRef: TemplateRef<undefined>;
    configureWidgetEmbeddingDialogRef: CardDialogRef;

    // VARIABLES
    description: string;
    descriptionMapping: PromptToTextMapping;
    descriptionAiGenerated: WritableSignal<boolean> = signal(false);
    private destroyRef: DestroyRef = inject(DestroyRef);
    embedConfigurationOption: ConfigurationOption;
    headline: string;
    headlineMapping: PromptToTextMapping;
    headlineAiGenerated: WritableSignal<boolean> = signal(false);
    initialized: WritableSignal<boolean> = signal(false);
    private searchResults: Map<string, number> = new Map<string, number>();
    updateInProgress: WritableSignal<boolean> = signal(false);
    private updateSearchInputCount$: Subject<void> = new Subject<void>();
    private viewInitialized: boolean = false;
    private widgetComponentRef: ComponentRef<any> | null = null;
    widgetInstance: WidgetComponentInterface | null = null;
    private widgetNode: Node;

    constructor(
        private aiHelperService: AiHelperService,
        private clipboard: Clipboard,
        private dialogs: DialogsService,
        private injector: Injector,
        private genericWidgetGlobalService: GenericWidgetGlobalService,
        private globalWidgetConfigService: GlobalWidgetConfigService,
        private platformLocation: PlatformLocation,
        private toast: Toast,
        private topicPageHelperService: TopicPageHelperService,
        private translate: TranslateService,
        private uiService: UIService,
    ) {
        this.updateSearchInputCount$
            .pipe(debounceTime(1000), takeUntilDestroyed(this.destroyRef))
            .subscribe((): void => {
                const hasHits: boolean = [...this.searchResults.values()].some(
                    (count) => count > 0,
                );
                this.searchInputHitsChanged.emit(hasHits);
            });

        // listen to changes in the selected variables and update potentially AI-generated properties
        this.topicPageHelperService
            .getSelectedVariablesSubject()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((): void => {
                if (!this.widgetNode) {
                    return;
                }
                const widgetConfig: WidgetConfig = retrieveWidgetConfigFromNode(this.widgetNode);
                const aiConfig: BapiConfigObject = retrieveAiConfigFromNode(this.widgetNode);
                // only update if an AI config does exist
                if (aiConfig && Object.keys(aiConfig).length) {
                    void this.updateCommonProperties(widgetConfig, aiConfig);
                }
            });
    }

    /**
     * Handles the initial loading of the widget component.
     */
    async ngAfterViewInit() {
        if (this.viewInitialized) {
            return;
        }
        this.viewInitialized = true;

        // define a common embed configuration option to be used in every widget
        this.updateCommonConfigurationOptions();

        // initial load
        await this.loadWidgetComponent();
    }

    /**
     * Handles changes made via input to the widget component.
     *
     * @param changes
     */
    async ngOnChanges(changes: SimpleChanges): Promise<void> {
        // allow later changes if certain inputs were changed
        if (
            this.viewInitialized &&
            (changes.editMode ||
                changes.gridIndex ||
                changes.searchInput ||
                changes.selectedDimensionValues ||
                changes.swimlaneColor ||
                changes.swimlaneIndex ||
                changes.swimlaneShape ||
                changes.widgetType)
        ) {
            // process incoming changes
            await this.processChanges(changes);
        }
        // nodeId was updated (read widget configuration again)
        if (
            this.viewInitialized &&
            (changes.configOverwrite ||
                (!changes.nodeId?.firstChange &&
                    changes.nodeId?.currentValue !== changes.nodeId?.previousValue))
        ) {
            // update common configuration options as
            this.updateCommonConfigurationOptions();
            // process incoming changes
            await this.processChanges(changes);
            // read widget configuration (again) and set widget values
            await this.readWidgetConfig();
        }
    }

    /**
     * Handles the destruction of the widget component.
     */
    ngOnDestroy(): void {
        if (this.widgetComponentRef) {
            this.widgetComponentRef.destroy();
        }
    }

    /**
     * Persists the currently defined config.
     */
    async persistConfig(): Promise<void> {
        this.updateInProgress.set(true);
        this.widgetInstance.updateInProgress.set(true);
        // retrieve the individual widget config
        let baseConfig: BaseWidgetConfig = this.retrieveBaseWidgetConfig();
        let specificWidgetConfig: WidgetConfig = this.widgetInstance.retrieveWidgetConfig
            ? this.widgetInstance.retrieveWidgetConfig()
            : {};
        let widgetConfig: WidgetConfig = { ...baseConfig, ...specificWidgetConfig };
        // retrieve an AI config object if either the description or the headline contains AI tags
        const aiDescription: string = containsAiTags(this.description) ? this.description : '';
        const aiHeadline: string = containsAiTags(this.headline) ? this.headline : '';
        let keyValue;
        if (this.widgetInstance.retrieveCustomAiKeyValuePairs) {
            keyValue = this.widgetInstance.retrieveCustomAiKeyValuePairs();
        }
        let aiConfig: BapiConfigObject = retrieveBapiConfigObject(
            aiDescription,
            aiHeadline,
            keyValue,
        );
        // persist config by creating new or updating existing node
        this.widgetNode = await this.topicPageHelperService.persistConfig(
            this.nodeId,
            this.gridIndex,
            this.swimlaneIndex,
            this.pageVariantNode,
            widgetConfig,
            aiConfig,
            this.contextNodeId,
            false,
            false,
        );
        // this is the case for updated nodes
        // as those do already exist, the properties need to be synchronized
        if (this.widgetNode) {
            // retrieve both the widget and the AI config from the node and update the values in the widget
            widgetConfig = retrieveWidgetConfigFromNode(this.widgetNode);
            aiConfig = retrieveAiConfigFromNode(this.widgetNode);
            if (this.widgetInstance.setWidgetValues) {
                const result: void | Promise<void> = this.widgetInstance.setWidgetValues(
                    widgetConfig,
                    aiConfig,
                );
                if (result instanceof Promise) {
                    await result;
                }
            }
            await this.updateCommonProperties(widgetConfig, aiConfig);
        }
        this.updateInProgress.set(false);
        this.widgetInstance.updateInProgress.set(false);
    }

    /**
     * Updates the common properties of the widget (description, headline + mappings).
     *
     * @param widgetConfig
     * @param aiConfig
     */
    async updateCommonProperties(
        widgetConfig: WidgetConfig,
        aiConfig: BapiConfigObject,
    ): Promise<void> {
        // reset the mappings
        this.headlineMapping = null;
        this.headlineAiGenerated.set(false);
        this.descriptionMapping = null;
        this.descriptionAiGenerated.set(false);
        // set description and headline
        if (widgetConfig.description !== undefined) {
            this.description = widgetConfig.description;
        }
        if (widgetConfig.headline) {
            this.headline = widgetConfig.headline;
        }
        // in case an AI config is defined, execute the prompts and store the results
        if (aiConfig && Object.keys(aiConfig).length) {
            if (aiConfig.headline) {
                const config: NodeConfig = {
                    type: 'node',
                    nodeId: convertNodeRefIntoNodeId(this.nodeId || this.propagatedNodeId),
                    configName: 'headline',
                };
                const promptResponse: ChatCompletionResult =
                    await this.aiHelperService.generateFromPrompt(
                        config,
                        this.topicPageHelperService.getSelectedVariables() || {},
                        this.contextNodeId,
                    );
                const responseText: string = retrieveResultString(promptResponse);
                const prompt: string = retrievePromptFromAiConfig(aiConfig, 'headline');
                // make sure to sync both headline and prompt
                this.headline = prompt;
                if (prompt && responseText) {
                    this.headlineMapping = new PromptToTextMapping(prompt, responseText);
                    this.headlineAiGenerated.set(true);
                }
            }
            if (aiConfig.description) {
                const config: NodeConfig = {
                    type: 'node',
                    nodeId: convertNodeRefIntoNodeId(this.nodeId || this.propagatedNodeId),
                    configName: 'description',
                };
                const promptResponse: ChatCompletionResult =
                    await this.aiHelperService.generateFromPrompt(
                        config,
                        this.topicPageHelperService.getSelectedVariables() || {},
                        this.contextNodeId,
                    );
                const responseText: string = retrieveResultString(promptResponse);
                const prompt: string = retrievePromptFromAiConfig(aiConfig, 'description');
                this.description = prompt;
                if (responseText) {
                    this.descriptionMapping = new PromptToTextMapping(prompt, responseText);
                    this.descriptionAiGenerated.set(true);
                }
            }
            if (this.widgetType === WIDGETS.MEDIA_RENDERING) {
                this.widgetComponentRef.setInput('headline', this.headline);
            }
            this.updateInProgress.set(false);
            this.widgetInstance.updateInProgress.set(false);
        }
    }

    /**
     * Embeds the widget depending on the selected widget embedding mode.
     */
    async embedWidget(mode: 'nodeId' | 'configOverwrite' = 'nodeId'): Promise<void> {
        // prepare embedding code output
        let embeddingCode: string = '';
        const baseUrl: string = window.location.origin + this.platformLocation.getBaseHrefFromDOM();
        const webComponentBaseHref: string = baseUrl + 'web-components/app/';

        // set API URL for embedding
        embeddingCode +=
            '<script>\n' +
            '    window.__env = {\n' +
            '        EDU_SHARING_API_URL: "' +
            baseUrl +
            'rest"\n' +
            '    };\n' +
            '</script>\n';
        // web-component specific files
        embeddingCode += '<script src="' + webComponentBaseHref + 'polyfills.js"></script>\n';
        embeddingCode +=
            '<script type="module" src="' + webComponentBaseHref + 'main.js"></script>\n';
        embeddingCode +=
            '<link rel="stylesheet" type="text/css" href="' +
            webComponentBaseHref +
            'styles.css" />\n';
        // build wlo-generic-widget tag dynamically
        embeddingCode += '<edu-sharing-generic-widget';

        // add required attributes
        if (this.contextNodeId) {
            embeddingCode += ` context-node-id="${this.contextNodeId}"`;
        }

        if (this.widgetType) {
            embeddingCode += ` widget-type="${this.widgetType}"`;
        }

        if (mode === 'nodeId') {
            if (this.nodeId) {
                embeddingCode += ` node-id="${this.nodeId}"`;
            } else if (this.propagatedNodeId) {
                embeddingCode += ` propagated-node-id="${this.propagatedNodeId}"`;
            }
        } else {
            // retrieve config overwrite from the widget
            let baseConfig: BaseWidgetConfig = this.retrieveBaseWidgetConfig();
            let specificWidgetConfig: WidgetConfig = this.widgetInstance.retrieveWidgetConfig
                ? this.widgetInstance.retrieveWidgetConfig()
                : {};
            const widgetConfig: WidgetConfig = { ...baseConfig, ...specificWidgetConfig };
            // special actions for potentially AI-generated properties (i.e., headline, description, prompt)
            // those cannot be generated via the configOverwrite, as they are node-dependent
            if (Object.keys(widgetConfig).length) {
                const commonAiProperties: (keyof WidgetConfig)[] = ['headline', 'description'];
                commonAiProperties.forEach((property) => {
                    if (widgetConfig[property] && containsAiTags(widgetConfig[property])) {
                        if (property === 'headline' && this.headlineMapping?.text) {
                            widgetConfig[property] = this.headlineMapping.text;
                        }
                        if (property === 'description' && this.descriptionMapping?.text) {
                            widgetConfig[property] = this.descriptionMapping.text;
                        }
                    }
                });

                // special case for AI text widget (execute prompt and store result in texts)
                if (
                    'prompt' in widgetConfig &&
                    widgetConfig.prompt &&
                    containsAiTags(widgetConfig.prompt)
                ) {
                    // as the generation takes place in the widget, we duplicate the function here
                    const defaultConfigId: string = getNodeOrDefaultNodeId(
                        this.defaultNodeId,
                        this.globalWidgetConfigService.defaultAiTextWidgetConfigId,
                    );
                    const config: NodeConfig = {
                        type: 'node',
                        nodeId: convertNodeRefIntoNodeId(this.nodeId || this.propagatedNodeId),
                        configName: 'prompt',
                    };
                    const selectedVariables =
                        this.topicPageHelperService.getSelectedVariables() || {};
                    const promptResponse = await this.aiHelperService.generateFromPrompt(
                        this.nodeId || this.propagatedNodeId ? config : defaultConfigId,
                        selectedVariables,
                        this.contextNodeId,
                    );
                    const resultString: string = retrieveResultString(promptResponse);
                    if (resultString) {
                        // delete the prompt and store a variable for texts instead
                        delete widgetConfig.prompt;
                        if (!widgetConfig.texts) {
                            widgetConfig.texts = [];
                        }
                        // create a new output object
                        const currentUser: string = await this.aiHelperService.getCurrentUser();
                        widgetConfig.texts = [
                            {
                                textValue: {
                                    text: resultString,
                                    updatedAt: Date.now(),
                                    updatedBy: currentUser,
                                },
                            },
                        ];
                    }
                }
                embeddingCode += ` config-overwrite="${JSON.stringify(widgetConfig).replace(
                    /"/g,
                    '&quot;',
                )}"`;
            }
        }

        // add additional widget-specific attributes based on widget type
        if (this.widgetType === WIDGETS.CONTENT_TEASER && this.searchText) {
            embeddingCode += ` search-text="${this.searchText}"`;
        }

        // close the tag
        embeddingCode += `></edu-sharing-generic-widget>`;
        this.clipboard.copy(embeddingCode);
        // inform user about code being copied successfully
        this.toast.show({
            message: 'TOPIC_PAGE.WIDGET.EMBEDDING.CODE_COPIED',
            type: 'info',
            subtype: ToastType.InfoSimple,
        });
    }

    /**
     * Opens a dialog to select between different widget embedding modes.
     */
    async openEmbedWidgetDialog(): Promise<void> {
        this.configureWidgetEmbeddingDialogRef = await this.dialogs.openGenericDialog({
            title: 'TOPIC_PAGE.CONFIG_WIDGET_EMBEDDING.HEADING',
            minWidth: '700px',
            maxWidth: '100%',
            contentTemplate: this.configureWidgetEmbeddingTemplateRef,
            closable: Closable.Casual,
            buttons: [{ label: 'CANCEL', config: { color: 'standard' } }],
        });
    }

    /**
     * Retrieves the base widget config.
     */
    private retrieveBaseWidgetConfig(): BaseWidgetConfig {
        return {
            description: this.description,
            headline: this.headline,
        };
    }

    /**
     * Reacts to wlo-generic-widget-header (textChange) event by updating the widget config.
     *
     * @param event
     */
    async onHeaderTextChange(event: { text: string; isHeadline: boolean }): Promise<void> {
        if (this.widgetInstance) {
            if (event.isHeadline) {
                this.headline = event.text;
                if (this.widgetType === WIDGETS.MEDIA_RENDERING) {
                    this.widgetComponentRef.setInput('headline', this.headline);
                }
            } else {
                this.description = event.text;
            }
            await this.persistConfig();
        }
    }

    /**
     * Reacts to wlo-editable-text (searchResultsUpdated) event by setting the search results
     * and updating the search input count.
     *
     * @param event
     */
    onSearchResultsUpdated(event: { count: number; type: string }): void {
        this.searchResults.set(event.type, event.count);
        this.updateSearchInputCount$.next();
    }

    /**
     * Reacts to wlo-editable-text (searchResultsUpdated) event and emit it.
     *
     * @param count
     * @param type
     */
    updateSearchResults(count: number, type: string): void {
        this.searchResults.set(type, count);
        this.updateSearchInputCount$.next();
    }

    // HELPERS
    /**
     * Helper function to update the common configuration options.
     */
    private updateCommonConfigurationOptions(): void {
        this.embedConfigurationOption = new ConfigurationOption(
            true,
            this.translate.instant('TOPIC_PAGE.WIDGET.EMBEDDING.LABEL'),
        );
        this.embedConfigurationOption.icon = 'code';
    }

    /**
     * Helper function to dynamically load the widget component.
     */
    private async loadWidgetComponent(): Promise<void> {
        if (!this.widgetContainer) {
            console.error(
                this.translate.instant('TOPIC_PAGE.WIDGET.GENERIC_WIDGET.NO_WIDGET_CONTAINER'),
            );
            return;
        }

        // clear existing component
        this.clearExistingComponent();

        try {
            const componentClass = await this.getComponentClass();
            // inject the component into the widget container
            this.widgetComponentRef = this.uiService.injectAngularComponent(
                this.widgetContainer,
                componentClass,
                this.widgetContainerElement.nativeElement,
                {},
                { replace: false },
                this.injector,
            );
            this.widgetInstance = this.widgetComponentRef.instance as WidgetComponentInterface;

            // set input properties
            this.setWidgetInputs();

            // set up output event handlers
            this.setupWidgetOutputs();
        } catch (error) {
            console.error(
                this.translate.instant('TOPIC_PAGE.WIDGET.GENERIC_WIDGET.ERROR_LOADING_WIDGET'),
                error,
            );
            console.error(
                this.translate.instant('TOPIC_PAGE.WIDGET.GENERIC_WIDGET.WIDGET_TYPE'),
                this.widgetType,
            );
            console.error(
                this.translate.instant('TOPIC_PAGE.WIDGET.GENERIC_WIDGET.STACK_TRACE'),
                error.stack,
            );
        } finally {
            // mark as initialized
            this.initialized.set(true);
        }
        // potential pre-processing action
        if (this.widgetInstance.preLoadAction) {
            await this.widgetInstance.preLoadAction();
        }
        // read widget configuration and set widget values
        await this.readWidgetConfig();
        // potential post-processing action
        if (this.widgetInstance.postLoadAction) {
            await this.widgetInstance.postLoadAction();
        }
        // make sure to update the i18n labels and visibility of the configuration options
        this.updateCommonConfigurationOptions();
    }

    /**
     * Helper function to read the widget configuration from the node inputs.
     */
    private async readWidgetConfig(): Promise<void> {
        let widgetConfig: WidgetConfig = {};
        let aiConfig: BapiConfigObject = {};

        if (this.nodeId || this.propagatedNodeId) {
            this.widgetNode = await this.topicPageHelperService.getNode(
                this.nodeId || this.propagatedNodeId,
            );
            widgetConfig = retrieveWidgetConfigFromNode(this.widgetNode);
            aiConfig = retrieveAiConfigFromNode(this.widgetNode);
        }

        // check if configOverwrite should override the retrieved config
        if (this.configOverwrite) {
            try {
                const parsedConfigOverwrite = JSON.parse(
                    this.configOverwrite,
                ) as Partial<WidgetConfig>;

                // Check if at least one valid value exists in configOverwrite
                const hasValidValues = Object.values(parsedConfigOverwrite).some(
                    (value) => value !== undefined && value !== null,
                );

                if (hasValidValues) {
                    // Merge the configs, with configOverwrite taking precedence
                    widgetConfig = { ...widgetConfig, ...parsedConfigOverwrite };
                }
            } catch (error) {
                console.warn(
                    this.translate.instant(
                        'TOPIC_PAGE.WIDGET.GENERIC_WIDGET.PARSE_OVERWRITE_ERROR',
                    ),
                    error,
                );
            }
        }

        // if at least a widget config or an AI config is defined, set the values in the widget
        if (
            (widgetConfig && Object.keys(widgetConfig)?.length) ||
            (aiConfig && Object.keys(aiConfig)?.length)
        ) {
            if (this.widgetInstance.setWidgetValues) {
                const result: void | Promise<void> = this.widgetInstance.setWidgetValues(
                    widgetConfig,
                    aiConfig,
                );
                if (result instanceof Promise) {
                    await result;
                }
            }
            await this.updateCommonProperties(widgetConfig, aiConfig);
        }
        this.widgetInstance.initialized.set(true);
    }

    /**
     * Helper function to clear the existing widget component and reset references.
     */
    private clearExistingComponent(): void {
        this.widgetContainer.clear();
        if (this.widgetComponentRef) {
            this.widgetComponentRef.destroy();
            this.widgetComponentRef = null;
            this.widgetInstance = null;
        }
    }

    /**
     * Helper function to dynamically import and return the appropriate component class based on widget type.
     */
    private async getComponentClass(): Promise<any> {
        let componentClass: any;
        componentClass = this.genericWidgetGlobalService.getCustomWidget(this.widgetType);
        if (componentClass != null) {
            return componentClass;
        }
        switch (this.widgetType) {
            case WIDGETS.AI_TEXT_WIDGET:
                const aiTextWidgetModule = await import(
                    '../ai-text-widget/ai-text-widget.component'
                );
                componentClass = aiTextWidgetModule.AiTextWidgetComponent;
                break;
            case WIDGETS.COLLECTION_CHIPS:
                const collectionChipsModule = await import(
                    '../collection-chips/collection-chips.component'
                );
                componentClass = collectionChipsModule.CollectionChipsComponent;
                break;
            case WIDGETS.CONTENT_TEASER:
                const contentTeaserModule = await import(
                    '../content-teaser/content-teaser.component'
                );
                componentClass = contentTeaserModule.ContentTeaserComponent;
                break;
            case WIDGETS.IFRAME_WIDGET:
                const iframeWidgetModule = await import('../iframe-widget/iframe-widget.component');
                componentClass = iframeWidgetModule.IframeWidgetComponent;
                break;

            case WIDGETS.MEDIA_RENDERING:
                const mediaRenderingModule = await import(
                    '../media-rendering/media-rendering.component'
                );
                componentClass = mediaRenderingModule.MediaRenderingComponent;
                break;
            case WIDGETS.TEXT_WIDGET:
                const textWidgetModule = await import('../text-widget/text-widget.component');
                componentClass = textWidgetModule.TextWidgetComponent;
                break;
            case WIDGETS.TOPICS_COLUMN_BROWSER:
                const topicsColumnBrowserModule = await import(
                    '../topics-column-browser/topics-column-browser.component'
                );
                componentClass = topicsColumnBrowserModule.TopicsColumnBrowserComponent;
                break;
            /*
        case WIDGETS.EDITORIAL_MEMBERS:
            const editorialMembersModule = await import(
            '../editorial-members/editorial-members.component'
            );
            componentClass = editorialMembersModule.EditorialMembersComponent;
            break;
*/
            default:
                componentClass = null;
        }

        if (!componentClass) {
            console.error(this.widgetType + ' not found');
            throw new Error(
                this.translate.instant(
                    'TOPIC_PAGE.WIDGET.GENERIC_WIDGET.WIDGET_NOT_FOUND_IN_MODULE_OR_UNKNOWN',
                    { type: this.widgetType },
                ),
            );
        }

        return componentClass;
    }

    /**
     * Helper function to set the initial inputs of the widget component.
     */
    private setWidgetInputs(): void {
        if (!this.widgetInstance || !this.widgetComponentRef) {
            return;
        }

        // set common properties
        this.widgetComponentRef.setInput('contextNodeId', this.contextNodeId);
        this.widgetComponentRef.setInput('editMode', this.editMode());
        this.widgetComponentRef.setInput('embedConfigurationOption', this.embedConfigurationOption);
        this.widgetComponentRef.setInput('gridIndex', this.gridIndex);
        this.widgetComponentRef.setInput('pageVariantNode', this.pageVariantNode);
        this.widgetComponentRef.setInput('swimlaneIndex', this.swimlaneIndex);

        // set widget-specific properties
        switch (this.widgetType) {
            case WIDGETS.AI_TEXT_WIDGET:
                this.widgetComponentRef.setInput('nodeId', this.nodeId);
                this.widgetComponentRef.setInput('propagatedNodeId', this.propagatedNodeId);
                this.widgetComponentRef.setInput('searchInput', this.searchInput());
                this.widgetComponentRef.setInput('selectDimensions', this.selectDimensions);
                this.widgetComponentRef.setInput(
                    'selectedDimensionValues',
                    this.selectedDimensionValues,
                );
                break;

            case WIDGETS.COLLECTION_CHIPS:
                this.widgetComponentRef.setInput('customUrl', retrieveCustomUrl);
                break;

            case WIDGETS.CONTENT_TEASER:
                this.widgetComponentRef.setInput('defaultNodeId', this.defaultNodeId);
                this.widgetComponentRef.setInput('nodeId', this.nodeId);
                this.widgetComponentRef.setInput('propagatedNodeId', this.propagatedNodeId);
                this.widgetComponentRef.setInput('searchInput', this.searchInput());
                this.widgetComponentRef.setInput('searchText', this.searchText);
                this.widgetComponentRef.setInput('swimlaneColor', this.swimlaneColor);
                this.widgetComponentRef.setInput('swimlaneShape', this.swimlaneShape);
                break;

            case WIDGETS.EDITORIAL_MEMBERS:
                this.widgetComponentRef.setInput('searchInput', this.searchInput());
                break;

            case WIDGETS.MEDIA_RENDERING:
                this.widgetComponentRef.setInput('headline', this.headline);
                this.widgetComponentRef.setInput('nodeId', this.nodeId);
                this.widgetComponentRef.setInput('searchInput', this.searchInput());
                break;

            case WIDGETS.TOPICS_COLUMN_BROWSER:
                // @TODO
                //this.widgetComponentRef.setInput('customUrl', retrieveCustomUrl);
                this.widgetComponentRef.setInput('height', this.height);
                this.widgetComponentRef.setInput('sidebarEmbedding', this.sidebarEmbedding);
                break;

            // default break for unknown widget types and widget types without additional inputs
            // e.g., iframe-widget and text-widget
            default:
                break;
        }
    }

    /**
     * Helper function to set up output event handlers for the widget component.
     */
    private setupWidgetOutputs(): void {
        if (!this.widgetComponentRef) return;

        const instance = this.widgetComponentRef.instance;

        // set up common outputs
        instance.configChanged?.subscribe(() => this.persistConfig());
        instance.embedWidgetClicked?.subscribe(() => this.openEmbedWidgetDialog());

        // set up widget-specific outputs
        instance.itemClickedEvent?.subscribe((node: Node) => this.itemClickedEvent.emit(node));
        instance.nodeStatisticsChanged?.subscribe((stats: StatisticNode[]) =>
            this.nodeStatisticsChanged.emit(stats),
        );
        instance.internalSearchResultCountChanged?.subscribe((count: number) => {
            this.updateSearchResults(count, 'internal');
        });
        instance.totalSearchResultCountChanged?.subscribe((count: number) => {
            this.totalSearchResultCountChanged.emit(count);
            this.updateSearchResults(count, 'nodes');
        });
    }

    /**
     * Helper function to process changes made to the widget component.
     *
     * @param changes
     */
    private async processChanges(changes: SimpleChanges): Promise<void> {
        if (this.widgetInstance && this.widgetComponentRef) {
            // update inputs
            Object.keys(changes).forEach((key) => {
                if (key in this.widgetComponentRef!.instance) {
                    this.widgetComponentRef.setInput(key, changes[key].currentValue);
                }
            });
        }
    }

    protected readonly WIDGETS = WIDGETS;
}
