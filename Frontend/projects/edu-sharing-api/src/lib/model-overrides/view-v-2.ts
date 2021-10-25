import { ViewV2 as ViewV2_ } from '../api/models';
import { Narrow } from './utility-types';

export type MdsViewRelation = 'suggestions';

export interface ViewV2 extends ViewV2_ {
    rel?: Narrow<ViewV2_['rel'], MdsViewRelation>;
}
