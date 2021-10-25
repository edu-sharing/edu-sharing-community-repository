/* tslint:disable */
/* eslint-disable */
import { Node } from './node';
import { UserSimple } from './user-simple';
export interface StreamEntry {
    author?: UserSimple;
    created?: number;
    description?: string;
    id?: string;
    modified?: number;
    nodes?: Array<Node>;
    priority?: number;
    properties?: {
        [key: string]: {};
    };
}
