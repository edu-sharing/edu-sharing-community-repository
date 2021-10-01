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

import { Collection } from '../models/collection';
import { Usage } from '../models/usage';
import { Usages } from '../models/usages';

@Injectable({
  providedIn: 'root',
})
export class UsageV1Service extends BaseService {
  constructor(
    config: ApiConfiguration,
    http: HttpClient
  ) {
    super(config, http);
  }

  /**
   * Path part for operation getUsagesByCourse
   */
  static readonly GetUsagesByCoursePath = '/usage/v1/usages/course/{appId}/{courseId}';

  /**
   * Get all usages of an course.
   *
   * Get all usages of an course.
   *
   * This method provides access to the full `HttpResponse`, allowing access to response headers.
   * To access only the response body, use `getUsagesByCourse()` instead.
   *
   * This method doesn't expect any request body.
   */
  getUsagesByCourse$Response(params: {

    /**
     * ID of application (or &quot;-home-&quot; for home repository)
     */
    appId: string;

    /**
     * ID of course
     */
    courseId: string;
  }): Observable<StrictHttpResponse<Usages>> {

    const rb = new RequestBuilder(this.rootUrl, UsageV1Service.GetUsagesByCoursePath, 'get');
    if (params) {
      rb.path('appId', params.appId, {"style":"simple","explode":false});
      rb.path('courseId', params.courseId, {"style":"simple","explode":false});
    }

    return this.http.request(rb.build({
      responseType: 'json',
      accept: 'application/json'
    })).pipe(
      filter((r: any) => r instanceof HttpResponse),
      map((r: HttpResponse<any>) => {
        return r as StrictHttpResponse<Usages>;
      })
    );
  }

  /**
   * Get all usages of an course.
   *
   * Get all usages of an course.
   *
   * This method provides access to only to the response body.
   * To access the full response (for headers, for example), `getUsagesByCourse$Response()` instead.
   *
   * This method doesn't expect any request body.
   */
  getUsagesByCourse(params: {

    /**
     * ID of application (or &quot;-home-&quot; for home repository)
     */
    appId: string;

    /**
     * ID of course
     */
    courseId: string;
  }): Observable<Usages> {

    return this.getUsagesByCourse$Response(params).pipe(
      map((r: StrictHttpResponse<Usages>) => r.body as Usages)
    );
  }

  /**
   * Path part for operation getUsagesByNode
   */
  static readonly GetUsagesByNodePath = '/usage/v1/usages/node/{nodeId}';

  /**
   * Get all usages of an node.
   *
   * Get all usages of an node.
   *
   * This method provides access to the full `HttpResponse`, allowing access to response headers.
   * To access only the response body, use `getUsagesByNode()` instead.
   *
   * This method doesn't expect any request body.
   */
  getUsagesByNode$Response(params: {

    /**
     * ID of node
     */
    nodeId: string;
  }): Observable<StrictHttpResponse<Usages>> {

    const rb = new RequestBuilder(this.rootUrl, UsageV1Service.GetUsagesByNodePath, 'get');
    if (params) {
      rb.path('nodeId', params.nodeId, {"style":"simple","explode":false});
    }

    return this.http.request(rb.build({
      responseType: 'json',
      accept: 'application/json'
    })).pipe(
      filter((r: any) => r instanceof HttpResponse),
      map((r: HttpResponse<any>) => {
        return r as StrictHttpResponse<Usages>;
      })
    );
  }

  /**
   * Get all usages of an node.
   *
   * Get all usages of an node.
   *
   * This method provides access to only to the response body.
   * To access the full response (for headers, for example), `getUsagesByNode$Response()` instead.
   *
   * This method doesn't expect any request body.
   */
  getUsagesByNode(params: {

    /**
     * ID of node
     */
    nodeId: string;
  }): Observable<Usages> {

    return this.getUsagesByNode$Response(params).pipe(
      map((r: StrictHttpResponse<Usages>) => r.body as Usages)
    );
  }

  /**
   * Path part for operation getUsagesByNodeCollections
   */
  static readonly GetUsagesByNodeCollectionsPath = '/usage/v1/usages/node/{nodeId}/collections';

