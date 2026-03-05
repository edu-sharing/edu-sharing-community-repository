import { Component, computed, input, output } from '@angular/core';
import { EduSharingUiModule } from 'ngx-edu-sharing-ui';
import { AiTextPromptPipe } from '../../../shared/pipes/ai-text-prompt.pipe';
import { PromptToTextMapping } from '../../../shared/types/prompt-to-text-mapping';
import { EditableTextComponent } from '../../shared/editable-text/editable-text.component';

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
    imports: [AiTextPromptPipe, EditableTextComponent, EduSharingUiModule],
    templateUrl: './generic-widget-header.component.html',
    styleUrls: ['./generic-widget-header.component.scss'],
})
export class WidgetHeaderComponent {
    aiSupported = input<boolean>(false);
    description = input<string>('');
    descriptionAiGenerated = input<boolean>(false);
    descriptionMapping = input<PromptToTextMapping>();
    descriptionOptional = input<boolean>(true);
    hideDescription = input<boolean>(false);
    headline = input<string>('');
    headlineAiGenerated = input<boolean>(false);
    headlineMapping = input<PromptToTextMapping>();
    editMode = input<boolean>(false);
    updateInProgress = input<boolean>(false);
    searchInput = input<string>('');

    textChange = output<TextChangeEvent>();
    searchResultsUpdated = output<SearchResultsEvent>();

    // computed properties for template
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
     * Emits the search results updated event.
     */
    onSearchResultsUpdated = (count: number, type: string): void => {
        this.searchResultsUpdated.emit({ count, type });
    };
}
