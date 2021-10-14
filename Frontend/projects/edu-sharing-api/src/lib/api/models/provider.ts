/* tslint:disable */
/* eslint-disable */
import { Location } from './location';
export interface Provider {
    areaServed?: 'Organization' | 'City' | 'State' | 'Country' | 'Continent' | 'World';
    email?: string;
    legalName?: string;
    location?: Location;
    url?: string;
}
