import { APP_BASE_HREF, PathLocationStrategy, PlatformLocation } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { UrlSerializer } from '@angular/router';

const PRESERVED_QUERY_PARAMS = ['reurl'];

// Adapted from
// https://www.bytelimes.com/a-neat-trick-to-globally-preserve-query-params-in-angular-router/
@Injectable()
export class AppLocationStrategy extends PathLocationStrategy {
    private platformLocation: PlatformLocation;
    private urlSerializer = inject(UrlSerializer);

    private get search(): string {
        return this.platformLocation?.search ?? '';
    }

    constructor() {
        const platformLocation = inject(PlatformLocation);
        const _baseHref = inject(APP_BASE_HREF, { optional: true });

        super(platformLocation, _baseHref);

        this.platformLocation = platformLocation;
    }

    prepareExternalUrl(internal: string): string {
        const path = super.prepareExternalUrl(internal);
        const existingURLSearchParams = new URLSearchParams(this.search);
        const preservedEntries = filter(existingURLSearchParams.entries(), ([key]) =>
            PRESERVED_QUERY_PARAMS.includes(key),
        );
        const preservedQueryParams = Object.fromEntries(preservedEntries);
        const urlTree = this.urlSerializer.parse(path);
        const nextQueryParams = urlTree.queryParams;
        urlTree.queryParams = { ...preservedQueryParams, ...nextQueryParams };
        return urlTree.toString();
    }
}

// Adapted from
// https://stackoverflow.com/questions/28718641/transforming-a-javascript-iterable-into-an-array/28718967#28718967
function* filter<T>(iterable: IterableIterator<T>, predicate: (value: T) => boolean) {
    for (var item of iterable) {
        if (predicate(item)) {
            yield item;
        }
    }
}
