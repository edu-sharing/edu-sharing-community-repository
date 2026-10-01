import { computed, inject, Injectable, Signal, signal, WritableSignal } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { Toast, ToastType } from '../../../../services/toast';
import { DEFAULT_WIDGET_CONFIG_PROP } from '../types/custom-definitions';
import { HistoryOperation, NodePropertyOperation } from '../types/history-operation';
import { HistoryStep } from '../types/history-step';
import { PageStructure } from '../types/page-structure';
import { TopicHeaderConfig } from '../types/widget-config/topic-header-config';
import { convertNodeRefIntoNodeId, retrievePageVariantConfig } from '../utils/template-util';
import { TopicPageHelperService } from './topic-page-helper.service';

/**
 * The editor state the history reads and restores. Implemented by the topic page editor.
 */
export interface TopicPageHistoryHost {
    // ID of the page variant node being edited, or null while none is loaded
    variantNodeId(): string | null;
    // true while changes are recorded: edit mode on a page that has its own page config
    canRecord(): boolean;
    // the persisted structure of the variant, as last written or read
    readStructure(): PageStructure | null;
    // persists a structure on the variant and renders it
    writeStructure(structure: PageStructure): Promise<void>;
    // refreshes what displays a node after one of its properties was restored
    nodePropertyRestored(nodeId: string): Promise<void>;
    setBusy(busy: boolean): void;
}

/**
 * Handed to the body of `TopicPageHistoryService.record` to report what a structure diff misses.
 */
export interface HistoryTransaction {
    trackNodeProperty(
        nodeId: string,
        property: string,
        before: string[] | null,
        after: string[] | null,
    ): void;
    // the operation failed half-way; the structure from before it is restored
    fail(): void;
}

/**
 * How a step is named and whether it may merge into the previous one.
 */
export interface StepOptions {
    // merges the step into the previous one with the same key, if that one is recent enough
    coalesceKey?: string;
    // i18n keys filled into the label; a merged step keeps the ones of the step it merged into
    labelParams?: { [param: string]: string };
}

class Transaction implements HistoryTransaction {
    readonly nodeOperations: NodePropertyOperation[] = [];
    failed: boolean = false;

    trackNodeProperty(
        nodeId: string,
        property: string,
        before: string[] | null,
        after: string[] | null,
    ): void {
        mergeOperations(this.nodeOperations, [
            {
                kind: 'nodeProperty',
                nodeId: convertNodeRefIntoNodeId(nodeId),
                property,
                before,
                after,
            },
        ]);
    }

    fail(): void {
        this.failed = true;
    }
}

const sameValue = (a: unknown, b: unknown): boolean =>
    JSON.stringify(a ?? null) === JSON.stringify(b ?? null);

const sameTarget = (a: HistoryOperation, b: HistoryOperation): boolean =>
    a.kind === 'structure'
        ? b.kind === 'structure'
        : b.kind === 'nodeProperty' && a.nodeId === b.nodeId && a.property === b.property;

// keeps one operation per target: a later change only replaces the resulting value
const mergeOperations = (target: HistoryOperation[], operations: HistoryOperation[]): void => {
    for (const operation of operations) {
        const existing: HistoryOperation = target.find((o) => sameTarget(o, operation));
        if (existing) {
            (existing as { after: unknown }).after = operation.after;
        } else {
            target.push({ ...operation });
        }
    }
};

// the widget nodes a structure links to
const referencedWidgetNodeIds = (structure: PageStructure | null): string[] => {
    const nodeIds: string[] = [
        structure?.breadcrumbNodeId,
        structure?.headerNodeId,
        ...(structure?.swimlanes ?? []).flatMap((s) => (s.grid ?? []).map((t) => t.nodeId)),
    ];
    return nodeIds.filter((id) => !!id).map(convertNodeRefIntoNodeId);
};

// the uploaded image a widget config links to; the image is a child of the widget node
const referencedUploadNodeIds = (widgetConfig: string[] | null): string[] => {
    try {
        const config: TopicHeaderConfig = JSON.parse(widgetConfig?.[0] ?? 'null');
        return config?.userUploadedNodeId ? [config.userUploadedNodeId] : [];
    } catch {
        return [];
    }
};

/**
 * Undo/redo history of one topic page editor. Nodes the page stops referencing are kept as
 * orphans so that undo can link them again, and deleted once nothing references them anymore.
 */
@Injectable()
export class TopicPageHistoryService {
    private toast = inject(Toast);
    private topicPageHelperService = inject(TopicPageHelperService);

    private static readonly MAX_STEPS: number = 25;
    private static readonly COALESCE_WINDOW_MS: number = 2000;

