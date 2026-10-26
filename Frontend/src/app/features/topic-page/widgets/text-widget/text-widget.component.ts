import {
    Component,
    CUSTOM_ELEMENTS_SCHEMA,
    EventEmitter,
    input,
    Input,
    InputSignal,
    Output,
    signal,
    ViewEncapsulation,
    WritableSignal,
} from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { SpinnerComponent } from 'ngx-edu-sharing-ui';
import { ConfigurationOption } from '../../shared/types/configuration-option';
import { WidgetComponentInterface } from '../generic-widget/generic-widget.component';
import { WidgetConfigurationButtonsComponent } from '../shared/widget-configuration-buttons/widget-configuration-buttons.component';

@Component({
    selector: 'es-text-widget',
    encapsulation: ViewEncapsulation.Emulated,
    imports: [SpinnerComponent, TranslateModule, WidgetConfigurationButtonsComponent],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    templateUrl: './text-widget.component.html',
    styleUrls: ['./text-widget.component.scss'],
})
export class TextWidgetComponent implements WidgetComponentInterface {
    // INPUTS
    @Input() contextNodeId: string;
    editMode: InputSignal<boolean> = input<boolean>(false);
    @Input() embedConfigurationOption?: ConfigurationOption;
    @Input() gridIndex: number = -1;
    @Input() pageVariantNode?: Node;
    @Input() swimlaneIndex: number = -1;

    @Output() embedWidgetClicked: EventEmitter<void> = new EventEmitter<void>();
    @Output() configChanged: EventEmitter<void> = new EventEmitter<void>();

    // VARIABLES
    initialized: WritableSignal<boolean> = signal(false);
    updateInProgress: WritableSignal<boolean> = signal(false);

    /**
     * Handles the embedding of the widget by emitting an embed widget clicked event.
     */
    embedWidget(): void {
        this.embedWidgetClicked.emit();
    }
}
