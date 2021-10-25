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

import { MdsEntriesV2 } from '../models/mds-entries-v-2';
import { MdsV2 } from '../models/mds-v-2';
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
     * Path part for operation getMetadataSetsV2
     */
    static readonly GetMetadataSetsV2Path = '/mds/v1/metadatasetsV2/{repository}';

    /**
     * Get metadata sets V2 of repository.
     *
     * Get metadata sets V2 of repository.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getMetadataSetsV2()` instead.
     *
     * This method doesn't expect any request body.
     */
    getMetadataSetsV2$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;
    }): Observable<StrictHttpResponse<MdsEntriesV2>> {
        const rb = new RequestBuilder(this.rootUrl, MdsV1Service.GetMetadataSetsV2Path, 'get');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
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
                    return r as StrictHttpResponse<MdsEntriesV2>;
                }),
            );
    }

    /**
     * Get metadata sets V2 of repository.
     *
     * Get metadata sets V2 of repository.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getMetadataSetsV2$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getMetadataSetsV2(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;
    }): Observable<MdsEntriesV2> {
        return this.getMetadataSetsV2$Response(params).pipe(
            map((r: StrictHttpResponse<MdsEntriesV2>) => r.body as MdsEntriesV2),
        );
    }

    /**
     * Path part for operation getMetadataSetV2
     */
    static readonly GetMetadataSetV2Path = '/mds/v1/metadatasetsV2/{repository}/{metadataset}';

    /**
     * Get metadata set new.
     *
     * Get metadata set new.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getMetadataSetV2()` instead.
     *
     * This method doesn't expect any request body.
     */
    getMetadataSetV2$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of metadataset (or &quot;-default-&quot; for default metadata set)
         */
        metadataset: string;
    }): Observable<StrictHttpResponse<MdsV2>> {
        const rb = new RequestBuilder(this.rootUrl, MdsV1Service.GetMetadataSetV2Path, 'get');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
            rb.path('metadataset', params.metadataset, { style: 'simple', explode: false });
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
                    return r as StrictHttpResponse<MdsV2>;
                }),
            );
    }

    /**
     * Get metadata set new.
     *
     * Get metadata set new.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getMetadataSetV2$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getMetadataSetV2(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of metadataset (or &quot;-default-&quot; for default metadata set)
         */
        metadataset: string;
    }): Observable<MdsV2> {
        return this.getMetadataSetV2$Response(params).pipe(
            map((r: StrictHttpResponse<MdsV2>) => r.body as MdsV2),
        );
    }

    /**
     * Path part for operation getValuesV2
     */
    static readonly GetValuesV2Path = '/mds/v1/metadatasetsV2/{repository}/{metadataset}/values';

    /**
     * Get values.
     *
     * Get values.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getValuesV2()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    getValuesV2$Response(params: {
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
    }): Observable<StrictHttpResponse<Suggestions>> {
        const rb = new RequestBuilder(this.rootUrl, MdsV1Service.GetValuesV2Path, 'post');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
            rb.path('metadataset', params.metadataset, { style: 'simple', explode: false });
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
     * Get values.
     *
     * Get values.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getValuesV2$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    getValuesV2(params: {
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
    }): Observable<Suggestions> {
        return this.getValuesV2$Response(params).pipe(
            map((r: StrictHttpResponse<Suggestions>) => r.body as Suggestions),
        );
    }

    /**
     * Path part for operation getValues4KeysV2
     */
    static readonly GetValues4KeysV2Path =
        '/mds/v1/metadatasetsV2/{repository}/{metadataset}/values_for_keys';

    /**
     * Get values for keys.
     *
     * Get values for keys.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getValues4KeysV2()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    getValues4KeysV2$Response(params: {
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
        const rb = new RequestBuilder(this.rootUrl, MdsV1Service.GetValues4KeysV2Path, 'post');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
            rb.path('metadataset', params.metadataset, { style: 'simple', explode: false });
            rb.query('query', params.query, { style: 'form', explode: true });
            rb.query('property', params.property, { style: 'form', explode: true });
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
     * To access the full response (for headers, for example), `getValues4KeysV2$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    getValues4KeysV2(params: {
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
        return this.getValues4KeysV2$Response(params).pipe(
            map((r: StrictHttpResponse<Suggestions>) => r.body as Suggestions),
        );
    }
}
