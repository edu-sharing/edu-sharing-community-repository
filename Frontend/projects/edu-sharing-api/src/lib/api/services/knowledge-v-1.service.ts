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

import { JobEntry } from '../models/job-entry';

@Injectable({
    providedIn: 'root',
})
export class KnowledgeV1Service extends BaseService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    /**
     * Path part for operation getAnalyzingJobStatus
     */
    static readonly GetAnalyzingJobStatusPath = '/knowledge/v1/analyze/jobs/{job}';

    /**
     * Get analyzing job status.
     *
     * Get analyzing job status.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `getAnalyzingJobStatus()` instead.
     *
     * This method doesn't expect any request body.
     */
    getAnalyzingJobStatus$Response(params: {
        /**
         * ID of job ticket
         */
        job: string;
    }): Observable<StrictHttpResponse<JobEntry>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            KnowledgeV1Service.GetAnalyzingJobStatusPath,
            'get',
        );
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
                    return r as StrictHttpResponse<JobEntry>;
                }),
            );
    }

    /**
     * Get analyzing job status.
     *
     * Get analyzing job status.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `getAnalyzingJobStatus$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    getAnalyzingJobStatus(params: {
        /**
         * ID of job ticket
         */
        job: string;
    }): Observable<JobEntry> {
        return this.getAnalyzingJobStatus$Response(params).pipe(
            map((r: StrictHttpResponse<JobEntry>) => r.body as JobEntry),
        );
    }

    /**
     * Path part for operation runAnalyzingJob
     */
    static readonly RunAnalyzingJobPath = '/knowledge/v1/analyze/jobs';

    /**
     * Run analyzing job.
     *
     * Run analyzing job for a node.
     *
     * This method provides access to the full `HttpResponse`, allowing access to response headers.
     * To access only the response body, use `runAnalyzingJob()` instead.
     *
     * This method doesn't expect any request body.
     */
    runAnalyzingJob$Response(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<StrictHttpResponse<JobEntry>> {
        const rb = new RequestBuilder(this.rootUrl, KnowledgeV1Service.RunAnalyzingJobPath, 'post');
        if (params) {
            rb.query('repository', params.repository, {});
            rb.query('node', params.node, {});
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
                    return r as StrictHttpResponse<JobEntry>;
                }),
            );
    }

    /**
     * Run analyzing job.
     *
     * Run analyzing job for a node.
     *
     * This method provides access to only to the response body.
     * To access the full response (for headers, for example), `runAnalyzingJob$Response()` instead.
     *
     * This method doesn't expect any request body.
     */
    runAnalyzingJob(params: {
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string;

        /**
         * ID of node
         */
        node: string;
    }): Observable<JobEntry> {
        return this.runAnalyzingJob$Response(params).pipe(
            map((r: StrictHttpResponse<JobEntry>) => r.body as JobEntry),
        );
    }
}
