import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { User } from '../../core-module/rest/data-object';

@Pipe({ name: 'authorityAffiliation' })
export class AuthorityAffiliationPipe implements PipeTransform {
    constructor(private translate: TranslateService) {}
    transform(authority: any | User, args: string[] = null): string {
        if (!authority) return 'invalid';
        if (authority.profile && authority.profile.primaryAffiliation) {
            let key = 'USER.PRIMARY_AFFILIATION.' + authority.profile.primaryAffiliation;
            let result = this.translate.instant(key);
            if (key != result) return result;
            // in case of no translation, fetch raw value
            return authority.profile.primaryAffiliation;
        }
        return '';
    }
}
