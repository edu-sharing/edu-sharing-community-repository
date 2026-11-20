import { LiveAnnouncer } from '@angular/cdk/a11y';
import {
    AfterViewInit,
    Component,
    ElementRef,
    HostListener,
    NgZone,
    OnDestroy,
    OnInit,
    ViewChild,
    inject,
} from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { MatChip } from '@angular/material/chips';
import { TranslateService } from '@ngx-translate/core';
import {
    FacetsDict,
    LabeledValue,
    LabeledValuesDict,
    MdsDefinition,
    MdsService,
} from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Subject } from 'rxjs';
import {
    debounceTime,
    distinctUntilChanged,
    filter,
    map,
    switchMap,
    takeUntil,
} from 'rxjs/operators';
import { MdsHelperService, MdsWidgetType } from 'ngx-edu-sharing-ui';
import { SearchFieldFacetsComponent } from '../../../features/mds/mds-editor/search-field-facets/search-field-facets.component';
import {
    MdsWidgetTree,
    Tree,
} from '../../../features/mds/mds-editor/widgets/mds-editor-widget-tree/tree';
import { Values } from '../../../features/mds/types/types';
import { LoadingScreenService } from '../../loading-screen/loading-screen.service';
import { SearchFieldInternalService } from './search-field-internal.service';
import { SearchFieldConfig } from './search-field.service';

/** Counter for DOM ids that must be unique per component instance. */
let nextUniqueId = 0;

@Component({
    selector: 'es-search-field',
    templateUrl: './search-field.component.html',
    styleUrls: ['./search-field.component.scss'],
    standalone: false,
})
export class SearchFieldComponent implements OnInit, OnDestroy, AfterViewInit {
    private elementRef = inject(ElementRef);
    private internal = inject(SearchFieldInternalService);
    private loadingScreen = inject(LoadingScreenService);
    private mdsService = inject(MdsService);
    private ngZone = inject(NgZone);
    private announcer = inject(LiveAnnouncer);
    private translate = inject(TranslateService);

    /** The number of filters visible on the facets overlay. */
    filtersCount: number;
    /** DOM id of the "active filters" heading, referenced by the chip list's aria-labelledby. */
    readonly activeFiltersLabelId = `es-search-field-active-filters-${nextUniqueId}`;
    /** DOM id of the suggestions panel, referenced by the search input's aria-controls. */
    readonly panelId = `es-search-field-panel-${nextUniqueId++}`;
    /** The total number of filters independently of categories of the facets overlay. */
    totalFiltersCount: number;
    config: SearchFieldConfig;

    readonly inputSubject = new BehaviorSubject<ElementRef<HTMLInputElement>>(null);
    private mds: MdsDefinition;
    @ViewChild('input')
    get input(): ElementRef<HTMLInputElement> {
        return this.inputSubject.value;
    }
    set input(value: ElementRef<HTMLInputElement>) {
        this.inputSubject.next(value);
    }
    @ViewChild('panel') private panel: ElementRef<HTMLElement>;
    @ViewChild(SearchFieldFacetsComponent) private searchFieldFacets: SearchFieldFacetsComponent;
    @ViewChild(MatChip) private firstActiveChip: MatChip;

    readonly inputControl = new UntypedFormControl('');
    /** The user clicked the filters button inside the search field. */
    readonly filtersButtonClicked = this.internal.filtersButtonClicked;
    readonly filters$ = this.internal.filters$;
    readonly rawFilters$ = this.internal.rawFilters$;
    readonly categories$ = this.internal.categoriesSubject;
    readonly suggestions$ = this.internal.suggestions$;
    readonly filterBarVisible = this.internal.filterBarVisible;
    readonly filterPanelId = this.internal.filterPanelId;

    showOverlay = new BehaviorSubject(false);
    inhibitOverlay = false;
    hasSuggestions = true;
    readonly mdsInfo$ = this.internal.mdsInfo$;
    inputHasFocus = false;
    isLoading: boolean;
    /**
     * Whether we got any user input into the search field since the overlay was dismissed the last
     * time.
     *
     * We need this to decide whether to open the overlay on new suggestions. In case this component
     * is included multiple times in the DOM, we only want to open the overlay on the instance the
     * user is currently interacting with.
     */
    private inputSinceOverlayDismissed = false;

    private readonly destroyed$ = new Subject<void>();

    constructor() {
        this.mdsInfo$
            .pipe(switchMap((mds) => this.mdsService.getMetadataSet(mds)))
            .subscribe((mds) => (this.mds = mds));
    }

