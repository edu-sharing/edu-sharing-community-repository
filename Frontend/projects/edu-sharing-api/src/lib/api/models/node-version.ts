/* tslint:disable */
/* eslint-disable */
import { NodeVersionRef } from './node-version-ref';
import { Person } from './person';
export interface NodeVersion {
    comment: string;
    contentUrl?: string;
    modifiedAt: string;
    modifiedBy: Person;
    properties?: {
        [key: string]: Array<string>;
    };
    version: NodeVersionRef;
}
