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

import { NodeEntries } from '../models/node-entries';
import { SharingInfo } from '../models/sharing-info';

@Injectable({
    providedIn: 'root',
})
export class SharingV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation getChildren1
     */
    static readonly GetChildren1Path = '/sharing/v1/sharing/{repository}/{node}/{share}/children';

    /**
     * Get all children of this share.
     *
     * Only valid for shared folders
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getChildren1()` instead.
     *
     * This method doesn't expect any request body.
     */
    getChildren1$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * Share token
         */
        share: string;

        /**
         * Password (required if share is locked)
         */
        password?: string;

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
         * sort ascending, true if not set. Use multiple values to change the direction according to the given property at the same index
         */
        sortAscending?: Array<boolean>;
    }): Observable<StrictHttpResponse<NodeEntries>> {
        const rb = new RequestBuilder(this.rootUrl, SharingV1Service.GetChildren1Path, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.path('share', params.share, {});
            rb.query('password', params.password, {});
            rb.query('maxItems', params.maxItems, {});
            rb.query('skipCount', params.skipCount, {});
            rb.query('sortProperties', params.sortProperties, {});
            rb.query('sortAscending', params.sortAscending, {});
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
                    return r as StrictHttpResponse<NodeEntries>;
                }),
            );
    }

    /**
     * Get all children of this share.
     *
     * Only valid for shared folders
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getChildren1$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getChildren1(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * Share token
         */
        share: string;

        /**
         * Password (required if share is locked)
         */
        password?: string;

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
         * sort ascending, true if not set. Use multiple values to change the direction according to the given property at the same index
         */
        sortAscending?: Array<boolean>;
    }): Observable<NodeEntries> {
        return this.getChildren1$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntries>) => r.body as NodeEntries),
        );
    }

    /**
     * Path part for operation getInfo
     */
    static readonly GetInfoPath = '/sharing/v1/sharing/{repository}/{node}/{share}';

    /**
     * Get general info of a share.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getInfo()` instead.
     *
     * This method doesn't expect any request body.
     */
    getInfo$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * Share token
         */
        share: string;

        /**
         * Password to validate (optional)
         */
        password?: string;
    }): Observable<StrictHttpResponse<SharingInfo>> {
        const rb = new RequestBuilder(this.rootUrl, SharingV1Service.GetInfoPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.path('share', params.share, {});
            rb.query('password', params.password, {});
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
                    return r as StrictHttpResponse<SharingInfo>;
                }),
            );
    }

    /**
     * Get general info of a share.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getInfo$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getInfo(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * Share token
         */
        share: string;

        /**
         * Password to validate (optional)
         */
        password?: string;
    }): Observable<SharingInfo> {
        return this.getInfo$Response(params).pipe(
            map((r: StrictHttpResponse<SharingInfo>) => r.body as SharingInfo),
        );
    }
}
