import { CommonModule } from '@angular/common';
import {
    Component,
    CUSTOM_ELEMENTS_SCHEMA,
    effect,
    ElementRef,
    EventEmitter,
    input,
    Input,
    InputSignal,
    Output,
    Signal,
    signal,
    ViewChild,
    ViewEncapsulation,
    WritableSignal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslateModule } from '@ngx-translate/core';
import { MdsAiConfig, MdsDefinition, MdsService, MdsWidget, Node } from 'ngx-edu-sharing-api';
import { NodeConfig } from 'ngx-edu-sharing-b-api';
import { MdsWidgetType, SpinnerComponent } from 'ngx-edu-sharing-ui';
import { firstValueFrom } from 'rxjs';
import { MdsModule } from '../../../../features/mds/mds.module';
import { Toast, ToastType } from '../../../../services/toast';
import { AiHelperService } from '../../shared/services/ai-helper.service';
import { GlobalWidgetConfigService } from '../../shared/services/global-widget-config.service';
import { TopicPageHelperService } from '../../shared/services/topic-page-helper.service';
import { BapiConfig } from '../../shared/types/bapi-config';
import { BapiConfigObject } from '../../shared/types/bapi-config-object';
import { ConfigurationOption } from '../../shared/types/configuration-option';
import { TextVariant } from '../../shared/types/text-variant';
import { AiTextWidgetConfig } from '../../shared/types/widget-config/ai-text-widget-config';
import { retrieveResultString } from '../../shared/utils/ai-util';
import { getNodeOrDefaultNodeId } from '../../shared/utils/node-util';
import {
    convertNodeRefIntoNodeId,
    retrievePromptFromAiConfig,
} from '../../shared/utils/template-util';
import { WidgetComponentInterface } from '../generic-widget/generic-widget.component';
import { GenericWidgetGlobalService } from '../generic-widget/generic-widget-global.service';
import { EditableTextComponent } from '../shared/editable-text/editable-text.component';
import { WidgetConfigurationButtonsComponent } from '../shared/widget-configuration-buttons/widget-configuration-buttons.component';

