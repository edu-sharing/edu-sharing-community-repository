import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import {
    DialogButton,
    RestConnectorService,
    RestConstants,
    RestHelper,
    RestNodeService,
} from '../../../../core-module/core.module';
import { Node } from 'ngx-edu-sharing-api';
import { Toast } from '../../../../services/toast';
import { UIHelper } from '../../../../core-ui-module/ui-helper';
import { DialogsService } from '../../dialogs.service';
import { BreadcrumbsService } from '../../../../shared/components/breadcrumbs/breadcrumbs.service';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { CreateMapLinkDialogData, CreateMapLinkDialogResult } from './create-map-link-dialog-data';

@Component({
    selector: 'es-create-map-link-dialog',
    templateUrl: './create-map-link-dialog.component.html',
    styleUrls: ['./create-map-link-dialog.component.scss'],
    standalone: false,
    providers: [BreadcrumbsService],
})
export class CreateMapLinkDialogComponent implements OnInit {
    data = inject<CreateMapLinkDialogData>(CARD_DIALOG_DATA);
    private dialogRef =
        inject<CardDialogRef<CreateMapLinkDialogData, CreateMapLinkDialogResult>>(CardDialogRef);
    private connector = inject(RestConnectorService);
    private toast = inject(Toast);
    private router = inject(Router);
    private nodeApi = inject(RestNodeService);
    private breadcrumbsService = inject(BreadcrumbsService);
    private dialogs = inject(DialogsService);

    name: string;

    private readonly buttons = [
        new DialogButton('CANCEL', { color: 'standard' }, () => this.dialogRef.close(null)),
        new DialogButton('MAP_LINK.CREATE', { color: 'primary' }, () => this.createLink()),
    ];

    ngOnInit(): void {
        this.dialogRef.patchConfig({ buttons: this.buttons });
        this.name = this.data.node.name;
        this.updateBreadcrumbs(RestConstants.INBOX);
        this.updateButtons();
    }

    updateButtons(): any {
        this.buttons[1].disabled = !this.name;
    }

    async chooseDirectory() {
        const dialogRef = await this.dialogs.openFileChooserDialog({
            title: 'MAP_LINK.FILE_PICKER_TITLE',
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

    private createLink() {
        const properties = RestHelper.createNameProperty(this.name);
        properties[RestConstants.CCM_PROP_MAP_REF_TARGET] = [
            RestHelper.createSpacesStoreRef(this.data.node),
        ];
        this.toast.showProgressSpinner();
        this.nodeApi
            .createNode(
                this.breadcrumbsService.breadcrumbs$.value[
                    this.breadcrumbsService.breadcrumbs$.value.length - 1
                ].ref.id,
                RestConstants.CCM_TYPE_MAP,
                [RestConstants.CCM_ASPECT_MAP_REF],
                properties,
            )
            .subscribe(
                ({ node }) => {
                    const additional = {
                        link: {
                            caption: 'MAP_LINK.CREATED_LINK',
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
                        'MAP_LINK.CREATED',
                        {
                            folder: this.breadcrumbsService.breadcrumbs$.value[
                                this.breadcrumbsService.breadcrumbs$.value.length - 1
                            ].name,
                        },
                        null,
                        null,
                        additional,
                    );
                    this.toast.closeProgressSpinner();
                    this.dialogRef.close(node);
                },
                (error) => {
                    this.toast.closeProgressSpinner();
                    this.toast.error(error);
                },
            );
    }
}
