import { Component, inject } from '@angular/core';
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
    standalone: false,
})
export class InputDialogComponent {
    data = inject<InputDialogData>(CARD_DIALOG_DATA);
    private dialogRef = inject<CardDialogRef<InputDialogData, InputDialogResult>>(CardDialogRef);

    control = new FormControl('');

    private confirmButton: DialogButton;

    constructor() {
        this.initButtons();
    }

    /** Confirms the dialog, e.g. triggered by the enter key. Ignored while the input is empty. */
    confirm(): void {
        if (this.confirmButton.disabled) {
            return;
        }
        this.confirmButton.callback();
    }

    private initButtons(): void {
        this.confirmButton = new DialogButton(
            this.data.confirmLabel ?? 'SAVE',
            { color: 'primary' },
            () => this.dialogRef.close(this.control.value.trim()),
        );
        this.confirmButton.disabled = true;
        this.control.valueChanges.subscribe(
            (value) => (this.confirmButton.disabled = !value.trim()),
        );
        this.dialogRef.patchConfig({
            buttons: [
                new DialogButton('CANCEL', { color: 'standard' }, () => this.dialogRef.close(null)),
                this.confirmButton,
            ],
        });
    }
}
