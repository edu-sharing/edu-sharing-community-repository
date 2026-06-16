import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiConfiguration } from './api/api-configuration';

@Injectable()
export class RenderingServiceApiInterceptor implements HttpInterceptor {
    constructor(private configuration: ApiConfiguration) {}
    private nextAuthHeader?: string;
    private nextAuthValue?: string;

    /** Set to session key */
    bearer(token: string): void {
        this.nextAuthHeader = 'Authorization';
        this.nextAuthValue = 'Bearer ' + token;
    }

    /** Clear any authentication headers (to be called after logout) */
    clear(): void {
        this.nextAuthHeader = undefined;
        this.nextAuthValue = undefined;
    }

    /** Apply the current authorization headers to the given request */
    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        const headers = {} as any;
        // We filter for requests that actually target the API since this interceptor will be called
        // on all HTTP requests by the application, not limited to this library. (See notes in
        // `edu-sharing-api.module.ts`.)

        const isApiRequest = req.url.startsWith(this.configuration.rootUrl);
        if (isApiRequest) {
            if (this.nextAuthHeader) {
                headers[this.nextAuthHeader] = this.nextAuthValue;
            }
        } else {
            return next.handle(req);
        }
        // Apply the headers to the request
        return next.handle(
            req.clone({
                setHeaders: headers,
                // rs requires cookies
                withCredentials: true,
            }),
        );
    }
}
