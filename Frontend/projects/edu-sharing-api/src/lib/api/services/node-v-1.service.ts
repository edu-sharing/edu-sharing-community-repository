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

import { Acl } from '../models/acl';
import { NodeEntries } from '../models/node-entries';
import { NodeEntry } from '../models/node-entry';
import { NodeLocked } from '../models/node-locked';
import { NodePermissionEntry } from '../models/node-permission-entry';
import { NodeRemote } from '../models/node-remote';
import { NodeShare } from '../models/node-share';
import { NodeText } from '../models/node-text';
import { NodeVersionEntry } from '../models/node-version-entry';
import { NodeVersionRefEntries } from '../models/node-version-ref-entries';
import { ParentEntries } from '../models/parent-entries';
import { SearchResult } from '../models/search-result';
import { WorkflowHistory } from '../models/workflow-history';

@Injectable({
    providedIn: 'root',
})
export class NodeV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation addAspects
     */
    static readonly AddAspectsPath = '/node/v1/nodes/{repository}/{node}/aspects';

    /**
     * Add aspect to node.
     *
     * Add aspect to node.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `addAspects()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    addAspects$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * aspect name, e.g. ccm:lomreplication
         */
        body: Array<string>;
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.AddAspectsPath, 'put');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
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
                    return r as StrictHttpResponse<NodeEntry>;
                }),
            );
    }

    /**
     * Add aspect to node.
     *
     * Add aspect to node.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `addAspects$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    addAspects(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * aspect name, e.g. ccm:lomreplication
         */
        body: Array<string>;
    }): Observable<NodeEntry> {
        return this.addAspects$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation getWorkflowHistory
     */
    static readonly GetWorkflowHistoryPath = '/node/v1/nodes/{repository}/{node}/workflow';

    /**
     * Get workflow history.
     *
     * Get workflow history of node.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getWorkflowHistory()` instead.
     *
     * This method doesn't expect any request body.
     */
    getWorkflowHistory$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.GetWorkflowHistoryPath, 'get');
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
                    return r as StrictHttpResponse<string>;
                }),
            );
    }

    /**
     * Get workflow history.
     *
     * Get workflow history of node.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getWorkflowHistory$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getWorkflowHistory(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<string> {
        return this.getWorkflowHistory$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation addWorkflowHistory
     */
    static readonly AddWorkflowHistoryPath = '/node/v1/nodes/{repository}/{node}/workflow';

    /**
     * Add workflow.
     *
     * Add workflow entry to node.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `addWorkflowHistory()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    addWorkflowHistory$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * The history entry to put (editor and time can be null and will be filled automatically)
         */
        body: WorkflowHistory;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.AddWorkflowHistoryPath, 'put');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
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
     * Add workflow.
     *
     * Add workflow entry to node.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `addWorkflowHistory$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    addWorkflowHistory(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * The history entry to put (editor and time can be null and will be filled automatically)
         */
        body: WorkflowHistory;
    }): Observable<any> {
        return this.addWorkflowHistory$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation changeContent
     */
    static readonly ChangeContentPath = '/node/v1/nodes/{repository}/{node}/content';

    /**
     * Change content of node.
     *
     * Change content of node.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `changeContent()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    changeContent$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * comment, leave empty &#x3D; no new version, otherwise new version is generated
         */
        versionComment?: string;

        /**
         * MIME-Type
         */
        mimetype: string;
        body?: {
            file?: {};
        };
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.ChangeContentPath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('versionComment', params.versionComment, {});
            rb.query('mimetype', params.mimetype, {});
            rb.body(params.body, 'multipart/form-data');
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
                    return r as StrictHttpResponse<NodeEntry>;
                }),
            );
    }

    /**
     * Change content of node.
     *
     * Change content of node.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `changeContent$Response()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    changeContent(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * comment, leave empty &#x3D; no new version, otherwise new version is generated
         */
        versionComment?: string;

        /**
         * MIME-Type
         */
        mimetype: string;
        body?: {
            file?: {};
        };
    }): Observable<NodeEntry> {
        return this.changeContent$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation getTextContent
     */
    static readonly GetTextContentPath = '/node/v1/nodes/{repository}/{node}/textContent';

    /**
     * Get the text content of a document.
     *
     * May fails with 500 if the node can not be read.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getTextContent()` instead.
     *
     * This method doesn't expect any request body.
     */
    getTextContent$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<StrictHttpResponse<NodeText>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.GetTextContentPath, 'get');
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
                    return r as StrictHttpResponse<NodeText>;
                }),
            );
    }

    /**
     * Get the text content of a document.
     *
     * May fails with 500 if the node can not be read.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getTextContent$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getTextContent(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<NodeText> {
        return this.getTextContent$Response(params).pipe(
            map((r: StrictHttpResponse<NodeText>) => r.body as NodeText),
        );
    }

    /**
     * Path part for operation changeContentAsText
     */
    static readonly ChangeContentAsTextPath = '/node/v1/nodes/{repository}/{node}/textContent';

    /**
     * Change content of node as text.
     *
     * Change content of node as text.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `changeContentAsText()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    changeContentAsText$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * comment, leave empty &#x3D; no new version, otherwise new version is generated
         */
        versionComment?: string;

        /**
         * MIME-Type
         */
        mimetype: string;

        /**
         * The content data to write (text)
         */
        body: string;
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.ChangeContentAsTextPath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('versionComment', params.versionComment, {});
            rb.query('mimetype', params.mimetype, {});
            rb.body(params.body, 'multipart/form-data');
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
                    return r as StrictHttpResponse<NodeEntry>;
                }),
            );
    }

    /**
     * Change content of node as text.
     *
     * Change content of node as text.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `changeContentAsText$Response()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    changeContentAsText(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * comment, leave empty &#x3D; no new version, otherwise new version is generated
         */
        versionComment?: string;

        /**
         * MIME-Type
         */
        mimetype: string;

        /**
         * The content data to write (text)
         */
        body: string;
    }): Observable<NodeEntry> {
        return this.changeContentAsText$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation getMetadata
     */
    static readonly GetMetadataPath = '/node/v1/nodes/{repository}/{node}/metadata';

    /**
     * Get metadata of node.
     *
     * Get metadata of node.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getMetadata()` instead.
     *
     * This method doesn't expect any request body.
     */
    getMetadata$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.GetMetadataPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('propertyFilter', params.propertyFilter, {});
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
                    return r as StrictHttpResponse<NodeEntry>;
                }),
            );
    }

    /**
     * Get metadata of node.
     *
     * Get metadata of node.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getMetadata$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getMetadata(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;
    }): Observable<NodeEntry> {
        return this.getMetadata$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation changeMetadata
     */
    static readonly ChangeMetadataPath = '/node/v1/nodes/{repository}/{node}/metadata';

    /**
     * Change metadata of node.
     *
     * Change metadata of node.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `changeMetadata()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    changeMetadata$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * properties
         */
        body: {
            [key: string]: Array<string>;
        };
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.ChangeMetadataPath, 'put');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
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
                    return r as StrictHttpResponse<NodeEntry>;
                }),
            );
    }

    /**
     * Change metadata of node.
     *
     * Change metadata of node.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `changeMetadata$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    changeMetadata(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * properties
         */
        body: {
            [key: string]: Array<string>;
        };
    }): Observable<NodeEntry> {
        return this.changeMetadata$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation changeMetadataWithVersioning
     */
    static readonly ChangeMetadataWithVersioningPath =
        '/node/v1/nodes/{repository}/{node}/metadata';

    /**
     * Change metadata of node (new version).
     *
     * Change metadata of node (new version).
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `changeMetadataWithVersioning()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    changeMetadataWithVersioning$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * comment
         */
        versionComment: string;

        /**
         * properties
         */
        body: {
            [key: string]: Array<string>;
        };
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            NodeV1Service.ChangeMetadataWithVersioningPath,
            'post',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('versionComment', params.versionComment, {});
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
                    return r as StrictHttpResponse<NodeEntry>;
                }),
            );
    }

    /**
     * Change metadata of node (new version).
     *
     * Change metadata of node (new version).
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `changeMetadataWithVersioning$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    changeMetadataWithVersioning(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * comment
         */
        versionComment: string;

        /**
         * properties
         */
        body: {
            [key: string]: Array<string>;
        };
    }): Observable<NodeEntry> {
        return this.changeMetadataWithVersioning$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation changePreview
     */
    static readonly ChangePreviewPath = '/node/v1/nodes/{repository}/{node}/preview';

    /**
     * Change preview of node.
     *
     * Change preview of node.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `changePreview()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    changePreview$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * MIME-Type
         */
        mimetype: string;
        body?: {
            image?: {};
        };
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.ChangePreviewPath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('mimetype', params.mimetype, {});
            rb.body(params.body, 'multipart/form-data');
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
                    return r as StrictHttpResponse<NodeEntry>;
                }),
            );
    }

    /**
     * Change preview of node.
     *
     * Change preview of node.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `changePreview$Response()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    changePreview(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * MIME-Type
         */
        mimetype: string;
        body?: {
            image?: {};
        };
    }): Observable<NodeEntry> {
        return this.changePreview$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation deletePreview
     */
    static readonly DeletePreviewPath = '/node/v1/nodes/{repository}/{node}/preview';

    /**
     * Delete preview of node.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `deletePreview()` instead.
     *
     * This method doesn't expect any request body.
     */
    deletePreview$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.DeletePreviewPath, 'delete');
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
                    return r as StrictHttpResponse<NodeEntry>;
                }),
            );
    }

    /**
     * Delete preview of node.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `deletePreview$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    deletePreview(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<NodeEntry> {
        return this.deletePreview$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation getTemplateMetadata
     */
    static readonly GetTemplateMetadataPath =
        '/node/v1/nodes/{repository}/{node}/metadata/template';

    /**
     * Get the metadata template + status for this folder.
     *
     * All the given metadata will be inherited to child nodes.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getTemplateMetadata()` instead.
     *
     * This method doesn't expect any request body.
     */
    getTemplateMetadata$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.GetTemplateMetadataPath, 'get');
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
                    return r as StrictHttpResponse<NodeEntry>;
                }),
            );
    }

    /**
     * Get the metadata template + status for this folder.
     *
     * All the given metadata will be inherited to child nodes.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getTemplateMetadata$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getTemplateMetadata(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<NodeEntry> {
        return this.getTemplateMetadata$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation changeTemplateMetadata
     */
    static readonly ChangeTemplateMetadataPath =
        '/node/v1/nodes/{repository}/{node}/metadata/template';

    /**
     * Set the metadata template for this folder.
     *
     * All the given metadata will be inherited to child nodes.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `changeTemplateMetadata()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    changeTemplateMetadata$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * Is the inherition currently enabled
         */
        enable: boolean;

        /**
         * properties
         */
        body: {
            [key: string]: Array<string>;
        };
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            NodeV1Service.ChangeTemplateMetadataPath,
            'put',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('enable', params.enable, {});
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
                    return r as StrictHttpResponse<NodeEntry>;
                }),
            );
    }

    /**
     * Set the metadata template for this folder.
     *
     * All the given metadata will be inherited to child nodes.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `changeTemplateMetadata$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    changeTemplateMetadata(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * Is the inherition currently enabled
         */
        enable: boolean;

        /**
         * properties
         */
        body: {
            [key: string]: Array<string>;
        };
    }): Observable<NodeEntry> {
        return this.changeTemplateMetadata$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation copyMetadata
     */
    static readonly CopyMetadataPath = '/node/v1/nodes/{repository}/{node}/metadata/copy/{from}';

    /**
     * Copy metadata from another node.
     *
     * Copies all common metadata from one note to another. Current user needs write access to the target node and read access to the source node.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `copyMetadata()` instead.
     *
     * This method doesn't expect any request body.
     */
    copyMetadata$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * The node where to copy the metadata from
         */
        from: string;
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.CopyMetadataPath, 'put');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.path('from', params.from, {});
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
                    return r as StrictHttpResponse<NodeEntry>;
                }),
            );
    }

    /**
     * Copy metadata from another node.
     *
     * Copies all common metadata from one note to another. Current user needs write access to the target node and read access to the source node.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `copyMetadata$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    copyMetadata(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * The node where to copy the metadata from
         */
        from: string;
    }): Observable<NodeEntry> {
        return this.copyMetadata$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation getChildren
     */
    static readonly GetChildrenPath = '/node/v1/nodes/{repository}/{node}/children';

    /**
     * Get children of node.
     *
     * Get children of node.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getChildren()` instead.
     *
     * This method doesn't expect any request body.
     */
    getChildren$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of parent node (or &quot;-userhome-&quot; for home directory of current user, &quot;-shared_files-&quot; for shared folders, &quot;-to_me_shared_files&quot; for shared files for the user,&quot;-my_shared_files-&quot; for files shared by the user, &quot;-inbox-&quot; for the inbox, &quot;-workflow_receive-&quot; for files assigned by workflow, &quot;-saved_search-&quot; for saved searches of the user)
         */
        node: string;

        /**
         * maximum items per page
         */
        maxItems?: number;

        /**
         * skip a number of items
         */
        skipCount?: number;

        /**
         * filter by type files,folders
         */
        filter?: Array<string>;

        /**
         * sort properties
         */
        sortProperties?: Array<string>;

        /**
         * sort ascending, true if not set. Use multiple values to change the direction according to the given property at the same index
         */
        sortAscending?: Array<boolean>;

        /**
         * Filter for a specific association. May be empty
         */
        assocName?: string;

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;
    }): Observable<StrictHttpResponse<NodeEntries>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.GetChildrenPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('maxItems', params.maxItems, {});
            rb.query('skipCount', params.skipCount, {});
            rb.query('filter', params.filter, {});
            rb.query('sortProperties', params.sortProperties, {});
            rb.query('sortAscending', params.sortAscending, {});
            rb.query('assocName', params.assocName, {});
            rb.query('propertyFilter', params.propertyFilter, {});
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
                    return r as StrictHttpResponse<NodeEntries>;
                }),
            );
    }

    /**
     * Get children of node.
     *
     * Get children of node.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getChildren$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getChildren(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of parent node (or &quot;-userhome-&quot; for home directory of current user, &quot;-shared_files-&quot; for shared folders, &quot;-to_me_shared_files&quot; for shared files for the user,&quot;-my_shared_files-&quot; for files shared by the user, &quot;-inbox-&quot; for the inbox, &quot;-workflow_receive-&quot; for files assigned by workflow, &quot;-saved_search-&quot; for saved searches of the user)
         */
        node: string;

        /**
         * maximum items per page
         */
        maxItems?: number;

        /**
         * skip a number of items
         */
        skipCount?: number;

        /**
         * filter by type files,folders
         */
        filter?: Array<string>;

        /**
         * sort properties
         */
        sortProperties?: Array<string>;

        /**
         * sort ascending, true if not set. Use multiple values to change the direction according to the given property at the same index
         */
        sortAscending?: Array<boolean>;

        /**
         * Filter for a specific association. May be empty
         */
        assocName?: string;

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;
    }): Observable<NodeEntries> {
        return this.getChildren$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntries>) => r.body as NodeEntries),
        );
    }

    /**
     * Path part for operation createChild
     */
    static readonly CreateChildPath = '/node/v1/nodes/{repository}/{node}/children';

    /**
     * Create a new child.
     *
     * Create a new child.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `createChild()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    createChild$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of parent node use -userhome- for userhome or -inbox- for inbox node
         */
        node: string;

        /**
         * type of node
         */
        type: string;

        /**
         * aspects of node
         */
        aspects?: Array<string>;

        /**
         * rename if the same node name exists
         */
        renameIfExists?: boolean;

        /**
         * comment, leave empty &#x3D; no inital version
         */
        versionComment?: string;

        /**
         * Association type, can be empty
         */
        assocType?: string;

        /**
         * properties, example: {"{http://www.alfresco.org/model/content/1.0}name": ["test"]}
         */
        body: {
            [key: string]: Array<string>;
        };
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.CreateChildPath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('type', params.type, {});
            rb.query('aspects', params.aspects, {});
            rb.query('renameIfExists', params.renameIfExists, {});
            rb.query('versionComment', params.versionComment, {});
            rb.query('assocType', params.assocType, {});
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
                    return r as StrictHttpResponse<NodeEntry>;
                }),
            );
    }

    /**
     * Create a new child.
     *
     * Create a new child.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `createChild$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    createChild(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of parent node use -userhome- for userhome or -inbox- for inbox node
         */
        node: string;

        /**
         * type of node
         */
        type: string;

        /**
         * aspects of node
         */
        aspects?: Array<string>;

        /**
         * rename if the same node name exists
         */
        renameIfExists?: boolean;

        /**
         * comment, leave empty &#x3D; no inital version
         */
        versionComment?: string;

        /**
         * Association type, can be empty
         */
        assocType?: string;

        /**
         * properties, example: {"{http://www.alfresco.org/model/content/1.0}name": ["test"]}
         */
        body: {
            [key: string]: Array<string>;
        };
    }): Observable<NodeEntry> {
        return this.createChild$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation createChildByCopying
     */
    static readonly CreateChildByCopyingPath = '/node/v1/nodes/{repository}/{node}/children/_copy';

    /**
     * Create a new child by copying.
     *
     * Create a new child by copying.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `createChildByCopying()` instead.
     *
     * This method doesn't expect any request body.
     */
    createChildByCopying$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of parent node
         */
        node: string;

        /**
         * ID of source node
         */
        source: string;

        /**
         * flag for children
         */
        withChildren: boolean;
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.CreateChildByCopyingPath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('source', params.source, {});
            rb.query('withChildren', params.withChildren, {});
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
                    return r as StrictHttpResponse<NodeEntry>;
                }),
            );
    }

    /**
     * Create a new child by copying.
     *
     * Create a new child by copying.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `createChildByCopying$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    createChildByCopying(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of parent node
         */
        node: string;

        /**
         * ID of source node
         */
        source: string;

        /**
         * flag for children
         */
        withChildren: boolean;
    }): Observable<NodeEntry> {
        return this.createChildByCopying$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation createChildByMoving
     */
    static readonly CreateChildByMovingPath = '/node/v1/nodes/{repository}/{node}/children/_move';

    /**
     * Create a new child by moving.
     *
     * Create a new child by moving.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `createChildByMoving()` instead.
     *
     * This method doesn't expect any request body.
     */
    createChildByMoving$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of parent node
         */
        node: string;

        /**
         * ID of source node
         */
        source: string;
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.CreateChildByMovingPath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('source', params.source, {});
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
                    return r as StrictHttpResponse<NodeEntry>;
                }),
            );
    }

    /**
     * Create a new child by moving.
     *
     * Create a new child by moving.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `createChildByMoving$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    createChildByMoving(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of parent node
         */
        node: string;

        /**
         * ID of source node
         */
        source: string;
    }): Observable<NodeEntry> {
        return this.createChildByMoving$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation createForkOfNode
     */
    static readonly CreateForkOfNodePath = '/node/v1/nodes/{repository}/{node}/children/_fork';

    /**
     * Create a copy of a node by creating a forked version (variant).
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `createForkOfNode()` instead.
     *
     * This method doesn't expect any request body.
     */
    createForkOfNode$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of parent node
         */
        node: string;

        /**
         * ID of source node
         */
        source: string;

        /**
         * flag for children
         */
        withChildren: boolean;
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.CreateForkOfNodePath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('source', params.source, {});
            rb.query('withChildren', params.withChildren, {});
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
                    return r as StrictHttpResponse<NodeEntry>;
                }),
            );
    }

    /**
     * Create a copy of a node by creating a forked version (variant).
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `createForkOfNode$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    createForkOfNode(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of parent node
         */
        node: string;

        /**
         * ID of source node
         */
        source: string;

        /**
         * flag for children
         */
        withChildren: boolean;
    }): Observable<NodeEntry> {
        return this.createForkOfNode$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation getShares
     */
    static readonly GetSharesPath = '/node/v1/nodes/{repository}/{node}/shares';

    /**
     * Get shares of node.
     *
     * Get list of shares (via mail/token) for a node.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getShares()` instead.
     *
     * This method doesn't expect any request body.
     */
    getShares$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * Filter for a specific email or use LINK for link shares (Optional)
         */
        email?: string;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.GetSharesPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('email', params.email, {});
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
     * Get shares of node.
     *
     * Get list of shares (via mail/token) for a node.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getShares$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getShares(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * Filter for a specific email or use LINK for link shares (Optional)
         */
        email?: string;
    }): Observable<string> {
        return this.getShares$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation createShare
     */
    static readonly CreateSharePath = '/node/v1/nodes/{repository}/{node}/shares';

    /**
     * Create a share for a node.
     *
     * Create a new share for a node
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `createShare()` instead.
     *
     * This method doesn't expect any request body.
     */
    createShare$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * expiry date for this share, leave empty or -1 for unlimited
         */
        expiryDate?: number;

        /**
         * password for this share, use none to not use a password
         */
        password?: string;
    }): Observable<StrictHttpResponse<NodeShare>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.CreateSharePath, 'put');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('expiryDate', params.expiryDate, {});
            rb.query('password', params.password, {});
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
                    return r as StrictHttpResponse<NodeShare>;
                }),
            );
    }

    /**
     * Create a share for a node.
     *
     * Create a new share for a node
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `createShare$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    createShare(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * expiry date for this share, leave empty or -1 for unlimited
         */
        expiryDate?: number;

        /**
         * password for this share, use none to not use a password
         */
        password?: string;
    }): Observable<NodeShare> {
        return this.createShare$Response(params).pipe(
            map((r: StrictHttpResponse<NodeShare>) => r.body as NodeShare),
        );
    }

    /**
     * Path part for operation delete
     */
    static readonly DeletePath = '/node/v1/nodes/{repository}/{node}';

    /**
     * Delete node.
     *
     * Delete node.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `delete()` instead.
     *
     * This method doesn't expect any request body.
     */
    delete$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * move the node to recycle
         */
        recycle?: boolean;

        /**
         * protocol
         */
        protocol?: string;

        /**
         * store
         */
        store?: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.DeletePath, 'delete');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('recycle', params.recycle, {});
            rb.query('protocol', params.protocol, {});
            rb.query('store', params.store, {});
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
     * Delete node.
     *
     * Delete node.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `delete$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    delete(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * move the node to recycle
         */
        recycle?: boolean;

        /**
         * protocol
         */
        protocol?: string;

        /**
         * store
         */
        store?: string;
    }): Observable<any> {
        return this.delete$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation getAssocs
     */
    static readonly GetAssocsPath = '/node/v1/nodes/{repository}/{node}/assocs';

    /**
     * Get related nodes.
     *
     * Get nodes related based on an assoc.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getAssocs()` instead.
     *
     * This method doesn't expect any request body.
     */
    getAssocs$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * maximum items per page
         */
        maxItems?: number;

        /**
         * skip a number of items
         */
        skipCount?: number;

        /**
         * sort properties
         */
        sortProperties?: Array<string>;

        /**
         * sort ascending, true if not set. Use multiple values to change the direction according to the given property at the same index
         */
        sortAscending?: Array<boolean>;

        /**
         * Either where the given node should be the &quot;SOURCE&quot; or the &quot;TARGET&quot;
         */
        direction: 'SOURCE' | 'TARGET';

        /**
         * Association name (e.g. ccm:forkio).
         */
        assocName?: string;

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;
    }): Observable<StrictHttpResponse<NodeEntries>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.GetAssocsPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('maxItems', params.maxItems, {});
            rb.query('skipCount', params.skipCount, {});
            rb.query('sortProperties', params.sortProperties, {});
            rb.query('sortAscending', params.sortAscending, {});
            rb.query('direction', params.direction, {});
            rb.query('assocName', params.assocName, {});
            rb.query('propertyFilter', params.propertyFilter, {});
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
                    return r as StrictHttpResponse<NodeEntries>;
                }),
            );
    }

    /**
     * Get related nodes.
     *
     * Get nodes related based on an assoc.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getAssocs$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getAssocs(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * maximum items per page
         */
        maxItems?: number;

        /**
         * skip a number of items
         */
        skipCount?: number;

        /**
         * sort properties
         */
        sortProperties?: Array<string>;

        /**
         * sort ascending, true if not set. Use multiple values to change the direction according to the given property at the same index
         */
        sortAscending?: Array<boolean>;

        /**
         * Either where the given node should be the &quot;SOURCE&quot; or the &quot;TARGET&quot;
         */
        direction: 'SOURCE' | 'TARGET';

        /**
         * Association name (e.g. ccm:forkio).
         */
        assocName?: string;

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;
    }): Observable<NodeEntries> {
        return this.getAssocs$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntries>) => r.body as NodeEntries),
        );
    }

    /**
     * Path part for operation getNodes
     */
    static readonly GetNodesPath = '/node/v1/nodes/{repository}';

    /**
     * Searching nodes.
     *
     * Searching nodes.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getNodes()` instead.
     *
     * This method doesn't expect any request body.
     */
    getNodes$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * lucene query
         */
        query: string;

        /**
         * facets
         */
        facets?: Array<string>;

        /**
         * maximum items per page
         */
        maxItems?: number;

        /**
         * skip a number of items
         */
        skipCount?: number;

        /**
         * sort properties
         */
        sortProperties?: Array<string>;

        /**
         * sort ascending, true if not set. Use multiple values to change the direction according to the given property at the same index
         */
        sortAscending?: Array<boolean>;

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;
    }): Observable<StrictHttpResponse<SearchResult>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.GetNodesPath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.query('query', params.query, {});
            rb.query('facets', params.facets, {});
            rb.query('maxItems', params.maxItems, {});
            rb.query('skipCount', params.skipCount, {});
            rb.query('sortProperties', params.sortProperties, {});
            rb.query('sortAscending', params.sortAscending, {});
            rb.query('propertyFilter', params.propertyFilter, {});
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
                    return r as StrictHttpResponse<SearchResult>;
                }),
            );
    }

    /**
     * Searching nodes.
     *
     * Searching nodes.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getNodes$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getNodes(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * lucene query
         */
        query: string;

        /**
         * facets
         */
        facets?: Array<string>;

        /**
         * maximum items per page
         */
        maxItems?: number;

        /**
         * skip a number of items
         */
        skipCount?: number;

        /**
         * sort properties
         */
        sortProperties?: Array<string>;

        /**
         * sort ascending, true if not set. Use multiple values to change the direction according to the given property at the same index
         */
        sortAscending?: Array<boolean>;

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;
    }): Observable<SearchResult> {
        return this.getNodes$Response(params).pipe(
            map((r: StrictHttpResponse<SearchResult>) => r.body as SearchResult),
        );
    }

    /**
     * Path part for operation getNotifyList
     */
    static readonly GetNotifyListPath = '/node/v1/nodes/{repository}/{node}/notifys';

    /**
     * Get notifys (sharing history) of the node.
     *
     * Ordered by the time of each notify
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getNotifyList()` instead.
     *
     * This method doesn't expect any request body.
     */
    getNotifyList$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.GetNotifyListPath, 'get');
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
                    return r as StrictHttpResponse<string>;
                }),
            );
    }

    /**
     * Get notifys (sharing history) of the node.
     *
     * Ordered by the time of each notify
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getNotifyList$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getNotifyList(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<string> {
        return this.getNotifyList$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation getParents
     */
    static readonly GetParentsPath = '/node/v1/nodes/{repository}/{node}/parents';

    /**
     * Get parents of node.
     *
     * Get all parents metadata + own metadata of node. Index 0 is always the current node
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getParents()` instead.
     *
     * This method doesn't expect any request body.
     */
    getParents$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;

        /**
         * activate to return the full alfresco path, otherwise the path for the user home is resolved
         */
        fullPath?: boolean;
    }): Observable<StrictHttpResponse<ParentEntries>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.GetParentsPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('propertyFilter', params.propertyFilter, {});
            rb.query('fullPath', params.fullPath, {});
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
                    return r as StrictHttpResponse<ParentEntries>;
                }),
            );
    }

    /**
     * Get parents of node.
     *
     * Get all parents metadata + own metadata of node. Index 0 is always the current node
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getParents$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getParents(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;

        /**
         * activate to return the full alfresco path, otherwise the path for the user home is resolved
         */
        fullPath?: boolean;
    }): Observable<ParentEntries> {
        return this.getParents$Response(params).pipe(
            map((r: StrictHttpResponse<ParentEntries>) => r.body as ParentEntries),
        );
    }

    /**
     * Path part for operation getPermission
     */
    static readonly GetPermissionPath = '/node/v1/nodes/{repository}/{node}/permissions';

    /**
     * Get all permission of node.
     *
     * Get all permission of node.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getPermission()` instead.
     *
     * This method doesn't expect any request body.
     */
    getPermission$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<StrictHttpResponse<NodePermissionEntry>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.GetPermissionPath, 'get');
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
                    return r as StrictHttpResponse<NodePermissionEntry>;
                }),
            );
    }

    /**
     * Get all permission of node.
     *
     * Get all permission of node.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getPermission$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getPermission(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<NodePermissionEntry> {
        return this.getPermission$Response(params).pipe(
            map((r: StrictHttpResponse<NodePermissionEntry>) => r.body as NodePermissionEntry),
        );
    }

    /**
     * Path part for operation setPermission
     */
    static readonly SetPermissionPath = '/node/v1/nodes/{repository}/{node}/permissions';

    /**
     * Set local permissions of node.
     *
     * Set local permissions of node.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `setPermission()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    setPermission$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * mailtext
         */
        mailtext?: string;

        /**
         * sendMail
         */
        sendMail: boolean;

        /**
         * sendCopy
         */
        sendCopy: boolean;

        /**
         * permissions
         */
        body: Acl;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.SetPermissionPath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('mailtext', params.mailtext, {});
            rb.query('sendMail', params.sendMail, {});
            rb.query('sendCopy', params.sendCopy, {});
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
     * Set local permissions of node.
     *
     * Set local permissions of node.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `setPermission$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    setPermission(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * mailtext
         */
        mailtext?: string;

        /**
         * sendMail
         */
        sendMail: boolean;

        /**
         * sendCopy
         */
        sendCopy: boolean;

        /**
         * permissions
         */
        body: Acl;
    }): Observable<any> {
        return this.setPermission$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation getPublishedCopies
     */
    static readonly GetPublishedCopiesPath = '/node/v1/nodes/{repository}/{node}/publish';

    /**
     * Publish.
     *
     * Get all published copies of the current node
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getPublishedCopies()` instead.
     *
     * This method doesn't expect any request body.
     */
    getPublishedCopies$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<StrictHttpResponse<NodeEntries>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.GetPublishedCopiesPath, 'get');
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
                    return r as StrictHttpResponse<NodeEntries>;
                }),
            );
    }

    /**
     * Publish.
     *
     * Get all published copies of the current node
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getPublishedCopies$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getPublishedCopies(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<NodeEntries> {
        return this.getPublishedCopies$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntries>) => r.body as NodeEntries),
        );
    }

    /**
     * Path part for operation publishCopy
     */
    static readonly PublishCopyPath = '/node/v1/nodes/{repository}/{node}/publish';

    /**
     * Publish.
     *
     * Create a published copy of the current node
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `publishCopy()` instead.
     *
     * This method doesn't expect any request body.
     */
    publishCopy$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * handle mode, if a handle should be created. Skip this parameter if you don&#x27;t want an handle
         */
        handleMode?: 'distinct' | 'update';
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.PublishCopyPath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('handleMode', params.handleMode, {});
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
                    return r as StrictHttpResponse<NodeEntry>;
                }),
            );
    }

    /**
     * Publish.
     *
     * Create a published copy of the current node
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `publishCopy$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    publishCopy(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * handle mode, if a handle should be created. Skip this parameter if you don&#x27;t want an handle
         */
        handleMode?: 'distinct' | 'update';
    }): Observable<NodeEntry> {
        return this.publishCopy$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation getVersionMetadata
     */
    static readonly GetVersionMetadataPath =
        '/node/v1/nodes/{repository}/{node}/versions/{major}/{minor}/metadata';

    /**
     * Get metadata of node version.
     *
     * Get metadata of node version.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getVersionMetadata()` instead.
     *
     * This method doesn't expect any request body.
     */
    getVersionMetadata$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * major version
         */
        major: number;

        /**
         * minor version
         */
        minor: number;

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;
    }): Observable<StrictHttpResponse<NodeVersionEntry>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.GetVersionMetadataPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.path('major', params.major, {});
            rb.path('minor', params.minor, {});
            rb.query('propertyFilter', params.propertyFilter, {});
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
                    return r as StrictHttpResponse<NodeVersionEntry>;
                }),
            );
    }

    /**
     * Get metadata of node version.
     *
     * Get metadata of node version.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getVersionMetadata$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getVersionMetadata(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * major version
         */
        major: number;

        /**
         * minor version
         */
        minor: number;

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;
    }): Observable<NodeVersionEntry> {
        return this.getVersionMetadata$Response(params).pipe(
            map((r: StrictHttpResponse<NodeVersionEntry>) => r.body as NodeVersionEntry),
        );
    }

    /**
     * Path part for operation getVersions
     */
    static readonly GetVersionsPath = '/node/v1/nodes/{repository}/{node}/versions';

    /**
     * Get all versions of node.
     *
     * Get all versions of node.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getVersions()` instead.
     *
     * This method doesn't expect any request body.
     */
    getVersions$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<StrictHttpResponse<NodeVersionRefEntries>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.GetVersionsPath, 'get');
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
                    return r as StrictHttpResponse<NodeVersionRefEntries>;
                }),
            );
    }

    /**
     * Get all versions of node.
     *
     * Get all versions of node.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getVersions$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getVersions(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<NodeVersionRefEntries> {
        return this.getVersions$Response(params).pipe(
            map((r: StrictHttpResponse<NodeVersionRefEntries>) => r.body as NodeVersionRefEntries),
        );
    }

    /**
     * Path part for operation hasPermission
     */
    static readonly HasPermissionPath = '/node/v1/nodes/{repository}/{node}/permissions/{user}';

    /**
     * Which permissions has user/group for node.
     *
     * Check for actual permissions (also when user is in groups) for a specific node
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `hasPermission()` instead.
     *
     * This method doesn't expect any request body.
     */
    hasPermission$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * Authority (user/group) to check (use &quot;-me-&quot; for current user
         */
        user: string;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.HasPermissionPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.path('user', params.user, {});
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
     * Which permissions has user/group for node.
     *
     * Check for actual permissions (also when user is in groups) for a specific node
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `hasPermission$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    hasPermission(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * Authority (user/group) to check (use &quot;-me-&quot; for current user
         */
        user: string;
    }): Observable<string> {
        return this.hasPermission$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation importNode
     */
    static readonly ImportNodePath = '/node/v1/nodes/{repository}/{node}/import';

    /**
     * Import node.
     *
     * Import a node from a foreign repository to the local repository.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `importNode()` instead.
     *
     * This method doesn't expect any request body.
     */
    importNode$Response(params: {
        /**
         * The id of the foreign repository
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * Parent node where to store it locally, may also use -userhome- or -inbox-
         */
        parent: string;
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.ImportNodePath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('parent', params.parent, {});
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
                    return r as StrictHttpResponse<NodeEntry>;
                }),
            );
    }

    /**
     * Import node.
     *
     * Import a node from a foreign repository to the local repository.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `importNode$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    importNode(params: {
        /**
         * The id of the foreign repository
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * Parent node where to store it locally, may also use -userhome- or -inbox-
         */
        parent: string;
    }): Observable<NodeEntry> {
        return this.importNode$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation islocked
     */
    static readonly IslockedPath = '/node/v1/nodes/{repository}/{node}/lock/status';

    /**
     * locked status of a node.
     *
     * locked status of a node.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `islocked()` instead.
     *
     * This method doesn't expect any request body.
     */
    islocked$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<StrictHttpResponse<NodeLocked>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.IslockedPath, 'get');
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
                    return r as StrictHttpResponse<NodeLocked>;
                }),
            );
    }

    /**
     * locked status of a node.
     *
     * locked status of a node.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `islocked$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    islocked(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<NodeLocked> {
        return this.islocked$Response(params).pipe(
            map((r: StrictHttpResponse<NodeLocked>) => r.body as NodeLocked),
        );
    }

    /**
     * Path part for operation prepareUsage
     */
    static readonly PrepareUsagePath = '/node/v1/nodes/{repository}/{node}/prepareUsage';

    /**
     * create remote object and get properties.
     *
     * create remote object and get properties.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `prepareUsage()` instead.
     *
     * This method doesn't expect any request body.
     */
    prepareUsage$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<StrictHttpResponse<NodeRemote>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.PrepareUsagePath, 'post');
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
                    return r as StrictHttpResponse<NodeRemote>;
                }),
            );
    }

    /**
     * create remote object and get properties.
     *
     * create remote object and get properties.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `prepareUsage$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    prepareUsage(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<NodeRemote> {
        return this.prepareUsage$Response(params).pipe(
            map((r: StrictHttpResponse<NodeRemote>) => r.body as NodeRemote),
        );
    }

    /**
     * Path part for operation updateShare
     */
    static readonly UpdateSharePath = '/node/v1/nodes/{repository}/{node}/shares/{shareId}';

    /**
     * update share of a node.
     *
     * update the specified share id
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `updateShare()` instead.
     *
     * This method doesn't expect any request body.
     */
    updateShare$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * share id
         */
        shareId: string;

        /**
         * expiry date for this share, leave empty or -1 for unlimited
         */
        expiryDate?: number;

        /**
         * new password for share, leave empty if you don&#x27;t want to change it
         */
        password?: string;
    }): Observable<StrictHttpResponse<NodeShare>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.UpdateSharePath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.path('shareId', params.shareId, {});
            rb.query('expiryDate', params.expiryDate, {});
            rb.query('password', params.password, {});
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
                    return r as StrictHttpResponse<NodeShare>;
                }),
            );
    }

    /**
     * update share of a node.
     *
     * update the specified share id
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `updateShare$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    updateShare(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * share id
         */
        shareId: string;

        /**
         * expiry date for this share, leave empty or -1 for unlimited
         */
        expiryDate?: number;

        /**
         * new password for share, leave empty if you don&#x27;t want to change it
         */
        password?: string;
    }): Observable<NodeShare> {
        return this.updateShare$Response(params).pipe(
            map((r: StrictHttpResponse<NodeShare>) => r.body as NodeShare),
        );
    }

    /**
     * Path part for operation removeShare
     */
    static readonly RemoveSharePath = '/node/v1/nodes/{repository}/{node}/shares/{shareId}';

    /**
     * Remove share of a node.
     *
     * Remove the specified share id
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `removeShare()` instead.
     *
     * This method doesn't expect any request body.
     */
    removeShare$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * share id
         */
        shareId: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.RemoveSharePath, 'delete');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.path('shareId', params.shareId, {});
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
     * Remove share of a node.
     *
     * Remove the specified share id
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `removeShare$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    removeShare(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * share id
         */
        shareId: string;
    }): Observable<any> {
        return this.removeShare$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation reportNode
     */
    static readonly ReportNodePath = '/node/v1/nodes/{repository}/{node}/report';

    /**
     * Report the node.
     *
     * Report a node to notify the admin about an issue)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `reportNode()` instead.
     *
     * This method doesn't expect any request body.
     */
    reportNode$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * the reason for the report
         */
        reason: string;

        /**
         * mail of reporting user
         */
        userEmail: string;

        /**
         * additional user comment
         */
        userComment?: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.ReportNodePath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('reason', params.reason, {});
            rb.query('userEmail', params.userEmail, {});
            rb.query('userComment', params.userComment, {});
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
     * Report the node.
     *
     * Report a node to notify the admin about an issue)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `reportNode$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    reportNode(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * the reason for the report
         */
        reason: string;

        /**
         * mail of reporting user
         */
        userEmail: string;

        /**
         * additional user comment
         */
        userComment?: string;
    }): Observable<any> {
        return this.reportNode$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation revertVersion
     */
    static readonly RevertVersionPath =
        '/node/v1/nodes/{repository}/{node}/versions/{major}/{minor}/_revert';

    /**
     * Revert to node version.
     *
     * Revert to node version.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `revertVersion()` instead.
     *
     * This method doesn't expect any request body.
     */
    revertVersion$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * major version
         */
        major: number;

        /**
         * minor version
         */
        minor: number;
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.RevertVersionPath, 'put');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.path('major', params.major, {});
            rb.path('minor', params.minor, {});
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
                    return r as StrictHttpResponse<NodeEntry>;
                }),
            );
    }

    /**
     * Revert to node version.
     *
     * Revert to node version.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `revertVersion$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    revertVersion(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * major version
         */
        major: number;

        /**
         * minor version
         */
        minor: number;
    }): Observable<NodeEntry> {
        return this.revertVersion$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation setOwner
     */
    static readonly SetOwnerPath = '/node/v1/nodes/{repository}/{node}/owner';

    /**
     * Set owner of node.
     *
     * Set owner of node.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `setOwner()` instead.
     *
     * This method doesn't expect any request body.
     */
    setOwner$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * username
         */
        username?: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.SetOwnerPath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('username', params.username, {});
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
     * Set owner of node.
     *
     * Set owner of node.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `setOwner$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    setOwner(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * username
         */
        username?: string;
    }): Observable<any> {
        return this.setOwner$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation setProperty
     */
    static readonly SetPropertyPath = '/node/v1/nodes/{repository}/{node}/property';

    /**
     * Set single property of node.
     *
     * When the property is unset (null), it will be removed
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `setProperty()` instead.
     *
     * This method doesn't expect any request body.
     */
    setProperty$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * property
         */
        property: string;

        /**
         * value
         */
        value?: Array<string>;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.SetPropertyPath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
            rb.query('property', params.property, {});
            rb.query('value', params.value, {});
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
     * Set single property of node.
     *
     * When the property is unset (null), it will be removed
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `setProperty$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    setProperty(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * property
         */
        property: string;

        /**
         * value
         */
        value?: Array<string>;
    }): Observable<any> {
        return this.setProperty$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation storeXApiData
     */
    static readonly StoreXApiDataPath = '/node/v1/nodes/{repository}/{node}/xapi';

    /**
     * Store xApi-Conform data for a given node.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `storeXApiData()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    storeXApiData$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * xApi conform json data
         */
        body: string;
    }): Observable<StrictHttpResponse<{}>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.StoreXApiDataPath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('node', params.node, {});
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
                    return r as StrictHttpResponse<{}>;
                }),
            );
    }

    /**
     * Store xApi-Conform data for a given node.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `storeXApiData$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    storeXApiData(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;

        /**
         * xApi conform json data
         */
        body: string;
    }): Observable<{}> {
        return this.storeXApiData$Response(params).pipe(
            map((r: StrictHttpResponse<{}>) => r.body as {}),
        );
    }

    /**
     * Path part for operation unlock
     */
    static readonly UnlockPath = '/node/v1/nodes/{repository}/{node}/lock/unlock';

    /**
     * unlock node.
     *
     * unlock node.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `unlock()` instead.
     *
     * This method doesn't expect any request body.
     */
    unlock$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, NodeV1Service.UnlockPath, 'get');
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
                    return r as StrictHttpResponse<any>;
                }),
            );
    }

    /**
     * unlock node.
     *
     * unlock node.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `unlock$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    unlock(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<any> {
        return this.unlock$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }
}
