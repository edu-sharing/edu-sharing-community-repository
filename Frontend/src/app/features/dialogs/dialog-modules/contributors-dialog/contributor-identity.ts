import { VCard } from 'ngx-edu-sharing-ui';

export type ContributorBucket = 'lifecycle' | 'metadata';

/**
 * Builds a stable identity for a contributor.
 *
 * Comparing the raw vCard strings is not an option: field order, whitespace and the contribute
 * date differ between nodes although the very same person is meant. The cascade below picks the
 * most reliable available discriminator instead:
 *
 * 1. persistent ids (these are also the fields that lock editing in the contributor edit dialog)
 * 2. the vCard uid
 * 3. the normalized name
 *
 * Fields that regularly differ per node (contribute date, address, mail, phone) are deliberately
 * not part of the identity - they would produce duplicates instead of one shared entry.
 */
export function contributorIdentity(vCard: VCard): string {
    if (vCard.orcid) {
        return 'orcid:' + normalize(vCard.orcid);
    }
    if (vCard.gnduri) {
        return 'gnd:' + normalize(vCard.gnduri);
    }
    if (vCard.ror) {
        return 'ror:' + normalize(vCard.ror);
    }
    if (vCard.wikidata) {
        return 'wikidata:' + normalize(vCard.wikidata);
    }
    if (vCard.uid) {
        return 'uid:' + normalize(vCard.uid);
    }
    if (vCard.getType() === VCard.TYPE_ORG) {
        return 'org:' + normalize(vCard.org);
    }
    return 'name:' + normalize(vCard.surname) + '|' + normalize(vCard.givenname);
}

/**
 * The key a contributor is grouped by. The role is part of it since the same person can be
 * author on one node and publisher on another - these are two distinct entries.
 */
export function contributorKey(vCard: VCard, bucket: ContributorBucket, role: string): string {
    return `${bucket}:${role}:${contributorIdentity(vCard)}`;
}

/**
 * Fields that are not part of the identity but are shown to the user. If two nodes disagree on
 * any of them, the entry is marked as conflicting.
 */
export function conflictingFields(a: VCard, b: VCard): string[] {
    const fields: { name: string; read: (vCard: VCard) => string }[] = [
        { name: 'title', read: (v) => v.title },
        { name: 'givenname', read: (v) => v.givenname },
        { name: 'surname', read: (v) => v.surname },
        { name: 'org', read: (v) => v.org },
        { name: 'email', read: (v) => v.email },
        { name: 'url', read: (v) => v.url },
        { name: 'orgPhone', read: (v) => v.orgPhone },
        { name: 'street', read: (v) => v.street },
        { name: 'city', read: (v) => v.city },
        { name: 'zip', read: (v) => v.zip },
        { name: 'country', read: (v) => v.country },
        { name: 'contributeDate', read: (v) => v.contributeDate },
    ];
    return fields
        .filter(({ read }) => normalize(read(a)) !== normalize(read(b)))
        .map(({ name }) => name);
}

function normalize(value: string): string {
    return (value ?? '').trim().replace(/\s+/g, ' ').toLowerCase();
}
