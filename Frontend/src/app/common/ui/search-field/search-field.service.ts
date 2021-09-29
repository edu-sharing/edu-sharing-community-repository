import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';

export interface Category {
    property: string;
    label: string;
    color: string;
}

export interface Filter {
    value: string;
    label: string;
}

export type Filters = { [property: string]: Filter[] };
export type Suggestions = Filters;

const DUMMY_CATEGORIES: Category[] = [
    { property: 'ccm:foo', label: 'foo', color: 'rgb(214, 166, 214)' },
    { property: 'ccm:bar', label: 'bar', color: 'rgb(166, 192, 214)' },
];

const DUMMY_FILTERS: Filters = {
    'ccm:foo': [
        { value: 'fooValue', label: 'foo' },
        { value: 'barValue', label: 'bar' },
    ],
    'ccm:bar': [
        { value: 'fooValue', label: 'foo' },
        { value: 'bazValue', label: 'bazzzzzzzzzz' },
    ],
};

const DUMMY_SUGGESTIONS: Suggestions = {
    'ccm:foo': [
        { value: 'fufuValue', label: 'fufu' },
        { value: 'babaValue', label: 'baba' },
    ],
    'ccm:bar': [
        { value: 'fufuValue', label: 'fufu' },
        { value: 'bazzValue', label: 'bazz' },
    ],
};

@Injectable({
    providedIn: 'root',
})
export class SearchFieldService {
    readonly categories$: Observable<Category[]>;
    readonly filters$: Observable<Filters>;
    readonly suggestions$: Observable<Suggestions>;

    private readonly filtersSubject = new BehaviorSubject<Filters>(DUMMY_FILTERS);
    // private readonly filtersSubject = new BehaviorSubject<Filters>({});

    constructor() {
        this.filters$ = this.filtersSubject.asObservable();
        this.categories$ = of(DUMMY_CATEGORIES);
        this.suggestions$ = of(DUMMY_SUGGESTIONS);
    }

    addFilter(property: string, filter: Filter): void {
        const filterList = this.filtersSubject.value[property] ?? [];
        if (!filterList.some((f) => f.value === filter.value)) {
            this.filtersSubject.next({
                ...this.filtersSubject.value,
                [property]: [...filterList, filter],
            });
        }
        this.triggerSearch();
    }

    removeFilter(property: string, filter: Filter): void {
        const filterList = this.filtersSubject.value[property];
        const index = filterList?.findIndex((f) => f.value === filter.value);
        if (index >= 0) {
            const filterCopy = filterList.slice();
            filterCopy.splice(index, 1);
            this.filtersSubject.next({ ...this.filtersSubject.value, [property]: filterCopy });
        }
        this.triggerSearch();
    }

    private triggerSearch(): void {
        console.log('trigger search');
        // TODO
    }
}
