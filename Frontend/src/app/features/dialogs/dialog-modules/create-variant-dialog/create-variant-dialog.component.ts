import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { LocalEventsService, OPEN_URL_MODE } from 'ngx-edu-sharing-ui';
import {
    Connector,
    DialogButton,
    RestConnectorService,
    RestConnectorsService,
    RestConstants,
    UIService,
} from '../../../../core-module/core.module';
import { Node, NodeService } from 'ngx-edu-sharing-api';
import { NodeHelperService } from '../../../../services/node-helper.service';
import { Toast } from '../../../../services/toast';
import { UIHelper } from '../../../../core-ui-module/ui-helper';
import { DialogsService } from '../../dialogs.service';
import { BreadcrumbsService } from '../../../../shared/components/breadcrumbs/breadcrumbs.service';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { CreateVariantDialogData, CreateVariantDialogResult } from './create-variant-dialog-data';
import { firstValueFrom, Observable } from 'rxjs';

@Component({
    selector: 'es-create-variant-dialog',
    templateUrl: './create-variant-dialog.component.html',
    styleUrls: ['./create-variant-dialog.component.scss'],
    standalone: false,
    providers: [BreadcrumbsService],
})
export class CreateVariantDialogComponent {
    data = inject<CreateVariantDialogData>(CARD_DIALOG_DATA);
    private dialogRef =
        inject<CardDialogRef<CreateVariantDialogData, CreateVariantDialogResult>>(CardDialogRef);
    private breadcrumbsService = inject(BreadcrumbsService);
    private connector = inject(RestConnectorService);
    private connectors = inject(RestConnectorsService);
    private localEvents = inject(LocalEventsService);
    private dialogs = inject(DialogsService);
    private uiService = inject(UIService);
    private nodeService = inject(NodeService);
    private nodeHelper = inject(NodeHelperService);
    private router = inject(Router);
    private toast = inject(Toast);
    private translate = inject(TranslateService);

    variantName: string;
    licenseWarning: string;

    private _openViaConnector: Connector;
    private _directory: string;

    constructor() {
        void this._initNode();
        this._updateButtons();
    }

    private async _initNode() {
        const node = this.data.node;
        this.variantName = this.translate.instant('NODE_VARIANT.DEFAULT_NAME', {
            name: this.data.node.name,
        });
        this._directory = RestConstants.INBOX;
        try {
            const parent = await firstValueFrom(this.nodeService.getNode(node.parent.id));
            if (this.nodeHelper.getNodesRight([parent], RestConstants.ACCESS_ADD_CHILDREN)) {
                this._directory = parent.ref.id;
            }
        } catch (e) {
            e.preventDefault();
        }
        this._updateBreadcrumbs();
        this._openViaConnector = this.connectors.connectorSupportsEdit(node);
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
    }

    private _cancel() {
        this.dialogRef.close(null);
    }

    private _done() {
        this.dialogRef.close(null);
    }

    private _create() {
        if (!this.breadcrumbsService.breadcrumbs$.value?.length) {
            return;
        }
        let win: any = null;
        if (this._openViaConnector) {
            win = UIHelper.getNewWindow(this.connector);
        }
        this.dialogRef.patchState({ isLoading: true });
        this.nodeService
            .forkNode(
                this.breadcrumbsService.breadcrumbs$.value[
                    this.breadcrumbsService.breadcrumbs$.value.length - 1
                ].ref.id,
                this.data.node.ref.id,
                this.variantName,
            )
            .subscribe(
                (created) => {
                    this.dialogRef.patchState({ isLoading: false });
                    this.localEvents.nodesCreated.emit([created.node]);
                    if (this._openViaConnector) {
                        void this.uiService.editConnector(created.node, { win });
                        UIHelper.goToWorkspaceFolder(
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
                                        this.router,
                                        this.connector.getCurrentLogin(),
                                        this.breadcrumbsService.breadcrumbs$.value[
                                            this.breadcrumbsService.breadcrumbs$.value.length - 1
                                        ].ref.id,
                                    );
                                },
                            },
                        };
                        this.toast.toast(
                            'NODE_VARIANT.CREATED',
                            {
                                folder: this.breadcrumbsService.breadcrumbs$.value[
                                    this.breadcrumbsService.breadcrumbs$.value.length - 1
                                ].name,
                            },
                            null,
                            null,
                            additional,
                        );
                    }
                    this._done();
                },
                (error) => {
                    this.dialogRef.patchState({ isLoading: false });
                    console.warn(error);
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
                this._setDirectory(result);
            }
        });
    }

    private _setDirectory(event: Node[]) {
        this._directory = event[0].ref.id;
        this._updateBreadcrumbs();
    }

    private _updateBreadcrumbs() {
        this.nodeService.getParents(this._directory, { fullPath: false }).subscribe((parents) => {
            this.breadcrumbsService.setNodePath(parents.nodes.reverse());
        });
    }

    openLicense(url: string) {
        UIHelper.openUrl(url, this.connector.getBridgeService(), OPEN_URL_MODE.BlankSystemBrowser);
    }

    getLicenseUrl(): Observable<string | null> {
        return this.nodeHelper.getLicenseUrlByString(
            this.data.node.properties[RestConstants.CCM_PROP_LICENSE]?.[0],
            this.data.node.properties[RestConstants.CCM_PROP_LICENSE_CC_VERSION]?.[0],
            this.data.node.properties[RestConstants.CCM_PROP_LICENSE_CC_LOCALE]?.[0],
        );
    }

    private _updateButtons(): void {
        const buttons = [
            new DialogButton('CANCEL', { color: 'standard' }, () => this._cancel()),
            new DialogButton(
                'NODE_VARIANT.CREATE' + (this._openViaConnector ? '_EDIT' : ''),
                { color: 'primary' },
                () => this._create(),
            ),
        ];
        this.dialogRef.patchConfig({ buttons });
    }
}
