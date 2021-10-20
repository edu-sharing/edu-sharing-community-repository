/* tslint:disable */
/* eslint-disable */
import { Key } from './key';
export interface JobDetail {
    description?: string;
    durable?: boolean;
    fullName?: string;
    group?: string;
    jobDataMap?: {
        [key: string]: {};
    };
    jobListenerNames?: Array<string>;
    key?: Key;
    name?: string;
    stateful?: boolean;
    volatile?: boolean;
}
