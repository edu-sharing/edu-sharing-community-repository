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

import { Filter } from '../models/filter';
import { NodeData } from '../models/node-data';
import { Statistics } from '../models/statistics';
import { StatisticsGlobal } from '../models/statistics-global';
import { Tracking } from '../models/tracking';
import { TrackingNode } from '../models/tracking-node';

@Injectable({
  providedIn: 'root',
})
export class StatisticV1Service extends BaseService {
  constructor(
    config: ApiConfiguration,
    http: HttpClient
  ) {
    super(config, http);
  }

  /**
   * Path part for operation get
   */
  static readonly GetPath = '/statistic/v1/facettes/{context}';

  /**
   * Get statistics of repository.
   *
   * Statistics.
   *
   * This method provides access to the full `HttpResponse`, allowing access to response headers.
   * To access only the response body, use `get()` instead.
   *
   * This method sends `application/json` and handles request body of type `application/json`.
   */
  get$Response(params: {

    /**
     * context, the node where to start
     */
    context: string;

    /**
     * properties
     */
    properties?: Array<string>;

    /**
     * filter
     */
    body: Filter
  }): Observable<StrictHttpResponse<Statistics>> {

    const rb = new RequestBuilder(this.rootUrl, StatisticV1Service.GetPath, 'post');
    if (params) {
      rb.path('context', params.context, {"style":"simple","explode":false});
      rb.query('properties', params.properties, {"style":"form","explode":true});
      rb.body(params.body, 'application/json');
    }

    return this.http.request(rb.build({
      responseType: 'json',
      accept: 'application/json'
    })).pipe(
      filter((r: any) => r instanceof HttpResponse),
      map((r: HttpResponse<any>) => {
        return r as StrictHttpResponse<Statistics>;
      })
    );
  }

  /**
   * Get statistics of repository.
   *
   * Statistics.
   *
   * This method provides access to only to the response body.
   * To access the full response (for headers, for example), `get$Response()` instead.
   *
   * This method sends `application/json` and handles request body of type `application/json`.
   */
  get(params: {

    /**
     * context, the node where to start
     */
    context: string;

    /**
     * properties
     */
    properties?: Array<string>;

    /**
     * filter
     */
    body: Filter
  }): Observable<Statistics> {

    return this.get$Response(params).pipe(
      map((r: StrictHttpResponse<Statistics>) => r.body as Statistics)
    );
  }

  /**
   * Path part for operation getGlobalStatistics
   */
  static readonly GetGlobalStatisticsPath = '/statistic/v1/public';

  /**
   * Get stats.
   *
   * Get global statistics for this repository.
   *
   * This method provides access to the full `HttpResponse`, allowing access to response headers.
   * To access only the response body, use `getGlobalStatistics()` instead.
   *
   * This method doesn't expect any request body.
   */
  getGlobalStatistics$Response(params?: {

    /**
     * primary property to build facettes and count+group values
     */
    group?: string;

    /**
     * additional properties to build facettes and count+sub-group values
     */
    subGroup?: Array<string>;
  }): Observable<StrictHttpResponse<StatisticsGlobal>> {

    const rb = new RequestBuilder(this.rootUrl, StatisticV1Service.GetGlobalStatisticsPath, 'get');
    if (params) {
      rb.query('group', params.group, {"style":"form","explode":true});
      rb.query('subGroup', params.subGroup, {"style":"form","explode":true});
    }

    return this.http.request(rb.build({
      responseType: 'json',
      accept: 'application/json'
    })).pipe(
      filter((r: any) => r instanceof HttpResponse),
      map((r: HttpResponse<any>) => {
        return r as StrictHttpResponse<StatisticsGlobal>;
      })
    );
  }

