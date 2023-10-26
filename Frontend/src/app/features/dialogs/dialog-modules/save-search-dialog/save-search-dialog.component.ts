import { Component, Inject, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { ApiErrorResponse, SavedSearch, SavedSearchesService } from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { BehaviorSubject } from 'rxjs';
import { first, map, take } from 'rxjs/operators';
import { DialogButton, RestConstants } from '../../../../core-module/core.module';
import { Toast } from '../../../../core-ui-module/toast';
import { DateHelper, notNull } from 'ngx-edu-sharing-ui';
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
    readonly nameControl = new UntypedFormControl();
    private readonly _savedSearches = new BehaviorSubject<SavedSearch[]>(null);
    autocompleteValues: string[];

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: SaveSearchDialogData,
        private dialogRef: CardDialogRef<SaveSearchDialogData, SaveSearchDialogResult>,
        private savedSearchesService: SavedSearchesService,
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
        this.savedSearchesService
            .observeMySavedSearches()
            .pipe(take(1))
            .subscribe(this._savedSearches);
        this.registerAutoCompleteValues();
    }

    private registerAutoCompleteValues(): void {
        rxjs.combineLatest([
            this._savedSearches.pipe(first(notNull)),
            this.nameControl.valueChanges,
        ])
            .pipe(
                map(([savedSearches, input]) =>
                    savedSearches
                        .map(({ title }) => title)
                        .filter((title) => title.includes(input)),
                ),
            )
            .subscribe((autocompleteValues) => (this.autocompleteValues = autocompleteValues));
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
        if (
            !replace &&
            this._savedSearches.value?.some(({ title }) => title === this.nameControl.value)
        ) {
            void this.showShouldReplaceDialog();
            return;
        }
        this.dialogRef.patchState({ isLoading: true });
        this.savedSearchesService.saveCurrentSearch(this.nameControl.value, { replace }).subscribe({
            next: (savedSearch) => {
                this.toast.toast('SEARCH.SAVE_SEARCH.TOAST_SAVED');
                this.dialogRef.close(savedSearch);
            },
            error: (error: ApiErrorResponse) => {
                this.dialogRef.patchState({ isLoading: false });
                if (error.status === RestConstants.DUPLICATE_NODE_RESPONSE) {
                    error.preventDefault();
                    void this.showShouldReplaceDialog();
                } else {
                    throw error;
                }
            },
        });
    }

    private async showShouldReplaceDialog(): Promise<void> {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'SEARCH.SAVE_SEARCH.SEARCH_EXISTS_TITLE',
            message: 'SEARCH.SAVE_SEARCH.SEARCH_EXISTS_MESSAGE',
            buttons: REPLACE_OR_BACK,
        });
        dialogRef.beforeClosed().subscribe((result) => {
            if (result === 'REPLACE') {
                this.save({ replace: true });
            } else {
                this.dialogRef.patchState({ isLoading: false });
            }
        });
    }
}
