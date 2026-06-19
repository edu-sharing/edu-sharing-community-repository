import { Pipe, PipeTransform, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import {
    AuthorityProfile,
    ConfigurationService,
    Group,
    Organization,
    User,
} from '../../core-module/core.module';

@Pipe({
    name: 'permissionName',
    standalone: false,
})
export class PermissionNamePipe implements PipeTransform {
    private translate = inject(TranslateService);
    private config = inject(ConfigurationService);

    transform(
        permission: Organization | AuthorityProfile | Group | User | any,
        args?: any,
    ): string {
        if (args && args['field']) {
            let field = args['field'];
            if (field == 'secondary') {
                if (permission.authorityType === 'GROUP') {
                    return permission.profile?.groupType
                        ? this.translate.instant(
                              'PERMISSIONS.GROUP_TYPE.' + permission.profile?.groupType,
                          )
                        : '';
                } else {
                    field = this.config.instant('userSecondaryDisplayName', null);
                }
            }

            if (field == 'email' || field == 'email-domain') {
                let email;
                if (permission.user) {
                    email = permission.user.email || permission.user.mailbox;
                }
                if (permission.profile) {
                    email =
                        permission.profile.email ||
                        permission.profile.mailbox ||
                        permission.profile.groupEmail;
                }
                if (field == 'email-domain') {
                    email = email ? email.substr(email.indexOf('@') + 1) : null;
                }
                return email;
            }
            if (field == 'authorityName') {
                if (permission.authorityType == 'USER') {
                    return permission.authorityName;
                }
            }
            return '';
        }
        if (permission.user && (permission.user.firstName || permission.user.lastName)) {
            return permission.user.firstName + ' ' + permission.user.lastName;
        }
        if (permission.group && permission.group.displayName) {
            return permission.group.displayName;
        }
        return this.translate.instant(permission.authority.authorityName);
    }
}