  /**
   * Get stats.
   *
   * Get global statistics for this repository.
   *
   * This method provides access to only to the response body.
   * To access the full response (for headers, for example), `getGlobalStatistics$Response()` instead.
   *
   * This method doesn't expect any request body.
   */
  getGlobalStatistics(params?: {

    /**
     * primary property to build facettes and count+group values
     */
    group?: string;

    /**
     * additional properties to build facettes and count+sub-group values
     */
    subGroup?: Array<string>;
  }): Observable<StatisticsGlobal> {

    return this.getGlobalStatistics$Response(params).pipe(
      map((r: StrictHttpResponse<StatisticsGlobal>) => r.body as StatisticsGlobal)
    );
  }

  /**
   * Path part for operation getStatisticsNode
   */
  static readonly GetStatisticsNodePath = '/statistic/v1/statistics/nodes';

  /**
   * get statistics for node actions.
   *
   * requires either toolpermission TOOLPERMISSION_GLOBAL_STATISTICS_NODES for global stats or to be admin of the requested mediacenter
   *
   * This method provides access to the full `HttpResponse`, allowing access to response headers.
   * To access only the response body, use `getStatisticsNode()` instead.
   *
   * This method sends `application/json` and handles request body of type `application/json`.
   */
  getStatisticsNode$Response(params: {

    /**
     * Grouping type (by date)
     */
    grouping: 'None' | 'Daily' | 'Monthly' | 'Yearly' | 'Node';

    /**
     * date range from
     */
    dateFrom: number;

    /**
     * date range to
     */
    dateTo: number;

    /**
     * the mediacenter to filter for statistics
     */
    mediacenter?: string;

    /**
     * additionals fields of the custom json object stored in each query that should be returned
     */
    additionalFields?: Array<string>;

    /**
     * grouping fields of the custom json object stored in each query (currently only meant to be combined with no grouping by date)
     */
    groupField?: Array<string>;

    /**
     * filters for the custom json object stored in each entry
     */
    body?: {
[key: string]: string;
}
  }): Observable<StrictHttpResponse<Array<TrackingNode>>> {

    const rb = new RequestBuilder(this.rootUrl, StatisticV1Service.GetStatisticsNodePath, 'post');
    if (params) {
      rb.query('grouping', params.grouping, {"style":"form","explode":true});
      rb.query('dateFrom', params.dateFrom, {"style":"form","explode":true});
      rb.query('dateTo', params.dateTo, {"style":"form","explode":true});
      rb.query('mediacenter', params.mediacenter, {"style":"form","explode":true});
      rb.query('additionalFields', params.additionalFields, {"style":"form","explode":true});
      rb.query('groupField', params.groupField, {"style":"form","explode":true});
      rb.body(params.body, 'application/json');
    }

    return this.http.request(rb.build({
      responseType: 'json',
      accept: 'application/json'
    })).pipe(
      filter((r: any) => r instanceof HttpResponse),
      map((r: HttpResponse<any>) => {
        return r as StrictHttpResponse<Array<TrackingNode>>;
      })
    );
  }

  /**
   * get statistics for node actions.
   *
   * requires either toolpermission TOOLPERMISSION_GLOBAL_STATISTICS_NODES for global stats or to be admin of the requested mediacenter
   *
   * This method provides access to only to the response body.
   * To access the full response (for headers, for example), `getStatisticsNode$Response()` instead.
   *
   * This method sends `application/json` and handles request body of type `application/json`.
   */
  getStatisticsNode(params: {

    /**
     * Grouping type (by date)
     */
    grouping: 'None' | 'Daily' | 'Monthly' | 'Yearly' | 'Node';

    /**
     * date range from
     */
    dateFrom: number;

    /**
     * date range to
     */
    dateTo: number;

    /**
     * the mediacenter to filter for statistics
     */
    mediacenter?: string;

    /**
     * additionals fields of the custom json object stored in each query that should be returned
     */
    additionalFields?: Array<string>;

    /**
     * grouping fields of the custom json object stored in each query (currently only meant to be combined with no grouping by date)
     */
    groupField?: Array<string>;

    /**
     * filters for the custom json object stored in each entry
     */
    body?: {
[key: string]: string;
}
  }): Observable<Array<TrackingNode>> {

    return this.getStatisticsNode$Response(params).pipe(
      map((r: StrictHttpResponse<Array<TrackingNode>>) => r.body as Array<TrackingNode>)
    );
  }

