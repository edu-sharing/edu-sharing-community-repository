import { MdsWidgetValue } from '../../types';

export interface DisplayValue {
    key: string;
    label: string;
    hint: string;
}

export class TreeNode {
    id: string;
    caption: string;
    children?: TreeNode[];
    parent?: TreeNode;
    checked: boolean;
    isHidden: boolean;
}

export function generateTree(
    definedValues: readonly MdsWidgetValue[],
    values: readonly string[],
): TreeNode[] {
    const tree: TreeNode[] = [];
    let remainingValues = definedValues;
    while (remainingValues.length > 0) {
        const newRemainingValues = [];
        for (const value of remainingValues) {
            const node: TreeNode = {
                id: value.id,
                caption: value.caption,
                checked: values.includes(value.id),
                isHidden: false,
            };
            if (!value.parent) {
                tree.push(node);
            } else {
                const parent = findNodeById(tree, value.parent);
                if (parent) {
                    node.parent = parent;
                    if (parent.children) {
                        parent.children.push(node);
                    } else {
                        parent.children = [node];
                    }
                } else {
                    newRemainingValues.push(value);
                }
            }
        }
        if (remainingValues.length === newRemainingValues.length) {
            console.error('Failed to find parents for the following nodes:', remainingValues);
            break;
        } else {
            remainingValues = newRemainingValues;
        }
    }
    return tree;
}

export function findNodes(tree: TreeNode[], predicate: (node: TreeNode) => boolean): TreeNode[] {
    let foundNodes: TreeNode[] = [];
    for (const node of tree) {
        if (predicate(node)) {
            foundNodes.push(node);
        }
        if (node.children) {
            foundNodes = [...foundNodes, ...findNodes(node.children, predicate)];
        }
    }
    return foundNodes;
}

export function findNodeById(tree: TreeNode[], id: string): TreeNode {
    for (const node of tree) {
        if (node.id === id) {
            return node;
        } else if (node.children) {
            const result = findNodeById(node.children, id);
            if (result) {
                return result;
            }
        }
    }
    return null;
}

function getFullDisplayValue(node: TreeNode): string {
    let result = node.caption;
    let parent = node.parent;
    while (parent) {
        result = `${parent.caption} / ${result}`;
        parent = parent.parent;
    }
    return result;
}

export function idToDisplayValue(id: string, tree: TreeNode[]): DisplayValue {
    const node = findNodeById(tree, id);
    return nodeToDisplayValue(node);
}

export function nodeToDisplayValue(node: TreeNode): DisplayValue {
    return {
        key: node.id,
        label: node.caption,
        hint: getFullDisplayValue(node),
    };
}

export function* iterateTree(tree: TreeNode[]): Generator<TreeNode> {
    for (const node of tree) {
        yield node;
        if (node.children) {
            for (const childNode of iterateTree(node.children)) {
                yield childNode;
            }
        }
    }
}

export function getAncestors(node: TreeNode): TreeNode[] {
    const ancestors: TreeNode[] = [];
    while (node) {
        ancestors.push(node);
        node = node.parent;
    }
    return ancestors;
}
