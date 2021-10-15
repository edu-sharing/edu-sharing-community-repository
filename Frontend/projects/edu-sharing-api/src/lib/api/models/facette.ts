/* tslint:disable */
/* eslint-disable */
import { Value } from './value';
export interface Facette {
    property: string;
    sumOtherDocCount?: number;
    values: Array<Value>;
}
