import { inject, Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { v4 as uuidv4 } from 'uuid';
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import { CopyOption } from '../../editor/add-page-variant-or-template-dialog/add-page-variant-or-template-dialog.component';
import {
    DEFAULT_PAGE_CONFIG_ASPECT,
    DEFAULT_PAGE_NAME_PREFIX,
    DEFAULT_PAGE_VARIANT_CONFIG_ASPECT,
    DEFAULT_PAGE_VARIANT_IS_TEMPLATE_PROP,
    DEFAULT_PAGE_VARIANT_PROFILING_PROPS,
    DEFAULT_PAGE_VARIANT_TEMPLATE_REF_PROP,
    DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION,
    DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION_PROP,
} from '../types/custom-definitions';
import { PageConfig } from '../types/page-config';
import {
    convertNodeRefIntoNodeId,
    prependWorkspacePrefix,
    retrieveNodeId,
    retrievePageConfig,
} from '../utils/template-util';
import { TopicPageHelperService } from './topic-page-helper.service';

type NodeProperties = { [key: string]: string | string[] };

/**
 * Builds the nodes of new page variants and templates: the variant node copied from a source and
 * the page config folder a page template is kept in.
 */
@Injectable()
export class TopicPageVariantNodesService {
    private topicPageHelperService = inject(TopicPageHelperService);
    private translate = inject(TranslateService);

    /**
     * Creates a page variant node below a page config folder, named after its source and carrying
     * the source's profiling.
     */
    async createVariantNode(
        parentNodeId: string,
        sourceNode: Node,
        properties: NodeProperties,
    ): Promise<Node> {
        const node: Node = await this.topicPageHelperService.createChild(
            parentNodeId,
            RestConstants.CCM_TYPE_MAP,
            // the UUIDs appended by earlier copies are dropped, so they do not stack up
            sourceNode.name.replace(
                /(_[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})+$/i,
                '',
            ) +
                '_' +
                uuidv4(),
            DEFAULT_PAGE_VARIANT_CONFIG_ASPECT,
            properties,
        );
        return (await this.copyProfilingProperties(sourceNode, retrieveNodeId(node))) ?? node;
    }

    /**
     * The properties of a new page template built from a source. A template source makes it a
     * child template that tracks the source's version; any other source makes it a root template.
     */
    async templateVariantProperties(
        sourceNode: Node,
        copyOption: CopyOption,
    ): Promise<NodeProperties> {
        const properties: NodeProperties =
            await this.topicPageHelperService.retrievePageVariantProperties(
                sourceNode,
                '_' + this.translate.instant('TOPIC_PAGE.COPY_SUFFIX'),
            );
        properties[DEFAULT_PAGE_VARIANT_IS_TEMPLATE_PROP] = 'true';
        if (copyOption === CopyOption.Template) {
            // a child template stores the compound "{parent_sync}:{own_counter}" version
            const parentVersion =
                sourceNode.properties?.[DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION_PROP]?.[0] ??
                DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION;
            properties[DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION_PROP] =
                parentVersion + ':' + DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION;
            properties[DEFAULT_PAGE_VARIANT_TEMPLATE_REF_PROP] = prependWorkspacePrefix(
                retrieveNodeId(sourceNode),
            );
        } else {
            properties[DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION_PROP] =
                DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION;
        }
        if (!properties[RestConstants.LOM_PROP_TITLE]) {
            properties[RestConstants.LOM_PROP_TITLE] = this.translate.instant(
                'TOPIC_PAGE.DEFAULT_PAGE_TEMPLATE_NAME',
            );
        }
        return properties;
    }

    /**
     * Resolves the page config folder holding a collection's page templates, created below the
     * collection when it has none yet. Linking a new folder to the collection is up to the caller.
     */
    async getOrCreatePropagateConfig(
        collectionNode: Node,
        propagateConfigRef: string | null,
    ): Promise<{ node: Node; pageConfig: PageConfig }> {
        if (!propagateConfigRef) {
            const node: Node = await this.topicPageHelperService.createChild(
                collectionNode.ref.id,
                RestConstants.CCM_TYPE_MAP,
                DEFAULT_PAGE_NAME_PREFIX + uuidv4(),
                DEFAULT_PAGE_CONFIG_ASPECT,
            );
            return { node, pageConfig: { variants: [] } };
        }
        const node: Node = await this.topicPageHelperService.getNode(
            convertNodeRefIntoNodeId(propagateConfigRef),
        );
        const pageConfig: PageConfig = retrievePageConfig(node);
        pageConfig.variants ??= [];
        return { node, pageConfig };
    }

    /**
     * Copies profiling properties from a source node to a target node if they are present.
     * Written apart from the node creation, which fails with them.
     *
     * @returns the updated target node, or null if the source has no profiling
     */
    async copyProfilingProperties(sourceNode: Node, targetNodeId: string): Promise<Node | null> {
        let modified: boolean = false;
        for (const prop of DEFAULT_PAGE_VARIANT_PROFILING_PROPS) {
            if (sourceNode.properties?.[prop]?.length) {
                await this.topicPageHelperService.setProperty(
                    targetNodeId,
                    prop,
                    sourceNode.properties[prop],
                );
                modified = true;
            }
        }
        if (modified) {
            return await this.topicPageHelperService.getNodeUncached(targetNodeId);
        }
        return null;
    }

    /**
     * Writes the variable dimension values of a changes map (everything except the title) onto a
     * given node.
     *
     * @returns the updated node, or null if there was nothing to write
     */
    async applyPersistedParameters(
        targetNodeId: string,
        changesMap: Map<string, string | string[]>,
    ): Promise<Node | null> {
        const parameterChanges: [string, string | string[]][] = Array.from(
            changesMap?.entries() ?? [],
        ).filter(([key]) => key !== RestConstants.LOM_PROP_TITLE);
        if (!parameterChanges.length) {
            return null;
        }
        for (const [key, value] of parameterChanges) {
            await this.topicPageHelperService.setProperty(targetNodeId, key, value);
        }
        return await this.topicPageHelperService.getNodeUncached(targetNodeId);
    }
}
