/* tslint:disable */
/* eslint-disable */
import { Pagination } from './pagination';
import { StreamEntry } from './stream-entry';
export interface StreamList {
    pagination?: Pagination;
    stream?: Array<StreamEntry>;
}
