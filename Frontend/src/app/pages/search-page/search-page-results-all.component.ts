import { Component, OnDestroy, OnInit } from '@angular/core';
import * as rxjs from 'rxjs';
import { delay, map, switchMap } from 'rxjs/operators';
import { Scope } from '../../core-ui-module/option-item';
import { NodeEntriesDisplayType } from '../../features/node-entries/entries-model';
import { SearchPageResultsAllService } from './search-page-results-all.service';
import { SearchPageService } from './search-page.service';

@Component({
    selector: 'es-search-page-results-all',
    templateUrl: './search-page-results-all.component.html',
    styleUrls: ['./search-page-results-all.component.scss'],
    providers: [SearchPageResultsAllService],
})
export class SearchPageResultsAllComponent implements OnInit {
    readonly Scope = Scope;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;

    readonly repoData = this._results.repoData;
    readonly loadingProgress = this._results.loadingProgress;
    readonly addToCollectionMode = this._searchPage.addToCollectionMode;
    allEmpty = false;

    private readonly _searchString = this._searchPage.searchString;
    private readonly _activeRepository = this._searchPage.activeRepository;

    constructor(
        private _searchPage: SearchPageService,
        private _results: SearchPageResultsAllService,
    ) {}

    ngOnInit(): void {
        setTimeout(() => {
            this._searchPage.results = this._results;
            this._searchPage.showingAllRepositories.next(true);
        });
        this._registerAllEmpty();
    }

    getShowMoreQueryParams(repoId: string): { [key: string]: string } {
        return {
            ...this._searchString.getQueryParamEntry(),
            ...this._activeRepository.getQueryParamEntry(repoId),
        };
    }

    private _registerAllEmpty(): void {
        this.loadingProgress
            .pipe(
                delay(0),
                map((progress) => {
                    if (progress < 100) {
                        return false;
                    } else {
                        return this.repoData.value.every((r) => r.dataSource.isEmpty());
                    }
                }),
            )
            .subscribe((allEmpty) => (this.allEmpty = allEmpty));
    }
}
