import { CdkOverlayOrigin } from '@angular/cdk/overlay';
import {
    AfterViewInit,
    ApplicationRef,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    NgZone,
    OnChanges,
    OnDestroy,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { MatMenuTrigger } from '@angular/material/menu';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort, MatSortHeader, Sort } from '@angular/material/sort';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import {
    delay,
    distinctUntilChanged,
    first,
    map,
    shareReplay,
    startWith,
    takeUntil,
    tap,
} from 'rxjs/operators';
import { Toast } from 'src/app/core-ui-module/toast';
import { ListItem, Node, UIService } from '../../../core-module/core.module';
import { NodeEntriesService } from '../../../core-ui-module/node-entries.service';
import { Target } from '../../../core-ui-module/option-item';
import { DragData } from '../../../services/nodes-drag-drop.service';
import { DropdownComponent } from '../../../shared/components/dropdown/dropdown.component';
import { BorderBoxObserverDirective } from '../../../shared/directives/border-box-observer.directive';
import { CanDrop } from '../../../shared/directives/nodes-drop-target.directive';
import { ClickSource, InteractionType, NodeEntriesDisplayType } from '../entries-model';
import { NodeEntriesDataType } from '../node-entries.component';

@Component({
    selector: 'es-node-entries-table',
    templateUrl: './node-entries-table.component.html',
    styleUrls: ['./node-entries-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NodeEntriesTableComponent<T extends NodeEntriesDataType>
    implements OnChanges, AfterViewInit, OnDestroy
{
    readonly InteractionType = InteractionType;
    readonly ClickSource = ClickSource;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
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
    columnChooserTriggerReady = false;
    error: Observable<any>;
    pageSizeOptions = [25, 50, 100];
    isDragging = false;

    private readonly maximumColumnsNumber$ = new BehaviorSubject(1);
    readonly visibleDataColumns$ = this.getVisibleDataColumns();
    readonly visibleColumnNames$ = this.getVisibleColumnNames();

    private destroyed = new Subject<void>();

    constructor(
        public entriesService: NodeEntriesService<T>,
        private applicationRef: ApplicationRef,
        private toast: Toast,
        private changeDetectorRef: ChangeDetectorRef,
        public ui: UIService,
        private ngZone: NgZone,
        private elementRef: ElementRef<HTMLElement>,
    ) {
        this.registerMaximumColumnsNumber();
    }

    ngAfterViewInit(): void {
        void Promise.resolve().then(() => {
            this.registerSortChanges();
        });
        this.visibleDataColumns$
            .pipe(first(), delay(0))
            .subscribe(() => (this.columnChooserTriggerReady = true));
        rxjs.combineLatest([
            this.entriesService.dataSource$.pipe(startWith(void 0 as void)),
            this.entriesService.options$.pipe(startWith(void 0 as void)),
            this.entriesService.dataSource.isLoadingSubject.pipe(startWith(void 0 as void)),
            this.entriesService.selection.changed.pipe(startWith(void 0 as void)),
        ])
            .pipe(takeUntil(this.destroyed))
            .subscribe(() => {
                this.changeDetectorRef.detectChanges();
            });
    }

    ngOnChanges(changes: SimpleChanges): void {
        this.updateSort();
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    onRowContextMenu({ event, node }: { event: MouseEvent | Event; node: T }) {
        if (!this.entriesService.selection.selected.includes(node)) {
            this.entriesService.selection.clear();
            this.entriesService.selection.select(node);
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
            start: this.entriesService.sort?.direction as 'asc' | 'desc',
            disableClear: false,
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

    private registerMaximumColumnsNumber() {
        BorderBoxObserverDirective.observeElement(this.elementRef)
            .pipe(
                takeUntil(this.destroyed),
                map((box) => this.getMaximumColumnsNumber(box.width)),
                distinctUntilChanged(),
            )
            .subscribe((maximumColumnsNumber) =>
                this.ngZone.run(() => this.maximumColumnsNumber$.next(maximumColumnsNumber)),
            );
    }

    private getMaximumColumnsNumber(tableWidth: number): number {
        return Math.max(
            1,
            Math.floor(
                // Subtract total width of always visible columns like checkboxes and icons.
                (tableWidth - 187) /
                    // Divide by with of data columns (including margin).
                    126,
            ),
        );
    }

    private getVisibleDataColumns(): Observable<ListItem[]> {
        return rxjs
            .combineLatest([
                this.maximumColumnsNumber$,
                this.entriesService.columnsChange.pipe(startWith(void 0)),
            ])
            .pipe(
                map(([maximumColumnsNumber]) => {
                    const columns = this.entriesService.columns;
                    return (columns ?? [])
                        .filter((column) => column.visible)
                        .filter((_, index) => index < maximumColumnsNumber);
                }),
                shareReplay(1),
            );
    }

    private getVisibleColumnNames(): Observable<string[]> {
        return this.visibleDataColumns$.pipe(
            map((visibleDataColumns) => {
                const columns = [];
                if (this.entriesService.checkbox) {
                    columns.push('select');
                }
                columns.push('icon');
                return columns.concat(visibleDataColumns.map((c) => c.name)).concat(['actions']);
            }),
            shareReplay(1),
        );
    }

    isSortable(column: ListItem) {
        return this.entriesService.sort?.columns.some((c) => c.name === column.name);
    }

    toggleAll(checked: boolean) {
        if (checked) {
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

    canDrop = (dragData: DragData<T>): CanDrop => {
        return this.entriesService.dragDrop.dropAllowed?.(dragData);
    };

    drop(dragData: DragData<Node>) {
        this.entriesService.dragDrop.dropped(dragData.target, {
            element: dragData.draggedNodes,
            mode: dragData.action,
        });
    }

    getDragData(node: T): T[] {
        const selection = this.entriesService.selection;
        if (selection.isSelected(node)) {
            return selection.selected;
        } else {
            return [node];
        }
    }

    onDragStarted(node: T) {
        if (!this.entriesService.selection.isSelected(node)) {
            this.entriesService.selection.clear();
            this.entriesService.selection.select(node);
        }
        this.isDragging = true;
    }

    onDragEnded() {
        this.isDragging = false;
    }

    loadData() {
        // TODO: focus next item when triggered via button.
        if (this.entriesService.dataSource.hasMore()) {
            this.entriesService.fetchData.emit({
                offset: this.entriesService.dataSource.getData().length,
            });
        }
    }

    async openMenu(node: T) {
        this.entriesService.selection.clear();
        this.entriesService.selection.select(node);
        await this.applicationRef.tick();
        this.dropdown.menu.focusFirstItem();
    }
}
