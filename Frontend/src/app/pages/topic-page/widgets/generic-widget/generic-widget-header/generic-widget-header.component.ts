import { Component, computed, input, output } from '@angular/core';
import { EduSharingUiModule } from 'ngx-edu-sharing-ui';
import { PromptToTextMapping } from '../../../shared/types/prompt-to-text-mapping';
import { EditableTextComponent } from '../../shared/editable-text/editable-text.component';
import { AiTextPromptPipe } from '../../../shared/pipes/ai-text-prompt.pipe';

export interface TextChangeEvent {
    text: string;
    isHeadline: boolean;
}

export interface SearchResultsEvent {
    count: number;
    type: string;
}

@Component({
    selector: 'es-generic-widget-header',
    standalone: true,
    imports: [AiTextPromptPipe, EditableTextComponent, EduSharingUiModule],
    styleUrls: ['./generic-widget-header.component.scss'],
    templateUrl: './generic-widget-header.component.html',
})
export class WidgetHeaderComponent {
    description = input<string>('');
    descriptionAiGenerated = input<boolean>(false);
    descriptionMapping = input<PromptToTextMapping>();
    descriptionOptional = input<boolean>(true);
    headline = input<string>('');
    headlineAiGenerated = input<boolean>(false);
    headlineMapping = input<PromptToTextMapping>();
    editMode = input<boolean>(false);
    updateInProgress = input<boolean>(false);
    searchInput = input<string>('');
    hideDescription = input<boolean>(false);

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
