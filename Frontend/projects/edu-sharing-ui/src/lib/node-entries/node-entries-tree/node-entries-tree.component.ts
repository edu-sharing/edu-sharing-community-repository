import { FlatTreeControl } from '@angular/cdk/tree';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    HostBinding,
    OnDestroy,
    signal,
    ViewChild,
    WritableSignal,
    inject,
} from '@angular/core';
import { MatMenuTrigger } from '@angular/material/menu';
import { Node, RestConstants } from 'ngx-edu-sharing-api';
import { combineLatest, Subject } from 'rxjs';
import { debounceTime, startWith, takeUntil } from 'rxjs/operators';
import { DropdownComponent } from '../../dropdown/dropdown.component';
import { NodeEntriesService } from '../../services/node-entries.service';
import { NodeHelperService } from '../../services/node-helper.service';
import { NodesDragDropService } from '../../services/nodes-drag-drop.service';
import { UIService } from '../../services/ui.service';
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
    private changeDetectorRef = inject(ChangeDetectorRef);
    entriesService = inject<NodeEntriesService<T>>(NodeEntriesService);
    nodeHelper = inject(NodeHelperService);
    private elementRef = inject(ElementRef);
    private nodesDragDropService = inject(NodesDragDropService);
    private translations = inject(TranslationsService);
    private treeNodeService = inject(TreeNodeService);

    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly Target = Target;
    readonly UIService = UIService;

    @HostBinding('class.is-loading') protected isLoading = false;

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
    indentOffset: WritableSignal<number> = signal(0);
    // 28px (toggle, checkbox, icon) + 8px (gap)
    protected readonly treeNodeIndent: number = 36;

    constructor() {
        // listening to the selection changed subject
        this.entriesService.selection.changed
            .pipe(takeUntil(this.destroyed), debounceTime(0))
            .subscribe(() => this.changeDetectorRef.detectChanges());
        // initialize the tree once data is available and loading is done
        combineLatest([
            this.entriesService.dataSource.isLoadingSubject.pipe(startWith(false)),
            this.entriesService.dataSource.connect().pipe(startWith([] as NodeEntriesDataType[])),
        ])
            .pipe(takeUntil(this.destroyed))
            .subscribe(async ([isLoading, data]) => {
                this.isLoading = isLoading !== false;
                if (!isLoading && data?.length && !this.treeInitialized()) {
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
        // define the indent offset (checkbox does replace first indent)
        if (this.entriesService.checkbox) {
            this.indentOffset.set(-1);
        }
        // set apply selection callback
        this.treeNodeService.setApplySelectionCallback((nodes: Node[]) => {
            this.entriesService.selection.select(...(nodes as T[]));
            this.changeDetectorRef.detectChanges();
        });
    }

    onContextMenu(event: MouseEvent | Event, node: T): void {
        event.stopPropagation();
        event.preventDefault();
        if (!this.dropdown) {
            return;
        }
        if (event instanceof MouseEvent) {
            ({ clientX: this.dropdownLeft, clientY: this.dropdownTop } = event);
        } else {
            ({ x: this.dropdownLeft, y: this.dropdownTop } = (
                event.target as HTMLElement
            ).getBoundingClientRect());
        }
        if (UIService.isMobileWidth()) {
            this.entriesService.openDropdown(this.dropdown, node, () =>
                this.dropdown.triggerBottomSheet(),
            );
        } else {
            this.entriesService.openDropdown(this.dropdown, node, () =>
                this.menuTrigger.openMenu(),
            );
        }
    }

    async openMenu(node: T): Promise<void> {
        if (UIService.isMobileWidth()) {
            this.entriesService.openDropdown(this.dropdown, node, () =>
                this.dropdown.triggerBottomSheet(),
            );
        } else {
            this.entriesService.openDropdown(this.dropdown, node);
        }
    }

    hasDropdownOptions(): boolean {
        return this.entriesService.options?.[Target.List]?.some(
            (o) => o.isEnabled && !o.showAlways,
        );
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
        this.treeNodeService.setApplySelectionCallback(null);
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
            (this.selectionMode() === 'target' && !this.isValidInsertTarget(flatNode)) ||
            (this.selectionMode() === 'source' && !this.isValidSource(flatNode))
        ) {
            return;
        }
        const node: T = flatNode.item as T;
        const handleParentSelection = (): void => {
            if (!this.entriesService.treeConfig?.selectParents) {
                return;
            }

            const parents: T[] = [];
            let parent = this.getParentNode(flatNode);

            while (parent && parent.level > 0) {
                parents.push(parent.item as T);
                parent = this.getParentNode(parent);
            }

            if (parents.length) {
                this.entriesService.selection.select(...parents);
            }
        };
        const handleChildrenDeselection = (): void => {
            if (!this.entriesService.treeConfig?.selectParents) {
                return;
            }

            const dataMap = this.treeNodeService.getDataMap();
            const queue: string[] = [flatNode.item.ref.id];
            const visited = new Set<string>();
            const childrenToDeselect: T[] = [];

            while (queue.length) {
                const parentId = queue.shift()!;
                if (visited.has(parentId)) {
                    continue;
                }
                visited.add(parentId);

                const children: Partial<Node>[] = dataMap.get(parentId) ?? [];
                for (const child of children) {
                    childrenToDeselect.push(child as T);
                    const childId = child.ref?.id;
                    if (childId && !visited.has(childId)) {
                        queue.push(childId);
                    }
                }
            }

            if (childrenToDeselect.length) {
                this.entriesService.selection.deselect(...childrenToDeselect);
            }
        };
        // either multiple selection is allowed or a key press on cmd / strg is detected
        const multipleSelectionAllowed: boolean =
            this.entriesService.treeConfig?.multipleSelection || false;
        const ctrlOrMetaKeyPressed: boolean = event && (event.ctrlKey || event.metaKey);
        if (multipleSelectionAllowed || ctrlOrMetaKeyPressed) {
            // multi-select: add or remove the node from the selection
            if (this.entriesService.selection.isSelected(node)) {
                this.entriesService.selection.deselect(node);
                handleChildrenDeselection();
            } else {
                this.entriesService.selection.select(node);
                handleParentSelection();
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
                handleParentSelection();
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
            node.isLoading.set(true);
            try {
                await this.treeNodeService.getFurtherChildren(parentId);
                setTimeout(async () => {
                    await this.triggerNodeUpdate(parentId);
                }, 100);
            } catch (error) {
                console.error(error);
            } finally {
                node.isLoading.set(false);
            }
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
        // custom validation callback defined?
        if (this.treeNodeService.getCustomIsValidTargetCallback()) {
            return this.treeNodeService.getCustomIsValidTargetCallback()(flatNode.item as Node);
        }
        if (flatNode.item?.mediatype) {
            return flatNode.item.mediatype === 'collection' || flatNode.item.mediatype === 'folder';
        }
        return false;
    }

    /**
     * Util function to check if a node is a valid source.
     *
     * @param flatNode
     */
    isValidSource(flatNode: DynamicFlatNode): boolean {
        // custom validation callback defined?
        if (this.treeNodeService.getCustomIsValidSourceCallback()) {
            return this.treeNodeService.getCustomIsValidSourceCallback()(flatNode.item as Node);
        }
        return (
            flatNode.item?.mediatype === 'collection' ||
            flatNode.item?.type === RestConstants.CCM_TYPE_IO
        );
    }

    // HELPER FUNCTIONS
    /**
     * Helper function to initialize the tree.
     */
    private async initializeTree(): Promise<void> {
        this.treeControl = new FlatTreeControl<DynamicFlatNode>(this.getLevel, this.isExpandable);
        this.dataSource = new DynamicDataSource(this.treeControl, this.treeNodeService);
        // Base "already initialized" on the rendered data (currentData), not dataMap: a global
        // nodesChanged can populate dataMap (via refreshTree) before the tree is built, leaving
        // currentData empty — that would otherwise skip init and render a blank tree permanently.
        const alreadyInitialized = this.treeNodeService.getCurrentData().length > 0;
        if (!alreadyInitialized) {
            // discard any dataMap entries populated prematurely, then build from the data source
            this.treeNodeService.resetData();
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
        const treeElement = this.treeNodeService
            .getCurrentData()
            .find((d) => d.item.ref.id === nodeId);
        if (treeElement) {
            const isExpanded = this.treeControl.isExpanded(treeElement);
            await this.dataSource.toggleNode(treeElement, !isExpanded);
            await this.dataSource.toggleNode(treeElement, isExpanded);
        }
    }

    /**
     * Helper function to get the parent node of a given node in the tree.
     *
     * @param node
     */
    private getParentNode(node: DynamicFlatNode): DynamicFlatNode | null {
        const parentId = node.item.parent?.id;
        if (!parentId) {
            return null;
        }
        const treeElement: DynamicFlatNode = this.treeNodeService
            .getCurrentData()
            .find((d) => d.item.ref.id === parentId);
        if (!treeElement) {
            return null;
        }
        return treeElement;
    }
}
