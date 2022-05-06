import { Component, Inject, OnInit } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { NodeHelperService } from '../../../../core-ui-module/node-helper.service';
import { CardDialogContentComponent } from '../../card-dialog/card-dialog-config';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog.service';

export interface QrDialogData {
    node: Node;
}

@Component({
    selector: 'es-qr-dialog',
    templateUrl: './qr-dialog.component.html',
    styleUrls: ['./qr-dialog.component.scss'],
})
export class QrDialogComponent implements OnInit, CardDialogContentComponent<QrDialogData, void> {
    url: string;

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: QrDialogData,
        private nodeHelper: NodeHelperService,
    ) {}

    ngOnInit(): void {
        this.url = this.nodeHelper.getNodeUrl(this.data.node);
    }
}
