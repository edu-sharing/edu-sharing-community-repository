import { Component, Inject, OnInit } from '@angular/core';
import { map } from 'rxjs/operators';
import {
    DialogButton,
    Node,
    RestConstants,
    RestNodeService,
} from '../../../../core-module/core.module';
import { VCard } from 'ngx-edu-sharing-ui';
import { Toast } from '../../../../core-ui-module/toast';
import {
    CARD_DIALOG_DATA,
    CardDialogConfig,
    Closable,
    configForNode,
} from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { DialogsService } from '../../dialogs.service';
import {
    ContributorEditDialogData,
    ContributorEditDialogResult,
    EditMode,
} from '../contributor-edit-dialog/contributor-edit-dialog-data';
import { YES_OR_NO } from '../generic-dialog/generic-dialog-data';
import { ContributorsDialogData, ContributorsDialogResult } from './contributors-dialog-data';

@Component({
    selector: 'es-contributors-dialog',
    templateUrl: './contributors-dialog.component.html',
    styleUrls: ['./contributors-dialog.component.scss'],
})
export class ContributorsDialogComponent implements OnInit {
    readonly rolesLifecycle = RestConstants.CONTRIBUTOR_ROLES_LIFECYCLE;
    readonly rolesMetadata = RestConstants.CONTRIBUTOR_ROLES_METADATA;
    contributorLifecycle: { [role: string]: VCard[] } = {};
    contributorMetadata: { [role: string]: VCard[] } = {};

    node: Node;

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: ContributorsDialogData,
        private dialogRef: CardDialogRef<ContributorsDialogData, ContributorsDialogResult>,
        private nodeService: RestNodeService,
        private toast: Toast,
        private dialogs: DialogsService,
    ) {}

    ngOnInit(): void {
        this.initButtons();
        void this.initData();
    }

    private initButtons() {
        this.dialogRef.patchConfig({
            buttons: [
                new DialogButton('CANCEL', { color: 'standard' }, () => this.dialogRef.close(null)),
                new DialogButton('APPLY', { color: 'primary' }, () => this.saveContributor()),
            ],
        });
    }

    private async initData() {
        if (typeof this.data.node === 'string') {
            this.dialogRef.patchState({ isLoading: true });
            this.node = await this.nodeService
                .getNodeMetadata(this.data.node, [RestConstants.ALL])
                .pipe(map(({ node }) => node))
                .toPromise();
        } else {
            this.node = this.data.node;
        }
        this.dialogRef.patchConfig(
            configForNode(this.node) as Partial<CardDialogConfig<ContributorsDialogData>>,
        );
        for (let role of this.rolesLifecycle) {
            this.contributorLifecycle[role] = [];
            let list = this.node.properties[RestConstants.CONTRIBUTOR_LIFECYCLE_PREFIX + role];
            if (!list) {
                continue;
            }
            for (let vCard of list) {
                if (vCard && new VCard(vCard).isValid()) {
                    this.contributorLifecycle[role].push(new VCard(vCard));
                }
            }
        }
        for (let role of this.rolesMetadata) {
            this.contributorMetadata[role] = [];
            let list = this.node.properties[RestConstants.CONTRIBUTOR_METADATA_PREFIX + role];
            if (!list) {
                continue;
            }
            for (let vCard of list) {
                if (vCard && new VCard(vCard).isValid()) {
                    this.contributorMetadata[role].push(new VCard(vCard));
                }
            }
        }
        this.dialogRef.patchState({ isLoading: false });
    }

    async remove(data: any[], pos: number) {
        const confirmDialogRef = await this.dialogs.openGenericDialog({
            title: 'WORKSPACE.CONTRIBUTOR.DELETE_TITLE',
            messageText: 'WORKSPACE.CONTRIBUTOR.DELETE_MESSAGE',
            messageParameters: { name: data[pos].getDisplayName() },
            buttons: YES_OR_NO,
        });
        confirmDialogRef.afterClosed().subscribe((response) => {
            if (response === 'YES') {
                data.splice(pos, 1);
                this.dialogRef.patchConfig({ closable: Closable.Confirm });
            }
        });
    }

    addVCard(mode: EditMode) {
        void this.openEditDialog({ editMode: mode });
    }

    async editVCard(mode: EditMode, vcard: VCard, role: string) {
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
                this.saveEdits(data, result);
            }
        });
    }

    private saveEdits(
        originalData: ContributorEditDialogData,
        result: ContributorEditDialogResult,
    ) {
        this.dialogRef.patchConfig({ closable: Closable.Confirm });
        let array =
            originalData.editMode == 'lifecycle'
                ? this.contributorLifecycle
                : this.contributorMetadata;
        if (originalData.role) {
            let pos = array[originalData.role].indexOf(originalData.vCard);
            array[originalData.role].splice(pos, 1);
            if (originalData.role === result.role) {
                array[originalData.role].splice(pos, 0, result.vCard);
            } else {
                array[result.role].push(result.vCard);
            }
        } else {
            array[result.role].push(result.vCard);
        }
    }

    private saveContributor() {
        this.dialogRef.patchState({ isLoading: true });
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
                this.node.ref.id,
                RestConstants.COMMENT_CONTRIBUTOR_UPDATE,
                properties,
            )
            .subscribe(
                ({ node }) => {
                    this.dialogRef.close(node);
                    this.toast.toast('WORKSPACE.TOAST.CONTRIBUTOR_UPDATED');
                },
                (error: any) => {
                    this.toast.error(error);
                    this.dialogRef.patchState({ isLoading: false });
                },
            );
    }
}
