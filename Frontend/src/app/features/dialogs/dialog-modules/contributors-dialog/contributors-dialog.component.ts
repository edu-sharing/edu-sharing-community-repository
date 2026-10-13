import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Node, NodeService } from 'ngx-edu-sharing-api';
import { LocalEventsService } from 'ngx-edu-sharing-ui';
import { firstValueFrom } from 'rxjs';
import { DialogButton, RestConstants } from '../../../../core-module/core.module';
import { Toast } from '../../../../services/toast';
import { CARD_DIALOG_DATA, CardDialogConfig, Closable } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { CardDialogUtilsService } from '../../card-dialog/card-dialog-utils.service';
import { DialogsService } from '../../dialogs.service';
import {
    ContributorEditDialogData,
    ContributorEditDialogResult,
} from '../contributor-edit-dialog/contributor-edit-dialog-data';
import { YES_OR_NO } from '../generic-dialog/generic-dialog-data';
import {
    applyEdit,
    buildEntries,
    buildNodeDiff,
    ContributorBucket,
    ContributorEntry,
    removeEntry,
    upsertEntry,
} from './contributor-bulk-model';
import { ContributorsDialogData, ContributorsDialogResult } from './contributors-dialog-data';

/**
 * Edits the contributors of one or many nodes.
 *
 * The dialog always works on the bulk model, even for a single node - with one node there simply
 * are no partial (`some`) states, so there is one code path instead of two. Saving writes only the
 * properties that actually changed, which keeps untouched nodes free of empty new versions.
 */
@Component({
    selector: 'es-contributors-dialog',
    templateUrl: './contributors-dialog.component.html',
    styleUrls: ['./contributors-dialog.component.scss'],
    standalone: false,
})
export class ContributorsDialogComponent implements OnInit {
    data = inject<ContributorsDialogData>(CARD_DIALOG_DATA);
    private dialogRef =
        inject<CardDialogRef<ContributorsDialogData, ContributorsDialogResult>>(CardDialogRef);
    private cardDialogUtils = inject(CardDialogUtilsService);
    private dialogs = inject(DialogsService);
    private localEvents = inject(LocalEventsService);
    private nodeService = inject(NodeService);
    private toast = inject(Toast);

    readonly entries = signal<ContributorEntry[]>([]);
    readonly nodes = signal<Node[]>([]);
    readonly nodeCount = computed(() => this.nodes().length);
    readonly isBulk = computed(() => this.nodeCount() > 1);

    ngOnInit(): void {
        this.initButtons();
        void this.initData();
    }

    private initButtons() {
        this.dialogRef.patchConfig({
            buttons: [
                new DialogButton('CANCEL', { color: 'standard' }, () => this.dialogRef.close(null)),
                new DialogButton('APPLY', { color: 'primary' }, () => void this.save()),
            ],
        });
    }

    private async initData() {
        this.dialogRef.patchState({ isLoading: true });
        const nodes = await Promise.all(
            this.data.nodes.map((node) =>
                typeof node === 'string' || !this.hasContributorProperties(node)
                    ? firstValueFrom(
                          this.nodeService.getNode(typeof node === 'string' ? node : node.ref.id),
                      )
                    : Promise.resolve(node),
            ),
        );
        this.nodes.set(nodes);
        this.entries.set(buildEntries(nodes));
        this.dialogRef.patchConfig(
            (await this.cardDialogUtils.configForNodes(nodes)) as Partial<
                CardDialogConfig<ContributorsDialogData>
            >,
        );
        this.dialogRef.patchState({ isLoading: false });
    }

    /**
     * Nodes coming from a list may be loaded with a reduced property filter. Only a node that
     * already carries contributor properties can be used without reloading it.
     */
    private hasContributorProperties(node: Node): boolean {
        return RestConstants.getAllVCardFields().some(
            (property) => node.properties?.[property] !== undefined,
        );
    }

    async onRemove(entry: ContributorEntry) {
        const confirmDialogRef = await this.dialogs.openGenericDialog({
            title: 'WORKSPACE.CONTRIBUTOR.DELETE_TITLE',
            message: this.isBulk()
                ? 'WORKSPACE.CONTRIBUTOR.BULK.DELETE_MESSAGE'
                : 'WORKSPACE.CONTRIBUTOR.DELETE_MESSAGE',
            messageParameters: {
                name: entry.vCard.getDisplayName(),
                total: String(this.nodeCount()),
            },
            buttons: YES_OR_NO,
        });
        confirmDialogRef.afterClosed().subscribe((response) => {
            if (response === 'YES') {
                removeEntry(this.entries(), entry);
                this.markChanged();
            }
        });
    }

    onAdd(bucket: ContributorBucket) {
        void this.openEditDialog({ editMode: bucket }, null);
    }

    onEdit(entry: ContributorEntry) {
        void this.openEditDialog(
            { vCard: entry.vCard.copy(), editMode: entry.bucket, role: entry.role },
            entry,
        );
    }

    private async openEditDialog(data: ContributorEditDialogData, entry: ContributorEntry | null) {
        const dialogRef = await this.dialogs.openContributorEditDialog(data);
        dialogRef.afterClosed().subscribe((result: ContributorEditDialogResult) => {
            if (!result) {
                return;
            }
            const entries = this.entries();
            if (entry) {
                applyEdit(entries, entry, result.vCard, result.role);
            } else {
                upsertEntry(entries, result.vCard, data.editMode, result.role);
            }
            this.markChanged();
        });
    }

    private markChanged() {
        this.dialogRef.patchConfig({ closable: Closable.Confirm });
        // the entries are mutated in place by the model helpers; re-emit so the view updates
        this.entries.set([...this.entries()]);
    }

    private async save() {
        const changes = buildNodeDiff(this.nodes(), this.entries());
        if (!changes.length) {
            this.dialogRef.close([]);
            return;
        }
        this.dialogRef.patchState({ isLoading: true });
        const updatedNodes: Node[] = [];
        const failed: { node: Node; error: any }[] = [];
        for (const { node, values } of changes) {
            try {
                updatedNodes.push(
                    await firstValueFrom(
                        this.nodeService.editNodeMetadata(node.ref.id, values, {
                            versionComment: RestConstants.COMMENT_CONTRIBUTOR_UPDATE,
                        }),
                    ),
                );
            } catch (error) {
                failed.push({ node, error });
            }
        }
        if (updatedNodes.length) {
            this.localEvents.nodesChanged.emit(updatedNodes);
        }
        if (failed.length) {
            // Keep the dialog open so the user does not lose the state of the other entries.
            this.dialogRef.patchState({ isLoading: false });
            this.toast.error(failed[0].error, 'WORKSPACE.CONTRIBUTOR.BULK.PARTIAL_ERROR', {
                count: updatedNodes.length,
                total: changes.length,
            });
            return;
        }
        this.dialogRef.close(updatedNodes);
        this.toast.toast(
            this.isBulk()
                ? 'WORKSPACE.TOAST.CONTRIBUTOR_UPDATED_MULTIPLE'
                : 'WORKSPACE.TOAST.CONTRIBUTOR_UPDATED',
            { count: updatedNodes.length },
        );
    }
}