  /**
   * Get all collections where this node is used.
   *
   *
   *
   * This method provides access to the full `HttpResponse`, allowing access to response headers.
   * To access only the response body, use `getUsagesByNodeCollections()` instead.
   *
   * This method doesn't expect any request body.
   */
  getUsagesByNodeCollections$Response(params: {

    /**
     * ID of node
     */
    nodeId: string;
  }): Observable<StrictHttpResponse<Array<Collection>>> {

    const rb = new RequestBuilder(this.rootUrl, UsageV1Service.GetUsagesByNodeCollectionsPath, 'get');
    if (params) {
      rb.path('nodeId', params.nodeId, {"style":"simple","explode":false});
    }

    return this.http.request(rb.build({
      responseType: 'json',
      accept: 'application/json'
    })).pipe(
      filter((r: any) => r instanceof HttpResponse),
      map((r: HttpResponse<any>) => {
        return r as StrictHttpResponse<Array<Collection>>;
      })
    );
  }

  /**
   * Get all collections where this node is used.
   *
   *
   *
   * This method provides access to only to the response body.
   * To access the full response (for headers, for example), `getUsagesByNodeCollections$Response()` instead.
   *
   * This method doesn't expect any request body.
   */
  getUsagesByNodeCollections(params: {

    /**
     * ID of node
     */
    nodeId: string;
  }): Observable<Array<Collection>> {

    return this.getUsagesByNodeCollections$Response(params).pipe(
      map((r: StrictHttpResponse<Array<Collection>>) => r.body as Array<Collection>)
    );
  }

  /**
   * Path part for operation deleteUsage
   */
  static readonly DeleteUsagePath = '/usage/v1/usages/node/{nodeId}/{usageId}';

  /**
   * Delete an usage of a node.
   *
   *
   *
   * This method provides access to the full `HttpResponse`, allowing access to response headers.
   * To access only the response body, use `deleteUsage()` instead.
   *
   * This method doesn't expect any request body.
   */
  deleteUsage$Response(params: {

    /**
     * ID of node
     */
    nodeId: string;

    /**
     * ID of usage
     */
    usageId: string;
  }): Observable<StrictHttpResponse<Usages>> {

    const rb = new RequestBuilder(this.rootUrl, UsageV1Service.DeleteUsagePath, 'delete');
    if (params) {
      rb.path('nodeId', params.nodeId, {"style":"simple","explode":false});
      rb.path('usageId', params.usageId, {"style":"simple","explode":false});
    }

    return this.http.request(rb.build({
      responseType: 'json',
      accept: 'application/json'
    })).pipe(
      filter((r: any) => r instanceof HttpResponse),
      map((r: HttpResponse<any>) => {
        return r as StrictHttpResponse<Usages>;
      })
    );
  }

  /**
   * Delete an usage of a node.
   *
   *
   *
   * This method provides access to only to the response body.
   * To access the full response (for headers, for example), `deleteUsage$Response()` instead.
   *
   * This method doesn't expect any request body.
   */
  deleteUsage(params: {

    /**
     * ID of node
     */
    nodeId: string;

    /**
     * ID of usage
     */
    usageId: string;
  }): Observable<Usages> {

    return this.deleteUsage$Response(params).pipe(
      map((r: StrictHttpResponse<Usages>) => r.body as Usages)
    );
  }

  /**
   * Path part for operation setUsage
   */
  static readonly SetUsagePath = '/usage/v1/usages/repository/{repositoryId}';

  /**
   * Set a usage for a node. app signature headers and authenticated user required.
   *
   * headers must be set: X-Edu-App-Id, X-Edu-App-Sig, X-Edu-App-Signed, X-Edu-App-Ts
   *
   * This method provides access to the full `HttpResponse`, allowing access to response headers.
   * To access only the response body, use `setUsage()` instead.
   *
   * This method sends `application/json` and handles request body of type `application/json`.
   */
  setUsage$Response(params: {

    /**
     * ID of repository (or &quot;-home-&quot; for home repository)
     */
    repository: string;

    /**
     * usage date
     */
    body: Usage
  }): Observable<StrictHttpResponse<Usage>> {

    const rb = new RequestBuilder(this.rootUrl, UsageV1Service.SetUsagePath, 'post');
    if (params) {
      rb.path('repository', params.repository, {"style":"simple","explode":false});
      rb.body(params.body, 'application/json');
    }

    return this.http.request(rb.build({
      responseType: 'json',
      accept: 'application/json'
    })).pipe(
      filter((r: any) => r instanceof HttpResponse),
      map((r: HttpResponse<any>) => {
        return r as StrictHttpResponse<Usage>;
      })
    );
  }

