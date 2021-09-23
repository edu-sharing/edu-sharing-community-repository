import {OPTIONS_HELPER_CONFIG, OptionsHelperService} from '../../../options-helper.service';
import {
    AfterViewInit,
    Component,
    ContentChild, ElementRef, Input,
    OnChanges,
    SimpleChanges,
    TemplateRef, ViewChild
} from '@angular/core';
import {NodeEntriesService} from '../../../node-entries.service';
import {Node} from '../../../../core-module/rest/data-object';
import {SortEvent} from '../../sort-dropdown/sort-dropdown.component';
import {$e} from 'codelyzer/angular/styles/chars';
import {CdkDragDrop, CdkDragEnter, CdkDragExit, moveItemInArray} from '@angular/cdk/drag-drop';
import {NodeEntriesDisplayType} from '../../node-entries-wrapper/node-entries-wrapper.component';
import {DragDropState} from '../../../directives/drag-cursor.directive';
import {Target} from '../../../option-item';

@Component({
    selector: 'app-node-entries-card-grid',
    templateUrl: 'node-entries-card-grid.component.html',
    styleUrls: ['node-entries-card-grid.component.scss'],
})
export class NodeEntriesCardGridComponent<T extends Node> implements OnChanges {
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly Target = Target;
    @ContentChild('title') titleRef: TemplateRef<any>;
    @ViewChild('grid') gridRef: ElementRef;
    @Input() displayType: NodeEntriesDisplayType;

    dragDropState: DragDropState<T> = {
        element: null,
        dropAllowed: false
    };
    constructor(
        public entriesService: NodeEntriesService<T>,
    ) {
    }

    ngOnChanges(changes: SimpleChanges): void {
    }

    changeSort(sort: SortEvent) {
        this.entriesService.sort.active = sort.name;
        this.entriesService.sort.direction = sort.ascending ? 'asc' : 'desc';
        this.entriesService.sortChange.emit(this.entriesService.sort);
    }

    reorder(drag: CdkDragDrop<number>) {
        moveItemInArray(this.entriesService.dataSource.getData(), drag.previousContainer.data, drag.container.data);

    }

    loadData() {
        if (this.entriesService.dataSource.hasMore()) {
            this.entriesService.fetchData.emit({
                offset: this.entriesService.dataSource.getData().length
            });
        }
    }
    getItemsPerRow(): number|undefined {
        if (!this.gridRef?.nativeElement) {
            return undefined;
        }
        return getComputedStyle(this.gridRef.nativeElement).getPropertyValue("grid-template-columns").split(' ').length;
        /*
        const grid: HTMLElement[] = Array.from(this.gridRef?.nativeElement?.children);
        if (grid?.length <= 1) {
            return undefined;
        }
        const baseOffset = grid[0].offsetTop;
        const breakIndex = grid.findIndex((item) => item.offsetTop > baseOffset);
        console.log(breakIndex);
        return (breakIndex === -1 ? grid.length : breakIndex);
        */
    }
    getVisibleNodes(nodes: T[]) {
        if (this.entriesService.gridConfig?.maxCols > 0 && this.getItemsPerRow() !== undefined) {
            const count = this.getItemsPerRow() * this.entriesService.gridConfig.maxCols;
            this.entriesService.dataSource.setDisplayCount(count);
            return nodes.slice(0, this.entriesService.dataSource.getDisplayCount());
        }
        this.entriesService.dataSource.setDisplayCount();
        return nodes;
    }

    getSortColumns() {
        return this.entriesService.sort.columns.filter((c) => this.entriesService.columns.some(
            (c2) => c2.name === c.name)
        );
    }

    dragEnter(drag: CdkDragEnter<T>) {
        const allowed = this.entriesService.dragDrop.dropAllowed?.(drag.container.data, {
            element: [drag.item.data],
            sourceList: this.entriesService.list,
            mode: this.dragDropState.mode
        });
        this.dragDropState.element = drag.container.data;
        this.dragDropState.dropAllowed = allowed;
    }

    drop(drop: CdkDragDrop<T, any>) {
        this.entriesService.dragDrop.dropped(drop.container.data,{
            element: [drop.item.data],
            sourceList: this.entriesService.list,
            mode: this.dragDropState.mode
        });
        this.dragDropState.element = null;
    }

    dragExit(exit: CdkDragExit<T>|any) {
        console.log(exit);
        this.dragDropState.element = null
    }
}
