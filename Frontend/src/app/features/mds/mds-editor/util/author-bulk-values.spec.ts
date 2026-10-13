import { Node } from 'ngx-edu-sharing-api';
import { VCard } from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import { applyAuthorValues } from './author-bulk-values';

const FREETEXT = RestConstants.CCM_PROP_AUTHOR_FREETEXT;
const AUTHOR = RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR;

function vCard(givenname: string, surname: string): VCard {
    const result = new VCard();
    result.givenname = givenname;
    result.surname = surname;
    return result;
}

function nodeWithAuthors(...authors: VCard[]): Node {
    return {
        properties: { [AUTHOR]: authors.map((a) => a.toVCardString()) },
    } as unknown as Node;
}

describe('applyAuthorValues', () => {
    it('writes only the freetext if only the freetext is passed', () => {
        const values = applyAuthorValues({}, nodeWithAuthors(vCard('Ada', 'Lovelace')), {
            freetext: 'Some text',
        });
        expect(values[FREETEXT]).toEqual(['Some text']);
        expect(AUTHOR in values).toBe(false);
    });

    it('writes an empty freetext when it was cleared', () => {
        expect(applyAuthorValues({}, null, { freetext: '' })[FREETEXT]).toEqual(['']);
    });

    it('replaces only the first author and keeps the further ones of the node', () => {
        const node = nodeWithAuthors(vCard('Ada', 'Lovelace'), vCard('Grace', 'Hopper'));
        const values = applyAuthorValues({}, node, { author: vCard('Alan', 'Turing') });
        expect(FREETEXT in values).toBe(false);
        expect(values[AUTHOR].length).toBe(2);
        expect(new VCard(values[AUTHOR][0]).surname).toBe('Turing');
        expect(new VCard(values[AUTHOR][1]).surname).toBe('Hopper');
    });

    it('does not modify the properties of the node', () => {
        const node = nodeWithAuthors(vCard('Ada', 'Lovelace'));
        const before = [...node.properties[AUTHOR]];
        applyAuthorValues({}, node, { author: vCard('Alan', 'Turing') });
        expect(node.properties[AUTHOR]).toEqual(before);
    });

    it('applies the same author to nodes with different further authors', () => {
        const one = nodeWithAuthors(vCard('Ada', 'Lovelace'));
        const two = nodeWithAuthors(vCard('Grace', 'Hopper'), vCard('Edsger', 'Dijkstra'));
        const author = vCard('Alan', 'Turing');
        expect(applyAuthorValues({}, one, { author })[AUTHOR].length).toBe(1);
        const result = applyAuthorValues({}, two, { author })[AUTHOR];
        expect(result.length).toBe(2);
        expect(new VCard(result[1]).surname).toBe('Dijkstra');
    });

    it('adds the author to a node without authors', () => {
        const values = applyAuthorValues({}, { properties: {} } as Node, {
            author: vCard('Ada', 'Lovelace'),
        });
        expect(values[AUTHOR].length).toBe(1);
        expect(new VCard(values[AUTHOR][0]).surname).toBe('Lovelace');
    });

    it('writes an empty list if the only author is removed, as null would not remove it', () => {
        const node = nodeWithAuthors(vCard('Ada', 'Lovelace'));
        expect(applyAuthorValues({}, node, { author: new VCard() })[AUTHOR]).toEqual([]);
    });

    it('writes null if there is no author to remove', () => {
        expect(
            applyAuthorValues({}, { properties: {} } as Node, { author: new VCard() })[AUTHOR],
        ).toBeNull();
        expect(applyAuthorValues({}, null, { author: new VCard() })[AUTHOR]).toBeNull();
    });

    it('removes the first entry without leaving a hole when there are further authors', () => {
        const node = nodeWithAuthors(vCard('Ada', 'Lovelace'), vCard('Grace', 'Hopper'));
        const result = applyAuthorValues({}, node, { author: new VCard() })[AUTHOR];
        expect(result.length).toBe(1);
        expect(0 in result).toBe(true);
        expect(new VCard(result[0]).surname).toBe('Hopper');
    });

    it('works without a node', () => {
        const values = applyAuthorValues({}, null, {
            freetext: 'x',
            author: vCard('Ada', 'Lovelace'),
        });
        expect(values[FREETEXT]).toEqual(['x']);
        expect(values[AUTHOR].length).toBe(1);
    });

    it('writes nothing if nothing is passed', () => {
        expect(applyAuthorValues({}, nodeWithAuthors(vCard('Ada', 'Lovelace')), {})).toEqual({});
    });
});
