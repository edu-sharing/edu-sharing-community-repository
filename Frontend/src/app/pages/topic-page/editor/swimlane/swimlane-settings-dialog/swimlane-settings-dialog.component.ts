import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { Component, Input, OnInit, inject } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';
import { Closable } from '../../../../../features/dialogs/card-dialog/card-dialog-config';
import { YES_OR_NO } from '../../../../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../../../../../features/dialogs/dialogs.service';
import { SharedModule } from '../../../../../shared/shared.module';
import {
    SWIMLANE_TYPE_OPTIONS,
    WIDGET_TYPE_OPTIONS,
} from '../../../shared/types/custom-definitions';
import { GridTile } from '../../../shared/types/grid-tile';
import { SelectOption } from '../../../shared/types/select-option';
import { ConfigureGridComponent } from '../configure-grid/configure-grid.component';
import { SelectWidgetTypeComponent } from '../select-widget-type/select-widget-type.component';

@Component({
    selector: 'es-swimlane-settings-dialog',
    imports: [ConfigureGridComponent, DragDropModule, SelectWidgetTypeComponent, SharedModule],
    templateUrl: './swimlane-settings-dialog.component.html',
    styleUrls: ['./swimlane-settings-dialog.component.scss'],
})
export class SwimlaneSettingsDialogComponent implements OnInit {
    private dialogs = inject(DialogsService);

    gridItems: GridTile[];
    widgetTypeToTextMap: Map<string, string> = new Map<string, string>();

    @Input() form: UntypedFormGroup;

    ngOnInit(): void {
        // created a parsed copy of the grid items to work with in the view
        this.gridItems = JSON.parse(this.form.get('grid').value ?? '[]');
        this.syncGridItemsWithFormData();
        // store matching between a widget type and its text
        WIDGET_TYPE_OPTIONS.forEach((option: SelectOption): void => {
            if (!this.widgetTypeToTextMap.get(option.value)) {
                this.widgetTypeToTextMap.set(option.value, option.viewValue);
            }
        });
    }

    /**
     * Handles the drop event by moving a grid item from one to another position.
     *
     * @param event
     */
    drop(event: CdkDragDrop<string[]>): void {
        moveItemInArray(this.gridItems, event.previousIndex, event.currentIndex);
        this.syncGridItemsWithFormData();
    }

    /**
     * Called by es-configure-grid and es-select-widget-type-dialog gridUpdated output event.
     * Updates the grid items and sync them with the form data.
     *
     * @param grid
     */
    updatedGrid(grid: GridTile[]): void {
        this.gridItems = grid;
        this.syncGridItemsWithFormData();
    }

    /**
     * Removes a grid tile at a given index.
     *
     * @tileIndex
     */
    async removeGridTile(tileIndex: number): Promise<void> {
        const gridTile: GridTile = this.gridItems[tileIndex];
        const removeGridTile = (): void => {
            this.gridItems.splice(tileIndex, 1);
            // adjust grid accordingly
            // possible cases:
            // * 3 -> 2 (cols: 3)
            // * 2 -> 1 (cols: 6)
            this.gridItems?.forEach((tile: GridTile): void => {
                if (this.gridItems.length === 2) {
                    tile.cols = 3;
                } else {
                    tile.cols = 6;
                }
            });
            this.syncGridItemsWithFormData();
        };
        if (!!gridTile.nodeId) {
            const dialogRef = await this.dialogs.openGenericDialog({
                title: 'TOPIC_PAGE.SWIMLANE.SELECT_WIDGET.DELETE',
                message: 'TOPIC_PAGE.SWIMLANE.SELECT_WIDGET.DELETE_WARNING',
                buttons: YES_OR_NO,
                closable: Closable.Casual,
            });
            dialogRef.afterClosed().subscribe(async (response) => {
                if (response === 'YES') {
                    removeGridTile();
                }
            });
        } else {
            removeGridTile();
        }
    }

    /**
     * Helper function to sync the grid items with the grid form data.
     */
    private syncGridItemsWithFormData(): void {
        this.form.get('grid').setValue(JSON.stringify(this.gridItems));
    }

    protected readonly swimlaneTypeOptions: SelectOption[] = SWIMLANE_TYPE_OPTIONS;
}
