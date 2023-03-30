import { Component, EventEmitter, Input, NgZone, Output, ViewChild } from '@angular/core';
import {
    ConfigurationService,
    DialogButton,
    FrameEventsService,
    Node,
    RestConnectorService,
    RestConnectorsService,
    RestConstants,
    RestHelper,
    RestIamService,
    RestNodeService,
} from '../../../core-module/core.module';
import { Toast } from '../../../core-ui-module/toast';
import { TranslateService } from '@ngx-translate/core';
import { trigger } from '@angular/animations';
import { UIAnimation } from '../../../../../projects/edu-sharing-ui/src/lib/util/ui-animation';
import { Router } from '@angular/router';
import { BridgeService } from '../../../core-bridge-module/bridge.service';
import { SimpleEditMetadataComponent } from './simple-edit-metadata/simple-edit-metadata.component';
import { SimpleEditInviteComponent } from './simple-edit-invite/simple-edit-invite.component';
import { SimpleEditLicenseComponent } from './simple-edit-license/simple-edit-license.component';
import { forkJoin, Observable } from 'rxjs';
import { CardType } from '../../../shared/components/card/card.component';
import { UIHelper } from '../../../core-ui-module/ui-helper';

export interface SimpleEditCloseEvent {
    reason: 'abort' | 'done' | 'temporary';
    nodes?: Node[];
}

@Component({
    selector: 'es-simple-edit-dialog',
    templateUrl: 'simple-edit-dialog.component.html',
    styleUrls: ['simple-edit-dialog.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
})
export class SimpleEditDialogComponent {
    @ViewChild('metadata') metadata: SimpleEditMetadataComponent;
    @ViewChild('invite') invite: SimpleEditInviteComponent;
    @ViewChild('license') license: SimpleEditLicenseComponent;
    _nodes: Node[];
    buttons: DialogButton[];
    /**
     * was this dialog called directly after upload
     * if true, the ui will behave a bit differently
     */
    @Input() fromUpload = false;
    initState: { license: boolean; invite: boolean };
    tpInvite: boolean;
    tpLicense: boolean;
    @Input() set nodes(nodes: Node[]) {
        this._nodes = nodes;
        this.initState = {
            invite: false,
            license: false,
        };
        this.toast.showProgressDialog();
        this.updateButtons();
    }
    @Output() onClose = new EventEmitter<SimpleEditCloseEvent>();
    @Output() onOpenMetadata = new EventEmitter<Node[]>();
    @Output() onOpenInvite = new EventEmitter<Node[]>();
    @Output() onOpenLicense = new EventEmitter<Node[]>();
    constructor(
        private connector: RestConnectorService,
        private iam: RestIamService,
        private translate: TranslateService,
        private connectors: RestConnectorsService,
        private config: ConfigurationService,
        private toast: Toast,
        private bridge: BridgeService,
        private ngZone: NgZone,
        private events: FrameEventsService,
        private router: Router,
        private nodeApi: RestNodeService,
    ) {
        this.connector
            .hasToolPermission(RestConstants.TOOLPERMISSION_INVITE)
            .subscribe((tp) => (this.tpInvite = tp));
        this.connector
            .hasToolPermission(RestConstants.TOOLPERMISSION_LICENSE)
            .subscribe((tp) => (this.tpLicense = tp));
        this.updateButtons();
        UIHelper.waitForComponent(this.ngZone, this, 'metadata').subscribe(() => {
            this.metadata.isInited.subscribe(() => this.updateInitState());
        });
    }
    public cancel() {
        this.onClose.emit({ reason: 'abort' });
    }
    updateButtons(): any {
        this.buttons = [
            new DialogButton('CANCEL', { color: 'standard' }, () => this.cancel()),
            new DialogButton('SAVE', { color: 'primary' }, () => this.save()),
        ];
    }

    save(callback: () => void = null) {
        // validate inputs first
        if (!this.metadata.validate()) {
            return;
        }
        this.toast.showProgressDialog();
        this.metadata.save().subscribe(
            () => {
                this.invite.save().subscribe(
                    () => {
                        this.license.save().subscribe(
                            () => {
                                forkJoin(
                                    this._nodes.map((n) =>
                                        this.nodeApi.getNodeMetadata(n.ref.id, [RestConstants.ALL]),
                                    ),
                                ).subscribe((nodes) => {
                                    this.toast.toast(
                                        'SIMPLE_EDIT.SAVED' +
                                            (nodes.length === 1 ? '' : '_MULTIPLE'),
                                        { name: nodes[0].node.name, count: nodes.length },
                                    );
                                    this.onClose.emit({
                                        reason: 'done',
                                        nodes: nodes.map((n) => n.node),
                                    });
                                    if (callback) {
                                        callback();
                                    }
                                    this.toast.closeModalDialog();
                                });
                            },
                            (error) => {
                                this.toast.error(error);
                                this.toast.closeModalDialog();
                            },
                        );
                    },
                    (error) => {
                        this.toast.error(error);
                        this.toast.closeModalDialog();
                    },
                );
            },
            (error) => {
                this.toast.error(error);
                this.toast.closeModalDialog();
            },
        );
    }
    checkIsDirty() {
        console.log(
            'mds dirty: ' +
                this.metadata.isDirty() +
                ' invite: ' +
                this.invite.isDirty() +
                ' license: ' +
                this.license.isDirty(),
        );
        return this.metadata.isDirty() || this.invite.isDirty() || this.license.isDirty();
    }
    openDialog(callback: () => void, force = false) {
        if (this.checkIsDirty() && !force) {
            this.showDirtyDialog(() => this.openDialog(callback, true));
            return;
        }
        this.onClose.emit({ reason: 'temporary', nodes: this._nodes });
        callback();
    }
    openMetadata(force = false) {
        this.openDialog(() => this.onOpenMetadata.emit(this._nodes));
    }
    openInvite(force = false) {
        this.openDialog(() => this.onOpenInvite.emit(this._nodes));
    }
    openLicense(force = false) {
        this.openDialog(() => this.onOpenLicense.emit(this._nodes));
    }

    private showDirtyDialog(callback: () => void) {
        this.toast.showConfigurableDialog({
            title: 'SIMPLE_EDIT.DIRTY.TITLE',
            message: 'SIMPLE_EDIT.DIRTY.MESSAGE',
            isCancelable: true,
            cardType: CardType.Question,
            buttons: [
                new DialogButton('DISCARD', { color: 'standard' }, () => {
                    this.toast.closeModalDialog();
                    this.onClose.emit({ reason: 'abort', nodes: this._nodes });
                    callback();
                }),
                new DialogButton('SAVE', { color: 'primary' }, () => {
                    this.toast.closeModalDialog();
                    this.save(callback);
                }),
            ],
        });
    }
    updateInitState(type = '') {
        if (type) {
            (this.initState as any)[type] = true;
        }
        if (this.metadata.isInited && this.initState.invite && this.initState.license) {
            this.toast.closeModalDialog();
        }
    }

    handleError(error: any) {
        if (error) {
            this.toast.error(error);
        }
        this.toast.closeModalDialog();
        this.onClose.emit({ reason: 'abort' });
    }

    hasPermission(permission: string) {
        return this._nodes.find((n) => n.access.indexOf(permission) === -1) == null;
    }
}
