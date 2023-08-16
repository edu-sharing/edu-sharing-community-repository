export interface WorkflowDefinition {
    id: string;
    color: string;
    hasReceiver: boolean;
    next: string[];
}

export const WORKFLOW_STATUS_UNCHECKED: WorkflowDefinition = {
    id: '100_unchecked',
    color: '#539DD5',
    hasReceiver: true,
    next: null,
};
export const WORKFLOW_STATUS_TO_CHECK: WorkflowDefinition = {
    id: '200_tocheck',
    color: '#3CB0B0',
    hasReceiver: true,
    next: null,
};
export const WORKFLOW_STATUS_HASFLAWS: WorkflowDefinition = {
    id: '300_hasflaws',
    color: '#D58553',
    hasReceiver: true,
    next: null,
};
export const WORKFLOW_STATUS_CHECKED: WorkflowDefinition = {
    id: '400_checked',
    color: '#42A053',
    hasReceiver: false,
    next: null,
};
export type WorkflowDefinitionStatus = {
    current: WorkflowDefinition;
    initial: WorkflowDefinition;
};
