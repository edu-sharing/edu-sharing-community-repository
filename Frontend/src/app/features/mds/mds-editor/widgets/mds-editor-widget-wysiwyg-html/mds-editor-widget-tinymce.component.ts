import {
    AfterViewInit,
    Component,
    effect,
    inject,
    signal,
    untracked,
    ViewChild,
} from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { MdsEditorWidgetBase } from '../mds-editor-widget-base';
import { EditorComponent, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';
import { PlatformLocation } from '@angular/common';
import { MdsEditorWidgetContainerComponent } from '../mds-editor-widget-container/mds-editor-widget-container.component';
import { CardDialogService } from '../../../../dialogs/card-dialog/card-dialog.service';
import { ThemeService } from '../../../../../services/theme.service';
import { ValueType } from 'ngx-edu-sharing-ui';
import {
    getTinyMceBaseConfig,
    getTinyMceThemeConfig,
} from '../../../../../shared/tinymce/tinymce-config';

@Component({
    selector: 'es-mds-editor-widget-checkbox',
    templateUrl: './mds-editor-widget-tinymce.component.html',
    styleUrls: ['./mds-editor-widget-tinymce.component.scss'],
    providers: [
        {
            provide: TINYMCE_SCRIPT_SRC,
            useFactory: (platformLocation: PlatformLocation) => {
                return platformLocation.getBaseHrefFromDOM() + 'assets/tinymce/tinymce.min.js';
            },
            deps: [PlatformLocation],
        },
    ],
    standalone: false,
})
export class MdsEditorWidgetTinyMCEComponent extends MdsEditorWidgetBase implements AfterViewInit {
    private platformLocation = inject(PlatformLocation);
    private theme = inject(ThemeService);
    cardService = inject(CardDialogService);

    @ViewChild(EditorComponent) editorComponent: EditorComponent;
    @ViewChild(MdsEditorWidgetContainerComponent)
    containerComponent: MdsEditorWidgetContainerComponent;
    readonly valueType: ValueType = ValueType.String;
    private editorConfigDefault = {
        height: 200,
        apiKey: '',
        menubar: false,
        statusbar: false,
        resize: true,
        plugins: ['link'],
        toolbar:
            'h1 h2 h3 h4 | bold italic underline | link | alignleft aligncenter alignright alignjustify | removeformat | undo redo',
    };
    editorConfig = signal<Record<string, any>>(null);
    _html = '';
    private disabledChangeRegistered = false;
    dummyControl = new UntypedFormControl();
    get html() {
        return this._html;
    }
    set html(html: string) {
        this._html = html;
        this.setValue([html]);
    }
    constructor() {
        super();
        // Rebuild the editor config when the theme flips so the editor is re-created
        // with the matching skin (TinyMCE cannot swap skins on a live instance).
        effect(() => {
            this.theme.isDarkMode();
            untracked(() => {
                if (this.editorConfig()) {
                    this.buildConfig();
                }
            });
        });
    }

    private buildConfig(): void {
        this.editorConfig.set({
            ...getTinyMceBaseConfig(
                this.platformLocation.getBaseHrefFromDOM(),
                this.translate.getDefaultLang(),
            ),
            ...this.editorConfigDefault,
            // custom configuration of the mds widget definition
            ...(this.widget.definition.configuration
                ? JSON.parse(this.widget.definition.configuration)
                : {}),
            ...getTinyMceThemeConfig(this.theme.isDarkMode()),
        });
    }

    onEditorInit(): void {
        this.editorComponent.editor.mode.set(this.dummyControl.disabled ? 'readonly' : 'design');
        if (!this.disabledChangeRegistered) {
            this.disabledChangeRegistered = true;
            this.dummyControl.registerOnDisabledChange((isDisabled) =>
                this.editorComponent?.editor?.mode.set(isDisabled ? 'readonly' : 'design'),
            );
        }
        // we need to disable the focus trap cause otherwise any overlay dialogs (i.e. insert link) of tinymce will break
        this.cardService.getFocusTraps().forEach((f) => f._disable());
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
        this.editorComponent.editor.focus();
        // this.editor.editorElem.focus();
    }

    blur(): void {
        // @TODO: Check if this works as expected
        this.editorComponent.editor.editorContainer.blur();
        //this.editorComponent.editor.execCommand('blur');
    }

    async ngAfterViewInit() {
        this._html = (await this.widget.getInitalValuesAsync()).jointValues[0];
        // dirty workaround for tinyMCE
        setTimeout(() => this.buildConfig());
    }
}
