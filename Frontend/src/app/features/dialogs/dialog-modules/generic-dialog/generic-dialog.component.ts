import { Component, OnInit, inject } from '@angular/core';
import { DialogButton } from '../../../../core-module/core.module';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { GenericDialogData } from './generic-dialog-data';

@Component({
    selector: 'es-generic-dialog',
    templateUrl: './generic-dialog.component.html',
    styleUrls: ['./generic-dialog.component.scss'],
    standalone: false,
})
export class GenericDialogComponent<R extends string> implements OnInit {
    data = inject<GenericDialogData<R>>(CARD_DIALOG_DATA);
    private dialogRef = inject<CardDialogRef<GenericDialogData<R>, R>>(CardDialogRef);

    ngOnInit(): void {
        this.initButtons();
    }

    private initButtons() {
        if (this.data.buttons) {
            const buttons = this.data.buttons.map(
                (button) =>
                    new DialogButton(button.label, button.config, async () => {
                        let close = true;
                        if (button.callback) {
                            close = await button.callback(this.dialogRef);
                        }
                        if (close) {
                            this.dialogRef.close(button.label);
                        }
                    }),
            );
            this.dialogRef.patchConfig({ buttons });
        }
    }
}
