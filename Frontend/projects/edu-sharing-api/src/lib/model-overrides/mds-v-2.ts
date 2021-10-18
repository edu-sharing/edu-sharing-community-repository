import { MdsV2 as MdsV2_ } from '../api/models';
import { SortV2 } from './sort-v-2';
import { Narrow } from './utility-types';
import { ViewV2 } from './view-v-2';

export type MdsViewRelation = 'suggestions';

export interface MdsV2 extends MdsV2_ {
    views?: Narrow<MdsV2_['views'], ViewV2[]>;
    sorts?: Narrow<MdsV2_['sorts'], SortV2[]>;
}
