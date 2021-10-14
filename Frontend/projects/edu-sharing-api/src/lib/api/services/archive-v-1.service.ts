/* tslint:disable */
/* eslint-disable */
import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { BaseService } from '../base-service';
import { ApiConfiguration } from '../api-configuration';
import { StrictHttpResponse } from '../strict-http-response';
import { RequestBuilder } from '../request-builder';
import { Observable } from 'rxjs';
import { map, filter } from 'rxjs/operators';

import { RestoreResults } from '../models/restore-results';
import { SearchResult } from '../models/search-result';

@Injectable({
    providedIn: 'root',
})
export class ArchiveV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation purge
     */
    static readonly PurgePath = '/archive/v1/purge/{repository}';

    /**
     * Searches for archive nodes.
     *
     * Searches for archive nodes.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `purge()` instead.
     *
     * This method doesn't expect any request body.
     */
    purge$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * archived node
         */
        archivedNodeIds: Array<string>;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, ArchiveV1Service.PurgePath, 'delete');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
            rb.query('archivedNodeIds', params.archivedNodeIds, { style: 'form', explode: true });
        }

        return this.http
            .request(
                rb.build({
                    responseType: 'json',
                    accept: 'application/json',
                }),
            )
            .pipe(
                filter((r: any) => r instanceof HttpResponse),
                map((r: HttpResponse<any>) => {
                    return r as StrictHttpResponse<string>;
                }),
            );
    }

    /**
     * Searches for archive nodes.
     *
     * Searches for archive nodes.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `purge$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    purge(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * archived node
         */
        archivedNodeIds: Array<string>;
    }): Observable<string> {
        return this.purge$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation restore
     */
    static readonly RestorePath = '/archive/v1/restore/{repository}';

    /**
     * restore archived nodes.
     *
     * restores archived nodes. restoreStatus can have the following values: FALLBACK_PARENT_NOT_EXISTS, FALLBACK_PARENT_NO_PERMISSION, DUPLICATENAME, FINE
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `restore()` instead.
     *
     * This method doesn't expect any request body.
     */
    restore$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * archived nodes
         */
        archivedNodeIds: Array<string>;

        /**
         * to target
         */
        target?: string;
    }): Observable<StrictHttpResponse<RestoreResults>> {
        const rb = new RequestBuilder(this.rootUrl, ArchiveV1Service.RestorePath, 'post');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
            rb.query('archivedNodeIds', params.archivedNodeIds, { style: 'form', explode: true });
            rb.query('target', params.target, { style: 'form', explode: true });
        }

        return this.http
            .request(
                rb.build({
                    responseType: 'json',
                    accept: 'application/json',
                }),
            )
            .pipe(
                filter((r: any) => r instanceof HttpResponse),
                map((r: HttpResponse<any>) => {
                    return r as StrictHttpResponse<RestoreResults>;
                }),
            );
    }

    /**
     * restore archived nodes.
     *
     * restores archived nodes. restoreStatus can have the following values: FALLBACK_PARENT_NOT_EXISTS, FALLBACK_PARENT_NO_PERMISSION, DUPLICATENAME, FINE
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `restore$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    restore(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * archived nodes
         */
        archivedNodeIds: Array<string>;

        /**
         * to target
         */
        target?: string;
    }): Observable<RestoreResults> {
        return this.restore$Response(params).pipe(
            map((r: StrictHttpResponse<RestoreResults>) => r.body as RestoreResults),
        );
    }

    /**
     * Path part for operation search
     */
    static readonly SearchPath = '/archive/v1/search/{repository}/{pattern}';

    /**
     * Searches for archive nodes.
     *
     * Searches for archive nodes.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `search()` instead.
     *
     * This method doesn't expect any request body.
     */
    search$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * search pattern
         */
        pattern: string;

        /**
         * maximum items per page
         */
        maxItems?: number;

        /**
         * skip a number of items
         */
        skipCount?: number;

        /**
         * sort properties
         */
        sortProperties?: Array<string>;

        /**
         * sort ascending
         */
        sortAscending?: Array<boolean>;

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;
    }): Observable<StrictHttpResponse<SearchResult>> {
        const rb = new RequestBuilder(this.rootUrl, ArchiveV1Service.SearchPath, 'get');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
            rb.path('pattern', params.pattern, { style: 'simple', explode: false });
            rb.query('maxItems', params.maxItems, { style: 'form', explode: true });
            rb.query('skipCount', params.skipCount, { style: 'form', explode: true });
            rb.query('sortProperties', params.sortProperties, { style: 'form', explode: true });
            rb.query('sortAscending', params.sortAscending, { style: 'form', explode: true });
            rb.query('propertyFilter', params.propertyFilter, { style: 'form', explode: true });
        }

        return this.http
            .request(
                rb.build({
                    responseType: 'json',
                    accept: 'application/json',
                }),
            )
            .pipe(
                filter((r: any) => r instanceof HttpResponse),
                map((r: HttpResponse<any>) => {
                    return r as StrictHttpResponse<SearchResult>;
                }),
            );
    }

    /**
     * Searches for archive nodes.
     *
     * Searches for archive nodes.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `search$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    search(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * search pattern
         */
        pattern: string;

        /**
         * maximum items per page
         */
        maxItems?: number;

        /**
         * skip a number of items
         */
        skipCount?: number;

        /**
         * sort properties
         */
        sortProperties?: Array<string>;

        /**
         * sort ascending
         */
        sortAscending?: Array<boolean>;

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;
    }): Observable<SearchResult> {
        return this.search$Response(params).pipe(
            map((r: StrictHttpResponse<SearchResult>) => r.body as SearchResult),
        );
    }

    /**
     * Path part for operation search_1
     */
    static readonly Search_1Path = '/archive/v1/search/{repository}/{pattern}/{person}';

    /**
     * Searches for archive nodes.
     *
     * Searches for archive nodes.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `search_1()` instead.
     *
     * This method doesn't expect any request body.
     */
    search_1$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * search pattern
         */
        pattern: string;

        /**
         * person
         */
        person: string;

        /**
         * maximum items per page
         */
        maxItems?: number;

        /**
         * skip a number of items
         */
        skipCount?: number;

        /**
         * sort properties
         */
        sortProperties?: Array<string>;

        /**
         * sort ascending
         */
        sortAscending?: Array<boolean>;

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;
    }): Observable<StrictHttpResponse<SearchResult>> {
        const rb = new RequestBuilder(this.rootUrl, ArchiveV1Service.Search_1Path, 'get');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
            rb.path('pattern', params.pattern, { style: 'simple', explode: false });
            rb.path('person', params.person, { style: 'simple', explode: false });
            rb.query('maxItems', params.maxItems, { style: 'form', explode: true });
            rb.query('skipCount', params.skipCount, { style: 'form', explode: true });
            rb.query('sortProperties', params.sortProperties, { style: 'form', explode: true });
            rb.query('sortAscending', params.sortAscending, { style: 'form', explode: true });
            rb.query('propertyFilter', params.propertyFilter, { style: 'form', explode: true });
        }

        return this.http
            .request(
                rb.build({
                    responseType: 'json',
                    accept: 'application/json',
                }),
            )
            .pipe(
                filter((r: any) => r instanceof HttpResponse),
                map((r: HttpResponse<any>) => {
                    return r as StrictHttpResponse<SearchResult>;
                }),
            );
    }

    /**
     * Searches for archive nodes.
     *
     * Searches for archive nodes.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `search_1$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    search_1(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * search pattern
         */
        pattern: string;

        /**
         * person
         */
        person: string;

        /**
         * maximum items per page
         */
        maxItems?: number;

        /**
         * skip a number of items
         */
        skipCount?: number;

        /**
         * sort properties
         */
        sortProperties?: Array<string>;

        /**
         * sort ascending
         */
        sortAscending?: Array<boolean>;

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;
    }): Observable<SearchResult> {
        return this.search_1$Response(params).pipe(
            map((r: StrictHttpResponse<SearchResult>) => r.body as SearchResult),
        );
    }
}
