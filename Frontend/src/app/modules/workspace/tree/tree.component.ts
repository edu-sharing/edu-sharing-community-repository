import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DropSource, NodeRoot } from 'src/app/features/node-entries/entries-model';
import { Node } from '../../../core-module/core.module';
import { Helper } from '../../../core-module/rest/helper';
import { OptionItem } from '../../../core-ui-module/option-item';
import { DragData } from '../../../services/nodes-drag-drop.service';
import { CanDrop } from '../../../shared/directives/nodes-drop-target.directive';
import { WorkspaceMainComponent } from '../workspace.component';

@Component({
    selector: 'es-workspace-tree',
    templateUrl: 'tree.component.html',
    styleUrls: ['tree.component.scss'],
})
export class WorkspaceTreeComponent {
    @Input() root: NodeRoot;
    @Input() workspace: WorkspaceMainComponent;
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

    readonly MY_FILES = 'MY_FILES';
    readonly SHARED_FILES = 'SHARED_FILES';
    readonly MY_SHARED_FILES = 'MY_SHARED_FILES';
    readonly TO_ME_SHARED_FILES = 'TO_ME_SHARED_FILES';
    readonly WORKFLOW_RECEIVE = 'WORKFLOW_RECEIVE';
    readonly RECYCLE: 'RECYCLE' = 'RECYCLE';

    reload: Boolean;

    currentPath: string[] = [];

    constructor() {}

    canDropOnRecycle = (dragData: DragData<'RECYCLE'>): CanDrop => {
        return { accept: dragData.action === 'move' };
    };

    setRoot(root: string) {
        this.onSetRoot.emit(root);
        this.currentPath = [];
    }

    onNodesDrop(dragData: DragData<'RECYCLE'>) {
        if (dragData.target === this.RECYCLE && dragData.action === 'move') {
            this.onDeleteNodes.emit(dragData.draggedNodes);
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
}
