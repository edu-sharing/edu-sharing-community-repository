import { FlatTreeControl } from '@angular/cdk/tree';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    OnDestroy,
    signal,
    ViewChild,
    WritableSignal,
} from '@angular/core';
import { MatMenuTrigger } from '@angular/material/menu';
import { Node, RestConstants } from 'ngx-edu-sharing-api';
import { combineLatest, Subject } from 'rxjs';
import { debounceTime, startWith, takeUntil } from 'rxjs/operators';
import { DropdownComponent } from '../../dropdown/dropdown.component';
import { NodeEntriesService } from '../../services/node-entries.service';
import { NodeHelperService } from '../../services/node-helper.service';
import { NodesDragDropService } from '../../services/nodes-drag-drop.service';
import { TranslationsService } from '../../translations/translations.service';
import { CanDrop, DragData } from '../../types/drag-drop';
import { Target } from '../../types/option-item';
import { NodeEntriesDisplayType } from '../entries-model';
import { DynamicDataSource } from './dynamic-data-source';
import { DynamicFlatNode } from './dynamic-flat-node';
import { TreeNodeService } from './tree-node.service';
import { NodeEntriesDataType } from '../data-type';

@Component({
    selector: 'es-node-entries-tree',
    templateUrl: './node-entries-tree.component.html',
    styleUrls: ['./node-entries-tree.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class NodeEntriesTreeComponent<T extends NodeEntriesDataType>
    implements AfterViewInit, OnDestroy
{
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly Target = Target;

    @ViewChild(DropdownComponent) dropdown: DropdownComponent;
    @ViewChild('menuTrigger') menuTrigger: MatMenuTrigger;

    dropdownLeft: number;
    dropdownTop: number;

    isDragging = false;

    private destroyed = new Subject<void>();

    // tree-related variables
    dataSource: DynamicDataSource;
    treeControl: FlatTreeControl<DynamicFlatNode>;
    getLevel = (node: DynamicFlatNode) => node.level;
    hasChild = (_: number, _nodeData: DynamicFlatNode) => _nodeData.expandable;
    isExpandable = (node: DynamicFlatNode) => node.expandable;
    isLoadMoreNode = (index: number, node: DynamicFlatNode): boolean => {
        return (
            node.item.parent &&
            this.treeNodeService.getParentIdToLastLoadedNodeId().get(node.item.parent.id) ===
                node.item.ref.id
        );
    };
    selectionMode: WritableSignal<'source' | 'target'> = signal('source');
    treeInitialized: WritableSignal<boolean> = signal(false);

    constructor(
        private changeDetectorRef: ChangeDetectorRef,
        public entriesService: NodeEntriesService<T>,
        public nodeHelper: NodeHelperService,
        private elementRef: ElementRef,
        private nodesDragDropService: NodesDragDropService,
        private translations: TranslationsService,
        private treeNodeService: TreeNodeService,
    ) {
        // listening to the selection changed subject
        this.entriesService.selection.changed
            .pipe(takeUntil(this.destroyed), debounceTime(0))
            .subscribe(() => this.changeDetectorRef.detectChanges());
        // listening to the loading subject
        this.entriesService.dataSource.isLoadingSubject
            .pipe(takeUntil(this.destroyed))
            .subscribe(async (isLoading) => {
                // after initial load and when not already initialized, initialize the tree
                if (!isLoading && !this.treeInitialized()) {
                    await this.initializeTree();
                }
            });
        // listening to tree-node service node changes
        this.treeNodeService.nodesChanged
            .pipe(takeUntil(this.destroyed))
            .subscribe((nodes: Node[]) => {
                void this.updateTree(nodes);
            });
        // retrieve the selection mode
        this.selectionMode.set(this.treeNodeService.getSelectionMode());
    }

    async ngAfterViewInit(): Promise<void> {
        combineLatest([
            this.entriesService.dataSource$.pipe(startWith(void 0 as void)),
            this.entriesService.options$.pipe(startWith(void 0 as void)),
            this.entriesService.dataSource.isLoadingSubject.pipe(startWith(void 0 as void)),
            this.entriesService.selection.changed.pipe(startWith(void 0 as void)),
            this.translations.waitForInit().pipe(startWith(void 0 as void)),
        ])
            .pipe(takeUntil(this.destroyed))
            .subscribe(() => {
                this.changeDetectorRef.detectChanges();
            });
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    /**
     * Returns an array of indices for a given number of children.
     *
     * @param count
     */
    childrenIndices(count: number): number[] {
        return Array.from({ length: count || 0 }, (_, index) => index);
    }

    /**
     * Handle the click event on a node by toggling its selection and expanding its children.
     *
     * @param flatNode
     * @param event
     */
    async updateSelectedNodes(flatNode: DynamicFlatNode, event?: MouseEvent): Promise<void> {
        if (
            flatNode.level === 0 ||
            (this.selectionMode() === 'target' && !this.isValidInsertTarget(flatNode))
        ) {
            return;
        }
        const node: T = flatNode.item as T;
        // either multiple selection is allowed or a key press on cmd / strg is detected
        const multipleSelectionAllowed: boolean = this.treeNodeService.isMultipleSelectionAllowed();
        const ctrlOrMetaKeyPressed: boolean = event && (event.ctrlKey || event.metaKey);
        if (multipleSelectionAllowed || ctrlOrMetaKeyPressed) {
            // multi-select: add or remove the node from the selection
            if (this.entriesService.selection.isSelected(node)) {
                this.entriesService.selection.deselect(node);
            } else {
                this.entriesService.selection.select(node);
            }
            return;
        } else {
            // only one node can be selected at a time, so deselect all other nodes before selecting the new one
            const nodeAlreadySelected = this.entriesService.selection.isSelected(node);
            // if multiple nodes are selected, the node should be selected again
            const multipleNodesSelected = this.entriesService.selection.selected.length > 1;
            this.entriesService.selection.clear();
            if (!nodeAlreadySelected || multipleNodesSelected) {
                this.entriesService.selection.select(node);
            }
        }
    }

    /**
     * Loads further children of a node parent.
     *
     * @param node
     */
    async loadFurtherChildren(node: DynamicFlatNode): Promise<void> {
        const parentId: string = node.item.parent?.id;
        if (parentId) {
            await this.treeNodeService.getFurtherChildren(parentId);
            setTimeout(async () => {
                await this.triggerNodeUpdate(parentId);
            }, 100);
        }
    }

    canDrop = (dragData: DragData<T>): CanDrop => {
        return this.entriesService.dragDrop.dropAllowed?.(
            this.nodesDragDropService.convertDragData(this.elementRef, dragData),
        );
    };

    async drop(dragData: DragData<Node>): Promise<void> {
        this.entriesService.dragDrop.dropped(dragData.target, {
            element: dragData.draggedNodes,
            mode: dragData.action,
        });
    }

    getDragData(node: T): T[] {
        const selection = this.entriesService.selection;
        if (selection.isSelected(node)) {
            return selection.selected;
        } else {
            return [node];
        }
    }

    onDragStarted(node: T) {
        if (!this.entriesService.selection.isSelected(node)) {
            this.entriesService.selection.clear();
            this.entriesService.selection.select(node);
        }
        this.isDragging = true;
    }

    onDragEnded() {
        this.isDragging = false;
    }

    /**
     * Util function to check if a node allows inserting new children.
     *
     * @param flatNode
     */
    isValidInsertTarget(flatNode: DynamicFlatNode): boolean {
        if (flatNode.item?.mediatype) {
            return flatNode.item.mediatype === 'collection' || flatNode.item.mediatype === 'folder';
        }
        return false;
    }

    // HELPER FUNCTIONS
    /**
     * Helper function to initialize the tree.
     */
    private async initializeTree(): Promise<void> {
        this.treeControl = new FlatTreeControl<DynamicFlatNode>(this.getLevel, this.isExpandable);
        this.dataSource = new DynamicDataSource(this.treeControl, this.treeNodeService);
        // retrieve the current nodes from the data source and initialize the tree with it
        const alreadyInitialized = this.treeNodeService.getDataMap().size > 0;
        if (!alreadyInitialized) {
            const nodes: Node[] = this.entriesService.dataSource.getData() as Node[];
            await this.treeNodeService.initializeTreeData(nodes);
        }
        this.dataSource.data = this.treeNodeService.getCurrentData();
        if (!alreadyInitialized) {
            // find a first level element that can be expanded and expand it
            const firstLevelElement = this.dataSource.data.find(
                (d) => d.level === 0 && d.expandable,
            );
            if (firstLevelElement) {
                this.treeControl.expand(firstLevelElement);
                await this.updateTree([firstLevelElement.item as Node]);
            }
        } else {
            // restore the previously existing expanded states
            for (const nodeId of this.treeNodeService.getExpandedNodes()) {
                const element = this.dataSource.data
                    .slice()
                    .reverse()
                    .find((d) => d.item.ref.id === nodeId);
                if (element) {
                    this.treeControl.expansionModel.select(element);
                }
            }
        }
        this.treeInitialized.set(true);
        this.changeDetectorRef.detectChanges();
    }

    /**
     * Helper function to update given nodes of the tree.
     */
    private async updateTree(nodes: Node[]): Promise<void> {
        for (const node of nodes) {
            await this.triggerNodeUpdate(
                [RestConstants.CM_TYPE_FOLDER, RestConstants.CCM_TYPE_MAP].includes(node.type)
                    ? node.ref.id
                    : node.parent?.id ?? node.ref.id,
            );
        }
        this.changeDetectorRef.detectChanges();
    }

    /**
     * Helper function as a workaround to trigger node updates by toggling the node expansion twice.
     *
     * @param nodeId
     */
    private async triggerNodeUpdate(nodeId: string): Promise<void> {
        const treeElement = this.dataSource.data.find((d) => d.item.ref.id === nodeId);
        if (treeElement) {
            const isExpanded = this.treeControl.isExpanded(treeElement);
            await this.dataSource.toggleNode(treeElement, !isExpanded);
            await this.dataSource.toggleNode(treeElement, isExpanded);
        }
    }
}
