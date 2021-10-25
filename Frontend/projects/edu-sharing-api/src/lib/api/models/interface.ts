/* tslint:disable */
/* eslint-disable */
export interface Interface {
    documentation?: string;
    format?: 'Json' | 'XML' | 'Text';
    metadataPrefix?: string;
    set?: string;
    type?: 'Search' | 'Sitemap' | 'Statistics' | 'OAI' | 'Generic_Api';
    url?: string;
}
