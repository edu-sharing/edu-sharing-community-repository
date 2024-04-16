/**
 * Created by Torsten on 13.01.2017.
 */

import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';
import { DomSanitizer } from '@angular/platform-browser';
import { AuthorityProfile } from '../../../../../core-module/core.module';
import { TranslateService } from '@ngx-translate/core';

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
