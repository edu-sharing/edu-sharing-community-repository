import { animate, state, style, transition, trigger } from '@angular/animations';
import { AfterViewInit, Component, HostBinding, OnInit, ViewChild } from '@angular/core';
import { filter, map } from 'rxjs/operators';
import { Node } from '../../core-module/core.module';
import { UIAnimation } from '../../core-module/ui/ui-animation';
import { Scope } from '../../core-ui-module/option-item';
import { NodeEntriesDisplayType } from '../../features/node-entries/entries-model';
import { NodeEntriesWrapperComponent } from '../../features/node-entries/node-entries-wrapper.component';
import { MainNavService } from '../../main/navigation/main-nav.service';
import { SearchPageService } from './search-page.service';

@Component({
    selector: 'es-search-page',
    templateUrl: './search-page.component.html',
    styleUrls: ['./search-page.component.scss'],
    providers: [SearchPageService],
    animations: [
        trigger('fadeOut', [
            state('visible', style({ opacity: 1 })),
            state('hidden', style({ opacity: 0 })),
            transition('visible => hidden', [animate(UIAnimation.ANIMATION_TIME_NORMAL)]),
        ]),
    ],
})
export class SearchPageComponent implements OnInit, AfterViewInit {
    readonly Scope = Scope;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;

    @ViewChild('nodeEntriesResults') nodeEntriesResults: NodeEntriesWrapperComponent<Node>;

    @HostBinding('class.has-tab-bar') tabBarIsVisible = false;
    progressBarIsVisible = false;

    readonly resultsDataSource = this.searchPage.resultsDataSource;
    readonly collectionsDataSource = this.searchPage.collectionsDataSource;
    readonly availableRepositories = this.searchPage.availableRepositories;
    readonly activeRepository = this.searchPage.activeRepository;
    readonly filterBarIsVisible = this.searchPage.filterBarIsVisible;
    readonly loadingProgress = this.searchPage.loadingProgress;
    readonly resultColumns = this.searchPage.resultColumns;
    readonly collectionColumns = this.searchPage.collectionColumns;
    readonly sortConfig = this.searchPage.sortConfig;

    constructor(private searchPage: SearchPageService, private mainNav: MainNavService) {
        this.searchPage.init();
    }

    ngOnInit(): void {
        this.initMainNav();
        this.availableRepositories
            .pipe(map((availableRepositories) => availableRepositories?.length > 1))
            .subscribe((tabBarIsVisible) => (this.tabBarIsVisible = tabBarIsVisible));
        this.registerProgressBarIsVisible();
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
            canOpen: true,
            // onCreate: (nodes) => this.nodeEntriesResults.addVirtualNodes(nodes),
        });
    }

    onProgressBarAnimationEnd(): void {
        if (this.searchPage.loadingProgress.value >= 100) {
            this.progressBarIsVisible = false;
        }
    }

    private registerProgressBarIsVisible(): void {
        this.searchPage.loadingProgress
            .pipe(filter((progress) => progress < 100))
            .subscribe(() => (this.progressBarIsVisible = true));
    }
}
