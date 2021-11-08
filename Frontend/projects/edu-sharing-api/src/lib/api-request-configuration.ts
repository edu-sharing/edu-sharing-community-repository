import { Injectable } from '@angular/core';
import { HttpRequest } from '@angular/common/http';

/**
 * Configuration for the performed HTTP requests
 */
@Injectable()
export class ApiRequestConfiguration {
    private locale: string | null = null;

    setLocale(locale: string): void {
        this.locale = locale;
    }

    /** Apply the current headers to the given request */
    apply(req: HttpRequest<any>): HttpRequest<any> {
        const headers: { [name: string]: string | string[] } = {};
        if (this.locale) {
            headers.locale = this.locale;
        }
        return req.clone({
            setHeaders: headers,
        });
    }
}
