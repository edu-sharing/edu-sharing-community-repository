import { CollectionViewer, DataSource, SelectionChange } from '@angular/cdk/collections';
import { FlatTreeControl } from '@angular/cdk/tree';
import { BehaviorSubject, merge, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { DynamicFlatNode } from './dynamic-flat-node';
import { TreeNodeService } from './tree-node.service';

export class DynamicDataSource implements DataSource<DynamicFlatNode> {
    dataChange = new BehaviorSubject<DynamicFlatNode[]>([]);

    get data(): DynamicFlatNode[] {
        return this.dataChange.value;
    }
    set data(value: DynamicFlatNode[]) {
        this.treeControl.dataNodes = value;
        this.dataChange.next(value);
    }

    constructor(
        private treeControl: FlatTreeControl<DynamicFlatNode>,
        private treeNodeService: TreeNodeService,
    ) {}

    /**
     * Connects the data source to the tree and forwards changes in the expansion model.
     *
     * @param collectionViewer
     */
    connect(collectionViewer: CollectionViewer): Observable<DynamicFlatNode[]> {
        this.treeControl.expansionModel.changed.subscribe((change) => {
            if (
                (change as SelectionChange<DynamicFlatNode>).added ||
                (change as SelectionChange<DynamicFlatNode>).removed
            ) {
                this.handleTreeControl(change as SelectionChange<DynamicFlatNode>);
            }
        });

        return merge(collectionViewer.viewChange, this.dataChange).pipe(map(() => this.data));
    }

    disconnect(collectionViewer: CollectionViewer): void {}

    /**
     * Handles expand/collapse changes by calling the specific node toggles.
     *
     * @param change
     */
    handleTreeControl(change: SelectionChange<DynamicFlatNode>) {
        if (change.added) {
            change.added.forEach((node) => this.toggleNode(node, true));
        }
        if (change.removed) {
            change.removed
                .slice()
                .reverse()
                .forEach((node) => void this.toggleNode(node, false));
        }
    }

    /**
     * Toggles the node and either adds it to or removes it from the display list.
     */
    async toggleNode(node: DynamicFlatNode, expand: boolean) {
        // when an expansion is triggered, the node should be visually loaded
        if (expand) {
            node.isLoading.set(true);
        }
        // retrieve the children of the node either from the cache or the server
        const children = await this.treeNodeService.getChildren(node.item.ref.id);
        const index = this.data.indexOf(node);
        if (!children || index < 0) {
            // if no children exist, or the node cannot be found, return
            node.isLoading.set(false);
            return;
        }

        // collapse or expand the node(s)
        if (!expand) {
            let count = 0;
            for (
                let i = index + 1;
                i < this.data.length && this.data[i].level > node.level;
                i++, count++
            ) {}
            this.data.splice(index + 1, count);
        } else {
            const nodes = children.map(
                (childNode) =>
                    new DynamicFlatNode(
                        childNode,
                        node.level + 1,
                        this.treeNodeService.isExpandable(childNode),
                    ),
            );
            this.data.splice(index + 1, 0, ...nodes);
        }
        // notify the change
        this.dataChange.next(this.data);
        node.isLoading.set(false);
    }
}
