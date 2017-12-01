import {Component, Input, EventEmitter, Output} from '@angular/core';
import {RestNodeService} from "../../../common/rest/services/rest-node.service";
import {Node, NodeList, NodeWrapper} from "../../../common/rest/data-object";
import {RestConstants} from "../../../common/rest/rest-constants";
import {RestHelper} from "../../../common/rest/rest-helper";

@Component({
  selector: 'search-sort-selection',
  templateUrl: 'sort-selection.component.html',
  styleUrls: ['sort-selection.component.scss']
})
export class SearchSortSelectionComponent  {
  @Input() private sortBy : string;
  @Input() public sortCriterias : string;
  @Output() onSortChanged = new EventEmitter();
  @Output() onClose = new EventEmitter();
  setSort(sort:string){
    this.onSortChanged.emit(sort);
    this.onClose.emit();
  }
  cancel(){
    this.onClose.emit();
  }
}

