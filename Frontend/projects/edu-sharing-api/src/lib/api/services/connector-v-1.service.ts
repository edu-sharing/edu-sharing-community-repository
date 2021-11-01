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

import { ConnectorList } from '../models/connector-list';

@Injectable({
    providedIn: 'root',
})
export class ConnectorV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation listConnectors
     */
    static readonly ListConnectorsPath = '/connector/v1/connectors/{repository}/list';

    /**
     * List all available connectors.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `listConnectors()` instead.
     *
     * This method doesn't expect any request body.
     */
    listConnectors$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;
    }): Observable<StrictHttpResponse<ConnectorList>> {
        const rb = new RequestBuilder(this.rootUrl, ConnectorV1Service.ListConnectorsPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
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
                    return r as StrictHttpResponse<ConnectorList>;
                }),
            );
    }

    /**
     * List all available connectors.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `listConnectors$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    listConnectors(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;
    }): Observable<ConnectorList> {
        return this.listConnectors$Response(params).pipe(
            map((r: StrictHttpResponse<ConnectorList>) => r.body as ConnectorList),
        );
    }
}
