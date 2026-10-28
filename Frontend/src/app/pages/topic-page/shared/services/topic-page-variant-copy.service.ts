import { inject, Injectable } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { v4 as uuidv4 } from 'uuid';
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import {
    DEFAULT_AI_CONFIG_PROP,
    DEFAULT_PAGE_CONFIG_ASPECT,
    DEFAULT_PAGE_CONFIG_PROP,
    DEFAULT_PAGE_NAME_PREFIX,
    DEFAULT_PAGE_VARIANT_CONFIG_ASPECT,
    DEFAULT_PAGE_VARIANT_CONFIG_PROP,
    DEFAULT_PAGE_VARIANT_NAME_PREFIX,
    DEFAULT_WIDGET_CONFIG_PROP,
    DEFAULT_WIDGET_NAME_PREFIX,
} from '../types/custom-definitions';
import { PageConfig } from '../types/page-config';
import { PageStructure } from '../types/page-structure';
import { PageVariantConfig } from '../types/page-variant-config';
import { ContentTeaserConfig } from '../types/widget-config/content-teaser-config';
import { hasCollectionFilter, rebindCollectionFilters } from '../utils/collection-filter-util';
import { TopicHeaderConfig } from '../types/widget-config/topic-header-config';
import { WidgetConfig } from '../types/widget-config/widget-config';
import { WidgetConfigObject } from '../types/widget-config-object';
import { containsPlaceholder } from '../utils/interpolate-util';
import {
    addNodeIdToPageVariantConfig,
    convertNodeRefIntoNodeId,
    markForCopy,
    mergeConfig,
    prependWorkspacePrefix,
    retrieveNodeId,
    retrievePageVariantConfig,
    retrieveWidgetConfigFromNode,
} from '../utils/template-util';
import { SwimlaneRepeatService } from './swimlane-repeat.service';
import { TopicPageHelperService } from './topic-page-helper.service';

/**
 * A widget added to a page whose config is copied in the same step: it is created in the copy of
 * its variant instead of the inherited one.
 */
export interface PendingWidget {
    variantNodeId: string;
    swimlaneIndex: number;
    gridIndex: number;
    widget: WidgetConfigObject;
    isHeaderNode?: boolean;
    isBreadcrumbNode?: boolean;
}

/**
 * Copies page variants with the widget, header and breadcrumb nodes they reference, so that a
 * copy never shares a node with its source.
 */
@Injectable()
export class TopicPageVariantCopyService {
    private swimlaneRepeatService = inject(SwimlaneRepeatService);
    private topicPageHelperService = inject(TopicPageHelperService);

    /**
     * Gives a collection a page config of its own, holding a copy of each given (inherited)
     * variant. The page config is not yet linked to the collection.
     *
     * @param selectedVariantRefId ref of the variant shown, whose copy becomes the default
     * @param defaultPosition position of the default variant among the given ones, -1 for none
     * @returns the new page config node and the position of the copy of the shown variant, or
     *          null if it was not among the given ones
     */
    async copyPageConfig(
        collectionNode: Node,
        variantNodes: Node[],
        selectedVariantRefId: string,
        defaultPosition: number,
        pendingWidget: PendingWidget | null,
        repeatContext: { collectionId: string; title: string },
    ): Promise<{ pageConfigNode: Node; selectedVariantPosition: number | null }> {
        const pageConfigNode: Node = await this.topicPageHelperService.createChild(
            collectionNode.ref.id,
            RestConstants.CCM_TYPE_MAP,
            DEFAULT_PAGE_NAME_PREFIX + uuidv4(),
            DEFAULT_PAGE_CONFIG_ASPECT,
        );
        const pageVariants: string[] = [];
        let updatedVariantRefId: string;
        for (const variantNode of variantNodes) {
            const properties: { [key: string]: string | string[] } =
                await this.topicPageHelperService.retrievePageVariantProperties(variantNode);
            const pageConfigVariantNode: Node = await this.topicPageHelperService.createChild(
                retrieveNodeId(pageConfigNode),
                RestConstants.CCM_TYPE_MAP,
                DEFAULT_PAGE_VARIANT_NAME_PREFIX + uuidv4(),
                DEFAULT_PAGE_VARIANT_CONFIG_ASPECT,
                properties,
            );
            pageVariants.push(prependWorkspacePrefix(retrieveNodeId(pageConfigVariantNode)));
            if (selectedVariantRefId.includes(retrieveNodeId(variantNode))) {
                updatedVariantRefId = prependWorkspacePrefix(retrieveNodeId(pageConfigVariantNode));
            }
            // the existing node IDs become temporaryNodeIds, so the referenced widget, breadcrumb
            // and header nodes are copied below
            const variantConfig: PageVariantConfig = retrievePageVariantConfig(variantNode);
            markForCopy(variantConfig, true);
            if (pendingWidget && pendingWidget.variantNodeId === retrieveNodeId(variantNode)) {
                await this.createPendingWidget(variantConfig, pageConfigVariantNode, pendingWidget);
            }
            // expand after the index-addressed widget handling above, which would otherwise
            // target a swimlane the expansion has moved
            await this.swimlaneRepeatService.expandRepeats(variantConfig.structure, repeatContext);
            // propagatedNodeIds must never be persisted; they only exist for temporary inheritance
            await this.persistRelinkedVariantConfig(
                variantConfig,
                pageConfigVariantNode,
                repeatContext.collectionId,
            );
        }
        const pageConfig: PageConfig = { variants: pageVariants };
        const defaultVariant: string =
            updatedVariantRefId ??
            (defaultPosition >= 0 ? pageVariants[defaultPosition] ?? pageVariants[0] : undefined);
        if (defaultVariant) {
            pageConfig.default = defaultVariant;
        }
        return {
            pageConfigNode: await this.topicPageHelperService.setPropertyAndRetrieveUpdatedNode(
                retrieveNodeId(pageConfigNode),
                DEFAULT_PAGE_CONFIG_PROP,
                JSON.stringify(pageConfig),
            ),
            selectedVariantPosition: updatedVariantRefId
                ? pageVariants.indexOf(updatedVariantRefId)
                : null,
        };
    }

