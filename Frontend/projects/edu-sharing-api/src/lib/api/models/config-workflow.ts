/* tslint:disable */
/* eslint-disable */
import { ConfigWorkflowList } from './config-workflow-list';
export interface ConfigWorkflow {
    commentRequired?: boolean;
    defaultReceiver?: string;
    defaultStatus?: string;
    workflows?: Array<ConfigWorkflowList>;
}
