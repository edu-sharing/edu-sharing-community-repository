import { trigger } from '@angular/animations';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormControl } from '@angular/forms';
import {
    DialogButton,
    Node,
    NodeWrapper,
    RestConstants,
    RestIamService,
    RestNodeService,
} from '../../../core-module/core.module';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import { VCard } from '../../../core-module/ui/VCard';
import { Toast } from '../../../core-ui-module/toast';
import {
    ContributorEditDialogData,
    ContributorEditDialogResult,
    EditMode,
} from '../../../features/dialogs/dialog-modules/contributor-edit-dialog/contributor-edit-dialog-data';
import { DialogsService } from '../../../features/dialogs/dialogs.service';

@Component({
    selector: 'es-workspace-contributor',
    templateUrl: 'contributor.component.html',
    styleUrls: ['contributor.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
})
export class WorkspaceContributorComponent {
    public contributorLifecycle: any = {};
    public contributorMetadata: any = {};
    public rolesLifecycle = RestConstants.CONTRIBUTOR_ROLES_LIFECYCLE;
    public rolesMetadata = RestConstants.CONTRIBUTOR_ROLES_METADATA;

    public loading = true;
    private editRoleOld: string;
    editOriginal: VCard;
    _node: Node;
    @Input() set node(node: Node) {
        this._node = node;
        for (let role of this.rolesLifecycle) {
            this.contributorLifecycle[role] = [];
            let list = node.properties[RestConstants.CONTRIBUTOR_LIFECYCLE_PREFIX + role];
            if (!list) continue;
            for (let vcard of list) {
                if (vcard && new VCard(vcard).isValid())
                    this.contributorLifecycle[role].push(new VCard(vcard));
            }
        }
        for (let role of this.rolesMetadata) {
            this.contributorMetadata[role] = [];
            let list = node.properties[RestConstants.CONTRIBUTOR_METADATA_PREFIX + role];
            if (!list) continue;
            for (let vcard of list) {
                if (vcard && new VCard(vcard).isValid())
                    this.contributorMetadata[role].push(new VCard(vcard));
            }
        }
        this.loading = false;
    }
    buttons: DialogButton[];

    @Input() set nodeId(nodeId: string) {
        this.loading = true;
        this.nodeService
            .getNodeMetadata(nodeId, [RestConstants.ALL])
            .subscribe((data: NodeWrapper) => {
                this.node = data.node;
            });
    }
    @Output() onClose = new EventEmitter<Node>();
    @Output() onLoading = new EventEmitter();
    givenname = new FormControl('');
    public remove(data: any[], pos: number) {
        this.toast.showConfigurableDialog({
            title: 'WORKSPACE.CONTRIBUTOR.DELETE_TITLE',
            message: 'WORKSPACE.CONTRIBUTOR.DELETE_MESSAGE',
            messageParameters: { name: data[pos].getDisplayName() },
            isCancelable: true,
            buttons: DialogButton.getYesNo(
                () => {
                    this.toast.closeModalDialog();
                },
                () => {
                    data.splice(pos, 1);
                    this.toast.closeModalDialog();
                },
            ),
        });
    }

    public addVCard(mode: EditMode) {
        this.editOriginal = null;
        this.editRoleOld = null;
        void this.openEditDialog({ editMode: mode });
    }

    async editVCard(mode: EditMode, vcard: VCard, role: string) {
        this.editOriginal = vcard;
        this.editRoleOld = role;
        void this.openEditDialog({
            vCard: vcard.copy(),
            editMode: mode,
            role,
        });
    }

    private async openEditDialog(data: ContributorEditDialogData) {
        const dialogRef = await this.dialogs.openContributorEditDialog(data);
        dialogRef.afterClosed().subscribe((result) => {
            if (result) {
                this.saveEdits({ ...result, editMode: data.editMode });
            }
        });
    }

    private saveEdits(data: ContributorEditDialogResult & { editMode: EditMode }) {
        let array =
            data.editMode == 'lifecycle' ? this.contributorLifecycle : this.contributorMetadata;
        if (this.editRoleOld) {
            let pos = array[this.editRoleOld].indexOf(this.editOriginal);
            array[this.editRoleOld].splice(pos, 1);
            if (this.editRoleOld === data.role) {
                array[this.editRoleOld].splice(pos, 0, data.vCard);
            } else {
                array[data.role].push(data.vCard);
            }
        } else {
            array[data.role].push(data.vCard);
        }
    }

    public saveContributor() {
        this.onLoading.emit(true);
        let properties: any = {};
        for (let role of this.rolesLifecycle) {
            let prop = [];
            for (let vcard of this.contributorLifecycle[role]) {
                prop.push(vcard.toVCardString());
            }
            properties['ccm:lifecyclecontributer_' + role] = prop;
        }
        for (let role of this.rolesMetadata) {
            let prop = [];
            for (let vcard of this.contributorMetadata[role]) {
                prop.push(vcard.toVCardString());
            }
            properties['ccm:metadatacontributer_' + role] = prop;
        }
        this.nodeService
            .editNodeMetadataNewVersion(
                this._node.ref.id,
                RestConstants.COMMENT_CONTRIBUTOR_UPDATE,
                properties,
            )
            .subscribe(
                ({ node }) => {
                    this.toast.toast('WORKSPACE.TOAST.CONTRIBUTOR_UPDATED');
                    this.onClose.emit(node);
                    this.onLoading.emit(false);
                },
                (error: any) => {
                    this.toast.error(error);
                    this.onLoading.emit(false);
                },
            );
    }
    public cancel() {
        this.onClose.emit();
    }
    public constructor(
        private nodeService: RestNodeService,
        private iamService: RestIamService,
        private toast: Toast,
        private dialogs: DialogsService,
    ) {
        this.buttons = [
            new DialogButton('CANCEL', { color: 'standard' }, () => this.cancel()),
            new DialogButton('APPLY', { color: 'primary' }, () => this.saveContributor()),
        ];
    }

    updatePersonSuggestions() {}
}
