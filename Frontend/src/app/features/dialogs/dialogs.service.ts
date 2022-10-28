import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { RestConnectorService } from '../../core-module/core.module';
import { Closable, configForNode } from './card-dialog/card-dialog-config';
import { CardDialogRef } from './card-dialog/card-dialog-ref';
import { CardDialogService } from './card-dialog/card-dialog.service';
import {
    AddFolderDialogData,
    AddFolderDialogResult,
} from './dialog-modules/add-folder-dialog/add-folder-dialog-data';
import {
    FileChooserDialogData,
    FileChooserDialogResult,
} from './dialog-modules/file-chooser-dialog/file-chooser-dialog-data';
import {
    LicenseAgreementDialogData,
    LicenseAgreementDialogResult,
} from './dialog-modules/license-agreement-dialog/license-agreement-dialog-data';
import { NodeEmbedDialogData } from './dialog-modules/node-embed-dialog/node-embed-dialog.component';
import { NodeInfoDialogData } from './dialog-modules/node-info-dialog/node-info-dialog.component';
import { NodeReportDialogData } from './dialog-modules/node-report-dialog/node-report-dialog.component';
import { QrDialogData } from './dialog-modules/qr-dialog/qr-dialog.component';

@Injectable({
    providedIn: 'root',
})
export class DialogsService {
    get openDialogs() {
        return this.cardDialog.openDialogs;
    }

    constructor(
        private cardDialog: CardDialogService,
        private translate: TranslateService,
        // TODO: Move the methods we use of `RestConnectorService` to a utils function if possible.
        private restConnector: RestConnectorService,
    ) {}

    async openQrDialog(data: QrDialogData): Promise<CardDialogRef<QrDialogData, void>> {
        const { QrDialogComponent } = await import('./dialog-modules/qr-dialog/qr-dialog.module');
        return this.cardDialog.open(QrDialogComponent, {
            title: 'OPTIONS.QR_CODE',
            ...configForNode(data.node),
            contentPadding: 0,
            data,
        });
    }

    async openNodeEmbedDialog(
        data: NodeEmbedDialogData,
    ): Promise<CardDialogRef<NodeEmbedDialogData, void>> {
        const { NodeEmbedDialogComponent } = await import(
            './dialog-modules/node-embed-dialog/node-embed-dialog.module'
        );
        return this.cardDialog.open(NodeEmbedDialogComponent, {
            title: 'OPTIONS.EMBED',
            ...configForNode(data.node),
            // Set size via NodeEmbedDialogComponent, so it can choose fitting values for its
            // responsive layouts.
            contentPadding: 0,
            data,
        });
    }

    async openNodeReportDialog(
        data: NodeReportDialogData,
    ): Promise<CardDialogRef<NodeReportDialogData, void>> {
        const { NodeReportDialogComponent } = await import(
            './dialog-modules/node-report-dialog/node-report-dialog.module'
        );
        return this.cardDialog.open(NodeReportDialogComponent, {
            title: 'NODE_REPORT.TITLE',
            ...configForNode(data.node),
            data,
        });
    }

    async openNodeInfoDialog(
        data: NodeInfoDialogData,
    ): Promise<CardDialogRef<NodeInfoDialogData, void>> {
        const { NodeInfoComponent } = await import(
            './dialog-modules/node-info-dialog/node-info-dialog.module'
        );
        return this.cardDialog.open(NodeInfoComponent, {
            // Header will be configured by the component
            data,
        });
    }

    async openNodeStoreDialog(): Promise<CardDialogRef<void, void>> {
        const { SearchNodeStoreComponent } = await import(
            './dialog-modules/node-store-dialog/node-store-dialog.module'
        );
        return this.cardDialog.open(SearchNodeStoreComponent, {
            title: 'SEARCH.NODE_STORE.TITLE',
            width: 400,
            minHeight: 'min(95%, 600px)',
            contentPadding: 0,
        });
    }

    async openLicenseAgreementDialog(
        data: LicenseAgreementDialogData,
    ): Promise<CardDialogRef<LicenseAgreementDialogData, LicenseAgreementDialogResult>> {
        const { LicenseAgreementDialogComponent } = await import(
            './dialog-modules/license-agreement-dialog/license-agreement-dialog.module'
        );
        return this.cardDialog.open(LicenseAgreementDialogComponent, {
            title: 'LICENSE_AGREEMENT.TITLE',
            closable: Closable.Disabled,
            width: 900,
            data,
        });
    }

    async openAccessibilityDialog(): Promise<CardDialogRef<void, void>> {
        const { AccessibilityDialogComponent } = await import(
            './dialog-modules/accessibility-dialog/accessibility-dialog.module'
        );
        return this.cardDialog.open(AccessibilityDialogComponent, {
            title: 'ACCESSIBILITY.TITLE',
            subtitle: 'ACCESSIBILITY.SUBTITLE',
            avatar: { kind: 'icon', icon: 'accessibility' },
            width: 700,
        });
    }

    async openFileChooserDialog(
        data: Partial<FileChooserDialogData>,
    ): Promise<CardDialogRef<FileChooserDialogData, FileChooserDialogResult>> {
        const { FileChooserDialogComponent } = await import(
            './dialog-modules/file-chooser-dialog/file-chooser-dialog.module'
        );
        return this.cardDialog.open(FileChooserDialogComponent, {
            contentPadding: 0,
            height: 800,
            data: { ...new FileChooserDialogData(), ...(data ?? {}) },
        });
    }

    async openAddFolderDialog(
        data: AddFolderDialogData,
    ): Promise<CardDialogRef<AddFolderDialogData, AddFolderDialogResult>> {
        const { AddFolderDialogComponent } = await import(
            './dialog-modules/add-folder-dialog/add-folder-dialog.module'
        );
        return this.cardDialog.open(AddFolderDialogComponent, {
            title: 'WORKSPACE.ADD_FOLDER_TITLE',
            ...configForNode(data.parent),
            avatar: { kind: 'image', url: this.restConnector.getThemeMimeIconSvg('folder.svg') },
            width: 600,
            autoFocus: '[autofocus=""]',
            data,
        });
    }
}
