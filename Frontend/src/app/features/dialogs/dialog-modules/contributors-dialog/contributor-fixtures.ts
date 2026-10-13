import { Node } from 'ngx-edu-sharing-api';
import { VCard } from 'ngx-edu-sharing-ui';
import { buildEntries, ContributorEntry, contributorProperty } from './contributor-bulk-model';

/**
 * Sample data shared by the unit tests and the storybook stories, so both describe the same
 * situations.
 */

export interface PersonOptions {
    givenname: string;
    surname: string;
    orcid?: string;
    email?: string;
    contributeDate?: string;
}

export function person({ givenname, surname, orcid, email, contributeDate }: PersonOptions): VCard {
    const vCard = new VCard();
    vCard.givenname = givenname;
    vCard.surname = surname;
    if (orcid) {
        vCard.orcid = orcid;
    }
    if (email) {
        vCard.email = email;
    }
    if (contributeDate) {
        vCard.contributeDate = contributeDate;
    }
    return vCard;
}

export function organization(org: string, ror?: string): VCard {
    const vCard = new VCard();
    vCard.org = org;
    if (ror) {
        vCard.ror = ror;
    }
    return vCard;
}

/** A node stub carrying the given contributors, e.g. `{ 'lifecycle:author': [vCard] }`. */
export function nodeWith(
    id: string,
    contributors: { [bucketAndRole: string]: VCard[] },
    name = id,
): Node {
    const properties: { [property: string]: string[] } = {};
    for (const [bucketAndRole, vCards] of Object.entries(contributors)) {
        const [bucket, role] = bucketAndRole.split(':');
        properties[contributorProperty(bucket as 'lifecycle' | 'metadata', role)] = vCards.map(
            (vCard) => vCard.toVCardString(),
        );
    }
    return {
        ref: { id, repo: '-home-' },
        name,
        title: name,
        properties,
    } as unknown as Node;
}

export const ADA = person({
    givenname: 'Ada',
    surname: 'Lovelace',
    orcid: '0000-0002-1825-0097',
    contributeDate: '2024-01-15T00:00:00.000Z',
});
export const GRACE = person({ givenname: 'Grace', surname: 'Hopper' });
export const ALAN = person({ givenname: 'Alan', surname: 'Turing' });
export const ACME = organization('ACME Bildungsverlag GmbH', 'https://ror.org/0abcdef12');

/** Same person as `ADA` (same ORCID) but with details that differ per node. */
export const ADA_OTHER_DETAILS = person({
    givenname: 'Ada',
    surname: 'Lovelace',
    orcid: '0000-0002-1825-0097',
    email: 'ada@example.org',
    contributeDate: '2023-06-01T00:00:00.000Z',
});

/** Two nodes sharing an author, each with one author of its own. */
export function mixedNodes(): Node[] {
    return [
        nodeWith('node-1', { 'lifecycle:author': [ADA, GRACE], 'metadata:creator': [ACME] }),
        nodeWith('node-2', { 'lifecycle:author': [ADA, ALAN] }),
    ];
}

export function singleNode(): Node[] {
    return [nodeWith('node-1', { 'lifecycle:author': [ADA, GRACE], 'metadata:creator': [ACME] })];
}

export function conflictingNodes(): Node[] {
    return [
        nodeWith('node-1', { 'lifecycle:author': [ADA] }),
        nodeWith('node-2', { 'lifecycle:author': [ADA_OTHER_DETAILS] }),
    ];
}

/** Three nodes without any contributor in common. */
export function noOverlapNodes(): Node[] {
    return [
        nodeWith('node-1', { 'lifecycle:author': [ADA] }),
        nodeWith('node-2', { 'lifecycle:author': [GRACE] }),
        nodeWith('node-3', { 'lifecycle:publisher': [ACME] }),
    ];
}

export function entriesOf(nodes: Node[]): ContributorEntry[] {
    return buildEntries(nodes);
}