  /**
   * Set a usage for a node. app signature headers and authenticated user required.
   *
   * headers must be set: X-Edu-App-Id, X-Edu-App-Sig, X-Edu-App-Signed, X-Edu-App-Ts
   *
   * This method provides access to only to the response body.
   * To access the full response (for headers, for example), `setUsage$Response()` instead.
   *
   * This method sends `application/json` and handles request body of type `application/json`.
   */
  setUsage(params: {

    /**
     * ID of repository (or &quot;-home-&quot; for home repository)
     */
    repository: string;

    /**
     * usage date
     */
    body: Usage
  }): Observable<Usage> {

    return this.setUsage$Response(params).pipe(
      map((r: StrictHttpResponse<Usage>) => r.body as Usage)
    );
  }

  /**
   * Path part for operation getUsages
   */
  static readonly GetUsagesPath = '/usage/v1/usages/repository/{repositoryId}/{nodeid}';

  /**
   * This method provides access to the full `HttpResponse`, allowing access to response headers.
   * To access only the response body, use `getUsages()` instead.
   *
   * This method doesn't expect any request body.
   */
  getUsages$Response(params: {

    /**
     * ID of repository
     */
    repositoryId: string;

    /**
     * ID of node. Use -all- for getting usages of all nodes
     */
    nodeId: string;

    /**
     * from date
     */
    from?: number;

    /**
     * to date
     */
    to?: number;
  }): Observable<StrictHttpResponse<void>> {

    const rb = new RequestBuilder(this.rootUrl, UsageV1Service.GetUsagesPath, 'get');
    if (params) {
      rb.path('repositoryId', params.repositoryId, {"style":"simple","explode":false});
      rb.path('nodeId', params.nodeId, {"style":"simple","explode":false});
      rb.query('from', params.from, {"style":"form","explode":true});
      rb.query('to', params.to, {"style":"form","explode":true});
    }

    return this.http.request(rb.build({
      responseType: 'text',
      accept: '*/*'
    })).pipe(
      filter((r: any) => r instanceof HttpResponse),
      map((r: HttpResponse<any>) => {
        return (r as HttpResponse<any>).clone({ body: undefined }) as StrictHttpResponse<void>;
      })
    );
  }

  /**
   * This method provides access to only to the response body.
   * To access the full response (for headers, for example), `getUsages$Response()` instead.
   *
   * This method doesn't expect any request body.
   */
  getUsages(params: {

    /**
     * ID of repository
     */
    repositoryId: string;

    /**
     * ID of node. Use -all- for getting usages of all nodes
     */
    nodeId: string;

    /**
     * from date
     */
    from?: number;

    /**
     * to date
     */
    to?: number;
  }): Observable<void> {

    return this.getUsages$Response(params).pipe(
      map((r: StrictHttpResponse<void>) => r.body as void)
    );
  }

  /**
   * Path part for operation getUsages_1
   */
  static readonly GetUsages_1Path = '/usage/v1/usages/{appId}';

  /**
   * Get all usages of an application.
   *
   * Get all usages of an application.
   *
   * This method provides access to the full `HttpResponse`, allowing access to response headers.
   * To access only the response body, use `getUsages_1()` instead.
   *
   * This method doesn't expect any request body.
   */
  getUsages_1$Response(params: {

    /**
     * ID of application (or &quot;-home-&quot; for home repository)
     */
    appId: string;
  }): Observable<StrictHttpResponse<Usages>> {

    const rb = new RequestBuilder(this.rootUrl, UsageV1Service.GetUsages_1Path, 'get');
    if (params) {
      rb.path('appId', params.appId, {"style":"simple","explode":false});
    }

    return this.http.request(rb.build({
      responseType: 'json',
      accept: 'application/json'
    })).pipe(
      filter((r: any) => r instanceof HttpResponse),
      map((r: HttpResponse<any>) => {
        return r as StrictHttpResponse<Usages>;
      })
    );
  }

  /**
   * Get all usages of an application.
   *
   * Get all usages of an application.
   *
   * This method provides access to only to the response body.
   * To access the full response (for headers, for example), `getUsages_1$Response()` instead.
   *
   * This method doesn't expect any request body.
   */
  getUsages_1(params: {

    /**
     * ID of application (or &quot;-home-&quot; for home repository)
     */
    appId: string;
  }): Observable<Usages> {

    return this.getUsages_1$Response(params).pipe(
      map((r: StrictHttpResponse<Usages>) => r.body as Usages)
    );
  }

}
