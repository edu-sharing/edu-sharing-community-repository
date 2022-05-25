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

import { Service } from '../models/service';
import { StoredService } from '../models/stored-service';

@Injectable({
    providedIn: 'root',
})
export class NetworkV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation getServices
     */
    static readonly GetServicesPath = '/network/v1/services';

    /**
     * Get services.
     *
     * Get registerted services.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getServices()` instead.
     *
     * This method doesn't expect any request body.
     */
    getServices$Response(params?: {
        /**
         * search or filter for services
         */
        query?: string;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, NetworkV1Service.GetServicesPath, 'get');
        if (params) {
            rb.query('query', params.query, {});
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
                    return r as StrictHttpResponse<string>;
                }),
            );
    }

    /**
     * Get services.
     *
     * Get registerted services.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getServices$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getServices(params?: {
        /**
         * search or filter for services
         */
        query?: string;
    }): Observable<string> {
        return this.getServices$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation addService
     */
    static readonly AddServicePath = '/network/v1/services';

    /**
     * Register service.
     *
     * Register a new service.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `addService()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    addService$Response(params?: {
        /**
         * Service data object
         */
        body?: Service;
    }): Observable<StrictHttpResponse<StoredService>> {
        const rb = new RequestBuilder(this.rootUrl, NetworkV1Service.AddServicePath, 'post');
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
                    return r as StrictHttpResponse<StoredService>;
                }),
            );
    }

    /**
     * Register service.
     *
     * Register a new service.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `addService$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    addService(params?: {
        /**
         * Service data object
         */
        body?: Service;
    }): Observable<StoredService> {
        return this.addService$Response(params).pipe(
            map((r: StrictHttpResponse<StoredService>) => r.body as StoredService),
        );
    }

    /**
     * Path part for operation getRepositories
     */
    static readonly GetRepositoriesPath = '/network/v1/repositories';

    /**
     * Get repositories.
     *
     * Get repositories.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getRepositories()` instead.
     *
     * This method doesn't expect any request body.
     */
    getRepositories$Response(params?: {}): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, NetworkV1Service.GetRepositoriesPath, 'get');
        if (params) {
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
                    return r as StrictHttpResponse<string>;
                }),
            );
    }

    /**
     * Get repositories.
     *
     * Get repositories.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getRepositories$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getRepositories(params?: {}): Observable<string> {
        return this.getRepositories$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation getService
     */
    static readonly GetServicePath = '/network/v1/service';

    /**
     * Get own service.
     *
     * Get the servic entry from the current repository.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getService()` instead.
     *
     * This method doesn't expect any request body.
     */
    getService$Response(params?: {}): Observable<StrictHttpResponse<StoredService>> {
        const rb = new RequestBuilder(this.rootUrl, NetworkV1Service.GetServicePath, 'get');
        if (params) {
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
                    return r as StrictHttpResponse<StoredService>;
                }),
            );
    }

    /**
     * Get own service.
     *
     * Get the servic entry from the current repository.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getService$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getService(params?: {}): Observable<StoredService> {
        return this.getService$Response(params).pipe(
            map((r: StrictHttpResponse<StoredService>) => r.body as StoredService),
        );
    }

    /**
     * Path part for operation updateService
     */
    static readonly UpdateServicePath = '/network/v1/services/{id}';

    /**
     * Update a service.
     *
     * Update an existing service.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `updateService()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    updateService$Response(params: {
        /**
         * Service id
         */
        id: string;

        /**
         * Service data object
         */
        body?: Service;
    }): Observable<StrictHttpResponse<StoredService>> {
        const rb = new RequestBuilder(this.rootUrl, NetworkV1Service.UpdateServicePath, 'put');
        if (params) {
            rb.path('id', params.id, {});
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
                    return r as StrictHttpResponse<StoredService>;
                }),
            );
    }

    /**
     * Update a service.
     *
     * Update an existing service.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `updateService$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    updateService(params: {
        /**
         * Service id
         */
        id: string;

        /**
         * Service data object
         */
        body?: Service;
    }): Observable<StoredService> {
        return this.updateService$Response(params).pipe(
            map((r: StrictHttpResponse<StoredService>) => r.body as StoredService),
        );
    }
}
