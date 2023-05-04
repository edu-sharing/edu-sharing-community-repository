import { AfterViewInit, Component, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import {
    ArchiveRestore,
    Node,
    RestArchiveService,
    RestConstants,
    TemporaryStorageService,
} from '../../../core-module/core.module';
import {
    ActionbarComponent,
    CustomOptions,
    ElementType,
    FetchEvent,
    InteractionType,
    ListItem,
    ListItemSort,
    ListSortConfig,
    NodeClickEvent,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
    OptionItem,
    Scope,
} from 'ngx-edu-sharing-ui';
import { Toast } from '../../../core-ui-module/toast';
import { SearchFieldService } from '../../../main/navigation/search-field/search-field.service';
import { RecycleRestoreComponent } from './restore/restore.component';
import { GenericAuthority } from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-recycle',
    templateUrl: 'recycle.component.html',
    styleUrls: ['recycle.component.scss'],
})
export class RecycleMainComponent implements OnInit, AfterViewInit, OnDestroy {
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    readonly Scope = Scope;
    @ViewChild('list') list: NodeEntriesWrapperComponent<Node>;
    dataSource = new NodeDataSource();
    public toDelete: Node[] = null;
    public restoreResult: ArchiveRestore;

    @Input() actionbar: ActionbarComponent;

    public columns: ListItem[] = [
        new ListItem('NODE', RestConstants.CM_NAME),
        new ListItem('NODE', RestConstants.CM_ARCHIVED_DATE),
    ];
    public options: CustomOptions = {
        useDefaultOptions: false,
        addOptions: [],
    };
    sort: ListSortConfig = {
        columns: [
            new ListItemSort('NODE', RestConstants.CM_NAME),
            new ListItemSort('NODE', RestConstants.CM_ARCHIVED_DATE),
        ],
        allowed: true,
        active: RestConstants.CM_ARCHIVED_DATE,
        direction: 'desc',
    };
    searchQuery: string;
    loadData(currentQuery: string, offset: number, sortBy: string, sortAscending: boolean) {
        return this.archive.search(currentQuery || '*', '', {
            propertyFilter: [RestConstants.ALL],
            offset: offset,
            sortBy: [sortBy],
            sortAscending: sortAscending,
        });
    }
    private destroyed = new Subject<void>();

    constructor(
        private archive: RestArchiveService,
        private toast: Toast,
        private translate: TranslateService,
        private service: TemporaryStorageService,
        private searchField: SearchFieldService,
    ) {}

    ngOnInit(): void {
        this.options.addOptions.push(
            new OptionItem('RECYCLE.OPTION.RESTORE_SINGLE', 'undo', (node: Node) =>
                this.restoreSingle(node),
            ),
        );
        this.options.addOptions.push(
            new OptionItem('RECYCLE.OPTION.DELETE_SINGLE', 'delete', (node: Node) =>
                this.deleteSingle(node),
            ),
        );
        this.options.addOptions.forEach((o) => {
            o.elementType = [ElementType.Node, ElementType.NodePublishedCopy];
        });
        this.searchField
            .getCurrentInstance()
            .onSearchTriggered()
            .pipe(takeUntil(this.destroyed))
            .subscribe(({ searchString }) => {
                this.searchQuery = searchString;
                this.refresh();
            });
    }

    ngAfterViewInit(): void {
        this.refresh();
        this.list.initOptionsGenerator({
            actionbar: this.actionbar,
            customOptions: this.options,
        });
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    private restoreFinished(list: Node[], restoreResult: any) {
        this.toast.closeModalDialog();

        RecycleRestoreComponent.prepareResults(this.translate, restoreResult);
        if (restoreResult.hasDuplicateNames || restoreResult.hasParentFolderMissing)
            this.restoreResult = restoreResult;

        if (list.length == 1) {
            this.toast.toast('RECYCLE.TOAST.RESTORE_FINISHED_SINGLE'); //,{link : 'TODO'},{enableHTML:true});
        } else this.toast.toast('RECYCLE.TOAST.RESTORE_FINISHED');
        this.refresh();
    }
    private delete(): void {
        this.deleteNodes(this.list.getSelection().selected);
    }
    private deleteSingle(node: Node): void {
        if (node == null) {
            this.delete();
            return;
        }
        this.deleteNodes([node]);
    }

    public deleteNodesWithoutConfirmation(list = this.toDelete) {
        this.toast.showProgressDialog();
        this.archive.delete(list).subscribe(
            () => this.deleteFinished(),
            (error) => this.handleErrors(error),
        );
    }

    private deleteFinished() {
        this.toast.closeModalDialog();
        this.toast.toast('RECYCLE.TOAST.DELETE_FINISHED');
        this.list.getSelection().clear();
        this.refresh();
    }
    private deleteNodes(list: Node[]) {
        if (this.service.get('recycleSkipDeleteConfirmation', false)) {
            this.deleteNodesWithoutConfirmation(list);
            return;
        }

        this.toDelete = [...list];
    }
    restoreNodesEvent(event: any) {
        this.restoreNodes(event.nodes, event.parent);
    }
    finishRestore() {
        this.restoreResult = null;
    }
    finishDelete() {
        this.toDelete = null;
    }
    public restoreNodes(list: Node[], toPath = '') {
        // archiveRestore list
        this.toast.showProgressDialog();
        this.archive.restore(list, toPath).subscribe(
            (result: ArchiveRestore) => this.restoreFinished(list, result),
            (error: any) => this.handleErrors(error),
        );
    }
    private handleErrors(error: any) {
        this.toast.error(error);
        this.toast.closeModalDialog();
    }

    private restoreSingle(node: Node): void {
        if (node == null) {
            this.restore();
            return;
        }

        this.restoreNodes([node]);
    }
    private restore(): void {
        this.restoreNodes(this.list.getSelection().selected);
    }

    async refresh(event?: FetchEvent) {
        this.dataSource.isLoading = true;
        if (event == null) {
            this.dataSource.reset();
            this.list?.getSelection().clear();
        }
        const result = await this.loadData(
            this.searchQuery,
            event ? event?.offset || this.dataSource.getData().length : 0,
            this.sort.active,
            this.sort.direction === 'asc',
        ).toPromise();
        if (event == null) {
            this.dataSource.setData(result.nodes, result.pagination);
        } else {
            this.dataSource.appendData(result.nodes);
            this.dataSource.setPagination(result.pagination);
        }
        this.dataSource.isLoading = false;
    }

    click(event: NodeClickEvent<Node | GenericAuthority>) {
        this.list.getSelection().toggle(event.element as Node);
    }

    updateSort(sort: ListSortConfig) {
        console.log(sort);
        this.sort = sort;
        this.refresh();
    }
}
