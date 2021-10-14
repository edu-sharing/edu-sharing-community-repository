/* tslint:disable */
/* eslint-disable */
import { Facette } from './facette';
import { Node } from './node';
import { Pagination } from './pagination';
export interface SearchResultNode {
    facettes: Array<Facette>;
    ignored?: Array<string>;
    nodes: Array<Node>;
    pagination: Pagination;
}
