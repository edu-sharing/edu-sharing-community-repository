import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Sort, SortDirection } from '@angular/material/sort';
import { ListItemSort } from '../../../core-module/core.module';
import { SortEvent } from '../../../shared/components/sort-dropdown/sort-dropdown.component';
import { SortPanel } from '../node-data-source-remote';

@Component({
    selector: 'es-sort-select-panel',
    templateUrl: './sort-select-panel.component.html',
    styleUrls: ['./sort-select-panel.component.scss'],
})
export class SortSelectPanelComponent implements SortPanel {
    @Input() active: string;
    @Input() direction: SortDirection;
    @Input() columns: ListItemSort[];
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
