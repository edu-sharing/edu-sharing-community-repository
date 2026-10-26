import { Component, EventEmitter, input, Input, InputSignal, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatButtonToggle, MatButtonToggleGroup } from '@angular/material/button-toggle';
import { MatTooltip } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';
import { CollectionListDisplayType } from '../../../shared/types/collection-list-display-type';
import { ConfigurationOption } from '../../../shared/types/configuration-option';
import { GenericNodeEntriesDisplayType } from '../../../shared/types/generic-node-entries-display-type';
import { LayoutOption } from '../../../shared/types/layout-option';
import { MediaRenderingDisplayType } from '../../../shared/types/media-rendering-display-type';

@Component({
    selector: 'es-widget-configuration-buttons',
    imports: [
        EduSharingUiCommonModule,
        FormsModule,
        MatButton,
        MatButtonToggle,
        MatButtonToggleGroup,
        MatTooltip,
        TranslateModule,
    ],
    templateUrl: './widget-configuration-buttons.component.html',
    styleUrls: ['./widget-configuration-buttons.component.scss'],
})
export class WidgetConfigurationButtonsComponent {
    @Input() ariaLabel: string = '';
    @Input() layout:
        | CollectionListDisplayType
        | GenericNodeEntriesDisplayType
        | MediaRenderingDisplayType;
    @Input() layoutOptions: LayoutOption[] = [];
    @Input() optionOne?: ConfigurationOption = new ConfigurationOption();
    @Input() optionTwo?: ConfigurationOption = new ConfigurationOption();
    @Input() pageVariantNode: Node;
    @Input() swimlaneIndex: number = -1;
    updateInProgress: InputSignal<boolean> = input(false);
    @Output() layoutChange: EventEmitter<
        CollectionListDisplayType | GenericNodeEntriesDisplayType | MediaRenderingDisplayType
    > = new EventEmitter<
        CollectionListDisplayType | GenericNodeEntriesDisplayType | MediaRenderingDisplayType
    >();
    @Output() optionOneClicked: EventEmitter<boolean> = new EventEmitter<boolean>();
    @Output() optionTwoClicked: EventEmitter<boolean> = new EventEmitter<boolean>();

    /**
     * Emits the layout change event.
     */
    changeLayout(): void {
        this.layoutChange.emit(this.layout);
    }

    /**
     * Emits the option one click.
     */
    clickOptionOne(): void {
        this.optionOneClicked.emit(true);
    }

    /**
     * Emits the option two click.
     */
    clickOptionTwo(): void {
        this.optionTwoClicked.emit(true);
    }
}
