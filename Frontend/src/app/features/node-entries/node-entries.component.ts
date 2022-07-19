import {
    AfterViewInit, ApplicationRef, ChangeDetectorRef,
    Component,
    ContentChild, HostListener, Input,
    OnChanges, OnDestroy,
    SimpleChanges,
    TemplateRef, ViewChild
} from '@angular/core';
import { UIService, GenericAuthority, Node } from '../../core-module/core.module';
import { KeyEvents } from '../../core-module/ui/key-events';
import { NodeEntriesService } from '../../core-ui-module/node-entries.service';
import { NodeEntriesDisplayType } from './entries-model';

import {NodeEntriesTemplatesService} from './node-entries-templates.service';
import {NodeEntriesGlobalService, PaginationStrategy} from "./node-entries-global.service";
import {MatPaginator, PageEvent} from "@angular/material/paginator";
import {TranslateService} from "@ngx-translate/core";
import {takeUntil} from "rxjs/operators";
import {Subject} from "rxjs";
import {TranslationsService} from "../../translations/translations.service";

@Component({
    selector: 'es-node-entries',
    templateUrl: 'node-entries.component.html',
    styleUrls: ['node-entries.component.scss'],

})
export class NodeEntriesComponent<T extends NodeEntriesDataType> implements OnChanges, AfterViewInit, OnDestroy {
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly PaginationStrategy = PaginationStrategy;
    private readonly destroyed$ = new Subject();
    @ViewChild(MatPaginator) paginator: MatPaginator;

    @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent): void {
        if (
            event.code === 'KeyA' &&
            (event.ctrlKey || this.uiService.isAppleCmd()) &&
            !KeyEvents.eventFromInputField(event)
        ) {
            if(this.entriesService.selection.isEmpty()) {
                this.entriesService.selection.select(...this.entriesService.dataSource.getData());
            } else {
                this.entriesService.selection.clear();
            }
            event.preventDefault();
            event.stopPropagation();
        }
    }


    constructor(
        private uiService: UIService,
        public entriesGlobalService: NodeEntriesGlobalService,
        public entriesService: NodeEntriesService<T>,
        public templatesService: NodeEntriesTemplatesService,
        public changeDetectorRef: ChangeDetectorRef,
        public translate: TranslateService,
        public translations: TranslationsService,
    ) {
    }

    ngOnChanges(changes: SimpleChanges): void {
    }

    async ngAfterViewInit() {
        if (this.paginator) {
            await this.translations.waitForInit().toPromise();
            this.paginator._intl.itemsPerPageLabel = this.translate.instant('PAGINATOR.itemsPerPageLabel');
            this.paginator._intl.nextPageLabel = this.translate.instant('PAGINATOR.nextPageLabel');
            this.paginator._intl.previousPageLabel = this.translate.instant('PAGINATOR.previousPageLabel');
            this.paginator._intl.getRangeLabel = (page, pageSize, length) => (
                this.translate.instant('PAGINATOR.getRangeLabel',
                    {page: (page + 1),
                        pageSize,
                        length,
                        pageCount: Math.ceil(length / pageSize)}
                )
            );
            this.paginator.length = this.entriesService.dataSource?.getTotal();
            this.entriesService.dataSource$.pipe(
                takeUntil(this.destroyed$)
            ).subscribe((dataSource) => {
                this.paginator.length = dataSource?.getTotal();
                dataSource?.connectPagination().pipe(
                    takeUntil(this.destroyed$)
                ).subscribe(() => {
                    this.paginator.length = this.entriesService.dataSource.getTotal();
                });
            });
        }
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    openPage(page: PageEvent) {
        this.entriesService.fetchData.emit({
            offset: page.pageIndex * page.pageSize,
            amount: page.pageSize,
            reset: true
        });
    }
}
export type NodeEntriesDataType = Node | GenericAuthority;
