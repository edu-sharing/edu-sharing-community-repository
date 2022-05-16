/* tslint:disable */
/* eslint-disable */
import { GroupProfile } from './group-profile';
import { NodeRef } from './node-ref';
export interface Organization {
    administrationAccess?: boolean;
    aspects?: Array<string>;
    authorityName: string;
    authorityType?: 'USER' | 'GROUP' | 'OWNER' | 'EVERYONE' | 'GUEST';
    editable?: boolean;
    groupName?: string;
    profile?: GroupProfile;
    properties?: {
        [key: string]: Array<string>;
    };
    ref?: NodeRef;
    sharedFolder?: NodeRef;
    signupMethod?: 'simple' | 'password' | 'list';
}
