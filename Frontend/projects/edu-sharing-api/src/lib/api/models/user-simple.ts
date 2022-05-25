/* tslint:disable */
/* eslint-disable */
import { Organization } from './organization';
import { UserProfile } from './user-profile';
import { UserStatus } from './user-status';
export interface UserSimple {
    authorityName: string;
    authorityType?: 'USER' | 'GROUP' | 'OWNER' | 'EVERYONE' | 'GUEST';
    editable?: boolean;
    organizations?: Array<Organization>;
    profile?: UserProfile;
    properties?: {
        [key: string]: Array<string>;
    };
    status?: UserStatus;
    userName?: string;
}
