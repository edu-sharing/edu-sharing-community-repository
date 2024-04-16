import { Component, OnInit } from '@angular/core';
import { RestConnectorService } from '../../../../core-module/core.module';
import { Toast } from '../../../../services/toast';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';

@Component({
    selector: 'es-third-party-licenses-dialog',
    templateUrl: './third-party-licenses-dialog.component.html',
    styleUrls: ['./third-party-licenses-dialog.component.scss'],
})
export class ThirdPartyLicensesDialogComponent implements OnInit {
    licenseDetails: { component: string; plugin: string; details: string }[] = [];

    constructor(
        private dialogRef: CardDialogRef,
        private toast: Toast,
        private connector: RestConnectorService,
    ) {
        this.dialogRef.patchState({ isLoading: true });
    }

    ngOnInit(): void {
        this.connector.getLicenses().subscribe(
            (licenses) => {
                const mapping = (component: string, plugin: string, details: string) => {
                    return {
                        component,
                        plugin: plugin.replace('.txt', ''),
                        details: details,
                    };
                };
                this.licenseDetails = Object.keys(licenses.repository).map((k) =>
                    mapping('Repository', k, licenses.repository[k]),
                );
                Object.keys(licenses.services).forEach(
                    (k) =>
                        (this.licenseDetails = this.licenseDetails.concat(
                            Object.keys(licenses.services[k]).map((p) =>
                                mapping(k, p, licenses.services[k][p]),
                            ),
                        )),
                );
                this.dialogRef.patchState({ isLoading: false });
            },
            (error) => {
                this.dialogRef.close();
                this.toast.error(error);
                console.error(error);
            },
        );
    }
}
