import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {Observable, throwError} from 'rxjs';
import {catchError, tap} from 'rxjs/operators';
import { ApiRequestConfiguration } from './api-request-configuration';
import {ApiConfiguration} from './api/api-configuration';

@Injectable()
export class ApiInterceptor implements HttpInterceptor {
    constructor(
        private apiRequestConfiguration: ApiRequestConfiguration,
        private apiConfiguration: ApiConfiguration,
    ) {}

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        // Apply the headers
        req = this.apiRequestConfiguration.apply(req);

        // Also handle errors globally
        return next.handle(req).pipe(
            catchError(
                (err) => {
                    // Handle this error externally
                    this.apiConfiguration.onError(err);
                    err.processed = true;
                    return throwError(err);
                },
            ));
    }
}
