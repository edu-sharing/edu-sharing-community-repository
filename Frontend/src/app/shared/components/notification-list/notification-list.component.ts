/**
 * Created by Torsten on 13.01.2017.
 */

import { Component, OnInit } from '@angular/core';
import { AboutService, AuthenticationService } from 'ngx-edu-sharing-api';
import { first } from 'rxjs/operators';
import { forkJoin } from 'rxjs';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { DialogsService } from '../../../features/dialogs/dialogs.service';

@Component({
    selector: 'es-notification-list',
    templateUrl: 'notification-list.component.html',
    styleUrls: ['notification-list.component.scss'],
})
export class NotificationListComponent implements OnInit {
    show = false;
    constructor(
        private aboutService: AboutService,
        private authenticationService: AuthenticationService,
        private dialogs: DialogsService,
    ) {}

    async ngOnInit() {
        const result = await forkJoin([
            this.authenticationService.observeLoginInfo().pipe(first()),
            this.aboutService.getAbout(),
        ]).toPromise();
        this.show =
            result[0].statusCode === RestConstants.STATUS_CODE_OK &&
            result[1].plugins.filter((s) => s.id === 'kafka-notification-plugin').length > 0;
    }

    async openSettings() {
        await this.dialogs.openNotificationDialog();
    }
}
