import { Sort } from '@angular/material/sort';

import { SelectionModel } from '@angular/cdk/collections';
import { CustomOptions, OptionItem, Target } from '../types/option-item';
import { ListItem, ListItemSort } from '../types/list-item';
import { CanDrop, DragData, DropAction } from '../types/drag-drop';
import { Node } from 'ngx-edu-sharing-api';
import { ActionbarComponent } from '../actionbar/actionbar.component';
import { Observable } from 'rxjs';
import { NodeEntriesDataType } from './data-type';

export type NodeRoot =
    | 'MY_FILES'
    | 'COLLECTION_HOME'
    | 'SHARED_FILES'
    | 'WORKFLOW_RECEIVE'
    | 'RECYCLE'
    | 'ALL_FILES';

export enum NodeEntriesDisplayType {
    Table,
    Grid,
    SmallGrid,
    Tree,
}

export enum InteractionType {
    // create router link
    DefaultActionLink,
    // emit an event
    Emitter,
    None,
}

export type ListOptions = { [key in Target]?: OptionItem[] };
export type ListOptionsConfig = {
    /**
     * The actionbar(s) that receive the computed options. Pass an array to drive several
     * actionbars from the same options (e.g. a toggles-only bar plus an actions-only sticky
     * selection bar); they all stay in sync with the selection via the options helper.
     */
    actionbar?: ActionbarComponent | ActionbarComponent[];
    parent?: Node;
    customOptions?: CustomOptions;
};

export interface ListSortConfig extends Sort {
    columns: ListItemSort[];
    allowed?: boolean;
    customSortingInProgress?: boolean;
}

export type DropTarget = Node | NodeRoot;

export interface DropSource<T extends NodeEntriesDataType> {
    element: T[];
    // sourceList: ListEventInterface<T>;
    mode: DropAction;
}

export interface ListDragGropConfig<T extends NodeEntriesDataType> {
    dragAllowed: boolean;
    dropAllowed?: (dragData: DragData<T>) => CanDrop;
    dropped?: (target: Node, source: DropSource<NodeEntriesDataType>) => void;
}

export type CtrlClickBehavior = 'multiselect' | 'emit';

export enum ClickSource {
    Preview,
    Icon,
    Metadata,
    Comments,
    Overlay,
    Dropdown, // keep: used in extensions
}

export type NodeClickEvent<T extends NodeEntriesDataType> = {
    element: T;
    source: ClickSource;
    attribute?: ListItem; // only when source === Metadata
    ctrlKey?: boolean;
};
export type FetchEvent = {
    offset: number;
    amount?: number;
    /**
     * is a reset of the current data required?
     * this should be true if this was a pagination request
     */
    reset?: boolean;
};
export type GridLayout = 'grid' | 'scroll';
export type GridConfig = {
    /**
     * max amount of rows that should be visible, unset for no limit
     */
    maxRows?: number;
    /**
     * layout, defaults to 'grid'
     * 'scroll' may only be used when maxRows is not set
     */
    layout?: GridLayout;
};
export type TableConfig = {
    /* avg. column width for table layouts. */
    dataColumnWidth?: number;
    /**
     * how the columns should be shown (in table mode)
     * limit: limits the max amount based on the avgColumnWidth
     * scroll: horizontal scrolling
     */
    dataColumnLayout: 'scroll' | 'limit';
};
export type TreeConfig = {
    /**
     * whether multiple selection is allowed
     */
    multipleSelection?: boolean;
    /**
     * whether to display the file name instead of the title
     */
    showFileName?: boolean;
    /**
     * whether the parents should be selected as well when selecting a node
     */
    selectParents?: boolean;
    /**
     * whether the tree acts as source (pick nodes) or target (pick destination folder)
     */
    selectionMode?: 'source' | 'target';
    /**
     * callback to validate which nodes are valid drag sources
     */
    isValidSourceCallback?: (node: Node) => boolean;
    /** whether to show file nodes in the tree (default: true) */
    showFiles?: boolean;
    /** whether to include resolved inherited access when loading children */
    includeResolveInheritedAccess?: boolean;
    /** node attribute used to determine the initial selection state */
    initialSelectionAttribute?: string;
};

export interface ListEventInterface<T extends NodeEntriesDataType> {
    updateNodes(nodes: void | T[]): void;

    onDisplayTypeChange(): Observable<NodeEntriesDisplayType>;

    getDisplayType(): NodeEntriesDisplayType;

    setDisplayType(displayType: NodeEntriesDisplayType): void;

    showReorderColumnsDialog(): void;

    addVirtualNodes(virtual: T[], options?: { select: boolean }): void;

    setOptions(options: ListOptions): void;

    /**
     * activate option (dropdown) generation
     */
    initOptionsGenerator(config: ListOptionsConfig): void | Promise<void>;

    selectAll(): void;

    getSelection(): SelectionModel<T>;

    /**
     * triggered when nodes/objects are deleted and should not be shown in the list anymore
     */
    deleteNodes(objects: T[]): void;
}
