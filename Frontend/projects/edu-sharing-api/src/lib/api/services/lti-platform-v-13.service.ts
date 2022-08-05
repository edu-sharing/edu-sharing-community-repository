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

import { ManualRegistrationData } from '../models/manual-registration-data';
import { OpenIdConfiguration } from '../models/open-id-configuration';
import { OpenIdRegistrationResult } from '../models/open-id-registration-result';
import { Tools } from '../models/tools';

@Injectable({
    providedIn: 'root',
})
export class LtiPlatformV13Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation auth
     */
    static readonly AuthPath = '/ltiplatform/v13/auth';

    /**
     * LTI Platform oidc endpoint. responds to a login authentication request.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `auth()` instead.
     *
     * This method doesn't expect any request body.
     */
    auth$Response(params: {
        /**
         * scope
         */
        scope: string;

        /**
         * response_type
         */
        response_type: string;

        /**
         * optional parameter client_id specifies the client id for the authorization server that should be used to authorize the subsequent LTI message request
         */
        client_id?: string;

        /**
         * login_hint
         */
        login_hint: string;

        /**
         * state
         */
        state: string;

        /**
         * response_mode
         */
        response_mode: string;

        /**
         * nonce
         */
        nonce: string;

        /**
         * prompt
         */
        prompt: string;

        /**
         * Similarly to the login_hint parameter, lti_message_hint value is opaque to the tool. If present in the login initiation request, the tool MUST include it back in the authentication request unaltered
         */
        lti_message_hint?: string;

        /**
         * redirect_uri
         */
        redirect_uri: string;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, LtiPlatformV13Service.AuthPath, 'get');
        if (params) {
            rb.query('scope', params.scope, {});
            rb.query('response_type', params.response_type, {});
            rb.query('client_id', params.client_id, {});
            rb.query('login_hint', params.login_hint, {});
            rb.query('state', params.state, {});
            rb.query('response_mode', params.response_mode, {});
            rb.query('nonce', params.nonce, {});
            rb.query('prompt', params.prompt, {});
            rb.query('lti_message_hint', params.lti_message_hint, {});
            rb.query('redirect_uri', params.redirect_uri, {});
        }

        return this.http
            .request(
                rb.build({
                    responseType: 'text',
                    accept: 'text/html',
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
     * LTI Platform oidc endpoint. responds to a login authentication request.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `auth$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    auth(params: {
        /**
         * scope
         */
        scope: string;

        /**
         * response_type
         */
        response_type: string;

        /**
         * optional parameter client_id specifies the client id for the authorization server that should be used to authorize the subsequent LTI message request
         */
        client_id?: string;

        /**
         * login_hint
         */
        login_hint: string;

        /**
         * state
         */
        state: string;

        /**
         * response_mode
         */
        response_mode: string;

        /**
         * nonce
         */
        nonce: string;

        /**
         * prompt
         */
        prompt: string;

        /**
         * Similarly to the login_hint parameter, lti_message_hint value is opaque to the tool. If present in the login initiation request, the tool MUST include it back in the authentication request unaltered
         */
        lti_message_hint?: string;

        /**
         * redirect_uri
         */
        redirect_uri: string;
    }): Observable<string> {
        return this.auth$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation authTokenEndpoint
     */
    static readonly AuthTokenEndpointPath = '/ltiplatform/v13/token';

    /**
     * LTIPlatform auth token endpoint.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `authTokenEndpoint()` instead.
     *
     * This method doesn't expect any request body.
     */
    authTokenEndpoint$Response(params?: {}): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            LtiPlatformV13Service.AuthTokenEndpointPath,
            'get',
        );
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
                    return r as StrictHttpResponse<any>;
                }),
            );
    }

    /**
     * LTIPlatform auth token endpoint.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `authTokenEndpoint$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    authTokenEndpoint(params?: {}): Observable<any> {
        return this.authTokenEndpoint$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation manualRegistration
     */
    static readonly ManualRegistrationPath = '/ltiplatform/v13/manual-registration';

    /**
     * manual registration endpoint for registration of tools.
     *
     * tool registration
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `manualRegistration()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    manualRegistration$Response(params: {
        /**
         * registrationData
         */
        body: ManualRegistrationData;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            LtiPlatformV13Service.ManualRegistrationPath,
            'post',
        );
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
                    return r as StrictHttpResponse<any>;
                }),
            );
    }

    /**
     * manual registration endpoint for registration of tools.
     *
     * tool registration
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `manualRegistration$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    manualRegistration(params: {
        /**
         * registrationData
         */
        body: ManualRegistrationData;
    }): Observable<any> {
        return this.manualRegistration$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation openIdRegistration
     */
    static readonly OpenIdRegistrationPath = '/ltiplatform/v13/openid-registration';

    /**
     * registration endpoint the tool uses to register at platform.
     *
     * tool registration
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `openIdRegistration()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    openIdRegistration$Response(params: {
        /**
         * registrationpayload
         */
        body: string;
    }): Observable<StrictHttpResponse<OpenIdRegistrationResult>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            LtiPlatformV13Service.OpenIdRegistrationPath,
            'post',
        );
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
                    return r as StrictHttpResponse<OpenIdRegistrationResult>;
                }),
            );
    }

    /**
     * registration endpoint the tool uses to register at platform.
     *
     * tool registration
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `openIdRegistration$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    openIdRegistration(params: {
        /**
         * registrationpayload
         */
        body: string;
    }): Observable<OpenIdRegistrationResult> {
        return this.openIdRegistration$Response(params).pipe(
            map(
                (r: StrictHttpResponse<OpenIdRegistrationResult>) =>
                    r.body as OpenIdRegistrationResult,
            ),
        );
    }

    /**
     * Path part for operation openidConfiguration
     */
    static readonly OpenidConfigurationPath = '/ltiplatform/v13/openid-configuration';

    /**
     * LTIPlatform openid configuration.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `openidConfiguration()` instead.
     *
     * This method doesn't expect any request body.
     */
    openidConfiguration$Response(params?: {}): Observable<StrictHttpResponse<OpenIdConfiguration>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            LtiPlatformV13Service.OpenidConfigurationPath,
            'get',
        );
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
                    return r as StrictHttpResponse<OpenIdConfiguration>;
                }),
            );
    }

    /**
     * LTIPlatform openid configuration.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `openidConfiguration$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    openidConfiguration(params?: {}): Observable<OpenIdConfiguration> {
        return this.openidConfiguration$Response(params).pipe(
            map((r: StrictHttpResponse<OpenIdConfiguration>) => r.body as OpenIdConfiguration),
        );
    }

    /**
     * Path part for operation startDynamicRegistration
     */
    static readonly StartDynamicRegistrationPath = '/ltiplatform/v13/start-dynamic-registration';

    /**
     * starts lti dynamic registration.
     *
     * start dynmic registration
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `startDynamicRegistration()` instead.
     *
     * This method sends `application/x-www-form-urlencoded` and handles request body of type `application/x-www-form-urlencoded`.
     */
    startDynamicRegistration$Response(params?: {
        body?: {
            /**
             * url
             */
            url: string;
        };
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            LtiPlatformV13Service.StartDynamicRegistrationPath,
            'post',
        );
        if (params) {
            rb.body(params.body, 'application/x-www-form-urlencoded');
        }

        return this.http
            .request(
                rb.build({
                    responseType: 'text',
                    accept: 'text/html',
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
     * starts lti dynamic registration.
     *
     * start dynmic registration
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `startDynamicRegistration$Response()` instead.
     *
     * This method sends `application/x-www-form-urlencoded` and handles request body of type `application/x-www-form-urlencoded`.
     */
    startDynamicRegistration(params?: {
        body?: {
            /**
             * url
             */
            url: string;
        };
    }): Observable<string> {
        return this.startDynamicRegistration$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation tools
     */
    static readonly ToolsPath = '/ltiplatform/v13/tools';

    /**
     * List of tools registered.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `tools()` instead.
     *
     * This method doesn't expect any request body.
     */
    tools$Response(params?: {}): Observable<StrictHttpResponse<Tools>> {
        const rb = new RequestBuilder(this.rootUrl, LtiPlatformV13Service.ToolsPath, 'get');
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
                    return r as StrictHttpResponse<Tools>;
                }),
            );
    }

    /**
     * List of tools registered.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `tools$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    tools(params?: {}): Observable<Tools> {
        return this.tools$Response(params).pipe(
            map((r: StrictHttpResponse<Tools>) => r.body as Tools),
        );
    }
}
