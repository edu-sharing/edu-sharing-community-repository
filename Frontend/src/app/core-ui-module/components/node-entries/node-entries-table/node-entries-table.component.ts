import { CdkDragDrop, CdkDragExit, CdkDropList } from '@angular/cdk/drag-drop';
import { CdkDrag } from '@angular/cdk/drag-drop/directives/drag';
import { CdkOverlayOrigin } from '@angular/cdk/overlay';
import {
    AfterViewInit, ApplicationRef, Component, OnChanges,
    SimpleChanges, ViewChild
} from '@angular/core';
import { MatMenuTrigger } from '@angular/material/menu';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort, MatSortHeader, Sort } from '@angular/material/sort';
import { BehaviorSubject, Observable } from 'rxjs';
import { Toast } from 'src/app/core-ui-module/toast';
import { Node } from '../../../../core-module/rest/data-object';
import { ListItem } from '../../../../core-module/ui/list-item';
import { DragCursorDirective } from '../../../directives/drag-cursor.directive';
import { NodeEntriesService } from '../../../node-entries.service';
import { Target } from '../../../option-item';
import { DropdownComponent } from '../../../../shared/components/dropdown/dropdown.component';
import { ClickSource, InteractionType } from '../../node-entries-wrapper/entries-model';
import {UIService} from '../../../../core-module/rest/services/ui.service';
import {NodeEntriesDataType} from '../node-entries.component';

@Component({
    selector: 'es-node-entries-table',
    templateUrl: './node-entries-table.component.html',
    styleUrls: ['./node-entries-table.component.scss'],
})
export class NodeEntriesTableComponent<T extends NodeEntriesDataType> implements OnChanges, AfterViewInit {
    readonly InteractionType = InteractionType;
    readonly ClickSource = ClickSource;
    readonly Target = Target;

    @ViewChild(MatSort) sort: MatSort;
    @ViewChild(MatPaginator) paginator: MatPaginator;
    @ViewChild('columnChooserTrigger') columnChooserTrigger: CdkOverlayOrigin;
    @ViewChild(DropdownComponent) dropdown: DropdownComponent;
    @ViewChild('menuTrigger') menuTrigger: MatMenuTrigger;

    dropdownLeft: number;
    dropdownTop: number;

    loading: Observable<boolean>;
    isPageSelected = new BehaviorSubject(false);
    isAllSelected = new BehaviorSubject(false);
    columnChooserVisible = false;
    ready = false;
    error: Observable<any>;
    pageSizeOptions = [25, 50, 100];
    dragSource: T;

    constructor(
                public entriesService: NodeEntriesService<T>,
                private applicationRef: ApplicationRef,
                private toast: Toast,
                public ui: UIService,
    ) {
    }

    ngAfterViewInit(): void {
        Promise.resolve().then(() => {
            this.ready = true;
            this.registerSortChanges();
        });
    }

    ngOnChanges(changes: SimpleChanges): void {
        this.updateSort();
    }


    onRowContextMenu({ event, node }: { event: MouseEvent | Event; node: T }) {
        if (!this.entriesService.selection.selected.includes(node)) {
            this.entriesService.selection.clear();
            this.entriesService.selection.select(node)
        }
        event.stopPropagation();
        event.preventDefault();
        if (event instanceof MouseEvent) {
            ({ clientX: this.dropdownLeft, clientY: this.dropdownTop } = event);
        } else {
            ({ x: this.dropdownLeft, y: this.dropdownTop } = (
                event.target as HTMLElement
            ).getBoundingClientRect());
        }
        // Wait for the menu to reflect changed options.
        setTimeout(() => {
            if (this.dropdown.canShowDropdown()) {
                this.menuTrigger.openMenu();
            } else {
                this.toast.toast('NO_AVAILABLE_OPTIONS');
            }
        });
    }

