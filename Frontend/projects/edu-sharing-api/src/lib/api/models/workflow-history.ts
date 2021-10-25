/* tslint:disable */
/* eslint-disable */
import { Authority } from './authority';
import { UserSimple } from './user-simple';
export interface WorkflowHistory {
    comment?: string;
    editor?: UserSimple;
    receiver?: Array<Authority>;
    status?: string;
    time?: number;
}
