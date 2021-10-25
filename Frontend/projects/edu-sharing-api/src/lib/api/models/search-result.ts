/* tslint:disable */
/* eslint-disable */
import { Facette } from './facette';
import { Node } from './node';
import { Pagination } from './pagination';
export interface SearchResult {
    facettes: Array<Facette>;
    nodes: Array<Node>;
    pagination: Pagination;
}
