import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, signal, WritableSignal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';
import { EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';
import { VarDirective } from '../../../shared/directives/ng-var.directive';
import { HighlightSearchPipe } from '../../../shared/pipes/highlight-search.pipe';
import { MarkdownPipe } from '../../../shared/pipes/markdown.pipe';
import { AiLabelComponent } from '../ai-label/ai-label.component';
import { SelfAdjustingTextareaComponent } from './self-adjusting-textarea/self-adjusting-textarea.component';

@Component({
    selector: 'es-editable-text',
    imports: [
        AiLabelComponent,
        CommonModule,
        EduSharingUiCommonModule,
        MatButtonModule,
        SelfAdjustingTextareaComponent,
        TranslateModule,
        VarDirective,
    ],
    providers: [HighlightSearchPipe, MarkdownPipe],
    templateUrl: './editable-text.component.html',
    styleUrls: ['./editable-text.component.scss'],
})
export class EditableTextComponent {
    @Input() aiGenerated: boolean = false;
    @Input() alignCenter: boolean = false;
    @Input() applyTextareaPadding: boolean = true;
    @Input() disabled: boolean = false;
    @Input() loading: boolean = false;
    @Input() editable: boolean;
    @Input() headerElement: '' | 'h1' | 'h2' | 'h3' = '';
    @Input() inputLimit?: number;
    @Input() label: string = '';
    // overwrite text in non-edit mode; useful for our string replacement
    @Input() nonEditText: string = '';
    @Input() prominentText: boolean = false;
    private _searchInput: string;
    @Input() get searchInput(): string {
        return this._searchInput;
    }
    set searchInput(value: string) {
        this._searchInput = value;
        this.computeOutputText();
    }
    @Input() showAiButtons: boolean = false;
    @Input() showAiCheckbox: boolean = false;
    @Input() showMoreLimit?: number;
    private _text: string;
    @Input() get text(): string {
        return this._text;
    }
    set text(value: string) {
        this._text = value ?? '';
        this.computeOutputText();
    }
    @Output() generateWithAiChanged: EventEmitter<boolean> = new EventEmitter<boolean>();
    @Output() searchResultsUpdated: EventEmitter<number> = new EventEmitter<number>();
    @Output() textChange: EventEmitter<string> = new EventEmitter<string>();

    outputText: WritableSignal<string> = signal(null);
    showMore: boolean = false;

    constructor(private markdown: MarkdownPipe, private highlightSearch: HighlightSearchPipe) {}

    /**
     * Emits the change event whether the text should be generated with AI received by self-adjusting-textarea.
     *
     * @param checked
     */
    onGenerateWithAiChanged(checked: boolean): void {
        this.generateWithAiChanged.emit(checked);
    }

    /**
     * Emits text changes received by self-adjusting-textarea.
     *
     * @param text
     */
    emitTextChange(text: string): void {
        this.textChange.emit(text);
    }

    /**
     * Toggle the value of the show more button.
     */
    toggleShowMore(): void {
        this.showMore = !this.showMore;
        this.computeOutputText();
    }

    /**
     * Helper function to compute the output text based on the inputs
     * and emitting the number of search hits if a search input exists.
     */
    computeOutputText(): void {
        const textToDisplay: string = this.nonEditText || this.text;
        const parseMarkdown: boolean = this.headerElement === '';
        let outputText: string = textToDisplay ?? '';
        if (this.showMoreLimit && textToDisplay.length > this.showMoreLimit && !this.showMore) {
            outputText = textToDisplay.slice(0, this.showMoreLimit) + '...';
        }
        if (parseMarkdown) {
            // apply markdown pipe
            outputText = this.markdown.transform(outputText);
        }
        // search, if necessary
        if (this.searchInput) {
            outputText = this.highlightSearch.transform(outputText, this.searchInput);
        }
        // emit number of hits
        const numberOfHits: number = (
            outputText.match(new RegExp('class="topic-page-search-highlight"', 'g')) || []
        ).length;
        this.searchResultsUpdated.emit(numberOfHits);
        this.outputText.set(outputText);
    }
}
