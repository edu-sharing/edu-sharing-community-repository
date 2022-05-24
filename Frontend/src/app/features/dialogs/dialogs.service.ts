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

    // private cardWithNodes(nodes: Node[]): Partial<CardDialogCardConfig> {
    //     if (nodes.length === 0) {
    //         return {};
    //     } else if (nodes.length === 1) {
    //         return this.cardWithNode(nodes[0]);
    //     } else {
    //         return {
    //             avatar: null,
    //             subtitle: this.translate.get('CARD_SUBTITLE_MULTIPLE', { count: nodes.length }),
    //         };
    //     }
    // }
}
