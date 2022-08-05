import { Sort } from '@angular/material/sort';

import { SelectionModel } from '@angular/cdk/collections';
import { NodeEntriesDataType } from '../node-entries/node-entries.component';
import { ActionbarComponent } from '../../shared/components/actionbar/actionbar.component';
import { ListItemSort, ListItem, Node } from '../../core-module/core.module';
import { DropAction } from '../../core-ui-module/directives/drag-nodes/drag-nodes';
import { OptionItem, Scope, CustomOptions, Target } from '../../core-ui-module/option-item';
import { CanDrop } from '../../shared/directives/nodes-drop-target.directive';
import { DragData } from '../../services/nodes-drag-drop.service';

export type NodeRoot =
    | 'MY_FILES'
    | 'SHARED_FILES'
    | 'MY_SHARED_FILES'
    | 'TO_ME_SHARED_FILES'
    | 'WORKFLOW_RECEIVE'
    | 'RECYCLE'
    | 'ALL_FILES';

export enum NodeEntriesDisplayType {
    Table,
    Grid,
    SmallGrid,
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
    scope: Scope;
    actionbar?: ActionbarComponent;
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

export enum ClickSource {
    Preview,
    Icon,
    Metadata,
    Comments,
    Overlay,
}

export type NodeClickEvent<T extends NodeEntriesDataType> = {
    element: T;
    source: ClickSource;
    attribute?: ListItem; // only when source === Metadata
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
export type GridConfig = {
    maxRows?: number;
};

export interface ListEventInterface<T extends NodeEntriesDataType> {
    updateNodes(nodes: void | T[]): void;

    getDisplayType(): NodeEntriesDisplayType;

    setDisplayType(displayType: NodeEntriesDisplayType): void;

    showReorderColumnsDialog(): void;

    addVirtualNodes(virtual: T[]): void;

    setOptions(options: ListOptions): void;

    /**
     * activate option (dropdown) generation
     */
    initOptionsGenerator(config: ListOptionsConfig): void | Promise<void>;

    getSelection(): SelectionModel<T>;
}
