import { Component, OnInit, inject } from '@angular/core';
import { DialogButton } from '../../../../core-module/core.module';
import { CARD_DIALOG_DATA, CardDialogConfig } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import {
    LicenseAgreementDialogData,
    LicenseAgreementDialogResult,
} from './license-agreement-dialog-data';

@Component({
    selector: 'es-license-agreement-dialog',
    templateUrl: './license-agreement-dialog.component.html',
    styleUrls: ['./license-agreement-dialog.component.scss'],
    standalone: false,
})
export class LicenseAgreementDialogComponent implements OnInit {
    data = inject<LicenseAgreementDialogData>(CARD_DIALOG_DATA);
    private dialogRef =
        inject<CardDialogRef<LicenseAgreementDialogData, LicenseAgreementDialogResult>>(
            CardDialogRef,
        );

    licenseAgreementHTML: string;
    acceptLicenseAgreement = false;
    readonly contentPadding = new CardDialogConfig().contentPadding;
    readonly acceptButton = new DialogButton('LICENSE_AGREEMENT.ACCEPT', { color: 'primary' }, () =>
        this.accept(),
    );
    readonly buttons = [
        new DialogButton('LICENSE_AGREEMENT.DECLINE', { color: 'standard' }, () => this.decline()),
        this.acceptButton,
    ];

    ngOnInit(): void {
        this.updateButtons();
        this.dialogRef.patchConfig({ buttons: this.buttons });
        this.licenseAgreementHTML = this.data.licenseHtml;
    }

    updateButtons() {
        this.acceptButton.disabled = !this.acceptLicenseAgreement;
    }

    private accept() {
        this.dialogRef.close('accepted');
    }

    private decline() {
        this.dialogRef.close('declined');
    }
}
