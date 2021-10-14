/* tslint:disable */
/* eslint-disable */
import { Group } from './group';
import { Organization } from './organization';
export interface TrackingAuthority {
    hash?: string;
    mediacenter?: Array<Group>;
    organization?: Array<Organization>;
}
