import { Component, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { Node } from '../../../core-module/core.module';
import {
    CanDrop,
    DragData,
    DropSource,
    NodeRoot,
    OptionItem,
    OptionsHelperDataService,
    Scope,
} from 'ngx-edu-sharing-ui';
import { WorkspacePageComponent } from '../workspace-page.component';
import { WorkspaceSubTreeComponent } from '../sub-tree/sub-tree.component';
import { NodeEntriesService } from 'ngx-edu-sharing-ui/services/node-entries.service';
import { HOME_REPOSITORY, RestConstants } from 'ngx-edu-sharing-api';
import { TranslateService } from '@ngx-translate/core';
import { DialogsService } from '../../../features/dialogs/dialogs.service';

@Component({
    selector: 'es-workspace-tree',
    templateUrl: 'tree.component.html',
    styleUrls: ['tree.component.scss'],
    providers: [NodeEntriesService, OptionsHelperDataService],
})
export class WorkspaceTreeComponent {
    @Input() root: NodeRoot;
    @Input() workspace: WorkspacePageComponent;
    @Input() isSafe: boolean;
    @Input() selectedNode: string;
    @Input() set path(path: Node[]) {
        const pathIds = path.map((node) => node.ref.id);
        this.currentPath = pathIds;
    }
    @Input() options: OptionItem[] = [];

    @Output() onOpenNode = new EventEmitter();
    @Output() onUpdateOptions = new EventEmitter();
    @Output() onSetRoot = new EventEmitter();
    @Output() onDrop = new EventEmitter<{ target: Node; source: DropSource<Node> }>();
    @Output() onDeleteNodes = new EventEmitter();

    @ViewChild(WorkspaceSubTreeComponent) subTree: WorkspaceSubTreeComponent;

    readonly MY_FILES = 'MY_FILES';
    readonly SHARED_FILES = 'SHARED_FILES';
    readonly MY_SHARED_FILES = 'MY_SHARED_FILES';
    readonly TO_ME_SHARED_FILES = 'TO_ME_SHARED_FILES';
    readonly WORKFLOW_RECEIVE = 'WORKFLOW_RECEIVE';
    readonly RECYCLE: 'RECYCLE' = 'RECYCLE';

    currentPath: string[] = [];

    constructor(
        private optionsHelperDataService: OptionsHelperDataService,
        private dialogsService: DialogsService,
        private translate: TranslateService,
    ) {
        this.optionsHelperDataService.setData({
            scope: Scope.WorkspaceTree,
        });
        this.optionsHelperDataService.initComponents();
    }

    canDropOnRecycle = (dragData: DragData<'RECYCLE'>): CanDrop => {
        return { accept: dragData.action === 'move' };
    };
    canDropOnMyFiles = (dragData: DragData<'MY_FILES'>): CanDrop => {
        return { accept: true };
    };
    canDropOnMySharedFiles = (dragData: DragData<'RECYCLE'>): CanDrop => {
        return { accept: dragData.action === 'move' };
    };
    setRoot(root: string) {
        this.onSetRoot.emit(root);
        this.currentPath = [];
    }

    onNodesDrop(dragData: DragData<string | 'MY_FILES' | 'MY_SHARED_FILES' | 'RECYCLE'>) {
        if (dragData.target === this.RECYCLE && dragData.action === 'move') {
            this.onDeleteNodes.emit(dragData.draggedNodes);
        } else if (dragData.target === this.MY_FILES) {
            this.onDrop.emit({
                target: {
                    ref: { id: RestConstants.USERHOME, repo: HOME_REPOSITORY },
                    name: this.translate.instant('WORKSPACE.MY_FILES'),
                } as Node,
                source: {
                    element: dragData.draggedNodes,
                    mode: dragData.action,
                },
            });
        } else if (dragData.target === this.MY_SHARED_FILES) {
            void this.dialogsService.openShareDialog({
                nodes: dragData.draggedNodes,
            });
        }
    }

    drop(event: any) {
        this.onDrop.emit(event);
    }

    updateOptions(event: Node) {
        this.onUpdateOptions.emit(event);
    }

    openNode(event: Node) {
        this.onOpenNode.emit(event);
    }

    public refresh() {
        this.subTree?.refresh();
    }
}
