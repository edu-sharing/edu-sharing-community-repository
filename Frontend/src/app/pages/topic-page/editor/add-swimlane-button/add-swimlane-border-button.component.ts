import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { v4 as uuidv4 } from 'uuid';
import { SharedModule } from '../../../../shared/shared.module';
import {
    DEFAULT_ICON_PATH,
    SWIMLANE_GRID_OPTIONS,
    SWIMLANE_TYPE_OPTIONS,
} from '../../shared/types/custom-definitions';
import { GridTile } from '../../shared/types/grid-tile';
import { SelectOption } from '../../shared/types/select-option';
import { Swimlane } from '../../shared/types/swimlane';

@Component({
    imports: [SharedModule],
    selector: 'es-add-swimlane-border-button',
    templateUrl: './add-swimlane-border-button.component.html',
    styleUrls: ['./add-swimlane-border-button.component.scss'],
})
export class AddSwimlaneBorderButtonComponent {
    @Input() requestInProgress: boolean = false;
    @Output() addSwimlaneTriggered: EventEmitter<Swimlane> = new EventEmitter<Swimlane>();
    supportedSwimlaneTypes: string[] = SWIMLANE_TYPE_OPTIONS.map(
        (type: SelectOption) => type.value,
    );

    constructor(private translate: TranslateService) {}

    /**
     * Configures the swimlane and grid by emitting a newly created swimlane.
     *
     * @param swimlaneType
     * @param gridType
     */
    configureSwimlaneAndGrid(swimlaneType: string, gridType: string): void {
        if (!this.supportedSwimlaneTypes.includes(swimlaneType)) {
            return;
        }
        const newSwimlane: Swimlane = {
            id: uuidv4(),
            heading: this.translate.instant('TOPIC_PAGE.SWIMLANE.DEFAULT_HEADING'),
            type: swimlaneType,
            grid: [],
        };
        // add tiles depending on the grid option value
        // note: we use a 6 column grid to display all possible options
        switch (gridType) {
            case 'one_column':
                newSwimlane.grid = [new GridTile(6, 1)];
                break;
            case 'two_columns':
                newSwimlane.grid = [new GridTile(3, 1), new GridTile(3, 1)];
                break;
            case 'three_columns':
                newSwimlane.grid = [new GridTile(2, 1), new GridTile(2, 1), new GridTile(2, 1)];
                break;
            case 'left_side_panel':
                newSwimlane.grid = [new GridTile(2, 1), new GridTile(4, 1)];
                break;
            case 'right_side_panel':
                newSwimlane.grid = [new GridTile(4, 1), new GridTile(2, 1)];
                break;
            default:
                newSwimlane.grid = [new GridTile(6, 1)];
        }
        this.addSwimlaneTriggered.emit(newSwimlane);
    }

    protected readonly iconPath: string = DEFAULT_ICON_PATH + 'editor/';
    protected readonly swimlaneGridOptions: SelectOption[] = SWIMLANE_GRID_OPTIONS;
}
