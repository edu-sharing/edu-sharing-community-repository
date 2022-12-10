import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DialogButton, RestConnectorService } from '../../../core-module/core.module';
import { Toast } from '../../../core-ui-module/toast';
import { RestNodeService } from '../../../core-module/core.module';
import { Connector, Node } from '../../../core-module/core.module';
import { ConfigurationService } from '../../../core-module/core.module';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { RestIamService } from '../../../core-module/core.module';
import { TranslateService } from '@ngx-translate/core';
import { trigger } from '@angular/animations';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import { RestConstants } from '../../../core-module/core.module';
import { Router } from '@angular/router';
import { RestHelper } from '../../../core-module/core.module';
import { RestConnectorsService } from '../../../core-module/core.module';
import { FrameEventsService } from '../../../core-module/core.module';
import { OPEN_URL_MODE } from '../../../core-module/ui/ui-constants';
import { BridgeService } from '../../../core-bridge-module/bridge.service';
import { NodeHelperService } from '../../../core-ui-module/node-helper.service';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { BreadcrumbsService } from '../../../shared/components/breadcrumbs/breadcrumbs.service';

@Component({
    selector: 'es-node-variant',
    templateUrl: 'node-variant.component.html',
    styleUrls: ['node-variant.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
    providers: [BreadcrumbsService],
})
export class NodeVariantComponent {
    _node: Node;
    variantName: string;
    openViaConnector: Connector;
    licenseWarning: string;
    buttons: DialogButton[];
    @Input() set node(node: Node) {
        this._node = node;
        this.variantName = this.translate.instant('NODE_VARIANT.DEFAULT_NAME', {
            name: this._node.name,
        });
        this.openViaConnector = this.connectors.connectorSupportsEdit(node);
        let license = node.properties[RestConstants.CCM_PROP_LICENSE]
            ? node.properties[RestConstants.CCM_PROP_LICENSE][0]
            : '';
        if (license.startsWith('CC_BY') && license.indexOf('ND') != -1) {
            this.licenseWarning = 'ND';
        } else if (license.startsWith('COPYRIGHT')) {
            this.licenseWarning = 'COPYRIGHT';
        } else if (!license) {
            this.licenseWarning = 'NO_LICENSE';
        }
        this.updateButtons();
    }
    @Output() onLoading = new EventEmitter();
    @Output() onCancel = new EventEmitter();
    @Output() onDone = new EventEmitter();
    constructor(
        private connector: RestConnectorService,
        private iam: RestIamService,
        private translate: TranslateService,
        private connectors: RestConnectorsService,
        private config: ConfigurationService,
        private nodeHelper: NodeHelperService,
        private toast: Toast,
        private bridge: BridgeService,
        private events: FrameEventsService,
        private router: Router,
        private breadcrumbsService: BreadcrumbsService,
        private nodeApi: RestNodeService,
        private dialogs: DialogsService,
    ) {
        this.updateBreadcrumbs(RestConstants.INBOX);
        this.updateButtons();
    }
    public cancel() {
        this.onCancel.emit();
    }

    public create() {
        if (!this.breadcrumbsService.breadcrumbs$.value?.length) {
            return;
        }
        let win: any = null;
        if (this.openViaConnector) {
            win = UIHelper.getNewWindow(this.connector);
        }
        this.onLoading.emit(true);
        this.nodeApi
            .forkNode(
                this.breadcrumbsService.breadcrumbs$.value[
                    this.breadcrumbsService.breadcrumbs$.value.length - 1
                ].ref.id,
                this._node.ref.id,
            )
            .subscribe(
                (created) => {
                    this.nodeApi
                        .editNodeMetadata(
                            created.node.ref.id,
                            RestHelper.createNameProperty(this.variantName),
                        )
                        .subscribe(
                            (edited) => {
                                this.onLoading.emit(false);
                                if (this.openViaConnector) {
                                    UIHelper.openConnector(
                                        this.connectors,
                                        this.iam,
                                        this.events,
                                        this.toast,
                                        edited.node,
                                        null,
                                        win,
                                    );
                                    UIHelper.goToWorkspaceFolder(
                                        this.nodeApi,
                                        this.router,
                                        this.connector.getCurrentLogin(),
                                        this.breadcrumbsService.breadcrumbs$.value[
                                            this.breadcrumbsService.breadcrumbs$.value.length - 1
                                        ].ref.id,
                                    );
                                } else {
                                    let additional = {
                                        link: {
                                            caption: 'NODE_VARIANT.CREATED_LINK',
                                            callback: () => {
                                                UIHelper.goToWorkspaceFolder(
                                                    this.nodeApi,
                                                    this.router,
                                                    this.connector.getCurrentLogin(),
                                                    this.breadcrumbsService.breadcrumbs$.value[
                                                        this.breadcrumbsService.breadcrumbs$.value
                                                            .length - 1
                                                    ].ref.id,
                                                );
                                            },
                                        },
                                    };
                                    this.toast.toast(
                                        'NODE_VARIANT.CREATED',
                                        {
                                            folder: this.breadcrumbsService.breadcrumbs$.value[
                                                this.breadcrumbsService.breadcrumbs$.value.length -
                                                    1
                                            ].name,
                                        },
                                        null,
                                        null,
                                        additional,
                                    );
                                }
                                this.onDone.emit();
                            },
                            (error) => {
                                this.onLoading.emit(false);
                                this.nodeHelper.handleNodeError(this.variantName, error);
                                if (win) win.close();
                            },
                        );
                },
                (error) => {
                    this.onLoading.emit(false);
                    console.log(error);
                    if (error.error?.error?.indexOf('DAORestrictedAccessException') !== -1) {
                        this.toast.error(null, 'RESTRICTED_ACCESS_COPY_ERROR');
                    } else {
                        this.nodeHelper.handleNodeError(this.variantName, error);
                    }
                    if (win) win.close();
                },
            );
    }

    async chooseDirectory() {
        const dialogRef = await this.dialogs.openFileChooserDialog({
            title: 'NODE_VARIANT.FILE_PICKER_TITLE',
            pickDirectory: true,
            writeRequired: true,
        });
        dialogRef.afterClosed().subscribe((result) => {
            if (result) {
                this.setDirectory(result);
            }
        });
    }

    setDirectory(event: Node[]) {
        this.updateBreadcrumbs(event[0].ref.id);
    }

    private updateBreadcrumbs(id: string) {
        this.nodeApi.getNodeParents(id, false).subscribe((parents) => {
            this.breadcrumbsService.setNodePath(parents.nodes.reverse());
        });
    }
    openLicense() {
        UIHelper.openUrl(
            this.getLicenseUrl(),
            this.connector.getBridgeService(),
            OPEN_URL_MODE.BlankSystemBrowser,
        );
    }

    getLicenseUrl(): string {
        return this.nodeHelper.getLicenseUrlByString(
            this._node.properties[RestConstants.CCM_PROP_LICENSE],
            this._node.properties[RestConstants.CCM_PROP_LICENSE_CC_VERSION],
        );
    }
    updateButtons(): any {
        this.buttons = [
            new DialogButton('CANCEL', { color: 'standard' }, () => this.cancel()),
            new DialogButton(
                'NODE_VARIANT.CREATE' + (this.openViaConnector ? '_EDIT' : ''),
                { color: 'primary' },
                () => this.create(),
            ),
        ];
    }
}
