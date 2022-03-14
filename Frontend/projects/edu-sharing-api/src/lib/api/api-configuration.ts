/* tslint:disable */
/* eslint-disable */
import { Injectable } from '@angular/core';

/**
 * Global configuration
 */
@Injectable({
    providedIn: 'root',
})
export class ApiConfiguration implements ApiConfigurationParams{
    rootUrl: string = '/edu-sharing/rest';
    onError = (error: any) => ({});
}

/**
 * Parameters for `ApiModule.forRoot()`
 */
export interface ApiConfigurationParams {
    rootUrl?: string;
    onError?: (error: any) => void
}
