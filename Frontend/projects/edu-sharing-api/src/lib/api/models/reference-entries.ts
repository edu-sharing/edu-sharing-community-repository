/* tslint:disable */
/* eslint-disable */
import { CollectionReference } from './collection-reference';
import { Pagination } from './pagination';
export interface ReferenceEntries {
    pagination?: Pagination;
    references: Array<CollectionReference>;
}
