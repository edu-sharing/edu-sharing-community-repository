/* tslint:disable */
/* eslint-disable */
import { RemoteAuthDescription } from './remote-auth-description';
import { LtiSession } from './lti-session';
export interface Login {
    authorityName?: string;
    currentScope: string;
    isAdmin: boolean;
    isGuest: boolean;
    isValidLogin: boolean;
    remoteAuthentications?: {
        [key: string]: RemoteAuthDescription;
    };
    sessionTimeout: number;
    statusCode?: string;
    toolPermissions?: Array<string>;
    userHome?: string;
    ltiSession?: LtiSession;
}
