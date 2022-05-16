/* tslint:disable */
/* eslint-disable */
import { Node } from './node';
export interface AdminStatistics {
    activeLocks?: Array<Node>;
    activeSessions?: number;
    allocatedMemory?: number;
    maxMemory?: number;
    numberOfPreviews?: number;
    previewCacheSize?: number;
}
