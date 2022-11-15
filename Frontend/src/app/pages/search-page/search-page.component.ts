import { AfterViewInit, Component, HostBinding, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { map } from 'rxjs/operators';
import { ListItem, Node, RestConstants } from '../../core-module/core.module';
import { Scope } from '../../core-ui-module/option-item';
import { NodeEntriesDisplayType } from '../../features/node-entries/entries-model';
import { NodeEntriesWrapperComponent } from '../../features/node-entries/node-entries-wrapper.component';
import { MainNavService } from '../../main/navigation/main-nav.service';
import { SearchPageService } from './search-page.service';
import { UserModifiableValuesService } from './user-modifiable-values';

@Component({
    selector: 'es-search-page',
    templateUrl: './search-page.component.html',
    styleUrls: ['./search-page.component.scss'],
    providers: [SearchPageService],
})
export class SearchPageComponent implements OnInit, AfterViewInit {
    readonly Scope = Scope;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;

    @ViewChild('nodeEntriesResults') nodeEntriesResults: NodeEntriesWrapperComponent<Node>;

    filterBarIsVisible = this.userModifiableValues.createBoolean(false);
    @HostBinding('class.has-tab-bar') tabBarIsVisible = false;
    shouldLimitCollectionRows = true;

    readonly columns = [
        new ListItem('NODE', RestConstants.CM_NAME),
        // new ListItem('NODE', RestConstants.CM_CREATOR),
        new ListItem('NODE', RestConstants.CM_MODIFIED_DATE),
    ];
    readonly resultsDataSource = this.searchPage.resultsDataSource;
    readonly collectionsDataSource = this.searchPage.collectionsDataSource;
    readonly availableRepositories = this.searchPage.availableRepositories;
    readonly activeRepository = this.searchPage.activeRepository;

    constructor(
        private searchPage: SearchPageService,
        private mainNav: MainNavService,
        private route: ActivatedRoute,
        private userModifiableValues: UserModifiableValuesService,
    ) {}

    ngOnInit(): void {
        this.initMainNav();
        this.filterBarIsVisible.registerQueryParameter('filterBar', this.route);
        this.availableRepositories
            .pipe(map((availableRepositories) => availableRepositories?.length > 1))
            .subscribe((tabBarIsVisible) => (this.tabBarIsVisible = tabBarIsVisible));
    }

    ngAfterViewInit(): void {
        void this.nodeEntriesResults.initOptionsGenerator({
            // TODO
        });
    }

    private initMainNav(): void {
        this.mainNav.setMainNavConfig({
            title: 'SEARCH.TITLE',
            currentScope: 'search',
            searchEnabled: true,
            searchPlaceholder: 'SEARCH.SEARCH_STUFF',
            canOpen: true,
            // onCreate: (nodes) => this.nodeEntriesResults.addVirtualNodes(nodes),
        });
    }
}
