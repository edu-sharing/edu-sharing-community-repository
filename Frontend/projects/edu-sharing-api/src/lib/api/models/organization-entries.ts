/* tslint:disable */
/* eslint-disable */
import { Organization } from './organization';
import { Pagination } from './pagination';
export interface OrganizationEntries {
    canCreate?: boolean;
    organizations: Array<Organization>;
    pagination: Pagination;
}
