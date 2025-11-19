import {
    Assignment,
    AssignmentFile,
    GenericAuthority,
    Node,
    NodeEntries,
    Submission,
} from 'ngx-edu-sharing-api';

export type NodeEntriesDataType =
    | Node
    | GenericAuthority
    | Assignment
    | AssignmentFile
    | Submission;

export type NodeEntriesData = Omit<NodeEntries, 'nodes'> & {
    nodes: NodeEntriesDataType[];
};
