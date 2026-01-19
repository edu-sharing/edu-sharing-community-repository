/**
 * Created by Torsten on 13.01.2017.
 */

import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';
import { DomSanitizer } from '@angular/platform-browser';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthenticationService, Group, Person, RestConstants } from 'ngx-edu-sharing-api';
import { map } from 'rxjs/operators';
import { EduSharingUiCommonModule } from '../common/edu-sharing-ui-common.module';
import { UIConstants } from '../util/ui-constants';
import { CommonModule } from '@angular/common';
import { AuthorityNamePipe } from '../pipes/authority-name.pipe';

@Component({
    selector: 'es-user-avatar',
    templateUrl: 'user-avatar.component.html',
    styleUrls: ['user-avatar.component.scss'],
    imports: [TranslateModule, EduSharingUiCommonModule, CommonModule],
})
export class UserAvatarComponent {
    /**
     * Automatically link to the given user profile
     * @type {boolean}
     */
    @Input() link = false;
    @Input() _user: Person | Group | any;
    @Input() set user(data: Person | Group | any) {
        let result: Person | Group | any = {};
        // map elements comming from the permissions api to generic iam user/group
        if (data?.authority) {
            result.authorityName = data.authority.authorityName;
            result.authorityType = data.authority.authorityType;
            result.profile = data.user || data.group;
        } else {
            result = data;
        }
        this._user = result;
    }
    /**
     * when a regular material icon should be used instead of an avatar
     */
    @Input() icon: string;
    /**
     * either xxsmall, xsmall, small, medium or large
     */
    @Input() size: 'xxsmall' | 'xsmall' | 'small' | 'medium' | 'large' = 'large';

    // random view id
    public id = Math.random();
    public _customImage: any;
    @Input() set customImage(customImage: File) {
        if (customImage == null) {
            this._customImage = null;
            return;
        }
        this._customImage = this.sanitizer.bypassSecurityTrustUrl(URL.createObjectURL(customImage));
    }
    constructor(
        private router: Router,
        private translate: TranslateService,
        private authenticationService: AuthenticationService,
        private sanitizer: DomSanitizer,
    ) {}
    isEditorialUser() {
        return (
            this._user &&
            this._user.profile &&
            ((this._user.profile.types &&
                this._user.profile.types.indexOf(RestConstants.GROUP_TYPE_EDITORIAL) !== -1) ||
                this._user.profile.groupType === RestConstants.GROUP_TYPE_EDITORIAL)
        );
    }
    openProfile() {
        void this.router.navigate([
            UIConstants.ROUTER_PREFIX + 'profiles',
            this._user.authorityName,
        ]);
    }

    getLetter(user: Person) {
        return this.authenticationService
            .observeLoginInfo()
            .pipe(
                map((info) =>
                    info?.isGuest
                        ? 'G'
                        : new AuthorityNamePipe(this.translate).transform(user, {
                              avatarShortcut: true,
                          }),
                ),
            );
    }

    isSafe() {
        return this.authenticationService
            .observeLoginInfo()
            .pipe(map((info) => info?.currentScope !== null));
    }
}
