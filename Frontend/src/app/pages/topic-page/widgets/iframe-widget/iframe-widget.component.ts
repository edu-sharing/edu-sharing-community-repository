import {
    Component,
    CUSTOM_ELEMENTS_SCHEMA,
    effect,
    EventEmitter,
    input,
    Input,
    InputSignal,
    Output,
    signal,
    ViewEncapsulation,
    WritableSignal,
} from '@angular/core';
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
        confirmation: new FormControl<boolean>(false),
    });
    iframeConfig: IframeWidgetConfig | null = null;
    initialized: WritableSignal<boolean> = signal(false);
    previewConfig: IframeWidgetConfig | null = null;
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
        this.previewConfig = this.iframeForm.value;
        this.statusConfirmed.set(false);
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
     *
     * @param config
     */
    setWidgetValues(config: IframeWidgetConfig): void {
        this.iframeForm.patchValue(config);
        this.iframeConfig = this.iframeForm.value;
    }
}
