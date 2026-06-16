/* tslint:disable */
/* eslint-disable */
import { TrackingRequest } from './api/models/tracking-request';
import { EduTrackingControllerService } from './api/services/edu-tracking-controller.service';
import { Injectable } from '@angular/core';
import { ApiConfiguration } from './api/api-configuration';
import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { StrictHttpResponse } from './api/strict-http-response';
import { RequestBuilder } from './api/request-builder';
import { filter, map } from 'rxjs/operators';

export type TrackingRequestWithToken = TrackingRequest & { token: string };

@Injectable({ providedIn: 'root' })
export class EduTrackingControllerWrapperService extends EduTrackingControllerService {
    constructor(config: ApiConfiguration, http: HttpClient) {
        super(config, http);
    }

    trackObjectToken$Response(
        params: TrackingRequestWithToken,
        context?: HttpContext,
    ): Observable<StrictHttpResponse<void>> {
        const rb = new RequestBuilder(
            this.rootUrl,
            EduTrackingControllerService.TrackObjectPath,
            'put',
        );
        if (params) {
            rb.header('Authorization', 'Bearer ' + params.token);
            rb.body(params, 'application/json');
        }
        return this.http
            .request(rb.build({ responseType: 'json', accept: 'application/json', context }))
            .pipe(
                filter((r: any): r is HttpResponse<any> => r instanceof HttpResponse),
                map((r: HttpResponse<any>) => {
                    return r as StrictHttpResponse<void>;
                }),
            );
    }

    trackObjectToken(params: TrackingRequestWithToken, context?: HttpContext): Observable<void> {
        return this.trackObjectToken$Response(params, context).pipe(
            map((r: StrictHttpResponse<void>): void => r.body),
        );
    }
}
