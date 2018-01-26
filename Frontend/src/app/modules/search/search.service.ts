import { Injectable } from '@angular/core';
import { Node } from "../../common/rest/data-object";
import {ListItem} from "../../common/ui/list-item";

@Injectable()
export class SearchService {

  searchTerm: string = '';
  searchResult: Node[] = [];
  searchResultRepositories: Node[][] = [];
  searchResultCollections: Node[] = [];
  columns : ListItem[]=[];
  ignored: Array<string> = [];
  reurl: string;
  facettes: Array<any> = [];
  autocompleteData:any = [];
  skipcount: number = 0;
  numberofresults:number = 0;
  offset: number = 0;
  complete: boolean = false;
  showchosenfilters: boolean = false;
  reinit = true;

  constructor() {}

   init() {
    if(!this.reinit)
      return;
     this.skipcount = 0;
     this.offset = 0;
     this.searchResult = [];
     this.searchResultCollections = [];
     this.searchResultRepositories = [];
     this.complete = false;
     this.facettes = [];
   }
}
