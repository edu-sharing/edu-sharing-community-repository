import {Component, Input, EventEmitter, Output} from '@angular/core';
import {RestNodeService} from "../../../common/rest/services/rest-node.service";
import {Node, NodeList, NodeWrapper} from "../../../common/rest/data-object";
import {RestConstants} from "../../../common/rest/rest-constants";
import {TranslateService} from "@ngx-translate/core";
import {RestConnectorService} from "../../../common/rest/services/rest-connector.service";
import {OptionItem} from "../../../common/ui/actionbar/option-item";
import {Toast} from "../../../common/ui/toast";
import {SessionStorageService} from "../../../common/services/session-storage.service";
import {RestSearchService} from "../../../common/rest/services/rest-search.service";
import {ConfigurationService} from "../../../common/services/configuration.service";
import {ListItem} from "../../../common/ui/list-item";

@Component({
  selector: 'workspace-explorer',
  templateUrl: 'explorer.component.html',
  styleUrls: ['explorer.component.scss']
})
export class WorkspaceExplorerComponent  {
  public _nodes : Node[]=[];
  public sortBy : string=RestConstants.CM_NAME;
  public sortAscending=RestConstants.DEFAULT_SORT_ASCENDING;


  public columns : ListItem[]=[];
  @Input() options : OptionItem[]=[];
  @Input() viewType = 0;


  private loading=false;
  public showLoading=false;

