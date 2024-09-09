import { Component, Inject, ViewChild } from '@angular/core';
import { Connector } from 'ngx-edu-sharing-api';
import { BehaviorSubject } from 'rxjs';
import { first } from 'rxjs/operators';
import { DialogButton } from '../../../../core-module/core.module';
import { CARD_DIALOG_DATA, Closable } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import {
    AddWithConnectorDialogData,
    AddWithConnectorDialogResult,
} from './add-with-connector-dialog-data';
import { MdsEditorComponent } from '../../../mds/mds-editor/mds-editor.component';
import { MdsEditorWrapperComponent } from '../../../mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';

@Component({
    selector: 'es-add-with-connector-dialog',
    templateUrl: './add-with-connector-dialog.component.html',
    styleUrls: ['./add-with-connector-dialog.component.scss'],
})
export class AddWithConnectorDialogComponent {
    @ViewChild(MdsEditorWrapperComponent) mdsEditorRef: MdsEditorWrapperComponent;
    readonly connector = this.processConnector(this.data.connector);
    private nameSubject = new BehaviorSubject<string>(this.data.name ?? '');
    get name(): string {
        return this.nameSubject.value;
    }
    set name(value: string) {
        this.nameSubject.next(value);
    }
    type = 0;

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: AddWithConnectorDialogData,
        private dialogRef: CardDialogRef<AddWithConnectorDialogData, AddWithConnectorDialogResult>,
    ) {
        this.initDialogConfig();
    }

    private cancel() {
        this.dialogRef.close(null);
    }

    private create() {
        if (this.connector.mdsGroup && !this.mdsEditorRef.mdsEditorInstance.getCanSave()) {
            return;
        } else if (!this.name.trim()) {
            return;
        }
        this.dialogRef.close({ name: this.name, type: this.getType() });
    }

    private processConnector(connector: Connector): Connector {
        for (let i = 0; i < connector.filetypes.length; i++) {
            if (!connector.filetypes[i].creatable) {
                connector.filetypes.splice(i, 1);
            }
        }
        return connector;
    }

    private initDialogConfig(): void {
        const cancelButton = new DialogButton('CANCEL', { color: 'standard' }, () => this.cancel());
        const createButton = new DialogButton('CREATE', { color: 'primary' }, () => this.create());
        this.dialogRef.patchConfig({
            title: 'CONNECTOR.' + this.connector.id + '.TITLE',
            subtitle: 'CONNECTOR.' + this.connector.id + '.NAME',
            avatar: { kind: 'icon', icon: this.connector.icon },
            buttons: [cancelButton, createButton],
        });
        this.nameSubject.subscribe((name) => (createButton.disabled = !name.trim()));
        this.nameSubject
            .pipe(first((name) => !!name))
            .subscribe(() => this.dialogRef.patchConfig({ closable: Closable.Standard }));
        this.mdsEditorRef?.mdsEditorInstance
            .observeCanSave()
            .subscribe((can) => (createButton.disabled = !can));
    }

    getType() {
        return this.connector.filetypes[this.type];
    }
}
