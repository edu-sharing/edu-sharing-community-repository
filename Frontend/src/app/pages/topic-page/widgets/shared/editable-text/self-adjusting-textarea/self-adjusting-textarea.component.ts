import { CdkTextareaAutosize } from '@angular/cdk/text-field';
import { CommonModule } from '@angular/common';
import {
    Component,
    CUSTOM_ELEMENTS_SCHEMA,
    EventEmitter,
    Input,
    NgZone,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltip } from '@angular/material/tooltip';
import '@github/markdown-toolbar-element';
import { TranslateModule } from '@ngx-translate/core';
import { EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';
import { take } from 'rxjs';
import { v4 as uuidv4 } from 'uuid';
import { RestConstants } from '../../../../../../core-module/rest/rest-constants';
import { AiTagOption } from '../../../../shared/types/ai-tag-option';

@Component({
    selector: 'es-self-adjusting-textarea',
    imports: [
        CommonModule,
        EduSharingUiCommonModule,
        FormsModule,
        MatButtonModule,
        MatFormFieldModule,
        MatInputModule,
        MatTooltip,
        ReactiveFormsModule,
        TranslateModule,
    ],
    templateUrl: './self-adjusting-textarea.component.html',
    styleUrls: ['./self-adjusting-textarea.component.scss'],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class SelfAdjustingTextareaComponent implements OnInit, OnChanges {
    @Input() alignCenter: boolean = false;
    @Input() disabled: boolean = false;
    @Input() headerElement: '' | 'h1' | 'h2' | 'h3' = '';
    @Input() hideToolbar: boolean = false;
    @Input() inputLimit?: number;
    @Input() label: string;
    @Input() showAiButtons: boolean = false;
    @Input() text: string;
    @Output() textChange: EventEmitter<string> = new EventEmitter<string>();
    @ViewChild('autosize', { static: false }) autosize: CdkTextareaAutosize;

    aiTagOptions: AiTagOption[] = [];
    avoidFocusChange: boolean = false;
    controlId: string;
    descriptionControl: FormControl;
    latestStoredText: string;
    markdownForm: FormGroup;

    constructor(private ngZone: NgZone) {}

    /**
     * Initializes the component by assigning a unique control id and creating a form control for the text input
     * if no control is provided via the input.
     */
    ngOnInit(): void {
        this.controlId = `MarkdownEditor-${uuidv4()}`;
        this.descriptionControl = this.descriptionControl ?? new FormControl();
        this.aiTagOptions.push(
            new AiTagOption(
                'TOPIC_PAGE.WIDGET.EDITABLE_TEXT.TITLE',
                'title',
                'TOPIC_PAGE.WIDGET.EDITABLE_TEXT.TITLE',
                '{{node(' + RestConstants.CM_PROP_TITLE + ')|-}}',
            ),
            new AiTagOption(
                'TOPIC_PAGE.WIDGET.EDITABLE_TEXT.DESCRIPTION',
                'description',
                'TOPIC_PAGE.WIDGET.EDITABLE_TEXT.DESCRIPTION',
                '{{node(' + RestConstants.CM_DESCRIPTION + ')|-}}',
            ),
            new AiTagOption(
                'TOPIC_PAGE.WIDGET.EDITABLE_TEXT.TARGET_GROUP',
                'group',
                'TOPIC_PAGE.WIDGET.EDITABLE_TEXT.TARGET_GROUP',
                '{{var(virtual:profiling_widget_intention)|-}}',
            ),
            new AiTagOption(
                'TOPIC_PAGE.WIDGET.EDITABLE_TEXT.EDUCATIONAL_LEVEL',
                'school',
                'TOPIC_PAGE.WIDGET.EDITABLE_TEXT.EDUCATIONAL_LEVEL',
                '{{var(virtual:profiling_widget_education_level)|-}}',
            ),
        );
    }

    /**
     * Listens to initial changes of the text and resizes the textarea accordingly.
     *
     * @param changes
     */
    ngOnChanges(changes: SimpleChanges): void {
        // listen to text changes
        if (changes.text?.firstChange) {
            this.latestStoredText = changes.text.currentValue;
            this.markdownForm = new FormGroup({
                description: new FormControl(this.text),
            });
            this.descriptionControl = this.markdownForm.controls.description as FormControl;
            // wait for changes to be applied, then trigger textarea resize
            // reference: https://stackblitz.com/angular/pbadbpbgyog?file=app%2Ftext-field-autosize-textarea-example.ts
            this.ngZone.onStable
                .pipe(take(1))
                .subscribe(() => this.autosize?.resizeToFitContent(true));
        } else if (changes.text?.previousValue !== changes.text?.currentValue) {
            this.latestStoredText = changes.text.currentValue;
            this.descriptionControl.patchValue(changes.text.currentValue);
        }
        // listen to changes of disabled state
        if (changes.disabled) {
            if (changes.disabled.currentValue === true) {
                this.descriptionControl.disable();
            } else {
                this.descriptionControl.enable();
            }
        }
    }

    /**
     * Adds a given AI tag string to the current text.
     */
    addAiTag(tag: string): void {
        // check whether the string ends with whitespace: https://stackoverflow.com/a/30566492
        const endSpace: RegExp = /\s$/;
        // only add leading whitespace, if it does not already exist
        this.descriptionControl.setValue(
            this.descriptionControl.value +
                (endSpace.test(this.descriptionControl.value) ? '' : ' ') +
                tag,
        );
    }

    /**
     * Saves the (changed) text by emitting it.
     */
    saveText(): void {
        // workaround to avoid saveText being triggered on every toolbar button click
        if (this.avoidFocusChange) {
            return;
        }
        // check, whether the text has been changed
        if (this.latestStoredText !== this.descriptionControl.value) {
            this.textChange.emit(this.descriptionControl.value);
        }
    }

    /**
     * Allows to avoid a focus change of the textarea,
     * which is necessary to prevent it from saving the text on every click on the toolbar.
     */
    avoidSaveText(): void {
        this.avoidFocusChange = true;
        setTimeout(() => {
            this.avoidFocusChange = false;
        }, 250);
    }
}
