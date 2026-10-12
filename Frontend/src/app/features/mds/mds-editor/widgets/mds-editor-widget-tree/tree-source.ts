import { Signal } from '@angular/core';
import { Observable } from 'rxjs';
import { Tree, TreeNode } from './tree';

/** A selected value as a tree source knows it. */
export interface TreeSourceValue {
    label: string;
    /** Ids of the nodes above the value, top-most first. */
    ancestorIds: string[];
}

/**
 * Supplies the nodes of a tree widget that loads them from a server instead of the valuespace,
 * e.g. level by level as nodes are expanded (see `TreeNode.hasChildren`).
 */
export interface TreeSource {
    /** The tree listed for a filter string, or without a filter (`null`). */
    getTree(filter: string | null): Observable<Tree>;
    /** Loads the children of a node marked with `hasChildren`, or their next page. */
    loadChildren(node: TreeNode): Promise<void>;
    /** Loads label and ancestors of the given values, see `getValue`. */
    loadValues(ids: string[]): void;
    /** Label and ancestors of a value, `undefined` until `loadValues` has loaded them. */
    getValue(id: string): TreeSourceValue | undefined;
    /** A checkbox shown above the tree, e.g. to limit it, `null` for none. */
    readonly headerToggle: Signal<{ label: string; checked: boolean } | null>;
    setHeaderToggle(checked: boolean): void;
    /** Whether the widget is listed without values in views that hide empty widgets. */
    readonly showWithoutValues: Signal<boolean>;
}
