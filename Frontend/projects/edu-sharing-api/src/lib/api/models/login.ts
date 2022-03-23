/* tslint:disable */
/* eslint-disable */
import { LtiSession } from './lti-session';
import { RemoteAuthDescription } from './remote-auth-description';
export interface Login {
    authorityName?: string;
    currentScope: string;
    isAdmin: boolean;
    isGuest: boolean;
    isValidLogin: boolean;
    ltiSession?: LtiSession;
    remoteAuthentications?: {
        [key: string]: RemoteAuthDescription;
    };
    sessionTimeout: number;
    statusCode?: string;
    toolPermissions?: Array<string>;
    userHome?: string;
}
