import { Assignment, GenericAuthority, Node, NodeEntries, Submission } from 'ngx-edu-sharing-api';

export type NodeEntriesDataType = Node | GenericAuthority | Assignment | Submission;

export type NodeEntriesData = Omit<NodeEntries, 'nodes'> & {
    nodes: NodeEntriesDataType[];
};
