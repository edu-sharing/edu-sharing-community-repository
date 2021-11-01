/* tslint:disable */
/* eslint-disable */
import { Facet } from './facet';
import { Node } from './node';
import { Pagination } from './pagination';
export interface SearchResultNode {
    facets: Array<Facet>;
    ignored?: Array<string>;
    nodes: Array<Node>;
    pagination: Pagination;
}
