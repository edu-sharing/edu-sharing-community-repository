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

import { Config } from '../models/config';
import { DynamicConfig } from '../models/dynamic-config';
import { Language } from '../models/language';
import { Variables } from '../models/variables';

@Injectable({
    providedIn: 'root',
})
export class ConfigV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation getConfig1
     */
    static readonly GetConfig1Path = '/config/v1/values';

    /**
     * get repository config values.
     *
     * Current is the actual (context-based) active config. Global is the default global config if no context is active (may be identical to the current)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getConfig1()` instead.
     *
     * This method doesn't expect any request body.
     */
    getConfig1$Response(params?: {}): Observable<StrictHttpResponse<Config>> {
        const rb = new RequestBuilder(this.rootUrl, ConfigV1Service.GetConfig1Path, 'get');
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
                    return r as StrictHttpResponse<Config>;
                }),
            );
    }

    /**
     * get repository config values.
     *
     * Current is the actual (context-based) active config. Global is the default global config if no context is active (may be identical to the current)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getConfig1$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getConfig1(params?: {}): Observable<Config> {
        return this.getConfig1$Response(params).pipe(
            map((r: StrictHttpResponse<Config>) => r.body as Config),
        );
    }

    /**
     * Path part for operation getDynamicValue
     */
    static readonly GetDynamicValuePath = '/config/v1/dynamic/{key}';

    /**
     * Get a config entry (appropriate rights for the entry are required).
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getDynamicValue()` instead.
     *
     * This method doesn't expect any request body.
     */
    getDynamicValue$Response(params: {
        /**
         * Key of the config value that should be fetched
         */
        key: string;
    }): Observable<StrictHttpResponse<DynamicConfig>> {
        const rb = new RequestBuilder(this.rootUrl, ConfigV1Service.GetDynamicValuePath, 'get');
        if (params) {
            rb.path('key', params.key, {});
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
                    return r as StrictHttpResponse<DynamicConfig>;
                }),
            );
    }

    /**
     * Get a config entry (appropriate rights for the entry are required).
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getDynamicValue$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getDynamicValue(params: {
        /**
         * Key of the config value that should be fetched
         */
        key: string;
    }): Observable<DynamicConfig> {
        return this.getDynamicValue$Response(params).pipe(
            map((r: StrictHttpResponse<DynamicConfig>) => r.body as DynamicConfig),
        );
    }

    /**
     * Path part for operation setDynamicValue
     */
    static readonly SetDynamicValuePath = '/config/v1/dynamic/{key}';

    /**
     * Set a config entry (admin rights required).
     *
     * the body must be a json encapsulated string
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `setDynamicValue()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    setDynamicValue$Response(params: {
        /**
         * Key of the config value that should be fetched
         */
        key: string;

        /**
         * Is everyone allowed to read the value
         */
        public: boolean;

        /**
         * Must be a json-encapsulated string
         */
        body: string;
    }): Observable<StrictHttpResponse<DynamicConfig>> {
        const rb = new RequestBuilder(this.rootUrl, ConfigV1Service.SetDynamicValuePath, 'post');
        if (params) {
            rb.path('key', params.key, {});
            rb.query('public', params.public, {});
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
                    return r as StrictHttpResponse<DynamicConfig>;
                }),
            );
    }

    /**
     * Set a config entry (admin rights required).
     *
     * the body must be a json encapsulated string
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `setDynamicValue$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    setDynamicValue(params: {
        /**
         * Key of the config value that should be fetched
         */
        key: string;

        /**
         * Is everyone allowed to read the value
         */
        public: boolean;

        /**
         * Must be a json-encapsulated string
         */
        body: string;
    }): Observable<DynamicConfig> {
        return this.setDynamicValue$Response(params).pipe(
            map((r: StrictHttpResponse<DynamicConfig>) => r.body as DynamicConfig),
        );
    }

    /**
     * Path part for operation getLanguage
     */
    static readonly GetLanguagePath = '/config/v1/language';

    /**
     * get override strings for the current language.
     *
     * Language strings
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getLanguage()` instead.
     *
     * This method doesn't expect any request body.
     */
    getLanguage$Response(params?: {}): Observable<StrictHttpResponse<Language>> {
        const rb = new RequestBuilder(this.rootUrl, ConfigV1Service.GetLanguagePath, 'get');
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
                    return r as StrictHttpResponse<Language>;
                }),
            );
    }

    /**
     * get override strings for the current language.
     *
     * Language strings
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getLanguage$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getLanguage(params?: {}): Observable<Language> {
        return this.getLanguage$Response(params).pipe(
            map((r: StrictHttpResponse<Language>) => r.body as Language),
        );
    }

    /**
     * Path part for operation getLanguageDefaults
     */
    static readonly GetLanguageDefaultsPath = '/config/v1/language/defaults';

    /**
     * get all inital language strings for angular.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getLanguageDefaults()` instead.
     *
     * This method doesn't expect any request body.
     */
    getLanguageDefaults$Response(params?: {}): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, ConfigV1Service.GetLanguageDefaultsPath, 'get');
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
     * get all inital language strings for angular.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getLanguageDefaults$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getLanguageDefaults(params?: {}): Observable<string> {
        return this.getLanguageDefaults$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation getVariables
     */
    static readonly GetVariablesPath = '/config/v1/variables';

    /**
     * get global config variables.
     *
     * global config variables
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getVariables()` instead.
     *
     * This method doesn't expect any request body.
     */
    getVariables$Response(params?: {}): Observable<StrictHttpResponse<Variables>> {
        const rb = new RequestBuilder(this.rootUrl, ConfigV1Service.GetVariablesPath, 'get');
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
                    return r as StrictHttpResponse<Variables>;
                }),
            );
    }

    /**
     * get global config variables.
     *
     * global config variables
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getVariables$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getVariables(params?: {}): Observable<Variables> {
        return this.getVariables$Response(params).pipe(
            map((r: StrictHttpResponse<Variables>) => r.body as Variables),
        );
    }
}