  /**
   * Path part for operation getNodesAlteredInRange
   */
  static readonly GetNodesAlteredInRangePath = '/statistic/v1/statistics/nodes/altered';

  /**
   * get the range of nodes which had tracked actions since a given timestamp.
   *
   * requires admin
   *
   * This method provides access to the full `HttpResponse`, allowing access to response headers.
   * To access only the response body, use `getNodesAlteredInRange()` instead.
   *
   * This method doesn't expect any request body.
   */
  getNodesAlteredInRange$Response(params: {

    /**
     * date range from
     */
    dateFrom: number;
  }): Observable<StrictHttpResponse<Array<string>>> {

    const rb = new RequestBuilder(this.rootUrl, StatisticV1Service.GetNodesAlteredInRangePath, 'get');
    if (params) {
      rb.query('dateFrom', params.dateFrom, {"style":"form","explode":true});
    }

    return this.http.request(rb.build({
      responseType: 'json',
      accept: 'application/json'
    })).pipe(
      filter((r: any) => r instanceof HttpResponse),
      map((r: HttpResponse<any>) => {
        return r as StrictHttpResponse<Array<string>>;
      })
    );
  }

  /**
   * get the range of nodes which had tracked actions since a given timestamp.
   *
   * requires admin
   *
   * This method provides access to only to the response body.
   * To access the full response (for headers, for example), `getNodesAlteredInRange$Response()` instead.
   *
   * This method doesn't expect any request body.
   */
  getNodesAlteredInRange(params: {

    /**
     * date range from
     */
    dateFrom: number;
  }): Observable<Array<string>> {

    return this.getNodesAlteredInRange$Response(params).pipe(
      map((r: StrictHttpResponse<Array<string>>) => r.body as Array<string>)
    );
  }

  /**
   * Path part for operation getNodeData
   */
  static readonly GetNodeDataPath = '/statistic/v1/statistics/nodes/node/{id}';

  /**
   * get the range of nodes which had tracked actions since a given timestamp.
   *
   * requires admin
   *
   * This method provides access to the full `HttpResponse`, allowing access to response headers.
   * To access only the response body, use `getNodeData()` instead.
   *
   * This method doesn't expect any request body.
   */
  getNodeData$Response(params: {

    /**
     * node id to fetch data for
     */
    id: string;

    /**
     * date range from
     */
    dateFrom: number;
  }): Observable<StrictHttpResponse<Array<NodeData>>> {

    const rb = new RequestBuilder(this.rootUrl, StatisticV1Service.GetNodeDataPath, 'get');
    if (params) {
      rb.path('id', params.id, {"style":"simple","explode":false});
      rb.query('dateFrom', params.dateFrom, {"style":"form","explode":true});
    }

    return this.http.request(rb.build({
      responseType: 'json',
      accept: 'application/json'
    })).pipe(
      filter((r: any) => r instanceof HttpResponse),
      map((r: HttpResponse<any>) => {
        return r as StrictHttpResponse<Array<NodeData>>;
      })
    );
  }

  /**
   * get the range of nodes which had tracked actions since a given timestamp.
   *
   * requires admin
   *
   * This method provides access to only to the response body.
   * To access the full response (for headers, for example), `getNodeData$Response()` instead.
   *
   * This method doesn't expect any request body.
   */
  getNodeData(params: {

    /**
     * node id to fetch data for
     */
    id: string;

    /**
     * date range from
     */
    dateFrom: number;
  }): Observable<Array<NodeData>> {

    return this.getNodeData$Response(params).pipe(
      map((r: StrictHttpResponse<Array<NodeData>>) => r.body as Array<NodeData>)
    );
  }

