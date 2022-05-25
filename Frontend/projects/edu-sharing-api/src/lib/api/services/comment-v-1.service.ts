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

import { Comments } from '../models/comments';

@Injectable({
    providedIn: 'root',
})
export class CommentV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation getComments
     */
    static readonly GetCommentsPath = '/comment/v1/comments/{repository}/{node}';

    /**
     * list comments.
     *
     * List all comments
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getComments()` instead.
     *
     * This method doesn't expect any request body.
     */
    getComments$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<StrictHttpResponse<Comments>> {
        const rb = new RequestBuilder(this.rootUrl, CommentV1Service.GetCommentsPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
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
                    return r as StrictHttpResponse<Comments>;
                }),
            );
    }

    /**
     * list comments.
     *
     * List all comments
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getComments$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getComments(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<Comments> {
        return this.getComments$Response(params).pipe(
            map((r: StrictHttpResponse<Comments>) => r.body as Comments),
        );
    }

    /**
     * Path part for operation addComment
     */
    static readonly AddCommentPath = '/comment/v1/comments/{repository}/{node}';

    /**
     * create a new comment.
     *
     * Adds a comment to the given node
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `addComment()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    addComment$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * In reply to an other comment, can be null
         */
        commentReference?: string;

        /**
         * Text content of comment
         */
        body: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, CommentV1Service.AddCommentPath, 'put');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('commentReference', params.commentReference, {});
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
     * create a new comment.
     *
     * Adds a comment to the given node
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `addComment$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    addComment(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * In reply to an other comment, can be null
         */
        commentReference?: string;

        /**
         * Text content of comment
         */
        body: string;
    }): Observable<any> {
        return this.addComment$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation editComment
     */
    static readonly EditCommentPath = '/comment/v1/comments/{repository}/{comment}';

    /**
     * edit a comment.
     *
     * Edit the comment with the given id
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `editComment()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    editComment$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * id of the comment to edit
         */
        comment: string;

        /**
         * Text content of comment
         */
        body: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, CommentV1Service.EditCommentPath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('comment', params.comment, {});
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
     * edit a comment.
     *
     * Edit the comment with the given id
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `editComment$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    editComment(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * id of the comment to edit
         */
        comment: string;

        /**
         * Text content of comment
         */
        body: string;
    }): Observable<any> {
        return this.editComment$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation deleteComment
     */
    static readonly DeleteCommentPath = '/comment/v1/comments/{repository}/{comment}';

    /**
     * delete a comment.
     *
     * Delete the comment with the given id
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `deleteComment()` instead.
     *
     * This method doesn't expect any request body.
     */
    deleteComment$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * id of the comment to delete
         */
        comment: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, CommentV1Service.DeleteCommentPath, 'delete');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('comment', params.comment, {});
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
     * delete a comment.
     *
     * Delete the comment with the given id
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `deleteComment$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    deleteComment(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * id of the comment to delete
         */
        comment: string;
    }): Observable<any> {
        return this.deleteComment$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }
}
