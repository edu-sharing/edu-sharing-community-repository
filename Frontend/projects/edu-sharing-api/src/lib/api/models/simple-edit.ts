/* tslint:disable */
/* eslint-disable */
import { SimpleEditGlobalGroups } from './simple-edit-global-groups';
import { SimpleEditOrganization } from './simple-edit-organization';
export interface SimpleEdit {
    globalGroups?: Array<SimpleEditGlobalGroups>;
    licenses?: Array<string>;
    organization?: SimpleEditOrganization;
    organizationFilter?: string;
}
