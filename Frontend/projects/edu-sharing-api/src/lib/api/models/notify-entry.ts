/* tslint:disable */
/* eslint-disable */
import { Acl } from './acl';
import { User } from './user';
export interface NotifyEntry {
    action: string;
    date: number;
    permissions: Acl;
    user: User;
}
