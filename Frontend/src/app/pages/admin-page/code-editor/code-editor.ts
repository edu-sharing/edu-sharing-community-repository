import {
    Component,
    EventEmitter,
    computed,
    forwardRef,
    Input,
    Output,
    inject,
    signal,
} from '@angular/core';
import { ConfigService } from 'ngx-edu-sharing-api';
import { NgxMonacoEditorConfig } from 'ngx-monaco-editor-v2';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { ThemeService } from '../../../services/theme.service';

@Component({
    selector: 'es-code-editor',
    templateUrl: 'code-editor.component.html',
    styleUrls: ['code-editor.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => CodeEditorComponent),
            multi: true,
        },
    ],
    standalone: false,
})
export class CodeEditorComponent implements ControlValueAccessor {
    private configService = inject(ConfigService);
    private themeService = inject(ThemeService);

    private optionsSignal = signal<NgxMonacoEditorConfig | any>(undefined);
    @Input() set options(value: NgxMonacoEditorConfig | any) {
        this.optionsSignal.set(value);
    }
    get options(): NgxMonacoEditorConfig | any {
        return this.optionsSignal();
    }
    // append theme variable to monaco settings depending on darkMode
    editorOptions = computed(() => ({
        ...(this.optionsSignal() ?? {}),
        theme: this.themeService.isDarkMode() ? 'vs-dark' : 'vs',
    }));

    @Input() ngModel: string;
    @Output() ngModelChange = new EventEmitter<string>();
    editorType: 'Textarea' | 'Monaco' | undefined;

    constructor() {
        this.configService.observeConfig().subscribe((config) => {
            this.editorType = config.admin?.editorType || 'Monaco';
        });
    }

    writeValue(obj: any): void {
        this.ngModel = obj;
    }
    registerOnChange(fn: any): void {
        this.ngModelChange.subscribe((v) => {
            fn(v);
        });
    }
    registerOnTouched(fn: any): void {}
    setDisabledState?(isDisabled: boolean): void {
        if (isDisabled) {
            throw new Error('Method not implemented.');
        }
    }
}
