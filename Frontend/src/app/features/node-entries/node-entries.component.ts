import {
    AfterViewInit,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit,
    ViewChild,
} from '@angular/core';
import { GenericAuthority, Node, UIService } from '../../core-module/core.module';
import { NodeEntriesService } from '../../core-ui-module/node-entries.service';
import { KeyboardShortcutsService } from '../../services/keyboard-shortcuts.service';
import { NodeEntriesDisplayType } from './entries-model';
import { NodeEntriesTemplatesService } from './node-entries-templates.service';
import { NodeEntriesGlobalService, PaginationStrategy } from './node-entries-global.service';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { TranslateService } from '@ngx-translate/core';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { TranslationsService } from '../../translations/translations.service';

@Component({
    selector: 'es-node-entries',
    templateUrl: 'node-entries.component.html',
    styleUrls: ['node-entries.component.scss'],
})
export class NodeEntriesComponent<T extends NodeEntriesDataType>
    implements OnInit, AfterViewInit, OnDestroy
{
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly PaginationStrategy = PaginationStrategy;
    private readonly destroyed$ = new Subject();
    @ViewChild(MatPaginator) paginator: MatPaginator;

    private destroyed = new Subject<void>();

    constructor(
        private uiService: UIService,
        public entriesGlobalService: NodeEntriesGlobalService,
        public entriesService: NodeEntriesService<T>,
        public templatesService: NodeEntriesTemplatesService,
        private globalKeyboardShortcuts: KeyboardShortcutsService,
        public changeDetectorRef: ChangeDetectorRef,
        public translate: TranslateService,
        public translations: TranslationsService,
    ) {}

    ngOnInit(): void {
        if (this.entriesService.globalKeyboardShortcuts) {
            this.registerGlobalKeyboardShortcuts();
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

    async ngAfterViewInit() {
        if (this.paginator) {
            await this.translations.waitForInit().toPromise();
            this.paginator._intl.itemsPerPageLabel = this.translate.instant(
                'PAGINATOR.itemsPerPageLabel',
            );
            this.paginator._intl.nextPageLabel = this.translate.instant('PAGINATOR.nextPageLabel');
            this.paginator._intl.previousPageLabel = this.translate.instant(
                'PAGINATOR.previousPageLabel',
            );
            this.paginator._intl.getRangeLabel = (page, pageSize, length) =>
                this.translate.instant('PAGINATOR.getRangeLabel', {
                    page: page + 1,
                    pageSize,
                    length,
                    pageCount: Math.ceil(length / pageSize),
                });
            this.paginator.length = this.entriesService.dataSource?.getTotal();
            this.entriesService.dataSource$
                .pipe(takeUntil(this.destroyed$))
                .subscribe((dataSource) => {
                    this.paginator.length = dataSource?.getTotal();
                    dataSource
                        ?.connectPagination()
                        .pipe(takeUntil(this.destroyed$))
                        .subscribe(() => {
                            this.paginator.length = this.entriesService.dataSource.getTotal();
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
