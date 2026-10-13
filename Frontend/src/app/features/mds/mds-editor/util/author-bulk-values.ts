import { Node } from 'ngx-edu-sharing-api';
import { VCard } from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import { Values } from '../../types/types';

/**
 * Writes the passed author parts of one node into `values`. `author` replaces only the node's first
 * person author (an empty one removes it), further authors are kept.
 */
export function applyAuthorValues(
    values: Values,
    node: Node | null,
    parts: { freetext?: string; author?: VCard },
): Values {
    if (parts.freetext !== undefined) {
        values[RestConstants.CCM_PROP_AUTHOR_FREETEXT] = [parts.freetext];
    }
    if (parts.author !== undefined) {
        const authors = [
            ...(node?.properties?.[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR] ?? []),
        ];
        if (parts.author.isValid()) {
            authors[0] = parts.author.toVCardString();
        } else {
            authors.splice(0, 1);
        }
        // the backend only removes a property for `[]`, `null` is ignored
        const hadAuthors =
            !!node?.properties?.[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR]?.length;
        values[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR] = authors.length
            ? authors
            : hadAuthors
            ? []
            : null;
    }
    return values;
}
