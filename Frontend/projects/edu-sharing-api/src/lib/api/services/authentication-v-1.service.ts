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

import { AuthenticationToken } from '../models/authentication-token';
import { Login } from '../models/login';
import { LoginCredentials } from '../models/login-credentials';
import { UserProfileAppAuth } from '../models/user-profile-app-auth';

@Injectable({
    providedIn: 'root',
})
export class AuthenticationV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation authenticate
     */
    static readonly AuthenticatePath = '/authentication/v1/appauth/{userId}';

    /**
     * authenticate user of an registered application.
     *
     * headers must be set: X-Edu-App-Id, X-Edu-App-Sig, X-Edu-App-Signed, X-Edu-App-Ts
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `authenticate()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    authenticate$Response(params: {
        /**
         * User Id
         */
        userId: string;

        /**
         * User Profile
         */
        body?: UserProfileAppAuth;
    }): Observable<StrictHttpResponse<AuthenticationToken>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            AuthenticationV1Service.AuthenticatePath,
            'post',
        );
        if (params) {
            rb.path('userId', params.userId, {});
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
                    return r as StrictHttpResponse<AuthenticationToken>;
                }),
            );
    }

    /**
     * authenticate user of an registered application.
     *
     * headers must be set: X-Edu-App-Id, X-Edu-App-Sig, X-Edu-App-Signed, X-Edu-App-Ts
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `authenticate$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    authenticate(params: {
        /**
         * User Id
         */
        userId: string;

        /**
         * User Profile
         */
        body?: UserProfileAppAuth;
    }): Observable<AuthenticationToken> {
        return this.authenticate$Response(params).pipe(
            map((r: StrictHttpResponse<AuthenticationToken>) => r.body as AuthenticationToken),
        );
    }

    /**
     * Path part for operation hasAccessToScope
     */
    static readonly HasAccessToScopePath = '/authentication/v1/hasAccessToScope';

    /**
     * Returns true if the current user has access to the given scope.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `hasAccessToScope()` instead.
     *
     * This method doesn't expect any request body.
     */
    hasAccessToScope$Response(params: {
        /**
         * scope
         */
        scope: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            AuthenticationV1Service.HasAccessToScopePath,
            'get',
        );
        if (params) {
            rb.query('scope', params.scope, {});
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
     * Returns true if the current user has access to the given scope.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `hasAccessToScope$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    hasAccessToScope(params: {
        /**
         * scope
         */
        scope: string;
    }): Observable<any> {
        return this.hasAccessToScope$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation login
     */
    static readonly LoginPath = '/authentication/v1/validateSession';

    /**
     * Validates the Basic Auth Credentials and check if the session is a logged in user.
     *
     * Use the Basic auth header field to transfer the credentials
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `login()` instead.
     *
     * This method doesn't expect any request body.
     */
    login$Response(params?: {}): Observable<StrictHttpResponse<Login>> {
        const rb = new RequestBuilder(this.rootUrl, AuthenticationV1Service.LoginPath, 'get');
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
                    return r as StrictHttpResponse<Login>;
                }),
            );
    }

    /**
     * Validates the Basic Auth Credentials and check if the session is a logged in user.
     *
     * Use the Basic auth header field to transfer the credentials
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `login$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    login(params?: {}): Observable<Login> {
        return this.login$Response(params).pipe(
            map((r: StrictHttpResponse<Login>) => r.body as Login),
        );
    }

    /**
     * Path part for operation loginToScope
     */
    static readonly LoginToScopePath = '/authentication/v1/loginToScope';

    /**
     * Validates the Basic Auth Credentials and check if the session is a logged in user.
     *
     * Use the Basic auth header field to transfer the credentials
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `loginToScope()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    loginToScope$Response(params: {
        /**
         * credentials, example: test,test
         */
        body: LoginCredentials;
    }): Observable<StrictHttpResponse<Login>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            AuthenticationV1Service.LoginToScopePath,
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
                    return r as StrictHttpResponse<Login>;
                }),
            );
    }

    /**
     * Validates the Basic Auth Credentials and check if the session is a logged in user.
     *
     * Use the Basic auth header field to transfer the credentials
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `loginToScope$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    loginToScope(params: {
        /**
         * credentials, example: test,test
         */
        body: LoginCredentials;
    }): Observable<Login> {
        return this.loginToScope$Response(params).pipe(
            map((r: StrictHttpResponse<Login>) => r.body as Login),
        );
    }

    /**
     * Path part for operation logout
     */
    static readonly LogoutPath = '/authentication/v1/destroySession';

    /**
     * Destroys the current session and logout the user.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `logout()` instead.
     *
     * This method doesn't expect any request body.
     */
    logout$Response(params?: {}): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, AuthenticationV1Service.LogoutPath, 'get');
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
     * Destroys the current session and logout the user.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `logout$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    logout(params?: {}): Observable<any> {
        return this.logout$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }
}
