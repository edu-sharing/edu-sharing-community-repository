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

import { WebsiteInformation } from '../models/website-information';

@Injectable({
    providedIn: 'root',
})
export class ClientutilsV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation getWebsiteInformation
     */
    static readonly GetWebsiteInformationPath = '/clientUtils/v1/getWebsiteInformation';

    /**
     * Read generic information about a webpage.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getWebsiteInformation()` instead.
     *
     * This method doesn't expect any request body.
     */
    getWebsiteInformation$Response(params?: {
        /**
         * full url with http or https
         */
        url?: string;
    }): Observable<StrictHttpResponse<WebsiteInformation>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            ClientutilsV1Service.GetWebsiteInformationPath,
            'get',
        );
        if (params) {
            rb.query('url', params.url, {});
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
                    return r as StrictHttpResponse<WebsiteInformation>;
                }),
            );
    }

    /**
     * Read generic information about a webpage.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getWebsiteInformation$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getWebsiteInformation(params?: {
        /**
         * full url with http or https
         */
        url?: string;
    }): Observable<WebsiteInformation> {
        return this.getWebsiteInformation$Response(params).pipe(
            map((r: StrictHttpResponse<WebsiteInformation>) => r.body as WebsiteInformation),
        );
    }
}
