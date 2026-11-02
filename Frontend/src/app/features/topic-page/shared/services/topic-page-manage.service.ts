import { inject, Injectable } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { DEFAULT_PAGE_VARIANT_TEMPLATE_REF_PROP } from '../types/custom-definitions';
import { PageConfig } from '../types/page-config';
import {
    convertNodeRefIntoNodeId,
    isTemplateUpdateAvailable,
    retrieveNodeId,
    retrievePageConfig,
    retrievePageConfigPropagateRef,
    retrievePageConfigRef,
} from '../utils/template-util';
import { checkUserAccess } from '../utils/node-util';
import { TopicPageGlobalTemplatesService } from './topic-page-global-templates.service';
import { TopicPageHelperService } from './topic-page-helper.service';

/**
 * `variant`: a page of the collection itself, `template`: a template the collection passes on to
 * its sub-collections, `inherited`: a page the collection shows from an ancestor's templates.
 */
export type TopicPageEntryKind = 'variant' | 'template' | 'inherited';

export type TemplateStatus = 'none' | 'upToDate' | 'updateAvailable' | 'templateMissing';

/**
 * Where the template of an entry can be edited by the user: as a global template, or in the
 * template mode of the collection whose template it is.
 */
export type TemplateEditTarget = 'global' | 'collection' | null;

export type TopicPageEntry = {
    kind: TopicPageEntryKind;
    node: Node;
    /** the page config node listing the entry among its variants */
    configNode: Node;
    isDefault: boolean;
    /** the template the entry was built from, if it is still available */
    templateNode: Node | null;
    templateStatus: TemplateStatus;
    templateEditTarget: TemplateEditTarget;
};

export type TopicPageEntries = {
    variants: TopicPageEntry[];
    templates: TopicPageEntry[];
    inherited: TopicPageEntry[];
    /** the ancestor whose templates the inherited pages are */
    inheritedFrom: Node | null;
};

/**
 * Reads the pages and templates of a collection as the topic page's sidebar lists them.
 */
@Injectable()
export class TopicPageManageService {
    private readonly topicPageHelperService = inject(TopicPageHelperService);
    private readonly globalTemplatesService = inject(TopicPageGlobalTemplatesService);

    async load(collectionId: string, includeTemplates: boolean): Promise<TopicPageEntries> {
        const collection: Node = await this.topicPageHelperService.getNodeUncached(collectionId);
        const ownConfigRef: string | undefined = retrievePageConfigRef(collection);
        const propagateRef: string | undefined = retrievePageConfigPropagateRef(collection);
        // the templates of the collection are only edited by who may see them here
        const ownTemplatesConfigId: string | null =
            includeTemplates && propagateRef ? convertNodeRefIntoNodeId(propagateRef) : null;
        const entries: TopicPageEntries = {
            variants: [],
            templates: [],
            inherited: [],
            inheritedFrom: null,
        };
        if (ownConfigRef) {
            entries.variants = await this.loadConfigEntries(
                ownConfigRef,
                'variant',
                ownTemplatesConfigId,
            );
        } else {
            // a collection without a page of its own shows the templates of the nearest ancestor
            const parents: Node[] =
                (await this.topicPageHelperService.getNodeParents(collectionId)).nodes ?? [];
            entries.inheritedFrom =
                parents.find(
                    (parent: Node) =>
                        retrieveNodeId(parent) !== collectionId &&
                        !!retrievePageConfigPropagateRef(parent),
                ) ?? null;
            if (entries.inheritedFrom) {
                entries.inherited = await this.loadConfigEntries(
                    retrievePageConfigPropagateRef(entries.inheritedFrom),
                    'inherited',
                    ownTemplatesConfigId,
                );
            }
        }
        if (includeTemplates && propagateRef) {
            entries.templates = await this.loadConfigEntries(
                propagateRef,
                'template',
                ownTemplatesConfigId,
            );
        }
        return entries;
    }

    /**
     * The variants of a page config in the order the config lists them; an unavailable config
     * has none.
     */
    private async loadConfigEntries(
        configRef: string,
        kind: TopicPageEntryKind,
        ownTemplatesConfigId: string | null,
    ): Promise<TopicPageEntry[]> {
        const configNode: Node | null = await this.topicPageHelperService.getNodeIfAvailable(
            convertNodeRefIntoNodeId(configRef),
            true,
        );
        if (!configNode) {
            return [];
        }
        const pageConfig: PageConfig = retrievePageConfig(configNode);
        const variantIds: string[] = (pageConfig.variants ?? []).map(convertNodeRefIntoNodeId);
        const defaultId: string | null = pageConfig.default
            ? convertNodeRefIntoNodeId(pageConfig.default)
            : null;
        const children: Node[] =
            (await this.topicPageHelperService.getNodeChildren(retrieveNodeId(configNode))).nodes ??
            [];
        const variants: Node[] = variantIds
            .map((id: string) => children.find((child: Node) => retrieveNodeId(child) === id))
            .filter((node: Node | undefined): node is Node => !!node);
        return Promise.all(
            variants.map(
                async (node: Node): Promise<TopicPageEntry> => ({
                    kind,
                    node,
                    configNode,
                    isDefault: retrieveNodeId(node) === defaultId,
                    ...(await this.resolveTemplate(node, ownTemplatesConfigId)),
                }),
            ),
        );
    }

    private async resolveTemplate(
        node: Node,
        ownTemplatesConfigId: string | null,
    ): Promise<Pick<TopicPageEntry, 'templateNode' | 'templateStatus' | 'templateEditTarget'>> {
        const templateRef: string | undefined =
            node.properties?.[DEFAULT_PAGE_VARIANT_TEMPLATE_REF_PROP]?.[0];
        if (!templateRef) {
            return { templateNode: null, templateStatus: 'none', templateEditTarget: null };
        }
        const templateNode: Node | null = await this.topicPageHelperService.getNodeIfAvailable(
            convertNodeRefIntoNodeId(templateRef),
            true,
        );
        if (!templateNode) {
            return {
                templateNode: null,
                templateStatus: 'templateMissing',
                templateEditTarget: null,
            };
        }
        return {
            templateNode,
            templateStatus: isTemplateUpdateAvailable(templateNode, node)
                ? 'updateAvailable'
                : 'upToDate',
            templateEditTarget: this.templateEditTarget(templateNode, ownTemplatesConfigId),
        };
    }

    private templateEditTarget(
        templateNode: Node,
        ownTemplatesConfigId: string | null,
    ): TemplateEditTarget {
        if (!checkUserAccess(templateNode)) {
            return null;
        }
        if (this.globalTemplatesService.asLinkedGlobalTemplate(templateNode)) {
            return 'global';
        }
        if (ownTemplatesConfigId && templateNode.parent?.id === ownTemplatesConfigId) {
            return 'collection';
        }
        return null;
    }
}
