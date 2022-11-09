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

import { AbstractEntries } from '../models/abstract-entries';
import { CollectionEntries } from '../models/collection-entries';
import { CollectionEntry } from '../models/collection-entry';
import { CollectionProposalEntries } from '../models/collection-proposal-entries';
import { Node } from '../models/node';
import { NodeEntry } from '../models/node-entry';
import { ReferenceEntries } from '../models/reference-entries';

@Injectable({
    providedIn: 'root',
})
export class CollectionV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation addToCollection
     */
    static readonly AddToCollectionPath =
        '/collection/v1/collections/{repository}/{collection}/references/{node}';

    /**
     * Add a node to a collection.
     *
     * Add a node to a collection.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `addToCollection()` instead.
     *
     * This method doesn't expect any request body.
     */
    addToCollection$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of collection
         */
        collection: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * ID of source repository
         */
        sourceRepo?: string;

        /**
         * Allow that a node that already is inside the collection can be added again
         */
        allowDuplicate?: boolean;

        /**
         * Mark this node only as a proposal (not really adding but just marking it). This can also be used for collections where you don&#x27;t have permissions
         */
        asProposal?: boolean;
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, CollectionV1Service.AddToCollectionPath, 'put');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('collection', params.collection, {});
            rb.path('node', params.node, {});
            rb.query('sourceRepo', params.sourceRepo, {});
            rb.query('allowDuplicate', params.allowDuplicate, {});
            rb.query('asProposal', params.asProposal, {});
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
     * Add a node to a collection.
     *
     * Add a node to a collection.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `addToCollection$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    addToCollection(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of collection
         */
        collection: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * ID of source repository
         */
        sourceRepo?: string;

        /**
         * Allow that a node that already is inside the collection can be added again
         */
        allowDuplicate?: boolean;

        /**
         * Mark this node only as a proposal (not really adding but just marking it). This can also be used for collections where you don&#x27;t have permissions
         */
        asProposal?: boolean;
    }): Observable<NodeEntry> {
        return this.addToCollection$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation deleteFromCollection
     */
    static readonly DeleteFromCollectionPath =
        '/collection/v1/collections/{repository}/{collection}/references/{node}';

    /**
     * Delete a node from a collection.
     *
     * Delete a node from a collection.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `deleteFromCollection()` instead.
     *
     * This method doesn't expect any request body.
     */
    deleteFromCollection$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of collection
         */
        collection: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            CollectionV1Service.DeleteFromCollectionPath,
            'delete',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('collection', params.collection, {});
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
                    return r as StrictHttpResponse<any>;
                }),
            );
    }

    /**
     * Delete a node from a collection.
     *
     * Delete a node from a collection.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `deleteFromCollection$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    deleteFromCollection(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of collection
         */
        collection: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<any> {
        return this.deleteFromCollection$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation changeIconOfCollection
     */
    static readonly ChangeIconOfCollectionPath =
        '/collection/v1/collections/{repository}/{collection}/icon';

    /**
     * Writes Preview Image of a collection.
     *
     * Writes Preview Image of a collection.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `changeIconOfCollection()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    changeIconOfCollection$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of collection
         */
        collection: string;

        /**
         * MIME-Type
         */
        mimetype: string;
        body?: {
            file?: {};
        };
    }): Observable<StrictHttpResponse<CollectionEntry>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            CollectionV1Service.ChangeIconOfCollectionPath,
            'post',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('collection', params.collection, {});
            rb.query('mimetype', params.mimetype, {});
            rb.body(params.body, 'multipart/form-data');
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
                    return r as StrictHttpResponse<CollectionEntry>;
                }),
            );
    }

    /**
     * Writes Preview Image of a collection.
     *
     * Writes Preview Image of a collection.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `changeIconOfCollection$Response()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    changeIconOfCollection(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of collection
         */
        collection: string;

        /**
         * MIME-Type
         */
        mimetype: string;
        body?: {
            file?: {};
        };
    }): Observable<CollectionEntry> {
        return this.changeIconOfCollection$Response(params).pipe(
            map((r: StrictHttpResponse<CollectionEntry>) => r.body as CollectionEntry),
        );
    }

    /**
     * Path part for operation removeIconOfCollection
     */
    static readonly RemoveIconOfCollectionPath =
        '/collection/v1/collections/{repository}/{collection}/icon';

    /**
     * Deletes Preview Image of a collection.
     *
     * Deletes Preview Image of a collection.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `removeIconOfCollection()` instead.
     *
     * This method doesn't expect any request body.
     */
    removeIconOfCollection$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of collection
         */
        collection: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            CollectionV1Service.RemoveIconOfCollectionPath,
            'delete',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('collection', params.collection, {});
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
     * Deletes Preview Image of a collection.
     *
     * Deletes Preview Image of a collection.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `removeIconOfCollection$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    removeIconOfCollection(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of collection
         */
        collection: string;
    }): Observable<any> {
        return this.removeIconOfCollection$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation createCollection
     */
    static readonly CreateCollectionPath =
        '/collection/v1/collections/{repository}/{collection}/children';

    /**
     * Create a new collection.
     *
     * Create a new collection.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `createCollection()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    createCollection$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of parent collection (or &quot;-root-&quot; for level0 collections)
         */
        collection: string;

        /**
         * collection
         */
        body: Node;
    }): Observable<StrictHttpResponse<CollectionEntry>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            CollectionV1Service.CreateCollectionPath,
            'post',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('collection', params.collection, {});
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
                    return r as StrictHttpResponse<CollectionEntry>;
                }),
            );
    }

    /**
     * Create a new collection.
     *
     * Create a new collection.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `createCollection$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    createCollection(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of parent collection (or &quot;-root-&quot; for level0 collections)
         */
        collection: string;

        /**
         * collection
         */
        body: Node;
    }): Observable<CollectionEntry> {
        return this.createCollection$Response(params).pipe(
            map((r: StrictHttpResponse<CollectionEntry>) => r.body as CollectionEntry),
        );
    }

    /**
     * Path part for operation updateCollection
     */
    static readonly UpdateCollectionPath = '/collection/v1/collections/{repository}/{collection}';

    /**
     * Update a collection.
     *
     * Update a collection.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `updateCollection()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    updateCollection$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of collection
         */
        collection: string;

        /**
         * collection node
         */
        body: Node;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            CollectionV1Service.UpdateCollectionPath,
            'put',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('collection', params.collection, {});
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
                    return r as StrictHttpResponse<any>;
                }),
            );
    }

    /**
     * Update a collection.
     *
     * Update a collection.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `updateCollection$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    updateCollection(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of collection
         */
        collection: string;

        /**
         * collection node
         */
        body: Node;
    }): Observable<any> {
        return this.updateCollection$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation deleteCollection
     */
    static readonly DeleteCollectionPath = '/collection/v1/collections/{repository}/{collection}';

    /**
     * Delete a collection.
     *
     * Delete a collection.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `deleteCollection()` instead.
     *
     * This method doesn't expect any request body.
     */
    deleteCollection$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of collection
         */
        collection: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            CollectionV1Service.DeleteCollectionPath,
            'delete',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('collection', params.collection, {});
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
     * Delete a collection.
     *
     * Delete a collection.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `deleteCollection$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    deleteCollection(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of collection
         */
        collection: string;
    }): Observable<any> {
        return this.deleteCollection$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation getCollection
     */
    static readonly GetCollectionPath = '/collection/v1/collections/{repository}/{collectionId}';

    /**
     * Get a collection.
     *
     * Get a collection.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getCollection()` instead.
     *
     * This method doesn't expect any request body.
     */
    getCollection$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of collection
         */
        collectionId: string;
    }): Observable<StrictHttpResponse<CollectionEntry>> {
        const rb = new RequestBuilder(this.rootUrl, CollectionV1Service.GetCollectionPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('collectionId', params.collectionId, {});
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
                    return r as StrictHttpResponse<CollectionEntry>;
                }),
            );
    }

    /**
     * Get a collection.
     *
     * Get a collection.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getCollection$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getCollection(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of collection
         */
        collectionId: string;
    }): Observable<CollectionEntry> {
        return this.getCollection$Response(params).pipe(
            map((r: StrictHttpResponse<CollectionEntry>) => r.body as CollectionEntry),
        );
    }

    /**
     * Path part for operation getCollectionsContainingProposals
     */
    static readonly GetCollectionsContainingProposalsPath =
        '/collection/v1/collections/{repository}/children/proposals/collections';

    /**
     * Get all collections containing proposals with a given state (via search index).
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getCollectionsContainingProposals()` instead.
     *
     * This method doesn't expect any request body.
     */
    getCollectionsContainingProposals$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * status of the proposals to search for
         */
        status?: 'PENDING' | 'ACCEPTED' | 'DECLINED';

        /**
         * fetch counts of collections (materials and subcollections). This parameter will decrease performance so only enable if if you need this data
         */
        fetchCounts?: boolean;

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
    }): Observable<StrictHttpResponse<CollectionProposalEntries>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            CollectionV1Service.GetCollectionsContainingProposalsPath,
            'get',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.query('status', params.status, {});
            rb.query('fetchCounts', params.fetchCounts, {});
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
                    return r as StrictHttpResponse<CollectionProposalEntries>;
                }),
            );
    }

    /**
     * Get all collections containing proposals with a given state (via search index).
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getCollectionsContainingProposals$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getCollectionsContainingProposals(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * status of the proposals to search for
         */
        status?: 'PENDING' | 'ACCEPTED' | 'DECLINED';

        /**
         * fetch counts of collections (materials and subcollections). This parameter will decrease performance so only enable if if you need this data
         */
        fetchCounts?: boolean;

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
    }): Observable<CollectionProposalEntries> {
        return this.getCollectionsContainingProposals$Response(params).pipe(
            map(
                (r: StrictHttpResponse<CollectionProposalEntries>) =>
                    r.body as CollectionProposalEntries,
            ),
        );
    }

    /**
     * Path part for operation getCollectionsProposals
     */
    static readonly GetCollectionsProposalsPath =
        '/collection/v1/collections/{repository}/{collection}/children/proposals';

    /**
     * Get proposed objects for collection (requires edit permissions on collection).
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getCollectionsProposals()` instead.
     *
     * This method doesn't expect any request body.
     */
    getCollectionsProposals$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of parent collection
         */
        collection: string;

        /**
         * Only show elements with given status
         */
        status: 'PENDING' | 'ACCEPTED' | 'DECLINED';
    }): Observable<StrictHttpResponse<AbstractEntries>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            CollectionV1Service.GetCollectionsProposalsPath,
            'get',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('collection', params.collection, {});
            rb.query('status', params.status, {});
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
                    return r as StrictHttpResponse<AbstractEntries>;
                }),
            );
    }

    /**
     * Get proposed objects for collection (requires edit permissions on collection).
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getCollectionsProposals$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getCollectionsProposals(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of parent collection
         */
        collection: string;

        /**
         * Only show elements with given status
         */
        status: 'PENDING' | 'ACCEPTED' | 'DECLINED';
    }): Observable<AbstractEntries> {
        return this.getCollectionsProposals$Response(params).pipe(
            map((r: StrictHttpResponse<AbstractEntries>) => r.body as AbstractEntries),
        );
    }

    /**
     * Path part for operation getCollectionsReferences
     */
    static readonly GetCollectionsReferencesPath =
        '/collection/v1/collections/{repository}/{collection}/children/references';

    /**
     * Get references objects for collection.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getCollectionsReferences()` instead.
     *
     * This method doesn't expect any request body.
     */
    getCollectionsReferences$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of parent collection
         */
        collection: string;

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

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;
    }): Observable<StrictHttpResponse<ReferenceEntries>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            CollectionV1Service.GetCollectionsReferencesPath,
            'get',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('collection', params.collection, {});
            rb.query('maxItems', params.maxItems, {});
            rb.query('skipCount', params.skipCount, {});
            rb.query('sortProperties', params.sortProperties, {});
            rb.query('sortAscending', params.sortAscending, {});
            rb.query('propertyFilter', params.propertyFilter, {});
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
                    return r as StrictHttpResponse<ReferenceEntries>;
                }),
            );
    }

    /**
     * Get references objects for collection.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getCollectionsReferences$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getCollectionsReferences(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of parent collection
         */
        collection: string;

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

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;
    }): Observable<ReferenceEntries> {
        return this.getCollectionsReferences$Response(params).pipe(
            map((r: StrictHttpResponse<ReferenceEntries>) => r.body as ReferenceEntries),
        );
    }

    /**
     * Path part for operation getCollectionsSubcollections
     */
    static readonly GetCollectionsSubcollectionsPath =
        '/collection/v1/collections/{repository}/{collection}/children/collections';

    /**
     * Get child collections for collection (or root).
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getCollectionsSubcollections()` instead.
     *
     * This method doesn't expect any request body.
     */
    getCollectionsSubcollections$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of parent collection (or &quot;-root-&quot; for level0 collections)
         */
        collection: string;

        /**
         * scope (only relevant if parent &#x3D;&#x3D; -root-)
         */
        scope: 'EDU_ALL' | 'EDU_GROUPS' | 'TYPE_EDITORIAL' | 'TYPE_MEDIA_CENTER' | 'MY' | 'RECENT';

        /**
         * fetch counts of collections (materials and subcollections). This parameter will decrease performance so only enable if if you need this data
         */
        fetchCounts?: boolean;

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

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;
    }): Observable<StrictHttpResponse<ReferenceEntries>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            CollectionV1Service.GetCollectionsSubcollectionsPath,
            'get',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('collection', params.collection, {});
            rb.query('scope', params.scope, {});
            rb.query('fetchCounts', params.fetchCounts, {});
            rb.query('maxItems', params.maxItems, {});
            rb.query('skipCount', params.skipCount, {});
            rb.query('sortProperties', params.sortProperties, {});
            rb.query('sortAscending', params.sortAscending, {});
            rb.query('propertyFilter', params.propertyFilter, {});
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
                    return r as StrictHttpResponse<ReferenceEntries>;
                }),
            );
    }

    /**
     * Get child collections for collection (or root).
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getCollectionsSubcollections$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getCollectionsSubcollections(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of parent collection (or &quot;-root-&quot; for level0 collections)
         */
        collection: string;

        /**
         * scope (only relevant if parent &#x3D;&#x3D; -root-)
         */
        scope: 'EDU_ALL' | 'EDU_GROUPS' | 'TYPE_EDITORIAL' | 'TYPE_MEDIA_CENTER' | 'MY' | 'RECENT';

        /**
         * fetch counts of collections (materials and subcollections). This parameter will decrease performance so only enable if if you need this data
         */
        fetchCounts?: boolean;

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

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;
    }): Observable<ReferenceEntries> {
        return this.getCollectionsSubcollections$Response(params).pipe(
            map((r: StrictHttpResponse<ReferenceEntries>) => r.body as ReferenceEntries),
        );
    }

    /**
     * Path part for operation searchCollections
     */
    static readonly SearchCollectionsPath = '/collection/v1/collections/{repository}/search';

    /**
     * Search collections.
     *
     * Search collections.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `searchCollections()` instead.
     *
     * This method doesn't expect any request body.
     */
    searchCollections$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * query string
         */
        query: string;

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
    }): Observable<StrictHttpResponse<CollectionEntries>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            CollectionV1Service.SearchCollectionsPath,
            'get',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.query('query', params.query, {});
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
                    return r as StrictHttpResponse<CollectionEntries>;
                }),
            );
    }

    /**
     * Search collections.
     *
     * Search collections.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `searchCollections$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    searchCollections(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * query string
         */
        query: string;

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
    }): Observable<CollectionEntries> {
        return this.searchCollections$Response(params).pipe(
            map((r: StrictHttpResponse<CollectionEntries>) => r.body as CollectionEntries),
        );
    }

    /**
     * Path part for operation setCollectionOrder
     */
    static readonly SetCollectionOrderPath =
        '/collection/v1/collections/{repository}/{collection}/order';

    /**
     * Set order of nodes in a collection. In order to work as expected, provide a list of all nodes in this collection.
     *
     * Current order will be overriden. Requires full permissions for the parent collection
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `setCollectionOrder()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    setCollectionOrder$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of collection
         */
        collection: string;

        /**
         * List of nodes in the order to be saved. If empty, custom order of the collection will be disabled
         */
        body?: Array<string>;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            CollectionV1Service.SetCollectionOrderPath,
            'post',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('collection', params.collection, {});
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
                    return r as StrictHttpResponse<any>;
                }),
            );
    }

    /**
     * Set order of nodes in a collection. In order to work as expected, provide a list of all nodes in this collection.
     *
     * Current order will be overriden. Requires full permissions for the parent collection
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `setCollectionOrder$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    setCollectionOrder(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of collection
         */
        collection: string;

        /**
         * List of nodes in the order to be saved. If empty, custom order of the collection will be disabled
         */
        body?: Array<string>;
    }): Observable<any> {
        return this.setCollectionOrder$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation setPinnedCollections
     */
    static readonly SetPinnedCollectionsPath = '/collection/v1/collections/{repository}/pinning';

    /**
     * Set pinned collections.
     *
     * Remove all currently pinned collections and set them in the order send. Requires TOOLPERMISSION_COLLECTION_PINNING
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `setPinnedCollections()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    setPinnedCollections$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * List of collections that should be pinned
         */
        body: Array<string>;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            CollectionV1Service.SetPinnedCollectionsPath,
            'post',
        );
        if (params) {
            rb.path('repository', params.repository, {});
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
                    return r as StrictHttpResponse<any>;
                }),
            );
    }

    /**
     * Set pinned collections.
     *
     * Remove all currently pinned collections and set them in the order send. Requires TOOLPERMISSION_COLLECTION_PINNING
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `setPinnedCollections$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    setPinnedCollections(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * List of collections that should be pinned
         */
        body: Array<string>;
    }): Observable<any> {
        return this.setPinnedCollections$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }
}
