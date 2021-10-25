/* tslint:disable */
/* eslint-disable */
import { CollectionOptions } from './collection-options';
import { DeleteOption } from './delete-option';
import { HomeFolderOptions } from './home-folder-options';
import { SharedFolderOptions } from './shared-folder-options';
export interface PersonDeleteOptions {
    cleanupMetadata?: boolean;
    collectionFeedback?: DeleteOption;
    collections?: CollectionOptions;
    comments?: DeleteOption;
    homeFolder?: HomeFolderOptions;
    ratings?: DeleteOption;
    receiver?: string;
    receiverGroup?: string;
    sharedFolders?: SharedFolderOptions;
    statistics?: DeleteOption;
    stream?: DeleteOption;
}
