import { Component, inject } from '@angular/core';
import { FormControl } from '@angular/forms';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { CheckboxDialogData, CheckboxDialogResult } from './checkbox-dialog-data';
import { SharedModule } from '../../../../shared/shared.module';

/**
 * A generic dialog with an input form field and configurable title, message, label and icon.
 */
@Component({
    imports: [SharedModule],
    templateUrl: './checkbox-dialog.component.html',
    styleUrls: ['./checkbox-dialog.component.scss'],
})
export class CheckboxDialogComponent {
    data = inject<CheckboxDialogData>(CARD_DIALOG_DATA);
    private dialogRef =
        inject<CardDialogRef<CheckboxDialogData, CheckboxDialogResult>>(CardDialogRef);

    control = new FormControl(false);

    constructor() {
        this.control.valueChanges.subscribe((value) => (this.data.state = value));
    }
}
