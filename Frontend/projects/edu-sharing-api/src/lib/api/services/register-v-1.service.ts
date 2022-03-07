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

import { RegisterExists } from '../models/register-exists';
import { RegisterInformation } from '../models/register-information';

@Injectable({
    providedIn: 'root',
})
export class RegisterV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation activate
     */
    static readonly ActivatePath = '/register/v1/activate/{key}';

    /**
     * Activate a new user (by using a supplied key).
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `activate()` instead.
     *
     * This method doesn't expect any request body.
     */
    activate$Response(params: {
        /**
         * The key for the user to activate
         */
        key: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, RegisterV1Service.ActivatePath, 'post');
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
                    return r as StrictHttpResponse<any>;
                }),
            );
    }

    /**
     * Activate a new user (by using a supplied key).
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `activate$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    activate(params: {
        /**
         * The key for the user to activate
         */
        key: string;
    }): Observable<any> {
        return this.activate$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation mailExists
     */
    static readonly MailExistsPath = '/register/v1/exists/{mail}';

    /**
     * Check if the given mail is already successfully registered.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `mailExists()` instead.
     *
     * This method doesn't expect any request body.
     */
    mailExists$Response(params: {
        /**
         * The mail (authority) of the user to check
         */
        mail: string;
    }): Observable<StrictHttpResponse<RegisterExists>> {
        const rb = new RequestBuilder(this.rootUrl, RegisterV1Service.MailExistsPath, 'get');
        if (params) {
            rb.path('mail', params.mail, {});
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
                    return r as StrictHttpResponse<RegisterExists>;
                }),
            );
    }

    /**
     * Check if the given mail is already successfully registered.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `mailExists$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    mailExists(params: {
        /**
         * The mail (authority) of the user to check
         */
        mail: string;
    }): Observable<RegisterExists> {
        return this.mailExists$Response(params).pipe(
            map((r: StrictHttpResponse<RegisterExists>) => r.body as RegisterExists),
        );
    }

    /**
     * Path part for operation recoverPassword
     */
    static readonly RecoverPasswordPath = '/register/v1/recover/{mail}';

    /**
     * Send a mail to recover/reset password.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `recoverPassword()` instead.
     *
     * This method doesn't expect any request body.
     */
    recoverPassword$Response(params: {
        /**
         * The mail (authority) of the user to recover
         */
        mail: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, RegisterV1Service.RecoverPasswordPath, 'post');
        if (params) {
            rb.path('mail', params.mail, {});
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
     * Send a mail to recover/reset password.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `recoverPassword$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    recoverPassword(params: {
        /**
         * The mail (authority) of the user to recover
         */
        mail: string;
    }): Observable<any> {
        return this.recoverPassword$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation register
     */
    static readonly RegisterPath = '/register/v1/register';

    /**
     * Register a new user.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `register()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    register$Response(params?: {
        body?: RegisterInformation;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, RegisterV1Service.RegisterPath, 'post');
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
     * Register a new user.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `register$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    register(params?: { body?: RegisterInformation }): Observable<any> {
        return this.register$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation resendMail
     */
    static readonly ResendMailPath = '/register/v1/resend/{mail}';

    /**
     * Resend a registration mail for a given mail address.
     *
     * The method will return false if there is no pending registration for the given mail
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `resendMail()` instead.
     *
     * This method doesn't expect any request body.
     */
    resendMail$Response(params: {
        /**
         * The mail a registration is pending for and should be resend to
         */
        mail: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, RegisterV1Service.ResendMailPath, 'post');
        if (params) {
            rb.path('mail', params.mail, {});
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
     * Resend a registration mail for a given mail address.
     *
     * The method will return false if there is no pending registration for the given mail
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `resendMail$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    resendMail(params: {
        /**
         * The mail a registration is pending for and should be resend to
         */
        mail: string;
    }): Observable<any> {
        return this.resendMail$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation resetPassword
     */
    static readonly ResetPasswordPath = '/register/v1/reset/{key}/{password}';

    /**
     * Send a mail to recover/reset password.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `resetPassword()` instead.
     *
     * This method doesn't expect any request body.
     */
    resetPassword$Response(params: {
        /**
         * The key for the password reset request
         */
        key: string;

        /**
         * The new password for the user
         */
        password: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, RegisterV1Service.ResetPasswordPath, 'post');
        if (params) {
            rb.path('key', params.key, {});
            rb.path('password', params.password, {});
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
     * Send a mail to recover/reset password.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `resetPassword$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    resetPassword(params: {
        /**
         * The key for the password reset request
         */
        key: string;

        /**
         * The new password for the user
         */
        password: string;
    }): Observable<any> {
        return this.resetPassword$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }
}
