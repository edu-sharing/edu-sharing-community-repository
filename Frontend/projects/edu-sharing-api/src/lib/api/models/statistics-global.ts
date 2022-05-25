/* tslint:disable */
/* eslint-disable */
import { StatisticsGroup } from './statistics-group';
import { StatisticsKeyGroup } from './statistics-key-group';
import { StatisticsUser } from './statistics-user';
export interface StatisticsGlobal {
    groups?: Array<StatisticsKeyGroup>;
    overall?: StatisticsGroup;
    user?: StatisticsUser;
}
