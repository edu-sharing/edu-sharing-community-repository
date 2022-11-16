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

import { Mds } from '../models/mds';
import { MdsEntries } from '../models/mds-entries';
import { MdsValue } from '../models/mds-value';
import { SuggestionParam } from '../models/suggestion-param';
import { Suggestions } from '../models/suggestions';

@Injectable({
    providedIn: 'root',
})
export class MdsV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation getMetadataSet
     */
    static readonly GetMetadataSetPath = '/mds/v1/metadatasets/{repository}/{metadataset}';

    /**
     * Get metadata set new.
     *
     * Get metadata set new.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getMetadataSet()` instead.
     *
     * This method doesn't expect any request body.
     */
    getMetadataSet$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of metadataset (or &quot;-default-&quot; for default metadata set)
         */
        metadataset: string;
    }): Observable<StrictHttpResponse<Mds>> {
        const rb = new RequestBuilder(this.rootUrl, MdsV1Service.GetMetadataSetPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('metadataset', params.metadataset, {});
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
                    return r as StrictHttpResponse<Mds>;
                }),
            );
    }

    /**
     * Get metadata set new.
     *
     * Get metadata set new.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getMetadataSet$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getMetadataSet(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of metadataset (or &quot;-default-&quot; for default metadata set)
         */
        metadataset: string;
    }): Observable<Mds> {
        return this.getMetadataSet$Response(params).pipe(
            map((r: StrictHttpResponse<Mds>) => r.body as Mds),
        );
    }

    /**
     * Path part for operation getMetadataSets
     */
    static readonly GetMetadataSetsPath = '/mds/v1/metadatasets/{repository}';

    /**
     * Get metadata sets V2 of repository.
     *
     * Get metadata sets V2 of repository.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getMetadataSets()` instead.
     *
     * This method doesn't expect any request body.
     */
    getMetadataSets$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;
    }): Observable<StrictHttpResponse<MdsEntries>> {
        const rb = new RequestBuilder(this.rootUrl, MdsV1Service.GetMetadataSetsPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
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
                    return r as StrictHttpResponse<MdsEntries>;
                }),
            );
    }

    /**
     * Get metadata sets V2 of repository.
     *
     * Get metadata sets V2 of repository.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getMetadataSets$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getMetadataSets(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;
    }): Observable<MdsEntries> {
        return this.getMetadataSets$Response(params).pipe(
            map((r: StrictHttpResponse<MdsEntries>) => r.body as MdsEntries),
        );
    }

    /**
     * Path part for operation getValues
     */
    static readonly GetValuesPath = '/mds/v1/metadatasets/{repository}/{metadataset}/values';

    /**
     * Get values.
     *
     * Get values.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getValues()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    getValues$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of metadataset (or &quot;-default-&quot; for default metadata set)
         */
        metadataset: string;

        /**
         * suggestionParam
         */
        body?: SuggestionParam;
    }): Observable<StrictHttpResponse<Mds>> {
        const rb = new RequestBuilder(this.rootUrl, MdsV1Service.GetValuesPath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('metadataset', params.metadataset, {});
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
                    return r as StrictHttpResponse<Mds>;
                }),
            );
    }

    /**
     * Get values.
     *
     * Get values.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getValues$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    getValues(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of metadataset (or &quot;-default-&quot; for default metadata set)
         */
        metadataset: string;

        /**
         * suggestionParam
         */
        body?: SuggestionParam;
    }): Observable<Mds> {
        return this.getValues$Response(params).pipe(
            map((r: StrictHttpResponse<Mds>) => r.body as Mds),
        );
    }

    /**
     * Path part for operation getValues4Keys
     */
    static readonly GetValues4KeysPath =
        '/mds/v1/metadatasets/{repository}/{metadataset}/values_for_keys';

    /**
     * Get values for keys.
     *
     * Get values for keys.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getValues4Keys()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    getValues4Keys$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of metadataset (or &quot;-default-&quot; for default metadata set)
         */
        metadataset: string;

        /**
         * query
         */
        query?: string;

        /**
         * property
         */
        property?: string;

        /**
         * keys
         */
        body?: Array<string>;
    }): Observable<StrictHttpResponse<Suggestions>> {
        const rb = new RequestBuilder(this.rootUrl, MdsV1Service.GetValues4KeysPath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('metadataset', params.metadataset, {});
            rb.query('query', params.query, {});
            rb.query('property', params.property, {});
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
                    return r as StrictHttpResponse<Suggestions>;
                }),
            );
    }

    /**
     * Get values for keys.
     *
     * Get values for keys.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getValues4Keys$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    getValues4Keys(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of metadataset (or &quot;-default-&quot; for default metadata set)
         */
        metadataset: string;

        /**
         * query
         */
        query?: string;

        /**
         * property
         */
        property?: string;

        /**
         * keys
         */
        body?: Array<string>;
    }): Observable<Suggestions> {
        return this.getValues4Keys$Response(params).pipe(
            map((r: StrictHttpResponse<Suggestions>) => r.body as Suggestions),
        );
    }

    /**
     * Path part for operation suggestValue
     */
    static readonly SuggestValuePath =
        '/mds/v1/metadatasets/{repository}/{metadataset}/values/{widget}/suggest';

    /**
     * Suggest a value.
     *
     * Suggest a new value for a given metadataset and widget. The suggestion will be forwarded to the corresponding person in the metadataset file
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `suggestValue()` instead.
     *
     * This method doesn't expect any request body.
     */
    suggestValue$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of metadataset (or &quot;-default-&quot; for default metadata set)
         */
        metadataset: string;

        /**
         * widget id, e.g. cm:name
         */
        widget: string;

        /**
         * caption of the new entry (id will be auto-generated)
         */
        caption: string;

        /**
         * parent id of the new entry (might be null)
         */
        parent?: string;

        /**
         * One or more nodes this suggestion relates to (optional, only for extended mail data)
         */
        nodeId?: Array<string>;
    }): Observable<StrictHttpResponse<MdsValue>> {
        const rb = new RequestBuilder(this.rootUrl, MdsV1Service.SuggestValuePath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('metadataset', params.metadataset, {});
            rb.path('widget', params.widget, {});
            rb.query('caption', params.caption, {});
            rb.query('parent', params.parent, {});
            rb.query('nodeId', params.nodeId, {});
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
                    return r as StrictHttpResponse<MdsValue>;
                }),
            );
    }

    /**
     * Suggest a value.
     *
     * Suggest a new value for a given metadataset and widget. The suggestion will be forwarded to the corresponding person in the metadataset file
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `suggestValue$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    suggestValue(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of metadataset (or &quot;-default-&quot; for default metadata set)
         */
        metadataset: string;

        /**
         * widget id, e.g. cm:name
         */
        widget: string;

        /**
         * caption of the new entry (id will be auto-generated)
         */
        caption: string;

        /**
         * parent id of the new entry (might be null)
         */
        parent?: string;

        /**
         * One or more nodes this suggestion relates to (optional, only for extended mail data)
         */
        nodeId?: Array<string>;
    }): Observable<MdsValue> {
        return this.suggestValue$Response(params).pipe(
            map((r: StrictHttpResponse<MdsValue>) => r.body as MdsValue),
        );
    }
}
