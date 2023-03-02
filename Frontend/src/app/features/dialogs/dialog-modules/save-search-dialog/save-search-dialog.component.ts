import { Component, Inject, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { ApiErrorResponse, SearchService } from 'ngx-edu-sharing-api';
import { DialogButton, RestConstants } from '../../../../core-module/core.module';
import { DateHelper } from '../../../../core-ui-module/DateHelper';
import { Toast } from '../../../../core-ui-module/toast';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { DialogsService } from '../../dialogs.service';
import { REPLACE_OR_BACK } from '../generic-dialog/generic-dialog-data';
import { SaveSearchDialogData, SaveSearchDialogResult } from './save-search-dialog-data';

@Component({
    selector: 'es-save-search-dialog',
    templateUrl: './save-search-dialog.component.html',
    styleUrls: ['./save-search-dialog.component.scss'],
})
export class SaveSearchDialogComponent implements OnInit {
    readonly nameControl = new FormControl();

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: SaveSearchDialogData,
        private dialogRef: CardDialogRef<SaveSearchDialogData, SaveSearchDialogResult>,
        private search: SearchService,
        private dialogs: DialogsService,
        private toast: Toast,
        private translate: TranslateService,
    ) {}

    ngOnInit(): void {
        const name = this.data.name || this.getInitialName(this.data.searchString);
        this.nameControl.setValue(name);
        const cancelButton = new DialogButton('CANCEL', { color: 'standard' }, () =>
            this.dialogRef.close(),
        );
        const saveButton = new DialogButton('SAVE', { color: 'primary' }, () => this.save());
        this.dialogRef.patchConfig({
            buttons: [cancelButton, saveButton],
        });
        const updateName = (name: string) => {
            this.dialogRef.patchConfig({ subtitle: name });
            saveButton.disabled = !name;
        };
        this.nameControl.valueChanges.subscribe((name) => updateName(name));
        updateName(name);
    }

    private getInitialName(searchString: string): string {
        return (
            (searchString || this.translate.instant('SEARCH.SAVE_SEARCH.UNKNOWN_QUERY')) +
            ' - ' +
            DateHelper.formatDate(this.translate, Date.now(), {
                showAlwaysTime: true,
                useRelativeLabels: false,
            })
        );
    }

    private save({ replace = false } = {}): void {
        this.dialogRef.patchState({ isLoading: true });
        this.search.saveCurrentSearch(this.nameControl.value, { replace }).subscribe({
            next: (entry) => {
                this.toast.toast('SEARCH.SAVE_SEARCH.TOAST_SAVED');
                this.dialogRef.close(entry.node);
            },
            error: (error: ApiErrorResponse) => {
                if (error.status === RestConstants.DUPLICATE_NODE_RESPONSE) {
                    error.preventDefault();
                    void this.showShouldReplaceDialog();
                }
            },
        });
    }

    private async showShouldReplaceDialog(): Promise<void> {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'SEARCH.SAVE_SEARCH.SEARCH_EXISTS_TITLE',
            messageText: 'SEARCH.SAVE_SEARCH.SEARCH_EXISTS_MESSAGE',
            buttons: REPLACE_OR_BACK,
        });
        dialogRef.afterClosed().subscribe((result) => {
            if (result === 'REPLACE') {
                this.save({ replace: true });
            } else {
                this.dialogRef.patchState({ isLoading: false });
            }
        });
    }
}
