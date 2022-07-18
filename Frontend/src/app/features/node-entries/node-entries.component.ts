import {
    AfterViewInit,
    Component,
    ContentChild, HostListener, Input,
    OnChanges,
    SimpleChanges,
    TemplateRef, ViewChild
} from '@angular/core';
import { UIService, GenericAuthority, Node } from '../../core-module/core.module';
import { KeyEvents } from '../../core-module/ui/key-events';
import { NodeEntriesService } from '../../core-ui-module/node-entries.service';
import { NodeEntriesDisplayType } from './entries-model';

import {NodeEntriesTemplatesService} from './node-entries-templates.service';
import {NodeEntriesGlobalService, PaginationStrategy} from "./node-entries-global.service";
import {MatPaginator} from "@angular/material/paginator";
import {TranslateService} from "@ngx-translate/core";
import {takeUntil} from "rxjs/operators";
import {Subject} from "rxjs";

@Component({
    selector: 'es-node-entries',
    templateUrl: 'node-entries.component.html',
    styleUrls: ['node-entries.component.scss'],

})
export class NodeEntriesComponent<T extends NodeEntriesDataType> implements OnChanges, AfterViewInit {
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
        public translate: TranslateService,
    ) {

    }

    ngOnChanges(changes: SimpleChanges): void {
    }

    async ngAfterViewInit() {
        console.log(this.paginator);
        if (this.paginator) {
            this.paginator._intl.itemsPerPageLabel = await this.translate.get('PAGINATOR.itemsPerPageLabel').toPromise();
            this.paginator._intl.nextPageLabel = await this.translate.get('PAGINATOR.nextPageLabel').toPromise();
            this.paginator._intl.previousPageLabel = await this.translate.get('PAGINATOR.previousPageLabel').toPromise();
            this.paginator._intl.getRangeLabel = (page, pageSize, length) => (
                this.translate.instant('PAGINATOR.getRangeLabel',
                    {page: (page + 1),
                        pageSize,
                        length,
                        pageCount: Math.ceil(length / pageSize)}
                )
            );
            this.paginator.length = this.entriesService.dataSource.getTotal();
            this.entriesService.dataSource.connect()
                .pipe(
                    takeUntil(this.destroyed$)
                ).subscribe((data) => {
                    this.paginator.length = this.entriesService.dataSource.getTotal();
            });
        }
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }
}
export type NodeEntriesDataType = Node | GenericAuthority;
