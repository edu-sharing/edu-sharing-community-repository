import { BreakpointObserver } from '@angular/cdk/layout';
import {
    AfterViewInit,
    Component,
    DestroyRef,
    EventEmitter,
    Input,
    OnInit,
    Output,
    Signal,
    TemplateRef,
    WritableSignal,
    inject,
    signal,
    viewChild,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { HOME_REPOSITORY } from 'ngx-edu-sharing-api';
import { Observable, map } from 'rxjs';
import { CardDialogRef } from '../../../../features/dialogs/card-dialog/card-dialog-ref';
import { DialogsService } from '../../../../features/dialogs/dialogs.service';
import { MdsEditorWrapperComponent } from '../../../../features/mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { MdsModule } from '../../../../features/mds/mds.module';
import { Values } from '../../../../features/mds/types/types';
import { ResizableSidenavDirective } from '../../../editorial-page/resizable-sidenav.directive';
import { SharedModule } from '../../../../shared/shared.module';
import { GenericWidgetGlobalService } from '../../widgets/generic-widget/generic-widget-global.service';

/**
 * Whether a set of MDS values holds a value at all.
 *
 * @param values
 */
function containsValues(values: Values): boolean {
    return Object.keys(values ?? {}).length > 0;
}

@Component({
    selector: 'es-topic-page-filters-sidebar',
    imports: [SharedModule, MdsModule, ResizableSidenavDirective],
    templateUrl: './topic-page-filters-sidebar.component.html',
    styleUrls: ['./topic-page-filters-sidebar.component.scss'],
    host: { '[class.as-dialog]': 'isMobileScreen()' },
})
export class TopicPageFiltersSidebarComponent implements OnInit, AfterViewInit {
    genericWidgetGlobalService = inject(GenericWidgetGlobalService);
    private readonly breakpointObserver = inject(BreakpointObserver);
    private readonly destroyRef = inject(DestroyRef);
    private readonly dialogs = inject(DialogsService);

    // the width below which the search page shows its filters as a dialog as well
    private readonly isMobileScreen$: Observable<boolean> = this.breakpointObserver
        .observe(['(max-width: 900px)'])
        .pipe(map(({ matches }) => matches));
    protected readonly isMobileScreen: Signal<boolean> = toSignal(this.isMobileScreen$, {
        initialValue: false,
    });
    private readonly filtersTemplate: Signal<TemplateRef<unknown>> =
        viewChild.required<TemplateRef<unknown>>('filtersTemplate');
    private readonly resetButtonTemplate: Signal<TemplateRef<HTMLElement>> =
        viewChild.required<TemplateRef<HTMLElement>>('resetButtonTemplate');
    // the open filter dialog; cleared before a programmatic close, so only a user close emits
    private filterDialog: Promise<CardDialogRef<unknown>> | null = null;

    private readonly mdsEditor: Signal<MdsEditorWrapperComponent | undefined> =
        viewChild(MdsEditorWrapperComponent);
    // whether any filter is set, which is what the reset button is offered for
    protected readonly hasFilters: WritableSignal<boolean> = signal(false);

    // the values the MDS editor starts with
    @Input() currentValues: Values = {};

    @Output() closeFilterbar: EventEmitter<void> = new EventEmitter<void>();
    @Output() currentValuesChange: EventEmitter<Values> = new EventEmitter<Values>();

    ngOnInit(): void {
        this.hasFilters.set(containsValues(this.currentValues));
    }

    ngAfterViewInit(): void {
        this.isMobileScreen$
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((isMobileScreen: boolean): void => {
                if (isMobileScreen) {
                    this.openFilterDialog();
                } else {
                    this.closeFilterDialog();
                }
            });
        this.destroyRef.onDestroy((): void => this.closeFilterDialog());
    }

    /**
     * Reacts to the currentValuesChange event and emits it the same way.
     *
     * @param selectedValues
     */
    applySearchFilters(selectedValues: Values): void {
        selectedValues = Object.fromEntries(
            Object.entries(selectedValues).filter(([, value]) => value && value.length > 0),
        );
        this.hasFilters.set(containsValues(selectedValues));
        this.currentValuesChange.emit(selectedValues);
    }

    /**
     * Drops every filter, both in the MDS editor and for the listeners of this sidebar.
     */
    protected async resetFilters(): Promise<void> {
        // the editor reads its values on initialization only
        this.currentValues = {};
        // the input binding passes the values on with the next change detection, after the re-init
        const editor = this.mdsEditor();
        if (editor) {
            editor.currentValues = this.currentValues;
        }
        await this.mdsEditor()?.reInit();
        this.applySearchFilters({});
    }

    /**
     * Shows the filters in a dialog; closing it closes the filter bar.
     */
    private openFilterDialog(): void {
        if (this.filterDialog) {
            return;
        }
        const filterDialog = this.dialogs.openGenericDialog({
            title: 'SEARCH.FILTERS',
            contentTemplate: this.filtersTemplate(),
            minWidth: 350,
            customHeaderBarContent: this.resetButtonTemplate(),
        });
        this.filterDialog = filterDialog;
        void filterDialog.then((dialogRef: CardDialogRef<unknown>): void => {
            dialogRef.afterClosed().subscribe((): void => {
                if (this.filterDialog === filterDialog) {
                    this.filterDialog = null;
                    this.closeFilterBar();
                }
            });
        });
    }

    /**
     * Closes the filter dialog without closing the filter bar.
     */
    private closeFilterDialog(): void {
        const filterDialog = this.filterDialog;
        this.filterDialog = null;
        void filterDialog?.then((dialogRef: CardDialogRef<unknown>): void => dialogRef.close());
    }

    /**
     * Emits a close event for the filter bar.
     */
    closeFilterBar(): void {
        this.closeFilterbar.emit();
    }

    protected readonly HOME_REPOSITORY = HOME_REPOSITORY;
}
