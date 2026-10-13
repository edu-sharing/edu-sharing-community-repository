import { VCard } from 'ngx-edu-sharing-ui';
import {
    applyEdit,
    buildEntries,
    buildNodeDiff,
    ContributorEntry,
    contributorProperty,
    createEntry,
    removeEntry,
    upsertEntry,
} from './contributor-bulk-model';
import { contributorIdentity } from './contributor-identity';
import {
    ACME,
    ADA,
    ALAN,
    conflictingNodes,
    GRACE,
    mixedNodes,
    nodeWith,
    organization,
    person,
    singleNode,
} from './contributor-fixtures';

const AUTHORS = contributorProperty('lifecycle', 'author');
const PUBLISHERS = contributorProperty('lifecycle', 'publisher');
const CREATORS = contributorProperty('metadata', 'creator');

function find(entries: ContributorEntry[], vCard: VCard, role = 'author'): ContributorEntry {
    return entries.find(
        (entry) => entry.role === role && entry.key.endsWith(contributorIdentity(vCard)),
    );
}

describe('contributorIdentity', () => {
    it('prefers the orcid over uid and name', () => {
        const vCard = person({ givenname: 'Ada', surname: 'Lovelace', orcid: '0000-1' });
        vCard.uid = 'some-uid';
        expect(contributorIdentity(vCard)).toBe('orcid:0000-1');
    });

    it('falls back to the uid when no persistent id is set', () => {
        const vCard = person({ givenname: 'Ada', surname: 'Lovelace' });
        vCard.uid = 'some-uid';
        expect(contributorIdentity(vCard)).toBe('uid:some-uid');
    });

    it('falls back to the normalized name', () => {
        expect(contributorIdentity(person({ givenname: ' Ada ', surname: 'Lovelace' }))).toBe(
            'name:lovelace|ada',
        );
    });

    it('uses the organization name for organizations', () => {
        expect(contributorIdentity(organization('ACME  GmbH'))).toBe('org:acme gmbh');
    });

    it('ignores the contribute date, so the same person matches across nodes', () => {
        const a = person({ givenname: 'Ada', surname: 'Lovelace', contributeDate: '2024-01-01' });
        const b = person({ givenname: 'Ada', surname: 'Lovelace', contributeDate: '2020-05-05' });
        expect(contributorIdentity(a)).toBe(contributorIdentity(b));
    });
});

describe('buildEntries', () => {
    it('marks a contributor present on every node as "all"', () => {
        const entries = buildEntries(mixedNodes());
        expect(find(entries, ADA).state).toBe('all');
        expect(find(entries, ADA).nodeIds).toEqual(['node-1', 'node-2']);
    });

    it('marks a contributor present on a subset as "some"', () => {
        const entries = buildEntries(mixedNodes());
        expect(find(entries, GRACE).state).toBe('some');
        expect(find(entries, GRACE).nodeIds).toEqual(['node-1']);
        expect(find(entries, ALAN).nodeIds).toEqual(['node-2']);
    });

    it('keeps roles apart, even for the same person', () => {
        const entries = buildEntries([
            nodeWith('node-1', { 'lifecycle:author': [ADA] }),
            nodeWith('node-2', { 'lifecycle:publisher': [ADA] }),
        ]);
        expect(entries.length).toBe(2);
        expect(entries.every((entry) => entry.state === 'some')).toBe(true);
    });

    it('flags entries whose details differ between nodes', () => {
        const entries = buildEntries(conflictingNodes());
        expect(entries.length).toBe(1);
        expect(entries[0].state).toBe('all');
        expect(entries[0].hasConflicts).toBe(true);
    });

    it('skips invalid vCards', () => {
        const node = nodeWith('node-1', {});
        node.properties[AUTHORS] = ['BEGIN:VCARD\nVERSION:3.0\nEND:VCARD', null];
        expect(buildEntries([node])).toEqual([]);
    });
});

