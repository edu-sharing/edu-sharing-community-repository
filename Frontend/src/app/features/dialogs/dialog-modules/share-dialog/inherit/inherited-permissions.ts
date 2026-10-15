import { Ace, RestConstants } from 'ngx-edu-sharing-api';
import { UIHelper } from '../../../../../core-ui-module/ui-helper';
import type { ExtendedAce } from '../share-dialog.component';

/**
 * The permissions a child of the given node would inherit, i.e. the node's own (local) and its
 * inherited permissions, without owners and without the current user.
 *
 * @param permissions permissions of the parent node, as returned by `NodeService.getPermissions`
 * @param currentAuthorityName the current user, not listed since they keep access anyway
 */
export function getInheritedPermissions(
    permissions: { localPermissions?: { permissions?: Ace[] }; inheritedPermissions?: Ace[] },
    currentAuthorityName: string,
): ExtendedAce[] {
    const isNoOwner = (p: Ace) => p.authority?.authorityType !== RestConstants.AUTHORITY_TYPE_OWNER;
    const inherited = (permissions.inheritedPermissions ?? [])
        .filter(isNoOwner)
        .filter((p) => p.authority.authorityName !== currentAuthorityName);
    return UIHelper.mergePermissions(
        inherited as ExtendedAce[],
        (permissions.localPermissions?.permissions ?? []).filter(isNoOwner),
    ) as ExtendedAce[];
}
