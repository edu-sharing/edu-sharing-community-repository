import { Component, computed, effect, input, output, viewChild } from '@angular/core';
import { SelectionChange } from '@angular/cdk/collections';
import { Node } from 'ngx-edu-sharing-api';
import {
    ColumnType,
    InteractionType,
    NodeDataSource,
    NodeEntriesDataType,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
    Scope,
} from 'ngx-edu-sharing-ui';

/**
 * Overlay content listing the currently selected search results as a horizontally-scrolling row of
 * cards. Each card carries a checkbox; unchecking a card emits {@link deselect} so the host can
 * remove that node from the live search selection.
 */
@Component({
    selector: 'es-search-selection-overlay',
    templateUrl: './search-selection-overlay.component.html',
    styleUrls: ['./search-selection-overlay.component.scss'],
    standalone: false,
    host: {
        '[class.table-mode]': 'effectiveDisplayType() === NodeEntriesDisplayType.Table',
    },
})
export class SearchSelectionOverlayComponent {
    readonly nodes = input<Node[]>([]);
    readonly columns = input<ColumnType>();
    /** Display type of the connected results list; reused so the overlay matches the list layout. */
    readonly displayType = input<NodeEntriesDisplayType>(NodeEntriesDisplayType.SmallGrid);
    readonly deselect = output<Node>();

    /** Table stays a table; any card layout (Grid/SmallGrid) collapses to SmallGrid. */
    readonly effectiveDisplayType = computed(() =>
        this.displayType() === NodeEntriesDisplayType.Table
            ? NodeEntriesDisplayType.Table
            : NodeEntriesDisplayType.SmallGrid,
    );

    /** Columns passed to the wrapper: full set for the table, first column only for cards. */
    readonly displayColumns = computed<ColumnType>(() => {
        const columns = this.columns();
        if (this.effectiveDisplayType() === NodeEntriesDisplayType.Table) {
            // Table keeps all columns.
            return columns ?? {};
        }
        // Keep only the first column of each variant so cards show a single (title) row.
        return {
            Default: columns?.Default?.slice(0, 1),
            Table: columns?.Table?.slice(0, 1),
        };
    });

    readonly Scope = Scope;
    readonly InteractionType = InteractionType;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;

    readonly dataSource = new NodeDataSource<Node>();
    private readonly wrapper = viewChild(NodeEntriesWrapperComponent);
    /** Suppresses `onSelectionChange` while we programmatically (re-)select all nodes. */
    private syncing = false;

    constructor() {
        // Keep the data source in sync with the input node list.
        effect(() => {
            this.dataSource.setData(this.nodes() ?? []);
            this.dataSource.isLoading = false;
        });
        // Pre-select every node so all cards render with a checked checkbox. Runs whenever the
        // wrapper becomes available or the node list changes (see syncSelection for convergence).
        effect(() => {
            // Read nodes() so this re-runs when the selection changes.
            this.nodes();
            const wrapper = this.wrapper();
            if (wrapper) {
                this.syncSelection(wrapper);
            }
        });
    }

    onSelectionChange(event: SelectionChange<NodeEntriesDataType>): void {
        if (this.syncing) {
            return;
        }
        const selected = event.source.selected as Node[];
        // A node present in our list but no longer selected was unchecked by the user.
        const removed = (this.nodes() ?? []).filter((node) => !selected.includes(node));
        removed.forEach((node) => this.deselect.emit(node));
    }

    private syncSelection(wrapper: NodeEntriesWrapperComponent<NodeEntriesDataType>): void {
        // `clear()`/`select()` emit synchronous `changed` events; guard so they aren't mistaken for
        // a user unchecking cards (which would otherwise deselect everything via onSelectionChange).
        this.syncing = true;
        try {
            const selection = wrapper.getSelection();
            selection.clear();
            const nodes = this.nodes();
            if (nodes?.length) {
                selection.select(...nodes);
            }
        } finally {
            this.syncing = false;
        }
    }
}