    /**
     * Copies all widget nodes referenced by `temporaryNodeId` in the given variant config as
     * children of the target page variant node and persists the resulting variant config (without
     * any temporary markers) on it.
     */
    async persistRelinkedVariantConfig(
        variantConfig: PageVariantConfig,
        targetVariantNode: Node,
        collectionId?: string,
    ): Promise<Node> {
        if (!variantConfig?.structure || !targetVariantNode) {
            return targetVariantNode;
        }
        await this.createAndRelinkPageVariantWidgets(
            variantConfig,
            targetVariantNode,
            collectionId,
        );
        await this.relinkTemporaryHeaderAndBreadcrumb(variantConfig.structure, targetVariantNode);
        return await this.topicPageHelperService.setPropertyAndRetrieveUpdatedNode(
            retrieveNodeId(targetVariantNode),
            DEFAULT_PAGE_VARIANT_CONFIG_PROP,
            JSON.stringify(variantConfig),
        );
    }

    /**
     * Creates the node of a pending widget below the copied variant and points its grid tile (or
     * the header / breadcrumb) at it.
     */
    private async createPendingWidget(
        variantConfig: PageVariantConfig,
        targetVariantNode: Node,
        pending: PendingWidget,
    ): Promise<void> {
        const widgetProperties: { [key: string]: string } = {
            [DEFAULT_WIDGET_CONFIG_PROP]: JSON.stringify(pending.widget.widgetConfig),
        };
        if (pending.widget.aiConfig && Object.keys(pending.widget.aiConfig)?.length) {
            widgetProperties[DEFAULT_AI_CONFIG_PROP] = JSON.stringify(pending.widget.aiConfig);
        }
        const widgetNode: Node = await this.topicPageHelperService.createChild(
            retrieveNodeId(targetVariantNode),
            RestConstants.CCM_TYPE_MAP,
            DEFAULT_WIDGET_NAME_PREFIX + uuidv4(),
            null,
            widgetProperties,
        );
        // the targeted tile drops its temporaryNodeId, so the copy step does not overwrite the
        // freshly created widget node
        const tile =
            variantConfig.structure?.swimlanes?.[pending.swimlaneIndex]?.grid?.[pending.gridIndex];
        if (!pending.isHeaderNode && !pending.isBreadcrumbNode && tile) {
            delete tile.temporaryNodeId;
        }
        addNodeIdToPageVariantConfig(
            variantConfig,
            pending.swimlaneIndex,
            pending.gridIndex,
            retrieveNodeId(widgetNode),
            pending.isHeaderNode,
            pending.isBreadcrumbNode,
        );
    }

