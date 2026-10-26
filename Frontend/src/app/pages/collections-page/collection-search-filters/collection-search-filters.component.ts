import { BreakpointObserver } from '@angular/cdk/layout';
import { NgTemplateOutlet } from '@angular/common';
import {
    Component,
    computed,
    ElementRef,
    inject,
    OnDestroy,
    OnInit,
    TemplateRef,
    viewChild,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { combineLatest, Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';
import { CardDialogRef } from '../../../features/dialogs/card-dialog/card-dialog-ref';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { SearchFieldInternalService } from '../../../main/navigation/search-field/search-field-internal.service';
import { EdgeToggleComponent } from '../../../shared/components/edge-toggle/edge-toggle.component';
import { ResizableSidenavDirective } from '../../editorial-page/resizable-sidenav.directive';
import { CollectionSearchStateService } from '../collection-search-state.service';

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
        MatSlideToggleModule,
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
    readonly hostRef = inject<ElementRef<HTMLElement>>(ElementRef);

    private readonly dialogContent = viewChild.required<TemplateRef<unknown>>('dialogContent');

    private readonly instanceId = nextInstanceId++;
    readonly titleId = `collection-search-filters-${this.instanceId}-title`;
    readonly panelId = `collection-search-filters-${this.instanceId}-panel`;

    readonly includeSubLevels = this.searchState.includeSubLevels;

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

    private readonly destroyed = new Subject<void>();
    private dialogRef: Promise<CardDialogRef<unknown>> | null = null;

    ngOnInit(): void {
        this.searchFieldInternal.filterPanelId.next(this.panelId);
        this.registerDialog();
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
