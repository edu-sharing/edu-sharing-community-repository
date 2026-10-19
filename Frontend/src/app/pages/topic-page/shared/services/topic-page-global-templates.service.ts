import { inject, Injectable, Signal, signal, WritableSignal } from '@angular/core';
import { Node, RestConstants as ApiRestConstants } from 'ngx-edu-sharing-api';
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import { NodeHelperService } from '../../../../services/node-helper.service';
import {
    DEFAULT_PAGE_VARIANT_IS_TEMPLATE_PROP,
    DEFAULT_PAGE_VARIANT_TEMPLATE_REF_PROP,
    DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION,
    DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION_PROP,
} from '../types/custom-definitions';
import { PageVariantConfig } from '../types/page-variant-config';
import {
    isDefaultGlobalTemplate,
    markForCopy,
    prependWorkspacePrefix,
    retrieveNodeId,
    retrievePageVariantConfig,
} from '../utils/template-util';
import { TopicPageHelperService } from './topic-page-helper.service';
import { TopicPageVariantNodesService } from './topic-page-variant-nodes.service';

/**
 * Copies the widget nodes of a variant config to a target node and persists the relinked config
 * on it, as the editor does when creating a page variant or template.
 */
export type PersistRelinkedVariantConfig = (
    variantConfig: PageVariantConfig,
    targetNode: Node,
) => Promise<Node>;

/**
 * The global topic page templates folder (`-topic_page_templates-`): access to it, recognizing
 * its templates and publishing a page variant into it.
 */
@Injectable()
export class TopicPageGlobalTemplatesService {
    private nodeHelperService = inject(NodeHelperService);
    private topicPageHelperService = inject(TopicPageHelperService);
    private topicPageVariantNodesService = inject(TopicPageVariantNodesService);

    private readonly folderId: WritableSignal<string | null> = signal(null);
    private readonly canAddValue: WritableSignal<boolean> = signal(false);
    // whether the current user may add templates to the folder
    readonly canAdd: Signal<boolean> = this.canAddValue.asReadonly();

    /**
     * Reads the folder and whether the current user may add templates to it.
     */
    async updatePermission(): Promise<void> {
        try {
            const folderNode: Node = await this.topicPageHelperService.getNode(
                ApiRestConstants.TOPIC_PAGE_TEMPLATES,
            );
            this.folderId.set(retrieveNodeId(folderNode));
            this.canAddValue.set(
                this.nodeHelperService.getNodesRight(
                    [folderNode],
                    ApiRestConstants.ACCESS_ADD_CHILDREN,
                ),
            );
        } catch {
            // folder missing or no access
            this.folderId.set(null);
            this.canAddValue.set(false);
        }
    }

    /**
     * Returns the given template if it is one of the folder's own, other than the
     * repository-maintained default template. Tracks the folder signal when called in a computed.
     */
    asLinkedGlobalTemplate(templateNode: Node | null): Node | null {
        const folderId: string = this.folderId();
        const isGlobal: boolean = !!folderId && templateNode?.parent?.id === folderId;
        return isGlobal && !isDefaultGlobalTemplate(templateNode) ? templateNode : null;
    }

    /**
     * Creates a self-contained global template from a page variant: a full copy with relinked
     * widget nodes, as a root template referencing itself. Editorial members (breadcrumb, topic
     * header) and custom colors other than those of the swimlanes are not taken over.
     */
    async createFrom(
        sourceNode: Node,
        changesMap: Map<string, string | string[]> | undefined,
        persistRelinkedVariantConfig: PersistRelinkedVariantConfig,
    ): Promise<Node> {
        // the title is taken over unchanged: a copy suffix only obscures which variant a global
        // template originated from (and stacks up on every republish)
        const properties: { [key: string]: string | string[] } =
            await this.topicPageHelperService.retrievePageVariantProperties(sourceNode);
        // the title just persisted is the one the user entered, whatever the source node holds
        const changedTitle: string | string[] = changesMap?.get(RestConstants.LOM_PROP_TITLE);
        if (changedTitle) {
            properties[RestConstants.LOM_PROP_TITLE] = Array.isArray(changedTitle)
                ? changedTitle[0]
                : changedTitle;
        }
        // a root template with an initial version; its template ref is set once its ID is known
        properties[DEFAULT_PAGE_VARIANT_IS_TEMPLATE_PROP] = 'true';
        properties[DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION_PROP] =
            DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION;
        delete properties[DEFAULT_PAGE_VARIANT_TEMPLATE_REF_PROP];
        let templateNode: Node = await this.topicPageVariantNodesService.createVariantNode(
            ApiRestConstants.TOPIC_PAGE_TEMPLATES,
            sourceNode,
            properties,
        );
        templateNode = await this.topicPageHelperService.setPropertyAndRetrieveUpdatedNode(
            retrieveNodeId(templateNode),
            DEFAULT_PAGE_VARIANT_TEMPLATE_REF_PROP,
            prependWorkspacePrefix(retrieveNodeId(templateNode)),
        );
        // the parameter values just persisted, written apart from the creation like the profiling
        templateNode =
            (await this.topicPageVariantNodesService.applyPersistedParameters(
                retrieveNodeId(templateNode),
                changesMap,
            )) ?? templateNode;
        const variantConfig: PageVariantConfig = retrievePageVariantConfig(sourceNode);
        markForCopy(variantConfig);
        delete variantConfig.structure.breadcrumbNodeId;
        delete variantConfig.structure.headerNodeId;
        delete variantConfig.structure.propagatedBreadcrumbNodeId;
        delete variantConfig.structure.propagatedHeaderNodeId;
        delete variantConfig.structure.anchorItemColor;
        delete variantConfig.structure.topicColor;
        await persistRelinkedVariantConfig(variantConfig, templateNode);
        return templateNode;
    }
}
