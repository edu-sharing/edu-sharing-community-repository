import {Component, Input, Output, EventEmitter} from "@angular/core";
import {RestConstants} from "../../../common/rest/rest-constants";
import {Toast} from "../../../common/ui/toast";
import {ArchiveRestore,Node,NodeList} from "../../../common/rest/data-object";
import {TemporaryStorageService} from "../../../common/services/temporary-storage.service";
import {RestSearchService} from "../../../common/rest/services/rest-search.service";
import {RestIamService} from "../../../common/rest/services/rest-iam.service";
import {ConfigurationService} from "../../../common/services/configuration.service";
import {NodeHelper} from "../../../common/ui/node-helper";
import {RestConnectorService} from "../../../common/rest/services/rest-connector.service";
import {Router} from "@angular/router";
import {UIConstants} from "../../../common/ui/ui-constants";
import {ListItem} from "../../../common/ui/list-item";
import {TranslateService} from "@ngx-translate/core";
import {OptionItem} from "../../../common/ui/actionbar/option-item";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../../../common/ui/ui-animation";
import {ActionbarHelperService} from "../../../common/services/actionbar-helper";
import {HttpClient} from '@angular/common/http';

@Component({
  selector: 'search-node-store',
  templateUrl: 'node-store.component.html',
  styleUrls: ['node-store.component.scss'],
  animations: [
    trigger('fade', UIAnimation.fade()),
    trigger('cardAnimation', UIAnimation.cardAnimation())
  ]
})
export class SearchNodeStoreComponent {

  @Output() onClose=new EventEmitter();
  private selected:Node[] = [];

  public columns : ListItem[]=[];
  public options : OptionItem[]=[];
  public loading=true;
  public actionOptions : OptionItem[]=[];
  public sortBy=RestConstants.CM_NAME;
  public sortAscending=true;
  public nodes: Node[] = [];

  constructor(private search : RestSearchService,
              private toast : Toast,
              private http : HttpClient,
              private router : Router,
              private config : ConfigurationService,
              private connector : RestConnectorService,
              private actionbar : ActionbarHelperService,
              private temporaryStorageService : TemporaryStorageService,
              private translate : TranslateService,
              private iam : RestIamService,
              private service : TemporaryStorageService){
    this.columns.push(new ListItem("NODE",RestConstants.CM_NAME));
    //this.columns.push(new ListItem("NODE",RestConstants.CM_MODIFIED_DATE));
    this.refresh();

}
  public onDoubleClick(node:Node){
    this.temporaryStorageService.set(TemporaryStorageService.NODE_RENDER_PARAMETER_LIST, this.nodes);
    this.temporaryStorageService.set(TemporaryStorageService.NODE_RENDER_PARAMETER_ORIGIN, "node-store");
    this.router.navigate([UIConstants.ROUTER_PREFIX+"render", node.ref.id]);
  }
  public onSelection(data:Node[]){
    this.selected=data;
    this.updateActionOptions();
  }
  public cancel(){
    this.onClose.emit();
  }
  public changeSort(data:any){
      this.sortBy=data.sortBy;
      this.sortAscending=data.sortAscending;
      this.refresh();
  }
  private updateActionOptions() {
    this.actionOptions=[];
    if(this.selected && this.selected.length){
      let download = this.actionbar.createOptionIfPossible('DOWNLOAD', this.selected, (node: Node) => NodeHelper.downloadNodes(this.toast, this.connector, node ? [node] : this.selected));
      /*let download=new OptionItem("WORKSPACE.OPTION.DOWNLOAD", "cloud_download",
        (node: Node) => NodeHelper.downloadNodes(this.toast,this.connector,node ? [node] : this.selected));
        */
      this.actionOptions.push(download);
      this.actionOptions.push(new OptionItem("SEARCH.NODE_STORE.REMOVE_ITEM","delete",()=>{
        this.deleteSelection();
      }));
    }
    else{

    }
    let custom=this.config.instant("nodeStoreOptions");
    NodeHelper.applyCustomNodeOptions(this.toast,this.http,this.connector,custom,this.nodes, this.selected, this.actionOptions,(load:boolean)=>this.loading=load);
  }

  private deleteSelection(position=0) {
    if(position==this.selected.length){
      this.toast.toast('SEARCH.NODE_STORE.REMOVED_ITEMS',{count:position});
      this.loading=false;
      this.refresh();
      return;
    }
    this.loading=true;
    this.iam.removeNodeList(RestConstants.NODE_STORE_LIST,this.selected[position].ref.id).subscribe(()=>{
      this.deleteSelection(position+1);
    });
  }

  private refresh() {
    this.loading=true;
    this.onSelection([]);
    this.iam.getNodeList(RestConstants.NODE_STORE_LIST,{sortBy:[this.sortBy],sortAscending:this.sortAscending}).subscribe((data:NodeList)=>{
      this.nodes=data.nodes;
      this.loading=false;
    });
  }
}
