import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { RestHelper } from '../../core-module/core.module';
import { CardDialogCardConfig } from './card-dialog/card-dialog-config';
import { CardDialogRef } from './card-dialog/card-dialog-ref';
import { CardDialogService } from './card-dialog/card-dialog.service';
import { NodeEmbedDialogData } from './dialog-modules/node-embed-dialog/node-embed-dialog.component';
import { NodeReportDialogData } from './dialog-modules/node-report-dialog/node-report/node-report.component';
import { QrDialogData } from './dialog-modules/qr-dialog/qr-dialog.component';

@Injectable({
    providedIn: 'root',
})
export class DialogsService {
    constructor(private cardDialog: CardDialogService, private translate: TranslateService) {}

    async openQrDialog(data: QrDialogData): Promise<CardDialogRef<void>> {
        const { QrDialogComponent } = await import('./dialog-modules/qr-dialog/qr-dialog.module');
        return this.cardDialog.open(QrDialogComponent, {
            cardConfig: {
                title: 'OPTIONS.QR_CODE',
                ...this.cardWithNode(data.node),
                contentPadding: 0,
            },
            data,
        });
    }

    async openNodeEmbedDialog(data: NodeEmbedDialogData): Promise<CardDialogRef<void>> {
        const { NodeEmbedDialogComponent } = await import(
            './dialog-modules/node-embed-dialog/node-embed-dialog.module'
        );
        return this.cardDialog.open(NodeEmbedDialogComponent, {
            cardConfig: {
                title: 'OPTIONS.EMBED',
                ...this.cardWithNode(data.node),
                // Set size via NodeEmbedDialogComponent, so it can choose fitting values for its
                // responsive layouts.
                contentPadding: 0,
            },
            data,
        });
    }

    async openNodeReportDialog(data: NodeReportDialogData): Promise<CardDialogRef<void>> {
        const { NodeReportComponent } = await import(
            './dialog-modules/node-report-dialog/node-report-dialog.module'
        );
        return this.cardDialog.open(NodeReportComponent, {
            cardConfig: {
                title: 'NODE_REPORT.TITLE',
                ...this.cardWithNode(data.node),
            },
            data,
        });
    }

    private cardWithNode(node: Node): Partial<CardDialogCardConfig> {
        return {
            avatar: { kind: 'image', url: node.iconURL },
            subtitle: RestHelper.getTitle(node),
        };
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
