import {Component, Input} from "@angular/core";
import {RestConstants} from "../../../common/rest/rest-constants";
import {OptionItem} from "../../../common/ui/actionbar/actionbar.component";
import {TranslateService} from "ng2-translate";
import {Toast} from "../../../common/ui/toast";
import {ArchiveRestore,Node} from "../../../common/rest/data-object";
import {TemporaryStorageService} from "../../../common/services/temporary-storage.service";
import {RestSearchService} from "../../../common/rest/services/rest-search.service";
import {ListItem} from "../../../common/ui/list-item";

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
