import { NestedTreeControl } from '@angular/cdk/tree';
import {
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild,
    inject,
} from '@angular/core';
import { MatTreeNestedDataSource } from '@angular/material/tree';
import { BehaviorSubject, from, merge, Observable, of, ReplaySubject, Subject } from 'rxjs';
import {
    catchError,
    debounceTime,
    distinctUntilChanged,
    map,
    switchMap,
    takeUntil,
} from 'rxjs/operators';
import { MdsEditorInstanceService, Widget } from '../../../mds-editor-instance.service';
import { DisplayValue } from '../../DisplayValues';
import { Tree, TreeNode } from '../tree';
import { TreeSource } from '../tree-source';
import { TreeSourceAdapter } from '../tree-source-adapter';
import { Toast } from '../../../../../../services/toast';
import { Helper } from '../../../../../../core-module/rest/helper';
import { RestConstants } from '../../../../../../core-module/rest/rest-constants';
import { MdsV1Service, SuggestionResponseDto } from 'ngx-edu-sharing-api';
import { MdsEditorWidgetTreeComponent } from '../mds-editor-widget-tree.component';
import { MdsWidgetType } from 'ngx-edu-sharing-ui';

let nextUniqueId = 0;

@Component({
    selector: 'es-mds-editor-widget-tree-core',
    templateUrl: './mds-editor-widget-tree-core.component.html',
    styleUrls: ['./mds-editor-widget-tree-core.component.scss'],
    standalone: false,
})
export class MdsEditorWidgetTreeCoreComponent implements OnInit, OnDestroy {
    private toast = inject(Toast);
    changeDetectorRef = inject(ChangeDetectorRef);
    private mdsEditorInstanceService = inject(MdsEditorInstanceService);
    private mdsService = inject(MdsV1Service);

    /** Time to wait after the last keystroke before an autocomplete request is sent. */
    private static readonly FILTER_DEBOUNCE_MS = 250;

    readonly uid = `app-mds-editor-widget-tree-core-${nextUniqueId++}`;
    @ViewChild('input') input: ElementRef;
    @Input() widget: Widget;
    @Input() tree: Tree;
    /** Supplies the trees of a widget that loads them from a server. */
    @Input() source: TreeSource | null = null;
    @Input() set values(values: DisplayValue[]) {
        this._values = values;
        if (this.sourceAdapter && this.tree) {
            this.sourceAdapter.syncCheckedState(this.tree, values);
        }
    }
    get values(): DisplayValue[] {
        return this._values;
    }
    private _values: DisplayValue[];
    @Input() indeterminateValues: string[];
    @Input() component: MdsEditorWidgetTreeComponent;
    @Input() set filter(filter: string) {
        this.filterString$.next(filter);
    }
    suggesting: boolean;
    get filterString() {
        return this.filterString$.value;
    }
    set filterString(filterString: string) {
        this.filterString$.next(filterString);
    }
    /**
     * Whether a checked parent node should visually indicate that child nodes are checked as well.
     *
     * Checkboxes of child nodes will be disabled in this case.
     *
     * Not compatible with single-value mode.
     */
    @Input() parentImpliesChildren = false;

    @Input() suggestions: Observable<SuggestionResponseDto[]>;
    @Output() closeTree = new EventEmitter<void>();
    @Output() valuesChange = new EventEmitter<DisplayValue[]>();

    @Output() indeterminateValuesChange = new EventEmitter<string[]>();
    treeControl = new NestedTreeControl<TreeNode>(
        (node) => this.sourceAdapter?.getChildren(node) ?? node.children,
    );
    dataSource = new MatTreeNestedDataSource<TreeNode>();
    selectedNode: TreeNode;
    /** Adapts the trees of `source` to the view, `null` for a tree from the valuespace. */
    private sourceAdapter: TreeSourceAdapter | null = null;

