import { Component, NgZone, OnDestroy, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { UIService } from '../../../../core-module/core.module';
import { CordovaService } from '../../../../services/cordova.service';
import { LtiToolOptionsService } from '../../../../services/lti-tool-options.service';
import { DialogButton } from '../../../../util/dialog-button';
import { CARD_DIALOG_DATA, Closable } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { CreateLtiToolDialogData, CreateLtiToolDialogResult } from './create-lti-tool-dialog-data';

@Component({
    selector: 'es-create-lti-tool-dialog',
    imports: [FormsModule, MatFormFieldModule, MatInputModule, TranslateModule],
    templateUrl: './create-lti-tool-dialog.component.html',
    styleUrls: ['./create-lti-tool-dialog.component.scss'],
})
export class CreateLtiToolDialogComponent implements OnDestroy {
    private data = inject<CreateLtiToolDialogData>(CARD_DIALOG_DATA);
    private dialogRef =
        inject<CardDialogRef<CreateLtiToolDialogData, CreateLtiToolDialogResult>>(CardDialogRef);
    private ngZone = inject(NgZone);
    private translate = inject(TranslateService);
    private uiService = inject(UIService);
    private cordova = inject(CordovaService);
    private ltiToolOptions = inject(LtiToolOptionsService);

    readonly tool = this.data.tool;
    name = '';
    nodes: Node[] = [];

    private createButton: DialogButton;

    constructor() {
        this.registerDeepLinkCallback();
        this.initDialogConfig();
        if (!this.tool.customContentOption) {
            this.openDeepLinkFlow();
        }
    }

    ngOnDestroy(): void {
        delete (window as any)['angularComponentReference'];
    }

    updateButtons(): void {
        this.createButton.disabled = !this.canCreate();
    }

    private canCreate(): boolean {
        return this.tool.customContentOption ? !!this.name.trim() : this.nodes.length > 0;
    }

    private cancel(): void {
        this.dialogRef.close(null);
    }

    private create(): void {
        if (!this.canCreate()) {
            return;
        }
        let win: Window;
        if (this.tool.customContentOption && !this.cordova.isRunningCordova()) {
            // the window must be opened within the user gesture, i.e. before the dialog is closed
            win = window.open(this.uiService.getLoadingSpinnerUrl());
        }
        this.dialogRef.close({ nodes: this.nodes, name: this.name, window: win });
    }

    private initDialogConfig(): void {
        const cancelButton = new DialogButton('CANCEL', { color: 'standard' }, () => this.cancel());
        this.createButton = new DialogButton('CREATE', { color: 'primary' }, () => this.create());
        this.createButton.disabled = !this.canCreate();
        this.dialogRef.patchConfig({
            title: 'WORKSPACE.LTI_V13_PLATFORM.NODE.CREATE.TITLE',
            subtitle: this.translate.instant('WORKSPACE.LTI_V13_PLATFORM.NODE.CREATE.NAME', {
                ltitool: this.tool.name,
            }),
            avatar: { kind: 'icon', icon: 'edit' },
            buttons: [cancelButton, this.createButton],
        });
        // nodes created by the deep-link flow have to be cleaned up if the dialog is dismissed
        this.dialogRef.beforeClosed().subscribe((result) => {
            if (!result) {
                this.ltiToolOptions.cancelDialogResult({ nodes: this.nodes });
            }
        });
    }

    private openDeepLinkFlow(): void {
        const url =
            '/edu-sharing/rest/ltiplatform/v13/generateLoginInitiationForm?appId=' +
            this.tool.appId +
            '&parentId=' +
            this.data.parent.ref.id;
        if (!window.open(url, '_blank')) {
            window.alert('popups are disabled');
        }
    }

    /**
     * Called by the deep-link response page opened via `openDeepLinkFlow`, see
     * `LTIPlatformApi#generateLoginInitiationForm`.
     */
    private registerDeepLinkCallback(): void {
        (window as any)['angularComponentReference'] = {
            component: this,
            zone: this.ngZone,
            loadAngularFunction: (nodeIds: string[], titles: string[]) =>
                this.deeplinkResponse(nodeIds, titles),
        };
    }

    private deeplinkResponse(nodeIds: string[], titles: string[]): void {
        this.name = titles[0];
        this.nodes = nodeIds.map(
            (nodeId, idx) => ({ ref: { id: nodeId }, name: titles[idx] } as Node),
        );
        this.updateButtons();
        // the user provided content, so the dialog should no longer close on a backdrop click
        this.dialogRef.patchConfig({ closable: Closable.Standard });
    }
}