    private readonly undoSteps: WritableSignal<HistoryStep[]> = signal([]);
    private readonly redoSteps: WritableSignal<HistoryStep[]> = signal([]);
    readonly canUndo: Signal<boolean> = computed((): boolean => this.undoSteps().length > 0);
    readonly canRedo: Signal<boolean> = computed((): boolean => this.redoSteps().length > 0);
    // the steps undo or redo would revert or re-apply next
    readonly nextUndoStep: Signal<HistoryStep | null> = computed(
        (): HistoryStep | null => this.undoSteps().at(-1) ?? null,
    );
    readonly nextRedoStep: Signal<HistoryStep | null> = computed(
        (): HistoryStep | null => this.redoSteps().at(-1) ?? null,
    );

    private host: TopicPageHistoryHost | null = null;
    // orphaned node ID mapped to the ID of the node it has to be a child of to be deleted
    private readonly orphanCandidates: Map<string, string> = new Map<string, string>();
    // the state the history last applied, which decides whether an orphan is still reachable
    private knownStructure: PageStructure | null = null;
    private knownVariantNodeId: string | null = null;
    private readonly knownNodeValues: Map<string, string[] | null> = new Map();
    // every change runs after the previous one has finished
    private queue: Promise<unknown> = Promise.resolve();
    private lastPushedStepId: number | null = null;
    private nextStepId: number = 1;

    attach(host: TopicPageHistoryHost): void {
        this.host = host;
    }

    /**
     * Runs a change of the page and records it as one step if it changed anything. The body has to
     * resolve only once all its writes are done; a coalesceKey merges it into an equal previous step.
     */
    record<T>(
        labelKey: string,
        body: (tx: HistoryTransaction) => Promise<T>,
        options: StepOptions = {},
    ): Promise<T> {
        return this.enqueue(async (): Promise<T> => {
            const tx: Transaction = new Transaction();
            const host: TopicPageHistoryHost = this.host;
            if (!host?.canRecord()) {
                return body(tx);
            }
            const variantNodeId: string = host.variantNodeId();
            const before: PageStructure = host.readStructure();
            let result: T;
            try {
                result = await body(tx);
            } catch (err) {
                await this.rollback(before, variantNodeId);
                throw err;
            }
            // the page got its own page config or another variant, which a step cannot span
            if (host.variantNodeId() !== variantNodeId) {
                await this.clear();
                return result;
            }
            if (tx.failed) {
                await this.rollback(before, variantNodeId);
                return result;
            }
            const after: PageStructure = host.readStructure();
            const operations: HistoryOperation[] = [];
            if (before && after && !sameValue(before, after)) {
                operations.push({ kind: 'structure', before, after });
            }
            operations.push(...tx.nodeOperations.filter((o) => !sameValue(o.before, o.after)));
            if (operations.length) {
                await this.push(labelKey, variantNodeId, operations, options);
            }
            return result;
        });
    }

    /**
     * Records property writes that were already performed outside of `record`.
     */
    recordApplied(
        labelKey: string,
        operations: NodePropertyOperation[],
        options: StepOptions = {},
    ): Promise<void> {
        return this.enqueue(async (): Promise<void> => {
            if (!this.host?.canRecord()) {
                return;
            }
            const changed: NodePropertyOperation[] = operations
                .filter((o) => !sameValue(o.before, o.after))
                .map((o) => ({ ...o, nodeId: convertNodeRefIntoNodeId(o.nodeId) }));
            if (changed.length) {
                await this.push(labelKey, this.host.variantNodeId(), changed, options);
            }
        });
    }

    /**
     * Takes over the deletion of a child node the page no longer needs, if changes are recorded.
     * Returns false if the caller has to delete the node itself.
     */
    markOrphaned(nodeId: string, parentNodeId: string): boolean {
        if (!this.host?.canRecord() || !nodeId || !parentNodeId) {
            return false;
        }
        this.orphanCandidates.set(
            convertNodeRefIntoNodeId(nodeId),
            convertNodeRefIntoNodeId(parentNodeId),
        );
        this.logState('orphan', { nodeId, parentNodeId });
        return true;
    }

    undo(): Promise<void> {
        return this.enqueue(async (): Promise<void> => {
            const step: HistoryStep = this.undoSteps().at(-1);
            if (!step || !(await this.apply(step, 'before'))) {
                return;
            }
            this.undoSteps.set(this.undoSteps().slice(0, -1));
            this.redoSteps.set([...this.redoSteps(), step]);
            this.lastPushedStepId = null;
            this.logState('undo', step.labelKey);
        });
    }

    redo(): Promise<void> {
        return this.enqueue(async (): Promise<void> => {
            const step: HistoryStep = this.redoSteps().at(-1);
            if (!step || !(await this.apply(step, 'after'))) {
                return;
            }
            this.redoSteps.set(this.redoSteps().slice(0, -1));
            this.undoSteps.set([...this.undoSteps(), step]);
            this.lastPushedStepId = null;
            this.logState('redo', step.labelKey);
        });
    }