    ngOnInit(): void {
        this.internal.config
            .pipe(takeUntil(this.destroyed$))
            .subscribe((config) => (this.config = config));
        this.internal.searchString
            .pipe(
                takeUntil(this.destroyed$),
                filter((searchString) => this.inputControl.value !== searchString),
            )
            .subscribe((searchString) => this.inputControl.setValue(searchString));
        this.inputControl.valueChanges.subscribe((inputString) => {
            if (inputString !== this.internal.searchString.value) {
                // The value was updated through user interaction and not by the component input
                // `searchString`.
                this.inputSinceOverlayDismissed = true;
                this.internal.searchString.next(inputString);
                this.internal.updateSuggestions(inputString);
                this.internal.searchStringChanged.next(inputString);
            }
        });
        rxjs.combineLatest([this.filters$, this.categories$])
            .pipe(
                takeUntil(this.destroyed$),
                map(([filters, categories]) => this.getFiltersCount(filters, categories)),
            )
            .subscribe((filtersCount) => (this.filtersCount = filtersCount));
        this.filters$
            .pipe(
                takeUntil(this.destroyed$),
                map((filters) => this.getTotalFiltersCount(filters)),
            )
            .subscribe((totalFiltersCount) => (this.totalFiltersCount = totalFiltersCount));
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
                if (this.hasSuggestions && this.inputSinceOverlayDismissed) {
                    this.showOverlay.next(true);
                }
            });
        // Announce the number of suggestions to screen reader users while they type, since the
        // panel appears without moving the focus.
        rxjs.combineLatest([this.suggestions$, this.rawFilters$])
            .pipe(
                map(([suggestions, filters]) => this.getSuggestionsCount(suggestions, filters)),
                debounceTime(500),
                distinctUntilChanged(),
                filter(() => this.inputHasFocus && !!this.inputControl.value),
                // last, so a pending debounced value is dropped instead of flushed on destroy
                takeUntil(this.destroyed$),
            )
            .subscribe((count) =>
                this.announcer.announce(
                    this.translate.instant(
                        count === 0
                            ? 'SEARCH.SUGGESTIONS_NONE'
                            : count === 1
                            ? 'SEARCH.SUGGESTIONS_COUNT_ONE'
                            : 'SEARCH.SUGGESTIONS_COUNT',
                        { count },
                    ),
                ),
            );
        this.showOverlay
            .pipe(
                distinctUntilChanged(),
                filter((showOverlay) => showOverlay === false),
            )
            .subscribe(() => (this.inputSinceOverlayDismissed = false));
        this.loadingScreen
            .observeIsLoading()
            .pipe(takeUntil(this.destroyed$))
            .subscribe((isLoading) => (this.isLoading = isLoading));
    }

    ngAfterViewInit(): void {
        setTimeout(() => {
            this.updateComponentInstance();
        });
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
        if (this.internal.searchFieldComponent.value === this) {
            this.internal.searchFieldComponent.next(null);
        }
    }

    @HostListener('window:resize') onResize() {
        // Keep the component instance pointed to the visible component when there are multiple
        // search-field instances that are shown/hidden with CSS media queries.
        this.updateComponentInstance();
    }

    /**
     * Assigns the component reference of `SearchFieldInternalService` to this instance if this
     * instance is currently visible in the DOM.
     */
    private updateComponentInstance(): void {
        const isVisibleInDom = this.elementRef.nativeElement.offsetParent != null;
        if (isVisibleInDom && this.internal.searchFieldComponent.value !== this) {
            this.internal.searchFieldComponent.next(this);
        }
    }

    onSubmit(): void {
        this.showOverlay.next(false);
        this.internal.triggerSearch({ searchString: this.inputControl.value, cleared: false });
    }

    onClear(): void {
        this.inputControl.setValue('');
        this.internal.triggerSearch({ searchString: null, cleared: true });
    }

    onValuesChange(values: Values): void {
        // A `valuesChange` event from the mds editor means, a suggestion card has been added as
        // filter.
        this.inputControl.setValue('');
        this.internal.setFilterValues(values, { emitValuesChange: true });
        // the activated suggestion disappears from the panel; continue in the search input
        if (this.elementRef.nativeElement.contains(document.activeElement)) {
            this.input.nativeElement.focus();
        }
    }

    onRemoveFilter(property: string, filter: LabeledValue): void {
        this.internal.removeFilter(property, filter);
    }

    /** Whether the suggestions panel below the search field is shown. */
    get panelOpen(): boolean {
        return (
            this.showOverlay.value &&
            (!!this.filtersCount || this.hasSuggestions) &&
            !this.isLoading
        );
    }

    /** Moves the focus from the search input to the first control of the open panel. */
    focusPanel(event: Event): void {
        event.preventDefault();
        if (this.panelOpen) {
            this.focusFirstPanelControl();
        } else if (this.filtersCount || this.hasSuggestions) {
            // open the panel first and move the focus once its content is rendered
            this.showOverlay.next(true);
            setTimeout(() => this.focusFirstPanelControl());
        }
    }

    private focusFirstPanelControl(): void {
        if (this.firstActiveChip) {
            this.firstActiveChip.focus();
        } else {
            this.searchFieldFacets.focus();
        }
    }

    /** Closes the panel once the focus has left the search field and the panel. */
    @HostListener('focusout', ['$event'])
    onFocusOut(event: FocusEvent): void {
        const target = event.relatedTarget as HTMLElement | null;
        if (!target || !this.elementRef.nativeElement.contains(target)) {
            this.showOverlay.next(false);
        }
    }

    /** Closes the panel on clicks elsewhere on the page, including ones that don't move focus. */
    @HostListener('document:click', ['$event'])
    onDocumentClick(event: MouseEvent): void {
        // the event path is fixed at dispatch, so it still holds a suggestion button that was
        // removed from the DOM by its own click handler
        if (
            this.showOverlay.value &&
            !event.composedPath().includes(this.elementRef.nativeElement)
        ) {
            this.showOverlay.next(false);
        }
    }

    /**
     * Esc in the search input or inside the panel closes the panel and leaves the focus in (or
     * returns it to) the search input. Esc on the other buttons of the field is left alone.
     */
    @HostListener('keydown.escape', ['$event'])
    onEscape(event: Event): void {
        const target = event.target as Node;
        const input = this.input.nativeElement;
        const fromInput = target === input;
        const fromPanel = !!this.panel?.nativeElement.contains(target);
        if (!this.panelOpen || (!fromInput && !fromPanel)) {
            return;
        }
        this.showOverlay.next(false);
        if (fromPanel) {
            input.focus();
            // focusing the input would otherwise reopen the panel right away
            this.showOverlay.next(false);
        }
        // the key press is consumed here, so page-level Esc handlers (sidebars, dialogs) and the
        // browser's clearing of the search input do not act on it as well
        event.preventDefault();
        event.stopPropagation();
    }

    onInputFocus(): void {
        void Promise.resolve().then(() => (this.inputHasFocus = true));
        if (!this.inhibitOverlay) {
            this.showOverlay.next(true);
        }
    }

    onInputBlur(): void {
        this.inputHasFocus = false;
    }

    /** the "Filters" button element, for returning focus on panel close */
    filtersButtonElement(): HTMLElement | null {
        return this.elementRef.nativeElement.querySelector('.filters-button');
    }

    onFiltersButtonClicked(): void {
        this.inhibitOverlay = true;
        this.filtersButtonClicked.next();
        this.ngZone.runOutsideAngular(() => setTimeout(() => (this.inhibitOverlay = false)));
    }

    onCategories(properties: string[]): void {
        this.internal.categoriesSubject.next(properties);
    }

    private getHasSuggestions(suggestions: FacetsDict): boolean {
        return (
            !!suggestions &&
            Object.values(suggestions).some((suggestion) => suggestion.values.length > 0)
        );
    }

    /** Number of suggestions shown in the panel, i.e. without values already set as filter. */
    private getSuggestionsCount(suggestions: FacetsDict, filters: Values | null): number {
        if (!suggestions) {
            return 0;
        }
        return Object.entries(suggestions)
            .map(
                ([property, facet]) =>
                    facet.values.filter((value) => !filters?.[property]?.includes(value.value))
                        .length,
            )
            .reduce((a, b) => a + b, 0);
    }

    getFiltersCount(filters: LabeledValuesDict | null, categories: string[]): number {
        if (!filters) {
            return 0;
        }
        const mapped = Object.keys(filters)
            .filter((f) => categories?.includes(f))
            .map((k) => filters[k].length);
        if (!mapped.length) {
            return 0;
        } else {
            return mapped.reduce((a, b) => a + b);
        }
    }

    getTotalFiltersCount(filters: LabeledValuesDict | null): number {
        if (!filters) {
            return 0;
        }
        const mapped = Object.keys(filters).map((k) => filters[k].length);
        if (!mapped.length) {
            return 0;
        } else {
            return mapped.reduce((a, b) => a + b);
        }
    }

    getTooltip(property: string, value: LabeledValue) {
        const widget = MdsHelperService.getWidget(
            property,
            undefined,
            this.mds.widgets,
        ) as MdsWidgetTree;
        if (
            [
                MdsWidgetType.MultiValueTree.toString(),
                MdsWidgetType.SingleValueTree.toString(),
            ].includes(widget.type)
        ) {
            if (!widget.tree) {
                // build up tree if not yet present
                widget.tree = Tree.generateTree(widget.values);
            }
            return widget.tree.toDisplayValue(value.value).hint;
        }
        return null;
    }
}
