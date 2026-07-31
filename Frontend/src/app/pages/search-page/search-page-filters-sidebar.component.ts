import { BreakpointObserver } from '@angular/cdk/layout';
import {
    Component,
    OnDestroy,
    OnInit,
    TemplateRef,
    ViewChild,
    inject,
    signal,
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { ConfigService } from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { Subject } from 'rxjs';
import { map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { CardDialogRef } from '../../features/dialogs/card-dialog/card-dialog-ref';
import { DialogsService } from '../../features/dialogs/dialogs.service';
import { SearchPageService } from './search-page.service';
import { SearchFieldInternalService } from '../../main/navigation/search-field/search-field-internal.service';

/** Width used when `searchFilterBarWidth` is not configured. */
const DEFAULT_FILTER_BAR_WIDTH_PX = 319;

@Component({
    selector: 'es-search-page-filters-sidebar',
    templateUrl: './search-page-filters-sidebar.component.html',
    styleUrls: ['./search-page-filters-sidebar.component.scss'],
    standalone: false,
})
export class SearchPageFiltersSidebarComponent implements OnInit, OnDestroy {
    private searchPage = inject(SearchPageService);
    private dialogs = inject(DialogsService);
    private translate = inject(TranslateService);
    private breakpointObserver = inject(BreakpointObserver);
    private searchFieldInternalService = inject(SearchFieldInternalService);
    private configService = inject(ConfigService);

    @ViewChild('filtersDialogContent', { static: true }) filtersDialogContent: TemplateRef<unknown>;
    @ViewChild('filtersDialogResetButton', { static: true })
    filtersDialogResetButton: TemplateRef<HTMLElement>;

    readonly searchFilters = this.searchPage.searchFilters;
    readonly filterBarIsVisible = this.searchPage.filterBarIsVisible;
    readonly showingAllRepositories = this.searchPage.showingAllRepositories;
    readonly isMobileScreen = this.getIsMobileScreen();
    /**
     * Initial width of the resizable filter bar, configurable via `searchFilterBarWidth`.
     * `null` until the config is resolved, the filter bar is rendered only afterwards so the
     * resizable directive picks up the value on its first initialization.
     */
    readonly defaultWidthPx = signal<number>(null);
    private readonly destroyed = new Subject<void>();

    async ngOnInit(): Promise<void> {
        this.registerFilterDialog();
        this.defaultWidthPx.set(
            await this.configService.get<number>(
                'searchFilterBarWidth',
                DEFAULT_FILTER_BAR_WIDTH_PX,
            ),
        );
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
