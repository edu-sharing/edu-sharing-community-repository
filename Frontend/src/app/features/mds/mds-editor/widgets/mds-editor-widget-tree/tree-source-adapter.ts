import { NestedTreeControl } from '@angular/cdk/tree';
import { BehaviorSubject, Observable } from 'rxjs';
import { DisplayValue } from '../DisplayValues';
import { Tree, TreeNode } from './tree';
import { TreeSource } from './tree-source';

/**
 * The nodes left expanded in each tree shown before. Kept apart from the views, so a tree shown
 * again, e.g. when its dropdown is reopened, looks the way the user left it.
 */
const expandedNodes = new WeakMap<Tree, Set<TreeNode>>();

/**
 * Adapts the trees of a `TreeSource` to the tree view, apart from the trees of the valuespace:
 * children loaded on expansion, and trees shown as delivered with a checked state from the values.
 */
export class TreeSourceAdapter {
    private readonly loadingNodes = new Set<TreeNode>();
    /**
     * Children of nodes marked with `hasChildren`. A rendered node reads its children only once,
     * so loaded children reach it through this subject.
     */
    private readonly lazyChildren = new Map<TreeNode, BehaviorSubject<TreeNode[]>>();
    private shownTree: Tree | null = null;

    /**
     * @param changed called when loading starts and ends, so the host can render the change
     */
    constructor(
        private readonly source: TreeSource,
        private readonly treeControl: NestedTreeControl<TreeNode>,
        private readonly changed: () => void,
    ) {
        treeControl.expansionModel.changed.subscribe(({ added, removed }) => {
            const expanded = this.shownTree && expandedNodes.get(this.shownTree);
            added.forEach((node) => expanded?.add(node));
            removed.forEach((node) => expanded?.delete(node));
        });
    }

    isLoading(node: TreeNode): boolean {
        return this.loadingNodes.has(node);
    }

    getChildren(node: TreeNode): TreeNode[] | Observable<TreeNode[]> {
        if (!node.hasChildren) {
            return node.children;
        }
        let children = this.lazyChildren.get(node);
        if (!children) {
            children = new BehaviorSubject(node.children ?? []);
            this.lazyChildren.set(node, children);
        }
        return children;
    }

    /** Loads the children of a node, or their next page. */
    async loadChildren(node: TreeNode): Promise<void> {
        if (this.loadingNodes.has(node)) {
            return;
        }
        this.loadingNodes.add(node);
        this.changed();
        try {
            await this.source.loadChildren(node);
        } catch {
            // the failed request reports the error to the user
            this.treeControl.collapse(node);
        }
        // marks the node as loaded, also when it turned out to have no children
        node.children ??= [];
        this.loadingNodes.delete(node);
        this.lazyChildren.get(node)?.next([...node.children]);
        this.changed();
    }

    /** Expands a node, waiting until its children are loaded. */
    async expandNode(node: TreeNode): Promise<void> {
        if (node.hasChildren && !node.children) {
            await this.loadChildren(node);
        }
        this.treeControl.expand(node);
    }

    /**
     * Shows a tree as the source delivers it, since its nodes may match a filter on more than their
     * captions. Nodes with loaded children are expanded the first time a tree is shown, later on
     * the ones left expanded.
     */
    show(tree: Tree, values: DisplayValue[]): void {
        this.syncCheckedState(tree, values);
        this.shownTree = tree;
        let expanded = expandedNodes.get(tree);
        if (!expanded) {
            expanded = new Set([...tree.iterate()].filter((node) => node.children?.length));
            expandedNodes.set(tree, expanded);
        }
        [...expanded].forEach((node) => this.treeControl.expand(node));
    }

    /** Derives the checked state of the nodes, since a source's tree may list a value twice. */
    syncCheckedState(tree: Tree, values: DisplayValue[]): void {
        const keys = new Set(values?.map((value) => value.key));
        for (const node of tree.iterate()) {
            node.isChecked = keys.has(node.id);
        }
    }
}