  @Input() set showProgress(showProgress:boolean){
    this.showLoading=showProgress;
    if(showProgress){
      this._nodes=[];
    }
  }
  public _searchQuery : string = null;
  private _node : string;
  public hasMoreToLoad :boolean ;
  private offset : number;
  private lastRequestSearch : boolean;
  @Input() selection : Node[];
  @Input() set current(current : string){
   this.setNodeId(current);

  }
  @Input() set searchQuery(query : string){
    this.setSearchQuery(query);
  }
  @Output() onOpenNode=new EventEmitter();
  @Output() onSelectionChanged=new EventEmitter();
  @Output() onListChange=new EventEmitter();
  @Output() onSelectNode=new EventEmitter();
  @Output() onUpdateOptions=new EventEmitter();
  @Output() onDrop=new EventEmitter();
  @Output() onReset=new EventEmitter();
  private path : Node[];
  public updateOptions(node : Node){
    this.onUpdateOptions.emit(node);
  }
  public drop(event : any){
    this.onDrop.emit(event);
  }
  public load(reset : boolean){
    if(this._node==null && !this._searchQuery)
      return;
    if(reset) {
      this.offset = 0;
      this.hasMoreToLoad = true;
      this._nodes=[];
      this.onSelectionChanged.emit([]);
      this.onUpdateOptions.emit();
      this.onReset.emit();
    }
    else if(!this.hasMoreToLoad){
      return;
    }
    else{
      this.offset+=this.connector.numberPerRequest;
    }
    this.loading=true;
    this.showLoading=true;
	let request : any={offset:this.offset,propertyFilter:[
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
	if(this._searchQuery){
    let query="*"+this._searchQuery+"*";
    this.lastRequestSearch=true;
    /*this.search.searchByProperties([RestConstants.NODE_ID,RestConstants.CM_PROP_TITLE,RestConstants.CM_NAME,RestConstants.LOM_PROP_DESCRIPTION,RestConstants.LOM_PROP_GENERAL_KEYWORD],
      [query,query,query,query,query],[],RestConstants.COMBINE_MODE_OR,RestConstants.CONTENT_TYPE_FILES_AND_FOLDERS, request).subscribe((data : NodeList) => this.addNodes(data,true));*/
    let criterias:any=[];
    criterias.push({'property': RestConstants.PRIMARY_SEARCH_CRITERIA, 'values': [query]});
    this.search.search(criterias,[],request,RestConstants.CONTENT_TYPE_ALL,RestConstants.HOME_REPOSITORY,
      RestConstants.DEFAULT,[],'workspace').subscribe((data:NodeList)=>{
      this.addNodes(data,true);
    });
		//this.nodeApi.searchNodes(query,[],request).subscribe((data : NodeList) => this.addNodes(data,true));
	}
	else{
    this.lastRequestSearch=false;
    console.log(this._node);
    this.nodeApi.getChildren(this._node,[],request).subscribe((data : NodeList) => this.addNodes(data,false),
      (error:any) => {
        if (error.status == 404)
          this.toast.error(null, "WORKSPACE.TOAST.NOT_FOUND", {id: this._node})
        else
          this.toast.error(error);

        this.loading=false;
        this.showLoading=false;
      });
	}
  }
  private addNodes(data : NodeList,wasSearch:boolean){
    if(this.lastRequestSearch!=wasSearch)
      return;
      let i=0;
      console.log(data);
      if(data && data.nodes) {
        for (let node of data.nodes) {
          this._nodes.push(node);
          i++;
        }
      }
      if(data.pagination.total==this._nodes.length)
        this.hasMoreToLoad=false;
      this.onListChange.emit(this._nodes);
      this.loading=false;
      this.showLoading=false;
  }
  public columnsChanged(columns:ListItem[]){
    this.storage.set("workspaceColumns",columns);
  }
  constructor(
    private connector : RestConnectorService,
    private translate : TranslateService,
    private storage : SessionStorageService,
    private config : ConfigurationService,
    private search : RestSearchService,
    private toast : Toast,
    private nodeApi : RestNodeService) {
    this.config.get("workspaceColumns").subscribe((data:string[])=> {
      this.storage.get("workspaceColumns").subscribe((columns:any[])=>{
        this.columns = WorkspaceExplorerComponent.getColumns(columns, data);
      });
    });
  }
  public static getColumns(customColumns:any[],configColumns:string[]){
    let defaultColumns:ListItem[]=[];
    defaultColumns.push(new ListItem("NODE", RestConstants.CM_NAME));
    defaultColumns.push(new ListItem("NODE", RestConstants.CM_CREATOR));
    defaultColumns.push(new ListItem("NODE", RestConstants.CM_MODIFIED_DATE));
    let size = new ListItem("NODE", RestConstants.SIZE);
    size.visible = false;
    let created = new ListItem("NODE", RestConstants.CM_PROP_C_CREATED);
    created.visible = false;
    let mediatype = new ListItem("NODE", RestConstants.MEDIATYPE);
    mediatype.visible = false;
    let keywords = new ListItem("NODE", RestConstants.LOM_PROP_GENERAL_KEYWORD);
    keywords.visible = false;
    let dimensions = new ListItem("NODE", RestConstants.DIMENSIONS);
    dimensions.visible = false;
    let version = new ListItem("NODE", RestConstants.LOM_PROP_LIFECYCLE_VERSION);
    version.visible = false;
    let usage = new ListItem("NODE", RestConstants.VIRTUAL_PROP_USAGECOUNT);
    usage.visible = false;
    let license = new ListItem("NODE", RestConstants.CCM_PROP_LICENSE);
    license.visible = false;
    let wfStatus = new ListItem("NODE", RestConstants.CCM_PROP_WF_STATUS);
    wfStatus.visible = false;
    defaultColumns.push(size);
    defaultColumns.push(created);
    defaultColumns.push(mediatype);
    defaultColumns.push(keywords);
    defaultColumns.push(dimensions);
    defaultColumns.push(version);
    defaultColumns.push(usage);
    defaultColumns.push(license);
    defaultColumns.push(wfStatus);

    if(configColumns){
      let configList:ListItem[]=[];
      for(let col of defaultColumns){
        if(configColumns.indexOf(col.name)!=-1){
          col.visible=true;
          configList.push(col);
        }
      }
      for(let col of defaultColumns){
        if(configColumns.indexOf(col.name)==-1){
          col.visible=false;
          configList.push(col);
        }
      }
      defaultColumns=configList;
    }
    if(customColumns){
      for(let column of defaultColumns){
        let add=true;
        for(let column2 of customColumns){
          if(column.name==column2.name){
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
  public setSorting(data:any){
    this.sortBy=data.sortBy;
    this.sortAscending=data.sortAscending;
    this.load(true);
  }
  public onSelection(event : Node[]){
    this.onSelectionChanged.emit(event);
  }
  /*
  private addParentToPath(node : Node,path : string[]) {

    path.splice(1,0,node.ref.id);
    if (node.parent.id==path[0] || node.parent.id==null) {
      this.onOpenNode.emit(node);
      return;
    }
    console.log("searching parents..."+" parent id: "+node.parent.id+", root "+path[0]);
    this.nodeApi.getNodeMetadata(node.parent.id).subscribe((data: NodeWrapper)=> {
      this.addParentToPath(data.node, path);
    });

  }
   */
  public doubleClick(node : Node){
    console.log("doubleclick");
    this.onOpenNode.emit(node);
  }

  public selectItem(node : Node){
    if(node.isDirectory){
      /*let path=this._parentPath.slice();
      if(path.length==1)
        this.addParentToPath(node,path);
      else {
        path.push(node.ref.id);
        this.onOpenPath.emit(path);
      }*/
      //this.onOpenNode.emit(node);
      this.onSelectNode.emit(node);

    }
    else{
      this.onSelectNode.emit(node);
    }
  }


  private setNodeId(current: string) {
    setTimeout(()=>{
      if(this._searchQuery)
        return;
      if(!current) {
        this._node=null;
        return;
      }
      if(this.loading){
        setTimeout(()=>this.setNodeId(current),10);
        return;
      }
      if(this._node==current)
        return;

      this._node=current;
      this._searchQuery=null;
      this.load(true);
    },5);
  }

  private setSearchQuery(query: string) {
    setTimeout(()=> {
      if (this.showLoading) {
        setTimeout(() => this.setSearchQuery(query), 10);
        return;
      }
      this._searchQuery = query;
      if (this._searchQuery) {
        this._node = null;
        this.load(true);
      }
    });
  }
  canDrop = (event:any)=>{
    return event.target.isDirectory;
  }
}
