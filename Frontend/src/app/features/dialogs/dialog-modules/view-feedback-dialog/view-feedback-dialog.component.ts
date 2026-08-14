import { Component, inject, signal } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { FeedbackData, FeedbackV1Service } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule, EduSharingUiModule } from 'ngx-edu-sharing-ui';
import { firstValueFrom } from 'rxjs';
import { RestConstants } from '../../../../core-module/core.module';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { ViewFeedbackDialogData } from './view-feedback-dialog-data';

@Component({
    selector: 'es-view-feedback-dialog',
    imports: [EduSharingUiCommonModule, EduSharingUiModule, TranslateModule],
    templateUrl: './view-feedback-dialog.component.html',
    styleUrls: ['./view-feedback-dialog.component.scss'],
})
export class ViewFeedbackDialogComponent {
    private data = inject<ViewFeedbackDialogData>(CARD_DIALOG_DATA);
    private dialogRef = inject<CardDialogRef<ViewFeedbackDialogData, void>>(CardDialogRef);
    private feedbackService = inject(FeedbackV1Service);

    protected readonly feedbacks = signal<FeedbackData[]>(null);

    constructor() {
        void this.fetch();
    }

    private async fetch(): Promise<void> {
        this.dialogRef.patchState({ isLoading: true });
        try {
            this.feedbacks.set(
                await firstValueFrom(
                    this.feedbackService.getFeedbacks({
                        repository: RestConstants.HOME_REPOSITORY,
                        node: this.data.node.ref.id,
                    }),
                ),
            );
        } catch (error) {
            this.dialogRef.close();
            return;
        } finally {
            this.dialogRef.patchState({ isLoading: false });
        }
    }
}
