/* tslint:disable */
/* eslint-disable */
import { Node } from './node';
import { Person } from './person';
export interface SharingInfo {
    expired?: boolean;
    invitedBy?: Person;
    node?: Node;
    password?: boolean;
    passwordMatches?: boolean;
}
