import { CdkDragDrop, CdkDragEnter, CdkDragExit, moveItemInArray } from '@angular/cdk/drag-drop';
import {
    Component,
    ElementRef,
    Input,
    OnChanges,
    QueryList,
    SimpleChanges,
    ViewChild,
    ViewChildren,
} from '@angular/core';
import * as rxjs from 'rxjs';
import { BehaviorSubject } from 'rxjs';
import { distinctUntilChanged, map, switchMap, take } from 'rxjs/operators';
import { UIService, ListItemSort, RestConstants } from '../../../core-module/core.module';
import { SortEvent } from '../../../shared/components/sort-dropdown/sort-dropdown.component';
import { DragCursorDirective } from '../../../core-ui-module/directives/drag-cursor.directive';
import { NodeEntriesService } from '../../../core-ui-module/node-entries.service';
import { Target } from '../../../core-ui-module/option-item';
import { NodeEntriesDisplayType } from '../entries-model';

import { NodeEntriesTemplatesService } from '../node-entries-templates.service';
import { Node } from 'ngx-edu-sharing-api'
import {NodeEntriesGlobalService, PaginationStrategy} from "../node-entries-global.service";


@Component({
    selector: 'es-node-entries-card-grid',
    templateUrl: 'node-entries-card-grid.component.html',
    styleUrls: ['node-entries-card-grid.component.scss'],
})
export class NodeEntriesCardGridComponent<T extends Node> implements OnChanges {
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly PaginationStrategy = PaginationStrategy;
    readonly Target = Target;
    @ViewChild('grid') gridRef: ElementRef;
    @ViewChildren('item', { read: ElementRef }) itemRefs: QueryList<ElementRef<HTMLElement>>;
    @Input() displayType: NodeEntriesDisplayType;

    private readonly nodes$ = this.entriesService.dataSource$.pipe(
        switchMap((dataSource) => dataSource?.connect()),
    );
    private readonly maxRows$ = this.entriesService.gridConfig$.pipe(
        map((gridConfig) => gridConfig?.maxRows || null),
        distinctUntilChanged(),
    );
    private readonly itemsPerRowSubject = new BehaviorSubject<number | null>(null);
    readonly visibleNodes$ = rxjs
        .combineLatest([
            this.nodes$,
            this.itemsPerRowSubject.pipe(distinctUntilChanged()),
            this.maxRows$,
        ])
        .pipe(
            map(([nodes, itemsPerRow, maxRows]) =>
                this.getVisibleNodes(nodes, itemsPerRow, maxRows),
            ),
        );

    constructor(
        public entriesService: NodeEntriesService<T>,
        public entriesGlobalService: NodeEntriesGlobalService,
        public templatesService: NodeEntriesTemplatesService,
        public ui: UIService,
    ) {}

    ngOnChanges(changes: SimpleChanges): void {}

    changeSort(sort: SortEvent) {
        this.entriesService.sort.active = sort.name;
        this.entriesService.sort.direction = sort.ascending ? 'asc' : 'desc';
        this.entriesService.sortChange.emit(this.entriesService.sort);
    }

    reorder(drag: CdkDragDrop<number>) {
        moveItemInArray(
            this.entriesService.dataSource.getData(),
            drag.previousContainer.data,
            drag.container.data,
        );
    }

    loadData(byButtonClick = false) {
        // @TODO: Maybe this is better handled in a more centraled service
        if (!byButtonClick) {
            // check if there is a footer
            const elements = document.getElementsByTagName('footer');
            if (elements.length && elements.item(0).innerHTML.trim()) {
                return;
            }
        }
        if (this.entriesService.dataSource.hasMore()) {
            this.entriesService.fetchData.emit({
                offset: this.entriesService.dataSource.getData().length,
                reset: false
            });
        }
        if (byButtonClick) {
            this.focusFirstNewItemWhenLoaded();
        }
    }

    private focusFirstNewItemWhenLoaded() {
        const oldLength = this.itemRefs.length;
        this.itemRefs.changes
            .pipe(take(1))
            .subscribe((items: NodeEntriesCardGridComponent<T>['itemRefs']) => {
                if (items.length > oldLength) {
                    this.focusOnce(items.get(oldLength).nativeElement);
                }
            });
    }

    private focusOnce(element: HTMLElement): void {
        element.setAttribute('tabindex', '-1');
        element.focus();
        element.addEventListener('blur', () => element.removeAttribute('tabindex'), { once: true });
    }

    onGridSizeChanges(): void {
        const itemsPerRow = this.getItemsPerRow();
        this.itemsPerRowSubject.next(itemsPerRow);
    }

    private getItemsPerRow(): number | null {
        if (!this.gridRef?.nativeElement) {
            return null;
        }
        return getComputedStyle(this.gridRef.nativeElement)
            .getPropertyValue('grid-template-columns')
            .split(' ').length;
    }

    private getVisibleNodes(nodes: T[], itemsPerRow: number | null, maxRows: number | null): T[] {
        if (maxRows > 0 && itemsPerRow !== null) {
            const count = itemsPerRow * maxRows;
            this.entriesService.dataSource.setDisplayCount(count);
            return nodes.slice(0, this.entriesService.dataSource.getDisplayCount());
        } else {
            this.entriesService.dataSource.setDisplayCount();
            return nodes;
        }
    }

    getSortColumns() {
        return this.entriesService.sort?.columns?.filter((c) =>
            this.entriesService.columns
                .concat(
                    new ListItemSort('NODE', 'score'),
                    new ListItemSort('NODE', RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION),
                    new ListItemSort('NODE', RestConstants.CM_PROP_TITLE),
                    new ListItemSort('NODE', RestConstants.CM_NAME),
                    new ListItemSort('NODE', RestConstants.CM_MODIFIED_DATE),
                )
                .some((c2) => c2.name === c.name),
        );
    }

    dragEnter(drag: CdkDragEnter<T>) {
        const allowed = this.entriesService.dragDrop.dropAllowed?.(drag.container.data, {
            element: [drag.item.data],
            sourceList: this.entriesService.list,
            mode: DragCursorDirective.dragState.mode,
        });
        DragCursorDirective.dragState.element = drag.container.data;
        DragCursorDirective.dragState.dropAllowed = allowed;
    }

    drop(drop: CdkDragDrop<T, any>) {
        this.entriesService.dragDrop.dropped(drop.container.data, {
            element: [drop.item.data],
            sourceList: this.entriesService.list,
            mode: DragCursorDirective.dragState.mode,
        });
        DragCursorDirective.dragState.element = null;
    }

    dragExit(exit: CdkDragExit<T> | any) {
        console.log(exit);
        DragCursorDirective.dragState.element = null;
    }

    getDragState() {
        return DragCursorDirective.dragState;
    }
}
