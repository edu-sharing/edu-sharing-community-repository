import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CdkOverlayOrigin } from '@angular/cdk/overlay';
import { ListItem } from 'src/app/core-module/core.module';

@Component({
    selector: 'es-column-chooser',
    templateUrl: './column-chooser.component.html',
    styleUrls: ['./column-chooser.component.scss'],
})
export class ColumnChooserComponent {
    @Input() origin: CdkOverlayOrigin;
    @Input() columnChooserVisible = false;
    @Output() columnChooserVisibleChange = new EventEmitter<boolean>();

    @Input() columns: ListItem[];
    @Output() columnsChange = new EventEmitter<ListItem[]>();

    constructor() {}

    columnChooserDrop(event: CdkDragDrop<string[]>) {
        moveItemInArray(this.columns, event.previousIndex, event.currentIndex);
        this.columnsChange.emit(this.columns);
    }

    columnChooserToggle(columnIndex: number): void {
        const column = this.columns[columnIndex];
        column.visible = !column.visible;
        this.columnsChange.emit(this.columns);
    }
}
