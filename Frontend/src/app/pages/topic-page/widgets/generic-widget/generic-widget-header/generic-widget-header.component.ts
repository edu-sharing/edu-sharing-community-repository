import { Component, computed, input, output } from '@angular/core';
import { SharedModule } from '../../../../../shared/shared.module';
import { AiTextPromptPipe } from '../../../shared/pipes/ai-text-prompt.pipe';
import { PromptToTextMapping } from '../../../shared/types/prompt-to-text-mapping';
import { AiTextPlaceholderComponent } from '../../shared/ai-text-placeholder/ai-text-placeholder.component';
import { EditableTextComponent } from '../../shared/editable-text/editable-text.component';

export interface GenerateWithAiChangeEvent {
    aiGenerated: boolean;
    isHeadline: boolean;
}
export interface SearchResultsEvent {
    count: number;
    type: string;
}
export interface TextChangeEvent {
    text: string;
    isHeadline: boolean;
}

@Component({
    selector: 'es-generic-widget-header',
    standalone: true,
    imports: [AiTextPlaceholderComponent, AiTextPromptPipe, EditableTextComponent, SharedModule],
    templateUrl: './generic-widget-header.component.html',
    styleUrls: ['./generic-widget-header.component.scss'],
})
export class WidgetHeaderComponent {
    aiSupported = input<boolean>(false);
    description = input<string>('');
    descriptionAiGenerated = input<boolean>(false);
    // the description is still being generated from its prompt
    descriptionLoading = input<boolean>(false);
    descriptionMapping = input<PromptToTextMapping>();
    descriptionOptional = input<boolean>(true);
    hideDescription = input<boolean>(false);
    headline = input<string>('');
    headlineAiGenerated = input<boolean>(false);
    // the headline is still being generated from its prompt
    headlineLoading = input<boolean>(false);
    headlineMapping = input<PromptToTextMapping>();
    editMode = input<boolean>(false);
    updateInProgress = input<boolean>(false);
    searchInput = input<string>('');

    generateWithAiChanged = output<GenerateWithAiChangeEvent>();
    textChange = output<TextChangeEvent>();
    searchResultsUpdated = output<SearchResultsEvent>();

    // computed properties for template
    // in the editor the prompt itself is shown, so a pending result is only marked outside of it
    readonly showHeadlinePlaceholder = computed(() => this.headlineLoading() && !this.editMode());
    readonly showDescriptionPlaceholder = computed(
        () => this.descriptionLoading() && !this.hideDescription() && !this.editMode(),
    );
    readonly shouldShowHeadline = computed(() => this.headline() || this.editMode());
    readonly shouldShowDescription = computed(
        () => (this.description() && !this.hideDescription()) || this.editMode(),
    );

    readonly descriptionLabel = computed(() =>
        this.descriptionOptional()
            ? 'TOPIC_PAGE.WIDGET.DESCRIPTION_LABEL_OPTIONAL'
            : 'TOPIC_PAGE.WIDGET.DESCRIPTION_LABEL',
    );

    /**
     * Emits the text change event.
     */
    onTextChange = (text: string, isHeadline: boolean = false): void => {
        this.textChange.emit({ text, isHeadline });
    };

    /**
     * Emits the change event whether the text should be generated with AI received by self-adjusting-textarea.
     */
    onGenerateWithAiChanged = (checked: boolean, isHeadline: boolean): void => {
        this.generateWithAiChanged.emit({ aiGenerated: checked, isHeadline });
    };

    /**
     * Emits the search results updated event.
     */
    onSearchResultsUpdated = (count: number, type: string): void => {
        this.searchResultsUpdated.emit({ count, type });
    };
}
