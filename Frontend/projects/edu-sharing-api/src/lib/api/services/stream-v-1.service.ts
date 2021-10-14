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

import { StreamEntryInput } from '../models/stream-entry-input';
import { StreamList } from '../models/stream-list';

@Injectable({
    providedIn: 'root',
})
export class StreamV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation canAccess
     */
    static readonly CanAccessPath = '/stream/v1/access/{repository}/{node}';

    /**
     * test.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `canAccess()` instead.
     *
     * This method doesn't expect any request body.
     */
    canAccess$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * The property to aggregate
         */
        node: string;
    }): Observable<
        StrictHttpResponse<{
            [key: string]: {};
        }>
    > {
        const rb = new RequestBuilder(this.rootUrl, StreamV1Service.CanAccessPath, 'get');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
            rb.path('node', params.node, { style: 'simple', explode: false });
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
                    return r as StrictHttpResponse<{
                        [key: string]: {};
                    }>;
                }),
            );
    }

    /**
     * test.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `canAccess$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    canAccess(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * The property to aggregate
         */
        node: string;
    }): Observable<{
        [key: string]: {};
    }> {
        return this.canAccess$Response(params).pipe(
            map(
                (
                    r: StrictHttpResponse<{
                        [key: string]: {};
                    }>,
                ) =>
                    r.body as {
                        [key: string]: {};
                    },
            ),
        );
    }

    /**
     * Path part for operation addEntry
     */
    static readonly AddEntryPath = '/stream/v1/add/{repository}';

    /**
     * add a new stream object.
     *
     * will return the object and add the id to the object if creation succeeded
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `addEntry()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    addEntry$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * Stream object to add
         */
        body: StreamEntryInput;
    }): Observable<StrictHttpResponse<StreamEntryInput>> {
        const rb = new RequestBuilder(this.rootUrl, StreamV1Service.AddEntryPath, 'put');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
            rb.body(params.body, 'application/json');
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
                    return r as StrictHttpResponse<StreamEntryInput>;
                }),
            );
    }

    /**
     * add a new stream object.
     *
     * will return the object and add the id to the object if creation succeeded
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `addEntry$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    addEntry(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * Stream object to add
         */
        body: StreamEntryInput;
    }): Observable<StreamEntryInput> {
        return this.addEntry$Response(params).pipe(
            map((r: StrictHttpResponse<StreamEntryInput>) => r.body as StreamEntryInput),
        );
    }

    /**
     * Path part for operation deleteEntry
     */
    static readonly DeleteEntryPath = '/stream/v1/delete/{repository}/{entry}';

    /**
     * delete a stream object.
     *
     * the current user must be author of the given stream object
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `deleteEntry()` instead.
     *
     * This method doesn't expect any request body.
     */
    deleteEntry$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * entry id to delete
         */
        entry: string;
    }): Observable<StrictHttpResponse<void>> {
        const rb = new RequestBuilder(this.rootUrl, StreamV1Service.DeleteEntryPath, 'delete');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
            rb.path('entry', params.entry, { style: 'simple', explode: false });
        }

        return this.http
            .request(
                rb.build({
                    responseType: 'text',
                    accept: '*/*',
                }),
            )
            .pipe(
                filter((r: any) => r instanceof HttpResponse),
                map((r: HttpResponse<any>) => {
                    return (r as HttpResponse<any>).clone({
                        body: undefined,
                    }) as StrictHttpResponse<void>;
                }),
            );
    }

    /**
     * delete a stream object.
     *
     * the current user must be author of the given stream object
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `deleteEntry$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    deleteEntry(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * entry id to delete
         */
        entry: string;
    }): Observable<void> {
        return this.deleteEntry$Response(params).pipe(
            map((r: StrictHttpResponse<void>) => r.body as void),
        );
    }

    /**
     * Path part for operation getPropertyValues
     */
    static readonly GetPropertyValuesPath = '/stream/v1/properties/{repository}/{property}';

    /**
     * Get top values for a property.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getPropertyValues()` instead.
     *
     * This method doesn't expect any request body.
     */
    getPropertyValues$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * The property to aggregate
         */
        property: string;
    }): Observable<
        StrictHttpResponse<{
            [key: string]: {};
        }>
    > {
        const rb = new RequestBuilder(this.rootUrl, StreamV1Service.GetPropertyValuesPath, 'get');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
            rb.path('property', params.property, { style: 'simple', explode: false });
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
                    return r as StrictHttpResponse<{
                        [key: string]: {};
                    }>;
                }),
            );
    }

    /**
     * Get top values for a property.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getPropertyValues$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getPropertyValues(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * The property to aggregate
         */
        property: string;
    }): Observable<{
        [key: string]: {};
    }> {
        return this.getPropertyValues$Response(params).pipe(
            map(
                (
                    r: StrictHttpResponse<{
                        [key: string]: {};
                    }>,
                ) =>
                    r.body as {
                        [key: string]: {};
                    },
            ),
        );
    }

    /**
     * Path part for operation search_3
     */
    static readonly Search_3Path = '/stream/v1/search/{repository}';

    /**
     * Get the stream content for the current user with the given status.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `search_3()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    search_3$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * Stream object status to search for
         */
        status?: string;

        /**
         * generic text to search for (in title or description)
         */
        query?: string;

        /**
         * maximum items per page
         */
        maxItems?: number;

        /**
         * skip a number of items
         */
        skipCount?: number;

        /**
         * sort properties, currently supported: created, priority, default: priority desc, created desc
         */
        sortProperties?: Array<string>;

        /**
         * sort ascending, true if not set. Use multiple values to change the direction according to the given property at the same index
         */
        sortAscending?: Array<boolean>;

        /**
         * map with property + value to search
         */
        body?: {
            [key: string]: string;
        };
    }): Observable<StrictHttpResponse<StreamList>> {
        const rb = new RequestBuilder(this.rootUrl, StreamV1Service.Search_3Path, 'post');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
            rb.query('status', params.status, { style: 'form', explode: true });
            rb.query('query', params.query, { style: 'form', explode: true });
            rb.query('maxItems', params.maxItems, { style: 'form', explode: true });
            rb.query('skipCount', params.skipCount, { style: 'form', explode: true });
            rb.query('sortProperties', params.sortProperties, { style: 'form', explode: true });
            rb.query('sortAscending', params.sortAscending, { style: 'form', explode: true });
            rb.body(params.body, 'application/json');
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
                    return r as StrictHttpResponse<StreamList>;
                }),
            );
    }

    /**
     * Get the stream content for the current user with the given status.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `search_3$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    search_3(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * Stream object status to search for
         */
        status?: string;

        /**
         * generic text to search for (in title or description)
         */
        query?: string;

        /**
         * maximum items per page
         */
        maxItems?: number;

        /**
         * skip a number of items
         */
        skipCount?: number;

        /**
         * sort properties, currently supported: created, priority, default: priority desc, created desc
         */
        sortProperties?: Array<string>;

        /**
         * sort ascending, true if not set. Use multiple values to change the direction according to the given property at the same index
         */
        sortAscending?: Array<boolean>;

        /**
         * map with property + value to search
         */
        body?: {
            [key: string]: string;
        };
    }): Observable<StreamList> {
        return this.search_3$Response(params).pipe(
            map((r: StrictHttpResponse<StreamList>) => r.body as StreamList),
        );
    }

    /**
     * Path part for operation updateEntry
     */
    static readonly UpdateEntryPath = '/stream/v1/status/{repository}/{entry}';

    /**
     * update status for a stream object and authority.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `updateEntry()` instead.
     *
     * This method doesn't expect any request body.
     */
    updateEntry$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * entry id to update
         */
        entry: string;

        /**
         * authority to set/change status
         */
        authority: string;

        /**
         * New status for this authority
         */
        status: string;
    }): Observable<StrictHttpResponse<void>> {
        const rb = new RequestBuilder(this.rootUrl, StreamV1Service.UpdateEntryPath, 'put');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
            rb.path('entry', params.entry, { style: 'simple', explode: false });
            rb.query('authority', params.authority, { style: 'form', explode: true });
            rb.query('status', params.status, { style: 'form', explode: true });
        }

        return this.http
            .request(
                rb.build({
                    responseType: 'text',
                    accept: '*/*',
                }),
            )
            .pipe(
                filter((r: any) => r instanceof HttpResponse),
                map((r: HttpResponse<any>) => {
                    return (r as HttpResponse<any>).clone({
                        body: undefined,
                    }) as StrictHttpResponse<void>;
                }),
            );
    }

    /**
     * update status for a stream object and authority.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `updateEntry$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    updateEntry(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * entry id to update
         */
        entry: string;

        /**
         * authority to set/change status
         */
        authority: string;

        /**
         * New status for this authority
         */
        status: string;
    }): Observable<void> {
        return this.updateEntry$Response(params).pipe(
            map((r: StrictHttpResponse<void>) => r.body as void),
        );
    }
}
