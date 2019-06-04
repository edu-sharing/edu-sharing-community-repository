import { Injectable } from '@angular/core';
import { Node } from "../../core-module/core.module";
import {ListItem} from "../../core-module/core.module";

@Injectable()
export class SearchService {

  searchTerm: string = '';
  searchResult: Node[] = [];
  searchResultRepositories: Node[][] = [];
  searchResultCollections: Node[] = [];
  columns : ListItem[]=[];
  collectionsColumns : ListItem[]=[];
  ignored: Array<string> = [];
  reurl: string;
  facettes: Array<any> = [];
  autocompleteData:any = [];
  skipcount: number[] = [];
  numberofresults:number = 0;
  offset: number = 0;
  complete: boolean = false;
  showchosenfilters: boolean = false;
  reinit = true;
  resultCount:any={};
  sidenavSet = false;
  sidenavOpened = false;
  showspinner: boolean;
  viewType = -1;
  sort:any = {}
  constructor() {}
  clear(){
    this.searchTerm="";
  }
   init() {
    if(!this.reinit)
      return;
     this.skipcount = [];
     this.offset = 0;
     this.searchResult = [];
     this.searchResultCollections = [];
     this.searchResultRepositories = [];
     this.complete = false;
     this.facettes = [];
   }
}
