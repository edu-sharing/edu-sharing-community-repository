/* tslint:disable */
/* eslint-disable */
import { Facet } from './facet';
import { Node } from './node';
import { Pagination } from './pagination';
export interface SearchResult {
    facets: Array<Facet>;
    nodes: Array<Node>;
    pagination: Pagination;
}
