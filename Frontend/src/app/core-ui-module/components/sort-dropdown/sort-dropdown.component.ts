import { trigger } from '@angular/animations';
import { Component, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { MatMenu } from '@angular/material/menu';
import {ListItem, ListItemSort} from '../../../core-module/core.module';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import {ListItemType} from '../../../core-module/ui/list-item';

@Component({
    selector: 'es-sort-dropdown',
    templateUrl: 'sort-dropdown.component.html',
    styleUrls: ['sort-dropdown.component.scss'],
    animations: [trigger('openOverlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST))],
})
export class SortDropdownComponent {
    @ViewChild('menu', { static: true }) menu: MatMenu;

    @Input() columns: ListItem[];
    @Input() sortBy: string;
    @Input() sortAscending: boolean;

    @Output() onSort = new EventEmitter<SortEvent>();

    constructor() {}

    setSort(item: ListItem|any): void {
        let ascending = this.sortAscending;
        const itemAscending = item.mode === 'ascending';
        if (item.name === this.sortBy) {
            if (item.mode != null) {
                // element is limited to one mode, ignore the request
                if (itemAscending === this.sortAscending) {
                    return;
                }
            }
            ascending = !ascending;
        } else if (item.mode != null) {
            // force mode when switching to item
            ascending = itemAscending;
        }
        (item as SortEvent).ascending = ascending;
        this.onSort.emit(item);
    }
}

export class SortEvent extends ListItemSort {
    ascending: boolean;
}

