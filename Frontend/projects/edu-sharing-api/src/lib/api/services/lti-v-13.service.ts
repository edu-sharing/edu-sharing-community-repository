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

import { DynamicRegistrationTokens } from '../models/dynamic-registration-tokens';
import { NodeLtiDeepLink } from '../models/node-lti-deep-link';
import { RegistrationUrl } from '../models/registration-url';

@Injectable({
    providedIn: 'root',
})
export class LtiV13Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation generateDeepLinkingResponse
     */
    static readonly GenerateDeepLinkingResponsePath = '/lti/v13/generateDeepLinkingResponse';

    /**
     * generate DeepLinkingResponse.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `generateDeepLinkingResponse()` instead.
     *
     * This method doesn't expect any request body.
     */
    generateDeepLinkingResponse$Response(params: {
        /**
         * selected node id&#x27;s
         */
        nodeIds: Array<string>;
    }): Observable<StrictHttpResponse<NodeLtiDeepLink>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            LtiV13Service.GenerateDeepLinkingResponsePath,
            'get',
        );
        if (params) {
            rb.query('nodeIds', params.nodeIds, {});
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
                    return r as StrictHttpResponse<NodeLtiDeepLink>;
                }),
            );
    }

    /**
     * generate DeepLinkingResponse.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `generateDeepLinkingResponse$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    generateDeepLinkingResponse(params: {
        /**
         * selected node id&#x27;s
         */
        nodeIds: Array<string>;
    }): Observable<NodeLtiDeepLink> {
        return this.generateDeepLinkingResponse$Response(params).pipe(
            map((r: StrictHttpResponse<NodeLtiDeepLink>) => r.body as NodeLtiDeepLink),
        );
    }

    /**
     * Path part for operation jwksUri
     */
    static readonly JwksUriPath = '/lti/v13/jwks';

    /**
     * LTI - returns repository JSON Web Key Sets.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `jwksUri()` instead.
     *
     * This method doesn't expect any request body.
     */
    jwksUri$Response(params?: {}): Observable<StrictHttpResponse<RegistrationUrl>> {
        const rb = new RequestBuilder(this.rootUrl, LtiV13Service.JwksUriPath, 'get');
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
                    return r as StrictHttpResponse<RegistrationUrl>;
                }),
            );
    }

    /**
     * LTI - returns repository JSON Web Key Sets.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `jwksUri$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    jwksUri(params?: {}): Observable<RegistrationUrl> {
        return this.jwksUri$Response(params).pipe(
            map((r: StrictHttpResponse<RegistrationUrl>) => r.body as RegistrationUrl),
        );
    }

    /**
     * Path part for operation loginInitiations
     */
    static readonly LoginInitiationsPath = '/lti/v13/oidc/login_initiations';

    /**
     * lti authentication process preparation.
     *
     * preflight phase. prepares lti authentication process. checks it issuer is valid
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `loginInitiations()` instead.
     *
     * This method sends `application/x-www-form-urlencoded` and handles request body of type `application/x-www-form-urlencoded`.
     */
    loginInitiations$Response(params?: {
        body?: {
            /**
             * Issuer of the request, will be validated
             */
            iss: string;

            /**
             * target url of platform at the end of the flow
             */
            target_link_uri: string;

            /**
             * Id of the issuer
             */
            client_id?: string;

            /**
             * context information of the platform
             */
            login_hint?: string;

            /**
             * additional context information of the platform
             */
            lti_message_hint?: string;

            /**
             * A can have multiple deployments in a platform
             */
            lti_deployment_id?: string;
        };
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, LtiV13Service.LoginInitiationsPath, 'post');
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
     * lti authentication process preparation.
     *
     * preflight phase. prepares lti authentication process. checks it issuer is valid
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `loginInitiations$Response()` instead.
     *
     * This method sends `application/x-www-form-urlencoded` and handles request body of type `application/x-www-form-urlencoded`.
     */
    loginInitiations(params?: {
        body?: {
            /**
             * Issuer of the request, will be validated
             */
            iss: string;

            /**
             * target url of platform at the end of the flow
             */
            target_link_uri: string;

            /**
             * Id of the issuer
             */
            client_id?: string;

            /**
             * context information of the platform
             */
            login_hint?: string;

            /**
             * additional context information of the platform
             */
            lti_message_hint?: string;

            /**
             * A can have multiple deployments in a platform
             */
            lti_deployment_id?: string;
        };
    }): Observable<string> {
        return this.loginInitiations$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation lti
     */
    static readonly LtiPath = '/lti/v13/lti13';

    /**
     * lti tool redirect.
     *
     * lti tool redirect
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `lti()` instead.
     *
     * This method sends `application/x-www-form-urlencoded` and handles request body of type `application/x-www-form-urlencoded`.
     */
    lti$Response(params?: {
        body?: {
            /**
             * Issuer of the request, will be validated
             */
            id_token: string;

            /**
             * Issuer of the request, will be validated
             */
            state: string;
        };
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, LtiV13Service.LtiPath, 'post');
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
     * lti tool redirect.
     *
     * lti tool redirect
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `lti$Response()` instead.
     *
     * This method sends `application/x-www-form-urlencoded` and handles request body of type `application/x-www-form-urlencoded`.
     */
    lti(params?: {
        body?: {
            /**
             * Issuer of the request, will be validated
             */
            id_token: string;

            /**
             * Issuer of the request, will be validated
             */
            state: string;
        };
    }): Observable<string> {
        return this.lti$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation ltiRegistrationDynamic
     */
    static readonly LtiRegistrationDynamicPath = '/lti/v13/registration/dynamic/{token}';

    /**
     * LTI Dynamic Registration - Initiate registration.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `ltiRegistrationDynamic()` instead.
     *
     * This method doesn't expect any request body.
     */
    ltiRegistrationDynamic$Response(params: {
        /**
         * the endpoint to the open id configuration to be used for this registration
         */
        openid_configuration: string;

        /**
         * the registration access token. If present, it must be used as the access token by the tool when making the registration request to the registration endpoint exposed in the openid configuration.
         */
        registration_token?: string;

        /**
         * one time usage token which is autogenerated with the url in edu-sharing admin gui.
         */
        token: string;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            LtiV13Service.LtiRegistrationDynamicPath,
            'get',
        );
        if (params) {
            rb.query('openid_configuration', params.openid_configuration, {});
            rb.query('registration_token', params.registration_token, {});
            rb.path('token', params.token, {});
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
     * LTI Dynamic Registration - Initiate registration.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `ltiRegistrationDynamic$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    ltiRegistrationDynamic(params: {
        /**
         * the endpoint to the open id configuration to be used for this registration
         */
        openid_configuration: string;

        /**
         * the registration access token. If present, it must be used as the access token by the tool when making the registration request to the registration endpoint exposed in the openid configuration.
         */
        registration_token?: string;

        /**
         * one time usage token which is autogenerated with the url in edu-sharing admin gui.
         */
        token: string;
    }): Observable<string> {
        return this.ltiRegistrationDynamic$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation ltiRegistrationUrl
     */
    static readonly LtiRegistrationUrlPath = '/lti/v13/registration/url';

    /**
     * LTI Dynamic Registration - generates url for platform.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `ltiRegistrationUrl()` instead.
     *
     * This method doesn't expect any request body.
     */
    ltiRegistrationUrl$Response(params: {
        /**
         * if to add a ne url to the list
         */
        generate: boolean;
    }): Observable<StrictHttpResponse<DynamicRegistrationTokens>> {
        const rb = new RequestBuilder(this.rootUrl, LtiV13Service.LtiRegistrationUrlPath, 'get');
        if (params) {
            rb.query('generate', params.generate, {});
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
                    return r as StrictHttpResponse<DynamicRegistrationTokens>;
                }),
            );
    }

    /**
     * LTI Dynamic Registration - generates url for platform.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `ltiRegistrationUrl$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    ltiRegistrationUrl(params: {
        /**
         * if to add a ne url to the list
         */
        generate: boolean;
    }): Observable<DynamicRegistrationTokens> {
        return this.ltiRegistrationUrl$Response(params).pipe(
            map(
                (r: StrictHttpResponse<DynamicRegistrationTokens>) =>
                    r.body as DynamicRegistrationTokens,
            ),
        );
    }

    /**
     * Path part for operation ltiTarget
     */
    static readonly LtiTargetPath = '/lti/v13/lti13/{nodeId}';

    /**
     * lti tool resource link target.
     *
     * used by some platforms for direct (without oidc login_init) launch requests
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `ltiTarget()` instead.
     *
     * This method sends `application/x-www-form-urlencoded` and handles request body of type `application/x-www-form-urlencoded`.
     */
    ltiTarget$Response(params: {
        /**
         * edu-sharing node id
         */
        nodeId: string;
        body?: {
            /**
             * Issuer of the request, will be validated
             */
            id_token: string;

            /**
             * Issuer of the request, will be validated
             */
            state: string;
        };
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, LtiV13Service.LtiTargetPath, 'post');
        if (params) {
            rb.path('nodeId', params.nodeId, {});
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
     * lti tool resource link target.
     *
     * used by some platforms for direct (without oidc login_init) launch requests
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `ltiTarget$Response()` instead.
     *
     * This method sends `application/x-www-form-urlencoded` and handles request body of type `application/x-www-form-urlencoded`.
     */
    ltiTarget(params: {
        /**
         * edu-sharing node id
         */
        nodeId: string;
        body?: {
            /**
             * Issuer of the request, will be validated
             */
            id_token: string;

            /**
             * Issuer of the request, will be validated
             */
            state: string;
        };
    }): Observable<string> {
        return this.ltiTarget$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation registerByType
     */
    static readonly RegisterByTypePath = '/lti/v13/registration/{type}';

    /**
     * register LTI platform.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `registerByType()` instead.
     *
     * This method doesn't expect any request body.
     */
    registerByType$Response(params: {
        /**
         * lti platform typ i.e. moodle
         */
        type: 'moodle';

        /**
         * base url i.e. http://localhost/moodle used as platformId
         */
        baseUrl: string;

        /**
         * client id
         */
        client_id?: string;

        /**
         * deployment id
         */
        deployment_id?: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, LtiV13Service.RegisterByTypePath, 'post');
        if (params) {
            rb.path('type', params.type, {});
            rb.query('baseUrl', params.baseUrl, {});
            rb.query('client_id', params.client_id, {});
            rb.query('deployment_id', params.deployment_id, {});
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
     * register LTI platform.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `registerByType$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    registerByType(params: {
        /**
         * lti platform typ i.e. moodle
         */
        type: 'moodle';

        /**
         * base url i.e. http://localhost/moodle used as platformId
         */
        baseUrl: string;

        /**
         * client id
         */
        client_id?: string;

        /**
         * deployment id
         */
        deployment_id?: string;
    }): Observable<any> {
        return this.registerByType$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation registerTest
     */
    static readonly RegisterTestPath = '/lti/v13/registration/static';

    /**
     * register LTI platform.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `registerTest()` instead.
     *
     * This method doesn't expect any request body.
     */
    registerTest$Response(params: {
        /**
         * the issuer
         */
        platformId: string;

        /**
         * client id
         */
        client_id: string;

        /**
         * deployment id
         */
        deployment_id: string;

        /**
         * oidc endpoint, authentication request url
         */
        authentication_request_url: string;

        /**
         * jwks endpoint, keyset url
         */
        keyset_url: string;

        /**
         * jwks key id
         */
        key_id?: string;

        /**
         * auth token url
         */
        auth_token_url: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, LtiV13Service.RegisterTestPath, 'post');
        if (params) {
            rb.query('platformId', params.platformId, {});
            rb.query('client_id', params.client_id, {});
            rb.query('deployment_id', params.deployment_id, {});
            rb.query('authentication_request_url', params.authentication_request_url, {});
            rb.query('keyset_url', params.keyset_url, {});
            rb.query('key_id', params.key_id, {});
            rb.query('auth_token_url', params.auth_token_url, {});
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
     * register LTI platform.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `registerTest$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    registerTest(params: {
        /**
         * the issuer
         */
        platformId: string;

        /**
         * client id
         */
        client_id: string;

        /**
         * deployment id
         */
        deployment_id: string;

        /**
         * oidc endpoint, authentication request url
         */
        authentication_request_url: string;

        /**
         * jwks endpoint, keyset url
         */
        keyset_url: string;

        /**
         * jwks key id
         */
        key_id?: string;

        /**
         * auth token url
         */
        auth_token_url: string;
    }): Observable<any> {
        return this.registerTest$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation removeLtiRegistrationUrl
     */
    static readonly RemoveLtiRegistrationUrlPath = '/lti/v13/registration/url/{token}';

    /**
     * LTI Dynamic Regitration - delete url.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `removeLtiRegistrationUrl()` instead.
     *
     * This method doesn't expect any request body.
     */
    removeLtiRegistrationUrl$Response(params: {
        /**
         * the token of the link you have to remove
         */
        token: string;
    }): Observable<StrictHttpResponse<DynamicRegistrationTokens>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            LtiV13Service.RemoveLtiRegistrationUrlPath,
            'delete',
        );
        if (params) {
            rb.path('token', params.token, {});
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
                    return r as StrictHttpResponse<DynamicRegistrationTokens>;
                }),
            );
    }

    /**
     * LTI Dynamic Regitration - delete url.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `removeLtiRegistrationUrl$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    removeLtiRegistrationUrl(params: {
        /**
         * the token of the link you have to remove
         */
        token: string;
    }): Observable<DynamicRegistrationTokens> {
        return this.removeLtiRegistrationUrl$Response(params).pipe(
            map(
                (r: StrictHttpResponse<DynamicRegistrationTokens>) =>
                    r.body as DynamicRegistrationTokens,
            ),
        );
    }
}
