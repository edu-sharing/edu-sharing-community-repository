/* tslint:disable */
/* eslint-disable */
import { CacheInfo } from './cache-info';
import { CacheMember } from './cache-member';
export interface CacheCluster {
    availableProcessors?: number;
    cacheInfos?: Array<CacheInfo>;
    freeMemory?: number;
    groupName?: string;
    instances?: Array<CacheMember>;
    localMember?: string;
    maxMemory?: number;
    timeStamp?: string;
    totalMemory?: number;
}