describe('buildNodeDiff', () => {
    it('writes nothing when nothing was touched', () => {
        const nodes = mixedNodes();
        expect(buildNodeDiff(nodes, buildEntries(nodes))).toEqual([]);
    });

    it('writes nothing for a single node that was not touched', () => {
        const nodes = singleNode();
        expect(buildNodeDiff(nodes, buildEntries(nodes))).toEqual([]);
    });

    it('leaves untouched partial contributors alone on every node', () => {
        const nodes = mixedNodes();
        const entries = buildEntries(nodes);
        removeEntry(entries, find(entries, ADA));
        const diff = buildNodeDiff(nodes, entries);
        // Ada is gone everywhere, Grace and Alan stay where they were
        expect(diff.length).toBe(2);
        expect(diff[0].values[AUTHORS]).toEqual([GRACE.toVCardString()]);
        expect(diff[1].values[AUTHORS]).toEqual([ALAN.toVCardString()]);
        // untouched buckets are not part of the diff
        expect(diff[0].values[CREATORS]).toBeUndefined();
    });

    it('adds a promoted contributor only where it is missing and keeps per-node details', () => {
        const nodes = [
            nodeWith('node-1', { 'lifecycle:author': [ADA] }),
            nodeWith('node-2', { 'lifecycle:author': [GRACE] }),
        ];
        const entries = buildEntries(nodes);
        find(entries, ADA).state = 'all';
        const diff = buildNodeDiff(nodes, entries);
        // node-1 already has Ada with its own contribute date - nothing changes there
        expect(diff.length).toBe(1);
        expect(diff[0].node.ref.id).toBe('node-2');
        expect(diff[0].values[AUTHORS]).toEqual([GRACE.toVCardString(), ADA.toVCardString()]);
    });

    it('unifies a contributor across all nodes once it was edited', () => {
        const nodes = conflictingNodes();
        const entries = buildEntries(nodes);
        const edited = person({
            givenname: 'Ada',
            surname: 'Lovelace',
            orcid: '0000-0002-1825-0097',
            email: 'unified@example.org',
        });
        applyEdit(entries, entries[0], edited, 'author');
        const diff = buildNodeDiff(nodes, entries);
        expect(diff.length).toBe(2);
        expect(diff[0].values[AUTHORS]).toEqual([edited.toVCardString()]);
        expect(diff[1].values[AUTHORS]).toEqual([edited.toVCardString()]);
    });

    it('moves a contributor to the new property when the role changed', () => {
        const nodes = [nodeWith('node-1', { 'lifecycle:author': [ADA] })];
        const entries = buildEntries(nodes);
        applyEdit(entries, entries[0], ADA, 'publisher');
        const diff = buildNodeDiff(nodes, entries);
        expect(diff[0].values[AUTHORS]).toEqual([]);
        expect(diff[0].values[PUBLISHERS]).toEqual([ADA.toVCardString()]);
    });

    it('adds a newly created contributor to every node', () => {
        const nodes = mixedNodes();
        const entries = buildEntries(nodes);
        entries.push(createEntry(ALAN, 'metadata', 'creator'));
        const diff = buildNodeDiff(nodes, entries);
        expect(diff.length).toBe(2);
        expect(diff[0].values[CREATORS]).toEqual([ACME.toVCardString(), ALAN.toVCardString()]);
        expect(diff[1].values[CREATORS]).toEqual([ALAN.toVCardString()]);
    });

    it('drops a newly created contributor that was removed again', () => {
        const nodes = mixedNodes();
        const entries = buildEntries(nodes);
        const created = createEntry(ALAN, 'metadata', 'creator');
        entries.push(created);
        removeEntry(entries, created);
        expect(entries).not.toContain(created);
        expect(buildNodeDiff(nodes, entries)).toEqual([]);
    });

    it("keeps the node's order and appends new contributors", () => {
        const nodes = [
            nodeWith('node-1', { 'lifecycle:author': [GRACE, ADA] }),
            nodeWith('node-2', { 'lifecycle:author': [ADA] }),
        ];
        const entries = buildEntries(nodes);
        entries.push(createEntry(ALAN, 'lifecycle', 'author'));
        const diff = buildNodeDiff(nodes, entries);
        const node1 = diff.find((entry) => entry.node.ref.id === 'node-1');
        expect(node1.values[AUTHORS]).toEqual([
            GRACE.toVCardString(),
            ADA.toVCardString(),
            ALAN.toVCardString(),
        ]);
    });

    it('does not treat a different order between nodes as a change', () => {
        const nodes = [
            nodeWith('node-1', { 'lifecycle:author': [ADA, GRACE, ALAN] }),
            nodeWith('node-2', { 'lifecycle:author': [ALAN, ADA, GRACE] }),
        ];
        expect(buildNodeDiff(nodes, buildEntries(nodes))).toEqual([]);
    });

    it('keeps the order of the remaining contributors when one in the middle is removed', () => {
        const nodes = [nodeWith('node-1', { 'lifecycle:author': [ALAN, ADA, GRACE] })];
        const entries = buildEntries(nodes);
        removeEntry(entries, find(entries, ADA));
        const diff = buildNodeDiff(nodes, entries);
        expect(diff[0].values[AUTHORS]).toEqual([ALAN.toVCardString(), GRACE.toVCardString()]);
    });

    it('replaces an edited contributor at its position', () => {
        const nodes = [nodeWith('node-1', { 'lifecycle:author': [GRACE, ADA] })];
        const entries = buildEntries(nodes);
        const edited = person({
            givenname: 'Ada',
            surname: 'Lovelace',
            orcid: '0000-0002-1825-0097',
            email: 'unified@example.org',
        });
        applyEdit(entries, find(entries, ADA), edited, 'author');
        const diff = buildNodeDiff(nodes, entries);
        expect(diff[0].values[AUTHORS]).toEqual([GRACE.toVCardString(), edited.toVCardString()]);
    });

    it('does not rewrite a node that stores the same contributor twice', () => {
        const nodes = [nodeWith('node-1', { 'lifecycle:author': [ADA, GRACE, ADA] })];
        expect(buildNodeDiff(nodes, buildEntries(nodes))).toEqual([]);
    });

    it('promotes the existing entry instead of duplicating it when the same person is added', () => {
        const nodes = mixedNodes();
        const entries = buildEntries(nodes);
        const before = entries.length;
        upsertEntry(entries, GRACE, 'lifecycle', 'author');
        expect(entries.length).toBe(before);
        expect(find(entries, GRACE).state).toBe('all');
        const diff = buildNodeDiff(nodes, entries);
        // only node-2 is missing Grace
        expect(diff.length).toBe(1);
        expect(diff[0].node.ref.id).toBe('node-2');
    });

    it('revives a removed contributor when it is added again', () => {
        const nodes = mixedNodes();
        const entries = buildEntries(nodes);
        removeEntry(entries, find(entries, ADA));
        upsertEntry(entries, ADA, 'lifecycle', 'author');
        expect(find(entries, ADA).state).toBe('all');
        expect(buildNodeDiff(nodes, entries)).toEqual([]);
    });

    it('keeps values it cannot parse instead of dropping them', () => {
        const node = nodeWith('node-1', { 'lifecycle:author': [ADA] });
        node.properties[AUTHORS] = [...node.properties[AUTHORS], 'not-a-vcard'];
        const entries = buildEntries([node]);
        entries.push(createEntry(GRACE, 'lifecycle', 'author'));
        const diff = buildNodeDiff([node], entries);
        expect(diff[0].values[AUTHORS]).toContain('not-a-vcard');
    });
});
