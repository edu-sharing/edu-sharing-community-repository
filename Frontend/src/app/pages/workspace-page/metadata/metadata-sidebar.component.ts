import { trigger } from '@angular/animations';
import { Component, input, inject } from '@angular/core';
import { Router } from '@angular/router';
import { LocalEventsService, UIAnimation, UIConstants } from 'ngx-edu-sharing-ui';
import {
    NodeVersions,
    RestConstants,
    RestNodeService,
    Version,
} from '../../../core-module/core.module';
import { Node } from 'ngx-edu-sharing-api';
import { Toast } from '../../../services/toast';
import { YES_OR_NO } from '../../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { BulkBehavior } from '../../../features/mds/types/types';
import { WorkspaceMetadataComponent } from './metadata.component';

/**
 * Container Component for the workspace's metadata sidebar.
 *
 * Handles input and output of the inner metadata component and shows / hides the sidebar as
 * requested.
 */
@Component({
    selector: 'es-metadata-sidebar',
    templateUrl: './metadata-sidebar.component.html',
    styleUrls: ['./metadata-sidebar.component.scss'],
    animations: [trigger('fromRight', UIAnimation.fromRight())],
    imports: [WorkspaceMetadataComponent],
})
export class MetadataSidebarComponent {
    private dialogs = inject(DialogsService);
    private localEvents = inject(LocalEventsService);
    private node = inject(RestNodeService);
    private router = inject(Router);
    private toast = inject(Toast);

    nodeSidebar = input.required<Node>();

    async restoreVersion(restore: { version: Version; node: Node }) {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'WORKSPACE.METADATA.RESTORE_TITLE',
            message: 'WORKSPACE.METADATA.RESTORE_MESSAGE',
            buttons: YES_OR_NO,
            nodes: [restore.node],
        });
        dialogRef.afterClosed().subscribe((response) => {
            if (response === 'YES') {
                this.doRestoreVersion(restore.version);
            }
        });
    }

    private doRestoreVersion(version: Version): void {
        this.toast.showProgressSpinner();
        this.node
            .revertNodeToVersion(
                version.version.node.id,
                version.version.major,
                version.version.minor,
            )
            .subscribe(
                () => {
                    this.toast.closeProgressSpinner();
                    // @TODO type is not compatible
                    this.node
                        .getNodeMetadata(version.version.node.id, [RestConstants.ALL])
                        .subscribe(
                            (node) => {
                                this.localEvents.nodesChanged.emit([node.node]);
                                this.toast.toast('WORKSPACE.REVERTED_VERSION');
                            },
                            (error: any) => this.toast.error(error),
                        );
                },
                (error: any) => this.toast.error(error),
            );
    }

    goToNode(node: Node) {
        if (node.content.version) {
            void this.router.navigate([
                UIConstants.ROUTER_PREFIX + 'render',
                node.ref.id,
                node.content.version,
            ]);
        } else {
            void this.router.navigate([UIConstants.ROUTER_PREFIX + 'render', node.ref.id]);
        }
    }

    async openMdsEditor(nodes: Node[]): Promise<void> {
        const dialogRef = await this.dialogs.openMdsEditorDialogForNodes({
            nodes,
            bulkBehavior: BulkBehavior.Default,
        });
        dialogRef
            .afterClosed()
            .subscribe((updatedNodes) => this.closeMdsEditor(nodes, updatedNodes as Node[]));
    }

    private closeMdsEditor(originalNodes: Node[], updatedNodes: Node[] = null) {
        let refresh = !!updatedNodes;
        if (refresh) {
            this.localEvents.nodesChanged.emit(updatedNodes);
        }
    }
}
