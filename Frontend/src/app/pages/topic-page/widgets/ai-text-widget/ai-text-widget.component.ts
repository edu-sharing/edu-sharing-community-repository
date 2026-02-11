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
    signal,
    ViewChild,
    ViewEncapsulation,
    WritableSignal,
} from '@angular/core';
import {
    FormArray,
    FormControl,
    FormGroup,
    FormsModule,
    ReactiveFormsModule,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslateModule } from '@ngx-translate/core';
import {
    MdsAiConfig,
    MdsDefinition,
    MdsService,
    MdsValue,
    MdsWidget,
    Node,
} from 'ngx-edu-sharing-api';
import { NodeConfig } from 'ngx-edu-sharing-b-api';
import { SpinnerComponent } from 'ngx-edu-sharing-ui';
import { firstValueFrom } from 'rxjs';
import { pairwise, startWith } from 'rxjs/operators';
import { Toast, ToastType } from '../../../../services/toast';
import { GlobalWidgetConfigService } from '../../shared/services/global-widget-config.service';
import { TextVariant } from '../../shared/types/text-variant';
import { AiTextWidgetConfig } from '../../shared/types/widget-config/ai-text-widget-config';
import { retrieveResultString } from '../../shared/utils/ai-util';
import { getNodeOrDefaultNodeId } from '../../shared/utils/node-util';
import {
    convertNodeRefIntoNodeId,
    retrievePromptFromAiConfig,
} from '../../shared/utils/template-util';
import { EditableTextComponent } from '../shared/editable-text/editable-text.component';
import { WidgetConfigurationButtonsComponent } from '../shared/widget-configuration-buttons/widget-configuration-buttons.component';
import { ConfigurationOption } from '../../shared/types/configuration-option';
import { AiHelperService } from '../../shared/services/ai-helper.service';
import { BapiConfig } from '../../shared/types/bapi-config';
import { BapiConfigObject } from '../../shared/types/bapi-config-object';
import { StandardSelectInput } from '../../shared/types/standard-select-input';
import { WidgetComponentInterface } from '../generic-widget/generic-widget.component';
import { GenericWidgetGlobalService } from '../generic-widget/generic-widget-global.service';

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
    selectedDimensionValues: InputSignal<MdsValue[]> = input<MdsValue[]>([]);
    @Input() swimlaneIndex: number = -1;

    @Output() configChanged: EventEmitter<void> = new EventEmitter<void>();
    @Output() embedWidgetClicked: EventEmitter<void> = new EventEmitter<void>();
    @Output() internalSearchResultCountChanged: EventEmitter<number> = new EventEmitter<number>();

    @ViewChild('promptTextarea') promptTextarea: ElementRef<HTMLTextAreaElement>;

    // VARIABLES
    aiGeneratedText: WritableSignal<boolean> = signal(false);
    form: FormGroup;
    initialized: WritableSignal<boolean> = signal(false);
    latestStoredPrompt: string = '';
    private latestStoredTexts: TextVariant[] = [];
    processPromptInProgress: boolean = false;
    reloadingIndicator: boolean = false;
    resultString: string = '';
    triggerTextUpdate: WritableSignal<boolean> = signal(false);
    updateInProgress: WritableSignal<boolean> = signal(false);

    constructor(
        private aiHelperService: AiHelperService,
        private globalWidgetConfigService: GlobalWidgetConfigService,
        private genericWidgetGlobalService: GenericWidgetGlobalService,
        private mdsService: MdsService,
        private toast: Toast,
    ) {
        effect(() => {
            // register signals as dependencies
            const currentEditMode = this.editMode();
            this.selectedDimensionValues();

            if (this.initialized()) {
                // react to edit mode changes
                void this.editModeDisplayAction(currentEditMode);

                // AI text generation
                if (!currentEditMode) {
                    this.reloadingIndicator = true;
                    this.retrieveTextOrRequestAiGeneration()
                        .then(() => {
                            this.reloadingIndicator = false;
                        })
                        .catch((error) => {
                            console.error('KI Widget: An error occurred', error);
                            this.initialized.set(true);
                        });
                }
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
     * Returns all available selection fields for the dimensions.
     */
    get dimensions(): FormArray {
        return this.form.get('dimensions') as FormArray;
    }

    /**
     * Returns the dimension IDs as an array.
     */
    get outputDimensionIds(): string[] {
        return this.form.get('dimensions').value.map((val: StandardSelectInput) => val.id);
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
        } else {
            // use the latest stored prompt to generate the select fields for the dimensions
            this.extractDimensionsFromPrompt();
        }
    }

    /**
     * Retrieves the text from the stored texts or executes a prompt to generate the text using AI.
     */
    private async retrieveTextOrRequestAiGeneration(): Promise<void> {
        const selectedIds: string[] = this.selectedDimensionValues().map((dim: MdsValue) => dim.id);
        const existingTextIndex: number = this.retrieveExistingValueForSelection(selectedIds);
        // a text exists, use it
        if (existingTextIndex !== -1) {
            this.resultString = this.latestStoredTexts?.[existingTextIndex]?.textValue?.text;
            this.aiGeneratedText.set(false);
        }
        // no text exists, request AI generation
        if (existingTextIndex === -1 || !this.resultString || this.resultString === '') {
            await this.executePrompt();
            this.aiGeneratedText.set(true);
        }
    }

    /**
     * Extracts the selection fields for the dimensions from the latest stored prompt.
     */
    private extractDimensionsFromPrompt(): void {
        // reset the dimensions to be always up-to-date
        this.dimensions.clear();
        // only create select fields for dimensions that are included in the latest stored prompt
        this.selectDimensionKeysUsed.forEach((): void => {
            const item: FormGroup<StandardSelectInput> = new FormGroup({
                id: new FormControl(''),
            });
            this.dimensions.push(item);
        });
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
        console.info('AI-Text-Widget: Processing in progress');
        // reset the latest stored texts and result text due to re-generation
        this.latestStoredTexts = [];
        this.form.patchValue({
            result: '',
        });
        this.latestStoredPrompt = this.form.get('prompt').value;
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
        // disable form, as this might take some time
        this.form.disable();
        // open a snack bar for the user to inform about a generation process
        this.toast.show({
            message: 'TOPIC_PAGE.WIDGET.AI_WIDGET.GENERATE_TEXT_HINT',
            type: 'info',
            subtype: ToastType.InfoSimple,
        });
        // create a request for the currently selected dimension values
        const outputDimensionIds = this.form.get('dimensions').value.map((val: any) => val.id);
        const variables: { [key: string]: string[] } = {};
        outputDimensionIds.forEach((selectedValue: string, index: number): void => {
            if (selectedValue !== '') {
                const dimensionKey: string = this.selectDimensionKeysUsed[index];
                variables[dimensionKey] = [selectedValue];
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

        if (
            Object.keys(givenVariables).length > 0 &&
            this.selectedDimensionValues().length !== this.availableSelectDimensionKeys.length
        ) {
            return;
        }

        // either use a given request body or create a new one from the globally selected dimension values
        let variables: { [key: string]: string[] } = {};
        if (Object.keys(givenVariables).length > 0) {
            variables = givenVariables;
        } else {
            this.availableSelectDimensionKeys.forEach((key: string, index: number): void => {
                const valueId: string = this.selectedDimensionValues()[index]?.id;
                if (key && valueId) {
                    variables[key] = [valueId];
                }
            });
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
        // in edit mode, patch the existing form value
        if (this.editMode()) {
            // enable form for patching the value
            this.form.enable();
            this.form.patchValue({
                result: this.resultString,
            });
        }
    }

    /**
     * Saves the modification made for the current selection.
     */
    async saveModification(): Promise<void> {
        // create an output object
        const outputObject: TextVariant = {};
        this.outputDimensionIds.forEach((selectedValue: string, index: number): void => {
            if (selectedValue !== '') {
                const dimensionKey: string = this.selectDimensionKeysUsed[index];
                outputObject[dimensionKey] = selectedValue;
            }
        });
        const currentUser: string = await this.aiHelperService.getCurrentUser();
        outputObject['textValue'] = {
            text: this.form.get('result').value,
            updatedAt: Date.now(),
            updatedBy: currentUser,
        };

        // retrieve a potentially existing value
        const existingValueIndex: number = this.retrieveExistingValueForSelection();

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
        const currentValue = this.form.get('prompt').value;
        // check whether the string ends with whitespace: https://stackoverflow.com/a/30566492
        const endSpace: RegExp = /\s$/;
        // add prefix and suffix
        const dimensionKey: string = this.dimensionPrefix + placeholderKey + this.dimensionSuffix;
        // only add leading whitespace, if it does not already exist
        let valueToAdd: string = endSpace.test(currentValue) ? dimensionKey : ' ' + dimensionKey;
        this.form.patchValue({
            prompt: this.form.get('prompt').value + valueToAdd,
        });
        // focus textarea after appending the string
        this.promptTextarea?.nativeElement?.focus();
    }

    /**
     * Checks for changes made in the prompt textarea to open a snack bar, if necessary.
     */
    checkForPromptChanges(): void {
        if (this.form.get('prompt').value !== this.latestStoredPrompt) {
            // open a snack bar for the user to inform about unsaved modifications
            this.toast.show({
                message: 'TOPIC_PAGE.WIDGET.AI_WIDGET.PROMPT_ADJUSTED_HINT',
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
        // define the initial form values
        this.form = new FormGroup({
            prompt: new FormControl(''),
            result: new FormControl(''),
            dimensions: new FormArray([]),
        });
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
            this.form.patchValue({
                prompt: this.latestStoredPrompt,
            });
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
            this.form.patchValue({
                prompt: this.latestStoredPrompt,
            });
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
        return { prompt: this.form.get('prompt').value };
    }

    // noinspection JSUnusedGlobalSymbols
    /**
     * Postload action to execute edit mode actions and listening to value changes in the form.
     */
    async postLoadAction(): Promise<void> {
        await this.editModeDisplayAction(this.editMode());

        // listen for value changes in the form dimensions: https://stackoverflow.com/a/54205373
        // this fills the buffer with an initial value, and it will emit immediately on value change
        this.form
            .get('dimensions')
            .valueChanges.pipe(startWith(null as string), pairwise())
            .subscribe(([, next]: [any, any]): void => {
                // in edit mode, check whether a direct match for the selection does exist
                if (this.editMode()) {
                    const customSelection = next.map((n: any) => n.id);
                    const existingValueIndex: number =
                        this.retrieveExistingValueForSelection(customSelection);
                    // if a value exists, set it into the form, else initialize with empty string
                    const resultValue =
                        existingValueIndex !== -1
                            ? {
                                  result:
                                      this.latestStoredTexts[existingValueIndex]?.textValue?.text ??
                                      '',
                              }
                            : { result: '' };
                    this.form.patchValue(resultValue);
                    // workaround to fix that text is not updated on selection change
                    this.triggerTextUpdate.set(true);
                    setTimeout((): void => {
                        this.triggerTextUpdate.set(false);
                    });
                }
            });
    }

    /**
     * Helper function to retrieve a potentially existing value for the current selection.
     *
     * @param customSelection
     */
    private retrieveExistingValueForSelection(customSelection?: string[]): number {
        // workaround to compare selections made
        const outputAttributeValueStrings: string[] = ['textValue'];

        // either take an input of selections or the selections from the form
        const selection: string[] = customSelection ?? this.outputDimensionIds;
        selection.forEach((selectedValue: string, index: number) => {
            // match by index (might not be working for every use case)
            const dimensionKey: string = this.selectDimensionKeysUsed[index];
            // ensure that the selectedValue does exist for the dimension
            const dimensionValues: string[] =
                this.selectDimensions.get(dimensionKey)?.values?.map((val: MdsValue) => val.id) ??
                [];
            if (dimensionValues.includes(selectedValue)) {
                // match both dimension key and value
                outputAttributeValueStrings.push(dimensionKey + '_' + selectedValue);
            } else {
                // workaround: search for first match
                // TODO: this does not work if dimensions have values with the same id
                for (let [key, dimension] of this.selectDimensions) {
                    let selectDimensionValues: string[] =
                        dimension?.values?.map((val: MdsValue) => val.id) ?? [];
                    if (selectDimensionValues.includes(selectedValue)) {
                        // match both dimension key and value
                        outputAttributeValueStrings.push(key + '_' + selectedValue);
                        break;
                    }
                }
            }
        });

        // search for an object that already defines all dimensions
        // if multiple exist, take the one with the most variables
        let existingValueIndex: number = -1;
        let highestNumberOfVariables: number = -1;
        // sort copy of latestStoredTexts by the largest number of variables
        // https://stackoverflow.com/a/42442909
        // const latestStoredTexts: TextVariant[] = [...this.latestStoredTexts].sort((a, b) => Object.keys(b).length - Object.keys(a).length);
        this.latestStoredTexts.forEach((textObject: TextVariant, index: number): void => {
            // match both dimension id and value
            const textAttributeValueStrings: string[] = Object.keys(textObject)
                .sort()
                .map((key: string) => {
                    if (key !== 'textValue') {
                        key += '_' + textObject[key];
                    }
                    return key;
                });

            // check if all items are included and no better match was found
            if (
                textAttributeValueStrings.every((val: string) =>
                    outputAttributeValueStrings.includes(val),
                ) &&
                textAttributeValueStrings.length > highestNumberOfVariables
            ) {
                existingValueIndex = index;
                highestNumberOfVariables = textAttributeValueStrings.length;
            }
        });
        return existingValueIndex;
    }

    protected readonly dimensionPrefix: string = '{{var(';
    protected readonly dimensionSuffix: string = ')|-}}';
}
