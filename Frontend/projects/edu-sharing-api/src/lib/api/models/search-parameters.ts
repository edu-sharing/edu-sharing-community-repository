/* tslint:disable */
/* eslint-disable */
import { MdsQueryCriteria } from './mds-query-criteria';
export interface SearchParameters {
  criterias: Array<MdsQueryCriteria>;
  facettes: Array<string>;
  permissions?: Array<string>;
  resolveCollections?: boolean;
}
