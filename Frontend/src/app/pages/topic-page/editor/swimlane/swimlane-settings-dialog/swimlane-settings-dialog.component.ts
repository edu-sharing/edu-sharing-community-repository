import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';
import { MatSelect } from '@angular/material/select';
import { Closable } from '../../../../../features/dialogs/card-dialog/card-dialog-config';
import { YES_OR_NO } from '../../../../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../../../../../features/dialogs/dialogs.service';
import { SharedModule } from '../../../../../shared/shared.module';
import {
    DEFAULT_COLLECTION_ID_PROP,
    SWIMLANE_TYPE_OPTIONS,
    WIDGET_TYPE_OPTIONS,
    WIDGETS,
} from '../../../shared/types/custom-definitions';
import { SwimlaneRepeatService } from '../../../shared/services/swimlane-repeat.service';
import { GridTile } from '../../../shared/types/grid-tile';
import { SelectOption } from '../../../shared/types/select-option';
import { REPEAT_PLACEHOLDERS } from '../../../shared/types/swimlane-repeat';
import { PatchCheckResult, checkConfigPatch } from '../../../shared/utils/repeat-validation-util';
import { ConfigureGridComponent } from '../configure-grid/configure-grid.component';
import { SelectWidgetTypeComponent } from '../select-widget-type/select-widget-type.component';

/**
 * Formats an example config patch the way it appears in the field.
 *
 * @param patch
 */
function patchExample(patch: Record<string, unknown>): string {
    return JSON.stringify(patch, null, 2);
}

@Component({
    selector: 'es-swimlane-settings-dialog',
    imports: [ConfigureGridComponent, DragDropModule, SelectWidgetTypeComponent, SharedModule],
    templateUrl: './swimlane-settings-dialog.component.html',
    styleUrls: ['./swimlane-settings-dialog.component.scss'],
})
export class SwimlaneSettingsDialogComponent implements OnInit {
    private dialogs = inject(DialogsService);
    private swimlaneRepeatService = inject(SwimlaneRepeatService);

    protected readonly i18nPrefix: string = 'TOPIC_PAGE.SWIMLANE.EDIT.ADVANCED.';

    gridItems: GridTile[];
    widgetTypeToTextMap: Map<string, string> = new Map<string, string>();

    @Input() form: UntypedFormGroup;
    /**
     * Whether the dynamic sections may be defined here. The rules themselves keep working
     * for everyone; only authoring them is held back while the feature is being tried out.
     */
    @Input() advancedEnabled: boolean = false;

    /** the config patch of each tile as the author edits it, before it is parsed */
    protected readonly patchTexts: string[] = [];
    /** i18n key and params of the problem of each tile's patch, if it has one */
    protected readonly patchErrors = signal<PatchCheckResult[]>([]);
    protected readonly repeatSources: string[] = this.swimlaneRepeatService.getSourceNames();
    protected readonly placeholders: string = REPEAT_PLACEHOLDERS.map((p) => '${' + p + '}').join(
        ', ',
    );
    /** how many advanced options carry a value, shown on the collapsed panel */
    protected readonly advancedCount = signal<number>(0);
    /** the patch a content teaser gets by default, which is what it is repeated for */
    private readonly collectionFilterExample: string = patchExample({
        propertyFilters: { [DEFAULT_COLLECTION_ID_PROP]: ['${item.nodeId}'] },
    });

    ngOnInit(): void {
        // created a parsed copy of the grid items to work with in the view
        this.gridItems = JSON.parse(this.form.get('grid').value ?? '[]');
        this.syncPatchTexts();
        this.syncGridItemsWithFormData();
        this.form.valueChanges.subscribe((): void => this.updateAdvancedCount());
        this.form.get('repeatSource').valueChanges.subscribe((source: string): void => {
            if (source) {
                this.prefillCollectionFilters();
            }
        });
        this.updateAdvancedCount();
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
        this.syncPatchTexts();
        this.syncGridItemsWithFormData();
    }

