import { trigger } from '@angular/animations';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import {
    DialogButton,
    Node,
    NodeShare,
    RestConstants,
    RestNodeService,
} from '../../../core-module/core.module';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import { DateHelper } from '../../../core-ui-module/DateHelper';
import { Toast } from '../../../core-ui-module/toast';
import { UIHelper } from '../../../core-ui-module/ui-helper';

@Component({
    selector: 'es-workspace-share-link',
    templateUrl: 'share-link.component.html',
    styleUrls: ['share-link.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
})
export class WorkspaceShareLinkComponent {
    @Input() priority = 1;
    @Input() set node(node: Node) {
        this.setNode(node);
    }

    @Output() onClose = new EventEmitter();
    @Output() onLoading = new EventEmitter();

    loading = true;
    _node: Node;
    enabled = false;
    expiry = false;
    passwordEnabled = false;
    passwordString: string;
    _expiryDate: Date;
    passwordAlreadySet: boolean;
    buttons: DialogButton[];
    today = new Date();
    set expiryDate(date: Date) {
        this._expiryDate = date;
        this.setExpiry(true);
    }
    get expiryDate() {
        return this._expiryDate;
    }
    currentShare: NodeShare;
    private currentDate: number;

    constructor(
        private nodeService: RestNodeService,
        private translate: TranslateService,
        private toast: Toast,
    ) {
        this.buttons = [new DialogButton('CLOSE', { color: 'primary' }, () => this.cancel())];
    }

    copyClipboard() {
        if (!this.enabled) return;
        try {
            UIHelper.copyToClipboard(this.currentShare.url);
            this.toast.toast('WORKSPACE.SHARE_LINK.COPIED_CLIPBOARD');
        } catch (e) {
            this.toast.error(null, 'WORKSPACE.SHARE_LINK.COPIED_CLIPBOARD_ERROR');
        }
    }

    cancel() {
        this.onClose.emit();
    }

    setEnabled(value: boolean) {
        if (value) {
            this.createShare();
            //this.updateShare(RestConstants.SHARE_EXPIRY_UNLIMITED);
        } else {
            this.deleteShare();
            this.expiry = false;
            this.passwordEnabled = false;
        }
    }

    private setNode(node: Node) {
        this._node = node;
        this.loading = true;
        this.nodeService.getNodeShares(node.ref.id, RestConstants.SHARE_LINK).subscribe(
            (data: NodeShare[]) => {
                this._expiryDate = new Date(new Date().getTime() + 3600 * 24 * 1000);
                // console.log(data);
                if (data.length) {
                    this.currentShare = data[0];
                    this.expiry = data[0].expiryDate > 0;
                    this.passwordEnabled = data[0].password;
                    if (this.passwordEnabled) {
                        this.passwordAlreadySet = true;
                    }
                    this.currentDate = data[0].expiryDate;
                    if (this.expiry) {
                        this.expiryDate = new Date(data[0].expiryDate);
                    }
                    if (data[0].expiryDate == 0) {
                        this.enabled = false;
                        this.loading = false;
                        this.currentShare.url = this.translate.instant(
                            'WORKSPACE.SHARE_LINK.DISABLED',
                        );
                    } else {
                        this.enabled = true;
                        this.loading = false;
                    }
                } else {
                    this.createShare();
                }
            },
            (error: any) => this.toast.error(error),
        );
    }

    private updateShare(date = this.currentDate) {
        // console.log(date);
        this.currentShare.url = this.translate.instant('LOADING');
        this.nodeService
            .updateNodeShare(
                this._node.ref.id,
                this.currentShare.shareId,
                date,
                this.passwordEnabled ? this.passwordString : '',
            )
            .subscribe((data: NodeShare) => {
                this.currentShare = data;
                if (date == 0)
                    this.currentShare.url = this.translate.instant('WORKSPACE.SHARE_LINK.DISABLED');
                // console.log(data);
            });
    }

    setExpiry(value: boolean) {
        if (!this.enabled) return;
        this.currentDate = value
            ? DateHelper.getDateFromDatepicker(this.expiryDate).getTime()
            : RestConstants.SHARE_EXPIRY_UNLIMITED;
        this.updateShare();
    }

    setPassword() {
        if (!this.passwordEnabled) {
            this.passwordString = null;
            this.passwordAlreadySet = false;
        }
        this.updateShare();
    }

    private createShare() {
        this.loading = true;
        this.nodeService.createNodeShare(this._node.ref.id).subscribe(
            (data: NodeShare) => {
                this.passwordAlreadySet = false;
                this.currentShare = data;
                this.loading = false;
                this.enabled = true;
            },
            (error: any) => this.toast.error(error),
        );
    }

    private deleteShare() {
        this.loading = true;
        this.nodeService
            .deleteNodeShare(this._node.ref.id, this.currentShare.shareId)
            .subscribe(() => {
                (this.currentShare as any) = {};
                this.loading = false;
            });
    }
}
