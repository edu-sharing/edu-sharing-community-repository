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

import { FeedbackData } from '../models/feedback-data';
import { FeedbackResult } from '../models/feedback-result';

@Injectable({
    providedIn: 'root',
})
export class FeedbackV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation addFeedback
     */
    static readonly AddFeedbackPath = '/feedback/v1/feedback/{repository}/{node}/add';

    /**
     * Give feedback on a node.
     *
     * Adds feedback to the given node. Depending on the internal config, the current user will be obscured to prevent back-tracing to the original id
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `addFeedback()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    addFeedback$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * feedback data, key/value pairs
         */
        body: {
            [key: string]: Array<string>;
        };
    }): Observable<StrictHttpResponse<FeedbackResult>> {
        const rb = new RequestBuilder(this.rootUrl, FeedbackV1Service.AddFeedbackPath, 'put');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
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
                    return r as StrictHttpResponse<FeedbackResult>;
                }),
            );
    }

    /**
     * Give feedback on a node.
     *
     * Adds feedback to the given node. Depending on the internal config, the current user will be obscured to prevent back-tracing to the original id
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `addFeedback$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    addFeedback(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * feedback data, key/value pairs
         */
        body: {
            [key: string]: Array<string>;
        };
    }): Observable<FeedbackResult> {
        return this.addFeedback$Response(params).pipe(
            map((r: StrictHttpResponse<FeedbackResult>) => r.body as FeedbackResult),
        );
    }

    /**
     * Path part for operation getFeedbacks
     */
    static readonly GetFeedbacksPath = '/feedback/v1/feedback/{repository}/{node}/list';

    /**
     * Get given feedback on a node.
     *
     * Get all given feedback for a node. Requires Coordinator permissions on node
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getFeedbacks()` instead.
     *
     * This method doesn't expect any request body.
     */
    getFeedbacks$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<StrictHttpResponse<Array<FeedbackData>>> {
        const rb = new RequestBuilder(this.rootUrl, FeedbackV1Service.GetFeedbacksPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
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
                    return r as StrictHttpResponse<Array<FeedbackData>>;
                }),
            );
    }

    /**
     * Get given feedback on a node.
     *
     * Get all given feedback for a node. Requires Coordinator permissions on node
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getFeedbacks$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getFeedbacks(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<Array<FeedbackData>> {
        return this.getFeedbacks$Response(params).pipe(
            map((r: StrictHttpResponse<Array<FeedbackData>>) => r.body as Array<FeedbackData>),
        );
    }
}
