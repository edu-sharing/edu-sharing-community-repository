/* tslint:disable */
/* eslint-disable */
import { MdsQueryCriteria } from './mds-query-criteria';
export interface SearchParameters {
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
    facettes: Array<string>;
    permissions?: Array<string>;
    resolveCollections?: boolean;
}
