/* tslint:disable */
/* eslint-disable */
import { Query } from './query';
export interface Frontpage {
    collection?: string;
    displayCount?: number;
    mode?: 'collection' | 'rating' | 'views' | 'downloads';
    queries?: Array<Query>;
    timespan?: number;
    timespanAll?: boolean;
    totalCount?: number;
}
