/* tslint:disable */
/* eslint-disable */
import { Injectable } from '@angular/core';

/**
 * Global configuration
 */
@Injectable({
    providedIn: 'root',
})
export class ApiConfiguration {
    rootUrl: string = '/edu-sharing/rest';
}

/**
 * Parameters for `ApiModule.forRoot()`
 */
export interface ApiConfigurationParams {
    rootUrl?: string;
}
