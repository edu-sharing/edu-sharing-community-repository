/* tslint:disable */
/* eslint-disable */
import { Create } from './create';
import { GroupV2 } from './group-v-2';
import { ListV2 } from './list-v-2';
import { SortV2 } from './sort-v-2';
import { ViewV2 } from './view-v-2';
import { WidgetV2 } from './widget-v-2';
export interface MdsV2 {
    create?: Create;
    groups?: Array<GroupV2>;
    lists?: Array<ListV2>;
    name?: string;
    sorts?: Array<SortV2>;
    views?: Array<ViewV2>;
    widgets?: Array<WidgetV2>;
}
