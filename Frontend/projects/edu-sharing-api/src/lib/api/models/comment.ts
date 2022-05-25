/* tslint:disable */
/* eslint-disable */
import { NodeRef } from './node-ref';
import { UserSimple } from './user-simple';
export interface Comment {
    comment?: string;
    created?: number;
    creator?: UserSimple;
    ref?: NodeRef;
    replyTo?: NodeRef;
}
