import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { CardDialogRef } from '../../../../features/dialogs/card-dialog/card-dialog-ref';
import { DialogButton } from '../../../../util/dialog-button';
import { ClientConfig, NotificationConfig, NotificationV1Service } from 'ngx-edu-sharing-api';
import { FormControl, FormGroup } from '@angular/forms';
import { ConfigService } from 'ngx-edu-sharing-api';
import { Toast } from '../../../../services/toast';
import { first } from 'rxjs/operators';

enum NotificationEvents {
    inviteEvent = 'inviteEvent',
    workflowEvent = 'workflowEvent',
    commentEvent = 'commentEvent',
    ratingEvent = 'ratingEvent',
    proposeForCollectionEvent = 'proposeForCollectionEvent',
    addToCollectionEvent = 'addToCollectionEvent',
    nodeIssueEvent = 'nodeIssueEvent',
    metadataSuggestionEvent = 'metadataSuggestionEvent',
}

@Component({
    selector: 'es-notification-dialog',
    templateUrl: './notification-dialog.component.html',
    styleUrls: ['./notification-dialog.component.scss'],
})
export class NotificationDialogComponent implements OnInit, OnDestroy {
    readonly destroyed$ = new Subject<void>();
    private config: NotificationConfig;
    private clientConfig: ClientConfig;

    notificationForm = new FormGroup({
        configMode: new FormControl(),
        defaultInterval: new FormControl(),
        intervals: new FormGroup({}),
    });

    constructor(
        private dialogRef: CardDialogRef<void, void>,
        private toast: Toast,
        private notificationService: NotificationV1Service,
        private configService: ConfigService,
    ) {}

    getGroups() {
        const result = [];
        for (let v in NotificationEvents) {
            // skip unused events
            if (
                ![
                    NotificationEvents.nodeIssueEvent.valueOf(),
                    NotificationEvents.metadataSuggestionEvent.valueOf(),
                ].includes(v) &&
                // do hide rating event if rating is disabled
                !(
                    (!this.clientConfig.rating || this.clientConfig.rating?.mode === 'none') &&
                    NotificationEvents.ratingEvent.valueOf() === v
                )
            ) {
                result.push(v);
            }
        }
        return result;
    }

    async ngOnInit() {
        this.clientConfig = await this.configService.observeConfig().pipe(first()).toPromise();
        console.log(this.clientConfig);
        for (let v in NotificationEvents) {
            (this.notificationForm.get('intervals') as FormGroup).addControl(v, new FormControl());
        }
        this.dialogRef.patchConfig({
            buttons: [
                new DialogButton('CANCEL', { color: 'standard' }, () => this.dialogRef.close()),
                new DialogButton('SAVE', { color: 'primary' }, () => this.save()),
            ],
        });
        this.dialogRef.patchState({
            isLoading: true,
        });
        this.config = await this.notificationService.getConfig2().toPromise();
        this.notificationForm.patchValue(this.config);
        this.dialogRef.patchState({
            isLoading: false,
        });
    }

    ngOnDestroy() {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    private async save() {
        this.dialogRef.patchState({
            isLoading: true,
        });

        await this.notificationService
            .setConfig1({
                body: this.notificationForm.value,
            })
            .toPromise();
        this.dialogRef.close();
        this.toast.toast('NOTIFICATION.SAVED');
    }
}
