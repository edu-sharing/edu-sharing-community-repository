import { BreakpointObserver } from '@angular/cdk/layout';
import { NgTemplateOutlet } from '@angular/common';
import {
    afterNextRender,
    Component,
    computed,
    ElementRef,
    inject,
    Injector,
    OnDestroy,
    OnInit,
    TemplateRef,
    viewChild,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { Values } from 'ngx-edu-sharing-ui';
import { combineLatest, Subject } from 'rxjs';
import { debounceTime, map, takeUntil } from 'rxjs/operators';
import { CardDialogRef } from '../../../features/dialogs/card-dialog/card-dialog-ref';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { MdsEditorWrapperComponent } from '../../../features/mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { MdsModule } from '../../../features/mds/mds.module';
import { sameSearchFilters } from '../../../features/topic-page/shared/utils/search-hit-util';
import { SearchFieldInternalService } from '../../../main/navigation/search-field/search-field-internal.service';
import { EdgeToggleComponent } from '../../../shared/components/edge-toggle/edge-toggle.component';
import { ResizableSidenavDirective } from '../../editorial-page/resizable-sidenav.directive';
import {
    COLLECTION_SEARCH_FILTERS_PARAM,
    CollectionSearchStateService,
} from '../collection-search-state.service';

/** Time the metadata filters have to stay unchanged before the search is repeated. */
const FILTER_CHANGE_DEBOUNCE_MS = 300;

let nextInstanceId = 0;

/**
 * Left panel with the settings of the collection search. Its visibility follows the filter button
 * of the search field; on narrow screens the same content opens in a dialog instead.
 */
@Component({
    selector: 'es-collection-search-filters',
    standalone: true,
    imports: [
        FormsModule,
        NgTemplateOutlet,
        MatButtonModule,
        MatSlideToggleModule,
        MdsModule,
        TranslateModule,
        ResizableSidenavDirective,
        EdgeToggleComponent,
    ],
    templateUrl: './collection-search-filters.component.html',
    styleUrls: ['./collection-search-filters.component.scss'],
})
export class CollectionSearchFiltersComponent implements OnInit, OnDestroy {
    private readonly searchState = inject(CollectionSearchStateService);
    private readonly searchFieldInternal = inject(SearchFieldInternalService);
    private readonly breakpointObserver = inject(BreakpointObserver);
    private readonly dialogs = inject(DialogsService);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly injector = inject(Injector);
    readonly hostRef = inject<ElementRef<HTMLElement>>(ElementRef);

    private readonly dialogContent = viewChild.required<TemplateRef<unknown>>('dialogContent');
    private readonly mdsEditor = viewChild(MdsEditorWrapperComponent);

    private readonly instanceId = nextInstanceId++;
    readonly titleId = `collection-search-filters-${this.instanceId}-title`;
    readonly panelId = `collection-search-filters-${this.instanceId}-panel`;

    readonly includeSubLevels = this.searchState.includeSubLevels;
    readonly filters = this.searchState.filters;
    readonly paramsApplied = this.searchState.paramsApplied;
    readonly hasFilters = computed(() => Object.keys(this.filters()).length > 0);

    private readonly visible = toSignal(this.searchFieldInternal.filterBarVisible, {
        initialValue: false,
    });
    private readonly isMobileScreen$ = this.breakpointObserver
        .observe(['(max-width: 900px)'])
        .pipe(map(({ matches }) => matches));
    private readonly isMobile = toSignal(this.isMobileScreen$, { initialValue: false });
    /** A collapsed panel stays rendered so that its width can animate, see `inert` in the template. */
    readonly isCollapsed = computed(() => !this.visible() || this.isMobile());
    readonly isOpen = computed(() => this.visible() && !this.isMobile());
    readonly showToggle = computed(() => !this.isMobile());
    /**
     * The panel and the dialog render the same content, but only the visible one may hold the
     * metadata editor: two editors would both write their values into the search.
     */
    readonly isMobileLayout = this.isMobile;

    private readonly destroyed = new Subject<void>();
    private readonly filterChanges = new Subject<Values>();
    private dialogRef: Promise<CardDialogRef<unknown>> | null = null;

    ngOnInit(): void {
        this.searchFieldInternal.filterPanelId.next(this.panelId);
        this.registerDialog();
        this.filterChanges
            .pipe(debounceTime(FILTER_CHANGE_DEBOUNCE_MS), takeUntil(this.destroyed))
            .subscribe((filters) => this.applyFilters(filters));
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
        this.searchFieldInternal.filterPanelId.next(null);
        void this.dialogRef?.then((ref) => ref.close());
    }

    toggle(): void {
        this.searchFieldInternal.filtersButtonClicked.next();
    }

    setIncludeSubLevels(value: boolean): void {
        void this.router.navigate([], {
            relativeTo: this.route,
            queryParams: { subLevels: value ? null : false },
            queryParamsHandling: 'merge',
        });
    }

    /** Takes over the values of the metadata editor, ignoring the ones without a selection. */
    onFiltersChange(values: Values): void {
        const filters = Object.fromEntries(
            Object.entries(values ?? {}).filter(
                ([, value]) => !(Array.isArray(value) && value.length === 0),
            ),
        );
        if (!sameSearchFilters(filters, this.filters())) {
            this.filterChanges.next(filters);
        }
    }

    resetFilters(): void {
        this.applyFilters({});
        // the editor reads its values only when it is initialized
        afterNextRender(() => void this.mdsEditor()?.reInit(), { injector: this.injector });
    }

    private applyFilters(filters: Values): void {
        if (sameSearchFilters(filters, this.filters())) {
            return;
        }
        this.filters.set(filters);
        void this.router.navigate([], {
            relativeTo: this.route,
            queryParams: {
                [COLLECTION_SEARCH_FILTERS_PARAM]: Object.keys(filters).length
                    ? JSON.stringify(filters)
                    : null,
            },
            queryParamsHandling: 'merge',
            replaceUrl: true,
        });
    }

    private registerDialog(): void {
        combineLatest([this.searchFieldInternal.filterBarVisible, this.isMobileScreen$])
            .pipe(takeUntil(this.destroyed))
            .subscribe(([visible, mobile]) => this.syncDialog(visible && mobile));
    }

    private syncDialog(open: boolean): void {
        if (open && !this.dialogRef) {
            const ref = this.dialogs.openGenericDialog({
                title: 'SEARCH.FILTERS',
                contentTemplate: this.dialogContent(),
                minWidth: 350,
            });
            this.dialogRef = ref;
            void ref.then((dialog) =>
                dialog.afterClosed().subscribe(() => {
                    this.dialogRef = null;
                    if (this.isMobile()) {
                        this.searchFieldInternal.filtersButtonClicked.next();
                    }
                }),
            );
        } else if (!open && this.dialogRef) {
            void this.dialogRef.then((dialog) => dialog.close());
        }
    }
}
