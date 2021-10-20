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

import { MdsQueryCriteria } from '../models/mds-query-criteria';
import { Node } from '../models/node';
import { NodeEntry } from '../models/node-entry';
import { SearchParameters } from '../models/search-parameters';
import { SearchParametersFacets } from '../models/search-parameters-facets';
import { SearchResultNode } from '../models/search-result-node';
import { SearchVCard } from '../models/search-v-card';

@Injectable({
    providedIn: 'root',
})
export class SearchV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation searchByProperty
     */
    static readonly SearchByPropertyPath = '/search/v1/custom/{repository}';

    /**
     * Search for custom properties with custom values.
     *
     * e.g. property=cm:name, value:*Test*
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `searchByProperty()` instead.
     *
     * This method doesn't expect any request body.
     */
    searchByProperty$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * Type of element
         */
        contentType?:
            | 'FILES'
            | 'FOLDERS'
            | 'FILES_AND_FOLDERS'
            | 'COLLECTIONS'
            | 'TOOLPERMISSIONS'
            | 'ALL';

        /**
         * Combine mode, AND or OR, defaults to AND
         */
        combineMode?: 'AND' | 'OR';

        /**
         * One (or more) properties to search for, will be combined by specified combine mode
         */
        property?: Array<string>;

        /**
         * One (or more) values to search for, matching the properties defined before
         */
        value?: Array<string>;

        /**
         * (Optional) comparator, only relevant for date or numerical fields, currently allowed &#x3D;, &lt;&#x3D;, &gt;&#x3D;
         */
        comparator?: Array<string>;

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
    }): Observable<StrictHttpResponse<SearchResultNode>> {
        const rb = new RequestBuilder(this.rootUrl, SearchV1Service.SearchByPropertyPath, 'get');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
            rb.query('contentType', params.contentType, { style: 'form', explode: true });
            rb.query('combineMode', params.combineMode, { style: 'form', explode: true });
            rb.query('property', params.property, { style: 'form', explode: true });
            rb.query('value', params.value, { style: 'form', explode: true });
            rb.query('comparator', params.comparator, { style: 'form', explode: true });
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
                    return r as StrictHttpResponse<SearchResultNode>;
                }),
            );
    }

    /**
     * Search for custom properties with custom values.
     *
     * e.g. property=cm:name, value:*Test*
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `searchByProperty$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    searchByProperty(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * Type of element
         */
        contentType?:
            | 'FILES'
            | 'FOLDERS'
            | 'FILES_AND_FOLDERS'
            | 'COLLECTIONS'
            | 'TOOLPERMISSIONS'
            | 'ALL';

        /**
         * Combine mode, AND or OR, defaults to AND
         */
        combineMode?: 'AND' | 'OR';

        /**
         * One (or more) properties to search for, will be combined by specified combine mode
         */
        property?: Array<string>;

        /**
         * One (or more) values to search for, matching the properties defined before
         */
        value?: Array<string>;

        /**
         * (Optional) comparator, only relevant for date or numerical fields, currently allowed &#x3D;, &lt;&#x3D;, &gt;&#x3D;
         */
        comparator?: Array<string>;

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
    }): Observable<SearchResultNode> {
        return this.searchByProperty$Response(params).pipe(
            map((r: StrictHttpResponse<SearchResultNode>) => r.body as SearchResultNode),
        );
    }

    /**
     * Path part for operation searchFingerprint
     */
    static readonly SearchFingerprintPath = '/search/v1/queries/{repository}/fingerprint/{nodeid}';

    /**
     * Perform queries based on metadata sets.
     *
     * Perform queries based on metadata sets.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `searchFingerprint()` instead.
     *
     * This method doesn't expect any request body.
     */
    searchFingerprint$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * nodeid
         */
        nodeid: string;

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
    }): Observable<StrictHttpResponse<SearchResultNode>> {
        const rb = new RequestBuilder(this.rootUrl, SearchV1Service.SearchFingerprintPath, 'post');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
            rb.path('nodeid', params.nodeid, { style: 'simple', explode: false });
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
                    return r as StrictHttpResponse<SearchResultNode>;
                }),
            );
    }

    /**
     * Perform queries based on metadata sets.
     *
     * Perform queries based on metadata sets.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `searchFingerprint$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    searchFingerprint(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * nodeid
         */
        nodeid: string;

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
    }): Observable<SearchResultNode> {
        return this.searchFingerprint$Response(params).pipe(
            map((r: StrictHttpResponse<SearchResultNode>) => r.body as SearchResultNode),
        );
    }

    /**
     * Path part for operation loadSaveSearch
     */
    static readonly LoadSaveSearchPath = '/search/v1/queriesV2/load/{nodeId}';

    /**
     * Load a saved search query.
     *
     * Load a saved search query.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `loadSaveSearch()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    loadSaveSearch$Response(params: {
        /**
         * Node id of the search item
         */
        nodeId: string;

        /**
         * Type of element
         */
        contentType?:
            | 'FILES'
            | 'FOLDERS'
            | 'FILES_AND_FOLDERS'
            | 'COLLECTIONS'
            | 'TOOLPERMISSIONS'
            | 'ALL';

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

        /**
         * facettes
         */
        body?: Array<string>;
    }): Observable<StrictHttpResponse<Node>> {
        const rb = new RequestBuilder(this.rootUrl, SearchV1Service.LoadSaveSearchPath, 'get');
        if (params) {
            rb.path('nodeId', params.nodeId, { style: 'simple', explode: false });
            rb.query('contentType', params.contentType, { style: 'form', explode: true });
            rb.query('maxItems', params.maxItems, { style: 'form', explode: true });
            rb.query('skipCount', params.skipCount, { style: 'form', explode: true });
            rb.query('sortProperties', params.sortProperties, { style: 'form', explode: true });
            rb.query('sortAscending', params.sortAscending, { style: 'form', explode: true });
            rb.query('propertyFilter', params.propertyFilter, { style: 'form', explode: true });
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
                    return r as StrictHttpResponse<Node>;
                }),
            );
    }

    /**
     * Load a saved search query.
     *
     * Load a saved search query.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `loadSaveSearch$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    loadSaveSearch(params: {
        /**
         * Node id of the search item
         */
        nodeId: string;

        /**
         * Type of element
         */
        contentType?:
            | 'FILES'
            | 'FOLDERS'
            | 'FILES_AND_FOLDERS'
            | 'COLLECTIONS'
            | 'TOOLPERMISSIONS'
            | 'ALL';

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

        /**
         * facettes
         */
        body?: Array<string>;
    }): Observable<Node> {
        return this.loadSaveSearch$Response(params).pipe(
            map((r: StrictHttpResponse<Node>) => r.body as Node),
        );
    }

    /**
     * Path part for operation searchContributor
     */
    static readonly SearchContributorPath = '/search/v1/queriesV2/{repository}/contributor';

    /**
     * Search for contributors.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `searchContributor()` instead.
     *
     * This method doesn't expect any request body.
     */
    searchContributor$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * search word
         */
        searchWord: string;

        /**
         * contributor kind
         */
        contributorKind: 'PERSON' | 'ORGANIZATION';

        /**
         * define which authority fields should be searched: [&#x27;firstname&#x27;, &#x27;lastname&#x27;, &#x27;email&#x27;, &#x27;uuid&#x27;, &#x27;url&#x27;]
         */
        fields?: Array<string>;

        /**
         * define which contributor props should be searched: [&#x27;ccm:lifecyclecontributer_author&#x27;, &#x27;ccm:lifecyclecontributer_publisher&#x27;, ..., &#x27;ccm:metadatacontributer_creator&#x27;, &#x27;ccm:metadatacontributer_validator&#x27;]
         */
        contributorProperties?: Array<string>;
    }): Observable<StrictHttpResponse<Array<SearchVCard>>> {
        const rb = new RequestBuilder(this.rootUrl, SearchV1Service.SearchContributorPath, 'get');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
            rb.query('searchWord', params.searchWord, { style: 'form', explode: true });
            rb.query('contributorKind', params.contributorKind, { style: 'form', explode: true });
            rb.query('fields', params.fields, { style: 'form', explode: true });
            rb.query('contributorProperties', params.contributorProperties, {
                style: 'form',
                explode: true,
            });
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
                    return r as StrictHttpResponse<Array<SearchVCard>>;
                }),
            );
    }

    /**
     * Search for contributors.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `searchContributor$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    searchContributor(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * search word
         */
        searchWord: string;

        /**
         * contributor kind
         */
        contributorKind: 'PERSON' | 'ORGANIZATION';

        /**
         * define which authority fields should be searched: [&#x27;firstname&#x27;, &#x27;lastname&#x27;, &#x27;email&#x27;, &#x27;uuid&#x27;, &#x27;url&#x27;]
         */
        fields?: Array<string>;

        /**
         * define which contributor props should be searched: [&#x27;ccm:lifecyclecontributer_author&#x27;, &#x27;ccm:lifecyclecontributer_publisher&#x27;, ..., &#x27;ccm:metadatacontributer_creator&#x27;, &#x27;ccm:metadatacontributer_validator&#x27;]
         */
        contributorProperties?: Array<string>;
    }): Observable<Array<SearchVCard>> {
        return this.searchContributor$Response(params).pipe(
            map((r: StrictHttpResponse<Array<SearchVCard>>) => r.body as Array<SearchVCard>),
        );
    }

    /**
     * Path part for operation searchV2
     */
    static readonly SearchV2Path = '/search/v1/queriesV2/{repository}/{metadataset}/{query}';

    /**
     * Perform queries based on metadata sets V2.
     *
     * Perform queries based on metadata sets V2.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `searchV2()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    searchV2$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of metadataset (or &quot;-default-&quot; for default metadata set)
         */
        metadataset: string;

        /**
         * ID of query
         */
        query: string;

        /**
         * Type of element
         */
        contentType?:
            | 'FILES'
            | 'FOLDERS'
            | 'FILES_AND_FOLDERS'
            | 'COLLECTIONS'
            | 'TOOLPERMISSIONS'
            | 'ALL';

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

        /**
         * search parameters
         */
        body: SearchParameters;
    }): Observable<StrictHttpResponse<SearchResultNode>> {
        const rb = new RequestBuilder(this.rootUrl, SearchV1Service.SearchV2Path, 'post');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
            rb.path('metadataset', params.metadataset, { style: 'simple', explode: false });
            rb.path('query', params.query, { style: 'simple', explode: false });
            rb.query('contentType', params.contentType, { style: 'form', explode: true });
            rb.query('maxItems', params.maxItems, { style: 'form', explode: true });
            rb.query('skipCount', params.skipCount, { style: 'form', explode: true });
            rb.query('sortProperties', params.sortProperties, { style: 'form', explode: true });
            rb.query('sortAscending', params.sortAscending, { style: 'form', explode: true });
            rb.query('propertyFilter', params.propertyFilter, { style: 'form', explode: true });
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
                    return r as StrictHttpResponse<SearchResultNode>;
                }),
            );
    }

    /**
     * Perform queries based on metadata sets V2.
     *
     * Perform queries based on metadata sets V2.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `searchV2$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    searchV2(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of metadataset (or &quot;-default-&quot; for default metadata set)
         */
        metadataset: string;

        /**
         * ID of query
         */
        query: string;

        /**
         * Type of element
         */
        contentType?:
            | 'FILES'
            | 'FOLDERS'
            | 'FILES_AND_FOLDERS'
            | 'COLLECTIONS'
            | 'TOOLPERMISSIONS'
            | 'ALL';

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

        /**
         * search parameters
         */
        body: SearchParameters;
    }): Observable<SearchResultNode> {
        return this.searchV2$Response(params).pipe(
            map((r: StrictHttpResponse<SearchResultNode>) => r.body as SearchResultNode),
        );
    }

    /**
     * Path part for operation searchFacets
     */
    static readonly SearchFacetsPath =
        '/search/v1/queriesV2/{repository}/{metadataset}/{query}/facets';

    /**
     * Search in facets.
     *
     * Perform queries based on metadata sets V2.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `searchFacets()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    searchFacets$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of metadataset (or &quot;-default-&quot; for default metadata set)
         */
        metadataset: string;

        /**
         * ID of query
         */
        query: string;

        /**
         * facet parameters
         */
        body: SearchParametersFacets;
    }): Observable<StrictHttpResponse<SearchResultNode>> {
        const rb = new RequestBuilder(this.rootUrl, SearchV1Service.SearchFacetsPath, 'post');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
            rb.path('metadataset', params.metadataset, { style: 'simple', explode: false });
            rb.path('query', params.query, { style: 'simple', explode: false });
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
                    return r as StrictHttpResponse<SearchResultNode>;
                }),
            );
    }

    /**
     * Search in facets.
     *
     * Perform queries based on metadata sets V2.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `searchFacets$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    searchFacets(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of metadataset (or &quot;-default-&quot; for default metadata set)
         */
        metadataset: string;

        /**
         * ID of query
         */
        query: string;

        /**
         * facet parameters
         */
        body: SearchParametersFacets;
    }): Observable<SearchResultNode> {
        return this.searchFacets$Response(params).pipe(
            map((r: StrictHttpResponse<SearchResultNode>) => r.body as SearchResultNode),
        );
    }

    /**
     * Path part for operation saveSearch
     */
    static readonly SaveSearchPath = '/search/v1/queriesV2/{repository}/{metadataset}/{query}/save';

    /**
     * Save a search query.
     *
     * Save a search query.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `saveSearch()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    saveSearch$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of metadataset (or &quot;-default-&quot; for default metadata set)
         */
        metadataset: string;

        /**
         * ID of query
         */
        query: string;

        /**
         * Name of the new search item
         */
        name: string;

        /**
         * Replace if search with the same name exists
         */
        replace?: boolean;

        /**
         * search parameters
         */
        body: Array<MdsQueryCriteria>;
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, SearchV1Service.SaveSearchPath, 'post');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
            rb.path('metadataset', params.metadataset, { style: 'simple', explode: false });
            rb.path('query', params.query, { style: 'simple', explode: false });
            rb.query('name', params.name, { style: 'form', explode: true });
            rb.query('replace', params.replace, { style: 'form', explode: true });
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
     * Save a search query.
     *
     * Save a search query.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `saveSearch$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    saveSearch(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of metadataset (or &quot;-default-&quot; for default metadata set)
         */
        metadataset: string;

        /**
         * ID of query
         */
        query: string;

        /**
         * Name of the new search item
         */
        name: string;

        /**
         * Replace if search with the same name exists
         */
        replace?: boolean;

        /**
         * search parameters
         */
        body: Array<MdsQueryCriteria>;
    }): Observable<NodeEntry> {
        return this.saveSearch$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation getRelevantNodes
     */
    static readonly GetRelevantNodesPath = '/search/v1/relevant/{repository}';

    /**
     * Get relevant nodes for the current user.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getRelevantNodes()` instead.
     *
     * This method doesn't expect any request body.
     */
    getRelevantNodes$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;

        /**
         * maximum items per page
         */
        maxItems?: number;

        /**
         * skip a number of items
         */
        skipCount?: number;
    }): Observable<StrictHttpResponse<SearchResultNode>> {
        const rb = new RequestBuilder(this.rootUrl, SearchV1Service.GetRelevantNodesPath, 'get');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
            rb.query('propertyFilter', params.propertyFilter, { style: 'form', explode: true });
            rb.query('maxItems', params.maxItems, { style: 'form', explode: true });
            rb.query('skipCount', params.skipCount, { style: 'form', explode: true });
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
                    return r as StrictHttpResponse<SearchResultNode>;
                }),
            );
    }

    /**
     * Get relevant nodes for the current user.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getRelevantNodes$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getRelevantNodes(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;

        /**
         * maximum items per page
         */
        maxItems?: number;

        /**
         * skip a number of items
         */
        skipCount?: number;
    }): Observable<SearchResultNode> {
        return this.getRelevantNodes$Response(params).pipe(
            map((r: StrictHttpResponse<SearchResultNode>) => r.body as SearchResultNode),
        );
    }
}
