import {
    AfterViewInit,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    Output,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import {
    ConfigurationService,
    ListItem,
    Node,
    NodeList,
    RestConnectorService,
    RestConstants,
    RestNodeService,
    RestSearchService,
    SessionStorageService,
    TemporaryStorageService,
    UIService,
} from '../../../core-module/core.module';
import { TranslateService } from '@ngx-translate/core';
import { CustomOptions, Scope } from '../../../core-ui-module/option-item';
import { Toast } from '../../../core-ui-module/toast';
import { Helper } from '../../../core-module/rest/helper';
import { ActionbarComponent } from '../../../shared/components/actionbar/actionbar.component';
import { ListTableComponent } from '../../../core-ui-module/components/list-table/list-table.component';
import {
    DropSource,
    DropTarget,
    FetchEvent,
    InteractionType,
    ListSortConfig,
    NodeClickEvent,
    NodeEntriesDisplayType,
    NodeRoot,
} from 'src/app/features/node-entries/entries-model';
import { NodeEntriesWrapperComponent } from 'src/app/features/node-entries/node-entries-wrapper.component';
import { NodeDataSource } from 'src/app/features/node-entries/node-data-source';
import { NodeEntriesDataType } from 'src/app/features/node-entries/node-entries.component';
import { canDropOnNode } from '../workspace-utils';
import { BehaviorSubject } from 'rxjs';

@Component({
    selector: 'es-workspace-explorer',
    templateUrl: 'explorer.component.html',
    styleUrls: ['explorer.component.scss'],
})
export class WorkspaceExplorerComponent implements OnDestroy, OnChanges, AfterViewInit {
    public readonly SCOPES = Scope;
    readonly InteractionType = InteractionType;

    public static getColumns(
        connector: RestConnectorService,
        customColumns: ListItem[] = [],
        configColumns: string[] = [],
    ) {
        let defaultColumns: ListItem[] = [];
        defaultColumns.push(new ListItem('NODE', RestConstants.CM_NAME));
        defaultColumns.push(new ListItem('NODE', RestConstants.CM_CREATOR));
        defaultColumns.push(new ListItem('NODE', RestConstants.CM_MODIFIED_DATE));
        if (connector.getCurrentLogin() ? connector.getCurrentLogin().isAdmin : false) {
            defaultColumns.push(new ListItem('NODE', RestConstants.NODE_ID));

            const repsource = new ListItem('NODE', RestConstants.CCM_PROP_REPLICATIONSOURCEID);
            repsource.visible = false;
            defaultColumns.push(repsource);
        }
        const title = new ListItem('NODE', RestConstants.LOM_PROP_TITLE);
        title.visible = false;
        const size = new ListItem('NODE', RestConstants.SIZE);
        size.visible = false;
        const created = new ListItem('NODE', RestConstants.CM_PROP_C_CREATED);
        created.visible = false;
        const mediatype = new ListItem('NODE', RestConstants.MEDIATYPE);
        mediatype.visible = false;
        const keywords = new ListItem('NODE', RestConstants.LOM_PROP_GENERAL_KEYWORD);
        keywords.visible = false;
        const dimensions = new ListItem('NODE', RestConstants.DIMENSIONS);
        dimensions.visible = false;
        const version = new ListItem('NODE', RestConstants.LOM_PROP_LIFECYCLE_VERSION);
        version.visible = false;
        const usage = new ListItem('NODE', RestConstants.VIRTUAL_PROP_USAGECOUNT);
        usage.visible = false;
        const license = new ListItem('NODE', RestConstants.CCM_PROP_LICENSE);
        license.visible = false;
        const wfStatus = new ListItem('NODE', RestConstants.CCM_PROP_WF_STATUS);
        wfStatus.visible = false;
        defaultColumns.push(title);
        defaultColumns.push(size);
        defaultColumns.push(created);
        defaultColumns.push(mediatype);
        defaultColumns.push(keywords);
        defaultColumns.push(dimensions);
        defaultColumns.push(version);
        defaultColumns.push(usage);
        defaultColumns.push(license);
        defaultColumns.push(wfStatus);

        if (Array.isArray(configColumns)) {
            const configList: ListItem[] = [];
            for (const col of defaultColumns) {
                if (configColumns.indexOf(col.name) != -1) {
                    col.visible = true;
                    configList.push(col);
                }
            }
            for (const col of defaultColumns) {
                if (configColumns.indexOf(col.name) == -1) {
                    col.visible = false;
                    configList.push(col);
                }
            }
            // sort as defined inside config
            configList.sort((a, b) => {
                let pos1 = configColumns.indexOf(a.name);
                let pos2 = configColumns.indexOf(b.name);
                if (pos1 === -1) pos1 = configColumns.length;
                if (pos2 === -1) pos2 = configColumns.length;
                return pos1 - pos2;
            });
            defaultColumns = configList;
        }
        if (Array.isArray(customColumns)) {
            for (const column of defaultColumns) {
                let add = true;
                for (const column2 of customColumns) {
                    if (column.name === column2.name) {
                        add = false;
                        break;
                    }
                }
                if (add) {
                    customColumns.push(column);
                }
            }
            return customColumns;
        }
        return defaultColumns;
    }

    @ViewChild('list') list: ListTableComponent;
    @ViewChild(NodeEntriesWrapperComponent) nodeEntries: NodeEntriesWrapperComponent<Node>;
    readonly dataSourceSubject = new BehaviorSubject<NodeDataSource<Node>>(null);
    @Input() customOptions: CustomOptions;
    @Input()
    get dataSource() {
        return this.dataSourceSubject.value;
    }
    set dataSource(dataSource: NodeDataSource<Node>) {
        this.dataSourceSubject.next(dataSource);
    }
    @Output() nodesChange = new EventEmitter<Node[]>();
    sort: ListSortConfig = {
        allowed: true,
        active: RestConstants.CM_NAME,
        direction: 'asc',
        columns: RestConstants.POSSIBLE_SORT_BY_FIELDS,
    };

    public columns: ListItem[] = [];
    @Input() displayType = NodeEntriesDisplayType.Table;
    @Output() displayTypeChange = new EventEmitter<NodeEntriesDisplayType>();
    @Input() reorderDialog = false;
    @Output() reorderDialogChange = new EventEmitter<boolean>();
    @Input() preventKeyevents: boolean;
    @Input() actionbar: ActionbarComponent;

    totalCount: number;

    public _searchQuery: string = null;
    _node: Node;
    private lastRequestSearch: boolean;

    _root: NodeRoot;
    @Input() set root(root: NodeRoot) {
        this._root = root;
        if (['MY_FILES', 'SHARED_FILES'].includes(root)) {
            this.sort.columns = RestConstants.POSSIBLE_SORT_BY_FIELDS;
        } else {
            this.sort.columns = RestConstants.POSSIBLE_SORT_BY_FIELDS_SOLR;
        }
        this.storage
            .get(SessionStorageService.KEY_WORKSPACE_SORT + root, null)
            .subscribe((data) => {
                if (data?.sortBy != null) {
                    this.sort.active = data.sortBy;
                    this.sort.direction = data.sortAscending ? 'asc' : 'desc';
                } else {
                    this.sort.active = RestConstants.CM_NAME;
                    this.sort.direction = 'asc';
                }
            });
    }
    @Input() set current(current: Node) {
        this.setNode(current);
    }
    @Input() set searchQuery(query: any) {
        this.setSearchQuery(query);
    }
    @Output() onOpenNode = new EventEmitter<NodeEntriesDataType>();
    @Output() onViewNode = new EventEmitter();
    @Output() onSelectionChanged = new EventEmitter();
    @Output() onSelectNode = new EventEmitter<NodeEntriesDataType>();
    @Output() onSearchGlobal = new EventEmitter();
    @Output() onDrop = new EventEmitter<{ target: DropTarget; source: DropSource<Node> }>();
    @Output() onReset = new EventEmitter();
    private path: Node[];
    searchGlobal() {
        this.onSearchGlobal.emit(this._searchQuery);
    }
    public load(event: FetchEvent = null) {
        if (this._node == null && !this._searchQuery) return;
        if (this.dataSource.isLoading) {
            setTimeout(() => this.load(event), 10);
            return;
        }
        if (event?.reset) {
            this.dataSource.reset();
            this.nodeEntries.getSelection().clear();
            this.onReset.emit();
            if (event.offset === 0) {
                console.log('reset pagination', event);
                this.nodeEntries.resetPagination();
            }
        } else if (this.dataSource.isFullyLoaded()) {
            return;
        }
        this.dataSource.isLoading = true;
        // ignore virtual (new) added/uploaded elements
        const offset = event.offset || this.getRealNodeCount();
        const request: any = {
            offset,
            propertyFilter: [
                RestConstants.ALL,
                /*RestConstants.CM_MODIFIED_DATE,
              RestConstants.CM_CREATOR,
              RestConstants.CM_PROP_C_CREATED,
              RestConstants.CCM_PROP_LICENSE,
              RestConstants.LOM_PROP_LIFECYCLE_VERSION,
              RestConstants.CCM_PROP_WF_STATUS,
              RestConstants.CCM_PROP_CCRESSOURCETYPE,
              RestConstants.CCM_PROP_CCRESSOURCESUBTYPE,
              RestConstants.CCM_PROP_CCRESSOURCEVERSION,
              RestConstants.CCM_PROP_WIDTH,
              RestConstants.CCM_PROP_HEIGHT,
              RestConstants.VIRTUAL_PROP_USAGECOUNT,*/
            ],
            sortBy: [this.sort.active],
            sortAscending: this.sort.direction === 'asc',
            count: event?.amount,
        };
        if (this._searchQuery) {
            const query = '*' + this._searchQuery + '*';
            this.lastRequestSearch = true;
            /*this.search.searchByProperties([RestConstants.NODE_ID,RestConstants.CM_PROP_TITLE,RestConstants.CM_NAME,RestConstants.LOM_PROP_DESCRIPTION,RestConstants.LOM_PROP_GENERAL_KEYWORD],
              [query,query,query,query,query],[],RestConstants.COMBINE_MODE_OR,RestConstants.CONTENT_TYPE_FILES_AND_FOLDERS, request).subscribe((data : NodeList) => this.addNodes(data,true));*/
            const criterias: any = [];
            criterias.push({ property: RestConstants.PRIMARY_SEARCH_CRITERIA, values: [query] });
            if (this._node) {
                criterias.push({
                    property: 'parent',
                    values: [this._node ? this._node.ref.id : ''],
                });
            }
            this.search
                .search(
                    criterias,
                    [],
                    request,
                    this.connector.getCurrentLogin() && this.connector.getCurrentLogin().isAdmin
                        ? RestConstants.CONTENT_TYPE_ALL
                        : RestConstants.CONTENT_TYPE_FILES_AND_FOLDERS,
                    RestConstants.HOME_REPOSITORY,
                    RestConstants.DEFAULT,
                    [],
                    'workspace',
                )
                .subscribe(
                    (data: NodeList) => {
                        this.addNodes(data, true);
                    },
                    (error: any) => {
                        this.totalCount = 0;
                        this.handleError(error);
                    },
                );
            // this.nodeApi.searchNodes(query,[],request).subscribe((data : NodeList) => this.addNodes(data,true));
        } else {
            this.lastRequestSearch = false;
            this.nodeApi.getChildren(this._node.ref.id, [], request).subscribe(
                (data: NodeList) => this.addNodes(data, false),
                (error: any) => {
                    this.totalCount = 0;
                    this.handleError(error);
                },
            );
        }
    }

    ngOnDestroy(): void {
        this.temporaryStorage.set(
            TemporaryStorageService.NODE_RENDER_PARAMETER_DATA_SOURCE,
            this.dataSource,
        );
    }

    async ngOnChanges(changes: SimpleChanges) {
        if (changes.displayType) {
            await this.initOptions();
        }
    }

    private handleError(error: any) {
        if (error.status == 404)
            this.toast.error(null, 'WORKSPACE.TOAST.NOT_FOUND', { id: this._node.ref.id });
        else this.toast.error(error);

        this.dataSource.isLoading = false;
    }
    private addNodes(data: NodeList, wasSearch: boolean) {
        if (this.lastRequestSearch !== wasSearch) {
            return;
        }
        this.dataSource.isLoading = false;
        if (data && data.nodes) {
            this.dataSource.appendData(data.nodes);
            this.dataSource.setPagination(data.pagination);
        }
    }
    constructor(
        private connector: RestConnectorService,
        private translate: TranslateService,
        private storage: SessionStorageService,
        private temporaryStorage: TemporaryStorageService,
        private config: ConfigurationService,
        private search: RestSearchService,
        private toast: Toast,
        public ui: UIService,
        private nodeApi: RestNodeService,
    ) {
        // super(temporaryStorage,['_node','_nodes','sortBy','sortAscending','columns','totalCount','hasMoreToLoad']);
        this.initColumns();
    }

    async ngAfterViewInit() {
        await this.initOptions();
    }
    public setSorting(config: ListSortConfig) {
        this.sort = config;
        this.storage.set(SessionStorageService.KEY_WORKSPACE_SORT + this._root, {
            sortBy: config.active,
            sortAscending: config.direction === 'asc',
        });
        this.load({ reset: true, offset: 0 });
    }
    public onSelection(event: Node[]) {
        this.onSelectionChanged.emit(event);
    }
    /*
    private addParentToPath(node : Node,path : string[]) {

      path.splice(1,0,node.ref.id);
      if (node.parent.id==path[0] || node.parent.id==null) {
        this.onOpenNode.emit(node);
        return;
      }
      this.nodeApi.getNodeMetadata(node.parent.id).subscribe((data: NodeWrapper)=> {
        this.addParentToPath(data.node, path);
      });

    }
     */
    public doubleClick(node: Node) {
        this.onOpenNode.emit(node);
    }

    private setNode(current: Node) {
        setTimeout(() => {
            this._searchQuery = null;
            if (!current) {
                this._node = null;
                return;
            }
            if (this.dataSource.isLoading) {
                setTimeout(() => this.setNode(current), 10);
                return;
            }
            if (Helper.objectEquals(this._node, current)) return;
            this._node = current;
            this.initOptions();
            this.load({ reset: true, offset: 0 });
        });
    }

    private setSearchQuery(query: any) {
        setTimeout(() => {
            if (query && query.query) {
                this._searchQuery = query.query;
                this._node = query.node;
                this.load({ reset: true, offset: 0 });
            } else {
                this._searchQuery = null;
            }
        });
    }
    canDrop = canDropOnNode;
    drop = (target: DropTarget, source: DropSource<Node>) => {
        this.onDrop.emit({
            target,
            source,
        });
    };

    private getRealNodeCount() {
        return this.dataSource?.getData().filter((n) => !n.virtual).length;
    }

    initColumns() {
        this.config.get('workspaceColumns').subscribe((data: string[]) => {
            this.storage.get('workspaceColumns').subscribe((columns: any[]) => {
                this.columns = WorkspaceExplorerComponent.getColumns(this.connector, columns, data);
            });
        });
    }

    select(event: NodeClickEvent<NodeEntriesDataType>) {
        if (
            !(
                this.nodeEntries.getSelection().selected.length === 1 &&
                this.nodeEntries.getSelection().selected[0] === event.element
            )
        ) {
            this.nodeEntries.getSelection().clear();
        }
        this.nodeEntries.getSelection().toggle(event.element as Node);
    }

    private async initOptions() {
        await this.nodeEntries?.initOptionsGenerator({
            actionbar: this.actionbar,
            customOptions: this.customOptions,
            scope: Scope.WorkspaceList,
            parent: this._node,
        });
    }

    onDelete(nodes: Node[]): void {
        this.dataSource.removeData(nodes);
        this.nodeEntries?.getSelection().clear();
    }

    saveColumns(columns: ListItem[]) {
        this.storage.set('workspaceColumns', columns);
    }

    clickItem(event: NodeClickEvent<NodeEntriesDataType>) {
        if (this.ui.isMobile()) {
            this.onOpenNode.emit(event.element);
        } else {
            this.select(event);
        }
    }
}
