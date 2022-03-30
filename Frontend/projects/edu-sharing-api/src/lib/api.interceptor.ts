import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiRequestConfiguration } from './api-request-configuration';
import { EduSharingApiConfiguration } from './edu-sharing-api-configuration';
import { handleError } from './utils/handle-error';

@Injectable()
export class ApiInterceptor implements HttpInterceptor {
    constructor(
        private apiRequestConfiguration: ApiRequestConfiguration,
        private configuration: EduSharingApiConfiguration,
    ) {}

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        // Apply the headers
        req = this.apiRequestConfiguration.apply(req);

        return next.handle(req).pipe(
            // Handle errors globally
            handleError((err) => this.configuration.onError?.(err)),
        );
    }
}
