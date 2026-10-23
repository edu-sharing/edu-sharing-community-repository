import {
    Component,
    CUSTOM_ELEMENTS_SCHEMA,
    input,
    Input,
    InputSignal,
    OnChanges,
    OnInit,
    signal,
    SimpleChanges,
    ViewEncapsulation,
    WritableSignal,
    inject,
} from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { Node, ParentEntries } from 'ngx-edu-sharing-api';
import { CreateChatCompletionResponse } from 'ngx-edu-sharing-b-api';
import {
    ColorHelper,
    EduSharingUiCommonModule,
    PreferredColor,
    SpinnerComponent,
    UIConstants,
} from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import { ThemeService } from '../../../../services/theme.service';
import { SharedModule } from '../../../../shared/shared.module';
import { TooltipAriaLabelDirective } from '../../shared/directives/tooltip-aria-label.directive';
import { HighlightSearchPipe } from '../../shared/pipes/highlight-search.pipe';
import { AiHelperService } from '../../shared/services/ai-helper.service';
import { GlobalWidgetConfigService } from '../../shared/services/global-widget-config.service';
import { TopicPageGlobalService } from '../../shared/services/topic-page-global.service';
import { TopicPageHelperService } from '../../shared/services/topic-page-helper.service';
import { DEFAULT_WIDGET_CONFIG_PROP } from '../../shared/types/custom-definitions';
import { TopicHeaderConfig } from '../../shared/types/widget-config/topic-header-config';
import { retrieveResultString } from '../../shared/utils/ai-util';
import { getNodeOrDefaultNodeId } from '../../shared/utils/node-util';
import { retrieveNodeId } from '../../shared/utils/template-util';
import { ColorPickerComponent } from '../shared/color-picker/color-picker.component';
import { EditableTextComponent } from '../shared/editable-text/editable-text.component';
import { ImageWrapperComponent } from '../shared/image-wrapper/image-wrapper.component';