    /**
     * Materializes temporary breadcrumb / topic-header copy-source markers as real child nodes of
     * the target variant node. The breadcrumb is copied in full; the topic-header is reduced to a
     * node carrying only the text background color.
     */
    private async relinkTemporaryHeaderAndBreadcrumb(
        structure: PageStructure,
        targetNode: Node,
    ): Promise<void> {
        if (!structure) {
            return;
        }
        if (structure.temporaryBreadcrumbNodeId) {
            const copiedBreadcrumb: Node | null =
                await this.topicPageHelperService.copyNodeAsChildIfExists(
                    structure.temporaryBreadcrumbNodeId,
                    retrieveNodeId(targetNode),
                );
            if (copiedBreadcrumb) {
                structure.breadcrumbNodeId = prependWorkspacePrefix(
                    retrieveNodeId(copiedBreadcrumb),
                );
            }
            delete structure.temporaryBreadcrumbNodeId;
        }
        if (structure.temporaryHeaderNodeId) {
            const sourceHeaderNode: Node | null =
                await this.topicPageHelperService.getNodeIfAvailable(
                    convertNodeRefIntoNodeId(structure.temporaryHeaderNodeId),
                );
            const sourceConfig = (
                sourceHeaderNode ? retrieveWidgetConfigFromNode(sourceHeaderNode) : {}
            ) as TopicHeaderConfig;
            const textBackgroundColor: string = sourceConfig?.textBackgroundColor;
            // only create a reduced header node when there is a custom color worth preserving
            if (textBackgroundColor) {
                const reducedConfig: TopicHeaderConfig = { textBackgroundColor };
                const headerNode: Node = await this.topicPageHelperService.createChild(
                    retrieveNodeId(targetNode),
                    RestConstants.CCM_TYPE_MAP,
                    DEFAULT_WIDGET_NAME_PREFIX + uuidv4(),
                    null,
                    { [DEFAULT_WIDGET_CONFIG_PROP]: JSON.stringify(reducedConfig) },
                );
                structure.headerNodeId = prependWorkspacePrefix(retrieveNodeId(headerNode));
            }
            delete structure.temporaryHeaderNodeId;
        }
    }

    /**
     * Copies the widget node of each grid tile marked by a temporaryNodeId below the target node
     * and points the tile at the copy.
     */
    private async createAndRelinkPageVariantWidgets(
        pageVariant: PageVariantConfig,
        node: Node,
        collectionId?: string,
    ): Promise<void> {
        for (const swimlane of pageVariant.structure?.swimlanes ?? []) {
            for (const gridTile of swimlane.grid ?? []) {
                // a deleted source node leaves the tile unconfigured, like a tile without a node
                const copiedNode: Node | null = gridTile.temporaryNodeId
                    ? await this.topicPageHelperService.copyNodeAsChildIfExists(
                          gridTile.temporaryNodeId,
                          node.ref.id,
                      )
                    : null;
                delete gridTile.temporaryNodeId;
                if (copiedNode) {
                    gridTile.nodeId = prependWorkspacePrefix(copiedNode.ref.id);
                    await this.applyWidgetConfigPatch(
                        copiedNode,
                        collectionId,
                        gridTile.configPatch,
                    );
                    // the patch belongs to the template rule, not to the created variant
                    delete gridTile.configPatch;
                } else if (gridTile.configPatch && !containsPlaceholder(gridTile.configPatch)) {
                    // a tile the template left unconfigured has no node to copy, so the patch
                    // alone becomes the widget's config
                    const widgetNode: Node = await this.topicPageHelperService.createChild(
                        node.ref.id,
                        RestConstants.CCM_TYPE_MAP,
                        DEFAULT_WIDGET_NAME_PREFIX + uuidv4(),
                        null,
                        {
                            [DEFAULT_WIDGET_CONFIG_PROP]: JSON.stringify(gridTile.configPatch),
                        },
                    );
                    gridTile.nodeId = prependWorkspacePrefix(retrieveNodeId(widgetNode));
                    delete gridTile.configPatch;
                } else {
                    // an unresolved patch belongs to a repeat rule that is gone; it addresses an
                    // item that does not exist, so no widget is created from it
                    delete gridTile.configPatch;
                }
            }
        }
    }

    /**
     * Binds a copied widget node to its target: the collection ID in its property filters is
     * replaced by the given one, then the grid tile's config patch is merged on top, so a patch
     * addressing a specific collection wins over the page's own.
     */
    private async applyWidgetConfigPatch(
        node: Node,
        collectionId?: string,
        configPatch?: Partial<WidgetConfig>,
    ): Promise<void> {
        const widgetConfigJson: string = node.properties?.[DEFAULT_WIDGET_CONFIG_PROP]?.[0];
        if (!widgetConfigJson) {
            return;
        }
        const widgetConfig = JSON.parse(widgetConfigJson) as WidgetConfig;
        const hasPatch: boolean = !!configPatch && Object.keys(configPatch).length > 0;
        const propertyFilters = (widgetConfig as ContentTeaserConfig)?.propertyFilters;
        const replacesCollectionId: boolean =
            !!collectionId && hasCollectionFilter(propertyFilters);
        if (!hasPatch && !replacesCollectionId) {
            return;
        }
        if (replacesCollectionId) {
            rebindCollectionFilters(propertyFilters, collectionId);
        }
        await this.topicPageHelperService.setPropertyAndRetrieveUpdatedNode(
            node.ref.id,
            DEFAULT_WIDGET_CONFIG_PROP,
            JSON.stringify(hasPatch ? mergeConfig(widgetConfig, configPatch) : widgetConfig),
        );
    }
}
