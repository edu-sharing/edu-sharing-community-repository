/* tslint:disable */
/* eslint-disable */
import { MdsSortColumn } from './mds-sort-column';
import { MdsSortDefault } from './mds-sort-default';
export interface MdsSort {
    columns?: Array<MdsSortColumn>;
    default?: MdsSortDefault;
    id: string;
}
