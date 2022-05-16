/* tslint:disable */
/* eslint-disable */
import { Node } from './node';
import { Pagination } from './pagination';
export interface ParentEntries {
    nodes: Array<Node>;
    pagination: Pagination;
    scope?: string;
}
