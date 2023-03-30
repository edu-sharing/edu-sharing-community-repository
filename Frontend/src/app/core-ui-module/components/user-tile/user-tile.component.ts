/**
 * Created by Torsten on 13.01.2017.
 */

import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';
import { UIConstants } from '../../../../../projects/edu-sharing-ui/src/lib/util/ui-constants';
import { DomSanitizer } from '@angular/platform-browser';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { AuthorityProfile, UserSimple } from '../../../core-module/core.module';
import { TranslateService } from '@ngx-translate/core';
import { AuthorityNamePipe } from '../../../shared/pipes/authority-name.pipe';

@Component({
    selector: 'es-user-tile',
    templateUrl: 'user-tile.component.html',
    styleUrls: ['user-tile.component.scss'],
})
export class UserTileComponent {
    @Input() user: AuthorityProfile;
    @Input() active = false;
    constructor(
        private router: Router,
        private translate: TranslateService,
        private sanitizer: DomSanitizer,
    ) {}
}
