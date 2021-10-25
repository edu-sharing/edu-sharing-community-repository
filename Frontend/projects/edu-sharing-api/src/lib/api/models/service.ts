/* tslint:disable */
/* eslint-disable */
import { Audience } from './audience';
import { Interface } from './interface';
import { Provider } from './provider';
export interface Service {
    about?: Array<string>;
    audience?: Array<Audience>;
    description?: string;
    icon?: string;
    inLanguage?: string;
    interfaces?: Array<Interface>;
    isAccessibleForFree?: boolean;
    logo?: string;
    name?: string;
    provider?: Provider;
    startDate?: string;
    type?: string;
    url?: string;
}
