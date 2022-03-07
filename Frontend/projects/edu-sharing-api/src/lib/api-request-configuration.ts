import { Injectable } from '@angular/core';
import { HttpRequest } from '@angular/common/http';
import {ApiConfiguration} from './api/api-configuration';

/**
 * Configuration for the performed HTTP requests
 */
@Injectable()
export class ApiRequestConfiguration {
    private authForNextRequest: string | null = null;
    private locale: string | null = null;

    constructor(private apiConfiguration: ApiConfiguration) {

    }

    setLocale(locale: string): void {
        this.locale = locale;
    }

    setBasicAuthForNextRequest(auth: { username: string; password: string }): void {
        this.authForNextRequest = 'Basic ' + btoa(auth.username + ':' + auth.password);
    }

    /** Apply the current headers to the given request */
    apply(req: HttpRequest<any>): HttpRequest<any> {
        const headers: { [name: string]: string | string[] } = {};
        const isAPICall = req.url.startsWith(this.apiConfiguration.rootUrl);
        if(!isAPICall) {
            return req;
        }
        if (this.locale) {
            headers.locale = this.locale;
        }
        if (this.authForNextRequest) {
            headers.Authorization = this.authForNextRequest;
            this.authForNextRequest = null;
        }
        return req.clone({
            setHeaders: headers,
        });
    }
}
