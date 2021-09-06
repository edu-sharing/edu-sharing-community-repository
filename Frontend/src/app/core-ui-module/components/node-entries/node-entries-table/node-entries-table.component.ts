import { SelectionModel } from '@angular/cdk/collections';
import { CdkOverlayOrigin } from '@angular/cdk/overlay';
import {
    AfterViewInit,
    Component,
    EventEmitter,
    Input,
    Output,
    ViewChild,
    OnChanges,
    SimpleChanges,
} from '@angular/core';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSort, MatSortHeader, Sort } from '@angular/material/sort';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { BehaviorSubject, combineLatest, Observable, Subject } from 'rxjs';
import { delay, first, map, takeUntil } from 'rxjs/operators';
import {NodeEntriesService} from '../../../node-entries.service';
import {Node} from '../../../../core-module/rest/data-object';
import {ListItem} from '../../../../core-module/ui/list-item';
import {MatCheckboxChange} from '@angular/material/checkbox';
import {
    ClickSource,
    InteractionType
} from '../../node-entries-wrapper/node-entries-wrapper.component';
import {Target} from '../../../option-item';

@Component({
    selector: 'app-node-entries-table',
    templateUrl: './node-entries-table.component.html',
    styleUrls: ['./node-entries-table.component.scss'],
})
export class NodeEntriesTableComponent<T extends Node> implements OnChanges, AfterViewInit {
    readonly InteractionType = InteractionType;
    readonly ClickSource = ClickSource;
    readonly Target = Target;

    @ViewChild(MatSort) sort: MatSort;
    @ViewChild(MatPaginator) paginator: MatPaginator;
    @ViewChild('columnChooserTrigger') columnChooserTrigger: CdkOverlayOrigin;

    loading: Observable<boolean>;
    isPageSelected = new BehaviorSubject(false);
    isAllSelected = new BehaviorSubject(false);
    columnChooserVisible = false;
    ready = false;
    error: Observable<any>;
    pageSizeOptions = [25, 50, 100];

    constructor(private route: ActivatedRoute,
                public entriesService: NodeEntriesService<T>,
                private router: Router
    ) {}

    ngAfterViewInit(): void {
        Promise.resolve().then(() => {
            this.registerNavigation();
            this.ready = true;
        });
    }

    ngOnChanges(changes: SimpleChanges): void {
    }


    onRowContextMenu({ event, node }: { event: MouseEvent; node: T }) {
        if (!this.entriesService.selection.selected.includes(node)) {
            this.entriesService.selection.clear();
            this.entriesService.selection.select(node)
        }
    }

    private registerNavigation(): void {
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
        this.sort.sortChange.subscribe((sort: Sort) => {
            console.log('sortChange', sort);
            this.router.navigate(['.'], {
                relativeTo: this.route,
                queryParams: { sort: JSON.stringify(sort) },
                queryParamsHandling: 'merge',
                replaceUrl: true,
            });
        });
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
    }
    getVisibleColumns() {
        return ['select', 'icon'].concat(
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
            console.log('checked');
            this.entriesService.selection.select(...this.entriesService.dataSource.getData());
        } else {
            this.entriesService.selection.clear();
        }

    }
}
