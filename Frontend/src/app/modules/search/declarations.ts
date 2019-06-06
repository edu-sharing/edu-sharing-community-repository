import {SearchComponent} from "./search.component";
import {AutocompleteComponent, SuggestItem} from "../../common/ui/autocomplete/autocomplete.component";
import {SearchHeaderComponent} from "../../common/ui/header/header.component";
import {SearchNodeStoreComponent} from "./node-store/node-store.component";
import {SearchSaveSearchComponent} from "./save-search/save-search.component";
import {GlobalProgressComponent} from "../../core-ui-module/components/global-progress/global-progress.component";

export const DECLARATIONS_SEARCH = [
  SearchComponent,
  SearchHeaderComponent,
  SearchNodeStoreComponent,
  SearchSaveSearchComponent
];
