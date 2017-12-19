import {Component, Input, Output, EventEmitter} from "@angular/core";
import {RestConstants} from "../../../common/rest/rest-constants";
import {OptionItem} from "../../../common/ui/actionbar/actionbar.component";
import {Toast} from "../../../common/ui/toast";
import {ArchiveRestore,Node,NodeList} from "../../../common/rest/data-object";
import {TemporaryStorageService} from "../../../common/services/temporary-storage.service";
import {RestSearchService} from "../../../common/rest/services/rest-search.service";
import {RestIamService} from "../../../common/rest/services/rest-iam.service";
import {ConfigurationService} from "../../../common/services/configuration.service";
import {NodeHelper} from "../../../common/ui/node-helper";
import {Http} from "@angular/http";
import {RestConnectorService} from "../../../common/rest/services/rest-connector.service";
import {NodeRenderComponent} from "../../../common/ui/node-render/node-render.component";
import {Router} from "@angular/router";
import {UIConstants} from "../../../common/ui/ui-constants";
import {ListItem} from "../../../common/ui/list-item";
import {RestHelper} from "../../../common/rest/rest-helper";
import {DateHelper} from "../../../common/ui/DateHelper";
import {TranslateService} from "@ngx-translate/core";

@Component({
  selector: 'search-save-search',
  templateUrl: 'save-search.component.html',
  styleUrls: ['save-search.component.scss']
})
export class SearchSaveSearchComponent {
  @Input() set searchQuery(searchQuery:string){
    this.setName(searchQuery);
  }
  @Input() set name(name:string){
    if(name)
      this._name=name;
  }

  @Output() onClose=new EventEmitter();
  @Output() onSave=new EventEmitter();
  public _name : string;
  constructor(private search : RestSearchService,
              private toast : Toast,
              private translate : TranslateService
             ){
    this.setName();
  }
  public cancel(){
    this.onClose.emit();
  }
  public save(){
    this.onSave.emit(this._name);
  }
  private setName(searchQuery: string=null) {
    this._name=(searchQuery ? searchQuery : this.translate.instant('SEARCH.SAVE_SEARCH.UNKNOWN_QUERY'))+" - "+DateHelper.formatDate(this.translate,Date.now(),true,false);
  }
}