    /**
     * Discards the history, deleting the orphans only it still referenced.
     */
    reset(): Promise<void> {
        return this.enqueue((): Promise<void> => this.clear());
    }

    // brings the page back to the structure a failed change started from
    private async rollback(before: PageStructure | null, variantNodeId: string): Promise<void> {
        if (!before || this.host?.variantNodeId() !== variantNodeId) {
            await this.clear();
            return;
        }
        try {
            await this.host.writeStructure(before);
        } catch (err) {
            console.error(err);
            await this.clear();
        }
    }

    private enqueue<T>(task: () => Promise<T>): Promise<T> {
        const run: Promise<T> = this.queue.then(task);
        this.queue = run.catch((): void => undefined);
        return run;
    }

    private async push(
        labelKey: string,
        variantNodeId: string,
        operations: HistoryOperation[],
        { coalesceKey, labelParams }: StepOptions,
    ): Promise<void> {
        const now: number = Date.now();
        this.observe(operations, 'after', variantNodeId);
        const undoSteps: HistoryStep[] = this.undoSteps();
        const top: HistoryStep = undoSteps.at(-1);
        const coalesces: boolean =
            !!top &&
            !!coalesceKey &&
            top.coalesceKey === coalesceKey &&
            top.id === this.lastPushedStepId &&
            top.variantNodeId === variantNodeId &&
            now - top.at < TopicPageHistoryService.COALESCE_WINDOW_MS;
        if (coalesces) {
            mergeOperations(top.operations, operations);
            top.at = now;
            // edits that ended where they started leave nothing to undo
            const remaining: HistoryStep[] = top.operations.every((o) =>
                sameValue(o.before, o.after),
            )
                ? undoSteps.slice(0, -1)
                : undoSteps;
            this.undoSteps.set([...remaining]);
            this.logState('merge', labelKey);
            return;
        }
        const step: HistoryStep = {
            id: this.nextStepId++,
            labelKey,
            labelParams,
            variantNodeId,
            operations,
            coalesceKey,
            at: now,
        };
        const evictedCount: number = Math.max(
            0,
            undoSteps.length + 1 - TopicPageHistoryService.MAX_STEPS,
        );
        const redoDiscarded: boolean = this.redoSteps().length > 0;
        this.undoSteps.set([...undoSteps.slice(evictedCount), step]);
        this.redoSteps.set([]);
        this.lastPushedStepId = step.id;
        this.logState('record', labelKey);
        if (redoDiscarded || evictedCount > 0) {
            await this.collectGarbage();
        }
    }

    /**
     * Writes one side of a step ('before' undoes, 'after' redoes), provided the server still
     * holds the other side.
     */
    private async apply(step: HistoryStep, side: 'before' | 'after'): Promise<boolean> {
        const host: TopicPageHistoryHost = this.host;
        if (!host || step.variantNodeId !== host.variantNodeId()) {
            await this.clear();
            return false;
        }
        host.setBusy(true);
        try {
            if (!(await this.matchesServer(step, side === 'before' ? 'after' : 'before'))) {
                this.toast.show({
                    message: 'TOPIC_PAGE.HISTORY.CHANGED_ELSEWHERE',
                    type: 'info',
                    subtype: ToastType.InfoSimple,
                });
                this.logState('refused: changed elsewhere', step.labelKey);
                await this.clear();
                return false;
            }
            const operations: HistoryOperation[] =
                side === 'before' ? [...step.operations].reverse() : step.operations;
            const restoredNodeIds: Set<string> = new Set<string>();
            for (const operation of operations) {
                if (operation.kind === 'structure') {
                    await host.writeStructure(operation[side]);
                } else {
                    await this.writeNodeProperty(operation, operation[side]);
                    restoredNodeIds.add(operation.nodeId);
                }
            }
            // a node is re-read once all of its properties are written, never half restored
            for (const nodeId of restoredNodeIds) {
                await host.nodePropertyRestored(nodeId);
            }
            this.observe(operations, side, step.variantNodeId);
            return true;
        } catch (err) {
            console.error(err);
            this.topicPageHelperService.displayErrorToast(err);
            await this.clear();
            return false;
        } finally {
            host.setBusy(false);
        }
    }

    // guards against overwriting what another editor saved or deleted in the meantime
    private async matchesServer(step: HistoryStep, side: 'before' | 'after'): Promise<boolean> {
        for (const operation of step.operations) {
            const node: Node | null = await this.topicPageHelperService.getNodeIfAvailable(
                operation.kind === 'structure' ? step.variantNodeId : operation.nodeId,
                true,
            );
            if (!node) {
                return false;
            }
            if (operation.kind === 'structure') {
                const structure: PageStructure = retrievePageVariantConfig(node)?.structure;
                if (!sameValue(structure, operation[side])) {
                    return false;
                }
            } else {
                if (!sameValue(node.properties?.[operation.property], operation[side])) {
                    return false;
                }
            }
        }
        return true;
    }

