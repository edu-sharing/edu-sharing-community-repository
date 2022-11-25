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

import { McOrgConnectResult } from '../models/mc-org-connect-result';
import { Mediacenter } from '../models/mediacenter';
import { MediacentersImportResult } from '../models/mediacenters-import-result';
import { OrganisationsImportResult } from '../models/organisations-import-result';
import { Profile } from '../models/profile';
import { SearchParameters } from '../models/search-parameters';

@Injectable({
    providedIn: 'root',
})
export class MediacenterV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation addMediacenterGroup
     */
    static readonly AddMediacenterGroupPath =
        '/mediacenter/v1/mediacenter/{repository}/{mediacenter}/manages/{group}';

    /**
     * add a group that is managed by the given mediacenter.
     *
     * although not restricted, it is recommended that the group is an edu-sharing organization (admin rights are required)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `addMediacenterGroup()` instead.
     *
     * This method doesn't expect any request body.
     */
    addMediacenterGroup$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * authorityName of the mediacenter that should manage the group
         */
        mediacenter: string;

        /**
         * authorityName of the group that should be managed by that mediacenter
         */
        group: string;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            MediacenterV1Service.AddMediacenterGroupPath,
            'put',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('mediacenter', params.mediacenter, {});
            rb.path('group', params.group, {});
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
     * add a group that is managed by the given mediacenter.
     *
     * although not restricted, it is recommended that the group is an edu-sharing organization (admin rights are required)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `addMediacenterGroup$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    addMediacenterGroup(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * authorityName of the mediacenter that should manage the group
         */
        mediacenter: string;

        /**
         * authorityName of the group that should be managed by that mediacenter
         */
        group: string;
    }): Observable<string> {
        return this.addMediacenterGroup$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation removeMediacenterGroup
     */
    static readonly RemoveMediacenterGroupPath =
        '/mediacenter/v1/mediacenter/{repository}/{mediacenter}/manages/{group}';

    /**
     * delete a group that is managed by the given mediacenter.
     *
     * admin rights are required.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `removeMediacenterGroup()` instead.
     *
     * This method doesn't expect any request body.
     */
    removeMediacenterGroup$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * authorityName of the mediacenter that should manage the group
         */
        mediacenter: string;

        /**
         * authorityName of the group that should not longer be managed by that mediacenter
         */
        group: string;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            MediacenterV1Service.RemoveMediacenterGroupPath,
            'delete',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('mediacenter', params.mediacenter, {});
            rb.path('group', params.group, {});
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
     * delete a group that is managed by the given mediacenter.
     *
     * admin rights are required.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `removeMediacenterGroup$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    removeMediacenterGroup(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * authorityName of the mediacenter that should manage the group
         */
        mediacenter: string;

        /**
         * authorityName of the group that should not longer be managed by that mediacenter
         */
        group: string;
    }): Observable<string> {
        return this.removeMediacenterGroup$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation editMediacenter
     */
    static readonly EditMediacenterPath = '/mediacenter/v1/mediacenter/{repository}/{mediacenter}';

    /**
     * edit a mediacenter in repository.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `editMediacenter()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    editMediacenter$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * mediacenter name
         */
        mediacenter: string;
        body?: Profile;
    }): Observable<StrictHttpResponse<Mediacenter>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            MediacenterV1Service.EditMediacenterPath,
            'put',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('mediacenter', params.mediacenter, {});
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
                    return r as StrictHttpResponse<Mediacenter>;
                }),
            );
    }

    /**
     * edit a mediacenter in repository.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `editMediacenter$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    editMediacenter(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * mediacenter name
         */
        mediacenter: string;
        body?: Profile;
    }): Observable<Mediacenter> {
        return this.editMediacenter$Response(params).pipe(
            map((r: StrictHttpResponse<Mediacenter>) => r.body as Mediacenter),
        );
    }

    /**
     * Path part for operation createMediacenter
     */
    static readonly CreateMediacenterPath =
        '/mediacenter/v1/mediacenter/{repository}/{mediacenter}';

    /**
     * create new mediacenter in repository.
     *
     * admin rights are required.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `createMediacenter()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    createMediacenter$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * mediacenter name
         */
        mediacenter: string;
        body?: Profile;
    }): Observable<StrictHttpResponse<Mediacenter>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            MediacenterV1Service.CreateMediacenterPath,
            'post',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('mediacenter', params.mediacenter, {});
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
                    return r as StrictHttpResponse<Mediacenter>;
                }),
            );
    }

    /**
     * create new mediacenter in repository.
     *
     * admin rights are required.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `createMediacenter$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    createMediacenter(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * mediacenter name
         */
        mediacenter: string;
        body?: Profile;
    }): Observable<Mediacenter> {
        return this.createMediacenter$Response(params).pipe(
            map((r: StrictHttpResponse<Mediacenter>) => r.body as Mediacenter),
        );
    }

    /**
     * Path part for operation deleteMediacenter
     */
    static readonly DeleteMediacenterPath =
        '/mediacenter/v1/mediacenter/{repository}/{mediacenter}';

    /**
     * delete a mediacenter group and it's admin group and proxy group.
     *
     * admin rights are required.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `deleteMediacenter()` instead.
     *
     * This method doesn't expect any request body.
     */
    deleteMediacenter$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * authorityName of the mediacenter that should manage the group
         */
        mediacenter: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            MediacenterV1Service.DeleteMediacenterPath,
            'delete',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('mediacenter', params.mediacenter, {});
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
     * delete a mediacenter group and it's admin group and proxy group.
     *
     * admin rights are required.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `deleteMediacenter$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    deleteMediacenter(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * authorityName of the mediacenter that should manage the group
         */
        mediacenter: string;
    }): Observable<any> {
        return this.deleteMediacenter$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation getMediacenterGroups
     */
    static readonly GetMediacenterGroupsPath =
        '/mediacenter/v1/mediacenter/{repository}/{mediacenter}/manages';

    /**
     * get groups that are managed by the given mediacenter.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getMediacenterGroups()` instead.
     *
     * This method doesn't expect any request body.
     */
    getMediacenterGroups$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * authorityName of the mediacenter that should manage the group
         */
        mediacenter: string;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            MediacenterV1Service.GetMediacenterGroupsPath,
            'get',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('mediacenter', params.mediacenter, {});
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
     * get groups that are managed by the given mediacenter.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getMediacenterGroups$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getMediacenterGroups(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * authorityName of the mediacenter that should manage the group
         */
        mediacenter: string;
    }): Observable<string> {
        return this.getMediacenterGroups$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation getMediacenterLicensedNodes
     */
    static readonly GetMediacenterLicensedNodesPath =
        '/mediacenter/v1/mediacenter/{repository}/{mediacenter}/licenses';

    /**
     * get nodes that are licensed by the given mediacenter.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getMediacenterLicensedNodes()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    getMediacenterLicensedNodes$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

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
         * authorityName of the mediacenter that licenses nodes
         */
        mediacenter: string;

        /**
         * searchword of licensed nodes
         */
        searchword: string;

        /**
         * search parameters
         */
        body: SearchParameters;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            MediacenterV1Service.GetMediacenterLicensedNodesPath,
            'post',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.query('maxItems', params.maxItems, {});
            rb.query('skipCount', params.skipCount, {});
            rb.query('sortProperties', params.sortProperties, {});
            rb.query('sortAscending', params.sortAscending, {});
            rb.query('propertyFilter', params.propertyFilter, {});
            rb.path('mediacenter', params.mediacenter, {});
            rb.query('searchword', params.searchword, {});
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
     * get nodes that are licensed by the given mediacenter.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getMediacenterLicensedNodes$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    getMediacenterLicensedNodes(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

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
         * authorityName of the mediacenter that licenses nodes
         */
        mediacenter: string;

        /**
         * searchword of licensed nodes
         */
        searchword: string;

        /**
         * search parameters
         */
        body: SearchParameters;
    }): Observable<string> {
        return this.getMediacenterLicensedNodes$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation getMediacenters
     */
    static readonly GetMediacentersPath = '/mediacenter/v1/mediacenter/{repository}';

    /**
     * get mediacenters in the repository.
     *
     * Only shows the one available/managing the current user (only admin can access all)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getMediacenters()` instead.
     *
     * This method doesn't expect any request body.
     */
    getMediacenters$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            MediacenterV1Service.GetMediacentersPath,
            'get',
        );
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
                    return r as StrictHttpResponse<string>;
                }),
            );
    }

    /**
     * get mediacenters in the repository.
     *
     * Only shows the one available/managing the current user (only admin can access all)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getMediacenters$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getMediacenters(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;
    }): Observable<string> {
        return this.getMediacenters$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation importMcOrgConnections
     */
    static readonly ImportMcOrgConnectionsPath = '/mediacenter/v1/import/mc_org';

    /**
     * Import Mediacenter Organisation Connection.
     *
     * Import Mediacenter Organisation Connection.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `importMcOrgConnections()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    importMcOrgConnections$Response(params?: {
        /**
         * removeSchoolsFromMC
         */
        removeSchoolsFromMC?: boolean;
        body?: {
            /**
             * Mediacenter Organisation Connection csv to import
             */
            mcOrgs: {};
        };
    }): Observable<StrictHttpResponse<McOrgConnectResult>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            MediacenterV1Service.ImportMcOrgConnectionsPath,
            'post',
        );
        if (params) {
            rb.query('removeSchoolsFromMC', params.removeSchoolsFromMC, {});
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
                    return r as StrictHttpResponse<McOrgConnectResult>;
                }),
            );
    }

    /**
     * Import Mediacenter Organisation Connection.
     *
     * Import Mediacenter Organisation Connection.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `importMcOrgConnections$Response()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    importMcOrgConnections(params?: {
        /**
         * removeSchoolsFromMC
         */
        removeSchoolsFromMC?: boolean;
        body?: {
            /**
             * Mediacenter Organisation Connection csv to import
             */
            mcOrgs: {};
        };
    }): Observable<McOrgConnectResult> {
        return this.importMcOrgConnections$Response(params).pipe(
            map((r: StrictHttpResponse<McOrgConnectResult>) => r.body as McOrgConnectResult),
        );
    }

    /**
     * Path part for operation importMediacenters
     */
    static readonly ImportMediacentersPath = '/mediacenter/v1/import/mediacenters';

    /**
     * Import mediacenters.
     *
     * Import mediacenters.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `importMediacenters()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    importMediacenters$Response(params?: {
        body?: {
            /**
             * Mediacenters csv to import
             */
            mediacenters: {};
        };
    }): Observable<StrictHttpResponse<MediacentersImportResult>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            MediacenterV1Service.ImportMediacentersPath,
            'post',
        );
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
                    return r as StrictHttpResponse<MediacentersImportResult>;
                }),
            );
    }

    /**
     * Import mediacenters.
     *
     * Import mediacenters.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `importMediacenters$Response()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    importMediacenters(params?: {
        body?: {
            /**
             * Mediacenters csv to import
             */
            mediacenters: {};
        };
    }): Observable<MediacentersImportResult> {
        return this.importMediacenters$Response(params).pipe(
            map(
                (r: StrictHttpResponse<MediacentersImportResult>) =>
                    r.body as MediacentersImportResult,
            ),
        );
    }

    /**
     * Path part for operation importOrganisations
     */
    static readonly ImportOrganisationsPath = '/mediacenter/v1/import/organisations';

    /**
     * Import Organisations.
     *
     * Import Organisations.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `importOrganisations()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    importOrganisations$Response(params?: {
        body?: {
            /**
             * Organisations csv to import
             */
            organisations: {};
        };
    }): Observable<StrictHttpResponse<OrganisationsImportResult>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            MediacenterV1Service.ImportOrganisationsPath,
            'post',
        );
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
                    return r as StrictHttpResponse<OrganisationsImportResult>;
                }),
            );
    }

    /**
     * Import Organisations.
     *
     * Import Organisations.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `importOrganisations$Response()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    importOrganisations(params?: {
        body?: {
            /**
             * Organisations csv to import
             */
            organisations: {};
        };
    }): Observable<OrganisationsImportResult> {
        return this.importOrganisations$Response(params).pipe(
            map(
                (r: StrictHttpResponse<OrganisationsImportResult>) =>
                    r.body as OrganisationsImportResult,
            ),
        );
    }
}
