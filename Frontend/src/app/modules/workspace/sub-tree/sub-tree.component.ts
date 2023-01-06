import { trigger } from '@angular/animations';
import {
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild,
} from '@angular/core';
import { MatMenuTrigger } from '@angular/material/menu';
import * as rxjs from 'rxjs';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { DropSource } from 'src/app/features/node-entries/entries-model';
import { Node, NodeList, RestConstants, RestNodeService } from '../../../core-module/core.module';
import { Helper } from '../../../core-module/rest/helper';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import { OptionItem, Scope } from '../../../core-ui-module/option-item';
import { OptionsHelperService } from '../../../core-ui-module/options-helper.service';
import { DragData } from '../../../services/nodes-drag-drop.service';
import { DropdownComponent } from '../../../shared/components/dropdown/dropdown.component';
import { canDropOnNode } from '../workspace-utils';

@Component({
    selector: 'es-workspace-sub-tree',
    templateUrl: 'sub-tree.component.html',
    styleUrls: ['sub-tree.component.scss'],
    animations: [
        trigger('openOverlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST)),
        trigger('open', UIAnimation.openOverlay()),
    ],
    providers: [OptionsHelperService],
})
export class WorkspaceSubTreeComponent implements OnInit, OnDestroy {
    private static MAX_FOLDER_COUNT = 100;

    @ViewChild('dropdown') dropdown: DropdownComponent;
    @ViewChild('dropdownTrigger') dropdownTrigger: MatMenuTrigger;
    dropdownLeft: string;
    dropdownTop: string;

    @Input() set reload(reload: Boolean) {
        if (reload) {
            this.refresh();
        }
    }
    private _currentPath: string[] = [];
    /** Parent hierarchy of the currently selected node. */
    @Input()
    get currentPath(): string[] {
        return this._currentPath;
    }
    set currentPath(value: string[]) {
        this._currentPath = value;
        this.expandCurrentPath();
    }
    @Input() depth = 0;
    /** The node rendered by this sub tree. */
    @Input() set node(node: string) {
        this._node = node;
        if (node == null) {
            return;
        }
        this.refresh();
    }

    @Output() onClick = new EventEmitter<Node>();
    @Output() onLoading = new EventEmitter();
    @Output() onDrop = new EventEmitter<{ target: Node; source: DropSource<Node> }>();
    @Output() hasChildren = new EventEmitter<boolean>();
    @Output() onUpdateOptions = new EventEmitter();

    _node: string;
    loading = true;
    _nodes: Node[];
    _hasChildren: { [nodeId: string]: boolean } = {};
    moreItems: number;
    loadingMore: boolean;
    loadingStates: boolean[] = [];

    /** IDs of child nodes of the node rendered by this sub tree, that should be expanded. */
    private expandedNodes: string[] = [];
    private destroyed = new Subject<void>();

    constructor(private nodeApi: RestNodeService, private optionsService: OptionsHelperService) {}

    ngOnInit(): void {
        rxjs.merge(this.optionsService.nodesChanged, this.optionsService.nodesDeleted)
            .pipe(takeUntil(this.destroyed))
            .subscribe(() => this.refresh());
    }

    /**
     * Resets expanded nodes to the parent hierarchy of the currently selected node.
     */
    private expandCurrentPath() {
        const currentChildNode = this._nodes?.find(
            (node) => node.ref.id === this.currentPath[this.depth],
        );
        if (currentChildNode) {
            this.expandedNodes = [currentChildNode.ref.id];
        }
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    setLoadingState(state: boolean, pos: number) {
        this.loadingStates[pos] = state;
    }

    optionIsShown(optionItem: OptionItem, node: Node) {
        if (optionItem.showCallback) {
            return optionItem.showCallback(node);
        }
        return true;
    }

    loadAll() {
        this.loadingMore = true;
        this.nodeApi
            .getChildren(this._node, [RestConstants.FILTER_FOLDERS], {
                offset: this._nodes.length,
                count: RestConstants.COUNT_UNLIMITED,
            })
            .subscribe((data: NodeList) => {
                this.loadingMore = false;
                this._nodes = this._nodes.concat(data.nodes);
                this.moreItems = 0;
            });
    }

    contextMenu(event: any, node: Node) {
        event.preventDefault();
        event.stopPropagation();

        this.showDropdown(event, node);
    }

    updateOptions(event: Node) {
        this.onUpdateOptions.emit(event);
    }

    private showDropdown(event: any, node: Node) {
        //if(this._options==null || this._options.length<1)
        //  return;
        this.dropdownLeft = event.clientX + 'px';
        this.dropdownTop = event.clientY + 'px';
        this.optionsService.setData({
            activeObjects: [node],
            scope: Scope.WorkspaceTree,
        });
        this.optionsService.initComponents(null, null, this.dropdown);
        this.optionsService.refreshComponents();
        this.dropdownTrigger.openMenu();
    }

    dropToParent(event: any) {
        this.onDrop.emit(event);
    }

    isSelected(node: Node): boolean {
        return this.currentPath[this.currentPath.length - 1] === node.ref.id;
    }

    openPathEvent(node: Node): void {
        this.onClick.emit(node);
    }

    isOpen(node: Node): boolean {
        return this.expandedNodes.includes(node.ref.id);
    }

    openOrCloseNode(node: Node): void {
        this.onClick.emit(node);
    }

    toggleNodeExpansion(event: MouseEvent, node: Node): void {
        if (this._hasChildren[node.ref.id] === false) {
            return;
        }
        event.stopPropagation();
        const index = this.expandedNodes.indexOf(node.ref.id);
        if (index < 0) {
            this.expandedNodes.push(node.ref.id);
        } else {
            this.expandedNodes.splice(index, 1);
        }
    }

    private refresh() {
        if (!this._node) return;
        this.nodeApi
            .getChildren(this._node, [RestConstants.FILTER_FOLDERS], {
                count: WorkspaceSubTreeComponent.MAX_FOLDER_COUNT,
            })
            .subscribe((data: NodeList) => {
                this._nodes = data.nodes;
                this.moreItems = data.pagination.total - data.pagination.count;
                this.loadingStates = Helper.initArray(this._nodes.length, true);
                this.hasChildren.emit(this._nodes?.length > 0);
                this.onLoading.emit(false);
                this.loading = false;
                this.expandCurrentPath();
            });
    }

    canDropOnNode = canDropOnNode;
    onDropped(dragData: DragData<Node>) {
        this.onDrop.emit({
            target: dragData.target,
            source: {
                element: dragData.draggedNodes,
                mode: dragData.action,
            },
        });
    }
}
