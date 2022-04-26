import {Component, Input, EventEmitter, Output, ViewChild} from '@angular/core';
import { RestNodeService } from '../../../core-module/core.module';
import { TranslateService } from '@ngx-translate/core';
import { RestConstants } from '../../../core-module/core.module';
import { Node, NodeList } from '../../../core-module/core.module';
import { TemporaryStorageService } from '../../../core-module/core.module';
import {OptionItem, Scope} from '../../../core-ui-module/option-item';
import { UIService } from '../../../core-module/core.module';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import { trigger } from '@angular/animations';
import { Helper } from '../../../core-module/rest/helper';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { DropData, DragData } from '../../../core-ui-module/directives/drag-nodes/drag-nodes';
import {MatMenuTrigger} from '@angular/material/menu';
import {DropdownComponent} from '../../../shared/components/dropdown/dropdown.component';
import {OPTIONS_HELPER_CONFIG, OptionsHelperService} from '../../../core-ui-module/options-helper.service';
import {MainNavComponent} from '../../../main/navigation/main-nav/main-nav.component';
import {CdkDragDrop, CdkDragEnter, CdkDragExit} from '@angular/cdk/drag-drop';
import {DragCursorDirective} from '../../../core-ui-module/directives/drag-cursor.directive';
import {DropSource} from '../../../core-ui-module/components/node-entries-wrapper/entries-model';

@Component({
    selector: 'es-workspace-sub-tree',
    templateUrl: 'sub-tree.component.html',
    styleUrls: ['sub-tree.component.scss'],
    animations: [
        trigger(
            'openOverlay',
            UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST),
        ),
        trigger('open', UIAnimation.openOverlay()),
    ],
    providers: [OptionsHelperService, {provide: OPTIONS_HELPER_CONFIG, useValue: {
            subscribeEvents: false
        }}]
})
export class WorkspaceSubTreeComponent {
    private static MAX_FOLDER_COUNT = 100;

    @ViewChild('dropdown') dropdown: DropdownComponent;
    @ViewChild('dropdownTrigger') dropdownTrigger: MatMenuTrigger;
    dropdownLeft: string;
    dropdownTop: string;

    @Input() openPath: string[][] = [];
    @Input() set reload(reload: Boolean) {
        if (reload) {
            this.refresh();
        }
    }
    @Input() selectedPath: string[] = [];
    @Input() parentPath: string[] = [];
    @Input() depth = 1;
    @Input() selectedNode: string;
    @Input() set node(node: string) {
        this._node = node;
        if (node == null) {
            return;
        }
        this.refresh();
    }

    @Output() onClick = new EventEmitter();
    @Output() onToggleTree = new EventEmitter();
    @Output() onLoading = new EventEmitter();
    @Output() onDrop = new EventEmitter<{target: Node, source: DropSource<Node>}>();
    @Output() hasChilds = new EventEmitter();
    @Output() onUpdateOptions = new EventEmitter();

    _node: string;
    loading = true;
    _nodes: Node[];
    dragHover: Node;
    _hasChilds: boolean[] = [];
    moreItems: number;
    loadingMore: boolean;

    loadingStates: boolean[] = [];

    constructor(
        private ui: UIService,
        private nodeApi: RestNodeService,
        private storage: TemporaryStorageService,
        private optionsService: OptionsHelperService,
    ) {}

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

    onNodesHoveringChange(nodesHovering: boolean, target: Node) {
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

    contextMenu(event: any, node: Node) {
        event.preventDefault();
        event.stopPropagation();

        this.showDropdown(event, node);
    }

    private callOption(option: OptionItem, node: Node) {
        if (!option.isEnabled) return;
        option.callback(node);
        this.dropdown = null;
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
            scope: Scope.WorkspaceTree
        });
        this.optionsService.initComponents(null, null, this.dropdown);
        this.optionsService.setListener({
            onRefresh: () => this.refresh(),
            onDelete: () => this.refresh()
        });
        this.optionsService.refreshComponents();
        this.dropdownTrigger.openMenu();
    }

    dropToParent(event: any) {
        this.onDrop.emit(event);
    }

    isSelected(node: Node) {
        return (
            this.selectedNode == node.ref.id ||
            (this.isOpen(node) &&
                this.selectedPath[this.selectedPath.length - 1] ==
                    node.ref.id &&
                this.selectedNode == null)
        );
    }

    getFullPath(node: Node): string[] {
        let path = this.parentPath.slice();
        path.push(node.ref.id);
        return path;
    }

    openPathEvent(event: string[]): void {
        this.onClick.emit(event);
    }

    toggleTreeEvent(event: string[]): void {
        this.onToggleTree.emit(event);
    }

    getPathOpen(node: Node) {
        for (let i = 0; i < this.openPath.length; i++) {
            if (this.openPath[i].indexOf(node.ref.id) != -1) return i;
        }
        return -1;
    }

    isOpen(node: Node): boolean {
        return this.getPathOpen(node) != -1;
    }

    openOrCloseNode(node: Node): void {
        /*
    let pos = this.openPath.indexOf(node.ref.id);

    let path =  this.parentPath.slice();
    if (pos == -1 || pos!=this.openPath.length-1) {
      path.push(node.ref.id);
    }
    this.onClick.emit(path);
    */
        this.onClick.emit(node);
    }

    openOrCloseTree(node: Node): void {
        /*
     let pos = this.openPath.indexOf(node.ref.id);

     let path =  this.parentPath.slice();
     if (pos == -1 || pos!=this.openPath.length-1) {
     path.push(node.ref.id);
     }
     this.onClick.emit(path);
     */
        this.onToggleTree.emit({ node: node, parent: this.parentPath });
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
                this.hasChilds.emit(this._nodes && this._nodes.length);
                this.onLoading.emit(false);
                this.loading = false;
            });
    }
    getDragState() {
        return DragCursorDirective.dragState;
    }
    dragExit(event: CdkDragExit<any>) {
        DragCursorDirective.dragState.element = null;
    }

    dragEnter(event: CdkDragEnter<any>) {
        DragCursorDirective.dragState.element = event.container.data;
        DragCursorDirective.dragState.dropAllowed = true;
    }
    drop(event: CdkDragDrop<Node, any>) {
        this.onDrop.emit({
            target: event.container.data,
            source: {
                element: [event.item.data || event.previousContainer.data],
                sourceList: null,
                mode: DragCursorDirective.dragState.mode
            }
        });
        DragCursorDirective.dragState.element = null;
    }
}
