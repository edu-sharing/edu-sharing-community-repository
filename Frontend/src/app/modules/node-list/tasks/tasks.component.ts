import {Component, Input} from "@angular/core";
import {ListItem, RestConstants} from "../../../core-module/core.module";
import {TranslateService} from "@ngx-translate/core";
import {Toast} from "../../../core-ui-module/toast";
import {ArchiveRestore,Node} from "../../../core-module/core.module";
import {TemporaryStorageService} from "../../../core-module/core.module";
import {RestSearchService} from "../../../core-module/core.module";
import {OptionItem} from "../../../core-ui-module/option-item";

@Component({
  selector: 'tasks',
  templateUrl: 'tasks.component.html'
})
export class TasksMainComponent {

  @Input() isInsideWorkspace = false;
  @Input() searchWorkspace:string;
  public reload : Boolean;
  private selected:Node[] = [];

  public columns : ListItem[]=[];
  public options : OptionItem[]=[];
  public fullscreenLoading:boolean;
  private loadData(currentQuery :string,offset : number,sortBy : string,sortAscending : boolean){
    let criterias:any=[];
    criterias.push({'property': 'ngsearchword', 'values': [currentQuery]});
    return this.search.search(criterias,[],{propertyFilter:[RestConstants.CM_MODIFIED_DATE],offset:offset,sortBy:[sortBy],sortAscending:sortAscending});
    //return this.search.search(currentQuery,"",{propertyFilter:[RestConstants.CM_MODIFIED_DATE],offset:offset,sortBy:[sortBy],sortAscending:sortAscending})
  }
  constructor(private search : RestSearchService,private toast : Toast,private translate : TranslateService,private service : TemporaryStorageService){
    this.columns.push(new ListItem("NODE",RestConstants.CM_NAME));
    this.columns.push(new ListItem("NODE",RestConstants.CM_MODIFIED_DATE));
    //this.options.push(new OptionItem("RECYCLE.OPTION.RESTORE_SINGLE","undo", (node : Node) => this.restoreSingle(node)));
    //this.options.push(new OptionItem("RECYCLE.OPTION.DELETE_SINGLE","delete", (node : Node) => this.deleteSingle(node)));

  }
  public onSelection(data:Node[]){
    this.selected=data;
  }

}
