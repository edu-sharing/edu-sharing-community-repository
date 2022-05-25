/* tslint:disable */
/* eslint-disable */
export interface UserProfileAppAuth {
    about?: string;
    avatar?: string;
    email?: string;
    extendedAttributes?: {
        [key: string]: Array<string>;
    };
    firstName?: string;
    lastName?: string;
    primaryAffiliation?: string;
    skills?: Array<string>;
    type?: Array<string>;
    types?: Array<string>;
    vcard?: string;
}
