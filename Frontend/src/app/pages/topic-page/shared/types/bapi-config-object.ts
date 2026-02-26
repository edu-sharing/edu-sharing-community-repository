import { BapiConfig } from './bapi-config';

export interface BapiConfigObject {
    headline?: BapiConfig;
    description?: BapiConfig;
    [key: string]: BapiConfig | undefined;
}
