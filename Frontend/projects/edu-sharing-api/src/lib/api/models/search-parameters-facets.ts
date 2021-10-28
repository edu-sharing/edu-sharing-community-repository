/* tslint:disable */
/* eslint-disable */
import { MdsQueryCriteria } from './mds-query-criteria';
export interface SearchParametersFacets {
    criterias: Array<MdsQueryCriteria>;

    /**
     * 10
     */
    facetLimit: number;

    /**
     * 5
     */
    facetMinCount: number;
    facetSuggest?: string;
    facets: Array<string>;
}
