import { Injectable } from '@angular/core';
import { SearchConfig, SearchService as SearchApiService } from 'edu-sharing-api';
import { BehaviorSubject } from 'rxjs';
import { SearchFieldService } from 'src/app/common/ui/search-field/search-field.service';
import { ListItem, Node } from '../../core-module/core.module';

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

    private readonly searchConfigSubject = new BehaviorSubject<Partial<SearchConfig>>({});

    constructor(private searchApi: SearchApiService, private searchField: SearchFieldService) {
        this.searchConfigSubject.pipe().subscribe((config) => {
            const { repository, metadataSet } = config;
            if (repository && metadataSet) {
                this.searchField.setMdsInfo({ repository, metadataSet });
            }
        });
    }

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
    }

    setRepository(repository: string): void {
        if (this.searchConfigSubject.value.repository !== repository) {
            this.searchConfigSubject.next({ ...this.searchConfigSubject.value, repository });
        }
    }

    setMetadataSet(metadataSet: string): void {
        if (this.searchConfigSubject.value.metadataSet !== metadataSet) {
            this.searchConfigSubject.next({ ...this.searchConfigSubject.value, metadataSet });
        }
    }
}
