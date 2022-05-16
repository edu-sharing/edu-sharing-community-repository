/* tslint:disable */
/* eslint-disable */
import { Ace } from './ace';
import { Acl } from './acl';
export interface NodePermissions {
    inheritedPermissions: Array<Ace>;
    localPermissions: Acl;
}
