import {
    Component,
    effect,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    viewChild,
} from '@angular/core';
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
})
export class SearchSelectionOverlayComponent implements OnChanges {
    @Input() nodes: Node[] = [];
    @Input() columns: ColumnType;
    @Output() deselect = new EventEmitter<Node>();

    /** Only the first column of the search config is shown on the overlay cards. */
    displayColumns: ColumnType = {};

    readonly Scope = Scope;
    readonly InteractionType = InteractionType;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;

    readonly dataSource = new NodeDataSource<Node>();
    private readonly wrapper = viewChild(NodeEntriesWrapperComponent);
    /** Suppresses `onSelectionChange` while we programmatically (re-)select all nodes. */
    private syncing = false;

    constructor() {
        // Pre-select every node so all cards render with a checked checkbox. Runs whenever the
        // wrapper becomes available or the node list changes (see syncSelection for convergence).
        effect(() => {
            const wrapper = this.wrapper();
            if (wrapper) {
                this.syncSelection(wrapper);
            }
        });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.columns) {
            // Keep only the first column of each variant so cards show a single (title) row.
            this.displayColumns = {
                Default: this.columns?.Default?.slice(0, 1),
                Table: this.columns?.Table?.slice(0, 1),
            };
        }
        if (changes.nodes) {
            this.dataSource.setData(this.nodes ?? []);
            this.dataSource.isLoading = false;
            const wrapper = this.wrapper();
            if (wrapper) {
                this.syncSelection(wrapper);
            }
        }
    }

    onSelectionChange(event: SelectionChange<NodeEntriesDataType>): void {
        if (this.syncing) {
            return;
        }
        const selected = event.source.selected as Node[];
        // A node present in our list but no longer selected was unchecked by the user.
        const removed = (this.nodes ?? []).filter((node) => !selected.includes(node));
        removed.forEach((node) => this.deselect.emit(node));
    }

    private syncSelection(wrapper: NodeEntriesWrapperComponent<NodeEntriesDataType>): void {
        // `clear()`/`select()` emit synchronous `changed` events; guard so they aren't mistaken for
        // a user unchecking cards (which would otherwise deselect everything via onSelectionChange).
        this.syncing = true;
        try {
            const selection = wrapper.getSelection();
            selection.clear();
            if (this.nodes?.length) {
                selection.select(...this.nodes);
            }
        } finally {
            this.syncing = false;
        }
    }
}
