import { PlatformLocation } from '@angular/common';
import { Injectable } from '@angular/core';
import { NavigationExtras, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import {
    CollectionEntries,
    CollectionService,
    HOME_REPOSITORY,
    Node,
    NodeEntries,
    NodeService,
    NodeServiceUnwrapped,
    ParentEntries,
    PROPERTY_FILTER_ALL,
} from 'ngx-edu-sharing-api';
import { OptionGroup, OptionItem, UIConstants } from 'ngx-edu-sharing-ui';
import { BehaviorSubject, firstValueFrom, Observable } from 'rxjs';
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import { UIHelper } from '../../../../core-ui-module/ui-helper';
import { DialogsService } from '../../../../features/dialogs/dialogs.service';
import { Toast, ToastType } from '../../../../services/toast';
import { GenericWidgetGlobalService } from '../../widgets/generic-widget/generic-widget-global.service';
import {
    DEFAULT_AI_CONFIG_PROP,
    DEFAULT_PAGE_VARIANT_CONFIG_PROP,
    DEFAULT_PAGE_VARIANT_IS_TEMPLATE_PROP,
    DEFAULT_PAGE_VARIANT_TEMPLATE_REF_PROP,
    DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION_PROP,
    DEFAULT_WIDGET_CONFIG_PROP,
} from '../types/custom-definitions';
import { BapiConfigObject } from '../types/bapi-config-object';
import { GridTile } from '../types/grid-tile';
import { PageVariantConfig } from '../types/page-variant-config';
import { Swimlane } from '../types/swimlane';
import { WidgetConfig } from '../types/widget-config/widget-config';
import { WidgetNodeAddedEvent } from '../types/widget-node-added-event';
import {
    convertNodeRefIntoNodeId,
    prependWorkspacePrefix,
    retrieveNodeId,
    retrievePageVariantTemplateRef,
    retrievePageVariantTemplateVersion,
} from '../utils/template-util';
import { TopicPageEventsService } from './topic-page-events.service';
import { TopicPageGlobalService } from './topic-page-global.service';

@Injectable({
    providedIn: 'root',
})
export class TopicPageHelperService {
    blobToUpload: Blob;
    private selectedVariablesSubject: BehaviorSubject<{ [key: string]: string[] }> =
        new BehaviorSubject<{ [key: string]: string[] }>({});
    private readonly shareOptionsI18nPrefix: string = 'TOPIC_PAGE.WIDGET.SHARE_OPTIONS.';

    constructor(
        private collectionApi: CollectionService,
        private dialogs: DialogsService,
        private genericWidgetGlobalService: GenericWidgetGlobalService,
        private nodeApi: NodeService,
        private nodeApiUnwrapped: NodeServiceUnwrapped,
        private platformLocation: PlatformLocation,
        private router: Router,
        private toast: Toast,
        private topicPageEventsService: TopicPageEventsService,
        private topicPageGlobalService: TopicPageGlobalService,
        private translate: TranslateService,
    ) {}

    /**
     * Retrieves the base href of the application.
     *
     * @param cutOffTrailingSlash
     */
    getBaseHref(cutOffTrailingSlash: boolean = true): string {
        let href: string = this.platformLocation.getBaseHrefFromDOM();
        // cut off trailing slash
        if (href?.endsWith('/') && cutOffTrailingSlash) {
            href = href.slice(0, -1);
        }
        return href;
    }

    /**
     * Opens a new tab with the Re-URL parameter set.
     */
    openReurlLink(extras: NavigationExtras = {}): Window | null {
        if (!extras.queryParams) {
            extras.queryParams = {};
        }
        if (this.topicPageGlobalService.getCustomReurlExtras()) {
            extras = this.mergeNavigationExtras(
                this.topicPageGlobalService.getCustomReurlExtras() || {},
                extras,
            );
        }
        extras.queryParams.reurl = 'WINDOW';
        // special case for filters: use stringify
        if (extras.queryParams.filters) {
            extras.queryParams.filters = JSON.stringify(extras.queryParams.filters);
        }
        const reurlComponent: string =
            this.topicPageGlobalService.getCustomReurlComponent() || 'search';
        const link: string =
            this.getBaseHref() +
            this.router
                .createUrlTree([UIConstants.ROUTER_PREFIX + reurlComponent], extras)
                .toString();
        return window.open(link, '_blank');
    }

    /**
     * Opens a new tab with the applyFilter parameter set.
     */
    openApplyFilterLink(extras: NavigationExtras = {}): Window | null {
        if (!extras.queryParams) {
            extras.queryParams = {};
        }
        if (this.topicPageGlobalService.getCustomApplyFilterExtras()) {
            extras = this.mergeNavigationExtras(
                this.topicPageGlobalService.getCustomApplyFilterExtras() || {},
                extras,
            );
        }
        extras.queryParams.action = 'applyFilter';
        // special case for filters: use stringify
        if (extras.queryParams.filters) {
            extras.queryParams.filters = JSON.stringify(extras.queryParams.filters);
        }
        const applyFilterComponent: string =
            this.topicPageGlobalService.getCustomApplyFilterComponent() || 'search';
        const link: string =
            this.getBaseHref() +
            this.router
                .createUrlTree([UIConstants.ROUTER_PREFIX + applyFilterComponent], extras)
                .toString();
        return window.open(link, '_blank');
    }

    /**
     * Opens the link to a node for inspection.
     *
     * @param node
     */
    openChangeOnInspectionTableLink(node: Node): void {
        const component = this.topicPageGlobalService.getCustomApplyFilterComponent() || 'render';
        const urlBase = this.buildInspectionTableUrlBase(component, node);
        const extras = this.topicPageGlobalService.getCustomInspectionTableExtras() || {};
        if (this.topicPageGlobalService.getCustomInspectionTableNodeIdQueryParam()) {
            extras.queryParams[
                this.topicPageGlobalService.getCustomInspectionTableNodeIdQueryParam()
            ] = retrieveNodeId(node);
        }
        const link = this.getBaseHref() + this.router.createUrlTree(urlBase, extras).toString();

        window.open(link, '_blank');
    }

    /**
     * Helper function to build the inspection table URL base.
     *
     * @param component
     * @param node
     */
    private buildInspectionTableUrlBase(component: string, node: Node): string[] {
        if (component !== 'render') {
            return [UIConstants.ROUTER_PREFIX + component];
        }

        const urlBase = [UIConstants.ROUTER_PREFIX + component, retrieveNodeId(node)];

        if (node.content.version) {
            urlBase.push(node.content.version);
        }

        return urlBase;
    }

    /**
     * Helper function to merge navigation extras with special consideration for filters.
     *
     * @param obj1
     * @param obj2
     */
    private mergeNavigationExtras(
        obj1: NavigationExtras,
        obj2: NavigationExtras,
    ): NavigationExtras {
        return {
            queryParams: {
                ...obj1.queryParams,
                ...obj2.queryParams,
                filters: {
                    ...obj1.queryParams?.filters,
                    ...obj2.queryParams?.filters,
                },
            },
        };
    }

    /**
     * Sets the selected variables for the topic page.
     *
     * @param variables
     */
    setSelectedVariables(variables: { [property: string]: string[] }): void {
        this.selectedVariablesSubject.next(variables);
    }

    /**
     * Returns the selected variables of the topic page.
     */
    getSelectedVariables(): { [property: string]: string[] } {
        return this.selectedVariablesSubject.value;
    }

    /**
     * Retrieves the selected variables variables to observe changes.
     */
    getSelectedVariables$(): Observable<{ [property: string]: string[] }> {
        return this.selectedVariablesSubject.asObservable();
    }

    /**
     * Sets an input blob as the next upload.
     *
     * @param blob
     */
    setBlobToUpload(blob: Blob): void {
        this.blobToUpload = blob;
    }

    /**
     * Returns the blob for the next upload
     */
    getBlobToUpload(): Blob {
        return this.blobToUpload;
    }

    // NODE HELPER FUNCTIONS
    /**
     * Retrieves a node with a given ID.
     */
    async getNode(nodeId: string): Promise<Node> {
        nodeId = convertNodeRefIntoNodeId(nodeId);
        return firstValueFrom(this.nodeApi.getNode(nodeId));
    }

    /**
     * Retrieves the parents of a node with a given ID.
     */
    async getNodeParents(nodeId: string): Promise<ParentEntries> {
        return firstValueFrom(
            this.nodeApi.getParents(nodeId, {
                propertyFilter: [PROPERTY_FILTER_ALL],
                fullPath: false,
            }),
        );
    }

    /**
     * Retrieves the children of a node with a given ID.
     */
    async getNodeChildren(nodeId: string): Promise<NodeEntries> {
        // TODO: Pagination vs. large maxItems number
        return firstValueFrom(this.nodeApi.getChildren(nodeId, { maxItems: 500 }));
    }

    /**
     * Retrieves the sub-collections of a collection with a given ID.
     */
    async getSubCollections(collectionId: string): Promise<Node[]> {
        return firstValueFrom(this.collectionApi.getSubCollections(collectionId));
    }

    /**
     * Retrieves the sub-collections with counts of a collection with a given ID.
     *
     * @param collectionId
     * @param fetchCounts
     */
    getSubcollections(
        collectionId: string,
        fetchCounts: boolean = false,
    ): Observable<CollectionEntries> {
        return this.collectionApi.getSubcollections({
            repository: HOME_REPOSITORY,
            collection: collectionId,
            scope: RestConstants.COLLECTIONSCOPE_ALL as
                | 'EDU_ALL'
                | 'EDU_GROUPS'
                | 'TYPE_EDITORIAL'
                | 'TYPE_MEDIA_CENTER'
                | 'MY'
                | 'RECENT',
            fetchCounts,
            maxItems: 500,
        });
    }

    /**
     * Creates a child for an existing node.
     */
    async createChild(
        parentId: string,
        type: string,
        name: string,
        aspect?: string,
        properties?: { [key: string]: string | string[] },
    ): Promise<Node> {
        parentId = convertNodeRefIntoNodeId(parentId);
        const request: any = {
            repository: HOME_REPOSITORY,
            node: parentId,
            type,
            body: {
                [RestConstants.CM_NAME]: [name],
                [RestConstants.CM_PROP_METADATASET_EDU_METADATASET]: [
                    this.genericWidgetGlobalService.getDefaultMds(),
                ],
            },
        };
        if (aspect) {
            request.aspects = [aspect];
        }
        const createWithoutProperties: boolean = !properties || !Object.keys(properties)?.length;
        if (createWithoutProperties) {
            return await firstValueFrom(this.nodeApi.createChild(request));
        } else {
            // important: set obeyMds to false to allow setting properties during creation
            request.obeyMds = false;
            // enrich the request body with the input parameters
            Object.entries(properties).forEach(([key, value]) => {
                request.body[key] = Array.isArray(value) ? value : [value];
            });
            return (await firstValueFrom(this.nodeApiUnwrapped.createChild(request))).node;
        }
    }

    /**
     * Sets a property to an existing node.
     */
    async setProperty(nodeId: string, property: string, value: string | string[]): Promise<Node> {
        nodeId = convertNodeRefIntoNodeId(nodeId);
        const convertedValue: string[] = Array.isArray(value) ? value : [value];
        // workaround to remove temporary properties from the page variant config
        if (property === DEFAULT_PAGE_VARIANT_CONFIG_PROP) {
            convertedValue[0] = this.cleanPageVariantConfig(convertedValue[0]);
        }
        return firstValueFrom(
            this.nodeApi.setProperty(HOME_REPOSITORY, nodeId, property, convertedValue),
        );
    }

    /**
     * Changes the content of a given node ID with a given mime type and blob.
     */
    changeContent(nodeId: string, mimeType: string, blob: Blob): Observable<Node> {
        nodeId = convertNodeRefIntoNodeId(nodeId);
        return this.nodeApi.changeContent(HOME_REPOSITORY, nodeId, mimeType, null, {
            file: blob,
        });
    }

    /**
     * Copies a node as a child of a given parent node.
     */
    async copyNodeAsChild(sourceNodeId: string, parentNodeId: string): Promise<Node> {
        sourceNodeId = convertNodeRefIntoNodeId(sourceNodeId);
        parentNodeId = convertNodeRefIntoNodeId(parentNodeId);
        return await firstValueFrom(
            this.nodeApi.createChildByCopying({
                repository: HOME_REPOSITORY,
                node: parentNodeId,
                source: sourceNodeId,
                withChildren: false,
            }),
        );
    }

    /**
     * Deletes a node with a given ID.
     */
    async deleteNode(nodeId: string): Promise<void> {
        nodeId = convertNodeRefIntoNodeId(nodeId);
        return firstValueFrom(this.nodeApi.deleteNode(nodeId));
    }

    // COMBINED NODE FUNCTIONS
    /**
     * Resets a given property of a node with a given ID.
     */
    async resetProperty(nodeId: string, propertyName: string): Promise<void> {
        return firstValueFrom(this.nodeApi.setProperty(HOME_REPOSITORY, nodeId, propertyName));
    }

    /**
     * Sets a property to an existing node and retrieves the updated node.
     */
    async setPropertyAndRetrieveUpdatedNode(
        nodeId: string,
        propertyName: string,
        value: string | string[],
    ): Promise<Node> {
        nodeId = convertNodeRefIntoNodeId(nodeId);
        await this.setProperty(nodeId, propertyName, value);
        return this.getNode(nodeId);
    }

    // INTERNAL HELPER FUNCTIONS
    /**
     * Helper function to clean temporary properties from page variant config
     */
    private cleanPageVariantConfig(value: string): string {
        const blacklistedProperties: string[] = ['hasHits', 'searchCount', 'statistics'];
        const parsedValue: PageVariantConfig = JSON.parse(value);
        // workaround to avoid keeping legacy properties
        const legacyProperties: string[] = ['template', 'variables'];
        legacyProperties.forEach((prop: string) => delete (parsedValue as any)[prop]);
        parsedValue.structure?.swimlanes?.forEach((swimlane: Swimlane): void => {
            swimlane.grid?.forEach((gridItem: GridTile): void => {
                // @ts-ignore
                blacklistedProperties.forEach((prop) => delete gridItem[prop]);
                // special case for propagatedNodeId
                if (gridItem.nodeId && gridItem.propagatedNodeId) {
                    delete gridItem.propagatedNodeId;
                }
            });
        });
        return JSON.stringify(parsedValue);
    }

    /**
     * Persists a selected color string by notifying the topic-page about the change.
     *
     * @param color
     * @param pageVariantNode
     * @param swimlaneIndex
     */
    persistColorChange(color: string, pageVariantNode: Node, swimlaneIndex: number): boolean {
        this.openSaveConfigToast();
        try {
            const allInputsExist: boolean = swimlaneIndex > -1 && !!pageVariantNode;
            if (allInputsExist) {
                // notify the topic-page about the color change
                this.topicPageEventsService.swimlaneColorChanged.emit({
                    color,
                    pageVariantNode,
                    swimlaneIndex,
                });
                return true;
            }
            return false;
        } catch (err) {
            this.displayErrorToast(err);
            return false;
        }
    }

    /**
     * Persists a given widgetConfig and aiConfig by either creating a new or updating an existing node with the config.
     *
     * @param nodeId
     * @param gridIndex
     * @param swimlaneIndex
     * @param pageVariantNode
     * @param widgetConfig
     * @param aiConfig
     * @param parentWidgetConfigNodeId
     * @param isHeaderNode
     * @param isBreadcrumbNode
     */
    async persistConfig(
        nodeId: string,
        gridIndex: number,
        swimlaneIndex: number,
        pageVariantNode: Node,
        widgetConfig: WidgetConfig,
        aiConfig: BapiConfigObject,
        parentWidgetConfigNodeId: string,
        isBreadcrumbNode: boolean = false,
        isHeaderNode: boolean = false,
    ): Promise<Node> {
        this.openSaveConfigToast();
        const configNodeExists: boolean = nodeId && nodeId !== '';
        try {
            // the page variant node might be the one that got propagated from the parent
            if (!configNodeExists) {
                // check whether all inputs exist, which are required to notify the clients
                const validParentVariant: boolean = !!pageVariantNode;
                const validSwimlaneIndex: boolean = swimlaneIndex > -1;
                const validGridIndex: boolean = gridIndex > -1;
                const validInputs: boolean = validGridIndex && validSwimlaneIndex;

                if (!validParentVariant || (!validInputs && !isBreadcrumbNode && !isHeaderNode)) {
                    console.error(
                        'Missing pageVariantNode or incomplete input and not breadcrumb or header node',
                        pageVariantNode,
                        swimlaneIndex,
                        gridIndex,
                        isBreadcrumbNode,
                        isHeaderNode,
                    );
                    return null;
                }
                // create a new node with the given widget and AI configs
                const event: WidgetNodeAddedEvent = {
                    gridIndex,
                    isBreadcrumbNode,
                    isHeaderNode,
                    pageVariantNode,
                    swimlaneIndex,
                    widget: {
                        widgetConfig,
                        aiConfig,
                    },
                };
                this.topicPageEventsService.widgetNodeAdded.emit(event);
                return null;
            } else {
                // update the config(s) and return the updated node
                await this.setProperty(
                    nodeId,
                    DEFAULT_WIDGET_CONFIG_PROP,
                    JSON.stringify(widgetConfig),
                );
                // either update or reset the AI config
                if (aiConfig && Object.keys(aiConfig)?.length) {
                    await this.setProperty(
                        nodeId,
                        DEFAULT_AI_CONFIG_PROP,
                        JSON.stringify(aiConfig),
                    );
                } else {
                    await this.resetProperty(nodeId, DEFAULT_AI_CONFIG_PROP);
                }
                // return the updated node
                return await this.getNode(nodeId);
            }
        } catch (err) {
            this.displayErrorToast(err);
            return null;
        }
    }

    /**
     * Uploads a file by creating a child node for a given parent node ID and setting the file data.
     *
     * @param parentId
     * @param name
     * @param mimeType
     * @param blob
     */
    async uploadFile(parentId: string, name: string, mimeType: string, blob: Blob): Promise<Node> {
        const fileNode: Node = await this.createChild(parentId, RestConstants.CCM_TYPE_IO, name);
        return await firstValueFrom(this.changeContent(fileNode.ref.id, mimeType, blob));
    }

    /**
     * Retrieves the page variant properties for a given node, title suffix and page config.
     *
     * @param node
     * @param customTitleSuffix
     * @param variantConfig
     */
    async retrievePageVariantProperties(
        node: Node,
        customTitleSuffix: string = '',
        variantConfig?: PageVariantConfig,
    ) {
        const variantTemplateVersion: string = retrievePageVariantTemplateVersion(node);
        // the new node is based on `node`: if `node` is itself a template, that template
        // is the new node's foundation; otherwise the new node inherits `node`'s own
        // foundation (a sibling copy shares the same template ref). Without this, a
        // variant created from a non-root template would point at the template's parent
        // instead of the template it was actually built on.
        const isTemplateSource: boolean =
            node.properties?.[DEFAULT_PAGE_VARIANT_IS_TEMPLATE_PROP]?.[0] === 'true';
        const templateRef: string = isTemplateSource
            ? prependWorkspacePrefix(retrieveNodeId(node))
            : retrievePageVariantTemplateRef(node);
        const properties: { [p: string]: string | string[] } = {
            [DEFAULT_PAGE_VARIANT_IS_TEMPLATE_PROP]: 'false',
            [DEFAULT_PAGE_VARIANT_TEMPLATE_REF_PROP]: templateRef,
            [DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION_PROP]: variantTemplateVersion,
        };
        // if a variant config is set, copy it as well
        if (variantConfig) {
            properties[DEFAULT_PAGE_VARIANT_CONFIG_PROP] = JSON.stringify(variantConfig);
        }
        // if a title is set, copy it as well, otherwise set a default
        if (
            node.properties[RestConstants.LOM_PROP_TITLE]?.length &&
            node.properties[RestConstants.LOM_PROP_TITLE][0]
        ) {
            properties[RestConstants.LOM_PROP_TITLE] =
                node.properties[RestConstants.LOM_PROP_TITLE][0] +
                (customTitleSuffix ? customTitleSuffix : '');
        } else {
            properties[RestConstants.LOM_PROP_TITLE] =
                this.translate.instant('TOPIC_PAGE.DEFAULT_PAGE_VARIANT_NAME') +
                (customTitleSuffix ? customTitleSuffix : '');
        }
        return properties;
    }

    /**
     * Define a list of custom options.
     */
    retrieveCustomOption(
        type: 'qrCode' | 'copyLink' | 'writeMail' | 'reportProblem',
        group: OptionGroup = null,
        priority?: number,
        customLabel?: string,
        customIcon?: string,
    ): OptionItem {
        let option: OptionItem;
        switch (type) {
            case 'qrCode':
                option = new OptionItem(
                    'OPTIONS.QR_CODE',
                    'edu-qr_code',
                    async (node: Node, nodes?: any[]): Promise<void> => {
                        const selectedNode: Node | null = nodes?.[0] ?? node;
                        void this.dialogs.openQrDialog({ node: selectedNode });
                    },
                );
                break;
            case 'copyLink':
                option = new OptionItem(
                    this.shareOptionsI18nPrefix + 'COPY_LINK',
                    'content_copy',
                    async (node: Node, nodes?: any[]): Promise<void> => {
                        const selectedNode: Node | null = nodes?.[0] ?? node;
                        this.copyLink(selectedNode);
                    },
                );
                break;
            case 'writeMail':
                option = new OptionItem(
                    this.shareOptionsI18nPrefix + 'WRITE_MAIL.HEADING',
                    'mail',
                    async (node: Node, nodes?: any[]): Promise<void> => {
                        const selectedNode: Node | null = nodes?.[0] ?? node;
                        this.writeMail(selectedNode);
                    },
                );
                break;
            case 'reportProblem':
                option = new OptionItem(
                    'OPTIONS.NODE_REPORT',
                    'flag',
                    async (node: Node, nodes?: any[]): Promise<void> => {
                        const selectedNode: Node | null = nodes?.[0] ?? node;
                        void this.dialogs.openNodeReportDialog({
                            node: selectedNode,
                            mode: 'NODE_REPORT',
                            showOptions: true,
                        });
                    },
                );
                break;
            default:
                return null;
        }
        option.enabledCallback = async (): Promise<boolean> => {
            return Promise.resolve(true);
        };
        if (group) {
            option.group = group;
        }
        if (priority !== undefined) {
            option.priority = priority;
        }
        if (customLabel) {
            option.name = customLabel;
        }
        if (customIcon) {
            option.icon = customIcon;
        }
        return option;
    }

    // TOASTS
    /**
     * Opens a toast for indicating that the config is being saved.
     */
    openSaveConfigToast(message?: string): void {
        const toastMessage: string = message ? message : 'TOPIC_PAGE.SAVE_CONFIG_MESSAGE';
        this.toast.show({
            message: toastMessage,
            type: 'info',
            subtype: ToastType.InfoSimple,
        });
    }

    /**
     * Handles an error by opening a toast container for the error.
     *
     * @param error
     */
    displayErrorToast(error: any = null): void {
        this.toast.error(error, 'TOPIC_PAGE.WIDGET.SAVE_CONFIG_ERROR_MESSAGE', null, null, null, {
            link: {
                caption: 'TOPIC_PAGE.WIDGET.SAVE_CONFIG_ERROR_ACTION',
                callback: (): void => {
                    window.location.reload();
                },
            },
        });
    }

    // HELPER FUNCTIONS
    /**
     * Helper function to copy the link to a given node into the clipboard.
     */
    private copyLink(node: Node): void {
        try {
            UIHelper.copyToClipboard(node.content.url);
            this.toast.toast('WORKSPACE.SHARE_LINK.COPIED_CLIPBOARD');
        } catch (e) {
            this.toast.error(null, 'WORKSPACE.SHARE_LINK.COPIED_CLIPBOARD_ERROR');
        }
    }

    /**
     * Helper function to write an email to share a given node.
     *
     * @param node
     */
    private writeMail(node: Node): void {
        if (node.content?.url) {
            const subjectText = this.translate.instant(
                this.shareOptionsI18nPrefix + 'WRITE_MAIL.SUBJECT_SHARED_CONTENT',
                {
                    title:
                        node.title ||
                        this.translate.instant(this.shareOptionsI18nPrefix + 'WRITE_MAIL.NO_TITLE'),
                },
            );
            const bodyText = this.translate.instant(
                this.shareOptionsI18nPrefix + 'WRITE_MAIL.BODY_SHARED_CONTENT',
                {
                    url: node.content.url,
                },
            );

            const subject = encodeURIComponent(subjectText);
            const body = encodeURIComponent(bodyText);

            const mailtoLink = `mailto:?subject=${subject}&body=${body}`;
            window.open(mailtoLink, '_self');
        } else {
            console.warn(
                this.translate.instant(this.shareOptionsI18nPrefix + 'WRITE_MAIL.ERROR_NO_URL'),
                node,
            );
        }
    }
}
