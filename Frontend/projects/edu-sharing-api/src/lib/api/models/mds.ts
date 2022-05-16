/* tslint:disable */
/* eslint-disable */
import { Create } from './create';
import { MdsGroup } from './mds-group';
import { MdsList } from './mds-list';
import { MdsSort } from './mds-sort';
import { MdsView } from './mds-view';
import { MdsWidget } from './mds-widget';
export interface Mds {
    create?: Create;
    groups: Array<MdsGroup>;
    lists: Array<MdsList>;
    name: string;
    sorts: Array<MdsSort>;
    views: Array<MdsView>;
    widgets: Array<MdsWidget>;
}
