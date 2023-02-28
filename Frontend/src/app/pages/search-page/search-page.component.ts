import { animate, state, style, transition, trigger } from '@angular/animations';
import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, HostBinding, OnDestroy, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import * as rxjs from 'rxjs';
import { Observable, Subject } from 'rxjs';
import { filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { Node, Repository, RestConstants, UIConstants } from '../../core-module/core.module';
import { UIAnimation } from '../../core-module/ui/ui-animation';
import { Scope } from '../../core-ui-module/option-item';
import { CardDialogRef } from '../../features/dialogs/card-dialog/card-dialog-ref';
import { DialogsService } from '../../features/dialogs/dialogs.service';
import { NodeEntriesWrapperComponent } from '../../features/node-entries/node-entries-wrapper.component';
import { MainNavService } from '../../main/navigation/main-nav.service';
import { notNull } from '../../util/functions';
import { NavigationScheduler } from './navigation-scheduler';
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
export class SearchPageComponent implements OnInit, OnDestroy {
    readonly Scope = Scope;

    @ViewChild('filtersDialogResetButton', { static: true })
    filtersDialogResetButton: TemplateRef<HTMLElement>;
    @ViewChild('filtersDialogContent', { static: true }) filtersDialogContent: TemplateRef<unknown>;
    @ViewChild('nodeEntriesResults') nodeEntriesResults: NodeEntriesWrapperComponent<Node>;

    @HostBinding('class.has-tab-bar') tabBarIsVisible: boolean = null;
    progressBarIsVisible = false;

    readonly availableRepositories = this.searchPage.availableRepositories;
    readonly activeRepository = this.searchPage.activeRepository;
    readonly showingAllRepositories = this.searchPage.showingAllRepositories;
    readonly filterBarIsVisible = this.searchPage.filterBarIsVisible;
    readonly searchString = this.searchPage.searchString;
    readonly searchFilters = this.searchPage.searchFilters;
    readonly loadingProgress = this.searchPage.loadingProgress;
    readonly isMobileScreen = this.getIsMobileScreen();
    private readonly destroyed = new Subject<void>();

    constructor(
        private breakpointObserver: BreakpointObserver,
        private mainNav: MainNavService,
        private searchPage: SearchPageService,
        private dialogs: DialogsService,
        private translate: TranslateService,
        private navigationScheduler: NavigationScheduler,
    ) {
        this.searchPage.init();
    }

    ngOnInit(): void {
        this.initMainNav();
        this.availableRepositories
            .pipe(
                filter(notNull),
                map((availableRepositories) => availableRepositories.length > 1),
            )
            .subscribe((tabBarIsVisible) => (this.tabBarIsVisible = tabBarIsVisible));
        this.registerProgressBarIsVisible();
        this.registerFilterDialog();
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    goToRepository(repository: Repository): void {
        this.activeRepository.setUserValue(repository.id);
        this.navigationScheduler.scheduleNavigation({
            route: [UIConstants.ROUTER_PREFIX, 'search'],
        });
    }

    private registerFilterDialog(): void {
        let dialogRefPromise: Promise<CardDialogRef<unknown>>;
        let isMobileScreen: boolean;
        rxjs.combineLatest([
            this.searchPage.filterBarIsVisible.observeValue().pipe(),
            this.isMobileScreen.pipe(tap((value) => (isMobileScreen = value))),
        ])
            .pipe(takeUntil(this.destroyed))
            .subscribe(async ([filterBarIsVisible]) => {
                if (isMobileScreen && filterBarIsVisible && !dialogRefPromise) {
                    dialogRefPromise = this.openFilterDialog();
                    const dialogRef = await dialogRefPromise;
                    dialogRef.afterClosed().subscribe(() => {
                        dialogRefPromise = null;
                        if (isMobileScreen) {
                            this.filterBarIsVisible.setUserValue(false);
                        }
                    });
                } else if (!isMobileScreen || !filterBarIsVisible) {
                    void dialogRefPromise?.then((dialogRef) => dialogRef.close());
                }
            });
    }

    private async openFilterDialog(): Promise<CardDialogRef<unknown>> {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'SEARCH.FILTERS',
            contentTemplate: this.filtersDialogContent,
            minWidth: 350,
            customHeaderBarContent: this.filtersDialogResetButton,
        });
        this.searchPage.results.totalResults
            .pipe(
                switchMap((results) => this.translate.get('SEARCH.NUMBER_RESULTS', { results })),
                takeUntil(dialogRef.afterClosed()),
            )
            .subscribe((numberResults) => {
                dialogRef.patchConfig({ subtitle: numberResults.toString() });
            });
        return dialogRef;
    }

    private getIsMobileScreen() {
        return this.breakpointObserver
            .observe(['(max-width: 900px)'])
            .pipe(map(({ matches }) => matches));
    }

    private initMainNav(): void {
        this.mainNav.setMainNavConfig({
            title: 'SEARCH.TITLE',
            currentScope: 'search',
            canOpen: true,
            onCreate: (nodes) => this.nodeEntriesResults.addVirtualNodes(nodes),
        });
        const activeRepositoryIsHome: Observable<boolean> = rxjs
            .combineLatest([this.availableRepositories, this.activeRepository.observeValue()])
            .pipe(
                filter(
                    ([availableRepositories, activeRepository]) =>
                        notNull(availableRepositories) && notNull(activeRepository),
                ),
                map(
                    ([availableRepositories, activeRepository]) =>
                        activeRepository === RestConstants.HOME_REPOSITORY ||
                        availableRepositories.find((r) => r.id === activeRepository).isHomeRepo,
                ),
            );
        activeRepositoryIsHome.subscribe((isHome) =>
            this.mainNav.patchMainNavConfig({
                create: { allowed: isHome, allowBinary: true },
            }),
        );
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
