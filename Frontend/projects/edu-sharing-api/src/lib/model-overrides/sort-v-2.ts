import { SortColumnV2, SortV2 as SortV2_, SortV2Default } from '../api/models';
import { Narrow } from './utility-types';

export type MdsViewRelation = 'suggestions';

export interface SortV2 extends SortV2_ {
    id: SortV2_['id'];
    default: Narrow<SortV2_['default'], Required<SortV2Default>>;
    columns: Narrow<SortV2_['columns'], Required<SortColumnV2>[]>;
}
