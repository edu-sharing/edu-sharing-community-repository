import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BApiConfiguration } from './b-api-configuration';

@Injectable()
export class ApiInterceptor implements HttpInterceptor {
    constructor(private bApiConfiguration: BApiConfiguration) {}
    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        // Apply the headers
        req = req.clone({
            setHeaders: {
                'X-API-KEY': this.bApiConfiguration.token,
            },
        });
        // Also handle errors globally
        return next.handle(req);
    }
}
