import { Component, OnInit, inject } from '@angular/core';
import { debounceTime, map } from 'rxjs/operators';
import { SearchPageResultsAllService } from './search-page-results-all.service';
import { SearchPageService } from './search-page.service';
import {
    InteractionType,
    NodeClickEvent,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
    Scope,
} from 'ngx-edu-sharing-ui';
import { ConfigService, Node } from 'ngx-edu-sharing-api';
import { EditorialSidebarService } from '../../features/editorial-sidebar/editorial-sidebar.service';
import { SelectionChange } from '@angular/cdk/collections';

@Component({
    selector: 'es-search-page-results-all',
    templateUrl: './search-page-results-all.component.html',
    styleUrls: ['./search-page-results-all.component.scss'],
    providers: [SearchPageResultsAllService],
    standalone: false,
})
export class SearchPageResultsAllComponent implements OnInit {
    searchPage = inject(SearchPageService);
    private _results = inject(SearchPageResultsAllService);
    private configService = inject(ConfigService);
    private editorialSidebarService = inject(EditorialSidebarService);

    readonly Scope = Scope;
    readonly InteractionType = InteractionType;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;

    readonly repoData = this._results.repoData;
    readonly loadingProgress = this._results.loadingProgress;
    readonly addToCollectionMode = this.searchPage.addToCollectionMode;
    allEmpty = false;

    readonly onDblClick = this._results.onDblClick;
    private readonly _searchString = this.searchPage.searchString;
    private readonly _activeRepository = this.searchPage.activeRepository;
    previewMode: string | 'Sidebar' | 'RenderingPage';

    async ngOnInit() {
        setTimeout(() => {
            this.searchPage.results = this._results;
            this.searchPage.showingAllRepositories.next(true);
        });
        this._registerAllEmpty();
        this.previewMode = await this.configService.get('searchPreviewMode', 'Sidebar');
    }

    getShowMoreQueryParams(repoId: string): { [key: string]: string } {
        return {
            ...this._searchString.getQueryParamEntry(),
            ...this._activeRepository.getQueryParamEntry(repoId),
        };
    }

    onClick(ref: NodeEntriesWrapperComponent<Node>, event: NodeClickEvent<Node>) {
        this.editorialSidebarService.handleSelect(ref, event, Scope.Search);
        this._results.onClick(event.element);
    }

    private _registerAllEmpty(): void {
        this.loadingProgress
            .pipe(
                debounceTime(0),
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

    selectionChange(selection: SelectionChange<Node>) {
        this.searchPage.selection.next(selection.source.selected);
        this.editorialSidebarService.handleSelection(selection);
    }
}
