import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { ApiRequestConfiguration } from './api-request-configuration';

@Injectable()
export class ApiInterceptor implements HttpInterceptor {
    constructor(private apiRequestConfiguration: ApiRequestConfiguration) {}
    
    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        // Apply the headers
        req = this.apiRequestConfiguration.apply(req);

        return next.handle(req);
        // Also handle errors globally
        // .pipe(
        //     tap(
        //         (x) => x,
        //         (err) => {
        //             // Handle this err
        //             console.error(`Error performing request, status code = ${err.status}`);
        //         },
        //     ),
        // );
    }
}
