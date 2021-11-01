/* tslint:disable */
/* eslint-disable */
import { Value } from './value';
export interface Facet {
    property: string;
    sumOtherDocCount?: number;
    values: Array<Value>;
}
