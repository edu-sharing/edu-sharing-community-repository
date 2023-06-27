/**
 * Created by Torsten on 13.01.2017.
 */

import { Component, OnInit } from '@angular/core';
import {
    AboutService,
    AuthenticationService,
    About,
    NotificationV1Service,
} from 'ngx-edu-sharing-api';
import { first } from 'rxjs/operators';
import { forkJoin } from 'rxjs';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { result } from 'lodash';
import { TranslationsService } from '../../../translations/translations.service';
import { DataSource } from '@angular/cdk/collections';
import { NodeDataSource } from '../../../features/node-entries/node-data-source';

@Component({
    selector: 'es-notification-list',
    templateUrl: 'notification-list.component.html',
    styleUrls: ['notification-list.component.scss'],
})
export class NotificationListComponent implements OnInit {
    dataSource = new NodeDataSource<any>();
    unreadNotificationsCount: number;
    show = false;
    private about: About;
    viewRead = false;
    constructor(
        private aboutService: AboutService,
        private authenticationService: AuthenticationService,
        private translations: TranslationsService,
        private notificationService: NotificationV1Service,
        private dialogs: DialogsService,
    ) {}

    async ngOnInit() {
        this.dataSource.isLoading = true;
        await this.translations.waitForInit().toPromise();
        this.about = await this.aboutService.getAbout().toPromise();
        this.authenticationService.observeLoginInfo().subscribe((login) => {
            this.show =
                login.statusCode === RestConstants.STATUS_CODE_OK &&
                this.about.plugins.filter((s) => s.id === 'kafka-notification-plugin').length > 0;
            this.loadNotifications();
        });
    }

    async openSettings() {
        await this.dialogs.openNotificationDialog();
    }

    setViewRead(viewRead: boolean) {
        this.viewRead = viewRead;
        this.loadNotifications();
    }

    markAllRead() {}

    private loadNotifications() {
        // this.notificationService
        this.dataSource.isLoading = false;
        if (!this.viewRead) {
            this.unreadNotificationsCount = this.dataSource.getTotal();
        }
    }

    updateStatus(entry: any, status: string) {
        entry.status = status;
        if (status === 'READ') {
            // client side update of unread notifications
            this.unreadNotificationsCount--;
        }
    }
}
