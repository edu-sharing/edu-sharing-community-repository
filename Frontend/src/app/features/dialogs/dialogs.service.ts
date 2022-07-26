import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { configForNode } from './card-dialog/card-dialog-config';
import { CardDialogRef } from './card-dialog/card-dialog-ref';
import { CardDialogService } from './card-dialog/card-dialog.service';
import { NodeEmbedDialogData } from './dialog-modules/node-embed-dialog/node-embed-dialog.component';
import { NodeInfoDialogData } from './dialog-modules/node-info-dialog/node-info/node-info.component';
import { NodeReportDialogData } from './dialog-modules/node-report-dialog/node-report/node-report.component';
import { QrDialogData } from './dialog-modules/qr-dialog/qr-dialog.component';

@Injectable({
    providedIn: 'root',
})
export class DialogsService {
    get openDialogs() {
        return this.cardDialog.openDialogs;
    }

    constructor(private cardDialog: CardDialogService, private translate: TranslateService) {}

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
        const { NodeReportComponent } = await import(
            './dialog-modules/node-report-dialog/node-report-dialog.module'
        );
        return this.cardDialog.open(NodeReportComponent, {
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
}
