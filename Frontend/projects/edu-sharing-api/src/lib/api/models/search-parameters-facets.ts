/* tslint:disable */
/* eslint-disable */
import { MdsQueryCriteria } from './mds-query-criteria';
export interface SearchParametersFacets {
    criteria: Array<MdsQueryCriteria>;
    facetLimit?: number;
    facetMinCount?: number;
    facetSuggest?: string;
    facets: Array<string>;
}
