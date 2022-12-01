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

import { AuthorityEntries } from '../models/authority-entries';
import { Group } from '../models/group';
import { GroupEntries } from '../models/group-entries';
import { GroupEntry } from '../models/group-entry';
import { GroupProfile } from '../models/group-profile';
import { GroupSignupDetails } from '../models/group-signup-details';
import { NodeEntries } from '../models/node-entries';
import { Preferences } from '../models/preferences';
import { ProfileSettings } from '../models/profile-settings';
import { User } from '../models/user';
import { UserCredential } from '../models/user-credential';
import { UserEntries } from '../models/user-entries';
import { UserEntry } from '../models/user-entry';
import { UserProfileEdit } from '../models/user-profile-edit';
import { UserStats } from '../models/user-stats';

@Injectable({
    providedIn: 'root',
})
export class IamV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation addMembership
     */
    static readonly AddMembershipPath = '/iam/v1/groups/{repository}/{group}/members/{member}';

    /**
     * Add member to the group.
     *
     * Add member to the group. (admin rights are required.)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `addMembership()` instead.
     *
     * This method doesn't expect any request body.
     */
    addMembership$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * groupname
         */
        group: string;

        /**
         * authorityName of member
         */
        member: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.AddMembershipPath, 'put');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('group', params.group, {});
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
     * Add member to the group.
     *
     * Add member to the group. (admin rights are required.)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `addMembership$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    addMembership(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * groupname
         */
        group: string;

        /**
         * authorityName of member
         */
        member: string;
    }): Observable<any> {
        return this.addMembership$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation deleteMembership
     */
    static readonly DeleteMembershipPath = '/iam/v1/groups/{repository}/{group}/members/{member}';

    /**
     * Delete member from the group.
     *
     * Delete member from the group. (admin rights are required.)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `deleteMembership()` instead.
     *
     * This method doesn't expect any request body.
     */
    deleteMembership$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * groupname
         */
        group: string;

        /**
         * authorityName of member
         */
        member: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.DeleteMembershipPath, 'delete');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('group', params.group, {});
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
     * Delete member from the group.
     *
     * Delete member from the group. (admin rights are required.)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `deleteMembership$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    deleteMembership(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * groupname
         */
        group: string;

        /**
         * authorityName of member
         */
        member: string;
    }): Observable<any> {
        return this.deleteMembership$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation addNodeList
     */
    static readonly AddNodeListPath = '/iam/v1/people/{repository}/{person}/nodeList/{list}/{node}';

    /**
     * Add a node to node a list of a user.
     *
     * For guest users, the list will be temporary stored in the current session
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `addNodeList()` instead.
     *
     * This method doesn't expect any request body.
     */
    addNodeList$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;

        /**
         * list name. If this list does not exist, it will be created
         */
        list: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.AddNodeListPath, 'put');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('person', params.person, {});
            rb.path('list', params.list, {});
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
     * Add a node to node a list of a user.
     *
     * For guest users, the list will be temporary stored in the current session
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `addNodeList$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    addNodeList(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;

        /**
         * list name. If this list does not exist, it will be created
         */
        list: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<any> {
        return this.addNodeList$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation removeNodeList
     */
    static readonly RemoveNodeListPath =
        '/iam/v1/people/{repository}/{person}/nodeList/{list}/{node}';

    /**
     * Delete a node of a node list of a user.
     *
     * For guest users, the list will be temporary stored in the current session
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `removeNodeList()` instead.
     *
     * This method doesn't expect any request body.
     */
    removeNodeList$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;

        /**
         * list name
         */
        list: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.RemoveNodeListPath, 'delete');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('person', params.person, {});
            rb.path('list', params.list, {});
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
     * Delete a node of a node list of a user.
     *
     * For guest users, the list will be temporary stored in the current session
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `removeNodeList$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    removeNodeList(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;

        /**
         * list name
         */
        list: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<any> {
        return this.removeNodeList$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation changeGroupProfile
     */
    static readonly ChangeGroupProfilePath = '/iam/v1/groups/{repository}/{group}/profile';

    /**
     * Set profile of the group.
     *
     * Set profile of the group. (admin rights are required.)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `changeGroupProfile()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    changeGroupProfile$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * groupname
         */
        group: string;

        /**
         * properties
         */
        body: GroupProfile;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.ChangeGroupProfilePath, 'put');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('group', params.group, {});
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
     * Set profile of the group.
     *
     * Set profile of the group. (admin rights are required.)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `changeGroupProfile$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    changeGroupProfile(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * groupname
         */
        group: string;

        /**
         * properties
         */
        body: GroupProfile;
    }): Observable<any> {
        return this.changeGroupProfile$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation changeUserAvatar
     */
    static readonly ChangeUserAvatarPath = '/iam/v1/people/{repository}/{person}/avatar';

    /**
     * Set avatar of the user.
     *
     * Set avatar of the user. (To set foreign avatars, admin rights are required.)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `changeUserAvatar()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    changeUserAvatar$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;
        body?: {
            /**
             * avatar image
             */
            avatar: {};
        };
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.ChangeUserAvatarPath, 'put');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('person', params.person, {});
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
                    return r as StrictHttpResponse<any>;
                }),
            );
    }

    /**
     * Set avatar of the user.
     *
     * Set avatar of the user. (To set foreign avatars, admin rights are required.)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `changeUserAvatar$Response()` instead.
     *
     * This method sends `multipart/form-data` and handles request body of type `multipart/form-data`.
     */
    changeUserAvatar(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;
        body?: {
            /**
             * avatar image
             */
            avatar: {};
        };
    }): Observable<any> {
        return this.changeUserAvatar$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation removeUserAvatar
     */
    static readonly RemoveUserAvatarPath = '/iam/v1/people/{repository}/{person}/avatar';

    /**
     * Remove avatar of the user.
     *
     * Remove avatar of the user. (To Remove foreign avatars, admin rights are required.)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `removeUserAvatar()` instead.
     *
     * This method doesn't expect any request body.
     */
    removeUserAvatar$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.RemoveUserAvatarPath, 'delete');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('person', params.person, {});
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
     * Remove avatar of the user.
     *
     * Remove avatar of the user. (To Remove foreign avatars, admin rights are required.)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `removeUserAvatar$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    removeUserAvatar(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;
    }): Observable<any> {
        return this.removeUserAvatar$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation changeUserPassword
     */
    static readonly ChangeUserPasswordPath = '/iam/v1/people/{repository}/{person}/credential';

    /**
     * Change/Set password of the user.
     *
     * Change/Set password of the user. (To change foreign passwords or set passwords, admin rights are required.)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `changeUserPassword()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    changeUserPassword$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;

        /**
         * credential
         */
        body: UserCredential;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.ChangeUserPasswordPath, 'put');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('person', params.person, {});
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
     * Change/Set password of the user.
     *
     * Change/Set password of the user. (To change foreign passwords or set passwords, admin rights are required.)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `changeUserPassword$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    changeUserPassword(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;

        /**
         * credential
         */
        body: UserCredential;
    }): Observable<any> {
        return this.changeUserPassword$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation changeUserProfile
     */
    static readonly ChangeUserProfilePath = '/iam/v1/people/{repository}/{person}/profile';

    /**
     * Set profile of the user.
     *
     * Set profile of the user. (To set foreign profiles, admin rights are required.)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `changeUserProfile()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    changeUserProfile$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;

        /**
         * properties
         */
        body: UserProfileEdit;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.ChangeUserProfilePath, 'put');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('person', params.person, {});
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
     * Set profile of the user.
     *
     * Set profile of the user. (To set foreign profiles, admin rights are required.)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `changeUserProfile$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    changeUserProfile(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;

        /**
         * properties
         */
        body: UserProfileEdit;
    }): Observable<any> {
        return this.changeUserProfile$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation confirmSignup
     */
    static readonly ConfirmSignupPath = '/iam/v1/groups/{repository}/{group}/signup/list/{user}';

    /**
     * put the pending user into the group.
     *
     * Requires admin rights or org administrator on this group
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `confirmSignup()` instead.
     *
     * This method doesn't expect any request body.
     */
    confirmSignup$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of group
         */
        group: string;

        /**
         * ID of user
         */
        user: string;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.ConfirmSignupPath, 'put');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('group', params.group, {});
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
     * put the pending user into the group.
     *
     * Requires admin rights or org administrator on this group
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `confirmSignup$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    confirmSignup(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of group
         */
        group: string;

        /**
         * ID of user
         */
        user: string;
    }): Observable<string> {
        return this.confirmSignup$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation rejectSignup
     */
    static readonly RejectSignupPath = '/iam/v1/groups/{repository}/{group}/signup/list/{user}';

    /**
     * reject the pending user.
     *
     * Requires admin rights or org administrator on this group
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `rejectSignup()` instead.
     *
     * This method doesn't expect any request body.
     */
    rejectSignup$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of group
         */
        group: string;

        /**
         * ID of user
         */
        user: string;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.RejectSignupPath, 'delete');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('group', params.group, {});
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
     * reject the pending user.
     *
     * Requires admin rights or org administrator on this group
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `rejectSignup$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    rejectSignup(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of group
         */
        group: string;

        /**
         * ID of user
         */
        user: string;
    }): Observable<string> {
        return this.rejectSignup$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation getGroup
     */
    static readonly GetGroupPath = '/iam/v1/groups/{repository}/{group}';

    /**
     * Get the group.
     *
     * Get the group. (To get foreign profiles, admin rights are required.)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getGroup()` instead.
     *
     * This method doesn't expect any request body.
     */
    getGroup$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * groupname
         */
        group: string;
    }): Observable<StrictHttpResponse<GroupEntry>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.GetGroupPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
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
                    return r as StrictHttpResponse<GroupEntry>;
                }),
            );
    }

    /**
     * Get the group.
     *
     * Get the group. (To get foreign profiles, admin rights are required.)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getGroup$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getGroup(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * groupname
         */
        group: string;
    }): Observable<GroupEntry> {
        return this.getGroup$Response(params).pipe(
            map((r: StrictHttpResponse<GroupEntry>) => r.body as GroupEntry),
        );
    }

    /**
     * Path part for operation createGroup
     */
    static readonly CreateGroupPath = '/iam/v1/groups/{repository}/{group}';

    /**
     * Create a new group.
     *
     * Create a new group. (admin rights are required.)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `createGroup()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    createGroup$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * groupname
         */
        group: string;

        /**
         * parent (will be added to this parent, also for name hashing), may be null
         */
        parent?: string;

        /**
         * properties
         */
        body: GroupProfile;
    }): Observable<StrictHttpResponse<Group>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.CreateGroupPath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('group', params.group, {});
            rb.query('parent', params.parent, {});
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
                    return r as StrictHttpResponse<Group>;
                }),
            );
    }

    /**
     * Create a new group.
     *
     * Create a new group. (admin rights are required.)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `createGroup$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    createGroup(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * groupname
         */
        group: string;

        /**
         * parent (will be added to this parent, also for name hashing), may be null
         */
        parent?: string;

        /**
         * properties
         */
        body: GroupProfile;
    }): Observable<Group> {
        return this.createGroup$Response(params).pipe(
            map((r: StrictHttpResponse<Group>) => r.body as Group),
        );
    }

    /**
     * Path part for operation deleteGroup
     */
    static readonly DeleteGroupPath = '/iam/v1/groups/{repository}/{group}';

    /**
     * Delete the group.
     *
     * Delete the group. (admin rights are required.)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `deleteGroup()` instead.
     *
     * This method doesn't expect any request body.
     */
    deleteGroup$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * groupname
         */
        group: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.DeleteGroupPath, 'delete');
        if (params) {
            rb.path('repository', params.repository, {});
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
                    return r as StrictHttpResponse<any>;
                }),
            );
    }

    /**
     * Delete the group.
     *
     * Delete the group. (admin rights are required.)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `deleteGroup$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    deleteGroup(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * groupname
         */
        group: string;
    }): Observable<any> {
        return this.deleteGroup$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation getUser
     */
    static readonly GetUserPath = '/iam/v1/people/{repository}/{person}';

    /**
     * Get the user.
     *
     * Get the user. (Not all information are feteched for foreign profiles if current user is not an admin)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getUser()` instead.
     *
     * This method doesn't expect any request body.
     */
    getUser$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;
    }): Observable<StrictHttpResponse<UserEntry>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.GetUserPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('person', params.person, {});
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
                    return r as StrictHttpResponse<UserEntry>;
                }),
            );
    }

    /**
     * Get the user.
     *
     * Get the user. (Not all information are feteched for foreign profiles if current user is not an admin)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getUser$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getUser(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;
    }): Observable<UserEntry> {
        return this.getUser$Response(params).pipe(
            map((r: StrictHttpResponse<UserEntry>) => r.body as UserEntry),
        );
    }

    /**
     * Path part for operation createUser
     */
    static readonly CreateUserPath = '/iam/v1/people/{repository}/{person}';

    /**
     * Create a new user.
     *
     * Create a new user. (admin rights are required.)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `createUser()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    createUser$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username
         */
        person: string;

        /**
         * Password, leave empty if you don&#x27;t want to set any
         */
        password?: string;

        /**
         * profile
         */
        body: UserProfileEdit;
    }): Observable<StrictHttpResponse<User>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.CreateUserPath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('person', params.person, {});
            rb.query('password', params.password, {});
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
                    return r as StrictHttpResponse<User>;
                }),
            );
    }

    /**
     * Create a new user.
     *
     * Create a new user. (admin rights are required.)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `createUser$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    createUser(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username
         */
        person: string;

        /**
         * Password, leave empty if you don&#x27;t want to set any
         */
        password?: string;

        /**
         * profile
         */
        body: UserProfileEdit;
    }): Observable<User> {
        return this.createUser$Response(params).pipe(
            map((r: StrictHttpResponse<User>) => r.body as User),
        );
    }

    /**
     * Path part for operation deleteUser
     */
    static readonly DeleteUserPath = '/iam/v1/people/{repository}/{person}';

    /**
     * Delete the user.
     *
     * Delete the user. (admin rights are required.)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `deleteUser()` instead.
     *
     * This method doesn't expect any request body.
     */
    deleteUser$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username
         */
        person: string;

        /**
         * force the deletion (if false then only persons which are previously marked for deletion are getting deleted)
         */
        force?: boolean;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.DeleteUserPath, 'delete');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('person', params.person, {});
            rb.query('force', params.force, {});
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
     * Delete the user.
     *
     * Delete the user. (admin rights are required.)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `deleteUser$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    deleteUser(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username
         */
        person: string;

        /**
         * force the deletion (if false then only persons which are previously marked for deletion are getting deleted)
         */
        force?: boolean;
    }): Observable<any> {
        return this.deleteUser$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation getMembership
     */
    static readonly GetMembershipPath = '/iam/v1/groups/{repository}/{group}/members';

    /**
     * Get all members of the group.
     *
     * Get all members of the group. (admin rights are required.)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getMembership()` instead.
     *
     * This method doesn't expect any request body.
     */
    getMembership$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * authority name (begins with GROUP_)
         */
        group: string;

        /**
         * pattern
         */
        pattern?: string;

        /**
         * authorityType either GROUP or USER, empty to show all
         */
        authorityType?: string;

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
    }): Observable<StrictHttpResponse<AuthorityEntries>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.GetMembershipPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('group', params.group, {});
            rb.query('pattern', params.pattern, {});
            rb.query('authorityType', params.authorityType, {});
            rb.query('maxItems', params.maxItems, {});
            rb.query('skipCount', params.skipCount, {});
            rb.query('sortProperties', params.sortProperties, {});
            rb.query('sortAscending', params.sortAscending, {});
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
                    return r as StrictHttpResponse<AuthorityEntries>;
                }),
            );
    }

    /**
     * Get all members of the group.
     *
     * Get all members of the group. (admin rights are required.)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getMembership$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getMembership(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * authority name (begins with GROUP_)
         */
        group: string;

        /**
         * pattern
         */
        pattern?: string;

        /**
         * authorityType either GROUP or USER, empty to show all
         */
        authorityType?: string;

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
    }): Observable<AuthorityEntries> {
        return this.getMembership$Response(params).pipe(
            map((r: StrictHttpResponse<AuthorityEntries>) => r.body as AuthorityEntries),
        );
    }

    /**
     * Path part for operation getNodeList
     */
    static readonly GetNodeListPath = '/iam/v1/people/{repository}/{person}/nodeList/{list}';

    /**
     * Get a specific node list for a user.
     *
     * For guest users, the list will be temporary stored in the current session
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getNodeList()` instead.
     *
     * This method doesn't expect any request body.
     */
    getNodeList$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;

        /**
         * list name
         */
        list: string;

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;

        /**
         * sort properties
         */
        sortProperties?: Array<string>;

        /**
         * sort ascending, true if not set. Use multiple values to change the direction according to the given property at the same index
         */
        sortAscending?: Array<boolean>;
    }): Observable<StrictHttpResponse<NodeEntries>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.GetNodeListPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('person', params.person, {});
            rb.path('list', params.list, {});
            rb.query('propertyFilter', params.propertyFilter, {});
            rb.query('sortProperties', params.sortProperties, {});
            rb.query('sortAscending', params.sortAscending, {});
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
     * Get a specific node list for a user.
     *
     * For guest users, the list will be temporary stored in the current session
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getNodeList$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getNodeList(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;

        /**
         * list name
         */
        list: string;

        /**
         * property filter for result nodes (or &quot;-all-&quot; for all properties)
         */
        propertyFilter?: Array<string>;

        /**
         * sort properties
         */
        sortProperties?: Array<string>;

        /**
         * sort ascending, true if not set. Use multiple values to change the direction according to the given property at the same index
         */
        sortAscending?: Array<boolean>;
    }): Observable<NodeEntries> {
        return this.getNodeList$Response(params).pipe(
            map((r: StrictHttpResponse<NodeEntries>) => r.body as NodeEntries),
        );
    }

    /**
     * Path part for operation getPreferences
     */
    static readonly GetPreferencesPath = '/iam/v1/people/{repository}/{person}/preferences';

    /**
     * Get preferences stored for user.
     *
     * Will fail for guest
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getPreferences()` instead.
     *
     * This method doesn't expect any request body.
     */
    getPreferences$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;
    }): Observable<StrictHttpResponse<Preferences>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.GetPreferencesPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('person', params.person, {});
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
                    return r as StrictHttpResponse<Preferences>;
                }),
            );
    }

    /**
     * Get preferences stored for user.
     *
     * Will fail for guest
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getPreferences$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getPreferences(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;
    }): Observable<Preferences> {
        return this.getPreferences$Response(params).pipe(
            map((r: StrictHttpResponse<Preferences>) => r.body as Preferences),
        );
    }

    /**
     * Path part for operation setPreferences
     */
    static readonly SetPreferencesPath = '/iam/v1/people/{repository}/{person}/preferences';

    /**
     * Set preferences for user.
     *
     * Will fail for guest
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `setPreferences()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    setPreferences$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;

        /**
         * preferences (json string)
         */
        body: string;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.SetPreferencesPath, 'put');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('person', params.person, {});
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
     * Set preferences for user.
     *
     * Will fail for guest
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `setPreferences$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    setPreferences(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;

        /**
         * preferences (json string)
         */
        body: string;
    }): Observable<any> {
        return this.setPreferences$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation getProfileSettings
     */
    static readonly GetProfileSettingsPath = '/iam/v1/people/{repository}/{person}/profileSettings';

    /**
     * Get profileSettings configuration.
     *
     * Will fail for guest
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getProfileSettings()` instead.
     *
     * This method doesn't expect any request body.
     */
    getProfileSettings$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;
    }): Observable<StrictHttpResponse<ProfileSettings>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.GetProfileSettingsPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('person', params.person, {});
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
                    return r as StrictHttpResponse<ProfileSettings>;
                }),
            );
    }

    /**
     * Get profileSettings configuration.
     *
     * Will fail for guest
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getProfileSettings$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getProfileSettings(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;
    }): Observable<ProfileSettings> {
        return this.getProfileSettings$Response(params).pipe(
            map((r: StrictHttpResponse<ProfileSettings>) => r.body as ProfileSettings),
        );
    }

    /**
     * Path part for operation setProfileSettings
     */
    static readonly SetProfileSettingsPath = '/iam/v1/people/{repository}/{person}/profileSettings';

    /**
     * Set profileSettings Configuration.
     *
     * Will fail for guest
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `setProfileSettings()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    setProfileSettings$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;

        /**
         * ProfileSetting Object
         */
        body: ProfileSettings;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.SetProfileSettingsPath, 'put');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('person', params.person, {});
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
     * Set profileSettings Configuration.
     *
     * Will fail for guest
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `setProfileSettings$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    setProfileSettings(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;

        /**
         * ProfileSetting Object
         */
        body: ProfileSettings;
    }): Observable<any> {
        return this.setProfileSettings$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation getRecentlyInvited
     */
    static readonly GetRecentlyInvitedPath = '/iam/v1/authorities/{repository}/recent';

    /**
     * Get recently invited authorities.
     *
     * Get the authorities the current user has recently invited.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getRecentlyInvited()` instead.
     *
     * This method doesn't expect any request body.
     */
    getRecentlyInvited$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;
    }): Observable<StrictHttpResponse<AuthorityEntries>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.GetRecentlyInvitedPath, 'get');
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
                    return r as StrictHttpResponse<AuthorityEntries>;
                }),
            );
    }

    /**
     * Get recently invited authorities.
     *
     * Get the authorities the current user has recently invited.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getRecentlyInvited$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getRecentlyInvited(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;
    }): Observable<AuthorityEntries> {
        return this.getRecentlyInvited$Response(params).pipe(
            map((r: StrictHttpResponse<AuthorityEntries>) => r.body as AuthorityEntries),
        );
    }

    /**
     * Path part for operation getSubgroupByType
     */
    static readonly GetSubgroupByTypePath = '/iam/v1/groups/{repository}/{group}/type/{type}';

    /**
     * Get a subgroup by the specified type.
     *
     * Get a subgroup by the specified type
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getSubgroupByType()` instead.
     *
     * This method doesn't expect any request body.
     */
    getSubgroupByType$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * authority name of the parent/primary group (begins with GROUP_)
         */
        group: string;

        /**
         * group type to filter for, e.g. ORG_ADMINISTRATORS
         */
        type: string;
    }): Observable<StrictHttpResponse<AuthorityEntries>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.GetSubgroupByTypePath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('group', params.group, {});
            rb.path('type', params.type, {});
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
                    return r as StrictHttpResponse<AuthorityEntries>;
                }),
            );
    }

    /**
     * Get a subgroup by the specified type.
     *
     * Get a subgroup by the specified type
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getSubgroupByType$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getSubgroupByType(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * authority name of the parent/primary group (begins with GROUP_)
         */
        group: string;

        /**
         * group type to filter for, e.g. ORG_ADMINISTRATORS
         */
        type: string;
    }): Observable<AuthorityEntries> {
        return this.getSubgroupByType$Response(params).pipe(
            map((r: StrictHttpResponse<AuthorityEntries>) => r.body as AuthorityEntries),
        );
    }

    /**
     * Path part for operation getUserGroups
     */
    static readonly GetUserGroupsPath = '/iam/v1/people/{repository}/{person}/memberships';

    /**
     * Get all groups the given user is member of.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getUserGroups()` instead.
     *
     * This method doesn't expect any request body.
     */
    getUserGroups$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * authority name
         */
        person: string;

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
    }): Observable<StrictHttpResponse<GroupEntries>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.GetUserGroupsPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('person', params.person, {});
            rb.query('pattern', params.pattern, {});
            rb.query('maxItems', params.maxItems, {});
            rb.query('skipCount', params.skipCount, {});
            rb.query('sortProperties', params.sortProperties, {});
            rb.query('sortAscending', params.sortAscending, {});
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
                    return r as StrictHttpResponse<GroupEntries>;
                }),
            );
    }

    /**
     * Get all groups the given user is member of.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getUserGroups$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getUserGroups(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * authority name
         */
        person: string;

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
    }): Observable<GroupEntries> {
        return this.getUserGroups$Response(params).pipe(
            map((r: StrictHttpResponse<GroupEntries>) => r.body as GroupEntries),
        );
    }

    /**
     * Path part for operation getUserStats
     */
    static readonly GetUserStatsPath = '/iam/v1/people/{repository}/{person}/stats';

    /**
     * Get the user stats.
     *
     * Get the user stats (e.g. publicly created material count)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getUserStats()` instead.
     *
     * This method doesn't expect any request body.
     */
    getUserStats$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;
    }): Observable<StrictHttpResponse<UserStats>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.GetUserStatsPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('person', params.person, {});
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
                    return r as StrictHttpResponse<UserStats>;
                }),
            );
    }

    /**
     * Get the user stats.
     *
     * Get the user stats (e.g. publicly created material count)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getUserStats$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getUserStats(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username (or &quot;-me-&quot; for current user)
         */
        person: string;
    }): Observable<UserStats> {
        return this.getUserStats$Response(params).pipe(
            map((r: StrictHttpResponse<UserStats>) => r.body as UserStats),
        );
    }

    /**
     * Path part for operation searchAuthorities
     */
    static readonly SearchAuthoritiesPath = '/iam/v1/authorities/{repository}';

    /**
     * Search authorities.
     *
     * Search authorities.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `searchAuthorities()` instead.
     *
     * This method doesn't expect any request body.
     */
    searchAuthorities$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * pattern
         */
        pattern: string;

        /**
         * global search context, defaults to true, otherwise just searches for users within the organizations
         */
        global?: boolean;

        /**
         * find a specific groupType (does nothing for persons)
         */
        groupType?: string;

        /**
         * find a specific signupMethod for groups (or asterisk for all including one) (does nothing for persons)
         */
        signupMethod?: string;

        /**
         * maximum items per page
         */
        maxItems?: number;

        /**
         * skip a number of items
         */
        skipCount?: number;
    }): Observable<StrictHttpResponse<AuthorityEntries>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.SearchAuthoritiesPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.query('pattern', params.pattern, {});
            rb.query('global', params.global, {});
            rb.query('groupType', params.groupType, {});
            rb.query('signupMethod', params.signupMethod, {});
            rb.query('maxItems', params.maxItems, {});
            rb.query('skipCount', params.skipCount, {});
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
                    return r as StrictHttpResponse<AuthorityEntries>;
                }),
            );
    }

    /**
     * Search authorities.
     *
     * Search authorities.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `searchAuthorities$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    searchAuthorities(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * pattern
         */
        pattern: string;

        /**
         * global search context, defaults to true, otherwise just searches for users within the organizations
         */
        global?: boolean;

        /**
         * find a specific groupType (does nothing for persons)
         */
        groupType?: string;

        /**
         * find a specific signupMethod for groups (or asterisk for all including one) (does nothing for persons)
         */
        signupMethod?: string;

        /**
         * maximum items per page
         */
        maxItems?: number;

        /**
         * skip a number of items
         */
        skipCount?: number;
    }): Observable<AuthorityEntries> {
        return this.searchAuthorities$Response(params).pipe(
            map((r: StrictHttpResponse<AuthorityEntries>) => r.body as AuthorityEntries),
        );
    }

    /**
     * Path part for operation searchGroups
     */
    static readonly SearchGroupsPath = '/iam/v1/groups/{repository}';

    /**
     * Search groups.
     *
     * Search groups. (admin rights are required.)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `searchGroups()` instead.
     *
     * This method doesn't expect any request body.
     */
    searchGroups$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * pattern
         */
        pattern: string;

        /**
         * find a specific groupType
         */
        groupType?: string;

        /**
         * find a specific signupMethod for groups (or asterisk for all including one)
         */
        signupMethod?: string;

        /**
         * global search context, defaults to true, otherwise just searches for groups within the organizations
         */
        global?: boolean;

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
    }): Observable<StrictHttpResponse<GroupEntries>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.SearchGroupsPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.query('pattern', params.pattern, {});
            rb.query('groupType', params.groupType, {});
            rb.query('signupMethod', params.signupMethod, {});
            rb.query('global', params.global, {});
            rb.query('maxItems', params.maxItems, {});
            rb.query('skipCount', params.skipCount, {});
            rb.query('sortProperties', params.sortProperties, {});
            rb.query('sortAscending', params.sortAscending, {});
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
                    return r as StrictHttpResponse<GroupEntries>;
                }),
            );
    }

    /**
     * Search groups.
     *
     * Search groups. (admin rights are required.)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `searchGroups$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    searchGroups(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * pattern
         */
        pattern: string;

        /**
         * find a specific groupType
         */
        groupType?: string;

        /**
         * find a specific signupMethod for groups (or asterisk for all including one)
         */
        signupMethod?: string;

        /**
         * global search context, defaults to true, otherwise just searches for groups within the organizations
         */
        global?: boolean;

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
    }): Observable<GroupEntries> {
        return this.searchGroups$Response(params).pipe(
            map((r: StrictHttpResponse<GroupEntries>) => r.body as GroupEntries),
        );
    }

    /**
     * Path part for operation searchUser
     */
    static readonly SearchUserPath = '/iam/v1/people/{repository}';

    /**
     * Search users.
     *
     * Search users. (admin rights are required.)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `searchUser()` instead.
     *
     * This method doesn't expect any request body.
     */
    searchUser$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * pattern
         */
        pattern: string;

        /**
         * global search context, defaults to true, otherwise just searches for users within the organizations
         */
        global?: boolean;

        /**
         * the user status (e.g. active), if not set, all users are returned
         */
        status?: 'active' | 'blocked' | 'todelete';

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
    }): Observable<StrictHttpResponse<UserEntries>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.SearchUserPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.query('pattern', params.pattern, {});
            rb.query('global', params.global, {});
            rb.query('status', params.status, {});
            rb.query('maxItems', params.maxItems, {});
            rb.query('skipCount', params.skipCount, {});
            rb.query('sortProperties', params.sortProperties, {});
            rb.query('sortAscending', params.sortAscending, {});
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
                    return r as StrictHttpResponse<UserEntries>;
                }),
            );
    }

    /**
     * Search users.
     *
     * Search users. (admin rights are required.)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `searchUser$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    searchUser(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * pattern
         */
        pattern: string;

        /**
         * global search context, defaults to true, otherwise just searches for users within the organizations
         */
        global?: boolean;

        /**
         * the user status (e.g. active), if not set, all users are returned
         */
        status?: 'active' | 'blocked' | 'todelete';

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
    }): Observable<UserEntries> {
        return this.searchUser$Response(params).pipe(
            map((r: StrictHttpResponse<UserEntries>) => r.body as UserEntries),
        );
    }

    /**
     * Path part for operation signupGroup
     */
    static readonly SignupGroupPath = '/iam/v1/groups/{repository}/{group}/signup';

    /**
     * let the current user signup to the given group.
     *
     *
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `signupGroup()` instead.
     *
     * This method doesn't expect any request body.
     */
    signupGroup$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of group
         */
        group: string;

        /**
         * Password for signup (only required if signupMethod &#x3D;&#x3D; password)
         */
        password?: string;
    }): Observable<
        StrictHttpResponse<'InvalidPassword' | 'AlreadyInList' | 'AlreadyMember' | 'Ok'>
    > {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.SignupGroupPath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('group', params.group, {});
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
                    return r as StrictHttpResponse<
                        'InvalidPassword' | 'AlreadyInList' | 'AlreadyMember' | 'Ok'
                    >;
                }),
            );
    }

    /**
     * let the current user signup to the given group.
     *
     *
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `signupGroup$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    signupGroup(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of group
         */
        group: string;

        /**
         * Password for signup (only required if signupMethod &#x3D;&#x3D; password)
         */
        password?: string;
    }): Observable<'InvalidPassword' | 'AlreadyInList' | 'AlreadyMember' | 'Ok'> {
        return this.signupGroup$Response(params).pipe(
            map(
                (
                    r: StrictHttpResponse<
                        'InvalidPassword' | 'AlreadyInList' | 'AlreadyMember' | 'Ok'
                    >,
                ) => r.body as 'InvalidPassword' | 'AlreadyInList' | 'AlreadyMember' | 'Ok',
            ),
        );
    }

    /**
     * Path part for operation signupGroupDetails
     */
    static readonly SignupGroupDetailsPath = '/iam/v1/groups/{repository}/{group}/signup/config';

    /**
     * requires admin rights.
     *
     * set group signup options
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `signupGroupDetails()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    signupGroupDetails$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of group
         */
        group: string;

        /**
         * Details to edit
         */
        body: GroupSignupDetails;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.SignupGroupDetailsPath, 'post');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('group', params.group, {});
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
     * requires admin rights.
     *
     * set group signup options
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `signupGroupDetails$Response()` instead.
     *
     * This method sends `application/json` and handles request body of type `application/json`.
     */
    signupGroupDetails(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of group
         */
        group: string;

        /**
         * Details to edit
         */
        body: GroupSignupDetails;
    }): Observable<any> {
        return this.signupGroupDetails$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }

    /**
     * Path part for operation signupGroupList
     */
    static readonly SignupGroupListPath = '/iam/v1/groups/{repository}/{group}/signup/list';

    /**
     * list pending users that want to join this group.
     *
     * Requires admin rights or org administrator on this group
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `signupGroupList()` instead.
     *
     * This method doesn't expect any request body.
     */
    signupGroupList$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of group
         */
        group: string;
    }): Observable<StrictHttpResponse<string>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.SignupGroupListPath, 'get');
        if (params) {
            rb.path('repository', params.repository, {});
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
     * list pending users that want to join this group.
     *
     * Requires admin rights or org administrator on this group
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `signupGroupList$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    signupGroupList(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of group
         */
        group: string;
    }): Observable<string> {
        return this.signupGroupList$Response(params).pipe(
            map((r: StrictHttpResponse<string>) => r.body as string),
        );
    }

    /**
     * Path part for operation updateUserStatus
     */
    static readonly UpdateUserStatusPath = '/iam/v1/people/{repository}/{person}/status/{status}';

    /**
     * update the user status.
     *
     * update the user status. (admin rights are required.)
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `updateUserStatus()` instead.
     *
     * This method doesn't expect any request body.
     */
    updateUserStatus$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username
         */
        person: string;

        /**
         * the new status to set
         */
        status: 'active' | 'blocked' | 'todelete';

        /**
         * notify the user via mail
         */
        notify: boolean;
    }): Observable<StrictHttpResponse<any>> {
        const rb = new RequestBuilder(this.rootUrl, IamV1Service.UpdateUserStatusPath, 'put');
        if (params) {
            rb.path('repository', params.repository, {});
            rb.path('person', params.person, {});
            rb.path('status', params.status, {});
            rb.query('notify', params.notify, {});
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
     * update the user status.
     *
     * update the user status. (admin rights are required.)
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `updateUserStatus$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    updateUserStatus(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * username
         */
        person: string;

        /**
         * the new status to set
         */
        status: 'active' | 'blocked' | 'todelete';

        /**
         * notify the user via mail
         */
        notify: boolean;
    }): Observable<any> {
        return this.updateUserStatus$Response(params).pipe(
            map((r: StrictHttpResponse<any>) => r.body as any),
        );
    }
}
