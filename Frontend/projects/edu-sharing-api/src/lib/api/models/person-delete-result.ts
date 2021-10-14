/* tslint:disable */
/* eslint-disable */
import { CollectionCounts } from './collection-counts';
import { Counts } from './counts';
export interface PersonDeleteResult {
    authorityName?: string;
    collectionFeedback?: number;
    collections?: CollectionCounts;
    comments?: number;
    deletedName?: string;
    homeFolder?: {
        [key: string]: Counts;
    };
    ratings?: number;
    sharedFolders?: {
        [key: string]: Counts;
    };
    stream?: number;
}
