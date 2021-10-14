/* tslint:disable */
/* eslint-disable */
import { StatisticsSubGroup } from './statistics-sub-group';
export interface StatisticsKeyGroup {
    count?: number;
    displayName?: string;
    key?: string;
    subGroups?: Array<StatisticsSubGroup>;
}
