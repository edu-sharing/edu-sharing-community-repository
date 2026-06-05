import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { delay, first } from 'rxjs/operators';
import { DialogButton, RestConstants } from '../../../../core-module/core.module';
import { Toast } from '../../../../services/toast';
import { FeedbackV1Service } from 'ngx-edu-sharing-api';
import { MdsEditorInstanceService } from '../../../mds/mds-editor/mds-editor-instance.service';
import { Values } from '../../../mds/types/types';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { SendFeedbackDialogData, SendFeedbackDialogResult } from './send-feedback-dialog-data';

@Component({
    selector: 'es-send-feedback-dialog',
    templateUrl: './send-feedback-dialog.component.html',
    styleUrls: ['./send-feedback-dialog.component.scss'],
    providers: [MdsEditorInstanceService],
    standalone: false,
})
export class SendFeedbackDialogComponent implements OnInit {
    data = inject<SendFeedbackDialogData>(CARD_DIALOG_DATA);
    private dialogRef =
        inject<CardDialogRef<SendFeedbackDialogData, SendFeedbackDialogResult>>(CardDialogRef);
    private route = inject(ActivatedRoute);
    private feedbackService = inject(FeedbackV1Service);
    private toast = inject(Toast);
    private mdsEditorInstance = inject(MdsEditorInstanceService);

    constructor() {
        this.dialogRef.patchState({ isLoading: true });
    }

    ngOnInit(): void {
        this.initButtons();
        void this.mdsEditorInstance.initWithoutNodes(
            'material_feedback',
            undefined,
            RestConstants.HOME_REPOSITORY,
            'form',
            {},
        );
        this.mdsEditorInstance.mdsInflated.pipe(first(), delay(0)).subscribe(() => {
            this.dialogRef.patchState({ isLoading: false });
        });
    }

    private initButtons(): void {
        const buttons = [
            new DialogButton('CANCEL', { color: 'standard' }, () => this.dialogRef.close(null)),
            new DialogButton('FEEDBACK.SAVE', { color: 'primary' }, () => this.addFeedback()),
        ];
        this.dialogRef.patchConfig({
            buttons,
        });
        this.mdsEditorInstance
            .observeCanSave()
            .subscribe((canSave) => (buttons[1].disabled = !canSave));
    }

    private async addFeedback() {
        const values = (await this.mdsEditorInstance.save()) as Values;
        this.dialogRef.patchState({ isLoading: true });
        try {
            await this.feedbackService
                .addFeedback({
                    repository: RestConstants.HOME_REPOSITORY,
                    node: this.data.node.ref.id,
                    body: values,
                })
                .toPromise();
        } catch (e) {
            this.toast.error(e);
        }
        this.dialogRef.close();
        const queryParams = await this.route.queryParams.toPromise();
        if (queryParams.feedbackClose) {
            window.close();
        }
    }
}
