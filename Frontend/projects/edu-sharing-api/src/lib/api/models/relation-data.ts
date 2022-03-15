/* tslint:disable */
/* eslint-disable */
import { Node } from './node';
import { User } from './user';
export interface RelationData {
    creator?: User;
    node?: Node;
    timestamp?: string;
    type?: 'isPartOf' | 'isBasedOn' | 'references' | 'hasPart' | 'isBasisFor';
}
