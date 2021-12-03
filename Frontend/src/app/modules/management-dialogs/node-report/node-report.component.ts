import { trigger } from '@angular/animations';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import {
    DialogButton,
    LoginResult,
    Node,
    RestConnectorService,
    RestIamService,
    RestNodeService,
} from '../../../core-module/core.module';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import { Toast } from '../../../core-ui-module/toast';
import { UIHelper } from '../../../core-ui-module/ui-helper';

@Component({
    selector: 'es-node-report',
    templateUrl: 'node-report.component.html',
    styleUrls: ['node-report.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
})
export class NodeReportComponent {
    reasons = ['UNAVAILABLE', 'INAPPROPRIATE_CONTENT', 'INVALID_METADATA', 'OTHER'];
    selectedReason: string;
    comment: string;
    email: string;
    _node: Node;
    isGuest: boolean;
    @Input() set node(node: Node) {
        this._node = node;
    }
    @Output() onCancel = new EventEmitter();
    @Output() onLoading = new EventEmitter();
    @Output() onDone = new EventEmitter();
    buttons: DialogButton[];

    constructor(
        private connector: RestConnectorService,
        private iam: RestIamService,
        private translate: TranslateService,
        private toast: Toast,
        private nodeApi: RestNodeService,
    ) {
        this.buttons = [
            new DialogButton('CANCEL', DialogButton.TYPE_CANCEL, () => this.cancel()),
            new DialogButton('NODE_REPORT.REPORT', DialogButton.TYPE_PRIMARY, () => this.report()),
        ];
        this.connector.isLoggedIn().subscribe((data: LoginResult) => {
            this.isGuest = data.isGuest;
            if (!data.isGuest) {
                this.iam.getUser().subscribe((user) => {
                    this.email = user.person.profile.email;
                });
            }
        });
    }

    cancel() {
        this.onCancel.emit();
    }

    done() {
        this.onDone.emit();
    }

    report() {
        if (!this.selectedReason) {
            this.toast.error(null, 'NODE_REPORT.REASON_REQUIRED');
            return;
        }
        if (!UIHelper.isEmail(this.email)) {
            this.toast.error(null, 'NODE_REPORT.EMAIL_REQUIRED');
            return;
        }
        this.onLoading.emit(true);
        this.nodeApi
            .reportNode(
                this._node.ref.id,
                this.getReasonAsString(),
                this.email,
                this.comment,
                this._node.ref.repo,
            )
            .subscribe(
                () => {
                    this.toast.toast('NODE_REPORT.DONE');
                    this.onLoading.emit(false);
                    this.onDone.emit();
                },
                (error: any) => {
                    this.onLoading.emit(false);
                    this.toast.error(error);
                },
            );
    }

    private getReasonAsString() {
        return (
            this.translate.instant('NODE_REPORT.REASONS.' + this.selectedReason) +
            ' (' +
            this.selectedReason +
            ')'
        );
    }
}
