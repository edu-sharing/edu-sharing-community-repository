import {ListItemSort, Node} from '../../../core-module/rest/data-object';
import {DropAction} from '../../directives/drag-nodes/drag-nodes';
import {Sort} from '@angular/material/sort';
import {CustomOptions, OptionItem, Scope, Target} from '../../option-item';
import {ListItem} from '../../../core-module/ui/list-item';
import {ActionbarComponent} from '../../../common/ui/actionbar/actionbar.component';
import {SelectionModel} from '@angular/cdk/collections';

export type NodeRoot =
    'MY_FILES'
    | 'SHARED_FILES'
    | 'MY_SHARED_FILES'
    | 'TO_ME_SHARED_FILES'
    | 'WORKFLOW_RECEIVE'
    | 'RECYCLE'
    | 'ALL_FILES';

export enum NodeEntriesDisplayType {
    Table,
    Grid,
    SmallGrid
}

export enum InteractionType {
    DefaultActionLink,
    Emitter,
    None
}

export type ListOptions = { [key in Target]?: OptionItem[] };
export type ListOptionsConfig = {
    scope: Scope,
    actionbar: ActionbarComponent,
    parent?: Node,
    customOptions?: CustomOptions,
};

export interface ListSortConfig extends Sort {
    columns: ListItemSort[];
    allowed?: boolean;
    customSortingInProgress?: boolean;
}

export type DropTarget = Node | NodeRoot;

export interface DropSource<T extends Node> {
    element: T[];
    sourceList: ListEventInterface<T>;
    mode: DropAction;
}

export interface ListDragGropConfig<T extends Node> {
    dragAllowed: boolean;
    dropAllowed?: (target: T, source: DropSource<T>) => boolean;
    dropped?: (target: T, source: DropSource<T>) => void;
}

export enum ClickSource {
    Preview,
    Icon,
    Metadata,
    Comments
}

export type NodeClickEvent<T extends Node> = {
    element: T,
    source: ClickSource,
    attribute?: ListItem // only when source === Metadata
}
export type FetchEvent = {
    offset: number,
    amount?: number;
}
export type GridConfig = {
    maxRows?: number
}

export interface ListEventInterface<T extends Node> {
    updateNodes(nodes: void | T[]): void;

    getDisplayType(): NodeEntriesDisplayType;

    setDisplayType(displayType: NodeEntriesDisplayType): void;

    showReorderColumnsDialog(): void;

    addVirtualNodes(virtual: T[]): void;

    setOptions(options: ListOptions): void;

    /**
     * activate option (dropdown) generation
     */
    initOptionsGenerator(actionbar: ListOptionsConfig): void | Promise<void>;

    getSelection(): SelectionModel<T>;
}