    @Input() isMultiValue: boolean;
    private filterString$ = new BehaviorSubject<string>(null);
    private reload$ = new Subject<void>();
    private destroyed$: ReplaySubject<void> = new ReplaySubject(1);
    private isDestroyed = false;
    isTree: boolean;
    treeOpen: boolean;

    ngOnInit(): void {
        this.clearFilter();
        // deep copy for modifications
        this.isTree = [
            MdsWidgetType.MultiValueTree,
            MdsWidgetType.SingleValueTree,
            MdsWidgetType.CollectionTree,
        ].includes(this.widget.definition.type as MdsWidgetType);
        this.filterDataSource();
        if (this.source) {
            this.sourceAdapter = new TreeSourceAdapter(this.source, this.treeControl, () =>
                this.renderSourceChange(),
            );
            this.sourceAdapter.syncCheckedState(this.tree, this.values);
        }
        if (
            this.widget.definition.allowValuespaceSuggestions &&
            this.mdsEditorInstanceService.editorMode === 'nodes'
        ) {
            this.dataSource.data = this.addSuggestionInput(
                Helper.deepCopyArray(this.dataSource.data),
            );
        }
        merge(
            this.filterString$.pipe(
                map((filterString) => this.toEffectiveFilter(filterString)),
                // debounce first, then drop repeated values, so a burst of keystrokes results in
                // exactly one request per settled filter string
                debounceTime(MdsEditorWidgetTreeCoreComponent.FILTER_DEBOUNCE_MS),
                distinctUntilChanged(),
            ),
            this.reload$.pipe(map(() => this.toEffectiveFilter(this.filterString))),
        )
            .pipe(
                // `switchMap` discards the result of an outdated request, so a slow response can
                // never overwrite the tree of a newer one
                switchMap((filterString) =>
                    this.fetchSuggestedTree(filterString).pipe(
                        map((tree) => ({ filterString, tree })),
                    ),
                ),
                takeUntil(this.destroyed$),
            )
            .subscribe(({ filterString, tree }) => {
                if (tree) {
                    this.tree = tree;
                    this.filterDataSource();
                }
                if (this.sourceAdapter) {
                    this.clearFilter();
                    this.sourceAdapter.show(this.tree, this.values);
                } else {
                    this.filterNodes(filterString);
                }
                this.changeDetectorRef.markForCheck();
            });
        if (this.source) {
            // the source's tree is listed right away, without waiting for the filter debounce
            this.reload$.next();
        }
        this.treeControl.expansionModel.changed.subscribe((change) => {
            for (const expandedNode of change.added) {
                if (expandedNode.children?.every((child) => child.isHidden)) {
                    expandedNode.children.forEach((child) => (child.isHidden = false));
                }
                if (expandedNode.hasChildren && !expandedNode.children) {
                    void this.sourceAdapter?.loadChildren(expandedNode);
                }
            }
        });
    }

    setHeaderToggle(checked: boolean): void {
        this.source?.setHeaderToggle(checked);
        this.reload$.next();
    }

    /** Expands a node of a tree from the source, waiting until its children are loaded. */
    expandNode(node: TreeNode): Promise<void> {
        return this.sourceAdapter?.expandNode(node) ?? Promise.resolve();
    }

    /** Loads the next siblings of a `more` row. */
    loadMore(node: TreeNode): void {
        if (node.parent) {
            void this.sourceAdapter?.loadChildren(node.parent);
        }
    }

    /** Whether the children of a node of a tree from the source are being loaded. */
    isLoading(node: TreeNode): boolean {
        return !!this.sourceAdapter?.isLoading(node);
    }

    private toEffectiveFilter(filterString: string): string | null {
        return filterString?.length >= 2 ? filterString : null;
    }

    /** Renders loaded children of a tree from the source, together with their checked state. */
    private renderSourceChange(): void {
        if (this.isDestroyed) {
            return;
        }
        this.sourceAdapter.syncCheckedState(this.tree, this.values);
        this.changeDetectorRef.detectChanges();
    }