  /**
   * Path part for operation getStatisticsUser
   */
  static readonly GetStatisticsUserPath = '/statistic/v1/statistics/users';

  /**
   * get statistics for user actions (login, logout).
   *
   * requires either toolpermission TOOLPERMISSION_GLOBAL_STATISTICS_USER for global stats or to be admin of the requested mediacenter
   *
   * This method provides access to the full `HttpResponse`, allowing access to response headers.
   * To access only the response body, use `getStatisticsUser()` instead.
   *
   * This method sends `application/json` and handles request body of type `application/json`.
   */
  getStatisticsUser$Response(params: {

    /**
     * Grouping type (by date)
     */
    grouping: 'None' | 'Daily' | 'Monthly' | 'Yearly' | 'Node';

    /**
     * date range from
     */
    dateFrom: number;

    /**
     * date range to
     */
    dateTo: number;

    /**
     * the mediacenter to filter for statistics
     */
    mediacenter?: string;

    /**
     * additionals fields of the custom json object stored in each query that should be returned
     */
    additionalFields?: Array<string>;

    /**
     * grouping fields of the custom json object stored in each query (currently only meant to be combined with no grouping by date)
     */
    groupField?: Array<string>;

    /**
     * filters for the custom json object stored in each entry
     */
    body?: {
[key: string]: string;
}
  }): Observable<StrictHttpResponse<Array<Tracking>>> {

    const rb = new RequestBuilder(this.rootUrl, StatisticV1Service.GetStatisticsUserPath, 'post');
    if (params) {
      rb.query('grouping', params.grouping, {"style":"form","explode":true});
      rb.query('dateFrom', params.dateFrom, {"style":"form","explode":true});
      rb.query('dateTo', params.dateTo, {"style":"form","explode":true});
      rb.query('mediacenter', params.mediacenter, {"style":"form","explode":true});
      rb.query('additionalFields', params.additionalFields, {"style":"form","explode":true});
      rb.query('groupField', params.groupField, {"style":"form","explode":true});
      rb.body(params.body, 'application/json');
    }

    return this.http.request(rb.build({
      responseType: 'json',
      accept: 'application/json'
    })).pipe(
      filter((r: any) => r instanceof HttpResponse),
      map((r: HttpResponse<any>) => {
        return r as StrictHttpResponse<Array<Tracking>>;
      })
    );
  }

  /**
   * get statistics for user actions (login, logout).
   *
   * requires either toolpermission TOOLPERMISSION_GLOBAL_STATISTICS_USER for global stats or to be admin of the requested mediacenter
   *
   * This method provides access to only to the response body.
   * To access the full response (for headers, for example), `getStatisticsUser$Response()` instead.
   *
   * This method sends `application/json` and handles request body of type `application/json`.
   */
  getStatisticsUser(params: {

    /**
     * Grouping type (by date)
     */
    grouping: 'None' | 'Daily' | 'Monthly' | 'Yearly' | 'Node';

    /**
     * date range from
     */
    dateFrom: number;

    /**
     * date range to
     */
    dateTo: number;

    /**
     * the mediacenter to filter for statistics
     */
    mediacenter?: string;

    /**
     * additionals fields of the custom json object stored in each query that should be returned
     */
    additionalFields?: Array<string>;

    /**
     * grouping fields of the custom json object stored in each query (currently only meant to be combined with no grouping by date)
     */
    groupField?: Array<string>;

    /**
     * filters for the custom json object stored in each entry
     */
    body?: {
[key: string]: string;
}
  }): Observable<Array<Tracking>> {

    return this.getStatisticsUser$Response(params).pipe(
      map((r: StrictHttpResponse<Array<Tracking>>) => r.body as Array<Tracking>)
    );
  }

}
