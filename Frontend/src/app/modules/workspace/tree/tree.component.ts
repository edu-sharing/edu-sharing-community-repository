import { Component, EventEmitter, Input, Output } from '@angular/core';
import {
    Node,
    RestConnectorService,
    RestNodeService,
    TemporaryStorageService,
} from '../../../core-module/core.module';
import { Helper } from '../../../core-module/rest/helper';
import { OptionItem } from '../../../core-ui-module/option-item';
import { DropData, DragData } from '../../../core-ui-module/directives/drag-nodes/drag-nodes';
import {WorkspaceMainComponent} from '../workspace.component';
import { DropSource, NodeRoot } from 'src/app/features/node-entries/entries-model';

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
        if (path.length === 0) {
            this.reload = new Boolean(true);
            return;
        }
        this._path[0] = [];

        for (const node of path) {
            if (node && node.ref) this._path[0].push(node.ref.id);
        }
        this._selectedPath = this._path[0];
    }
    @Input() set current(current: string) {
        // TODO: Using this fixes bug for AddDirectory, but constantly refreshes
        //this.homeDirectory=new String(this.homeDirectory);
        if (!current) return;
        this._current = current;
    }
    @Input() options: OptionItem[] = [];

    @Output() onOpenNode = new EventEmitter();
    @Output() onUpdateOptions = new EventEmitter();
    @Output() onSetRoot = new EventEmitter();
    @Output() onDrop = new EventEmitter<{target: Node, source: DropSource<Node>}>();
    @Output() onDeleteNodes = new EventEmitter();

    readonly MY_FILES = 'MY_FILES';
    readonly SHARED_FILES = 'SHARED_FILES';
    readonly MY_SHARED_FILES = 'MY_SHARED_FILES';
    readonly TO_ME_SHARED_FILES = 'TO_ME_SHARED_FILES';
    readonly WORKFLOW_RECEIVE = 'WORKFLOW_RECEIVE';
    readonly RECYCLE = 'RECYCLE';

    reload: Boolean;
    dragHover: string;

    _path: string[][] = [];
    // just for highlighting, does not open nodes!
    _selectedPath: string[] = [];
    private _current: string;

    constructor(
        private node: RestNodeService,
        private connector: RestConnectorService,
        private storage: TemporaryStorageService,
    ) {}

    setRoot(root: string) {
        this.onSetRoot.emit(root);
        this._path = [];
        /*
    if(root==this.MY_FILES) {
      this.onOpenPath.emit([this.homeDirectory]) ;
    }
    */
    }

    toggleTree(event: any) {
        let id = event.node.ref.id;
        let create = true;
        for (let i = 0; i < this._path.length; i++) {
            let pos = this._path[i].indexOf(id);
            if (pos != -1) {
                //this._path[i].splice(pos,this._path[i].length-pos);
                this._path[i].splice(pos, 1);
                create = false;
                i--;
            }
            /*
            if(event.parent.length){
                let pos=this._path[i].indexOf(event.parent[event.parent.length-1]);
            }
            */
        }
        if (create) {
            let path = Helper.deepCopy(event.parent);
            path.push(id);
            this._path.push(path);
        }
    }

    onNodesHoveringChange(nodesHovering: boolean, target: string) {
        if (nodesHovering) {
            this.dragHover = target;
        } else {
            // The enter event of another node might have fired before this leave
            // event and already updated `dragHover`. Only set it to null if that is
            // not the case.
            if (this.dragHover === target) {
                this.dragHover = null;
            }
        }
    }

    onNodesDrop({ nodes }: DragData, target: string) {
        if (target == this.RECYCLE) {
            this.onDeleteNodes.emit(nodes);
        }
    }

    drop(event: any) {
        this.onDrop.emit(event);
    }

    updateOptions(event: Node) {
        this.onUpdateOptions.emit(event);
    }

    openNode(event: Node) {
        this._path.splice(1, this._path.length - 1);
        this.onOpenNode.emit(event);
    }
}
