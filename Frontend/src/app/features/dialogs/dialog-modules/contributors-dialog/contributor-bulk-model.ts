import { Node, RestConstants } from 'ngx-edu-sharing-api';
import { VCard } from 'ngx-edu-sharing-ui';
import { ContributorBucket, conflictingFields, contributorKey } from './contributor-identity';

export type { ContributorBucket };

/**
 * State of a contributor across the selected nodes. Mirrors the tri-state of the mds multivalue
 * widgets, where `all` corresponds to a joint value, `some` to an individual (indeterminate) one.
 */
export type ContributorState = 'all' | 'some' | 'removed';

export interface ContributorEntry {
    /** identity including bucket and role, see `contributorKey` */
    key: string;
    bucket: ContributorBucket;
    role: string;
    /** representative vCard, taken from the first node carrying this contributor */
    vCard: VCard;
    state: ContributorState;
    /** nodes that carry this contributor, in selection order */
    nodeIds: string[];
    /** the original vCard string per node, so untouched per-node details survive */
    vCardStrings: { [nodeId: string]: string };
    /** edited in this dialog session - the vCard is written to every node */
    edited: boolean;
    /** same contributor, but nodes disagree on details that are not part of the identity */
    hasConflicts: boolean;
}

export interface NodeValues {
    node: Node;
    values: { [property: string]: string[] };
}

export function contributorProperty(bucket: ContributorBucket, role: string): string {
    return bucket === 'lifecycle'
        ? RestConstants.CONTRIBUTOR_LIFECYCLE_PREFIX + role
        : RestConstants.CONTRIBUTOR_METADATA_PREFIX + role;
}

export function rolesOf(bucket: ContributorBucket): string[] {
    return bucket === 'lifecycle'
        ? RestConstants.CONTRIBUTOR_ROLES_LIFECYCLE
        : RestConstants.CONTRIBUTOR_ROLES_METADATA;
}

const BUCKETS: ContributorBucket[] = ['lifecycle', 'metadata'];

/**
 * Condenses the contributors of all given nodes into one list of entries.
 *
 * Equivalent to `MdsEditorInstanceService.Widget.calculateInitialValues()`: a contributor present
 * on every node becomes `all`, one present on a subset becomes `some` (shown as indeterminate).
 */
export function buildEntries(nodes: Node[]): ContributorEntry[] {
    const entries: ContributorEntry[] = [];
    const byKey = new Map<string, ContributorEntry>();
    for (const node of nodes) {
        for (const bucket of BUCKETS) {
            for (const role of rolesOf(bucket)) {
                const strings = node.properties?.[contributorProperty(bucket, role)] ?? [];
                for (const vCardString of strings) {
                    if (!vCardString) {
                        continue;
                    }
                    const vCard = new VCard(vCardString);
                    if (!vCard.isValid()) {
                        continue;
                    }
                    const key = contributorKey(vCard, bucket, role);
                    const existing = byKey.get(key);
                    if (existing) {
                        if (!existing.nodeIds.includes(node.ref.id)) {
                            existing.nodeIds.push(node.ref.id);
                            existing.vCardStrings[node.ref.id] = vCardString;
                        }
                        if (conflictingFields(existing.vCard, vCard).length) {
                            existing.hasConflicts = true;
                        }
                    } else {
                        const entry: ContributorEntry = {
                            key,
                            bucket,
                            role,
                            vCard,
                            state: 'all',
                            nodeIds: [node.ref.id],
                            vCardStrings: { [node.ref.id]: vCardString },
                            edited: false,
                            hasConflicts: false,
                        };
                        byKey.set(key, entry);
                        entries.push(entry);
                    }
                }
            }
        }
    }
    for (const entry of entries) {
        entry.state = entry.nodeIds.length === nodes.length ? 'all' : 'some';
    }
    return entries;
}

/** Creates an entry for a contributor the user just added. It applies to every node. */
export function createEntry(
    vCard: VCard,
    bucket: ContributorBucket,
    role: string,
): ContributorEntry {
    return {
        key: contributorKey(vCard, bucket, role),
        bucket,
        role,
        vCard,
        state: 'all',
        nodeIds: [],
        vCardStrings: {},
        edited: true,
        hasConflicts: false,
    };
}

/**
 * Removes a contributor from every node. Entries the user added in this session are dropped
 * right away, existing ones are kept as `removed` so the diff can delete them.
 */
