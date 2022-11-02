import { Component, Inject, OnInit } from '@angular/core';
import { DialogButton, RestAdminService } from '../../../../core-module/core.module';
import { Toast } from '../../../../core-ui-module/toast';
import { CARD_DIALOG_DATA, Closable } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import {
    XmlAppPropertiesDialogData,
    XmlAppPropertiesDialogResult,
} from './xml-app-properties-dialog-data';

const MULTILINE_PROPERTIES = ['custom_html_headers', 'public_key'];

@Component({
    selector: 'es-xml-app-properties-dialog',
    templateUrl: './xml-app-properties-dialog.component.html',
    styleUrls: ['./xml-app-properties-dialog.component.scss'],
})
export class XmlAppPropertiesDialogComponent implements OnInit {
    xmlAppAdditionalPropertyName: string;
    xmlAppAdditionalPropertyValue: string;

    readonly propertyKeys = Object.keys(this.data.properties);

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: XmlAppPropertiesDialogData,
        private dialogRef: CardDialogRef<XmlAppPropertiesDialogData, XmlAppPropertiesDialogResult>,
        private admin: RestAdminService,
        private toast: Toast,
    ) {}

    ngOnInit(): void {
        this.dialogRef.patchConfig({
            buttons: [
                new DialogButton('CANCEL', { color: 'standard' }, () => {
                    this.dialogRef.close(null);
                }),
                new DialogButton('APPLY', { color: 'primary' }, () => {
                    this.saveApp();
                }),
            ],
        });
    }

    isMultilineProperty(key: string) {
        if (MULTILINE_PROPERTIES.includes(key)) {
            return true;
        }
        return this.data.properties[key].includes('\n');
    }

    removeAppProperty(pos: number) {
        const key = this.propertyKeys[pos];
        this.propertyKeys.splice(pos, 1);
        delete this.data.properties[key];
    }

    onChange() {
        this.dialogRef.patchConfig({ closable: Closable.Standard });
    }

    private saveApp() {
        this.dialogRef.patchState({ isLoading: true });
        if (this.xmlAppAdditionalPropertyName && this.xmlAppAdditionalPropertyName.trim()) {
            this.data.properties[this.xmlAppAdditionalPropertyName.trim()] =
                this.xmlAppAdditionalPropertyValue;
        }
        this.admin.updateApplicationXML(this.data.appXml, this.data.properties).subscribe(
            () => {
                this.toast.toast('ADMIN.APPLICATIONS.APP_SAVED', {
                    xml: this.data.appXml,
                });
                this.dialogRef.patchState({ isLoading: false });
                this.dialogRef.close(true);
            },
            (error: any) => {
                this.dialogRef.patchState({ isLoading: false });
                this.toast.error(error);
            },
        );
    }
}
