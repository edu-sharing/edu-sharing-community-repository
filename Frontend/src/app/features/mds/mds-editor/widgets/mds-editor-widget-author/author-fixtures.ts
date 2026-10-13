import { Node } from 'ngx-edu-sharing-api';
import { VCard } from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';
import { person } from '../../../../dialogs/dialog-modules/contributors-dialog/contributor-fixtures';
import { DummyNode, registerMockNode } from '../../storybook-utils';

export const ADA = person({ givenname: 'Ada', surname: 'Lovelace' });
export const GRACE = person({ givenname: 'Grace', surname: 'Hopper' });
export const ALAN = person({ givenname: 'Alan', surname: 'Turing' });

export interface AuthorNodeOptions {
    freetext?: string;
    authors?: VCard[];
}

/** A node with only the given author fields, registered for the storybook node service. */
export function authorNode(id: string, { freetext, authors }: AuthorNodeOptions = {}): Node {
    const node = {
        ...DummyNode,
        ref: { ...DummyNode.ref, id },
        name: id,
        title: id,
        properties: {},
    } as Node;
    if (freetext !== undefined) {
        node.properties[RestConstants.CCM_PROP_AUTHOR_FREETEXT] = [freetext];
    }
    if (authors?.length) {
        node.properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR] = authors.map((a) =>
            a.toVCardString(),
        );
    }
    registerMockNode(node);
    return node;
}

export function singleAuthorNode(): Node[] {
    return [authorNode('author-single', { freetext: 'Free text', authors: [ADA] })];
}

export function sameFreetextNodes(): Node[] {
    return [
        authorNode('author-same-1', { freetext: 'Shared author text' }),
        authorNode('author-same-2', { freetext: 'Shared author text' }),
    ];
}

export function mixedAuthorNodes(): Node[] {
    return [
        authorNode('author-mixed-1', { freetext: 'First text', authors: [ADA] }),
        authorNode('author-mixed-2', { freetext: 'Second text', authors: [GRACE] }),
    ];
}

export function furtherAuthorsNodes(): Node[] {
    return [
        authorNode('author-further-1', { authors: [ADA, GRACE] }),
        authorNode('author-further-2', { authors: [ALAN] }),
    ];
}
