import { computed, inject, Injectable, Signal, signal, WritableSignal } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { Toast } from '../../../../services/toast';
import type {
    CopyOption,
    PageVariantSearchState,
} from '../../editor/add-page-variant-or-template-dialog/add-page-variant-or-template-dialog.component';
import { CreationKind, VariantPreview, VariantPreviewRestoreState } from '../types/variant-preview';
import { retrieveNodeId } from '../utils/template-util';
import { TopicPageHelperService } from './topic-page-helper.service';

/**
 * The page a variant preview replaces and hands back. Implemented by the topic page editor.
 */
export interface VariantPreviewHost {
    // true while a global template is edited, which a preview never replaces
    isGlobalTemplateOpen(): boolean;
    // captures the collection's page and closes its editing UI for the preview
    enterPreview(): VariantPreviewRestoreState;
    // renders a listed node read-only in place of the collection's page
    showNode(node: Node): Promise<void>;
    // puts the captured page back without reloading it
    restorePage(state: VariantPreviewRestoreState): void;
    // reloads the restored page, to apply a profiling selection made during the preview
    reloadPage(): Promise<void>;
    createFrom(kind: CreationKind, node: Node, copyOption: CopyOption): Promise<void>;
}

/**
 * Pages through the nodes listed by the creation dialog of a page variant or template, showing
 * one at a time in place of the collection's page. A preview never writes.
 */
@Injectable()
export class TopicPageVariantPreviewService {
    private toast = inject(Toast);
    private topicPageHelperService = inject(TopicPageHelperService);

    private host: VariantPreviewHost | null = null;
    private readonly previewState: WritableSignal<VariantPreview | null> = signal(null);
    private readonly restoreStateValue: WritableSignal<VariantPreviewRestoreState | null> =
        signal(null);
    // identifies the latest preview load, so that an outdated one is dropped
    private loadId: number = 0;

    readonly preview: Signal<VariantPreview | null> = this.previewState.asReadonly();
    readonly restoreState: Signal<VariantPreviewRestoreState | null> =
        this.restoreStateValue.asReadonly();
    readonly node: Signal<Node | null> = computed(() => {
        const preview: VariantPreview | null = this.previewState();
        return preview?.nodes[preview.index] ?? null;
    });

    attach(host: VariantPreviewHost): void {
        this.host = host;
    }

    /**
     * Shows a listed node in place of the collection's page, read-only and resolved for this
     * collection. Paging then moves through the other nodes of the same search.
     */
    async start(
        kind: CreationKind,
        copyOption: CopyOption,
        searchState: PageVariantSearchState | null,
        node: Node,
    ): Promise<void> {
        if (!this.host || this.host.isGlobalTemplateOpen()) {
            return;
        }
        let nodes: Node[] = searchState?.nodes ?? [];
        let index: number = nodes.findIndex(
            (listedNode: Node): boolean => retrieveNodeId(listedNode) === retrieveNodeId(node),
        );
        if (index === -1) {
            nodes = [node];
            index = 0;
        }
        // a new list from the dialog keeps the state captured when the first preview started
        if (!this.restoreStateValue()) {
            this.restoreStateValue.set(this.host.enterPreview());
        }
        this.previewState.set({ kind, copyOption, nodes, index, searchState });
        await this.showAt(index);
    }

    /**
     * Shows the listed node at the given position of the running preview.
     */
    async showAt(index: number): Promise<void> {
        const preview: VariantPreview | null = this.previewState();
        const listedNode: Node = preview?.nodes[index];
        if (!listedNode) {
            return;
        }
        this.previewState.set({ ...preview, index });
        const loadId: number = ++this.loadId;
        try {
            const node: Node | null = await this.topicPageHelperService.getNodeIfAvailable(
                retrieveNodeId(listedNode),
            );
            if (loadId !== this.loadId) {
                return;
            }
            if (!node) {
                this.toast.error(null, 'TOPIC_PAGE.VARIANT_PREVIEW.UNAVAILABLE', {
                    title: listedNode.title || listedNode.name,
                });
                await this.remove(listedNode);
                return;
            }
            await this.host.showNode(node);
        } catch (err) {
            console.error(err);
            this.topicPageHelperService.displayErrorToast();
        }
    }

    /**
     * Creates a page variant or template from the node shown in the running preview.
     */
    async use(): Promise<void> {
        const preview: VariantPreview | null = this.previewState();
        const node: Node | null = this.node();
        if (!preview || !node) {
            return;
        }
        await this.end();
        await this.host.createFrom(preview.kind, node, preview.copyOption);
    }

    /**
     * Ends the running preview. A profiling selection made meanwhile is applied to the restored
     * page, as the preview kept it from switching the variant.
     */
    async end(): Promise<void> {
        const restoreState: VariantPreviewRestoreState | null = this.restoreStateValue();
        if (!restoreState) {
            return;
        }
        this.leave();
        const selectedVariables: string = JSON.stringify(
            this.topicPageHelperService.getSelectedVariables() ?? null,
        );
        if (selectedVariables !== restoreState.selectedVariables) {
            await this.host.reloadPage();
        }
    }

    /**
     * Drops a node that no longer exists from the running preview. If it was the one shown, its
     * neighbour is shown instead; without any node left, the preview ends.
     */
    async remove(node: Node): Promise<void> {
        const preview: VariantPreview | null = this.previewState();
        const removedIndex: number =
            preview?.nodes.findIndex(
                (listedNode: Node): boolean => retrieveNodeId(listedNode) === retrieveNodeId(node),
            ) ?? -1;
        if (removedIndex === -1) {
            return;
        }
        const nodes: Node[] = preview.nodes.filter((_, index: number) => index !== removedIndex);
        if (!nodes.length) {
            await this.end();
            return;
        }
        const index: number =
            removedIndex < preview.index
                ? preview.index - 1
                : Math.min(preview.index, nodes.length - 1);
        this.previewState.set({ ...preview, nodes, index });
        if (removedIndex === preview.index) {
            await this.showAt(index);
        }
    }

    /**
     * Restores the collection's page as it was before the running preview, without reloading it.
     */
    leave(): void {
        const restoreState: VariantPreviewRestoreState | null = this.restoreStateValue();
        this.loadId++;
        this.previewState.set(null);
        this.restoreStateValue.set(null);
        if (restoreState) {
            this.host?.restorePage(restoreState);
        }
    }
}
