import { Component, OnInit } from '@angular/core';
import { TranslationsService } from 'ngx-edu-sharing-ui';
import { Closable } from '../../features/dialogs/card-dialog/card-dialog-config';
import { OK } from '../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../../features/dialogs/dialogs.service';
import { MainNavService } from '../../main/navigation/main-nav.service';

@Component({
    selector: 'es-lti',
    template: '',
})
export class LtiComponent implements OnInit {
    constructor(
        private dialogs: DialogsService,
        private mainNav: MainNavService,
        private translations: TranslationsService,
    ) {}

    ngOnInit(): void {
        this.mainNav.setMainNavConfig({ currentScope: 'lti', title: '', show: false });
        // Wait for translations to avoid flashing translation strings.
        this.translations.waitForInit().subscribe(() => void this.showDialog());
    }

    private async showDialog(): Promise<void> {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'LTI.REGISTERED.TITLE',
            message: 'LTI.REGISTERED.DESCRIPTION',
            buttons: OK,
            closable: Closable.Disabled,
        });
        dialogRef.afterClosed().subscribe(() => {
            (window.opener || window.parent).postMessage(
                { subject: 'org.imsglobal.lti.close' },
                '*',
            );
        });
    }
}
