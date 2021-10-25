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
export class RatingV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation addOrUpdateRating
     */
    static readonly AddOrUpdateRatingPath = '/rating/v1/ratings/{repository}/{node}';

    /**
     * create or update a rating.
     *
     * Adds the rating. If the current user already rated that element, the rating will be altered
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `addOrUpdateRating()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    addOrUpdateRating$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * The rating (usually in range 1-5)
         */
        rating: number;

        /**
         * Text content of rating
         */
        body: string;
    }): Observable<StrictHttpResponse<void>> {
        const rb = new RequestBuilder(this.rootUrl, RatingV1Service.AddOrUpdateRatingPath, 'put');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
            rb.path('node', params.node, { style: 'simple', explode: false });
            rb.query('rating', params.rating, { style: 'form', explode: true });
            rb.body(params.body, 'application/json');
        }

        return this.http
            .request(
                rb.build({
                    responseType: 'text',
                    accept: '*/*',
                }),
            )
            .pipe(
                filter((r: any) => r instanceof HttpResponse),
                map((r: HttpResponse<any>) => {
                    return (r as HttpResponse<any>).clone({
                        body: undefined,
                    }) as StrictHttpResponse<void>;
                }),
            );
    }

    /**
     * create or update a rating.
     *
     * Adds the rating. If the current user already rated that element, the rating will be altered
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `addOrUpdateRating$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    addOrUpdateRating(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * The rating (usually in range 1-5)
         */
        rating: number;

        /**
         * Text content of rating
         */
        body: string;
    }): Observable<void> {
        return this.addOrUpdateRating$Response(params).pipe(
            map((r: StrictHttpResponse<void>) => r.body as void),
        );
    }

    /**
     * Path part for operation deleteRating
     */
    static readonly DeleteRatingPath = '/rating/v1/ratings/{repository}/{node}';

    /**
     * delete a comment.
     *
     * Delete the comment with the given id
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `deleteRating()` instead.
     *
     * This method doesn't expect any request body.
     */
    deleteRating$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<StrictHttpResponse<void>> {
        const rb = new RequestBuilder(this.rootUrl, RatingV1Service.DeleteRatingPath, 'delete');
        if (params) {
            rb.path('repository', params.repository, { style: 'simple', explode: false });
            rb.path('node', params.node, { style: 'simple', explode: false });
        }

        return this.http
            .request(
                rb.build({
                    responseType: 'text',
                    accept: '*/*',
                }),
            )
            .pipe(
                filter((r: any) => r instanceof HttpResponse),
                map((r: HttpResponse<any>) => {
                    return (r as HttpResponse<any>).clone({
                        body: undefined,
                    }) as StrictHttpResponse<void>;
                }),
            );
    }

    /**
     * delete a comment.
     *
     * Delete the comment with the given id
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `deleteRating$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    deleteRating(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<void> {
        return this.deleteRating$Response(params).pipe(
            map((r: StrictHttpResponse<void>) => r.body as void),
        );
    }
}
