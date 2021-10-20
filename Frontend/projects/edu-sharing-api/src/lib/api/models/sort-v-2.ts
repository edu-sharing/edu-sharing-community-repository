/* tslint:disable */
/* eslint-disable */
import { SortColumnV2 } from './sort-column-v-2';
import { SortV2Default } from './sort-v-2-default';
export interface SortV2 {
    columns?: Array<SortColumnV2>;
    default?: SortV2Default;
    id?: string;
}
