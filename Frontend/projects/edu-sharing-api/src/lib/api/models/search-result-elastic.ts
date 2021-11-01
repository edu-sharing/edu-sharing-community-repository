/* tslint:disable */
/* eslint-disable */
import { Facet } from './facet';
import { Pagination } from './pagination';
export interface SearchResultElastic {
    elasticResponse?: string;
    facets: Array<Facet>;
    ignored?: Array<string>;
    nodes: Array<{}>;
    pagination: Pagination;
}
