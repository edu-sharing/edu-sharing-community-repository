import { Node, NodeConstants } from 'ngx-edu-sharing-api';
import {
    DEFAULT_AI_CONFIG_PROP,
    DEFAULT_WIDGET_CONFIG_PROP,
    DEFAULT_PAGE_CONFIG_PROPAGATE_REF_PROP,
    DEFAULT_PAGE_CONFIG_REF_PROP,
    DEFAULT_PAGE_CONFIG_PROP,
    DEFAULT_PAGE_VARIANT_CONFIG_PROP,
    DEFAULT_PAGE_VARIANT_TEMPLATE_REF_PROP,
    DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION_PROP,
    DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION,
} from '../types/custom-definitions';
import { BapiConfigObject } from '../types/bapi-config-object';
import { PageVariantConfig } from '../types/page-variant-config';
import { Swimlane } from '../types/swimlane';
import { GridTile } from '../types/grid-tile';
import { WidgetConfig } from '../types/widget-config/widget-config';
import { PageConfig } from '../types/page-config';

/**
 * Sets the topic color depending on an existing page variant or collection node.
 */
export const retrieveTopicColor = (
    pageVariant: PageVariantConfig,
    collectionNode: Node,
    topicName: string,
): string => {
    if (pageVariant.structure.topicColor) {
        return pageVariant.structure.topicColor;
    } else if (collectionNode?.collection?.color) {
        return collectionNode.collection.color;
    }
    // set the background to some random (but deterministic) color, just for visuals
    return getTopicColor(topicName);
};

/**
 * Retrieves a background color for a given topic name (just for visuals).
 *
 * @param topicName
 */
const getTopicColor = (topicName: string): string => {
    let topicColor: string = stringToColour(topicName);

    // TODO: later, this will be stored as variable that can be changed by the user
    // check, if dark mode is preferred (https://stackoverflow.com/a/57795495)
    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
        // check, if the color is too light
        // https://stackoverflow.com/a/12043228
        const c: string = topicColor.substring(1); // strip #
        const rgb: number = parseInt(c, 16); // convert rrggbb to decimal
        const r: number = (rgb >> 16) & 0xff; // extract red
        const g: number = (rgb >> 8) & 0xff; // extract green
        const b: number = (rgb >> 0) & 0xff; // extract blue

        const luma: number = 0.2126 * r + 0.7152 * g + 0.0722 * b; // per ITU-R BT.709
        if (luma > 100) {
            // darken the color
            topicColor = shadeColor(topicColor, -60);
        }
    }
    return topicColor;
};

/**
 * Creates a hexadecimal color based on a string.
 * Reference: https://stackoverflow.com/a/16348977
 *
 * @param str
 */
const stringToColour = (str: string): string => {
    let hash = 0;
    str.split('').forEach((char) => {
        hash = char.charCodeAt(0) + ((hash << 5) - hash);
    });
    let colour = '#';
    for (let i = 0; i < 3; i++) {
        const value = (hash >> (i * 8)) & 0xff;
        colour += value.toString(16).padStart(2, '0');
    }
    return colour;
};

/**
 * Shades a given color code by a given percentage number.
 * Reference: https://stackoverflow.com/a/13532993
 *
 * @param color
 * @param percent
 */
const shadeColor = (color: string, percent: number): string => {
    let r: number = parseInt(color.substring(1, 3), 16);
    let g: number = parseInt(color.substring(3, 5), 16);
    let b: number = parseInt(color.substring(5, 7), 16);

    r = Math.round((r * (100 + percent)) / 100);
    g = Math.round((g * (100 + percent)) / 100);
    b = Math.round((b * (100 + percent)) / 100);

    r = r < 255 ? r : 255;
    g = g < 255 ? g : 255;
    b = b < 255 ? b : 255;

    r = Math.round(r);
    g = Math.round(g);
    b = Math.round(b);

    const RR: string = r.toString(16).length == 1 ? '0' + r.toString(16) : r.toString(16);
    const GG: string = g.toString(16).length == 1 ? '0' + g.toString(16) : g.toString(16);
    const BB: string = b.toString(16).length == 1 ? '0' + b.toString(16) : b.toString(16);

    return '#' + RR + GG + BB;
};

/**
 * Retrieves the ID from a given node.
 *
 * @param node
 */
export const retrieveNodeId = (node: Node): string => {
    return node?.ref.id;
};

/**
 * Prepends the workspace spaces store prefix to a given node ID.
 *
 * @param nodeId
 */
export const prependWorkspacePrefix = (nodeId: string): string => {
    if (!nodeId.startsWith(NodeConstants.SPACES_STORE_REF)) {
        return NodeConstants.SPACES_STORE_REF + nodeId;
    }
    return nodeId;
};

/**
 * Converts a given node ref including the workspace prefix into its node ID.
 * Example: workspace://SpacesStore/UUID -> UUID
 *
 * @param nodeRef
 */
export const convertNodeRefIntoNodeId = (nodeRef: string): string => {
    if (nodeRef?.includes(NodeConstants.SPACES_STORE_REF)) {
        return nodeRef.split('/')?.[nodeRef.split('/').length - 1];
    }
    return nodeRef;
};

/**
 * Retrieves the page variant template ref by checking if the node has one, otherwise set it to its ref.
 *
 * @param node
 */
