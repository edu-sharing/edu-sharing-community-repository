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

@Injectable({
    providedIn: 'root',
})
export class TrackingV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation trackEvent
     */
    static readonly TrackEventPath = '/tracking/v1/tracking/{repository}/{event}';

    /**
     * Track a user interaction.
     *
     * Currently limited to video / audio play interactions
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `trackEvent()` instead.
     *
     * This method doesn't expect any request body.
     */
    trackEvent$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * type of event to track
         */
        event:
            | 'DOWNLOAD_MATERIAL'
            | 'VIEW_MATERIAL'
            | 'VIEW_MATERIAL_EMBEDDED'
            | 'VIEW_MATERIAL_PLAY_MEDIA'
            | 'LOGIN_USER_SESSION'
            | 'LOGIN_USER_OAUTH_PASSWORD'
            | 'LOGIN_USER_OAUTH_REFRESH_TOKEN'
            | 'LOGOUT_USER_TIMEOUT'
            | 'LOGOUT_USER_REGULAR';

        /**
         * node id for which the event is tracked. For some event, this can be null
         */
        node?: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, TrackingV1Service.TrackEventPath, 'put');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('event', params.event, {});
            rb.query('node', params.node, {});
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
     * Track a user interaction.
     *
     * Currently limited to video / audio play interactions
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `trackEvent$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    trackEvent(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * type of event to track
         */
        event:
            | 'DOWNLOAD_MATERIAL'
            | 'VIEW_MATERIAL'
            | 'VIEW_MATERIAL_EMBEDDED'
            | 'VIEW_MATERIAL_PLAY_MEDIA'
            | 'LOGIN_USER_SESSION'
            | 'LOGIN_USER_OAUTH_PASSWORD'
            | 'LOGIN_USER_OAUTH_REFRESH_TOKEN'
            | 'LOGOUT_USER_TIMEOUT'
            | 'LOGOUT_USER_REGULAR';

        /**
         * node id for which the event is tracked. For some event, this can be null
         */
        node?: string;
    }): Observable<any> {
        return this.trackEvent$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }
}
