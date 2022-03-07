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

import { NodeEntry } from '../models/node-entry';

@Injectable({
    providedIn: 'root',
})
export class BulkV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation find
     */
    static readonly FindPath = '/bulk/v1/find';

    /**
     * gets a given node.
     *
     * Get a given node based on the posted, multiple criteria. Make sure that they'll provide an unique result
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `find()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    find$Response(params: {
        /**
         * properties that must match (with "AND" concatenated)
         */
        body: {
            [key: string]: Array<string>;
        };
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, BulkV1Service.FindPath, 'post');
        if (params) {
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
                    return r as StrictHttpResponse<NodeEntry>;
                }),
            );
    }

    /**
     * gets a given node.
     *
     * Get a given node based on the posted, multiple criteria. Make sure that they'll provide an unique result
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `find$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    find(params: {
        /**
         * properties that must match (with "AND" concatenated)
         */
        body: {
            [key: string]: Array<string>;
        };
    }): Observable<NodeEntry> {
        return this.find$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation sync
     */
    static readonly SyncPath = '/bulk/v1/sync/{group}';

    /**
     * Create or update a given node.
     *
     * Depending on the given "match" properties either a new node will be created or the existing one will be updated
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `sync()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    sync$Response(params: {
        /**
         * The group to which this node belongs to. Used for internal structuring. Please use simple names only
         */
        group: string;

        /**
         * The properties that must match to identify if this node exists. Multiple properties will be and combined and compared
         */
        match: Array<string>;

        /**
         * The properties on which the imported nodes should be grouped (for each value, a folder with the corresponding data is created)
         */
        groupBy?: Array<string>;

        /**
         * type of node. If the node already exists, this will not change the type afterwards
         */
        type: string;

        /**
         * aspects of node
         */
        aspects?: Array<string>;

        /**
         * reset all versions (like a complete reimport), all data inside edu-sharing will be lost
         */
        resetVersion?: boolean;

        /**
         * properties, they'll not get filtered via mds, so be careful what you add here
         */
        body: {
            [key: string]: Array<string>;
        };
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, BulkV1Service.SyncPath, 'put');
        if (params) {
            rb.path('group', params.group, {});
            rb.query('match', params.match, {});
            rb.query('groupBy', params.groupBy, {});
            rb.query('type', params.type, {});
            rb.query('aspects', params.aspects, {});
            rb.query('resetVersion', params.resetVersion, {});
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
                    return r as StrictHttpResponse<NodeEntry>;
                }),
            );
    }

    /**
     * Create or update a given node.
     *
     * Depending on the given "match" properties either a new node will be created or the existing one will be updated
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `sync$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    sync(params: {
        /**
         * The group to which this node belongs to. Used for internal structuring. Please use simple names only
         */
        group: string;

        /**
         * The properties that must match to identify if this node exists. Multiple properties will be and combined and compared
         */
        match: Array<string>;

        /**
         * The properties on which the imported nodes should be grouped (for each value, a folder with the corresponding data is created)
         */
        groupBy?: Array<string>;

        /**
         * type of node. If the node already exists, this will not change the type afterwards
         */
        type: string;

        /**
         * aspects of node
         */
        aspects?: Array<string>;

        /**
         * reset all versions (like a complete reimport), all data inside edu-sharing will be lost
         */
        resetVersion?: boolean;

        /**
         * properties, they'll not get filtered via mds, so be careful what you add here
         */
        body: {
            [key: string]: Array<string>;
        };
    }): Observable<NodeEntry> {
        return this.sync$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }
}