    private async writeNodeProperty(
        operation: NodePropertyOperation,
        value: string[] | null,
    ): Promise<void> {
        if (value === null) {
            await this.topicPageHelperService.resetProperty(operation.nodeId, operation.property);
        } else {
            await this.topicPageHelperService.setProperty(
                operation.nodeId,
                operation.property,
                value,
            );
        }
    }

    /**
     * Takes over the state the operations (in applied order) lead to and collects the nodes they
     * stop referencing.
     */
    private observe(
        operations: HistoryOperation[],
        side: 'before' | 'after',
        variantNodeId: string,
    ): void {
        const previousSide: 'before' | 'after' = side === 'after' ? 'before' : 'after';
        for (const operation of operations) {
            if (operation.kind === 'structure') {
                const current: string[] = referencedWidgetNodeIds(operation[side]);
                referencedWidgetNodeIds(operation[previousSide])
                    .filter((id) => !current.includes(id))
                    .forEach((id) => this.orphanCandidates.set(id, variantNodeId));
                this.knownStructure = operation[side];
                this.knownVariantNodeId = variantNodeId;
            } else {
                if (operation.property === DEFAULT_WIDGET_CONFIG_PROP) {
                    const current: string[] = referencedUploadNodeIds(operation[side]);
                    referencedUploadNodeIds(operation[previousSide])
                        .filter((id) => !current.includes(id))
                        .forEach((id) => this.orphanCandidates.set(id, operation.nodeId));
                }
                this.knownNodeValues.set(
                    operation.nodeId + '|' + operation.property,
                    operation[side],
                );
            }
        }
    }

    private async clear(): Promise<void> {
        this.undoSteps.set([]);
        this.redoSteps.set([]);
        this.lastPushedStepId = null;
        await this.collectGarbage();
        // the remaining candidates are referenced by the page itself
        this.orphanCandidates.clear();
        this.knownStructure = null;
        this.knownVariantNodeId = null;
        this.knownNodeValues.clear();
        this.logState('reset');
    }

    /**
     * Deletes the orphans that neither the page nor a step references anymore.
     */
    private async collectGarbage(): Promise<void> {
        if (!this.orphanCandidates.size) {
            return;
        }
        const variantNodeId: string = this.host?.variantNodeId();
        const currentStructure: PageStructure | null =
            variantNodeId && variantNodeId === (this.knownVariantNodeId ?? variantNodeId)
                ? this.host.readStructure()
                : null;
        // a substring test is enough: node IDs are UUIDs
        const reachable: string = JSON.stringify([
            this.knownStructure,
            currentStructure,
            this.undoSteps(),
            this.redoSteps(),
            [...this.knownNodeValues.values()],
        ]);
        const deleted: string[] = [];
        for (const [nodeId, parentNodeId] of [...this.orphanCandidates.entries()]) {
            if (reachable.includes(nodeId)) {
                continue;
            }
            this.orphanCandidates.delete(nodeId);
            try {
                const node: Node | null = await this.topicPageHelperService.getNodeIfAvailable(
                    nodeId,
                    true,
                );
                // an orphan is only ever a child of the node that linked it
                if (node?.parent?.id === parentNodeId) {
                    await this.topicPageHelperService.deleteNodeIfExists(nodeId);
                    deleted.push(nodeId);
                }
            } catch (err) {
                // a node that cannot be checked is left in place
                console.warn(err);
            }
        }
        if (deleted.length) {
            this.logState('collect garbage', { deleted });
        }
    }

    // debug output of the history stacks, to be removed once the history has proven itself
    /* eslint-disable no-console */
    private logState(action: string, details?: unknown): void {
        const describe = (step: HistoryStep) => ({
            id: step.id,
            label: step.labelKey.split('.').pop(),
            labelParams: JSON.stringify(step.labelParams ?? {}),
            operations: step.operations
                .map((o) => (o.kind === 'structure' ? 'structure' : o.property + '@' + o.nodeId))
                .join(', '),
            coalesceKey: step.coalesceKey ?? '',
        });
        console.groupCollapsed(
            `[topic-page history] ${action} · undo: ${this.undoSteps().length}` +
                ` · redo: ${this.redoSteps().length}`,
        );
        if (details !== undefined) {
            console.log(details);
        }
        console.log('undo stack (latest last)');
        console.table(this.undoSteps().map(describe));
        console.log('redo stack (next last)');
        console.table(this.redoSteps().map(describe));
        console.log('orphan candidates (node → parent)', Object.fromEntries(this.orphanCandidates));
        console.groupEnd();
    }
    /* eslint-enable no-console */
}
