import { Component, OnInit, inject } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { NodeHelperService } from '../../../../services/node-helper.service';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';

export interface QrDialogData {
    node: Node;
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

    url: string;

    ngOnInit(): void {
        this.url = this.nodeHelper.getNodeUrl(this.data.node);
    }
}