    /**
     * Resolves the tree to display for the given filter string, or `null` if the current widget
     * type does not fetch its values remotely (the existing tree is kept in that case).
     */
    private fetchSuggestedTree(filterString: string): Observable<Tree | null> {
        if (this.source) {
            return this.source.getTree(filterString).pipe(catchError(() => of(null)));
        }
        if (
            !['multivalueSuggestBadges', 'multivalueFixedBadges'].includes(
                this.widget.definition.type,
            )
        ) {
            return of(null);
        }
        return from(this.widget.getSuggestedValues(filterString)).pipe(
            map((suggestedValues) => Tree.generateTree(suggestedValues, [], [])),
            catchError(() => of(null)),
        );
    }

    findNodeByKeyOrCaption(keyOrCaption: string, treeRoot = this.dataSource.data): TreeNode {
        for (let leaf of treeRoot) {
            if (
                leaf.caption.toLowerCase() === keyOrCaption.toLowerCase() ||
                leaf.id.toLowerCase() === keyOrCaption.toLowerCase()
            ) {
                return leaf;
            }
            if (leaf.children) {
                const hit = this.findNodeByKeyOrCaption(keyOrCaption, leaf.children);
                if (hit) {
                    return hit;
                }
            }
        }
        return null;
    }

    ngOnDestroy() {
        this.isDestroyed = true;
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    revealInTree(node: TreeNode): void {
        let parent = node.parent;
        while (parent) {
            this.treeControl.expand(parent);
            parent = parent.parent;
        }
        setTimeout(() => this.selectNode(node));
    }

    handleKeydown(keyCode: string): boolean {
        if (keyCode === 'ArrowDown') {
            this.selectNode(this.findNextVisibleNode(this.selectedNode));
            return true;
        } else if (keyCode === 'ArrowUp') {
            this.selectNode(this.findPreviousVisibleNode(this.selectedNode));
            return true;
        }
        if (this.selectedNode) {
            if (keyCode === 'ArrowLeft') {
                if (this.treeControl.isExpanded(this.selectedNode)) {
                    this.treeControl.collapse(this.selectedNode);
                } else if (this.selectedNode.parent) {
                    this.selectNode(this.selectedNode.parent);
                }
                return true;
            } else if (keyCode === 'ArrowRight') {
                if (
                    this.hasChild(0, this.selectedNode) &&
                    !this.treeControl.isExpanded(this.selectedNode)
                ) {
                    this.treeControl.expand(this.selectedNode);
                }
                return true;
            } else if (this.selectedNode.type === 'more') {
                if (keyCode === 'Space' || keyCode === 'Enter') {
                    this.loadMore(this.selectedNode);
                    return true;
                }
            } else if (keyCode === 'Space') {
                if (this.selectedNode.type === 'group') {
                    this.treeControl.toggle(this.selectedNode);
                } else if (!this.getIsDisabled(this.selectedNode)) {
                    this.toggleNode(this.selectedNode);
                }
                return true;
            }
        }
        return false;
    }

    hasChild(_: number, node: TreeNode) {
        return (!!node.children && node.children.length > 0) || !!node.hasChildren;
    }

    toggleNode(node: TreeNode, checked?: boolean, byUser = true, closeDialog = false): void {
        if (node.type === 'group' || node.type === 'more') {
            return;
        }
        checked = checked ?? !node.isChecked;
        if (checked && !this.isMultiValue) {
            this.clearAll();
        }
        node.isChecked = checked;
        if (node.isChecked) {
            this.add(node);
        } else {
            this.remove(node);
        }
        this.valuesChange.emit(this.values);
        this.removeFromIndeterminateValues(node);
        if (node.isChecked && node.children && this.parentImpliesChildren) {
            // Toggle any checked child nodes off since they are already implicitly checked by this
            // node.
            for (const childNode of this.tree.iterate(node.children)) {
                if (childNode.isChecked) {
                    this.toggleNode(childNode, false, false);
                }
            }
        }
        if (byUser) {
            document
                .getElementById(this.getCheckboxId(node) + '-state')
                ?.setAttribute('role', 'alert');
        }
        if (closeDialog) {
            this.closeTree.emit();
        }
        this.changeDetectorRef.detectChanges();
    }

    /** Translation key of the state announced for a node, `null` for a heading. */
    getCheckedStateKey(node: TreeNode): string | null {
        if (node.type === 'group') {
            return null;
        }
        return this.getIsChecked(node) ? 'CHECKBOX_CHECKED' : 'CHECKBOX_NOT_CHECKED';
    }

    getCheckboxId(node: TreeNode): string {
        return `${node.uid}-checkbox`;
    }

    hasCheckedAncestor(node: TreeNode): boolean {
        while (node) {
            node = node.parent;
            if (node?.isChecked) {
                return true;
            }
        }
        return false;
    }

    getIsDisabled(node: TreeNode): boolean {
        if (this.parentImpliesChildren) {
            return this.hasCheckedAncestor(node);
        } else {
            return false;
        }
    }

    getIsChecked(node: TreeNode): boolean {
        if (this.parentImpliesChildren) {
            return node.isChecked || this.hasCheckedAncestor(node);
        } else {
            return node.isChecked;
        }
    }

    /** Call only via `toggleNode`. */
    private add(node: TreeNode): void {
        if (!this.values.find((value) => node.id === value.key)) {
            this.values.push(this.tree.nodeToDisplayValue(node));
            this.filterDataSource();
        }
    }

    /** Call only via `toggleNode`. */
    private remove(node: TreeNode): void {
        const index = this.values.findIndex((value) => node.id === value.key);
        if (index >= 0) {
            this.values.splice(index, 1);
            this.filterDataSource();
        }
    }

    private removeFromIndeterminateValues(node: TreeNode): void {
        node.isIndeterminate = false;
        if (this.indeterminateValues?.includes(node.id)) {
            this.indeterminateValues.splice(this.indeterminateValues.indexOf(node.id), 1);
            this.indeterminateValuesChange.emit(this.indeterminateValues);
        }
    }

    private clearAll(): void {
        for (const value of this.values) {
            const node = this.tree.findById(value.key);
            if (node) {
                node.isChecked = false;
            }
        }
        this.values = [];
        if (this.indeterminateValues) {
            for (const key of this.indeterminateValues) {
                const node = this.tree.findById(key);
                if (node) {
                    node.isIndeterminate = false;
                }
            }
            this.indeterminateValues = null;
            this.indeterminateValuesChange.emit(null);
        }
    }

    private clearFilter(): void {
        for (const node of this.tree.iterate()) {
            node.isHidden = false;
        }
    }

    private filterNodes(filterString: string): void {
        const MAX_EXPAND_NODES = 30;
        let expandedNodes = 0;
        if (!filterString) {
            this.clearFilter();
            return;
        }
        this.treeControl.collapseAll();
        for (const node of this.tree.iterate()) {
            node.isHidden = true;
        }
        const filteredNodes = this.getFilteredNodes(filterString);
        for (const node of filteredNodes) {
            for (const ancestor of this.tree.getAncestors(node)) {
                ancestor.isHidden = false;
                if (ancestor !== node && expandedNodes++ <= MAX_EXPAND_NODES) {
                    this.treeControl.expand(ancestor);
                }
            }
        }
    }

    private getFilteredNodes(filterString: string): TreeNode[] {
        return this.tree.find((node) => {
            if (!node.caption) {
                return false;
            }
            const nodeWords = node.caption.trim().toLowerCase().split(/\s+/);
            const filterWords = filterString.trim().toLowerCase().split(/\s+/);
            return filterWords.every((filterWord) =>
                nodeWords.some((nodeWord) => nodeWord.indexOf(filterWord) !== -1),
            );
        });
    }

    /** Selects node for keyboard navigation (not checkbox). */
    private selectNode(node: TreeNode): void {
        this.selectedNode = node;
        if (this.selectedNode) {
            this.scrollIntoView(this.selectedNode);
        }
    }

    private scrollIntoView(node: TreeNode, options: ScrollIntoViewOptions = {}): void {
        document
            .getElementById(this.getCheckboxId(node))
            ?.scrollIntoView({ behavior: 'smooth', block: 'center', ...options });
    }

    private findNextVisibleNode(node?: TreeNode): TreeNode | null {
        do {
            node = this.findNextExpandedNode(node);
        } while (!!node && node.isHidden);
        return node;
    }

    private findPreviousVisibleNode(node?: TreeNode): TreeNode | null {
        do {
            node = this.findPreviousExpandedNode(node);
        } while (!!node && node.isHidden);
        return node;
    }

    private findNextExpandedNode(node?: TreeNode): TreeNode | null {
        if (!node) {
            return this.tree.rootNodes[0];
        } else if (node.children?.length > 0 && this.treeControl.isExpanded(node)) {
            return node.children[0];
        }
        while (node) {
            const sibling = this.findNextSibling(node);
            if (sibling) {
                return sibling;
            } else {
                node = node.parent;
            }
        }
        return null;
    }

    private findPreviousExpandedNode(node?: TreeNode): TreeNode | null {
        if (!node) {
            return this.findLastExpandedDescendent(
                this.tree.rootNodes[this.tree.rootNodes.length - 1],
            );
        }
        const previousSibling = this.findPreviousSibling(node);
        if (previousSibling) {
            return this.findLastExpandedDescendent(previousSibling);
        }
        return node.parent;
    }

    private findLastExpandedDescendent(node: TreeNode): TreeNode {
        while (this.treeControl.isExpanded(node) && node.children?.length) {
            node = node.children[node.children.length - 1];
        }
        return node;
    }

    private findNextSibling(node: TreeNode): TreeNode | null {
        const nodesList = node.parent?.children ?? this.tree.rootNodes;
        if (nodesList && nodesList.length >= nodesList.indexOf(node)) {
            return nodesList[nodesList.indexOf(node) + 1];
        } else {
            return null;
        }
    }

    private findPreviousSibling(node: TreeNode): TreeNode | null {
        const nodesList = node.parent?.children ?? this.tree.rootNodes;
        if (nodesList && nodesList.indexOf(node) > 0) {
            return nodesList[nodesList.indexOf(node) - 1];
        } else {
            return null;
        }
    }

    private addSuggestionInput(data: TreeNode[], parent: TreeNode = null) {
        data.filter((t) => t.children).forEach(
            (t) => (t.children = this.addSuggestionInput(t.children, t)),
        );
        // already processed, skip
        if (data.some((d) => d.type === 'suggestionInput')) {
            return data;
        }
        return data.concat([
            {
                id: null,
                alternativeIds: null,
                uid: null,
                caption: null,
                parent,
                type: 'suggestionInput',
            },
        ]);
    }

    async suggestValue(value: string, node: TreeNode) {
        try {
            this.suggesting = true;
            await this.mdsService
                .suggestValue({
                    repository: RestConstants.HOME_REPOSITORY,
                    widget: this.widget.definition.id,
                    metadataset: this.mdsEditorInstanceService.mdsId,
                    parent: node.parent?.id,
                    nodeId: this.mdsEditorInstanceService.nodes$.value?.map((n) => n.ref.id),
                    caption: value,
                })
                .toPromise();
            this.toast.toast('MDS.SUGGEST_VALUE_SENT');
        } catch (e) {
            // Do nothing
        }
        this.suggesting = false;
    }
    // in case of non tree view, hide already present elements
    private filterDataSource() {
        if (this.isTree) {
            this.dataSource.data = this.tree.rootNodes;
            return;
        }
        this.dataSource.data = this.tree.rootNodes.filter(
            (n) => !this.values?.find((v) => v.key === n.id),
        );
    }
}
