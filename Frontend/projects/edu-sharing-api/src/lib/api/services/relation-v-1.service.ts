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

import { NodeRelation } from '../models/node-relation';

@Injectable({
    providedIn: 'root',
})
export class RelationV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation createRelation
     */
    static readonly CreateRelationPath =
        '/relation/v1/relation/{repository}/{source}/{type}/{target}';

    /**
     * create a relation between nodes.
     *
     * Creates a relation between two nodes of the given type.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `createRelation()` instead.
     *
     * This method doesn't expect any request body.
     */
    createRelation$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        source: string;

        /**
         * ID of node
         */
        type: 'isPartOf' | 'isBasedOn' | 'references';

        /**
         * ID of node
         */
        target: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, RelationV1Service.CreateRelationPath, 'put');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('source', params.source, {});
            rb.path('type', params.type, {});
            rb.path('target', params.target, {});
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
                    return r as StrictHttpResponse<any>;
                }),
            );
    }

    /**
     * create a relation between nodes.
     *
     * Creates a relation between two nodes of the given type.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `createRelation$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    createRelation(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        source: string;

        /**
         * ID of node
         */
        type: 'isPartOf' | 'isBasedOn' | 'references';

        /**
         * ID of node
         */
        target: string;
    }): Observable<any> {
        return this.createRelation$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation deleteRelation
     */
    static readonly DeleteRelationPath =
        '/relation/v1/relation/{repository}/{source}/{type}/{target}';

    /**
     * delete a relation between nodes.
     *
     * Delete a relation between two nodes of the given type.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `deleteRelation()` instead.
     *
     * This method doesn't expect any request body.
     */
    deleteRelation$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        source: string;

        /**
         * ID of node
         */
        type: 'isPartOf' | 'isBasedOn' | 'references';

        /**
         * ID of node
         */
        target: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, RelationV1Service.DeleteRelationPath, 'delete');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('source', params.source, {});
            rb.path('type', params.type, {});
            rb.path('target', params.target, {});
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
                    return r as StrictHttpResponse<any>;
                }),
            );
    }

    /**
     * delete a relation between nodes.
     *
     * Delete a relation between two nodes of the given type.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `deleteRelation$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    deleteRelation(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        source: string;

        /**
         * ID of node
         */
        type: 'isPartOf' | 'isBasedOn' | 'references';

        /**
         * ID of node
         */
        target: string;
    }): Observable<any> {
        return this.deleteRelation$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation getRelations
     */
    static readonly GetRelationsPath = '/relation/v1/relation/{repository}/{node}';

    /**
     * get all relation of the node.
     *
     * Returns all relations of the node.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getRelations()` instead.
     *
     * This method doesn't expect any request body.
     */
    getRelations$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<StrictHttpResponse<NodeRelation>> {
        const rb = new RequestBuilder(this.rootUrl, RelationV1Service.GetRelationsPath, 'get');
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
                    return r as StrictHttpResponse<NodeRelation>;
                }),
            );
    }

    /**
     * get all relation of the node.
     *
     * Returns all relations of the node.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getRelations$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getRelations(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<NodeRelation> {
        return this.getRelations$Response(params).pipe(
            map((r: StrictHttpResponse<NodeRelation>) => r.body as NodeRelation),
        );
    }
}
