import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DialogButton, RestConnectorService, RestHelper } from '../../../core-module/core.module';
import { RestNodeService } from '../../../core-module/core.module';
import { Node } from '../../../core-module/core.module';
import { trigger } from '@angular/animations';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import { RestConstants } from '../../../core-module/core.module';
import { Toast } from '../../../core-ui-module/toast';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { Router } from '@angular/router';
import { DialogsService } from '../../../features/dialogs/dialogs.service';

@Component({
    selector: 'es-map-link',
    templateUrl: 'map-link.component.html',
    styleUrls: ['map-link.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
})
export class MapLinkComponent {
    _node: Node;
    breadcrumbs: Node[];
    buttons: DialogButton[];
    @Input() set node(node: Node) {
        this._node = node;
        this.name = node.name;
        this.updateButtons();
    }
    @Output() onCancel = new EventEmitter();
    @Output() onDone = new EventEmitter();
    name: string;
    constructor(
        private connector: RestConnectorService,
        private toast: Toast,
        private router: Router,
        private nodeApi: RestNodeService,
        private dialogs: DialogsService,
    ) {
        this.updateBreadcrumbs(RestConstants.INBOX);
        this.updateButtons();
    }
    public cancel() {
        this.onCancel.emit();
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
            this.breadcrumbs = parents.nodes.reverse();
        });
    }

    updateButtons(): any {
        this.buttons = [
            new DialogButton('CANCEL', { color: 'standard' }, () => this.cancel()),
            new DialogButton('MAP_LINK.CREATE', { color: 'primary' }, () => this.createLink()),
        ];
        this.buttons[1].disabled = !this.name;
    }

    private createLink() {
        const properties = RestHelper.createNameProperty(this.name);
        properties[RestConstants.CCM_PROP_MAP_REF_TARGET] = [
            RestHelper.createSpacesStoreRef(this._node),
        ];
        this.toast.showProgressDialog();
        this.nodeApi
            .createNode(
                this.breadcrumbs[this.breadcrumbs.length - 1].ref.id,
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
                                    this.nodeApi,
                                    this.router,
                                    this.connector.getCurrentLogin(),
                                    this.breadcrumbs[this.breadcrumbs.length - 1].ref.id,
                                );
                            },
                        },
                    };
                    this.toast.toast(
                        'MAP_LINK.CREATED',
                        { folder: this.breadcrumbs[this.breadcrumbs.length - 1].name },
                        null,
                        null,
                        additional,
                    );
                    this.toast.closeModalDialog();
                    this.onDone.emit(node);
                },
                (error) => {
                    this.toast.closeModalDialog();
                    this.toast.error(error);
                },
            );
    }
}
