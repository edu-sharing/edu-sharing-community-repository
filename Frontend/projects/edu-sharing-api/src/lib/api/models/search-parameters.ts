/* tslint:disable */
/* eslint-disable */
import { MdsQueryCriteria } from './mds-query-criteria';
export interface SearchParameters {
    criteria: Array<MdsQueryCriteria>;
    facetLimit?: number;
    facetMinCount?: number;
    facetSuggest?: string;
    facets?: Array<string>;
    permissions?: Array<string>;
    resolveCollections?: boolean;
    returnSuggestions?: boolean;
}
