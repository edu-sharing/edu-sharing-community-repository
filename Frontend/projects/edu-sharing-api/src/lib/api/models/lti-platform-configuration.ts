/* tslint:disable */
/* eslint-disable */
import { Message } from './message';
export interface LtiPlatformConfiguration {
    messages_supported?: Array<Message>;
    product_family_code?: string;
    variables?: Array<string>;
    version?: string;
}
