import { Component, Inject } from '@angular/core';
import { FormControl } from '@angular/forms';
import { DialogButton } from '../../../../core-module/core.module';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { InputDialogData, InputDialogResult } from './input-dialog-data';

/**
 * A generic dialog with an input form field and configurable title, message, label and icon.
 */
@Component({
    selector: 'es-input-dialog',
    templateUrl: './input-dialog.component.html',
    styleUrls: ['./input-dialog.component.scss'],
})
export class InputDialogComponent {
    control = new FormControl('');

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: InputDialogData,
        private dialogRef: CardDialogRef<InputDialogData, InputDialogResult>,
    ) {
        this.initButtons();
    }

    private initButtons(): void {
        const buttons = [
            new DialogButton('CANCEL', { color: 'standard' }, () => this.dialogRef.close(null)),
            new DialogButton('SAVE', { color: 'primary' }, () =>
                this.dialogRef.close(this.control.value.trim()),
            ),
        ];
        buttons[1].disabled = true;
        this.control.valueChanges.subscribe((value) => (buttons[1].disabled = !value.trim()));
        this.dialogRef.patchConfig({ buttons });
    }
}
