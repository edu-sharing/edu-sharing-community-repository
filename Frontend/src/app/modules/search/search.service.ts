import { Injectable } from '@angular/core';

@Injectable()
export class SearchService {

  searchTerm: string = '';
  searchResult: Array<any> = [];
  searchResultCollections: Array<any> = [];
  ignored: Array<string> = [];
  reurl: string;
  facettes: Array<any> = [];
  autocompleteData:any = [];
  skipcount: number = 0;
  numberofresults:number = 0;
  offset: number = 0;
  complete: Boolean = false;
  showchosenfilters: boolean = false;

  constructor() {}

   init() {
     this.skipcount = 0;
     this.offset = 0;
     this.searchResult = [];
     this.complete = false;
     this.facettes = [];
   }
}
