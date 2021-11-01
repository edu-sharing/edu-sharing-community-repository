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

import { Organization } from '../models/organization';
import { OrganizationEntries } from '../models/organization-entries';

@Injectable({
    providedIn: 'root',
})
export class OrganizationV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation getOrganization
     */
    static readonly GetOrganizationPath =
        '/organization/v1/organizations/{repository}/{organization}';

    /**
     * Get organization by id.
     *
     * Get organization by id.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getOrganization()` instead.
     *
     * This method doesn't expect any request body.
     */
    getOrganization$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of organization
         */
        organization: string;
    }): Observable<StrictHttpResponse<Organization>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            OrganizationV1Service.GetOrganizationPath,
            'get',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('organization', params.organization, {});
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
                    return r as StrictHttpResponse<Organization>;
                }),
            );
    }

    /**
     * Get organization by id.
     *
     * Get organization by id.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getOrganization$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getOrganization(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of organization
         */
        organization: string;
    }): Observable<Organization> {
        return this.getOrganization$Response(params).pipe(
            map((r: StrictHttpResponse<Organization>) => r.body as Organization),
        );
    }

    /**
     * Path part for operation createOrganizations
     */
    static readonly CreateOrganizationsPath =
        '/organization/v1/organizations/{repository}/{organization}';

    /**
     * create organization in repository.
     *
     * create organization in repository.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `createOrganizations()` instead.
     *
     * This method doesn't expect any request body.
     */
    createOrganizations$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * organization name
         */
        organization: string;

        /**
         * eduscope (may be null)
         */
        eduscope?: string;
    }): Observable<StrictHttpResponse<Organization>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            OrganizationV1Service.CreateOrganizationsPath,
            'put',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('organization', params.organization, {});
            rb.query('eduscope', params.eduscope, {});
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
                    return r as StrictHttpResponse<Organization>;
                }),
            );
    }

    /**
     * create organization in repository.
     *
     * create organization in repository.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `createOrganizations$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    createOrganizations(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * organization name
         */
        organization: string;

        /**
         * eduscope (may be null)
         */
        eduscope?: string;
    }): Observable<Organization> {
        return this.createOrganizations$Response(params).pipe(
            map((r: StrictHttpResponse<Organization>) => r.body as Organization),
        );
    }

    /**
     * Path part for operation deleteOrganizations
     */
    static readonly DeleteOrganizationsPath =
        '/organization/v1/organizations/{repository}/{organization}';

    /**
     * Delete organization of repository.
     *
     * Delete organization of repository.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `deleteOrganizations()` instead.
     *
     * This method doesn't expect any request body.
     */
    deleteOrganizations$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * groupname
         */
        organization: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            OrganizationV1Service.DeleteOrganizationsPath,
            'delete',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('organization', params.organization, {});
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
     * Delete organization of repository.
     *
     * Delete organization of repository.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `deleteOrganizations$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    deleteOrganizations(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * groupname
         */
        organization: string;
    }): Observable<any> {
        return this.deleteOrganizations$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation getOrganizations
     */
    static readonly GetOrganizationsPath = '/organization/v1/organizations/{repository}';

    /**
     * Get organizations of repository.
     *
     * Get organizations of repository the current user is member. May returns an empty list.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getOrganizations()` instead.
     *
     * This method doesn't expect any request body.
     */
    getOrganizations$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * pattern
         */
        pattern?: string;

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
         * search only in memberships, false can only be done by admin
         */
        onlyMemberships?: boolean;
    }): Observable<StrictHttpResponse<OrganizationEntries>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            OrganizationV1Service.GetOrganizationsPath,
            'get',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.query('pattern', params.pattern, {});
            rb.query('maxItems', params.maxItems, {});
            rb.query('skipCount', params.skipCount, {});
            rb.query('sortProperties', params.sortProperties, {});
            rb.query('sortAscending', params.sortAscending, {});
            rb.query('onlyMemberships', params.onlyMemberships, {});
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
                    return r as StrictHttpResponse<OrganizationEntries>;
                }),
            );
    }

    /**
     * Get organizations of repository.
     *
     * Get organizations of repository the current user is member. May returns an empty list.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getOrganizations$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getOrganizations(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * pattern
         */
        pattern?: string;

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
         * search only in memberships, false can only be done by admin
         */
        onlyMemberships?: boolean;
    }): Observable<OrganizationEntries> {
        return this.getOrganizations$Response(params).pipe(
            map((r: StrictHttpResponse<OrganizationEntries>) => r.body as OrganizationEntries),
        );
    }

    /**
     * Path part for operation removeFromOrganization
     */
    static readonly RemoveFromOrganizationPath =
        '/organization/v1/organizations/{repository}/{organization}/member/{member}';

    /**
     * Remove member from organization.
     *
     * Remove member from organization.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `removeFromOrganization()` instead.
     *
     * This method doesn't expect any request body.
     */
    removeFromOrganization$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * groupname
         */
        organization: string;

        /**
         * authorityName of member
         */
        member: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            OrganizationV1Service.RemoveFromOrganizationPath,
            'delete',
        );
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('organization', params.organization, {});
            rb.path('member', params.member, {});
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
     * Remove member from organization.
     *
     * Remove member from organization.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `removeFromOrganization$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    removeFromOrganization(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * groupname
         */
        organization: string;

        /**
         * authorityName of member
         */
        member: string;
    }): Observable<any> {
        return this.removeFromOrganization$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }
}