@Component({
    selector: 'es-topic-header',
    encapsulation: ViewEncapsulation.Emulated,
    imports: [
        EduSharingUiCommonModule,
        ColorPickerComponent,
        EditableTextComponent,
        HighlightSearchPipe,
        ImageWrapperComponent,
        SharedModule,
        SpinnerComponent,
        TooltipAriaLabelDirective,
        TranslateModule,
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    templateUrl: './topic-header.component.html',
    styleUrls: ['./topic-header.component.scss'],
})
export class TopicHeaderComponent implements OnChanges, OnInit {
    private aiHelperService = inject(AiHelperService);
    private globalWidgetConfigService = inject(GlobalWidgetConfigService);
    private topicPageGlobalService = inject(TopicPageGlobalService);
    private topicPageHelperService = inject(TopicPageHelperService);
    private themeService = inject(ThemeService);

    // CONSTANTS
    private readonly DEFAULT_DESCRIPTION: string = '';
    readonly DEFAULT_HEADER_TEXT_BG_COLOR = '#FFFFFF';
    readonly i18nPrefix: string = 'TOPIC_PAGE.WIDGET.TOPIC_HEADER.';

    private _collectionNode: Node = null;
    @Input() set collectionNode(value: Node) {
        this._collectionNode = value;
        this.collectionDescription = value?.properties?.[RestConstants.CM_DESCRIPTION]?.[0];
        if (!this.collectionDescription) {
            void this.generateCollectionDescription(retrieveNodeId(value));
        }
    }
    get collectionNode(): Node {
        return this._collectionNode;
    }
    @Input() contextNodeId: string;
    @Input() defaultDescriptionNodeId?: string = '';
    @Input() defaultImageNodeId?: string = '';
    @Input() defaultTextNodeId?: string = '';
    @Input() editMode: boolean = false;
    @Input() nodeId: string = '';
    // propagated header node: only its text background color is taken over,
    // other parameters fall back to defaults
    @Input() propagatedNodeId: string = '';
    @Input() pageVariantNode: Node;
    parentEntries: InputSignal<ParentEntries> = input<ParentEntries>(null);
    searchInput: InputSignal<string> = input<string>(null);
    @Input() topic: string;

    // own config node, empty if none exists or it was deleted, so saving creates a new one
    availableNodeId: string = '';
    aiGeneratedDescription: WritableSignal<boolean> = signal(false);
    aiGeneratedImage: boolean;
    aiGeneratedText: WritableSignal<boolean> = signal(false);
    aiSupported: WritableSignal<boolean> = signal(false);
    backToCollectionButtonVisible: WritableSignal<boolean> = signal(false);
    collectionDescription: string;
    // description is used for the "description" of importance (stored in a "header" widget node)
    description: string = this.DEFAULT_DESCRIPTION;
    initialized: WritableSignal<boolean> = signal(false);
    // the page is creating the config node this header asked for
    private nodeCreationPending: boolean = false;
    // saves run one after another, each storing the state current at its turn
    private saveQueue: Promise<void> = Promise.resolve();
    // the config last stored (or loaded), so an unchanged state is not stored again
    private storedConfig: string = null;
    textBackgroundColor: string = this.DEFAULT_HEADER_TEXT_BG_COLOR;
    userUploadedNodeId: string = null;
    widgetNodeId: string = null;

    constructor() {
        this.backToCollectionButtonVisible.set(
            this.topicPageGlobalService.getBackToCollectionButtonVisible(),
        );
    }

    /**
     * Initializes the component.
     */
    async ngOnInit(): Promise<void> {
        // retrieve the widget node ID for a potential AI request
        this.widgetNodeId = getNodeOrDefaultNodeId(
            this.defaultImageNodeId,
            this.globalWidgetConfigService.defaultTopicHeaderImageWidgetNodeId,
        );
        // by default, the image should be generated by AI if it is supported
        this.aiSupported.set(await this.aiHelperService.hasAISupport());
        this.aiGeneratedImage = this.aiSupported();
    }

    /**
     * Initializes or updates the topic header component.
     */
    async ngOnChanges(changes: SimpleChanges): Promise<void> {
        const topicChanged: boolean = changes.topic && !changes.topic?.firstChange;
        const nodeIdChanged: boolean =
            !!changes.nodeId && changes.nodeId.currentValue !== changes.nodeId.previousValue;
        const propagatedNodeIdChanged: boolean =
            !!changes.propagatedNodeId &&
            changes.propagatedNodeId.currentValue !== changes.propagatedNodeId.previousValue;
        // a node created on request of this header keeps the local state, which may be newer
        const ownNodeCreated: boolean =
            nodeIdChanged &&
            !changes.nodeId.firstChange &&
            !!this.nodeId &&
            !topicChanged &&
            (this.nodeCreationPending || this.nodeId === this.availableNodeId);
        if (ownNodeCreated) {
            this.availableNodeId = this.nodeId;
        } else if (
            changes.nodeId?.firstChange ||
            changes.propagatedNodeId?.firstChange ||
            nodeIdChanged ||
            propagatedNodeIdChanged ||
            topicChanged
        ) {
            // reset several variables if an actual (non-first) change was detected
            if (
                (!changes.nodeId?.firstChange && nodeIdChanged) ||
                (!changes.propagatedNodeId?.firstChange && propagatedNodeIdChanged) ||
                topicChanged
            ) {
                this.aiGeneratedImage = this.aiSupported();
                this.aiGeneratedText.set(false);
                this.initialized.set(false);
                this.description = this.DEFAULT_DESCRIPTION;
                this.textBackgroundColor = this.DEFAULT_HEADER_TEXT_BG_COLOR;
                this.userUploadedNodeId = null;
            }
            // initialize the topic header component
            await this.initTopicHeader();
        }
        if (!changes.editMode?.firstChange && changes.editMode?.currentValue === false) {
            // regenerate texts of the topic header component
            await this.regenerateTexts();
        }
    }

    /**
     * Initializes the topic header by generating an introductory text or retrieving it from the config.
     */
    async initTopicHeader(): Promise<void> {
        // retrieve a config node if one exists
        let configJson: string = '{}';
        // when only a propagated node exists, take over only its color attribute
        let colorOnly: boolean = false;
        const configNodeId: string = this.nodeId || this.propagatedNodeId;
        let node: Node | null = null;
        if (configNodeId) {
            node = await this.topicPageHelperService.getNodeIfAvailable(configNodeId);
            if (node?.properties?.[DEFAULT_WIDGET_CONFIG_PROP]?.[0]) {
                configJson = node.properties[DEFAULT_WIDGET_CONFIG_PROP][0];
            }
            colorOnly = !this.nodeId && !!this.propagatedNodeId;
        }
        this.availableNodeId = this.nodeId && node ? this.nodeId : '';
        // read config from this JSON string and generates an AI text if no description is found
        const aiNodeId: string = getNodeOrDefaultNodeId(
            this.defaultTextNodeId,
            this.globalWidgetConfigService.defaultTopicHeaderTextWidgetNodeId,
        );
        await this.readConfigFromJson(configJson, aiNodeId, colorOnly);
        this.storedConfig = JSON.stringify(this.retrieveWidgetConfig());
        // set the component to be initialized (further processing is done in the child component)
        this.initialized.set(true);
    }

    /**
     * Regenerates collectionDescription and description via AI if they were cleared.
     */
    async regenerateTexts(): Promise<void> {
        if (!this.collectionDescription) {
            await this.generateCollectionDescription(retrieveNodeId(this._collectionNode));
        }
        if (!this.description) {
            const aiNodeId: string = getNodeOrDefaultNodeId(
                this.defaultTextNodeId,
                this.globalWidgetConfigService.defaultTopicHeaderTextWidgetNodeId,
            );
            await this.generateDescription(aiNodeId);
        }
    }

    /**
     * Changes the text background color by emitting a persist config event.
     */
    async changeTextBackgroundColor(color: string): Promise<void> {
        if (color !== this.textBackgroundColor) {
            this.textBackgroundColor = color;
            await this.persistConfig();
        }
    }

    /**
     * Handles a manual change of the importance description text. The text is no longer
     * AI-generated once edited by the user, so the AI label must be hidden before persisting.
     */
    async onDescriptionChanged(): Promise<void> {
        this.aiGeneratedText.set(false);
        await this.persistConfig();
    }

    /**
     * Handles a manual change of the collection description text. The text is no longer
     * AI-generated once edited by the user, so the AI label must be hidden before persisting.
     */
    async onCollectionDescriptionChanged(description: string): Promise<void> {
        this.aiGeneratedDescription.set(false);
        await this.persistCollectionDescription(description);
    }

    /**
     * Persists the currently defined config once the saves requested before are done.
     *
     * @param force stores the config even if unchanged, e.g. to get a node for an image upload
     */
    persistConfig(force: boolean = false): Promise<void> {
        const save: Promise<void> = this.saveQueue.then(() => this.storeConfig(force));
        this.saveQueue = save.catch((): void => undefined);
        return save;
    }

    /**
     * Stores the current config, either in the own node or in a node the page creates for it.
     */
    private async storeConfig(force: boolean): Promise<void> {
        // a config being loaded is no state of this header yet
        if (!this.initialized()) {
            return;
        }
        const widgetConfig: TopicHeaderConfig = this.retrieveWidgetConfig();
        const serializedConfig: string = JSON.stringify(widgetConfig);
        if (!force && serializedConfig === this.storedConfig) {
            return;
        }
        if (this.availableNodeId) {
            const node: Node = await this.topicPageHelperService.persistConfig(
                this.availableNodeId,
                -1,
                -1,
                this.pageVariantNode,
                widgetConfig,
                null,
                this.contextNodeId,
                false,
                true,
            );
            this.storedConfig = node ? serializedConfig : null;
            return;
        }
        // creating the node may involve copying an inherited page config first
        this.nodeCreationPending = true;
        const nodeId: string | null = await new Promise<string | null>((resolve): void => {
            void this.topicPageHelperService.persistConfig(
                '',
                -1,
                -1,
                this.pageVariantNode,
                widgetConfig,
                null,
                this.contextNodeId,
                false,
                true,
                resolve,
            );
        });
        this.nodeCreationPending = false;
        if (nodeId) {
            this.availableNodeId = nodeId;
            this.storedConfig = serializedConfig;
        }
    }

    /**
     * Persists the description of the collection.
     */
    async persistCollectionDescription(description: string): Promise<void> {
        if (!this.collectionNode?.ref.id) {
            return;
        }
        try {
            this.topicPageHelperService.openSaveConfigToast();
            await this.topicPageHelperService.setProperty(
                this.collectionNode.ref.id,
                RestConstants.CM_DESCRIPTION,
                description,
            );
        } catch (err) {
            this.topicPageHelperService.displayErrorToast(err);
        }
    }

    /**
     * Helper function to generate the description of a collection.
     */
    private async generateCollectionDescription(contextNodeId: string): Promise<void> {
        if (!(await this.aiHelperService.hasAISupport())) {
            return;
        }
        const mdsConfigId: string = getNodeOrDefaultNodeId(
            this.defaultDescriptionNodeId,
            this.globalWidgetConfigService.defaultTopicHeaderDescriptionWidgetNodeId,
        );
        try {
            const promptResponse: CreateChatCompletionResponse =
                await this.aiHelperService.generateFromPrompt(
                    mdsConfigId,
                    {},
                    this.contextNodeId || contextNodeId,
                );
            const responseText: string = retrieveResultString(promptResponse) ?? '';
            if (responseText !== '') {
                this.collectionDescription = responseText;
                this.aiGeneratedDescription.set(true);
            }
        } catch {
            // AI generation failed; fall back to no collection description
        }
    }

    /**
     * Helper function to read the config from a given string.
     */
    private async readConfigFromJson(
        configString: string,
        nodeId: string,
        colorOnly: boolean = false,
    ): Promise<void> {
        const config: TopicHeaderConfig = JSON.parse(configString);
        // check whether a text background color is explicitly set (taken over in all modes)
        if (
            config.textBackgroundColor &&
            config.textBackgroundColor !== this.DEFAULT_HEADER_TEXT_BG_COLOR
        ) {
            this.textBackgroundColor = config.textBackgroundColor;
        }
        // colorOnly (propagated header): only the color is taken over; image and upload fall back
        // to their defaults and the description is (re)generated by AI as the default
        if (colorOnly) {
            await this.generateDescription(nodeId);
            return;
        }
        // this is the case if a widgetNode for the header exists
        if (config.description) {
            this.description = config.description;
            this.aiGeneratedText.set(false);
        } else {
            await this.generateDescription(nodeId);
        }
        // check whether an AI-generated image is explicitly set to false
        if (config.aiGeneratedImage === false) {
            this.aiGeneratedImage = false;
        }
        // check if an image was uploaded
        if (config.userUploadedNodeId) {
            this.userUploadedNodeId = config.userUploadedNodeId;
        }
    }

    /**
     * Helper function to generate the description text via AI.
     */
    private async generateDescription(nodeId: string): Promise<void> {
        if (!(await this.aiHelperService.hasAISupport())) {
            return;
        }
        try {
            const promptResponse: CreateChatCompletionResponse =
                await this.aiHelperService.generateFromPrompt(nodeId, {}, this.contextNodeId);
            const responseText: string = retrieveResultString(promptResponse) ?? '';
            if (responseText !== '') {
                this.description = responseText;
                this.aiGeneratedText.set(true);
            }
        } catch {
            // AI generation failed; fall back to no description text
        }
    }

    /**
     * Helper function to retrieve a widget config from the currently set variables in the component.
     */
    private retrieveWidgetConfig(): TopicHeaderConfig {
        const headerConfig: TopicHeaderConfig = {
            aiGeneratedImage: this.aiGeneratedImage,
            description: this.description,
            userUploadedNodeId: this.userUploadedNodeId,
        };
        if (this.textBackgroundColor) {
            headerConfig.textBackgroundColor = this.textBackgroundColor;
        }
        return headerConfig;
    }

    /**
     * Checks whether a given color is dark.
     */
    isDarkColor(color: string): boolean {
        if (!color) {
            return false;
        }
        return ColorHelper.getPreferredColor(color) === PreferredColor.Black;
    }

    /**
     * Resolves the header text background color to paint. The default color leaves the themed
     * surface shared with the image; a custom color is adapted to a dark surface in dark mode.
     */
    headerBackgroundColor(color: string): string | null {
        if (!color || color.toUpperCase() === this.DEFAULT_HEADER_TEXT_BG_COLOR) {
            return null;
        }
        return this.themeService.isDarkMode() ? this.themeService.toDarkSurfaceColor(color) : color;
    }

    protected readonly ROUTER_PREFIX: string = UIConstants.ROUTER_PREFIX;
}
