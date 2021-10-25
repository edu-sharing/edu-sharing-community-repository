/* tslint:disable */
/* eslint-disable */
import { Facette } from './facette';
import { Pagination } from './pagination';
export interface SearchResultElastic {
    elasticResponse?: string;
    facettes: Array<Facette>;
    ignored?: Array<string>;
    nodes: Array<{}>;
    pagination: Pagination;
}
