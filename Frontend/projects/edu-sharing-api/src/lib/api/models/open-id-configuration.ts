/* tslint:disable */
/* eslint-disable */
import { LtiPlatformConfiguration } from './lti-platform-configuration';
export interface OpenIdConfiguration {
    authorization_endpoint?: string;
    claims_supported?: Array<string>;
    'https://purl.imsglobal.org/spec/lti-platform-configuration'?: LtiPlatformConfiguration;
    id_token_signing_alg_values_supported?: Array<string>;
    issuer?: string;
    jwks_uri?: string;
    registration_endpoint?: string;
    response_types_supported?: Array<string>;
    scopes_supported?: Array<string>;
    subject_types_supported?: Array<string>;
    token_endpoint?: string;
    token_endpoint_auth_methods_supported?: Array<string>;
    token_endpoint_auth_signing_alg_values_supported?: Array<string>;
}
