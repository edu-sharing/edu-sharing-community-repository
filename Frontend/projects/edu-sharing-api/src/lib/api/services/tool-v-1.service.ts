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

import { NodeEntry } from '../models/node-entry';

@Injectable({
    providedIn: 'root',
})
export class ToolV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation getAllToolDefinitions
     */
    static readonly GetAllToolDefinitionsPath = '/tool/v1/tools/{repository}/tooldefinitions';

    /**
     * Get all ToolDefinitions.
     *
     * Get all ToolDefinitions.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getAllToolDefinitions()` instead.
     *
     * This method doesn't expect any request body.
     */
    getAllToolDefinitions$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, ToolV1Service.GetAllToolDefinitionsPath, 'get');
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
                    return r as StrictHttpResponse<NodeEntry>;
                }),
            );
    }

    /**
     * Get all ToolDefinitions.
     *
     * Get all ToolDefinitions.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getAllToolDefinitions$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getAllToolDefinitions(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;
    }): Observable<NodeEntry> {
        return this.getAllToolDefinitions$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation createToolDefintition
     */
    static readonly CreateToolDefintitionPath = '/tool/v1/tools/{repository}/tooldefinitions';

    /**
     * Create a new tool definition object.
     *
     * Create a new tool definition object.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `createToolDefintition()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    createToolDefintition$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * rename if the same node name exists
         */
        renameIfExists?: boolean;

        /**
         * comment, leave empty &#x3D; no inital version
         */
        versionComment?: string;

        /**
         * properties, example: {"{http://www.alfresco.org/model/content/1.0}name": ["test"]}
         */
        body: {
            [key: string]: Array<string>;
        };
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            ToolV1Service.CreateToolDefintitionPath,
            'post',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.query('renameIfExists', params.renameIfExists, {});
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
     * Create a new tool definition object.
     *
     * Create a new tool definition object.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `createToolDefintition$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    createToolDefintition(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * rename if the same node name exists
         */
        renameIfExists?: boolean;

        /**
         * comment, leave empty &#x3D; no inital version
         */
        versionComment?: string;

        /**
         * properties, example: {"{http://www.alfresco.org/model/content/1.0}name": ["test"]}
         */
        body: {
            [key: string]: Array<string>;
        };
    }): Observable<NodeEntry> {
        return this.createToolDefintition$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation getInstances
     */
    static readonly GetInstancesPath = '/tool/v1/tools/{repository}/{toolDefinition}/toolinstances';

    /**
     * Get Instances of a ToolDefinition.
     *
     * Get Instances of a ToolDefinition.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getInstances()` instead.
     *
     * This method doesn't expect any request body.
     */
    getInstances$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        toolDefinition: string;
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, ToolV1Service.GetInstancesPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('toolDefinition', params.toolDefinition, {});
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
     * Get Instances of a ToolDefinition.
     *
     * Get Instances of a ToolDefinition.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getInstances$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getInstances(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        toolDefinition: string;
    }): Observable<NodeEntry> {
        return this.getInstances$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation createToolInstance
     */
    static readonly CreateToolInstancePath =
        '/tool/v1/tools/{repository}/{toolDefinition}/toolinstances';

    /**
     * Create a new tool Instance object.
     *
     * Create a new tool Instance object.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `createToolInstance()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    createToolInstance$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of parent node must have tool_definition aspect
         */
        toolDefinition: string;

        /**
         * rename if the same node name exists
         */
        renameIfExists?: boolean;

        /**
         * comment, leave empty &#x3D; no inital version
         */
        versionComment?: string;

        /**
         * properties, example: {"{http://www.alfresco.org/model/content/1.0}name": ["test"]}
         */
        body: {
            [key: string]: Array<string>;
        };
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, ToolV1Service.CreateToolInstancePath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('toolDefinition', params.toolDefinition, {});
            rb.query('renameIfExists', params.renameIfExists, {});
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
     * Create a new tool Instance object.
     *
     * Create a new tool Instance object.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `createToolInstance$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    createToolInstance(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of parent node must have tool_definition aspect
         */
        toolDefinition: string;

        /**
         * rename if the same node name exists
         */
        renameIfExists?: boolean;

        /**
         * comment, leave empty &#x3D; no inital version
         */
        versionComment?: string;

        /**
         * properties, example: {"{http://www.alfresco.org/model/content/1.0}name": ["test"]}
         */
        body: {
            [key: string]: Array<string>;
        };
    }): Observable<NodeEntry> {
        return this.createToolInstance$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation createToolObject
     */
    static readonly CreateToolObjectPath = '/tool/v1/tools/{repository}/{toolinstance}/toolobject';

    /**
     * Create a new tool object for a given tool instance.
     *
     * Create a new tool object for a given tool instance.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `createToolObject()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    createToolObject$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of parent node (a tool instance object)
         */
        toolinstance: string;

        /**
         * rename if the same node name exists
         */
        renameIfExists?: boolean;

        /**
         * comment, leave empty &#x3D; no inital version
         */
        versionComment?: string;

        /**
         * properties, example: {"{http://www.alfresco.org/model/content/1.0}name": ["test"]}
         */
        body: {
            [key: string]: Array<string>;
        };
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, ToolV1Service.CreateToolObjectPath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('toolinstance', params.toolinstance, {});
            rb.query('renameIfExists', params.renameIfExists, {});
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
     * Create a new tool object for a given tool instance.
     *
     * Create a new tool object for a given tool instance.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `createToolObject$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    createToolObject(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of parent node (a tool instance object)
         */
        toolinstance: string;

        /**
         * rename if the same node name exists
         */
        renameIfExists?: boolean;

        /**
         * comment, leave empty &#x3D; no inital version
         */
        versionComment?: string;

        /**
         * properties, example: {"{http://www.alfresco.org/model/content/1.0}name": ["test"]}
         */
        body: {
            [key: string]: Array<string>;
        };
    }): Observable<NodeEntry> {
        return this.createToolObject$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }

    /**
     * Path part for operation getInstance
     */
    static readonly GetInstancePath = '/tool/v1/tools/{repository}/{nodeid}/toolinstance';

    /**
     * Get Instances of a ToolDefinition.
     *
     * Get Instances of a ToolDefinition.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getInstance()` instead.
     *
     * This method doesn't expect any request body.
     */
    getInstance$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        nodeid: string;
    }): Observable<StrictHttpResponse<NodeEntry>> {
        const rb = new RequestBuilder(this.rootUrl, ToolV1Service.GetInstancePath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('nodeid', params.nodeid, {});
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
     * Get Instances of a ToolDefinition.
     *
     * Get Instances of a ToolDefinition.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getInstance$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getInstance(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        nodeid: string;
    }): Observable<NodeEntry> {
        return this.getInstance$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntry>) => r.body as NodeEntry),
        );
    }
}
