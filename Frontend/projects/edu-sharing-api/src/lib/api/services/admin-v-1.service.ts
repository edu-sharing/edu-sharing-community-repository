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

import { AdminStatistics } from '../models/admin-statistics';
import { CacheCluster } from '../models/cache-cluster';
import { CacheInfo } from '../models/cache-info';
import { CollectionsResult } from '../models/collections-result';
import { ExcelResult } from '../models/excel-result';
import { Node } from '../models/node';
import { PersonDeleteOptions } from '../models/person-delete-options';
import { PersonReport } from '../models/person-report';
import { RepositoryConfig } from '../models/repository-config';
import { SearchResult } from '../models/search-result';
import { SearchResultElastic } from '../models/search-result-elastic';
import { UploadResult } from '../models/upload-result';

@Injectable({
    providedIn: 'root',
})
export class AdminV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation addApplication
     */
    static readonly AddApplicationPath = '/admin/v1/applications/xml';

    /**
     * register/add an application via xml file.
     *
     * register the xml file provided.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `addApplication()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    addApplication$Response(params?: {
        body?: {
            /**
             * XML file for app to register
             */
            xml: {};
        };
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.AddApplicationPath, 'put');
        if (params) {
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
                    return r as StrictHttpResponse<string>;
                }),
            );
    }

    /**
     * register/add an application via xml file.
     *
     * register the xml file provided.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `addApplication$Response()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    addApplication(params?: {
        body?: {
            /**
             * XML file for app to register
             */
            xml: {};
        };
    }): Observable<string> {
        return this.addApplication$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation getApplications
     */
    static readonly GetApplicationsPath = '/admin/v1/applications';

    /**
     * list applications.
     *
     * List all registered applications.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getApplications()` instead.
     *
     * This method doesn't expect any request body.
     */
    getApplications$Response(params?: {}): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.GetApplicationsPath, 'get');
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
                    return r as StrictHttpResponse<string>;
                }),
            );
    }

    /**
     * list applications.
     *
     * List all registered applications.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getApplications$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getApplications(params?: {}): Observable<string> {
        return this.getApplications$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation addApplication1
     */
    static readonly AddApplication1Path = '/admin/v1/applications';

    /**
     * register/add an application.
     *
     * register the specified application.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `addApplication1()` instead.
     *
     * This method doesn't expect any request body.
     */
    addApplication1$Response(params: {
        /**
         * Remote application metadata url
         */
        url: string;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.AddApplication1Path, 'put');
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
                    return r as StrictHttpResponse<string>;
                }),
            );
    }

    /**
     * register/add an application.
     *
     * register the specified application.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `addApplication1$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    addApplication1(params: {
        /**
         * Remote application metadata url
         */
        url: string;
    }): Observable<string> {
        return this.addApplication1$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation addToolpermission
     */
    static readonly AddToolpermissionPath = '/admin/v1/toolpermissions/add/{name}';

    /**
     * add a new toolpermissions.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `addToolpermission()` instead.
     *
     * This method doesn't expect any request body.
     */
    addToolpermission$Response(params: {
        /**
         * Name/ID of toolpermission
         */
        name: string;
    }): Observable<StrictHttpResponse<Node>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.AddToolpermissionPath, 'post');
        if (params) {
            rb.path('name', params.name, {});
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
                    return r as StrictHttpResponse<Node>;
                }),
            );
    }

    /**
     * add a new toolpermissions.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `addToolpermission$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    addToolpermission(params: {
        /**
         * Name/ID of toolpermission
         */
        name: string;
    }): Observable<Node> {
        return this.addToolpermission$Response(params).pipe(
            map((r: StrictHttpResponse<Node>) => r.body as Node),
        );
    }

    /**
     * Path part for operation applyTemplate
     */
    static readonly ApplyTemplatePath = '/admin/v1/applyTemplate';

    /**
     * apply a folder template.
     *
     * apply a folder template.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `applyTemplate()` instead.
     *
     * This method doesn't expect any request body.
     */
    applyTemplate$Response(params: {
        /**
         * Template Filename
         */
        template: string;

        /**
         * Group name (authority name)
         */
        group: string;

        /**
         * Folder name
         */
        folder?: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.ApplyTemplatePath, 'post');
        if (params) {
            rb.query('template', params.template, {});
            rb.query('group', params.group, {});
            rb.query('folder', params.folder, {});
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
     * apply a folder template.
     *
     * apply a folder template.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `applyTemplate$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    applyTemplate(params: {
        /**
         * Template Filename
         */
        template: string;

        /**
         * Group name (authority name)
         */
        group: string;

        /**
         * Folder name
         */
        folder?: string;
    }): Observable<any> {
        return this.applyTemplate$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation cancelJob
     */
    static readonly CancelJobPath = '/admin/v1/jobs/{job}';

    /**
     * cancel a running job.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `cancelJob()` instead.
     *
     * This method doesn't expect any request body.
     */
    cancelJob$Response(params: { job: string }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.CancelJobPath, 'delete');
        if (params) {
            rb.path('job', params.job, {});
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
     * cancel a running job.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `cancelJob$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    cancelJob(params: { job: string }): Observable<any> {
        return this.cancelJob$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation changeLogging
     */
    static readonly ChangeLoggingPath = '/admin/v1/log';

    /**
     * Change the loglevel for classes at runtime.
     *
     * Root appenders are used. Check the appender treshold.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `changeLogging()` instead.
     *
     * This method doesn't expect any request body.
     */
    changeLogging$Response(params: {
        /**
         * name
         */
        name: string;

        /**
         * loglevel
         */
        loglevel: string;

        /**
         * appender
         */
        appender?: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.ChangeLoggingPath, 'post');
        if (params) {
            rb.query('name', params.name, {});
            rb.query('loglevel', params.loglevel, {});
            rb.query('appender', params.appender, {});
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
     * Change the loglevel for classes at runtime.
     *
     * Root appenders are used. Check the appender treshold.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `changeLogging$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    changeLogging(params: {
        /**
         * name
         */
        name: string;

        /**
         * loglevel
         */
        loglevel: string;

        /**
         * appender
         */
        appender?: string;
    }): Observable<any> {
        return this.changeLogging$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation clearCache
     */
    static readonly ClearCachePath = '/admin/v1/cache/clearCache';

    /**
     * clear cache.
     *
     * clear cache
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `clearCache()` instead.
     *
     * This method doesn't expect any request body.
     */
    clearCache$Response(params?: {
        /**
         * bean
         */
        bean?: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.ClearCachePath, 'post');
        if (params) {
            rb.query('bean', params.bean, {});
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
     * clear cache.
     *
     * clear cache
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `clearCache$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    clearCache(params?: {
        /**
         * bean
         */
        bean?: string;
    }): Observable<any> {
        return this.clearCache$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation deletePerson
     */
    static readonly DeletePersonPath = '/admin/v1/deletePersons';

    /**
     * delete persons.
     *
     * delete the given persons. Their status must be set to "todelete"
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `deletePerson()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    deletePerson$Response(params: {
        /**
         * names of the users to delete
         */
        username: Array<string>;

        /**
         * options object what and how to delete user contents
         */
        body?: PersonDeleteOptions;
    }): Observable<StrictHttpResponse<PersonReport>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.DeletePersonPath, 'put');
        if (params) {
            rb.query('username', params.username, {});
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
                    return r as StrictHttpResponse<PersonReport>;
                }),
            );
    }

    /**
     * delete persons.
     *
     * delete the given persons. Their status must be set to "todelete"
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `deletePerson$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    deletePerson(params: {
        /**
         * names of the users to delete
         */
        username: Array<string>;

        /**
         * options object what and how to delete user contents
         */
        body?: PersonDeleteOptions;
    }): Observable<PersonReport> {
        return this.deletePerson$Response(params).pipe(
            map((r: StrictHttpResponse<PersonReport>) => r.body as PersonReport),
        );
    }

    /**
     * Path part for operation exportByLucene
     */
    static readonly ExportByLucenePath = '/admin/v1/lucene/export';

    /**
     * Search for custom lucene query and choose specific properties to load.
     *
     * e.g. @cm\:name:"*"
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `exportByLucene()` instead.
     *
     * This method doesn't expect any request body.
     */
    exportByLucene$Response(params?: {
        /**
         * query
         */
        query?: string;

        /**
         * sort properties
         */
        sortProperties?: Array<string>;

        /**
         * sort ascending, true if not set. Use multiple values to change the direction according to the given property at the same index
         */
        sortAscending?: Array<boolean>;

        /**
         * properties to fetch, use parent::&lt;property&gt; to include parent property values
         */
        properties?: Array<string>;

        /**
         * store, workspace or archive
         */
        store?: 'Workspace' | 'Archive';

        /**
         * authority scope to search for
         */
        authorityScope?: Array<string>;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.ExportByLucenePath, 'get');
        if (params) {
            rb.query('query', params.query, {});
            rb.query('sortProperties', params.sortProperties, {});
            rb.query('sortAscending', params.sortAscending, {});
            rb.query('properties', params.properties, {});
            rb.query('store', params.store, {});
            rb.query('authorityScope', params.authorityScope, {});
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
     * Search for custom lucene query and choose specific properties to load.
     *
     * e.g. @cm\:name:"*"
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `exportByLucene$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    exportByLucene(params?: {
        /**
         * query
         */
        query?: string;

        /**
         * sort properties
         */
        sortProperties?: Array<string>;

        /**
         * sort ascending, true if not set. Use multiple values to change the direction according to the given property at the same index
         */
        sortAscending?: Array<boolean>;

        /**
         * properties to fetch, use parent::&lt;property&gt; to include parent property values
         */
        properties?: Array<string>;

        /**
         * store, workspace or archive
         */
        store?: 'Workspace' | 'Archive';

        /**
         * authority scope to search for
         */
        authorityScope?: Array<string>;
    }): Observable<string> {
        return this.exportByLucene$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation exportLom
     */
    static readonly ExportLomPath = '/admin/v1/export/lom';

    /**
     * Export Nodes with LOM Metadata Format.
     *
     * Export Nodes with LOM Metadata Format.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `exportLom()` instead.
     *
     * This method doesn't expect any request body.
     */
    exportLom$Response(params: {
        /**
         * filterQuery
         */
        filterQuery: string;

        /**
         * targetDir
         */
        targetDir: string;

        /**
         * subObjectHandler
         */
        subObjectHandler: boolean;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.ExportLomPath, 'get');
        if (params) {
            rb.query('filterQuery', params.filterQuery, {});
            rb.query('targetDir', params.targetDir, {});
            rb.query('subObjectHandler', params.subObjectHandler, {});
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
     * Export Nodes with LOM Metadata Format.
     *
     * Export Nodes with LOM Metadata Format.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `exportLom$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    exportLom(params: {
        /**
         * filterQuery
         */
        filterQuery: string;

        /**
         * targetDir
         */
        targetDir: string;

        /**
         * subObjectHandler
         */
        subObjectHandler: boolean;
    }): Observable<any> {
        return this.exportLom$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation getAllJobs
     */
    static readonly GetAllJobsPath = '/admin/v1/jobs/all';

    /**
     * get all available jobs.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getAllJobs()` instead.
     *
     * This method doesn't expect any request body.
     */
    getAllJobs$Response(params?: {}): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.GetAllJobsPath, 'get');
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
                    return r as StrictHttpResponse<string>;
                }),
            );
    }

    /**
     * get all available jobs.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getAllJobs$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getAllJobs(params?: {}): Observable<string> {
        return this.getAllJobs$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation getAllToolpermissions
     */
    static readonly GetAllToolpermissionsPath = '/admin/v1/toolpermissions/{authority}';

    /**
     * get all toolpermissions for an authority.
     *
     * Returns explicit (rights set for this authority) + effective (resulting rights for this authority) toolpermission
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getAllToolpermissions()` instead.
     *
     * This method doesn't expect any request body.
     */
    getAllToolpermissions$Response(params: {
        /**
         * Authority to load (user or group)
         */
        authority: string;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            AdminV1Service.GetAllToolpermissionsPath,
            'get',
        );
        if (params) {
            rb.path('authority', params.authority, {});
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
     * get all toolpermissions for an authority.
     *
     * Returns explicit (rights set for this authority) + effective (resulting rights for this authority) toolpermission
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getAllToolpermissions$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getAllToolpermissions(params: {
        /**
         * Authority to load (user or group)
         */
        authority: string;
    }): Observable<string> {
        return this.getAllToolpermissions$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation setToolpermissions
     */
    static readonly SetToolpermissionsPath = '/admin/v1/toolpermissions/{authority}';

    /**
     * set toolpermissions for an authority.
     *
     * If a toolpermission has status UNDEFINED, it will remove explicit permissions for the authority
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `setToolpermissions()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    setToolpermissions$Response(params: {
        /**
         * Authority to set (user or group)
         */
        authority: string;
        body?: {
            [key: string]: 'ALLOWED' | 'DENIED' | 'UNDEFINED';
        };
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.SetToolpermissionsPath, 'put');
        if (params) {
            rb.path('authority', params.authority, {});
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
                    return r as StrictHttpResponse<string>;
                }),
            );
    }

    /**
     * set toolpermissions for an authority.
     *
     * If a toolpermission has status UNDEFINED, it will remove explicit permissions for the authority
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `setToolpermissions$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    setToolpermissions(params: {
        /**
         * Authority to set (user or group)
         */
        authority: string;
        body?: {
            [key: string]: 'ALLOWED' | 'DENIED' | 'UNDEFINED';
        };
    }): Observable<string> {
        return this.setToolpermissions$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation getApplicationXml
     */
    static readonly GetApplicationXmlPath = '/admin/v1/applications/{xml}';

    /**
     * list any xml properties (like from homeApplication.properties.xml).
     *
     * list any xml properties (like from homeApplication.properties.xml)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getApplicationXml()` instead.
     *
     * This method doesn't expect any request body.
     */
    getApplicationXml$Response(params: {
        /**
         * Properties Filename (*.xml)
         */
        xml: string;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.GetApplicationXmlPath, 'get');
        if (params) {
            rb.path('xml', params.xml, {});
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
     * list any xml properties (like from homeApplication.properties.xml).
     *
     * list any xml properties (like from homeApplication.properties.xml)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getApplicationXml$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getApplicationXml(params: {
        /**
         * Properties Filename (*.xml)
         */
        xml: string;
    }): Observable<string> {
        return this.getApplicationXml$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation updateApplicationXml
     */
    static readonly UpdateApplicationXmlPath = '/admin/v1/applications/{xml}';

    /**
     * edit any properties xml (like homeApplication.properties.xml).
     *
     * if the key exists, it will be overwritten. Otherwise, it will be created. You only need to transfer keys you want to edit
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `updateApplicationXml()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    updateApplicationXml$Response(params: {
        /**
         * Properties Filename (*.xml)
         */
        xml: string;
        body?: {
            [key: string]: string;
        };
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.UpdateApplicationXmlPath, 'put');
        if (params) {
            rb.path('xml', params.xml, {});
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
     * edit any properties xml (like homeApplication.properties.xml).
     *
     * if the key exists, it will be overwritten. Otherwise, it will be created. You only need to transfer keys you want to edit
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `updateApplicationXml$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    updateApplicationXml(params: {
        /**
         * Properties Filename (*.xml)
         */
        xml: string;
        body?: {
            [key: string]: string;
        };
    }): Observable<any> {
        return this.updateApplicationXml$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation getCacheEntries
     */
    static readonly GetCacheEntriesPath = '/admin/v1/cache/cacheEntries/{id}';

    /**
     * Get entries of a cache.
     *
     * Get entries of a cache.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getCacheEntries()` instead.
     *
     * This method doesn't expect any request body.
     */
    getCacheEntries$Response(params: {
        /**
         * Id/bean name of the cache
         */
        id: string;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.GetCacheEntriesPath, 'get');
        if (params) {
            rb.path('id', params.id, {});
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
     * Get entries of a cache.
     *
     * Get entries of a cache.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getCacheEntries$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getCacheEntries(params: {
        /**
         * Id/bean name of the cache
         */
        id: string;
    }): Observable<string> {
        return this.getCacheEntries$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation getCacheInfo
     */
    static readonly GetCacheInfoPath = '/admin/v1/cache/cacheInfo/{id}';

    /**
     * Get information about a cache.
     *
     * Get information about a cache.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getCacheInfo()` instead.
     *
     * This method doesn't expect any request body.
     */
    getCacheInfo$Response(params: {
        /**
         * Id/bean name of the cache
         */
        id: string;
    }): Observable<StrictHttpResponse<CacheInfo>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.GetCacheInfoPath, 'get');
        if (params) {
            rb.path('id', params.id, {});
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
                    return r as StrictHttpResponse<CacheInfo>;
                }),
            );
    }

    /**
     * Get information about a cache.
     *
     * Get information about a cache.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getCacheInfo$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getCacheInfo(params: {
        /**
         * Id/bean name of the cache
         */
        id: string;
    }): Observable<CacheInfo> {
        return this.getCacheInfo$Response(params).pipe(
            map((r: StrictHttpResponse<CacheInfo>) => r.body as CacheInfo),
        );
    }

    /**
     * Path part for operation getCatalinaOut
     */
    static readonly GetCatalinaOutPath = '/admin/v1/catalina';

    /**
     * Get last info from catalina out.
     *
     * Get catalina.out log.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getCatalinaOut()` instead.
     *
     * This method doesn't expect any request body.
     */
    getCatalinaOut$Response(params?: {}): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.GetCatalinaOutPath, 'get');
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
                    return r as StrictHttpResponse<string>;
                }),
            );
    }

    /**
     * Get last info from catalina out.
     *
     * Get catalina.out log.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getCatalinaOut$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getCatalinaOut(params?: {}): Observable<string> {
        return this.getCatalinaOut$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation getCluster
     */
    static readonly GetClusterPath = '/admin/v1/clusterInfo';

    /**
     * Get information about the Cluster.
     *
     * Get information the Cluster
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getCluster()` instead.
     *
     * This method doesn't expect any request body.
     */
    getCluster$Response(params?: {}): Observable<StrictHttpResponse<CacheCluster>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.GetClusterPath, 'get');
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
                    return r as StrictHttpResponse<CacheCluster>;
                }),
            );
    }

    /**
     * Get information about the Cluster.
     *
     * Get information the Cluster
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getCluster$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getCluster(params?: {}): Observable<CacheCluster> {
        return this.getCluster$Response(params).pipe(
            map((r: StrictHttpResponse<CacheCluster>) => r.body as CacheCluster),
        );
    }

    /**
     * Path part for operation getClusters
     */
    static readonly GetClustersPath = '/admin/v1/clusterInfos';

    /**
     * Get information about the Cluster.
     *
     * Get information the Cluster
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getClusters()` instead.
     *
     * This method doesn't expect any request body.
     */
    getClusters$Response(params?: {}): Observable<StrictHttpResponse<CacheCluster>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.GetClustersPath, 'get');
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
                    return r as StrictHttpResponse<CacheCluster>;
                }),
            );
    }

    /**
     * Get information about the Cluster.
     *
     * Get information the Cluster
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getClusters$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getClusters(params?: {}): Observable<CacheCluster> {
        return this.getClusters$Response(params).pipe(
            map((r: StrictHttpResponse<CacheCluster>) => r.body as CacheCluster),
        );
    }

    /**
     * Path part for operation getConfig
     */
    static readonly GetConfigPath = '/admin/v1/repositoryConfig';

    /**
     * get the repository config object.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getConfig()` instead.
     *
     * This method doesn't expect any request body.
     */
    getConfig$Response(params?: {}): Observable<StrictHttpResponse<RepositoryConfig>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.GetConfigPath, 'get');
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
                    return r as StrictHttpResponse<RepositoryConfig>;
                }),
            );
    }

    /**
     * get the repository config object.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getConfig$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getConfig(params?: {}): Observable<RepositoryConfig> {
        return this.getConfig$Response(params).pipe(
            map((r: StrictHttpResponse<RepositoryConfig>) => r.body as RepositoryConfig),
        );
    }

    /**
     * Path part for operation setConfig
     */
    static readonly SetConfigPath = '/admin/v1/repositoryConfig';

    /**
     * set/update the repository config object.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `setConfig()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    setConfig$Response(params?: { body?: RepositoryConfig }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.SetConfigPath, 'put');
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
     * set/update the repository config object.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `setConfig$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    setConfig(params?: { body?: RepositoryConfig }): Observable<any> {
        return this.setConfig$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation getConfigFile
     */
    static readonly GetConfigFilePath = '/admin/v1/configFile';

    /**
     * get a base system config file (e.g. edu-sharing.conf).
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getConfigFile()` instead.
     *
     * This method doesn't expect any request body.
     */
    getConfigFile$Response(params: {
        /**
         * filename to fetch
         */
        filename: string;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.GetConfigFilePath, 'get');
        if (params) {
            rb.query('filename', params.filename, {});
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
     * get a base system config file (e.g. edu-sharing.conf).
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getConfigFile$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getConfigFile(params: {
        /**
         * filename to fetch
         */
        filename: string;
    }): Observable<string> {
        return this.getConfigFile$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation updateConfigFile
     */
    static readonly UpdateConfigFilePath = '/admin/v1/configFile';

    /**
     * update a base system config file (e.g. edu-sharing.conf).
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `updateConfigFile()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    updateConfigFile$Response(params: {
        /**
         * filename to fetch
         */
        filename: string;
        body?: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.UpdateConfigFilePath, 'put');
        if (params) {
            rb.query('filename', params.filename, {});
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
     * update a base system config file (e.g. edu-sharing.conf).
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `updateConfigFile$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    updateConfigFile(params: {
        /**
         * filename to fetch
         */
        filename: string;
        body?: string;
    }): Observable<any> {
        return this.updateConfigFile$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation getGlobalGroups
     */
    static readonly GetGlobalGroupsPath = '/admin/v1/globalGroups';

    /**
     * Get global groups.
     *
     * Get global groups (groups across repositories).
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getGlobalGroups()` instead.
     *
     * This method doesn't expect any request body.
     */
    getGlobalGroups$Response(params?: {}): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.GetGlobalGroupsPath, 'get');
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
                    return r as StrictHttpResponse<string>;
                }),
            );
    }

    /**
     * Get global groups.
     *
     * Get global groups (groups across repositories).
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getGlobalGroups$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getGlobalGroups(params?: {}): Observable<string> {
        return this.getGlobalGroups$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation getJobs
     */
    static readonly GetJobsPath = '/admin/v1/jobs';

    /**
     * get all running jobs.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getJobs()` instead.
     *
     * This method doesn't expect any request body.
     */
    getJobs$Response(params?: {}): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.GetJobsPath, 'get');
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
                    return r as StrictHttpResponse<string>;
                }),
            );
    }

    /**
     * get all running jobs.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getJobs$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getJobs(params?: {}): Observable<string> {
        return this.getJobs$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation getLightbendConfig
     */
    static readonly GetLightbendConfigPath = '/admin/v1/config/merged';

    /**
     * Get the fully merged & parsed (lightbend) backend config
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getLightbendConfig()` instead.
     *
     * This method doesn't expect any request body.
     */
    getLightbendConfig$Response(params?: {}): Observable<StrictHttpResponse<{}>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.GetLightbendConfigPath, 'get');
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
                    return r as StrictHttpResponse<{}>;
                }),
            );
    }

    /**
     * Get the fully merged & parsed (lightbend) backend config
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getLightbendConfig$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getLightbendConfig(params?: {}): Observable<{}> {
        return this.getLightbendConfig$Response(params).pipe(
            map((r: StrictHttpResponse<{}>) => r.body as {}),
        );
    }

    /**
     * Path part for operation getOaiClasses
     */
    static readonly GetOaiClassesPath = '/admin/v1/import/oai/classes';

    /**
     * Get OAI class names.
     *
     * Get available importer classes for OAI import.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getOaiClasses()` instead.
     *
     * This method doesn't expect any request body.
     */
    getOaiClasses$Response(params?: {}): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.GetOaiClassesPath, 'get');
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
                    return r as StrictHttpResponse<string>;
                }),
            );
    }

    /**
     * Get OAI class names.
     *
     * Get available importer classes for OAI import.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getOaiClasses$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getOaiClasses(params?: {}): Observable<string> {
        return this.getOaiClasses$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation getPropertyToMds
     */
    static readonly GetPropertyToMdsPath = '/admin/v1/propertyToMds';

    /**
     * Get a Mds Valuespace for all values of the given properties.
     *
     * Get a Mds Valuespace for all values of the given properties.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getPropertyToMds()` instead.
     *
     * This method doesn't expect any request body.
     */
    getPropertyToMds$Response(params: {
        /**
         * one or more properties
         */
        properties: Array<string>;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.GetPropertyToMdsPath, 'get');
        if (params) {
            rb.query('properties', params.properties, {});
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
     * Get a Mds Valuespace for all values of the given properties.
     *
     * Get a Mds Valuespace for all values of the given properties.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getPropertyToMds$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getPropertyToMds(params: {
        /**
         * one or more properties
         */
        properties: Array<string>;
    }): Observable<string> {
        return this.getPropertyToMds$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation getStatistics
     */
    static readonly GetStatisticsPath = '/admin/v1/statistics';

    /**
     * get statistics.
     *
     * get statistics.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getStatistics()` instead.
     *
     * This method doesn't expect any request body.
     */
    getStatistics$Response(params?: {}): Observable<StrictHttpResponse<AdminStatistics>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.GetStatisticsPath, 'get');
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
                    return r as StrictHttpResponse<AdminStatistics>;
                }),
            );
    }

    /**
     * get statistics.
     *
     * get statistics.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getStatistics$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getStatistics(params?: {}): Observable<AdminStatistics> {
        return this.getStatistics$Response(params).pipe(
            map((r: StrictHttpResponse<AdminStatistics>) => r.body as AdminStatistics),
        );
    }

    /**
     * Path part for operation importCollections
     */
    static readonly ImportCollectionsPath = '/admin/v1/import/collections';

    /**
     * import collections via a xml file.
     *
     * xml file must be structured as defined by the xsd standard
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `importCollections()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    importCollections$Response(params?: {
        /**
         * Id of the root to initialize the collection structure, or &#x27;-root-&#x27; to inflate them on the first level
         */
        parent?: string;
        body?: {
            /**
             * XML file to parse (or zip file containing exactly 1 xml file to parse)
             */
            xml: {};
        };
    }): Observable<StrictHttpResponse<CollectionsResult>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.ImportCollectionsPath, 'post');
        if (params) {
            rb.query('parent', params.parent, {});
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
                    return r as StrictHttpResponse<CollectionsResult>;
                }),
            );
    }

    /**
     * import collections via a xml file.
     *
     * xml file must be structured as defined by the xsd standard
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `importCollections$Response()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    importCollections(params?: {
        /**
         * Id of the root to initialize the collection structure, or &#x27;-root-&#x27; to inflate them on the first level
         */
        parent?: string;
        body?: {
            /**
             * XML file to parse (or zip file containing exactly 1 xml file to parse)
             */
            xml: {};
        };
    }): Observable<CollectionsResult> {
        return this.importCollections$Response(params).pipe(
            map((r: StrictHttpResponse<CollectionsResult>) => r.body as CollectionsResult),
        );
    }

    /**
     * Path part for operation importExcel
     */
    static readonly ImportExcelPath = '/admin/v1/import/excel';

    /**
     * Import excel data.
     *
     * Import excel data.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `importExcel()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    importExcel$Response(params: {
        /**
         * parent
         */
        parent: string;

        /**
         * addToCollection
         */
        addToCollection: boolean;
        body?: {
            /**
             * Excel file to import
             */
            excel: {};
        };
    }): Observable<StrictHttpResponse<ExcelResult>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.ImportExcelPath, 'post');
        if (params) {
            rb.query('parent', params.parent, {});
            rb.query('addToCollection', params.addToCollection, {});
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
                    return r as StrictHttpResponse<ExcelResult>;
                }),
            );
    }

    /**
     * Import excel data.
     *
     * Import excel data.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `importExcel$Response()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    importExcel(params: {
        /**
         * parent
         */
        parent: string;

        /**
         * addToCollection
         */
        addToCollection: boolean;
        body?: {
            /**
             * Excel file to import
             */
            excel: {};
        };
    }): Observable<ExcelResult> {
        return this.importExcel$Response(params).pipe(
            map((r: StrictHttpResponse<ExcelResult>) => r.body as ExcelResult),
        );
    }

    /**
     * Path part for operation importOai
     */
    static readonly ImportOaiPath = '/admin/v1/import/oai';

    /**
     * Import oai data.
     *
     * Import oai data.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `importOai()` instead.
     *
     * This method doesn't expect any request body.
     */
    importOai$Response(params: {
        /**
         * base url
         */
        baseUrl: string;

        /**
         * set/catalog id
         */
        set: string;

        /**
         * metadata prefix
         */
        metadataPrefix: string;

        /**
         * id metadataset
         */
        metadataset?: string;

        /**
         * importer job class name (call /classes to obtain a list)
         */
        className: string;

        /**
         * importer class name (call /classes to obtain a list)
         */
        importerClassName?: string;

        /**
         * RecordHandler class name
         */
        recordHandlerClassName?: string;

        /**
         * BinaryHandler class name (may be empty for none)
         */
        binaryHandlerClassName?: string;

        /**
         * PersistentHandlerClassName class name (may be empty for none)
         */
        persistentHandlerClassName?: string;

        /**
         * url to file
         */
        fileUrl?: string;

        /**
         * OAI Ids to import, can be null than the whole set will be imported
         */
        oaiIds?: string;

        /**
         * force Update of all entries
         */
        forceUpdate?: boolean;

        /**
         * from: datestring yyyy-MM-dd)
         */
        from?: string;

        /**
         * until: datestring yyyy-MM-dd)
         */
        until?: string;

        /**
         * periodInDays: internal sets from and until. only effective if from/until not set)
         */
        periodInDays?: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.ImportOaiPath, 'post');
        if (params) {
            rb.query('baseUrl', params.baseUrl, {});
            rb.query('set', params.set, {});
            rb.query('metadataPrefix', params.metadataPrefix, {});
            rb.query('metadataset', params.metadataset, {});
            rb.query('className', params.className, {});
            rb.query('importerClassName', params.importerClassName, {});
            rb.query('recordHandlerClassName', params.recordHandlerClassName, {});
            rb.query('binaryHandlerClassName', params.binaryHandlerClassName, {});
            rb.query('persistentHandlerClassName', params.persistentHandlerClassName, {});
            rb.query('fileUrl', params.fileUrl, {});
            rb.query('oaiIds', params.oaiIds, {});
            rb.query('forceUpdate', params.forceUpdate, {});
            rb.query('from', params.from, {});
            rb.query('until', params.until, {});
            rb.query('periodInDays', params.periodInDays, {});
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
     * Import oai data.
     *
     * Import oai data.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `importOai$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    importOai(params: {
        /**
         * base url
         */
        baseUrl: string;

        /**
         * set/catalog id
         */
        set: string;

        /**
         * metadata prefix
         */
        metadataPrefix: string;

        /**
         * id metadataset
         */
        metadataset?: string;

        /**
         * importer job class name (call /classes to obtain a list)
         */
        className: string;

        /**
         * importer class name (call /classes to obtain a list)
         */
        importerClassName?: string;

        /**
         * RecordHandler class name
         */
        recordHandlerClassName?: string;

        /**
         * BinaryHandler class name (may be empty for none)
         */
        binaryHandlerClassName?: string;

        /**
         * PersistentHandlerClassName class name (may be empty for none)
         */
        persistentHandlerClassName?: string;

        /**
         * url to file
         */
        fileUrl?: string;

        /**
         * OAI Ids to import, can be null than the whole set will be imported
         */
        oaiIds?: string;

        /**
         * force Update of all entries
         */
        forceUpdate?: boolean;

        /**
         * from: datestring yyyy-MM-dd)
         */
        from?: string;

        /**
         * until: datestring yyyy-MM-dd)
         */
        until?: string;

        /**
         * periodInDays: internal sets from and until. only effective if from/until not set)
         */
        periodInDays?: string;
    }): Observable<any> {
        return this.importOai$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation removeOaiImports
     */
    static readonly RemoveOaiImportsPath = '/admin/v1/import/oai';

    /**
     * Remove deleted imports.
     *
     * Remove deleted imports.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `removeOaiImports()` instead.
     *
     * This method doesn't expect any request body.
     */
    removeOaiImports$Response(params: {
        /**
         * base url
         */
        baseUrl: string;

        /**
         * set/catalog id
         */
        set: string;

        /**
         * metadata prefix
         */
        metadataPrefix: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.RemoveOaiImportsPath, 'delete');
        if (params) {
            rb.query('baseUrl', params.baseUrl, {});
            rb.query('set', params.set, {});
            rb.query('metadataPrefix', params.metadataPrefix, {});
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
     * Remove deleted imports.
     *
     * Remove deleted imports.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `removeOaiImports$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    removeOaiImports(params: {
        /**
         * base url
         */
        baseUrl: string;

        /**
         * set/catalog id
         */
        set: string;

        /**
         * metadata prefix
         */
        metadataPrefix: string;
    }): Observable<any> {
        return this.removeOaiImports$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation importOaiXml
     */
    static readonly ImportOaiXmlPath = '/admin/v1/import/oai/xml';

    /**
     * Import single xml via oai (for testing).
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `importOaiXml()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    importOaiXml$Response(params?: {
        /**
         * RecordHandler class name
         */
        recordHandlerClassName?: string;

        /**
         * BinaryHandler class name (may be empty for none)
         */
        binaryHandlerClassName?: string;
        body?: {
            xml?: {};
        };
    }): Observable<StrictHttpResponse<Node>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.ImportOaiXmlPath, 'post');
        if (params) {
            rb.query('recordHandlerClassName', params.recordHandlerClassName, {});
            rb.query('binaryHandlerClassName', params.binaryHandlerClassName, {});
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
                    return r as StrictHttpResponse<Node>;
                }),
            );
    }

    /**
     * Import single xml via oai (for testing).
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `importOaiXml$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    importOaiXml(params?: {
        /**
         * RecordHandler class name
         */
        recordHandlerClassName?: string;

        /**
         * BinaryHandler class name (may be empty for none)
         */
        binaryHandlerClassName?: string;
        body?: {
            xml?: {};
        };
    }): Observable<Node> {
        return this.importOaiXml$Response(params).pipe(
            map((r: StrictHttpResponse<Node>) => r.body as Node),
        );
    }

    /**
     * Path part for operation refreshAppInfo
     */
    static readonly RefreshAppInfoPath = '/admin/v1/refreshAppInfo';

    /**
     * refresh app info.
     *
     * Refresh the application info.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `refreshAppInfo()` instead.
     *
     * This method doesn't expect any request body.
     */
    refreshAppInfo$Response(params?: {}): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.RefreshAppInfoPath, 'post');
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
     * refresh app info.
     *
     * Refresh the application info.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `refreshAppInfo$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    refreshAppInfo(params?: {}): Observable<any> {
        return this.refreshAppInfo$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation refreshCache
     */
    static readonly RefreshCachePath = '/admin/v1/import/refreshCache/{folder}';

    /**
     * Refresh cache.
     *
     * Refresh importer cache.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `refreshCache()` instead.
     *
     * This method doesn't expect any request body.
     */
    refreshCache$Response(params: {
        /**
         * refresh cache root folder id
         */
        folder: string;

        /**
         * sticky
         */
        sticky: boolean;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.RefreshCachePath, 'post');
        if (params) {
            rb.path('folder', params.folder, {});
            rb.query('sticky', params.sticky, {});
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
     * Refresh cache.
     *
     * Refresh importer cache.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `refreshCache$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    refreshCache(params: {
        /**
         * refresh cache root folder id
         */
        folder: string;

        /**
         * sticky
         */
        sticky: boolean;
    }): Observable<any> {
        return this.refreshCache$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation refreshEduGroupCache
     */
    static readonly RefreshEduGroupCachePath = '/admin/v1/cache/refreshEduGroupCache';

    /**
     * Refresh the Edu Group Cache.
     *
     * Refresh the Edu Group Cache.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `refreshEduGroupCache()` instead.
     *
     * This method doesn't expect any request body.
     */
    refreshEduGroupCache$Response(params?: {
        /**
         * keep existing
         */
        keepExisting?: boolean;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            AdminV1Service.RefreshEduGroupCachePath,
            'post',
        );
        if (params) {
            rb.query('keepExisting', params.keepExisting, {});
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
     * Refresh the Edu Group Cache.
     *
     * Refresh the Edu Group Cache.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `refreshEduGroupCache$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    refreshEduGroupCache(params?: {
        /**
         * keep existing
         */
        keepExisting?: boolean;
    }): Observable<any> {
        return this.refreshEduGroupCache$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation removeApplication
     */
    static readonly RemoveApplicationPath = '/admin/v1/applications/{id}';

    /**
     * remove an application.
     *
     * remove the specified application.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `removeApplication()` instead.
     *
     * This method doesn't expect any request body.
     */
    removeApplication$Response(params: {
        /**
         * Application id
         */
        id: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.RemoveApplicationPath, 'delete');
        if (params) {
            rb.path('id', params.id, {});
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
     * remove an application.
     *
     * remove the specified application.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `removeApplication$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    removeApplication(params: {
        /**
         * Application id
         */
        id: string;
    }): Observable<any> {
        return this.removeApplication$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation removeCacheEntry
     */
    static readonly RemoveCacheEntryPath = '/admin/v1/cache/removeCacheEntry';

    /**
     * remove cache entry.
     *
     * remove cache entry
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `removeCacheEntry()` instead.
     *
     * This method doesn't expect any request body.
     */
    removeCacheEntry$Response(params?: {
        /**
         * cacheIndex
         */
        cacheIndex?: number;

        /**
         * bean
         */
        bean?: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.RemoveCacheEntryPath, 'post');
        if (params) {
            rb.query('cacheIndex', params.cacheIndex, {});
            rb.query('bean', params.bean, {});
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
     * remove cache entry.
     *
     * remove cache entry
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `removeCacheEntry$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    removeCacheEntry(params?: {
        /**
         * cacheIndex
         */
        cacheIndex?: number;

        /**
         * bean
         */
        bean?: string;
    }): Observable<any> {
        return this.removeCacheEntry$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation searchByElasticDsl
     */
    static readonly SearchByElasticDslPath = '/admin/v1/elastic';

    /**
     * Search for custom elastic DSL query.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `searchByElasticDsl()` instead.
     *
     * This method doesn't expect any request body.
     */
    searchByElasticDsl$Response(params?: {
        /**
         * dsl query (json encoded)
         */
        dsl?: string;
    }): Observable<StrictHttpResponse<SearchResultElastic>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.SearchByElasticDslPath, 'get');
        if (params) {
            rb.query('dsl', params.dsl, {});
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
                    return r as StrictHttpResponse<SearchResultElastic>;
                }),
            );
    }

    /**
     * Search for custom elastic DSL query.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `searchByElasticDsl$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    searchByElasticDsl(params?: {
        /**
         * dsl query (json encoded)
         */
        dsl?: string;
    }): Observable<SearchResultElastic> {
        return this.searchByElasticDsl$Response(params).pipe(
            map((r: StrictHttpResponse<SearchResultElastic>) => r.body as SearchResultElastic),
        );
    }

    /**
     * Path part for operation searchByLucene
     */
    static readonly SearchByLucenePath = '/admin/v1/lucene';

    /**
     * Search for custom lucene query.
     *
     * e.g. @cm\:name:"*"
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `searchByLucene()` instead.
     *
     * This method doesn't expect any request body.
     */
    searchByLucene$Response(params?: {
        /**
         * query
         */
        query?: string;

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

        /**
         * store, workspace or archive
         */
        store?: 'Workspace' | 'Archive';

        /**
         * authority scope to search for
         */
        authorityScope?: Array<string>;
    }): Observable<StrictHttpResponse<SearchResult>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.SearchByLucenePath, 'get');
        if (params) {
            rb.query('query', params.query, {});
            rb.query('maxItems', params.maxItems, {});
            rb.query('skipCount', params.skipCount, {});
            rb.query('sortProperties', params.sortProperties, {});
            rb.query('sortAscending', params.sortAscending, {});
            rb.query('propertyFilter', params.propertyFilter, {});
            rb.query('store', params.store, {});
            rb.query('authorityScope', params.authorityScope, {});
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
     * Search for custom lucene query.
     *
     * e.g. @cm\:name:"*"
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `searchByLucene$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    searchByLucene(params?: {
        /**
         * query
         */
        query?: string;

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

        /**
         * store, workspace or archive
         */
        store?: 'Workspace' | 'Archive';

        /**
         * authority scope to search for
         */
        authorityScope?: Array<string>;
    }): Observable<SearchResult> {
        return this.searchByLucene$Response(params).pipe(
            map((r: StrictHttpResponse<SearchResult>) => r.body as SearchResult),
        );
    }

    /**
     * Path part for operation serverUpdateList
     */
    static readonly ServerUpdateListPath = '/admin/v1/serverUpdate/list';

    /**
     * list available update tasks.
     *
     * list available update tasks
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `serverUpdateList()` instead.
     *
     * This method doesn't expect any request body.
     */
    serverUpdateList$Response(params?: {}): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.ServerUpdateListPath, 'get');
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
                    return r as StrictHttpResponse<string>;
                }),
            );
    }

    /**
     * list available update tasks.
     *
     * list available update tasks
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `serverUpdateList$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    serverUpdateList(params?: {}): Observable<string> {
        return this.serverUpdateList$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation serverUpdateList1
     */
    static readonly ServerUpdateList1Path = '/admin/v1/serverUpdate/run/{id}';

    /**
     * Run an update tasks.
     *
     * Run a specific update task (test or full update).
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `serverUpdateList1()` instead.
     *
     * This method doesn't expect any request body.
     */
    serverUpdateList1$Response(params: {
        /**
         * Id of the update task
         */
        id: string;

        /**
         * Actually execute (if false, just runs in test mode)
         */
        execute: boolean;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.ServerUpdateList1Path, 'post');
        if (params) {
            rb.path('id', params.id, {});
            rb.query('execute', params.execute, {});
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
     * Run an update tasks.
     *
     * Run a specific update task (test or full update).
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `serverUpdateList1$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    serverUpdateList1(params: {
        /**
         * Id of the update task
         */
        id: string;

        /**
         * Actually execute (if false, just runs in test mode)
         */
        execute: boolean;
    }): Observable<string> {
        return this.serverUpdateList1$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation startJob
     */
    static readonly StartJobPath = '/admin/v1/job/{jobClass}';

    /**
     * Start a Job.
     *
     * Start a Job.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `startJob()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    startJob$Response(params: {
        /**
         * jobClass
         */
        jobClass: string;

        /**
         * params
         */
        body: {
            [key: string]: {};
        };
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.StartJobPath, 'post');
        if (params) {
            rb.path('jobClass', params.jobClass, {});
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
     * Start a Job.
     *
     * Start a Job.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `startJob$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    startJob(params: {
        /**
         * jobClass
         */
        jobClass: string;

        /**
         * params
         */
        body: {
            [key: string]: {};
        };
    }): Observable<any> {
        return this.startJob$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation startJobSync
     */
    static readonly StartJobSyncPath = '/admin/v1/job/{jobClass}/sync';

    /**
     * Start a Job.
     *
     * Start a Job. Wait for the result synchronously
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `startJobSync()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    startJobSync$Response(params: {
        /**
         * jobClass
         */
        jobClass: string;

        /**
         * params
         */
        body: {
            [key: string]: {};
        };
    }): Observable<StrictHttpResponse<{}>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.StartJobSyncPath, 'post');
        if (params) {
            rb.path('jobClass', params.jobClass, {});
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
     * Start a Job.
     *
     * Start a Job. Wait for the result synchronously
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `startJobSync$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    startJobSync(params: {
        /**
         * jobClass
         */
        jobClass: string;

        /**
         * params
         */
        body: {
            [key: string]: {};
        };
    }): Observable<{}> {
        return this.startJobSync$Response(params).pipe(
            map((r: StrictHttpResponse<{}>) => r.body as {}),
        );
    }

    /**
     * Path part for operation switchAuthority
     */
    static readonly SwitchAuthorityPath = '/admin/v1/authenticate/{authorityName}';

    /**
     * switch the session to a known authority name.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `switchAuthority()` instead.
     *
     * This method doesn't expect any request body.
     */
    switchAuthority$Response(params: {
        /**
         * the authority to use (must be a person)
         */
        authorityName: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.SwitchAuthorityPath, 'post');
        if (params) {
            rb.path('authorityName', params.authorityName, {});
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
     * switch the session to a known authority name.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `switchAuthority$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    switchAuthority(params: {
        /**
         * the authority to use (must be a person)
         */
        authorityName: string;
    }): Observable<any> {
        return this.switchAuthority$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation testMail
     */
    static readonly TestMailPath = '/admin/v1/mail/{receiver}/{template}';

    /**
     * Test a mail template.
     *
     * Sends the given template as a test to the given receiver.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `testMail()` instead.
     *
     * This method doesn't expect any request body.
     */
    testMail$Response(params: {
        receiver: string;
        template: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.TestMailPath, 'post');
        if (params) {
            rb.path('receiver', params.receiver, {});
            rb.path('template', params.template, {});
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
     * Test a mail template.
     *
     * Sends the given template as a test to the given receiver.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `testMail$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    testMail(params: { receiver: string; template: string }): Observable<any> {
        return this.testMail$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation uploadTemp
     */
    static readonly UploadTempPath = '/admin/v1/upload/temp/{name}';

    /**
     * Upload a file.
     *
     * Upload a file to tomcat temp directory, to use it on the server (e.g. an update)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `uploadTemp()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    uploadTemp$Response(params: {
        /**
         * filename
         */
        name: string;
        body?: {
            /**
             * file to upload
             */
            file: {};
        };
    }): Observable<StrictHttpResponse<UploadResult>> {
        const rb = new RequestBuilder(this.rootUrl, AdminV1Service.UploadTempPath, 'put');
        if (params) {
            rb.path('name', params.name, {});
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
                    return r as StrictHttpResponse<UploadResult>;
                }),
            );
    }

    /**
     * Upload a file.
     *
     * Upload a file to tomcat temp directory, to use it on the server (e.g. an update)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `uploadTemp$Response()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    uploadTemp(params: {
        /**
         * filename
         */
        name: string;
        body?: {
            /**
             * file to upload
             */
            file: {};
        };
    }): Observable<UploadResult> {
        return this.uploadTemp$Response(params).pipe(
            map((r: StrictHttpResponse<UploadResult>) => r.body as UploadResult),
        );
    }
}