    private updateSort(): void {
        this.sort.sort({
            id: this.entriesService.sort?.active,
            start: (this.entriesService.sort?.direction as 'asc'|'desc'),
            disableClear: false
        });
        // Fix missing sorting indicators. See
        // https://github.com/angular/components/issues/10242#issuecomment-470726829. Seems
        // to be fixed upstream with Angular 11.
        (
            this.sort.sortables.get(this.entriesService.sort?.active) as MatSortHeader
        )._setAnimationTransitionState({
            toState: 'active',
        });
        /*
        this.route.queryParams.pipe(first()).subscribe((queryParams: Params) => {
            const sort: Sort = queryParams.sort ? JSON.parse(queryParams.sort) : null;
            if (sort && sort.direction) {
                this.sort.sort({ id: sort.active, start: sort.direction, disableClear: false });
                // Fix missing sorting indicators. See
                // https://github.com/angular/components/issues/10242#issuecomment-470726829. Seems
                // to be fixed upstream with Angular 11.
                (
                    this.sort.sortables.get(sort.active) as MatSortHeader
                )._setAnimationTransitionState({
                    toState: 'active',
                });
            }
            if (queryParams.pageIndex && queryParams.pageSize) {
                this.paginator.pageIndex = queryParams.pageIndex;
                this.paginator.pageSize = queryParams.pageSize;
            }
        });
         */
    }
    getVisibleColumns() {
        const columns = [];
        if(this.entriesService.checkbox) {
            columns.push('select');
        }
        columns.push('icon');
        return columns.concat(
            this.entriesService.columns.filter((c) => c.visible).map((c) => c.name)
        ).concat(
            ['actions']
        );
    }

    isSortable(column: ListItem) {
        return this.entriesService.sort?.columns.some((c) => c.name === column.name);
    }

    toggleAll(checked: boolean) {
        if(checked) {
            this.entriesService.selection.select(...this.entriesService.dataSource.getData());
        } else {
            this.entriesService.selection.clear();
        }

    }

    private registerSortChanges() {
        this.sort.sortChange.subscribe((sort: Sort) => {
            this.entriesService.sort.active = sort.active;
            this.entriesService.sort.direction = sort.direction;
            this.entriesService.sortChange.emit(this.entriesService.sort);
            /*this.router.navigate(['.'], {
                relativeTo: this.route,
                queryParams: { sort: JSON.stringify(sort) },
                queryParamsHandling: 'merge',
                replaceUrl: true,
            });*/
        });
        /*
        this.paginator.page
            .pipe(
                // As a response to changes of other parameters, the pageIndex might be reset to 0 and a
                // page event triggers. This change of other parameters is likely to cause a
                // `router.navigate()` call elsewhere. When this happens just before our call, our
                // updates are ignored. To shield against this, we wait a tick.
                delay(0),
            )
            .subscribe(({ pageIndex, pageSize }: PageEvent) => {
                this.router.navigate(['.'], {
                    relativeTo: this.route,
                    queryParams: { pageIndex, pageSize },
                    queryParamsHandling: 'merge',
                    replaceUrl: true,
                });
            });
         */
    }

    dragEnter = (index: number, drag: CdkDrag, drop: CdkDropList) => {
        const target = this.entriesService.dataSource.getData()[index];
        const allowed = this.entriesService.dragDrop.dropAllowed?.(target as Node, {
            element: [this.dragSource],
            sourceList: this.entriesService.list,
            mode: DragCursorDirective.dragState.mode
        });
        DragCursorDirective.dragState.element = target;
        DragCursorDirective.dragState.dropAllowed = allowed;
        return false;
    }

    drop(drop: CdkDragDrop<T, any>) {
        this.entriesService.dragDrop.dropped(DragCursorDirective.dragState.element,{
            element: [this.dragSource],
            sourceList: this.entriesService.list,
            mode: DragCursorDirective.dragState.mode
        });
        DragCursorDirective.dragState.element = null;
    }

    dragExit(exit: CdkDragExit<T>|any) {
        DragCursorDirective.dragState.element = null
    }

    loadData() {
        if (this.entriesService.dataSource.hasMore()) {
            this.entriesService.fetchData.emit({
                offset: this.entriesService.dataSource.getData().length
            });
        }
    }

    getDragState() {
        return DragCursorDirective.dragState;
    }

    async openMenu(node: T) {
        this.entriesService.selection.clear();
        this.entriesService.selection.select(node);
        await this.applicationRef.tick();
        this.dropdown.menu.focusFirstItem();
    }
}
