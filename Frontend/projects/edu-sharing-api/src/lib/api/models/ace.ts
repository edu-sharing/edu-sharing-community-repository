/* tslint:disable */
/* eslint-disable */
import { Authority } from './authority';
import { GroupProfile } from './group-profile';
import { UserProfile } from './user-profile';
export interface Ace {
    authority: Authority;
    editable?: boolean;
    group?: GroupProfile;
    permissions: Array<string>;
    user?: UserProfile;
}
