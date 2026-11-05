import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';
import { Sort, SortDirection } from '@angular/material/sort';
import { ListItemSort, SortEvent, SortPanel } from '../../types/list-item';

@Component({
    selector: 'es-sort-select-panel',
    templateUrl: './sort-select-panel.component.html',
    styleUrls: ['./sort-select-panel.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class SortSelectPanelComponent implements SortPanel {
    @Input() active: string;
    @Input() direction: SortDirection;
    @Input() columns: ListItemSort[];
    /** whether the sort criterion can be chosen, in addition to the toggle for manual ordering */
    @Input() showSelection = true;
    /** whether the toggle for manual ordering is offered */
    @Input() showCustomToggle = true;
    @Output() sortChange = new EventEmitter<Sort>();
    @Input() customSortingInProgress: boolean;
    @Output() customSortingInProgressChange = new EventEmitter<boolean>();

    constructor() {}

    onSort(event: SortEvent) {
        this.sortChange.emit({
            active: event.name,
            direction: event.ascending ? 'asc' : 'desc',
        });
    }
}
