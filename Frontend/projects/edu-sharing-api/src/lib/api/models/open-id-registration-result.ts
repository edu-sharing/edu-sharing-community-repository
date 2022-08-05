/* tslint:disable */
/* eslint-disable */
import { LtiToolConfiguration } from './lti-tool-configuration';
export interface OpenIdRegistrationResult {
    application_type?: string;
    client_id?: string;
    client_name?: string;
    grant_types?: Array<string>;
    'https://purl.imsglobal.org/spec/lti-tool-configuration'?: LtiToolConfiguration;
    initiate_login_uri?: string;
    jwks_uri?: string;
    logo_uri?: string;
    redirect_uris?: Array<string>;
    response_types?: Array<string>;
    scope?: string;
    token_endpoint_auth_method?: string;
}
