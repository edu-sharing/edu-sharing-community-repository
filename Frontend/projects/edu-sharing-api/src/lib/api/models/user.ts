/* tslint:disable */
/* eslint-disable */
import { NodeRef } from './node-ref';
import { Organization } from './organization';
import { UserProfile } from './user-profile';
import { UserQuota } from './user-quota';
import { UserStatus } from './user-status';
export interface User {
    authorityName: string;
    authorityType?: 'USER' | 'GROUP' | 'OWNER' | 'EVERYONE' | 'GUEST';
    editable?: boolean;
    homeFolder: NodeRef;
    organizations?: Array<Organization>;
    profile?: UserProfile;
    properties?: {
        [key: string]: Array<string>;
    };
    quota?: UserQuota;
    sharedFolders?: Array<NodeRef>;
    status?: UserStatus;
    userName?: string;
}
