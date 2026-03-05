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
    | SubmissionWithAssignment;

export type NodeEntriesData = Omit<NodeEntries, 'nodes'> & {
    nodes: NodeEntriesDataType[];
};

export type SubmissionWithAssignment = Submission & {
    assignment: Assignment;
};
