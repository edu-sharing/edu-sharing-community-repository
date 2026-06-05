/**
 * Created by Torsten on 13.01.2017.
 */

import { Component, Input, inject } from '@angular/core';
import { Router } from '@angular/router';
import { DomSanitizer } from '@angular/platform-browser';
import { AuthorityProfile } from '../../../../../core-module/core.module';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'es-user-tile',
    templateUrl: 'user-tile.component.html',
    styleUrls: ['user-tile.component.scss'],
    standalone: false,
})
export class UserTileComponent {
    private router = inject(Router);
    private translate = inject(TranslateService);
    private sanitizer = inject(DomSanitizer);

    @Input() user: AuthorityProfile;
    @Input() active = false;
}
