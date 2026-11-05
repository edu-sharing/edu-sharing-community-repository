import { ScopeField } from '../api/models/scope-field';
import { ScopeInfo } from '../api/models/scope-info';

export type ScopeRole = NonNullable<ScopeInfo['role']>;
export type ScopeFieldVisibility = NonNullable<ScopeField['visibility']>;

/**
 * Scope block of a node (scopes concept §9, package F5): properties of the own scope for editors,
 * public non-replacing properties of published entries of other scopes for everyone.
 *
 * TODO: not part of the openapi yet — replace by the generated model (and `Node.scopes`) once F5
 * is deployed and the client is regenerated.
 */
export interface NodeScope {
    displayName?: string;
    excluded?: boolean;
    /** original values of the fields replaced inside the active scope (editors only) */
    originals?: {
        [key: string]: Array<string>;
    };
    properties?: {
        [key: string]: Array<string>;
    };
    published?: boolean;
    scopeId?: string;
}

/** a node as delivered with the scope block (see {@link NodeScope}) */
export interface NodeWithScopes {
    scopes?: NodeScope[];
}
