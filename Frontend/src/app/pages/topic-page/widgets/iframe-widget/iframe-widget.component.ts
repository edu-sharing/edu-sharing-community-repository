import {
    Component,
    computed,
    CUSTOM_ELEMENTS_SCHEMA,
    effect,
    EventEmitter,
    input,
    Input,
    InputSignal,
    Output,
    Signal,
    signal,
    ViewEncapsulation,
    WritableSignal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup } from '@angular/forms';
import { Node } from 'ngx-edu-sharing-api';
import { SharedModule } from '../../../../shared/shared.module';
import { SafeUrlPipe } from '../../shared/pipes/save-url.pipe';
import { ConfigurationOption } from '../../shared/types/configuration-option';
import { IframeWidgetConfig } from '../../shared/types/widget-config/iframe-widget-config';
import { WidgetComponentInterface } from '../generic-widget/generic-widget.component';
import { WidgetConfigurationButtonsComponent } from '../shared/widget-configuration-buttons/widget-configuration-buttons.component';

export type IframeConfigForm = {
    [K in keyof IframeWidgetConfig]: FormControl<IframeWidgetConfig[K]>;
};

@Component({
    selector: 'es-iframe-widget',
    encapsulation: ViewEncapsulation.Emulated,
    imports: [SafeUrlPipe, SharedModule, WidgetConfigurationButtonsComponent],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    templateUrl: './iframe-widget.component.html',
    styleUrls: ['./iframe-widget.component.scss'],
})
export class IframeWidgetComponent implements WidgetComponentInterface {
    // CONSTANTS
    readonly i18nPrefix: string = 'TOPIC_PAGE.WIDGET.IFRAME_WIDGET.';

    // INPUTS + OUTPUTS
    @Input() contextNodeId: string;
    editMode: InputSignal<boolean> = input<boolean>(false);
    @Input() embedConfigurationOption?: ConfigurationOption;
    @Input() gridIndex: number = -1;
    @Input() pageVariantNode?: Node;
    @Input() swimlaneIndex: number = -1;

    @Output() configChanged: EventEmitter<void> = new EventEmitter<void>();
    @Output() embedWidgetClicked: EventEmitter<void> = new EventEmitter<void>();

    // VARIABLES
    iframeForm: FormGroup<IframeConfigForm> = new FormGroup<IframeConfigForm>({
        src: new FormControl<string>(''),
        width: new FormControl<number | string>('100%'),
        height: new FormControl<number | string>('450px'),
        title: new FormControl<string>(''),
        border: new FormControl<boolean>(false),
        confirmation: new FormControl<boolean>(true),
    });
    iframeConfig: IframeWidgetConfig | null = null;
    // the saved config; the form starts from it, or from its own defaults for a new widget
    private readonly savedConfig: WritableSignal<IframeWidgetConfig> = signal(
        this.iframeForm.getRawValue(),
    );
    private readonly formValue: Signal<Partial<IframeWidgetConfig>> = toSignal(
        this.iframeForm.valueChanges,
        { initialValue: this.iframeForm.value },
    );
    /** Whether the form holds changes that are not live yet, as only saving publishes them. */
    readonly hasUnsavedChanges: Signal<boolean> = computed(() =>
        this.differsFromForm(this.savedConfig()),
    );
    initialized: WritableSignal<boolean> = signal(false);
    // the config the preview shows; it only follows the form on request, never while typing
    previewConfig: WritableSignal<IframeWidgetConfig | null> = signal(null);
    /** Whether a shown preview lags behind the form. */
    readonly previewOutdated: Signal<boolean> = computed(
        () => !!this.previewConfig() && this.differsFromForm(this.previewConfig()),
    );
    statusConfirmed: WritableSignal<boolean> = signal(false);
    updateInProgress: WritableSignal<boolean> = signal(false);

    constructor() {
        effect(() => {
            // register signal as a dependency to reset the confirmed status
            this.editMode();
            this.statusConfirmed.set(false);
        });
    }

    /**
     * Saves the config by emitting a config changed event.
     */
    saveConfig(): void {
        this.configChanged.emit();
    }

    /**
     * Shows a preview of the current form state.
     */
    showPreview(): void {
        this.previewConfig.set(this.iframeForm.getRawValue());
        this.statusConfirmed.set(false);
    }

    /**
     * Whether any field of the form differs from the given config, treating unset fields as empty.
     */
    private differsFromForm(config: Partial<IframeWidgetConfig>): boolean {
        const formValue: Partial<IframeWidgetConfig> = this.formValue();
        return (Object.keys(this.iframeForm.controls) as (keyof IframeWidgetConfig)[]).some(
            (key) => (formValue[key] ?? '') !== (config[key] ?? ''),
        );
    }

    /**
     * Handles the embedding of the widget by emitting an embed widget clicked event.
     */
    embedWidget(): void {
        this.embedWidgetClicked.emit();
    }

    // noinspection JSUnusedGlobalSymbols
    /**
     * Called by generic-widget component to retrieve a widget config from the currently set variables in the component.
     */
    retrieveWidgetConfig(): IframeWidgetConfig {
        return {
            src: this.iframeForm.value.src,
            width: this.iframeForm.value.width,
            height: this.iframeForm.value.height,
            border: this.iframeForm.value.border,
            confirmation: this.iframeForm.value.confirmation,
            title: this.iframeForm.value.title,
        };
    }

    // noinspection JSUnusedGlobalSymbols
    /**
     * Called by generic-widget component to set widget-specific values.
     */
    setWidgetValues(config: IframeWidgetConfig): void {
        this.iframeForm.patchValue(config);
        this.iframeConfig = this.iframeForm.value;
        this.savedConfig.set(this.iframeForm.getRawValue());
    }
}
