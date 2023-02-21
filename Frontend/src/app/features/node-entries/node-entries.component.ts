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
import { takeUntil } from 'rxjs/operators';
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
        if (this.entriesService.dataSource instanceof NodeDataSourceRemote) {
            const pageSize = this.entriesGlobalService.getPaginatorSizeOptions(
                this.entriesService.scope,
            )[0];
            this.entriesService.dataSource.init({
                paginationConfig: {
                    defaultPageSize: pageSize,
                    strategy: this.entriesService.paginationStrategy,
                },
                initialSort: this.entriesService.sort,
            });
            if (
                this.entriesService.primaryInstance &&
                this.entriesService.paginationStrategy === 'paginator'
            ) {
                // Automatic query-params handling is only supported by node-data-source-remote.
                this.entriesService.dataSource.registerQueryParameters(this.route, this.router);
            }
        }
    }

    ngAfterViewInit() {
        if (this.paginator) {
            this.initPaginator(this.paginator);
            this.changeDetectorRef.detectChanges();
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
