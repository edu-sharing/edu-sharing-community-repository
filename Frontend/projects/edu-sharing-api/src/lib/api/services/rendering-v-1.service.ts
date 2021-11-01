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

import { RenderingDetailsEntry } from '../models/rendering-details-entry';

@Injectable({
    providedIn: 'root',
})
export class RenderingV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation getDetailsSnippet
     */
    static readonly GetDetailsSnippetPath = '/rendering/v1/details/{repository}/{node}';

    /**
     * Get metadata of node.
     *
     * Get metadata of node.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getDetailsSnippet()` instead.
     *
     * This method doesn't expect any request body.
     */
    getDetailsSnippet$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * version of node
         */
        version?: string;

        /**
         * Rendering displayMode
         */
        displayMode?: string;
    }): Observable<StrictHttpResponse<RenderingDetailsEntry>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            RenderingV1Service.GetDetailsSnippetPath,
            'get',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('version', params.version, {});
            rb.query('displayMode', params.displayMode, {});
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
                    return r as StrictHttpResponse<RenderingDetailsEntry>;
                }),
            );
    }

    /**
     * Get metadata of node.
     *
     * Get metadata of node.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getDetailsSnippet$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getDetailsSnippet(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * version of node
         */
        version?: string;

        /**
         * Rendering displayMode
         */
        displayMode?: string;
    }): Observable<RenderingDetailsEntry> {
        return this.getDetailsSnippet$Response(params).pipe(
            map((r: StrictHttpResponse<RenderingDetailsEntry>) => r.body as RenderingDetailsEntry),
        );
    }

    /**
     * Path part for operation getDetailsSnippetWithParameters
     */
    static readonly GetDetailsSnippetWithParametersPath =
        '/rendering/v1/details/{repository}/{node}';

    /**
     * Get metadata of node.
     *
     * Get metadata of node.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getDetailsSnippetWithParameters()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    getDetailsSnippetWithParameters$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * version of node
         */
        version?: string;

        /**
         * Rendering displayMode
         */
        displayMode?: string;

        /**
         * additional parameters to send to the rendering service
         */
        body?: {
            [key: string]: string;
        };
    }): Observable<StrictHttpResponse<RenderingDetailsEntry>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            RenderingV1Service.GetDetailsSnippetWithParametersPath,
            'post',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('version', params.version, {});
            rb.query('displayMode', params.displayMode, {});
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
                    return r as StrictHttpResponse<RenderingDetailsEntry>;
                }),
            );
    }

    /**
     * Get metadata of node.
     *
     * Get metadata of node.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getDetailsSnippetWithParameters$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    getDetailsSnippetWithParameters(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * version of node
         */
        version?: string;

        /**
         * Rendering displayMode
         */
        displayMode?: string;

        /**
         * additional parameters to send to the rendering service
         */
        body?: {
            [key: string]: string;
        };
    }): Observable<RenderingDetailsEntry> {
        return this.getDetailsSnippetWithParameters$Response(params).pipe(
            map((r: StrictHttpResponse<RenderingDetailsEntry>) => r.body as RenderingDetailsEntry),
        );
    }
}
