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

import { About } from '../models/about';

@Injectable({
    providedIn: 'root',
})
export class AboutService extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation about
     */
    static readonly AboutPath = '/_about';

    /**
     * Discover the API.
     *
     * Get all services provided by this API.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `about()` instead.
     *
     * This method doesn't expect any request body.
     */
    about$Response(params?: {}): Observable<StrictHttpResponse<About>> {
        const rb = new RequestBuilder(this.rootUrl, AboutService.AboutPath, 'get');
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
                    return r as StrictHttpResponse<About>;
                }),
            );
    }

    /**
     * Discover the API.
     *
     * Get all services provided by this API.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `about$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    about(params?: {}): Observable<About> {
        return this.about$Response(params).pipe(
            map((r: StrictHttpResponse<About>) => r.body as About),
        );
    }

    /**
     * Path part for operation status
     */
    static readonly StatusPath = '/_about/status/{mode}';

    /**
     * status of repo services.
     *
     * returns http status 200 when ok
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `status()` instead.
     *
     * This method doesn't expect any request body.
     */
    status$Response(params: {
        mode: 'SEARCH' | 'SERVICE';
        timeoutSeconds?: number;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, AboutService.StatusPath, 'get');
        if (params) {
            rb.path('mode', params.mode, {});
            rb.query('timeoutSeconds', params.timeoutSeconds, {});
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
     * status of repo services.
     *
     * returns http status 200 when ok
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `status$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    status(params: { mode: 'SEARCH' | 'SERVICE'; timeoutSeconds?: number }): Observable<string> {
        return this.status$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }
}
