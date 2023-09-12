import {Component, EventEmitter, Input, OnDestroy, Output, ViewChild} from '@angular/core';
import {ConfigurationService, ListItem, Node, NodeList, RestConnectorService, RestConstants, RestNodeService, RestSearchService, SessionStorageService, Store, TemporaryStorageService} from '../../../core-module/core.module';
import {TranslateService} from '@ngx-translate/core';
import {CustomOptions, Scope} from '../../../core-ui-module/option-item';
import {Toast} from '../../../core-ui-module/toast';
import {Helper} from '../../../core-module/rest/helper';
import {ActionbarComponent} from '../../../common/ui/actionbar/actionbar.component';
import {MainNavComponent} from '../../../common/ui/main-nav/main-nav.component';
import {ListTableComponent} from '../../../core-ui-module/components/list-table/list-table.component';
import {DropData} from '../../../core-ui-module/directives/drag-nodes/drag-nodes';
import {compareNumbers} from '@angular/compiler-cli/src/diagnostics/typescript_version';
import {WorkspaceRoot} from '../workspace.component';

@Component({
    selector: 'workspace-explorer',
    templateUrl: 'explorer.component.html',
    styleUrls: ['explorer.component.scss']
})
export class WorkspaceExplorerComponent implements OnDestroy {
    public readonly SCOPES = Scope;
    possibleSortByFields = RestConstants.POSSIBLE_SORT_BY_FIELDS;
    @ViewChild('list') list: ListTableComponent;
    public _nodes: Node[] = [];
    @Input() customOptions: CustomOptions;
    @Input() set nodes(nodes: Node[]) {
        this._nodes = nodes;
    }
    @Output() nodesChange = new EventEmitter<Node[]>();
    public sortBy: string = RestConstants.CM_NAME;
    public sortAscending = RestConstants.DEFAULT_SORT_ASCENDING;


    public columns : ListItem[]=[];
    @Input() viewType = 0;
    @Input() reorderDialog = false;
    @Output() reorderDialogChange = new EventEmitter<boolean>();
    @Input() preventKeyevents:boolean;
    @Input() mainNav:MainNavComponent;
    @Input() actionbar:ActionbarComponent;


    private loading=false;
    public showLoading=false;
    totalCount: number;

    @Input() set showProgress(showProgress:boolean) {
        this.showLoading=showProgress;
        if(showProgress) {
            this._nodes=[];
        }
    }
    public _searchQuery : string = null;
    _node : Node;
    public hasMoreToLoad :boolean ;
    private lastRequestSearch : boolean;

    @Input() selection : Node[];
    _root: WorkspaceRoot;
    @Input() set root (root: WorkspaceRoot) {
        this._root = root;
        if(['MY_FILES', 'SHARED_FILES'].includes(root)) {
            this.possibleSortByFields = RestConstants.POSSIBLE_SORT_BY_FIELDS;
        } else {
            this.possibleSortByFields = RestConstants.POSSIBLE_SORT_BY_FIELDS_SOLR;
        }
        this.storage.get(SessionStorageService.KEY_WORKSPACE_SORT + root, null).subscribe((data) => {
            if (data?.sortBy != null) {
                this.sortBy = data.sortBy;
                this.sortAscending = data.sortAscending;
            } else {
                this.sortBy = RestConstants.CM_NAME;
                this.sortAscending = RestConstants.DEFAULT_SORT_ASCENDING;
            }
        });
    }
    @Input() set current(current : Node) {
        this.setNode(current);

    }
    @Input() set searchQuery(query : any) {
        this.setSearchQuery(query);
    }
    @Output() onOpenNode=new EventEmitter();
    @Output() onViewNode=new EventEmitter();
    @Output() onSelectionChanged=new EventEmitter();
    @Output() onSelectNode=new EventEmitter();
    @Output() onSearchGlobal=new EventEmitter();
    @Output() onDrop=new EventEmitter();
    @Output() onReset=new EventEmitter();
    private path : Node[];
    public drop(event : any) {
        this.onDrop.emit(event);
    }
    searchGlobal() {
        this.onSearchGlobal.emit(this._searchQuery);
    }
    public load(reset : boolean) {
        if(this._node==null && !this._searchQuery)
            return;
        if(this.loading) {
            setTimeout(()=>this.load(reset),10);
            return;
        }
        if(reset) {
            this.hasMoreToLoad = true;
            this._nodes=[];
            this.onSelectionChanged.emit([]);
            this.onReset.emit();
        }
        else if(!this.hasMoreToLoad) {
            return;
        }
        this.loading=true;
        this.showLoading=true;
        // ignore virtual (new) added/uploaded elements
        const offset = this.getRealNodeCount();
        const request: any= {offset,propertyFilter:[
                RestConstants.ALL
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
            ],sortBy:[this.sortBy],sortAscending:this.sortAscending};
        if(this._searchQuery) {
            const query = this._searchQuery;
            this.lastRequestSearch=true;
            /*this.search.searchByProperties([RestConstants.NODE_ID,RestConstants.CM_PROP_TITLE,RestConstants.CM_NAME,RestConstants.LOM_PROP_DESCRIPTION,RestConstants.LOM_PROP_GENERAL_KEYWORD],
              [query,query,query,query,query],[],RestConstants.COMBINE_MODE_OR,RestConstants.CONTENT_TYPE_FILES_AND_FOLDERS, request).subscribe((data : NodeList) => this.addNodes(data,true));*/
            const criterias:any=[];
            criterias.push({property: RestConstants.PRIMARY_SEARCH_CRITERIA, values: [query]});
            if(this._node) {
                criterias.push({property: 'parent', values: [this._node ? this._node.ref.id : '']});
            }
            this.search.search(criterias,[],request,this.connector.getCurrentLogin() && this.connector.getCurrentLogin().isAdmin ? RestConstants.CONTENT_TYPE_ALL : RestConstants.CONTENT_TYPE_FILES_AND_FOLDERS,RestConstants.HOME_REPOSITORY,
                RestConstants.DEFAULT,[],'workspace').subscribe((data:NodeList)=> {
                this.addNodes(data,true);
            },(error:any)=> {
                this.totalCount=0;
                this.handleError(error);
            });
            // this.nodeApi.searchNodes(query,[],request).subscribe((data : NodeList) => this.addNodes(data,true));
        }
        else {
            this.lastRequestSearch=false;
            this.nodeApi.getChildren(this._node.ref.id,[],request).subscribe((data : NodeList) => this.addNodes(data,false),
                (error:any) => {
                    this.totalCount=0;
                    this.handleError(error);
                });
        }
    }

