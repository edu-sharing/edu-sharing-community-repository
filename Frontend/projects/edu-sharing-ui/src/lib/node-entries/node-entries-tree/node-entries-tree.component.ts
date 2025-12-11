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
            this.treeNodeService.parentIdToLastLoadedNodeId.get(node.item.parent.id) ===
                node.item.ref.id
        );
    };
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
     * Toggles the expansion state of a given node.
     *
     * @param flatNode
     * @param event
     */
    async toggleNode(flatNode: DynamicFlatNode, event: Event): Promise<void> {
        event.preventDefault();
        event.stopPropagation();
        // if the node is not already expanded, check for its children being selected before expanding it
        if (!this.treeControl.isExpanded(flatNode)) {
            if (this.entriesService.selection.isSelected(flatNode.item as T)) {
                await this.selectNodeChildren(flatNode);
            }
            this.treeControl.expand(flatNode);
        } else {
            this.treeControl.collapse(flatNode);
        }
    }

    /**
     * Handle the click event on a node by toggling its selection and expanding its children.
     *
     * @param flatNode
     */
    async updateSelectedNodes(flatNode: DynamicFlatNode): Promise<void> {
        if (flatNode.level === 0) {
            return;
        }
        if (!this.entriesService.selection.isSelected(flatNode.item as T)) {
            this.entriesService.selection.select(flatNode.item as T);
            await this.selectNodeChildren(flatNode);
            this.treeControl.expand(flatNode);
        } else {
            this.entriesService.selection.deselect(flatNode.item as T);
            await this.deselectNodeChildren(flatNode);
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

    // HELPER FUNCTIONS
    /**
     * Helper function to initialize the tree.
     */
    private async initializeTree(): Promise<void> {
        this.treeControl = new FlatTreeControl<DynamicFlatNode>(this.getLevel, this.isExpandable);
        this.dataSource = new DynamicDataSource(this.treeControl, this.treeNodeService);
        // retrieve the current nodes from the data source and initialize the tree with it
        const nodes: Node[] = this.entriesService.dataSource.getData() as Node[];
        this.dataSource.data = await this.treeNodeService.getInitialData(nodes);
        // find a first level element that can be expanded and expand it
        const firstLevelElement = this.dataSource.data.find((d) => d.level === 0 && d.expandable);
        if (firstLevelElement) {
            this.treeControl.expand(firstLevelElement);
            await this.updateTree([firstLevelElement.item as Node]);
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

    /**
     * Helper function to select the node children in a tree.
     * Recursively selects all nested children using only dataMap entries.
     *
     * @param flatNode
     */
    private async selectNodeChildren(flatNode: DynamicFlatNode): Promise<void> {
        flatNode.isLoading.set(true);

        // only call getChildren once for the initial node to avoid requesting invisible children
        const initialChildren: Partial<Node>[] = await this.treeNodeService.getChildren(
            flatNode.item,
        );

        if (initialChildren?.length) {
            for (const child of initialChildren) {
                if (child) {
                    this.selectNodeChildrenRecursive(child, new Set<string>());
                }
            }
        }

        flatNode.isLoading.set(false);
    }

    /**
     * Recursively selects children using only dataMap entries.
     *
     * @param node
     * @param visitedNodes
     */
    private selectNodeChildrenRecursive(node: Partial<Node>, visitedNodes: Set<string>): void {
        const nodeId: string = node.ref.id;

        // prevent infinite loops
        if (visitedNodes.has(nodeId)) {
            return;
        }
        visitedNodes.add(nodeId);

        // select if not selected
        if (!this.entriesService.selection.isSelected(node as T)) {
            this.entriesService.selection.select(node as T);
        }

        // check if this node has children in the dataMap
        const childrenInDataMap: Partial<Node>[] = this.treeNodeService.dataMap.get(nodeId);
        if (childrenInDataMap?.length) {
            for (const child of childrenInDataMap) {
                if (child) {
                    this.selectNodeChildrenRecursive(child, visitedNodes);
                }
            }
        }
    }

    /**
     * Helper function to deselect the node children in a tree if the given node is selected.
     * Recursively deselects all nested children using only dataMap entries.
     *
     * @param flatNode
     */
    private async deselectNodeChildren(flatNode: DynamicFlatNode): Promise<void> {
        flatNode.isLoading.set(true);

        // only call getChildren once for the initial node to avoid requesting invisible children
        const initialChildren: Partial<Node>[] = await this.treeNodeService.getChildren(
            flatNode.item,
        );

        if (initialChildren?.length) {
            for (const child of initialChildren) {
                if (child) {
                    this.deselectNodeChildrenRecursive(child, new Set<string>());
                }
            }
        }

        flatNode.isLoading.set(false);
    }

    /**
     * Recursively deselects children using only dataMap entries.
     *
     * @param node
     * @param visitedNodes
     */
    private deselectNodeChildrenRecursive(node: Partial<Node>, visitedNodes: Set<string>): void {
        const nodeId: string = node.ref.id;

        // prevent infinite loops
        if (visitedNodes.has(nodeId)) {
            return;
        }
        visitedNodes.add(nodeId);

        // deselect if selected
        if (this.entriesService.selection.isSelected(node as T)) {
            this.entriesService.selection.deselect(node as T);
        }

        // check if this node has children in the dataMap
        const childrenInDataMap: Partial<Node>[] = this.treeNodeService.dataMap.get(nodeId);
        if (childrenInDataMap?.length) {
            for (const child of childrenInDataMap) {
                if (child) {
                    this.deselectNodeChildrenRecursive(child, visitedNodes);
                }
            }
        }
    }
}
