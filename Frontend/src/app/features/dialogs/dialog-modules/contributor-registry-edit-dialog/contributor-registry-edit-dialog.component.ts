import { Component, OnInit, inject } from '@angular/core';
import { ContributorData } from 'ngx-edu-sharing-api';
import { DialogButton } from '../../../../core-module/core.module';
import { Toast } from '../../../../services/toast';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import {
    ContributorRegistryEditDialogData,
    ContributorRegistryEditDialogResult,
} from './contributor-registry-edit-dialog-data';

type ContributorModel = Partial<ContributorData>;

@Component({
    selector: 'es-contributor-registry-edit-dialog',
    templateUrl: './contributor-registry-edit-dialog.component.html',
    styleUrls: ['./contributor-registry-edit-dialog.component.scss'],
    standalone: false,
})
export class ContributorRegistryEditDialogComponent implements OnInit {
    data = inject<ContributorRegistryEditDialogData>(CARD_DIALOG_DATA);
    private dialogRef =
        inject<
            CardDialogRef<ContributorRegistryEditDialogData, ContributorRegistryEditDialogResult>
        >(CardDialogRef);
    private toast = inject(Toast);

    model: ContributorModel = { kind: 'PERSON' };
    isNew = true;
    applyToExisting = false;

    ngOnInit(): void {
        this.isNew = !this.data.contributor;
        if (this.data.contributor) {
            // copy so cancel discards changes
            this.model = { ...this.data.contributor };
        }
        this.initButtons();
    }

    private initButtons() {
        this.dialogRef.patchConfig({
            buttons: [
                new DialogButton('CANCEL', { color: 'standard' }, () => this.dialogRef.close(null)),
                new DialogButton('SAVE', { color: 'primary' }, () => this.save()),
            ],
        });
    }

    private save() {
        if (!this.hasAnyId()) {
            this.toast.error(null, 'ADMIN.CONTRIBUTORS.ERROR_NO_ID');
            return;
        }
        this.dialogRef.close({
            contributor: this.model,
            applyToExisting: this.applyToExisting,
        });
    }

    private hasAnyId(): boolean {
        return !!(
            this.model.orcid ||
            this.model.gnduri ||
            this.model.ror ||
            this.model.wikidata ||
            this.model.email
        );
    }
}
