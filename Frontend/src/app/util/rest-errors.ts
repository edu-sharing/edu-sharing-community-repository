import { RestConstants } from 'ngx-edu-sharing-api';

/** i18n key of the hint shown when a node name collides with an existing one */
export const DUPLICATE_NODE_NAME_ERROR = 'MDS.ERROR_DUPLICATE_NODE_NAME';

/**
 * Whether the given api error is a name conflict, i.e. a sibling node in the same folder already
 * uses the requested `cm:name`. The backend answers such a case with a `409` carrying a
 * `DAODuplicateNodeNameException`.
 */
export function isDuplicateNodeNameError(error: any): boolean {
    return (
        error?.status === RestConstants.DUPLICATE_NODE_RESPONSE ||
        !!error?.error?.error?.endsWith?.('DAODuplicateNodeNameException')
    );
}