    ngOnDestroy(): void {
        this.temporaryStorage.set(TemporaryStorageService.NODE_RENDER_PARAMETER_LIST, this._nodes);
    }
    private handleError(error: any) {
        if (error.status == 404)
            this.toast.error(null, 'WORKSPACE.TOAST.NOT_FOUND', {id: this._node.ref.id})
        else
            this.toast.error(error);

        this.loading=false;
        this.showLoading=false;
    }
    private addNodes(data: NodeList,wasSearch: boolean) {
        if (this.lastRequestSearch !== wasSearch) {
            return;
        }
        if (data && data.nodes) {
            this.totalCount=data.pagination.total;
            this._nodes=this._nodes.concat(data.nodes);
        }
        if (data.pagination.total === this.getRealNodeCount()) {
            this.hasMoreToLoad = false;
        }
        this.nodesChange.emit(this._nodes);
        this.loading=false;
        this.showLoading=false;
    }
    constructor(
        private connector : RestConnectorService,
        private translate : TranslateService,
        private storage : SessionStorageService,
        private temporaryStorage : TemporaryStorageService,
        private config : ConfigurationService,
        private search : RestSearchService,
        private toast : Toast,
        private nodeApi : RestNodeService) {
        // super(temporaryStorage,['_node','_nodes','sortBy','sortAscending','columns','totalCount','hasMoreToLoad']);
        this.initColumns();
    }
    public getColumns(customColumns:any[],configColumns:string[]) {
        let defaultColumns:ListItem[]=[];
        defaultColumns.push(new ListItem('NODE', RestConstants.CM_NAME));
        defaultColumns.push(new ListItem('NODE', RestConstants.CM_CREATOR));
        defaultColumns.push(new ListItem('NODE', RestConstants.CM_MODIFIED_DATE));
        if(this.connector.getCurrentLogin() ? this.connector.getCurrentLogin().isAdmin : false) {
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

        if(Array.isArray(configColumns)) {
            const configList:ListItem[]=[];
            for(const col of defaultColumns) {
                if(configColumns.indexOf(col.name)!=-1) {
                    col.visible=true;
                    configList.push(col);
                }
            }
            for(const col of defaultColumns) {
                if(configColumns.indexOf(col.name)==-1) {
                    col.visible=false;
                    configList.push(col);
                }
            }
            // sort as defined inside config
            configList.sort((a, b) => {
                let pos1 = configColumns.indexOf(a.name);
                let pos2 = configColumns.indexOf(b.name);
                if(pos1 === -1) pos1 = configColumns.length;
                if(pos2 === -1) pos2 = configColumns.length;
                return pos1 - pos2;
            });
            defaultColumns=configList;
        }
        if(Array.isArray(customColumns)) {
            for(const column of defaultColumns) {
                let add=true;
                for(const column2 of customColumns) {
                    if(column.name==column2.name) {
                        add=false;
                        break;
                    }
                }
                if(add)
                    customColumns.push(column);
            }
            return customColumns;
        }
        return defaultColumns;
    }
    public setSorting(data:any) {
        this.sortBy=data.sortBy;
        this.sortAscending=data.sortAscending;
        this.storage.set(SessionStorageService.KEY_WORKSPACE_SORT + this._root, data);
        this.load(true);
    }
    public onSelection(event : Node[]) {
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
    public doubleClick(node : Node) {
        this.onOpenNode.emit(node);
    }

    private setNode(current: Node) {
        setTimeout(()=> {
            this._searchQuery=null;
            if(!current) {
                this._node=null;
                return;
            }
            if(this.loading) {
                setTimeout(()=>this.setNode(current),10);
                return;
            }
            if(Helper.objectEquals(this._node,current))
                return;
            this._node=current;
            this.load(true);
        });
    }

    private setSearchQuery(query: any) {
        setTimeout(()=> {
            if (query && query.query) {
                this._searchQuery = query.query;
                this._node=query.node;
                this.load(true);
            }
            else {
                this._searchQuery=null;
            }
        });
    }
    canDrop = (event: DropData)=> {
        return event.target.isDirectory;
    }

    private getRealNodeCount() {
        return this._nodes.filter((n) => !n.virtual).length;
    }

    initColumns() {
        this.config.get('workspaceColumns').subscribe((data:string[])=> {
            this.storage.get('workspaceColumns').subscribe((columns:any[])=> {
                this.columns = this.getColumns(columns, data);
            });
        });
    }
}
