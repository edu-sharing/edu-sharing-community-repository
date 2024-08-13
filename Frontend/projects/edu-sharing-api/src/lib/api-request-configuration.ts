import { HttpRequest } from '@angular/common/http';
import { EventEmitter, Injectable } from '@angular/core';
import { v4 as uuidv4 } from 'uuid';
import { ApiConfiguration } from './api/api-configuration';

/**
 * Configuration for the performed HTTP requests
 */
@Injectable()
export class ApiRequestConfiguration {
    private authForNextRequest: string | null = null;
    private _locale: string | null = null;
    private _language: string | null = null;

    /** Emits each time, an API request is performed. */
    readonly apiRequest = new EventEmitter<void>();

    constructor(private apiConfiguration: ApiConfiguration) {}

    setLocale(locale: string): void {
        this._locale = locale;
    }

    /**
     * internal edu sharing language code, might be "de", "en" or something like "de-formal"
     */
    setLanguage(language: string): void {
        this._language = language;
    }

    public getLocale(): string | null {
        return this._locale;
    }

    public getLanguage(): string | null {
        return this._language;
    }

    setBasicAuthForNextRequest(auth: { username: string; password: string }): void {
        this.authForNextRequest = 'Basic ' + btoa(auth.username + ':' + auth.password);
    }
    setBearerAuthForNextRequest(accessToken?: string): void {
        this.authForNextRequest = 'Bearer ' + accessToken;
    }

    /** Apply the current headers to the given request */
    apply(req: HttpRequest<any>): HttpRequest<any> {
        const headers: { [key: string]: string | string[] } = {};
        const isAPICall = req.url.startsWith(this.apiConfiguration.rootUrl);
        if (!isAPICall) {
            return req;
        }
        this.apiRequest.emit();
        headers['X-Client-Trace-Id'] = this.generateTraceId();
        if (this._locale) {
            headers.locale = this._locale;
        }
        if (this._language) {
            headers['X-Edu-Sharing-Language'] = this._language;
        }
        if (this.authForNextRequest) {
            headers.Authorization = this.authForNextRequest;
            this.authForNextRequest = null;
        }
        return req.clone({
            setHeaders: headers,
        });
    }

    private generateTraceId(): string {
        return uuidv4();
    }
}
