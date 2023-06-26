/**
 * Created by Torsten on 13.01.2017.
 */

import { Component, OnInit } from '@angular/core';
import { AboutService, AuthenticationService, About } from 'ngx-edu-sharing-api';
import { first } from 'rxjs/operators';
import { forkJoin } from 'rxjs';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { result } from 'lodash';
import { TranslationsService } from '../../../translations/translations.service';

@Component({
    selector: 'es-notification-list',
    templateUrl: 'notification-list.component.html',
    styleUrls: ['notification-list.component.scss'],
})
export class NotificationListComponent implements OnInit {
    show = false;
    count: number;
    private about: About;
    viewRead = false;
    loading = false;
    constructor(
        private aboutService: AboutService,
        private authenticationService: AuthenticationService,
        private translations: TranslationsService,
        private dialogs: DialogsService,
    ) {}

    async ngOnInit() {
        await this.translations.waitForInit().toPromise();
        this.about = await this.aboutService.getAbout().toPromise();
        this.authenticationService.observeLoginInfo().subscribe((login) => {
            this.show =
                login.statusCode === RestConstants.STATUS_CODE_OK &&
                this.about.plugins.filter((s) => s.id === 'kafka-notification-plugin').length > 0;
            this.count = 1337;
        });
    }
    async openSettings() {
        await this.dialogs.openNotificationDialog();
    }

    setViewRead(viewRead: boolean) {
        this.viewRead = viewRead;
    }
}
