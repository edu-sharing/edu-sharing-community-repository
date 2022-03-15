/* tslint:disable */
/* eslint-disable */
import { Node } from './node';
import { RelationData } from './relation-data';
export interface NodeRelation {
    node?: Node;
    relations?: Array<RelationData>;
}
