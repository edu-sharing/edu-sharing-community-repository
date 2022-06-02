import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Group, Permission, RestConstants, User } from '../../core-module/core.module';

@Pipe({ name: 'authorityName' })
export class AuthorityNamePipe implements PipeTransform {
    constructor(private translate: TranslateService) {}

    transform(authority: Permission | User | Group | any, args: string[] = null): string {
        if (!authority) return 'invalid';
        if (authority.profile?.displayName) return authority.profile.displayName;
        if (authority.profile?.firstName || authority.profile?.lastName)
            return authority.profile.firstName + ' ' + authority.profile.lastName;
        if (authority.user?.firstName || authority.user?.lastName) {
            return authority.user.firstName + ' ' + authority.user.lastName;
        }
        if (authority.group?.displayName) {
            return authority.group.displayName;
        }
        if (authority.displayName) {
            return authority.displayName;
        }
        if (authority.firstName || authority.lastName) {
            return authority.firstName + ' ' + authority.lastName;
        }
        // for Permissions, the authority is encapsulated
        if (authority.authority) {
            authority = authority.authority;
        }
        if (authority.authorityType === RestConstants.AUTHORITY_TYPE_EVERYONE) {
            return this.translate.instant('GROUP_EVERYONE');
        }
        if (authority.authorityName) {
            if (authority.authorityName?.startsWith(RestConstants.AUTHORITY_DELETED_USER)) {
                // we could also add the date of the deletion (DELETED_USER_<timestamp>), but do we want that?
                return this.translate.instant('DELETED_USER');
            }
            return authority.authorityName;
        }
        return 'invalid';
    }
}
