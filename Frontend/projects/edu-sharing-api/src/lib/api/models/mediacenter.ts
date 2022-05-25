/* tslint:disable */
/* eslint-disable */
import { GroupProfile } from './group-profile';
import { NodeRef } from './node-ref';
import { Organization } from './organization';
export interface Mediacenter {
    administrationAccess?: boolean;
    aspects?: Array<string>;
    authorityName: string;
    authorityType?: 'USER' | 'GROUP' | 'OWNER' | 'EVERYONE' | 'GUEST';
    editable?: boolean;
    groupName?: string;
    organizations?: Array<Organization>;
    profile?: GroupProfile;
    properties?: {
        [key: string]: Array<string>;
    };
    ref?: NodeRef;
    signupMethod?: 'simple' | 'password' | 'list';
}
