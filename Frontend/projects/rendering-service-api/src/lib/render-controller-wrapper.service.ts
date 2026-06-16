/* tslint:disable */
/* eslint-disable */
import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { delay, Observable, ReplaySubject, switchMap, tap, timer } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { RenderControllerService } from './api/services';
import { RenderDataRequest } from './api/models/render-data-request';
import { ApiConfiguration } from './api/api-configuration';
import { StrictHttpResponse } from './api/strict-http-response';
import { RenderDataResponse } from './api/models/render-data-response';
import { getRenderData } from './api/fn/render-controller/get-render-data';
import { RequestBuilder } from './api/request-builder';

export type RenderDataRequestWithToken = RenderDataRequest & {
    token: string;
    renderingBaseUrl?: string;
};
@Injectable({ providedIn: 'root' })
export class RenderControllerWrapperService extends RenderControllerService {
    private firstRequestCompleted$ = new ReplaySubject<void>(1);
    private firstRequestStarted = false;

    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    getRenderDataTokenSessionSafe(
        params: RenderDataRequestWithToken,
        context?: HttpContext,
    ): Observable<RenderDataResponse> {
        if (!this.firstRequestStarted) {
            this.firstRequestStarted = true;
            return this.getRenderDataToken$Response(params, context).pipe(
                tap(() => {
                    this.firstRequestCompleted$.next();
                }),
                map((resp) => resp.body!),
                switchMap((data) => timer(50).pipe(map(() => data))),
            );
        }

        return this.firstRequestCompleted$.pipe(
            switchMap(() => {
                return this.getRenderDataToken$Response(params, context).pipe(
                    map((resp) => resp.body!),
                );
            }),
        );
    }

    getRenderDataToken(
        params: RenderDataRequestWithToken,
        context?: HttpContext,
    ): Observable<RenderDataResponse> {
        return this.getRenderDataToken$Response(params, context).pipe(
            map((r: StrictHttpResponse<RenderDataResponse>): RenderDataResponse => r.body),
        );
    }

    private getRenderDataToken$Response(
        params: RenderDataRequestWithToken,
        context?: HttpContext,
    ): Observable<StrictHttpResponse<RenderDataResponse>> {
        const rb = new RequestBuilder(this.rootUrl, getRenderData.PATH, 'post');
        if (params) {
            rb.header('Authorization', 'Bearer ' + params.token);
            rb.body(params, 'application/json');
        }
        return this.http
            .request(rb.build({ responseType: 'json', accept: 'application/json', context }))
            .pipe(
                filter((r: any): r is HttpResponse<any> => r instanceof HttpResponse),
                map((r: HttpResponse<any>) => {
                    return r as StrictHttpResponse<RenderDataResponse>;
                }),
            );
    }
}
