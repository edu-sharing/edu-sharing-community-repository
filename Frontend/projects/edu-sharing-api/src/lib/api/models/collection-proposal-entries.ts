/* tslint:disable */
/* eslint-disable */
import { NodeCollectionProposalCount } from './node-collection-proposal-count';
import { Pagination } from './pagination';
export interface CollectionProposalEntries {
    collections: Array<NodeCollectionProposalCount>;
    pagination?: Pagination;
}
