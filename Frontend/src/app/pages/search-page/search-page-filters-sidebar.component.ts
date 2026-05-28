import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, OnDestroy, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import * as rxjs from 'rxjs';
import { Subject } from 'rxjs';
import { map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { CardDialogRef } from '../../features/dialogs/card-dialog/card-dialog-ref';
import { DialogsService } from '../../features/dialogs/dialogs.service';
import { SearchPageService } from './search-page.service';
import { SearchFieldInternalService } from '../../main/navigation/search-field/search-field-internal.service';

@Component({
    selector: 'es-search-page-filters-sidebar',
    templateUrl: './search-page-filters-sidebar.component.html',
    styleUrls: ['./search-page-filters-sidebar.component.scss'],
    standalone: false,
})
export class SearchPageFiltersSidebarComponent implements OnInit, OnDestroy {
    @ViewChild('filtersDialogContent', { static: true }) filtersDialogContent: TemplateRef<unknown>;
    @ViewChild('filtersDialogResetButton', { static: true })
    filtersDialogResetButton: TemplateRef<HTMLElement>;

    readonly searchFilters = this.searchPage.searchFilters;
    readonly filterBarIsVisible = this.searchPage.filterBarIsVisible;
    readonly showingAllRepositories = this.searchPage.showingAllRepositories;
    readonly isMobileScreen = this.getIsMobileScreen();
    private readonly destroyed = new Subject<void>();

    constructor(
        private searchPage: SearchPageService,
        private dialogs: DialogsService,
        private translate: TranslateService,
        private breakpointObserver: BreakpointObserver,
        private searchFieldInternalService: SearchFieldInternalService,
    ) {}

    ngOnInit(): void {
        this.registerFilterDialog();
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    private registerFilterDialog(): void {
        let dialogRefPromise: Promise<CardDialogRef<unknown>>;
        let isMobileScreen: boolean;
        rxjs.combineLatest([
            this.searchFieldInternalService.filterBarVisible,
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

    private getIsMobileScreen() {
        return this.breakpointObserver
            .observe(['(max-width: 900px)'])
            .pipe(map(({ matches }) => matches));
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
}
