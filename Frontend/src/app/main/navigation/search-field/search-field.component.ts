import { CdkConnectedOverlay, ConnectedPosition } from '@angular/cdk/overlay';
import {
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild,
} from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatChip } from '@angular/material/chips';
import { FacetsDict, LabeledValue, LabeledValuesDict } from 'ngx-edu-sharing-api';
import { Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';
import { SearchFieldFacetsComponent } from '../../../common/ui/mds-editor/search-field-facets/search-field-facets.component';
import { Values } from '../../../common/ui/mds-editor/types';
import { SearchFieldService } from './search-field.service';

@Component({
    selector: 'es-search-field',
    templateUrl: './search-field.component.html',
    styleUrls: ['./search-field.component.scss'],
})
export class SearchFieldComponent implements OnInit, OnDestroy {
    filtersCount: number;
    @Input()
    set searchString(s: string) {
        this.searchString_ = s;
        this.inputControl.setValue(s);
    }
    get searchString() {
        return this.searchString_;
    }
    private searchString_: string;
    @Output() searchStringChange = new EventEmitter<string>();
    @Input() placeholder: string;
    /**
     * If enabled, shows filters as chips inside the search field and suggests additional filters in
     * an overlay as the user types into the search field.
     *
     * Relies on active filters values being provided to `SearchFieldService` via `setFilterValues`
     * and `filterValuesChange` being handled.
     */
    @Input() enableFiltersAndSuggestions: boolean;
    @Output() search = new EventEmitter<string>();
    @Output() clear = new EventEmitter<void>();

    @ViewChild('input') input: ElementRef;
    @ViewChild(CdkConnectedOverlay) private overlay: CdkConnectedOverlay;
    @ViewChild(SearchFieldFacetsComponent) private searchFieldFacets: SearchFieldFacetsComponent;
    @ViewChild(MatChip) private firstActiveChip: MatChip;

    readonly inputControl = new FormControl('');
    readonly filters$ = this.searchField.filters$;
    readonly rawFilters$ = this.searchField.rawFilters$;
    readonly categories$ = this.searchField.categoriesSubject;
    readonly suggestions$ = this.searchField.suggestions$;
    showOverlay = false;
    hasSuggestions = true;
    readonly overlayPositions: ConnectedPosition[] = [
        {
            originX: 'center',
            originY: 'bottom',
            offsetX: 0,
            offsetY: 4,
            overlayX: 'center',
            overlayY: 'top',
        },
    ];
    readonly mdsInfo$ = this.searchField.mdsInfo$;
    inputHasFocus = false;

    private readonly destroyed$ = new Subject<void>();

    constructor(private searchField: SearchFieldService) {}

    ngOnInit(): void {
        this.searchField.setEnableFiltersAndSuggestions(this.enableFiltersAndSuggestions);
        this.inputControl.valueChanges.subscribe((inputString) => {
            if (inputString !== this.searchString) {
                // The value was updated through user interaction and not by the component input
                // `searchString`.
                this.searchField.updateSuggestions(inputString);
                this.searchStringChange.emit(inputString);
            }
        });
        this.filters$
            .pipe(
                takeUntil(this.destroyed$),
                map((filters) => this.getFiltersCount(filters)),
            )
            .subscribe((filtersCount) => (this.filtersCount = filtersCount));
        this.suggestions$
            .pipe(
                takeUntil(this.destroyed$),
                map((suggestions) => this.getHasSuggestions(suggestions)),
            )
            .subscribe((hasSuggestions) => {
                this.hasSuggestions = hasSuggestions;
                // We only fetch new suggestions when the user types into the search field. In case
                // the user dismissed the suggestions overlay earlier (`showOverlay = false`), this
                // is the time to show it again.
                if (this.hasSuggestions) {
                    this.showOverlay = true;
                }
            });
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    onSubmit(): void {
        this.showOverlay = false;
        this.search.emit(this.inputControl.value);
    }

    onClear(): void {
        this.inputControl.setValue('');
        this.clear.emit();
    }

    onValuesChange(values: Values): void {
        // A `valuesChange` event from the mds editor means, a suggestion card has been added as
        // filter.
        this.inputControl.setValue('');
        this.searchField.setFilterValues(values, { emitValuesChange: true });
    }

    onRemoveFilter(property: string, filter: LabeledValue): void {
        this.searchField.removeFilter(property, filter);
    }

    onOutsideClick(event: MouseEvent): void {
        const clickTarget = event.target as HTMLElement;
        if (!(this.overlay.origin.elementRef.nativeElement as HTMLElement).contains(clickTarget)) {
            this.showOverlay = false;
        }
    }

    focusOverlayIfOpen(event: Event): void {
        if (this.firstActiveChip) {
            this.firstActiveChip._elementRef.nativeElement.focus();
            event.stopPropagation();
            event.preventDefault();
        } else if (this.showOverlay && this.hasSuggestions) {
            this.searchFieldFacets.focus();
            event.stopPropagation();
            event.preventDefault();
        }
    }

    onDetach(): void {
        const focusWasOnOverlay = this.overlay.overlayRef.overlayElement.contains(
            document.activeElement,
        );
        if (focusWasOnOverlay) {
            this.input.nativeElement.focus();
        }
        // Update `showOverlay` if the user closed the overlay by hitting Esc, but leave it if it
        // was detached because we have no suggestions right now. In the latter case, we want to
        // show the overlay again as soon as suggestions become available.
        if (this.hasSuggestions) {
            this.showOverlay = false;
        }
    }

    onInputFocus(): void {
        Promise.resolve().then(() => (this.inputHasFocus = true));
        this.showOverlay = true;
    }

    onInputBlur(event: FocusEvent): void {
        this.inputHasFocus = false;
        if (!this.overlay.overlayRef?.overlayElement.contains(event.relatedTarget as HTMLElement)) {
            this.showOverlay = false;
        }
    }

    onCategories(properties: string[]): void {
        this.searchField.categoriesSubject.next(properties);
    }

    private getHasSuggestions(suggestions: FacetsDict): boolean {
        return (
            suggestions &&
            Object.values(suggestions).some((suggestion) => suggestion.values.length > 0)
        );
    }

    getFiltersCount(filters: LabeledValuesDict | null): number {
        if (!filters) {
            return 0;
        }
        const mapped = Object.keys(filters)
            .filter((f) => this.categories$.value.includes(f))
            .map((k) => filters[k].length);
        if (!mapped.length) {
            return 0;
        } else {
            return mapped.reduce((a, b) => a + b);
        }
    }
}
