import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import * as rxjs from 'rxjs';
import { BehaviorSubject } from 'rxjs';
import { DialogButton, Node } from '../../../../core-module/core.module';
import { LocalEventsService } from '../../../../services/local-events.service';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { LicenseDialogContentComponent } from './license-dialog-content.component';
import { LicenseDialogData, LicenseDialogResult } from './license-dialog-data';

@Component({
    selector: 'es-license-dialog',
    templateUrl: './license-dialog.component.html',
    styleUrls: ['./license-dialog.component.scss'],
})
export class LicenseDialogComponent implements OnInit {
    @ViewChild(LicenseDialogContentComponent) content: LicenseDialogContentComponent;

    readonly canSave = new BehaviorSubject(true);

    private readonly saveButton = new DialogButton('SAVE', { color: 'primary' }, () =>
        this.content.saveLicense(),
    );

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: LicenseDialogData,
        private dialogRef: CardDialogRef<LicenseDialogData, LicenseDialogResult>,
        private localEvents: LocalEventsService,
    ) {}

    ngOnInit(): void {
        this.initButtons();
        this.registerUpdateButtons();
    }

    private initButtons() {
        this.dialogRef.patchConfig({
            buttons: [
                new DialogButton('CANCEL', { color: 'standard' }, () => this.dialogRef.close(null)),
                this.saveButton,
            ],
        });
    }

    private registerUpdateButtons() {
        rxjs.combineLatest([this.canSave, this.dialogRef.observeState('isLoading')]).subscribe(
            ([canSave, isLoading]) => {
                this.saveButton.disabled = isLoading || !canSave;
            },
        );
    }

    onDone(result: LicenseDialogResult) {
        this.dialogRef.close(result);
        if (this.data.kind === 'nodes' && Array.isArray(result)) {
            this.localEvents.nodesChanged.emit(result as Node[]);
        }
    }

    setIsLoading(isLoading: boolean) {
        void Promise.resolve().then(() => {
            this.dialogRef.patchState({ isLoading });
        });
    }
}
