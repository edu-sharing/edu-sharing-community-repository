import {
    HttpEvent,
    HttpHandler,
    HttpInterceptor,
    HttpParams,
    HttpRequest,
    HttpResponse,
} from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

/**
 * Prints warnings for high response times and appends a timestamp query parameter to each request
 * for debugging.
 */
@Injectable()
export class TimingInterceptor implements HttpInterceptor {
    readonly warningThresholdMs = 1000;

    constructor() {}

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        const requestTime = new Date().getTime();
        const newReq = req.clone({
            params: (req.params ?? new HttpParams()).set('timestamp', requestTime),
        });
        return next.handle(newReq).pipe(
            tap((event) => {
                if (event instanceof HttpResponse) {
                    const responseTime = new Date().getTime();
                    const delta = responseTime - requestTime;
                    if (delta > this.warningThresholdMs) {
                        console.warn(`Request took ${delta}ms to complete`, event.url);
                    }
                }
            }),
        );
    }
}
