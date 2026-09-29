import { Clipboard } from '@angular/cdk/clipboard';
import { Component, OnInit, inject } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { NodeHelperService } from '../../../../services/node-helper.service';
import { Toast, ToastType } from '../../../../services/toast';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';

export interface QrDialogData {
    node: Node;
    showShortlink?: boolean;
}

@Component({
    selector: 'es-qr-dialog',
    templateUrl: './qr-dialog.component.html',
    styleUrls: ['./qr-dialog.component.scss'],
    standalone: false,
})
export class QrDialogComponent implements OnInit {
    data = inject<QrDialogData>(CARD_DIALOG_DATA);
    private nodeHelper = inject(NodeHelperService);
    private clipboard = inject(Clipboard);
    private toast = inject(Toast);

    url: string;
    shortUrl: string;

    ngOnInit(): void {
        this.url = this.nodeHelper.getNodeUrl(this.data.node);
        if (this.data.showShortlink ?? true) {
            this.shortUrl = this.nodeHelper.getNodeUrl(this.data.node, null, true);
        }
    }

    copyShortlink(): void {
        if (this.clipboard.copy(this.shortUrl)) {
            this.toast.show({
                message: 'WORKSPACE.SHARE_LINK.COPIED_CLIPBOARD',
                type: 'info',
                subtype: ToastType.InfoSimple,
            });
        } else {
            this.toast.error(null, 'WORKSPACE.SHARE_LINK.COPIED_CLIPBOARD_ERROR');
        }
    }
}
