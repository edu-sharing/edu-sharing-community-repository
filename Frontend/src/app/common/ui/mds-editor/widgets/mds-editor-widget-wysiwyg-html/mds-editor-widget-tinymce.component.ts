import {AfterViewInit, Component, OnInit, ViewChild} from '@angular/core';
import { AbstractControl, FormControl, ValidatorFn } from '@angular/forms';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';
import {ContentChange, QuillEditorComponent} from 'ngx-quill';
import {AngularEditorComponent, AngularEditorConfig} from '@kolkov/angular-editor';
import {Editor, NgxEditorComponent} from 'ngx-editor';
import Locals from 'ngx-editor/lib/Locals';
import {TranslateService} from '@ngx-translate/core';
import {MdsEditorInstanceService} from '../../mds-editor-instance.service';
import {EditorComponent} from '@tinymce/tinymce-angular';
import {EventObj} from '@tinymce/tinymce-angular/editor/Events';
import {PlatformLocation} from '@angular/common';
import {Translation} from '../../../../../core-ui-module/translation';

@Component({
    selector: 'es-mds-editor-widget-checkbox',
    templateUrl: './mds-editor-widget-tinymce.component.html',
    styleUrls: ['./mds-editor-widget-tinymce.component.scss'],
})
export class MdsEditorWidgetTinyMCE extends MdsEditorWidgetBase implements OnInit, AfterViewInit {
    @ViewChild('editorComponent') editorComponent: EditorComponent;
    readonly valueType: ValueType = ValueType.String;
    editorConfig = {
        branding: false,
        height: 200,
        base_url: this.platformLocation.getBaseHrefFromDOM() + 'tinymce',
        suffix: '.min',
        menubar: false,
        statusbar: false,
        resize: true,
        plugins: ['link'],
        style_formats: [

            { title: 'Red header', block: 'h1', styles: { color: '#ff0000' } },

        ],
        toolbar:
            'h1 h2 h3 h4 | bold italic underline | link | alignleft aligncenter alignright alignjustify | removeformat | undo redo',
        language: this.translate.getDefaultLang()
    };
    _html = '';
    get html() {
        return this._html;
    }
    set html(html: string) {
        this._html = html;
        this.setValue([html]);
    }
    constructor(
        private platformLocation: PlatformLocation,
        public mdsEditorInstance: MdsEditorInstanceService,
        protected translate: TranslateService,
    ) {
       super(mdsEditorInstance, translate);
    }
    ngOnInit(): void {
    }

    onIndeterminateChange(isIndeterminate: boolean): void {
        this.setIndeterminateValues(isIndeterminate);
    }

    private setIndeterminateValues(isIndeterminate: boolean): void {
        if (isIndeterminate) {
            this.widget.setIndeterminateValues(['false', 'true']);
        } else {
            this.widget.setIndeterminateValues(null);
        }
    }

    focus(): void {
        // this.editor.editorElem.focus();
    }

    blur(): void {
        // this.editor.blur();
    }

    ngAfterViewInit(): void {
        this._html = this.widget.getInitialValues().jointValues[0];
        if(this.widget.definition.configuration) {
            this.editorConfig = {...this.editorConfig, ...JSON.parse(this.widget.definition.configuration)};
        }
        /*this.editor.modules = {
            toolbar: [
                [{ header: 1 }, { header: 2 }],['bold', 'italic', 'underline', 'strike'], ['link']
            ]
        };
        this.editor.content = this.widget.getInitialValues().jointValues[0];
         */
        // this.editorComponent.initialValue = this.widget.getInitialValues().jointValues[0];
    }
}
