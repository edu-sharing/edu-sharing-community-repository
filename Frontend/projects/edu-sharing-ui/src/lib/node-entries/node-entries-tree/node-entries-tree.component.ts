import { FlatTreeControl } from '@angular/cdk/tree';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    signal,
    ViewChild,
    WritableSignal,
} from '@angular/core';
import { MatMenuTrigger } from '@angular/material/menu';
import { Node } from 'ngx-edu-sharing-api';
import { combineLatest, Subject } from 'rxjs';
import { debounceTime, startWith, takeUntil } from 'rxjs/operators';
import { DropdownComponent } from '../../dropdown/dropdown.component';
import { NodeEntriesService } from '../../services/node-entries.service';
import { NodeHelperService } from '../../services/node-helper.service';
import { TranslationsService } from '../../translations/translations.service';
import { Target } from '../../types/option-item';
import { NodeEntriesDataType, NodeEntriesDisplayType } from '../entries-model';
import { DynamicDataSource } from './dynamic-data-source';
import { DynamicFlatNode } from './dynamic-flat-node';
import { TreeNodeService } from './tree-node.service';

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

    private destroyed = new Subject<void>();

    // tree-related variables
    dataSource: DynamicDataSource;
    treeControl: FlatTreeControl<DynamicFlatNode>;
    getLevel = (node: DynamicFlatNode) => node.level;
    hasChild = (_: number, _nodeData: DynamicFlatNode) => _nodeData.expandable;
    isExpandable = (node: DynamicFlatNode) => node.expandable;
    searchText: string = '';
    treeInitialized: WritableSignal<boolean> = signal(false);

    constructor(
        public entriesService: NodeEntriesService<T>,
        private translations: TranslationsService,
        private changeDetectorRef: ChangeDetectorRef,
        public nodeHelper: NodeHelperService,
        private treeNodeService: TreeNodeService,
    ) {
        // listening to selection changed subject
        this.entriesService.selection.changed
            .pipe(takeUntil(this.destroyed), debounceTime(0))
            .subscribe(() => this.changeDetectorRef.detectChanges());
        // listening to loading subject
        this.entriesService.dataSource.isLoadingSubject
            .pipe(takeUntil(this.destroyed))
            .subscribe(async (isLoading) => {
                // after initial load and when not already initialized, initialize the tree
                if (!isLoading && !this.treeInitialized()) {
                    await this.initializeTree();
                }
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
     * Clears the search text.
     */
    clearSearch() {
        this.searchText = '';
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
     * Collapses all nodes of the tree.
     */
    collapseNodes() {
        this.treeControl.collapseAll();
    }

    /**
     * Expands the first level of the tree.
     */
    expandNodes() {
        this.treeControl.dataNodes?.forEach((node) => {
            if (node.level === 0 && node.expandable) {
                this.treeControl.expand(node);
            }
        });
    }

    /**
     * Handle the click event on a node by toggling its selection.
     *
     * @param flatNode
     */
    updateSelectedNodes(flatNode: DynamicFlatNode) {
        if (flatNode.level === 0) {
            return;
        }
        this.entriesService.selection.toggle(flatNode.item as T);
    }

    /**
     * Helper function to initialize the tree.
     */
    private async initializeTree(): Promise<void> {
        this.treeControl = new FlatTreeControl<DynamicFlatNode>(this.getLevel, this.isExpandable);
        this.dataSource = new DynamicDataSource(this.treeControl, this.treeNodeService);
        // retrieve the current nodes from the data source and initialize the tree with it
        const nodes: Node[] = this.entriesService.dataSource.getData() as Node[];
        this.dataSource.data = await this.treeNodeService.getInitialData(nodes);
        this.treeInitialized.set(true);
        this.changeDetectorRef.detectChanges();
    }
}
