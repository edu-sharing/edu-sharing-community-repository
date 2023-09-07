import { Injectable, TemplateRef } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

/**
 * Custom templates to replace or extend standard components within the search page.
 */
export interface SearchPageCustomTemplates {
    resultsTop?: TemplateRef<unknown>;
    filterBarBottom?: TemplateRef<unknown>;
}

/**
 * Singleton service for public interfacing with the search page.
 */
@Injectable({
    providedIn: 'root',
})
export class GlobalSearchPageService {
    constructor(private internal: GlobalSearchPageServiceInternal) {}

    /**
     * Register custom templates to replace or extend standard components within the search page.
     */
    setCustomTemplates(customTemplates: SearchPageCustomTemplates): void {
        this.internal.customTemplates.next(customTemplates);
    }
}

/**
 * Internal part of the `GlobalSearchPageService` for use within the search page component and
 * services only.
 */
@Injectable({
    providedIn: 'root',
})
export class GlobalSearchPageServiceInternal {
    readonly customTemplates = new BehaviorSubject<SearchPageCustomTemplates>({});
}
