import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { NodeEntriesDisplayType, Scope, TemporaryStorageService } from 'ngx-edu-sharing-ui';
import { ActionbarComponent } from 'ngx-edu-sharing-ui';
import { SearchPageResultsService } from './search-page-results.service';
import { SearchPageService } from './search-page.service';
import { GlobalSearchPageServiceInternal } from './global-search-page.service';

@Component({
    selector: 'es-search-page-results',
    templateUrl: './search-page-results.component.html',
    styleUrls: ['./search-page-results.component.scss'],
    providers: [SearchPageResultsService],
})
export class SearchPageResultsComponent implements OnInit, OnDestroy {
    readonly Scope = Scope;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;

    @ViewChild(ActionbarComponent)
    set _actionbar(value: ActionbarComponent) {
        // Avoid changed-after-checked error.
        setTimeout(() => (this.actionbar = value));
    }
    actionbar: ActionbarComponent;

    readonly resultsDataSource = this.results.resultsDataSource;
    readonly collectionsDataSource = this.results.collectionsDataSource;
    readonly resultColumns = this.results.resultColumns;
    readonly collectionColumns = this.results.collectionColumns;
    readonly sortConfig = this.results.sortConfig;
    readonly addToCollectionMode = this.searchPage.addToCollectionMode;
    readonly customTemplates = this.globalSearchPageInternal.customTemplates;

    constructor(
        private globalSearchPageInternal: GlobalSearchPageServiceInternal,
        private results: SearchPageResultsService,
        private searchPage: SearchPageService,
        private temporaryStorageService: TemporaryStorageService,
    ) {}

    ngOnInit(): void {
        setTimeout(() => {
            this.searchPage.results = this.results;
            this.searchPage.showingAllRepositories.next(false);
        });
    }

    toggleFilters(): void {
        const filterBarIsVisible = this.searchPage.filterBarIsVisible;
        filterBarIsVisible.setUserValue(!filterBarIsVisible.getValue());
    }

    ngOnDestroy(): void {
        this.temporaryStorageService.set(
            TemporaryStorageService.NODE_RENDER_PARAMETER_DATA_SOURCE,
            this.resultsDataSource,
        );
    }

    getCountClass() {
        if (this.searchPage.searchString.getValue()) {
            return 'count-ngsearchword';
        }
        return '';
    }
}
