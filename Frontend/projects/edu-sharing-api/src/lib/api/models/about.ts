/* tslint:disable */
/* eslint-disable */
import { Service } from './service';
import { ServiceVersion } from './service-version';
export interface About {
    lastCacheUpdate?: number;
    services: Array<Service>;
    themesUrl?: string;
    version: ServiceVersion;
}
