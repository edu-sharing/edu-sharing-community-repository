import {
    AfterViewInit,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit,
    ViewChild,
} from '@angular/core';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { distinctUntilChanged, filter, map, takeUntil, tap } from 'rxjs/operators';
import { GenericAuthority, Node } from '../../core-module/core.module';
import { NodeEntriesService } from '../../core-ui-module/node-entries.service';
import { KeyboardShortcutsService } from '../../services/keyboard-shortcuts.service';
import { TranslationsService } from '../../translations/translations.service';
import { NodeEntriesDisplayType } from './entries-model';
import { NodeDataSourceRemote } from './node-data-source-remote';
import { NodeEntriesGlobalService } from './node-entries-global.service';
import { NodeEntriesTemplatesService } from './node-entries-templates.service';

@Component({
    selector: 'es-node-entries',
    templateUrl: 'node-entries.component.html',
    styleUrls: ['node-entries.component.scss'],
})
export class NodeEntriesComponent<T extends NodeEntriesDataType>
    implements OnInit, AfterViewInit, OnDestroy
{
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;

    @ViewChild(MatPaginator) paginator: MatPaginator;

    private readonly destroyed = new Subject<void>();

    constructor(
        public changeDetectorRef: ChangeDetectorRef,
        public entriesGlobalService: NodeEntriesGlobalService,
        public entriesService: NodeEntriesService<T>,
        public templatesService: NodeEntriesTemplatesService,
        private globalKeyboardShortcuts: KeyboardShortcutsService,
        private route: ActivatedRoute,
        private router: Router,
        private translate: TranslateService,
        private translations: TranslationsService,
    ) {}

    ngOnInit(): void {
        if (this.entriesService.primaryInstance) {
            this.registerGlobalKeyboardShortcuts();
        }
    }

    ngAfterViewInit() {
        if (this.paginator) {
            this.initPaginator(this.paginator);
        }
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    private registerGlobalKeyboardShortcuts() {
        this.globalKeyboardShortcuts.register(
            [
                {
                    modifiers: ['Ctrl/Cmd'],
                    keyCode: 'KeyA',
                    ignoreWhen: (event) =>
                        // SmallGrid doesn't support selection
                        this.entriesService.displayType === NodeEntriesDisplayType.SmallGrid,
                    callback: () => this.entriesService.toggleSelectAll(),
                },
            ],
            { until: this.destroyed },
        );
    }

    private initPaginator(paginator: MatPaginator) {
        // I18n.
        this.translations.waitForInit().subscribe(() => {
            paginator._intl.itemsPerPageLabel = this.translate.instant(
                'PAGINATOR.itemsPerPageLabel',
            );
            paginator._intl.nextPageLabel = this.translate.instant('PAGINATOR.nextPageLabel');
            paginator._intl.previousPageLabel = this.translate.instant(
                'PAGINATOR.previousPageLabel',
            );
            paginator._intl.getRangeLabel = (page, pageSize, length) =>
                this.translate.instant('PAGINATOR.getRangeLabel', {
                    page: page + 1,
                    pageSize,
                    length,
                    pageCount: Math.ceil(length / pageSize),
                });
        });
        // Connect data source.
        this.entriesService.dataSource$.pipe(takeUntil(this.destroyed)).subscribe((dataSource) => {
            if (dataSource instanceof NodeDataSourceRemote) {
                dataSource.paginator = paginator;
            } else {
                paginator.length = dataSource?.getTotal();
                dataSource
                    ?.connectPagination()
                    .pipe(takeUntil(this.destroyed))
                    .subscribe(() => {
                        paginator.length = dataSource.getTotal();
                    });
            }
        });
        // Register query params.
        if (this.entriesService.primaryInstance) {
            const defaultPageSize = this.paginator.pageSize;
            let currentPageParam = 0;
            let currentPageSizeParam = defaultPageSize;
            this.route.queryParams
                .pipe(
                    map((params) => ({
                        page: params.page ? parseInt(params.page) - 1 : 0,
                        pageSize: params.pageSize ? parseInt(params.pageSize) : defaultPageSize,
                    })),
                    tap(({ page, pageSize }) => {
                        currentPageParam = page;
                        currentPageSizeParam = pageSize;
                    }),
                    filter(
                        ({ page, pageSize }) =>
                            page !== this.paginator.pageIndex ||
                            pageSize !== this.paginator.pageSize,
                    ),
                )
                .subscribe(({ page, pageSize }) => {
                    const previousPage = this.paginator.pageIndex;
                    this.paginator.pageIndex = page;
                    this.paginator.pageSize = pageSize;
                    this.paginator['_emitPageEvent'](previousPage);
                    this.changeDetectorRef.detectChanges();
                });
            this.paginator.page
                .pipe(
                    filter(
                        (event) =>
                            currentPageParam !== event.pageIndex ||
                            currentPageSizeParam !== event.pageSize,
                    ),
                )
                .subscribe((event) => {
                    const page = event.pageIndex > 0 ? event.pageIndex + 1 : null;
                    const pageSize = event.pageSize !== defaultPageSize ? event.pageSize : null;
                    void this.router.navigate([], {
                        queryParams: { page, pageSize },
                        queryParamsHandling: 'merge',
                    });
                });
        }
    }

    openPage(page: PageEvent) {
        this.entriesService.fetchData.emit({
            offset: page.pageIndex * page.pageSize,
            amount: page.pageSize,
            reset: true,
        });
    }
}

export type NodeEntriesDataType = Node | GenericAuthority;
