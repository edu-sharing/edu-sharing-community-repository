import { Injectable } from '@angular/core';
import { Node } from '../../core-module/core.module';
import { ListItem } from '../../core-module/core.module';
import { BehaviorSubject } from 'rxjs';
import {NodeDataSource} from '../../core-ui-module/components/node-entries-wrapper/node-data-source';
import {NodeEntriesDisplayType} from '../../core-ui-module/components/node-entries-wrapper/node-entries-wrapper.component';

/**
 * Session state for search.component.
 *
 * The service is provided by app.module. Member variables are almost exclusively used by
 * search.component with the exception of `reinit`, which is set by node-render.component.
 */
@Injectable()
export class SearchService {
    searchTerm: string = '';
    dataSourceSearchResult: {[key: number]: NodeDataSource<Node>} = [];
    searchResultRepositories: Node[][] = [];
    dataSourceCollections = new NodeDataSource<Node>();
    columns: ListItem[] = [];
    collectionsColumns: ListItem[] = [];
    ignored: Array<string> = [];
    reurl: string;
    facettes: Array<any> = [];
    autocompleteData: any = [];
    numberofresults: number = 0;
    offset: number = 0;
    complete: boolean = false;
    showchosenfilters: boolean = false;
    displayType: NodeEntriesDisplayType = null;
    // Used by node-render.component
    reinit = true;
    sidenavSet = false;
    sidenavOpened$ = new BehaviorSubject(false);
    set sidenavOpened(value: boolean) {
        this.sidenavOpened$.next(value);
    }
    get sidenavOpened(): boolean {
        return this.sidenavOpened$.value;
    }
    ex: boolean;
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
        this.offset = 0;
        this.dataSourceSearchResult = [new NodeDataSource<Node>()];
        this.dataSourceCollections.reset();
        this.searchResultRepositories = [];
        this.complete = false;
        this.facettes = [];
    }
}
