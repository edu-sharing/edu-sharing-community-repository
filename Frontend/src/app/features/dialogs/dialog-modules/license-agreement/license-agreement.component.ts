import { Component, Inject, OnInit } from '@angular/core';
import { DialogButton } from '../../../../core-module/core.module';
import { CardDialogConfig, CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import {
    LicenseAgreementDialogData,
    LicenseAgreementDialogResult,
} from './license-agreement-dialog-data';

@Component({
    selector: 'es-license-agreement',
    templateUrl: './license-agreement.component.html',
    styleUrls: ['./license-agreement.component.scss'],
})
export class LicenseAgreementComponent implements OnInit {
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

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: LicenseAgreementDialogData,
        private dialogRef: CardDialogRef<LicenseAgreementDialogData, LicenseAgreementDialogResult>,
    ) {}

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