export const retrievePageVariantTemplateRef = (node: Node): string => {
    const templateRefOrNodeId: string =
        node.properties?.[DEFAULT_PAGE_VARIANT_TEMPLATE_REF_PROP]?.[0] || retrieveNodeId(node);
    return prependWorkspacePrefix(templateRefOrNodeId);
};

/**
 * Retrieves the page variant template version by checking if the node has one, otherwise set it to a default.
 *
 * @param node
 */
export const retrievePageVariantTemplateVersion = (node: Node): string => {
    return (
        node.properties?.[DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION_PROP]?.[0] ||
        DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION
    );
};

/**
 * Retrieves an AI config from a given node.
 *
 * @param widgetNode
 */
export const retrieveAiConfigFromNode = (widgetNode: Node): BapiConfigObject => {
    const configString: string = widgetNode.properties[DEFAULT_AI_CONFIG_PROP]?.[0] ?? '{}';
    return JSON.parse(configString);
};

/**
 * Retrieves the prompt from an AI config.
 *
 * @param aiConfig
 * @param propertyName
 */
export const retrievePromptFromAiConfig = (
    aiConfig: BapiConfigObject,
    propertyName: keyof BapiConfigObject = 'description',
): string => {
    return aiConfig[propertyName].prompt?.messages?.find((m) => m?.role === 'user')?.content ?? '';
};

/**
 * Retrieves the page config ref from a given (collection) node.
 *
 * @param node
 */
export const retrievePageConfigRef = (node: Node): string => {
    return node.properties?.[DEFAULT_PAGE_CONFIG_REF_PROP]?.[0];
};

/**
 * Retrieves the page config from a given page config node.
 *
 * @param node
 */
export const retrievePageConfig = (node: Node): PageConfig => {
    if (node?.properties?.[DEFAULT_PAGE_CONFIG_PROP]?.[0]) {
        return JSON.parse(node.properties[DEFAULT_PAGE_CONFIG_PROP][0]);
    }
    return {};
};

/**
 * Retrieves the page config propagate ref from a given (collection) node.
 *
 * @param node
 */
export const retrievePageConfigPropagateRef = (node: Node): string => {
    return node.properties?.[DEFAULT_PAGE_CONFIG_PROPAGATE_REF_PROP]?.[0];
};

/**
 * Retrieves the variant config from a given variant node.
 *
 * @param variantNode
 */
export const retrievePageVariantConfig = (variantNode: Node): PageVariantConfig => {
    if (variantNode.properties[DEFAULT_PAGE_VARIANT_CONFIG_PROP]?.[0]) {
        return JSON.parse(variantNode.properties[DEFAULT_PAGE_VARIANT_CONFIG_PROP][0]);
    }
    return null;
};

/**
 * Helper function to prepare the page variant config by removing possible existing breadcrumbNodeId, headerNodeId and nodeIds + save propagated node IDs.
 */
export const preparePageVariantConfig = (
    pageVariant: PageVariantConfig,
    propagated: boolean = false,
    deleteHeaderIds: boolean = true,
): void => {
    pageVariant.structure.swimlanes?.forEach((swimlane: Swimlane): void => {
        swimlane.grid?.forEach((gridItem: GridTile): void => {
            if (gridItem.nodeId) {
                // if the page config was propagated, store a propagatedNodeId to load existing settings
                if (propagated) {
                    gridItem.propagatedNodeId = gridItem.nodeId;
                }
                delete gridItem.nodeId;
            }
        });
    });
    if (!deleteHeaderIds) {
        return;
    }
    if (pageVariant.structure.breadcrumbNodeId) {
        delete pageVariant.structure.breadcrumbNodeId;
    }
    if (pageVariant.structure.headerNodeId) {
        delete pageVariant.structure.headerNodeId;
    }
};

/**
 * Helper function to update the nodeId of given indexes or of the header / breadcrumb within the structure of a page variant config.
 */
export const addNodeIdToPageVariantConfig = (
    pageVariant: PageVariantConfig,
    swimlaneIndex?: number,
    gridIndex?: number,
    widgetNodeId?: string,
    isHeaderNode?: boolean,
    isBreadcrumbNode?: boolean,
): void => {
    if (!widgetNodeId) {
        return;
    }
    widgetNodeId = prependWorkspacePrefix(widgetNodeId);
    // modify header nodeId
    if (isHeaderNode) {
        pageVariant.structure.headerNodeId = widgetNodeId;
    }
    // modify header nodeId
    else if (isBreadcrumbNode) {
        pageVariant.structure.breadcrumbNodeId = widgetNodeId;
    }
    // modify nodeId of swimlane grid tile
    else if (pageVariant.structure?.swimlanes?.[swimlaneIndex]?.grid?.[gridIndex]) {
        pageVariant.structure.swimlanes[swimlaneIndex].grid[gridIndex].nodeId = widgetNodeId;
    }
};

/**
 * Retrieves the widget config from a given widget config node.
 *
 * @param node
 */
export const retrieveWidgetConfigFromNode = (node: Node): WidgetConfig => {
    if (node.properties?.[DEFAULT_WIDGET_CONFIG_PROP]?.[0]) {
        return JSON.parse(node.properties[DEFAULT_WIDGET_CONFIG_PROP][0]);
    }
    return {};
};