@Component({
    selector: 'es-ai-text-widget',
    encapsulation: ViewEncapsulation.Emulated,
    imports: [
        CommonModule,
        EditableTextComponent,
        FormsModule,
        MatButtonModule,
        MatDialogModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MdsModule,
        ReactiveFormsModule,
        SpinnerComponent,
        TranslateModule,
        WidgetConfigurationButtonsComponent,
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    templateUrl: './ai-text-widget.component.html',
    styleUrls: ['./ai-text-widget.component.scss'],
})
export class AiTextWidgetComponent implements WidgetComponentInterface {
    // CONSTANTS
    readonly i18nPrefix: string = 'TOPIC_PAGE.WIDGET.AI_WIDGET.';

    // INPUTS + OUTPUTS
    @Input() contextNodeId: string;
    @Input() defaultNodeId: string = '';
    editMode: InputSignal<boolean> = input<boolean>(false);
    @Input() embedConfigurationOption?: ConfigurationOption;
    @Input() gridIndex: number;
    @Input() nodeId?: string;
    @Input() pageVariantNode?: Node;
    @Input() propagatedNodeId?: string;
    searchInput: InputSignal<string> = input<string>(null);
    @Input() selectDimensions: Map<string, MdsWidget> = new Map<string, MdsWidget>();
    @Input() swimlaneIndex: number = -1;

    @Output() configChanged: EventEmitter<void> = new EventEmitter<void>();
    @Output() embedWidgetClicked: EventEmitter<void> = new EventEmitter<void>();
    @Output() internalSearchResultCountChanged: EventEmitter<number> = new EventEmitter<number>();

    @ViewChild('promptTextarea') promptTextarea: ElementRef<HTMLTextAreaElement>;

    // VARIABLES
    aiGeneratedText: WritableSignal<boolean> = signal(false);
    disabled: WritableSignal<boolean> = signal(false);
    form: FormGroup;
    initialized: WritableSignal<boolean> = signal(false);
    latestSelectedDimensionValues: { [p: string]: string[] } = {};
    latestStoredPrompt: string = '';
    latestStoredTexts: TextVariant[] = [];
    mds: string;
    processPromptInProgress: boolean = false;
    promptInput: string = '';
    reloadingIndicator: WritableSignal<boolean> = signal(false);
    resultInput: string = '';
    resultString: string = '';
    selectedVariables: Signal<{ [property: string]: string[] }>;
    updateInProgress: WritableSignal<boolean> = signal(false);

    constructor(
        private aiHelperService: AiHelperService,
        private globalWidgetConfigService: GlobalWidgetConfigService,
        private genericWidgetGlobalService: GenericWidgetGlobalService,
        private mdsService: MdsService,
        private toast: Toast,
        private topicPageHelperService: TopicPageHelperService,
    ) {
        this.selectedVariables = toSignal(this.topicPageHelperService.getSelectedVariables$(), {
            initialValue: {},
        });
        this.mds = this.genericWidgetGlobalService.getDefaultMds();
        effect(() => {
            // register signals as dependencies
            const currentEditMode = this.editMode();
            this.selectedVariables();

            if (this.initialized()) {
                // react to edit mode changes
                this.reloadingIndicator.set(true);
                this.editModeDisplayAction(currentEditMode)
                    .then(() => {
                        this.reloadingIndicator.set(false);
                    })
                    .catch((error) => {
                        console.error('KI Widget: An error occurred', error);
                    });
            }
        });
    }

    /**
     * Returns all available select dimensions that were input.
     */
    get availableSelectDimensionKeys(): string[] {
        return Array.from(this.selectDimensions.keys());
    }

    /**
     * Returns the select dimension keys used within the latest stored prompt.
     */
    get selectDimensionKeysUsed(): string[] {
        return this.availableSelectDimensionKeys.filter((key: string) =>
            this.latestStoredPrompt.includes(key),
        );
    }

    /**
     * Executes a specific display action based on a given editMode.
     *
     * @param editMode
     */
    private async editModeDisplayAction(editMode: boolean): Promise<void> {
        if (!editMode) {
            // retrieve the latest stored text or an AI generated one to be displayed
            await this.retrieveTextOrRequestAiGeneration();
        }
    }

    /**
     * Retrieves the text from the stored texts or executes a prompt to generate the text using AI.
     */
    private async retrieveTextOrRequestAiGeneration(): Promise<void> {
        const existingTextIndex: number = this.retrieveExistingValueForSelection(
            this.latestStoredTexts,
            this.selectedVariables(),
            true,
        );
        // if an exact full match exists, use it
        if (existingTextIndex !== -1) {
            this.resultString = this.latestStoredTexts?.[existingTextIndex]?.textValue?.text;
            this.aiGeneratedText.set(false);
        }
        // no exact full match exists, request AI generation
        if (existingTextIndex === -1 || !this.resultString) {
            await this.executePrompt();
            this.aiGeneratedText.set(true);
        }
    }

    /**
     * Handles the embedding of the widget by emitting an embed widget clicked event.
     */
    embedWidget(): void {
        this.embedWidgetClicked.emit();
    }

    /**
     * Reacts to es-editable-text (searchResultsUpdated) event and emit it.
     *
     * @param count
     */
    updateSearchResults(count: number): void {
        this.internalSearchResultCountChanged.emit(count);
    }

    /**
     * Updates the prompt in the config.
     */
    async processPromptUpdate(): Promise<void> {
        this.processPromptInProgress = true;
        // reset the latest stored texts and result text due to re-generation
        this.latestStoredTexts = [];
        this.latestSelectedDimensionValues = {};
        this.latestStoredPrompt = '';
        this.resultInput = '';
        this.latestStoredPrompt = this.promptInput;
        this.configChanged.emit();
        // call editModeDisplayAction to extract the updated dimensions from the prompt
        setTimeout(async () => {
            await this.editModeDisplayAction(this.editMode());
            this.processPromptInProgress = false;
        }, 5000);
    }

    /**
     * Generate text with the current selected dimensions.
     */
    async generateTextForSelection(): Promise<void> {
        this.disabled.set(true);
        // open a snack bar for the user to inform about a generation process
        this.toast.show({
            message: this.i18nPrefix + 'GENERATE_TEXT_HINT',
            type: 'info',
            subtype: ToastType.InfoSimple,
        });
        // create a request for the currently selected dimension values
        const variables: { [key: string]: string[] } = {};
        // filter out empty values
        Object.keys(this.latestSelectedDimensionValues).forEach((key: string) => {
            if (this.latestSelectedDimensionValues[key].length > 0) {
                variables[key] = this.latestSelectedDimensionValues[key];
            }
        });
        // execute the prompt
        await this.executePrompt(variables);
    }

    /**
     * Executes the stored prompt using the B-API.
     *
     * @param givenVariables
     */
    private async executePrompt(givenVariables: { [key: string]: string[] } = {}): Promise<void> {
        const defaultConfigId: string = getNodeOrDefaultNodeId(
            this.defaultNodeId,
            this.globalWidgetConfigService.defaultAiTextWidgetConfigId,
        );

        // either use a given request body or create a new one from the globally selected dimension values
        let variables: { [key: string]: string[] } = this.selectedVariables();
        if (Object.keys(givenVariables).length) {
            variables = givenVariables;
        }
        // generate a text using both configId and variables
        const config: NodeConfig = {
            type: 'node',
            nodeId: convertNodeRefIntoNodeId(this.nodeId || this.propagatedNodeId),
            configName: 'prompt',
        };
        const promptResponse = await this.aiHelperService.generateFromPrompt(
            this.nodeId || this.propagatedNodeId ? config : defaultConfigId,
            variables,
            this.contextNodeId,
        );
        this.resultString = retrieveResultString(promptResponse);
        this.disabled.set(false);
        // in edit mode, patch the existing form value
        if (this.editMode()) {
            if (Object.keys(givenVariables).length) {
                this.resultInput = this.resultString;
            }
        }
    }

    /**
     * Saves the modification made for the current selection.
     */
    async saveModification(): Promise<void> {
        // create an output object
        const outputObject: TextVariant = {};
        this.selectDimensions.forEach((val, key) => {
            if (this.latestSelectedDimensionValues[key].length) {
                outputObject[key] = this.latestSelectedDimensionValues[key];
            }
        });
        const currentUser: string = await this.aiHelperService.getCurrentUser();
        outputObject['textValue'] = {
            text: this.resultInput,
            updatedAt: Date.now(),
            updatedBy: currentUser,
        };

        // retrieve a potentially existing value
        const existingValueIndex: number = this.retrieveExistingValueForSelection(
            this.latestStoredTexts,
            this.selectedVariables(),
            true,
        );

        // replace the existing value or push a new value
        let latestStoredTexts: TextVariant[] = this.latestStoredTexts ?? [];
        if (existingValueIndex !== -1) {
            latestStoredTexts[existingValueIndex] = outputObject;
        } else {
            latestStoredTexts.push(outputObject);
        }

        // retrieve the updated values to ensure that the modifications were successfully
        this.latestStoredTexts = latestStoredTexts;
        // store latestStoredPrompt, as saveModification is not used for changing the prompt
        this.configChanged.emit();
    }

    /**
     * Appends a placeholder key to the current prompt input, while keeping the focus on the textarea.
     */
    appendDimensionKey(placeholderKey: string): void {
        // const currentValue = this.form.get('prompt').value;
        const currentValue: string = this.promptInput;
        // check whether the string ends with whitespace: https://stackoverflow.com/a/30566492
        const endSpace: RegExp = /\s$/;
        // add prefix and suffix
        const dimensionKey: string = this.dimensionPrefix + placeholderKey + this.dimensionSuffix;
        // only add leading whitespace, if it does not already exist
        let valueToAdd: string = endSpace.test(currentValue) ? dimensionKey : ' ' + dimensionKey;
        this.promptInput = currentValue + valueToAdd;
        // focus textarea after appending the string
        this.promptTextarea?.nativeElement?.focus();
    }

    /**
     * Checks for changes made in the prompt textarea to open a snack bar, if necessary.
     */
    checkForPromptChanges(): void {
        // if (this.form.get('prompt').value !== this.latestStoredPrompt) {
        if (this.promptInput !== this.latestStoredPrompt) {
            // open a snack bar for the user to inform about unsaved modifications
            this.toast.show({
                message: this.i18nPrefix + 'PROMPT_ADJUSTED_HINT',
                type: 'info',
                subtype: ToastType.InfoSimple,
            });
        }
    }

    // noinspection JSUnusedGlobalSymbols
    /**
     * Preload action to define the initial form and load the initial prompt.
     */
    async preLoadAction(): Promise<void> {
        const aiConfigId: string = getNodeOrDefaultNodeId(
            this.defaultNodeId,
            this.globalWidgetConfigService.defaultAiTextWidgetConfigId,
        );
        const mds: MdsDefinition = await firstValueFrom(
            this.mdsService.getMetadataSet({
                metadataSet: this.genericWidgetGlobalService.getDefaultMds(),
            }),
        );
        const mdsAIConfig: MdsAiConfig = mds.aiConfigs.find(
            (config: MdsAiConfig) => config.id === aiConfigId,
        );
        // parse the JSON string of chatCompletion
        mdsAIConfig.chatCompletion =
            typeof mdsAIConfig.chatCompletion === 'string'
                ? JSON.parse(mdsAIConfig.chatCompletion)
                : mdsAIConfig.chatCompletion;
        if (mdsAIConfig) {
            const aiConfig: BapiConfigObject = {
                prompt: mdsAIConfig as BapiConfig,
            };
            this.latestStoredPrompt = retrievePromptFromAiConfig(aiConfig, 'prompt');
            this.promptInput = this.latestStoredPrompt;
        }
    }

    // noinspection JSUnusedGlobalSymbols
    /**
     * Called by generic-widget component to set widget-specific values.
     *
     * @param config
     * @param aiConfig
     */
    async setWidgetValues(config: AiTextWidgetConfig, aiConfig: BapiConfigObject): Promise<void> {
        // read the latest stored prompt from the AI config
        const prompt: string =
            aiConfig && Object.keys(aiConfig)?.length
                ? retrievePromptFromAiConfig(aiConfig, 'prompt')
                : config?.prompt ?? '';
        if (prompt) {
            this.latestStoredPrompt = prompt;
            this.promptInput = this.latestStoredPrompt;
        }
        this.latestStoredTexts = config?.texts ?? [];
    }

    // noinspection JSUnusedGlobalSymbols
    /**
     * Called by generic-widget component to retrieve a widget config from the currently set variables in the component.
     */
    retrieveWidgetConfig(): AiTextWidgetConfig {
        return {
            prompt: this.latestStoredPrompt,
            texts: this.latestStoredTexts,
        };
    }

    // noinspection JSUnusedGlobalSymbols
    /**
     * Called by generic-widget component to retrieve a custom AI key value pair from the currently set variables in the component.
     */
    retrieveCustomAiKeyValuePairs(): { [key: string]: string } {
        // return { prompt: this.form.get('prompt').value };
        return { prompt: this.promptInput };
    }

    // noinspection JSUnusedGlobalSymbols
    /**
     * Postload action to execute edit mode actions.
     */
    async postLoadAction(): Promise<void> {
        await this.editModeDisplayAction(this.editMode());
        this.initialized.set(true);
    }

    /**
     * At least one of the dimensions was updated, so check for a matching text variant.
     */
    updateResultInput() {
        const currentSelection: { [p: string]: string[] } = {};
        this.selectDimensions.forEach((val, key) => {
            if (this.latestSelectedDimensionValues[key]?.length) {
                currentSelection[key] = this.latestSelectedDimensionValues[key];
            }
        });
        // check for an exact match or reset the result input
        const exactMatchIndex: number = this.retrieveExistingValueForSelection(
            this.latestStoredTexts,
            currentSelection,
            true,
        );
        if (exactMatchIndex !== -1) {
            this.resultInput = this.latestStoredTexts[exactMatchIndex].textValue.text;
        } else {
            this.resultInput = '';
        }
    }

    /**
     * Helper function to retrieve a potentially existing value for the current selection.
     */
    private retrieveExistingValueForSelection(
        texts: TextVariant[],
        userSelection: { [key: string]: string[] },
        requireExactFullMatch: boolean = false,
    ): number {
        // early return, if no user selection is available
        if (!Object.keys(userSelection).length) {
            return -1;
        }
        const numberOfUserSelectionVariables = Object.values(userSelection).reduce(
            (sum, values) => sum + values.length,
            0,
        );
        let highestNumberOfMatches = 0;
        let bestMatchIndex = -1;
        // prefer fewer variables
        let bestMatchNumberOfVariables = Number.MAX_VALUE;
        for (let index = 0; index < texts.length; index++) {
            const textEntry = texts[index];
            // count the number of variables in textEntry (excluding textValue property)
            const textEntryNumberOfVariables = Object.entries(textEntry)
                .filter(([key]) => key !== 'textValue')
                .reduce((sum, [, values]) => sum + (Array.isArray(values) ? values.length : 0), 0);
            let totalMatches = 0;
            // count matches per dimension
            for (const [dimensionKey, selectedValues] of Object.entries(userSelection)) {
                const storedValues = textEntry[dimensionKey] || [];
                selectedValues.forEach((val) => {
                    if (storedValues.includes(val)) {
                        totalMatches++;
                    }
                });
            }
            if (totalMatches > 0 && totalMatches >= highestNumberOfMatches) {
                if (totalMatches > highestNumberOfMatches) {
                    highestNumberOfMatches = totalMatches;
                    bestMatchIndex = index;
                    bestMatchNumberOfVariables = textEntryNumberOfVariables;
                } else {
                    // prefer textEntry with fewer variables (fewer extra/irrelevant variables)
                    if (textEntryNumberOfVariables < bestMatchNumberOfVariables) {
                        highestNumberOfMatches = totalMatches;
                        bestMatchIndex = index;
                        bestMatchNumberOfVariables = textEntryNumberOfVariables;
                    }
                }
                // an exact match is found (all variables are matched and the number of variables is equal)
                if (
                    totalMatches === numberOfUserSelectionVariables &&
                    textEntryNumberOfVariables === numberOfUserSelectionVariables
                ) {
                    return index;
                }
            }
        }

        // no exact match was returned before
        if (requireExactFullMatch) {
            return -1;
        }
        return bestMatchIndex;
    }

    protected readonly dimensionPrefix: string = '{{var(';
    protected readonly dimensionSuffix: string = ')|-}}';
    protected readonly MdsWidgetType = MdsWidgetType;
}
