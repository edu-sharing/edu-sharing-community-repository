import {Component, Input, EventEmitter, ViewEncapsulation, Output} from '@angular/core';
import { Observable } from 'rxjs';
import { BrowserModule } from '@angular/platform-browser';
import {Toast} from "../../core-ui-module/toast";
import {ArchiveRestore, RestoreResult, ArchiveSearch, Node, ListItem, Pagination} from '../../core-module/core.module';
import {RestConnectorService} from "../../core-module/core.module";
import {CustomOptions, OptionItem} from "../../core-ui-module/option-item";
import {ActionbarComponent} from "../../common/ui/actionbar/actionbar.component";

@Component({
  selector: 'es-node-list',
  templateUrl: 'node-list.component.html',
  styleUrls: ['node-list.component.scss']
  /*encapsulation:ViewEncapsulation.None*/
})
export class NodeListComponent {
  @Input() isInsideWorkspace = false;
  @Input() searchLabel : string;
  @Input() parent : any;
  @Input() actionbar:ActionbarComponent;
  @Input() customOptions : CustomOptions;
  @Input() set searchWorkspace(query : string){
    if(query && query.trim()) {
      this.currentQuery = query;
      this.hasSearched=true;
      this.doReload();
    }
    else{
      this.searchAll();
    }
  }
  public list : Node[];
  pagination: Pagination;
  _columns : ListItem[];
  @Input() set columns(columns : ListItem[]){
    this._columns=columns;
    if(this._columns && this._columns.length)
      this.sortBy=this._columns[0].name;
  };
  @Input() options : OptionItem[];
  @Input() sortBy : string;
  @Input() sortAscending=true;
  @Input() set reload(reload:Boolean){
    if(reload)
      this.doReload();
}
  @Output() onSelectionChanged = new EventEmitter();
  public hasSearched = false;
  @Input() selected:Node[] = [];
    @Input() fullscreenLoading=false;

    // the current node which has an overlay menu open
    public currentMore : Node;
    public isLoading=false;
    public query : string;
    private currentQuery : string;
	  constructor(private connector : RestConnectorService,
                private toast: Toast) {
        // http://plnkr.co/edit/btpW3l0jr5beJVjohy1Q?p=preview

        /*
        let restoreResult=new ArchiveRestore();
        let r1=new RestoreResult();
        r1.name="Duplicate";r1.restoreStatus=RecycleRestoreComponent.STATUS_DUPLICATENAME;
        let r2=new RestoreResult();
        r2.name="Missing";r2.restoreStatus=RecycleRestoreComponent.STATUS_PARENT_FOLDER_MISSING;
        restoreResult.results=[r1,r2];
        RecycleRestoreComponent.prepareResults(this.translate,restoreResult);
        this.restoreResult=restoreResult;
        */

    }

    setSorting(data:any){
      this.sortBy=data.sortBy;
      this.sortAscending=data.sortAscending;
      this.doReload();
    }
    loadMore() : void{
      if(this.isLoading) {
          return;
        }
      this.search(false);
    }
    public searchField() : void{
        this.currentQuery=this.query;
        this.list=null;
        if(this.query=="")
          this.searchAll();
        else
          this.search(true);
    }

    onSelection(node : Node[]){
      this.selected=node;
      this.onSelectionChanged.emit(node);
    }



    private doReload() : void{
      setTimeout(()=> {
          this.list = null;
          this.search(this.hasSearched);
      });
    }
    private searchAll() : void{
        this.hasSearched=false;
        this.currentQuery="*";
        this.doReload();
    }
    private redo() : void{
        this.query="";
        this.searchAll();
    }


	private search(searched : boolean) : void{
	if(this.isLoading){
	  setTimeout(()=>this.search(searched),10);
	  return;
    }
    this.isLoading=true;

    this.parent.loadData(this.currentQuery,this.list ? this.list.length : 0,this.sortBy,this.sortAscending)
            .subscribe(
				(data:ArchiveSearch) => this.display(data,searched),
              (error:any) => this.handleErrors(error),
                () => console.log('Get all Items complete'));
	}
    private isSelected(node : Node){
        return this.selected.indexOf(node)!=-1;
    }

    private display(data : ArchiveSearch,searched : boolean){
      let list=data.nodes;
      this.pagination = data.pagination;
      if(this.list){
          this.list=this.list.concat(list);
        }
        else{
            this.list=list;
            this.selected=[];
            if(this.list.length==0)
                this.list=null;
        }

        this.hasSearched=searched;
        this.isLoading=false;

    }

  private handleErrors(error: any) {
    this.toast.error(error);
      this.fullscreenLoading=false;
  }

}
