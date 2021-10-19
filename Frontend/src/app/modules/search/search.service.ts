import { Injectable } from '@angular/core';
import { Node } from '../../core-module/core.module';
import { ListItem } from '../../core-module/core.module';
import { BehaviorSubject } from 'rxjs';

/**
 * Session state for search.component.
 *
 * The service is provided by app.module. Member variables are almost exclusively used by
 * search.component with the exception of `reinit`, which is set by node-render.component.
 */
@Injectable()
export class SearchService {
    searchTerm: string = '';
    searchResult: Node[] = [];
    searchResultRepositories: Node[][] = [];
    searchResultCollectionsSubject = new BehaviorSubject<Node[]>([]);
    set searchResultCollections(collections: Node[]) {
        this.searchResultCollectionsSubject.next(collections);
    }
    get searchResultCollections(): Node[] {
        return this.searchResultCollectionsSubject.value;
    }
    columns: ListItem[] = [];
    collectionsColumns: ListItem[] = [];
    ignored: Array<string> = [];
    reurl: string;
    facettes: Array<any> = [];
    autocompleteData: any = [];
    skipcount: number[] = [];
    numberofresults: number = 0;
    offset: number = 0;
    complete: boolean = false;
    showchosenfilters: boolean = false;
    // Used by node-render.component
    reinit = true;
    resultCount: any = {};
    sidenavSet = false;
    sidenavOpened$ = new BehaviorSubject(false);
    set sidenavOpened(value: boolean) {
        this.sidenavOpened$.next(value);
    }
    get sidenavOpened(): boolean {
        return this.sidenavOpened$.value;
    }
    showspinner: boolean;
    ex: boolean;
    viewType = -1;
    sort: any = {};
    extendedSearchUsed = false;

    constructor() {}

    clear() {
        this.searchTerm = '';
    }

    init() {
        if (!this.reinit) {
            return;
        }
        this.skipcount = [];
        this.offset = 0;
        this.searchResult = [];
        this.searchResultCollections = [];
        this.searchResultRepositories = [];
        this.complete = false;
        this.facettes = [];
    }
}
