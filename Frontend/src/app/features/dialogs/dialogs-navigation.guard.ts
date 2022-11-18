import { Injectable } from '@angular/core';
import { CanDeactivate } from '@angular/router';
import { CardDialogService } from './card-dialog/card-dialog.service';

@Injectable({
    providedIn: 'root',
})
export class DialogsNavigationGuardService implements CanDeactivate<unknown> {
    constructor(private cardDialog: CardDialogService) {}

    /**
     * Tries to close the top-most open dialogs.
     *
     * @returns `true` if there are no open dialogs
     */
    canDeactivate(): boolean {
        const openDialogs = this.cardDialog.openDialogs;
        const topMostDialog = openDialogs[openDialogs.length - 1];
        topMostDialog?.tryCancel('navigation');
        return !topMostDialog;
    }
}