export function removeEntry(entries: ContributorEntry[], entry: ContributorEntry): void {
    if (!entry.nodeIds.length) {
        const index = entries.indexOf(entry);
        if (index >= 0) {
            entries.splice(index, 1);
        }
        return;
    }
    entry.state = 'removed';
}

/**
 * Adds a contributor, or revives / unifies the matching one if it is already in the list. Without
 * this an entry that exists on a subset of the nodes would be duplicated when the user adds the
 * same person again instead of promoting the existing one.
 */
export function upsertEntry(
    entries: ContributorEntry[],
    vCard: VCard,
    bucket: ContributorBucket,
    role: string,
): ContributorEntry {
    const key = contributorKey(vCard, bucket, role);
    const existing = entries.find((entry) => entry.key === key);
    if (existing) {
        existing.vCard = vCard;
        existing.state = 'all';
        existing.edited = true;
        existing.hasConflicts = false;
        return existing;
    }
    const created = createEntry(vCard, bucket, role);
    entries.push(created);
    return created;
}

/**
 * Applies the result of the contributor edit dialog. Editing unifies the contributor across all
 * nodes. A changed role or name moves the contributor to another entry, since both are part of
 * its key.
 */
export function applyEdit(
    entries: ContributorEntry[],
    entry: ContributorEntry,
    vCard: VCard,
    role: string,
): ContributorEntry {
    if (entry.key === contributorKey(vCard, entry.bucket, role)) {
        entry.vCard = vCard;
        entry.state = 'all';
        entry.edited = true;
        entry.hasConflicts = false;
        return entry;
    }
    removeEntry(entries, entry);
    return upsertEntry(entries, vCard, entry.bucket, role);
}

/** The entries to render, in the order they should be written. */
export function visibleEntries(
    entries: ContributorEntry[],
    bucket: ContributorBucket,
    role: string,
): ContributorEntry[] {
    return entries.filter(
        (entry) => entry.bucket === bucket && entry.role === role && entry.state !== 'removed',
    );
}

/**
 * Computes what has to be written per node, containing only properties that actually change.
 *
 * The stored order of a node is the baseline: contributors the user removed are dropped, edited
 * ones are replaced in place, everything else stays untouched. Applied contributors the node does
 * not have yet are appended in dialog order. A different order between nodes is therefore never a
 * change on its own. This is the contributor equivalent of
 * `MdsEditorInstanceService.getNewPropertyValue()`: only what the user touched is written.
 */
export function buildNodeDiff(nodes: Node[], entries: ContributorEntry[]): NodeValues[] {
    const result: NodeValues[] = [];
    const byKey = new Map(entries.map((entry) => [entry.key, entry]));
    for (const node of nodes) {
        const values: { [property: string]: string[] } = {};
        for (const bucket of BUCKETS) {
            for (const role of rolesOf(bucket)) {
                const property = contributorProperty(bucket, role);
                const oldStrings = node.properties?.[property] ?? [];
                const next: string[] = [];
                const present = new Set<string>();
                for (const vCardString of oldStrings) {
                    const entry = vCardString
                        ? byKey.get(keyOfString(vCardString, bucket, role))
                        : undefined;
                    // unknown or unparsable values are kept untouched rather than dropped
                    if (!entry) {
                        next.push(vCardString);
                    } else if (entry.state === 'removed') {
                        continue;
                    } else if (entry.state === 'all' && entry.edited) {
                        // an explicit edit unifies the details; a repeated occurrence collapses
                        if (!present.has(entry.key)) {
                            next.push(entry.vCard.toVCardString());
                        }
                    } else {
                        next.push(vCardString);
                    }
                    if (entry) {
                        present.add(entry.key);
                    }
                }
                for (const entry of entries) {
                    if (
                        entry.bucket === bucket &&
                        entry.role === role &&
                        entry.state === 'all' &&
                        !present.has(entry.key)
                    ) {
                        next.push(entry.vCard.toVCardString());
                    }
                }
                if (!arrayIsEqual(next, oldStrings)) {
                    values[property] = next;
                }
            }
        }
        if (Object.keys(values).length) {
            result.push({ node, values });
        }
    }
    return result;
}

function keyOfString(vCardString: string, bucket: ContributorBucket, role: string): string {
    return contributorKey(new VCard(vCardString), bucket, role);
}

function arrayIsEqual(a: string[], b: string[]): boolean {
    return a.length === b.length && a.every((value, index) => value === b[index]);
}
