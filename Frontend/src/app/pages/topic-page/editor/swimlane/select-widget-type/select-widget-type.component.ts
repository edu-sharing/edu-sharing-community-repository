import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { EduSharingUiCommonModule, Helper } from 'ngx-edu-sharing-ui';
import { firstValueFrom } from 'rxjs';
import { Closable } from '../../../../../features/dialogs/card-dialog/card-dialog-config';
import { YES_OR_NO } from '../../../../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../../../../../features/dialogs/dialogs.service';
import { SharedModule } from '../../../../../shared/shared.module';
import { AiHelperService } from '../../../shared/services/ai-helper.service';
import {
    WIDGET_TYPE,
    WIDGET_TYPE_OPTIONS,
    WIDGETS,
} from '../../../shared/types/custom-definitions';
import { GridTile } from '../../../shared/types/grid-tile';
import { WidgetSelectOption } from '../../../shared/types/widget-select-option';

@Component({
    selector: 'es-select-widget-type',
    imports: [EduSharingUiCommonModule, SharedModule],
    templateUrl: './select-widget-type.component.html',
    styleUrls: ['./select-widget-type.component.scss'],
})
export class SelectWidgetTypeComponent implements OnInit {
    private aiHelperService = inject(AiHelperService);
    private dialogs = inject(DialogsService);

    @Input() grid: GridTile[] = [];
    @Input() tileIndex: number;
    @Output() gridUpdated: EventEmitter<GridTile[]> = new EventEmitter<GridTile[]>();

    widgetTypeOptions: WidgetSelectOption[] = WIDGET_TYPE_OPTIONS;

    async ngOnInit(): Promise<void> {
        const [hasAI, hasRendering2] = await Promise.all([
            this.aiHelperService.hasAISupport(),
            this.aiHelperService.hasRendering2Support(),
        ]);

        this.widgetTypeOptions = WIDGET_TYPE_OPTIONS.filter(
            ({ value }) =>
                (hasAI || value !== WIDGETS.AI_TEXT_WIDGET) &&
                (hasRendering2 || value !== WIDGETS.MEDIA_RENDERING),
        );
    }

    /**
     * Selects the widget type for a grid tile at a given index.
     *
     * @param widgetType
     * @param tileIndex
     */
    async selectWidgetType(widgetType: WIDGET_TYPE, tileIndex: number): Promise<void> {
        // change copy of the grid to avoid overwriting input
        const gridCopy: GridTile[] = Helper.deepCopy(this.grid);
        const gridTile: GridTile = gridCopy[tileIndex];
        const changeType = (): void => {
            if (!!gridTile.nodeId) {
                delete gridTile.nodeId;
            }
            gridTile.item = widgetType;
        };
        // nodeId does already exist, confirm overwrite
        if (!!gridTile.nodeId) {
            const dialogRef = await this.dialogs.openGenericDialog({
                title: 'TOPIC_PAGE.SWIMLANE.SELECT_WIDGET.CHANGE_TYPE',
                message: 'TOPIC_PAGE.SWIMLANE.SELECT_WIDGET.CHANGE_TYPE_WARNING',
                buttons: YES_OR_NO,
                closable: Closable.Casual,
            });
            const response = await firstValueFrom(dialogRef.afterClosed());
            if (response === 'YES') {
                changeType();
            }
        } else {
            changeType();
        }
        this.gridUpdated.emit(gridCopy);
    }
}