    /**
     * Validates the config patch an author typed for a tile and, when it is sound, stores it.
     * An unsound patch marks the form invalid instead of being written, so it cannot be applied.
     *
     * @param tileIndex
     * @param text
     */
    protected patchChanged(tileIndex: number, text: string): void {
        this.patchTexts[tileIndex] = text;
        const result: PatchCheckResult = checkConfigPatch(text);
        const errors: PatchCheckResult[] = [...this.patchErrors()];
        errors[tileIndex] = result;
        this.patchErrors.set(errors);
        if (!result.errorKey) {
            if (result.value) {
                this.gridItems[tileIndex].configPatch = result.value;
            } else {
                delete this.gridItems[tileIndex].configPatch;
            }
            this.syncGridItemsWithFormData();
        }
        this.form.get('advancedValid').setValue(!errors.some((e) => !!e?.errorKey));
        this.updateAdvancedCount();
    }

    /**
     * Ready-made config patches for a widget type, so the field does not have to be understood
     * from scratch. Only a content teaser is bound to the repeated item itself; for the others
     * the texts are what differs per section.
     *
     * @param widgetType
     */
    protected examplesFor(widgetType: string): { key: string; json: string }[] {
        const texts = [
            { key: 'HEADLINE', json: patchExample({ headline: 'Neu in ${item.title}' }) },
            { key: 'DESCRIPTION', json: patchExample({ description: '${item.description}' }) },
        ];
        if (widgetType !== WIDGETS.CONTENT_TEASER) {
            return texts;
        }
        return [
            { key: 'COLLECTION_FILTER', json: this.collectionFilterExample },
            {
                key: 'COLLECTION_FILTER_NEWEST',
                json: patchExample({
                    propertyFilters: { [DEFAULT_COLLECTION_ID_PROP]: ['${item.nodeId}'] },
                    sort: { active: 'cm:modified', direction: 'desc' },
                }),
            },
            ...texts,
            { key: 'SEARCH_TEXT', json: patchExample({ searchText: '${item.title}' }) },
        ];
    }

    /**
     * Gives every content teaser that has no patch yet the collection filter, which is what a
     * section per collection is for. Tiles an author already configured are left alone.
     */
    private prefillCollectionFilters(): void {
        this.gridItems?.forEach((gridTile: GridTile, index: number): void => {
            if (gridTile.item === WIDGETS.CONTENT_TEASER && !this.patchTexts[index]?.trim()) {
                this.patchChanged(index, this.collectionFilterExample);
            }
        });
    }

    /**
     * Writes an example into the config patch of a tile, replacing what is there. The select it
     * was picked from is a menu rather than a state, so it does not keep the choice.
     *
     * @param tileIndex
     * @param json
     * @param select
     */
    protected applyExample(tileIndex: number, json: string, select?: MatSelect): void {
        this.patchChanged(tileIndex, json);
        if (select) {
            select.value = null;
        }
    }

    /**
     * Reads the config patches of the grid items into their editable text form.
     */
    private syncPatchTexts(): void {
        this.patchTexts.length = 0;
        this.gridItems?.forEach((gridTile: GridTile, index: number): void => {
            this.patchTexts[index] = gridTile.configPatch
                ? JSON.stringify(gridTile.configPatch, null, 2)
                : '';
        });
        this.patchErrors.set([]);
    }

    /**
     * Counts the advanced options that carry a value, so a collapsed panel still says whether
     * anything is configured behind it.
     */
    private updateAdvancedCount(): void {
        const values = this.form.value;
        const repeatValues: unknown[] = [
            values.repeatSource,
            values.repeatOrderBy,
            values.repeatOrderDescending || null,
            values.repeatMaxItems,
        ];
        this.advancedCount.set(
            repeatValues.filter((value) => value !== null && value !== undefined && value !== '')
                .length + this.patchTexts.filter((text: string) => !!text?.trim()).length,
        );
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
