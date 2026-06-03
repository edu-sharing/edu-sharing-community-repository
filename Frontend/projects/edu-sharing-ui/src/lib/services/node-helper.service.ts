import { Injectable, Optional } from '@angular/core';
import {
    ApiHelpersService,
    Assignment,
    AssignmentFile,
    CollectionReference,
    ConfigService,
    NetworkService,
    Node,
    ProposalNode,
    RestConstants,
    ROOT,
    User,
} from 'ngx-edu-sharing-api';
import { TranslateService } from '@ngx-translate/core';
import * as Workflow from '../types/workflow';
import { RepoUrlService } from './repo-url.service';
import { Params, Router } from '@angular/router';
import { UIConstants } from '../util/ui-constants';
import { of } from 'rxjs';
import { NodesRightMode } from '../types/option-item';
import { PlatformLocation } from '@angular/common';
import { EduSharingUiConfiguration } from '../edu-sharing-ui-configuration';
import { Sort } from '@angular/material/sort';
import { NodeEntriesDataType } from '../node-entries/data-type';
import { Toast } from './abstract/toast.service';
import { AssignmentPipe } from '../pipes/assignment.pipe';
import { NodeClickEvent } from '../node-entries/entries-model';

@Injectable({
    providedIn: 'root',
})
export class NodeHelperService {
    readonly LICENSE_URLS = {
        CC_BY_ABOUT: 'https://creativecommons.org/licenses/list.{{language}}',
        CC_BY: 'https://creativecommons.org/licenses/by/{{version}}/{{locale}}deed.{{language}}',
        CC_BY_ND:
            'https://creativecommons.org/licenses/by-nd/{{version}}/{{locale}}deed.{{language}}',
        CC_BY_SA:
            'https://creativecommons.org/licenses/by-sa/{{version}}/{{locale}}deed.{{language}}',
        CC_BY_NC:
            'https://creativecommons.org/licenses/by-nc/{{version}}/{{locale}}deed.{{language}}',
        CC_BY_NC_ND:
            'https://creativecommons.org/licenses/by-nc-nd/{{version}}/{{locale}}deed.{{language}}',
        CC_BY_NC_SA:
            'https://creativecommons.org/licenses/by-nc-sa/{{version}}/{{locale}}deed.{{language}}',
        CC_0: 'https://creativecommons.org/publicdomain/zero/1.0/legalcode.{{language}}',
        PDM: 'https://creativecommons.org/public-domain/pdm/',
    } as { [key: string]: string };
    constructor(
        protected translate: TranslateService,
        protected apiHelpersService: ApiHelpersService,
        protected networkService: NetworkService,
        protected configService: ConfigService,
        protected configuration: EduSharingUiConfiguration,
        protected repoUrlService: RepoUrlService,
        protected platformLocation: PlatformLocation,
        protected toast: Toast,
        @Optional() protected router: Router,
    ) {}

    /**
     * Navigates to the primary action URL for the node carried in the click event.
     * Ctrl/Meta-click and middle-click open in a new browser tab instead.
     */
    navigateToNode(clickEvent: NodeClickEvent<NodeEntriesDataType>): void {
        const node = clickEvent.element as Node | Assignment;
        if (clickEvent.ctrlKey) {
            window.open(this.getNodeLink('plain', node) as string, '_blank');
        } else {
            const routerLink = this.getNodeLink('routerLink', node) as string;
            const queryParams = this.getNodeLink('queryParams', node) as Params;
            void this.router?.navigate([routerLink], { queryParams });
        }
    }

    /**
     * Returns true if a single click on the given node should immediately trigger its primary
     * action instead of opening a selection/sidebar.
     * Currently applies to assignments where the current user is not a coordinator.
     */
    directActionOnSingleClick(node: Node | Assignment): boolean {
        if (this.isNodeAssignment(node)) {
            return (
                new AssignmentPipe().transform(node as Assignment, { mode: 'permissions' }) !==
                'COORDINATOR'
            );
        }
        return false;
    }

    public getCollectionScopeInfo(node: Node): { icon: string; scopeName: string } {
        const scope = node.collection ? node.collection.scope : null;
        let icon = 'help';
        let scopeName = 'UNKNOWN';
        if (scope === RestConstants.COLLECTIONSCOPE_MY) {
            icon = 'lock';
            scopeName = 'MY';
        }
        if (
            scope === RestConstants.COLLECTIONSCOPE_ORGA ||
            scope === RestConstants.COLLECTIONSCOPE_CUSTOM
        ) {
            icon = 'group';
            scopeName = 'SHARED';
        }
        if (
            scope === RestConstants.COLLECTIONSCOPE_ALL ||
            scope === RestConstants.COLLECTIONSCOPE_CUSTOM_PUBLIC
        ) {
            icon = 'language';
            scopeName = 'PUBLIC';
        }
        if (node.collection?.type === RestConstants.COLLECTIONTYPE_EDITORIAL) {
            icon = 'star';
            scopeName = 'TYPE_EDITORIAL';
        }
        if (node.collection?.type === RestConstants.COLLECTIONTYPE_MEDIA_CENTER) {
            icon = 'business';
            scopeName = 'TYPE_MEDIA_CENTER';
        }
        return { icon, scopeName };
    }
    /**
     * Return the license icon of a node
     */
    async getLicenseIcon(node: Node): Promise<string> {
        // prefer manual mapping instead of backend data to support custom states from local edits
        const license = node.properties?.[RestConstants.CCM_PROP_LICENSE]?.[0];
        if (license) {
            return this.getLicenseIconByString(license);
        }
        return node.license ? this.repoUrlService.getRepoUrl(node.license.icon, node) : null;
    }

    /**
     * Get a license icon by using the property value string
     */
    public getLicenseIconByString(string: String, useNoneAsFallback = true): string {
        let icon = string.replace(/_/g, '-').toLowerCase();
        if (icon == '') icon = 'none';

        const LICENSE_ICONS = [
            'cc-0',
            'cc-by-nc',
            'cc-by-nc-nd',
            'cc-by-nc-sa',
            'cc-by-nd',
            'cc-by-sa',
            'cc-by',
            'copyright-free',
            'copyright-license',
            'custom',
            'edu-nc-nd-noDo',
            'edu-nc-nd',
            'edu-p-nr-nd-noDo',
            'edu-p-nr-nd',
            'none',
            'pdm',
            'schulfunk',
            'unterrichts-und-lehrmedien',
        ];
        if (LICENSE_ICONS.indexOf(icon) == -1 && !useNoneAsFallback) return null; // icon='none';
        if (icon == 'none' && !useNoneAsFallback) return null;
        const result =
            this.apiHelpersService.getServerUrl() + '/../ccimages/licenses/' + icon + '.svg';
        return result;
    }
    /**
     * Return a translated name of a license name for a node
     * @param node
     * @param translate
     * @returns {string|any|string|any|string|any|string|any|string|any|string}
     */
    public getLicenseName(node: Node) {
        let prop = node.properties[RestConstants.CCM_PROP_LICENSE]?.[0];
        if (!prop) prop = '';
        return this.getLicenseNameByString(prop);
    }

    /**
     * Return a translated name for a license string
     * @param string
     * @param translate
     * @returns {any}
     */
    public getLicenseNameByString(name: string) {
        if (name == '') {
            name = 'NONE';
        }
        return this.translate.instant('LICENSE.NAMES.' + name);
        // return name.replace(/_/g,"-");
    }

    /**
     * return the License URL (e.g. for CC_BY licenses) for a license string and version
     * @param licenseProperty
     * @param licenseVersion
     * @param licenseLocale
     */
    public getLicenseUrlByString(
        licenseProperty: string,
        licenseVersion: string,
        licenseLocale: string,
    ) {
        const isV4 = licenseVersion === '4.0';
        const locale = isV4 || !licenseLocale ? '' : licenseLocale.toLowerCase() + '/';
        const url = this.LICENSE_URLS[licenseProperty];
        if (!url) {
            return of(null);
        }
        return of(
            url
                .replace('{{version}}', licenseVersion)
                .replace('{{locale}}', locale)
                // use base language for de- special variants
                .replace('{{language}}', this.translate.currentLang?.split('-')?.[0] || 'en'),
        );
    }

    public getWorkflowStatusById(id: string) {
        const workflows = this.getWorkflows();
        return workflows.filter((w) => w.id === id)?.[0];
    }
    public getWorkflowStatus(node: Node, useFromConfig = false): Workflow.WorkflowDefinitionStatus {
        let value = node.properties[RestConstants.CCM_PROP_WF_STATUS]?.[0];
        if (!value) {
            return this.getDefaultWorkflowStatus(useFromConfig);
        }
        return {
            current: this.getWorkflowStatusById(value),
            initial: this.getWorkflowStatusById(value),
        };
    }
    getDefaultWorkflowStatus(useFromConfig = false): Workflow.WorkflowDefinitionStatus {
        const result = {
            current: null as Workflow.WorkflowDefinition,
            initial: null as Workflow.WorkflowDefinition,
        };
        result.initial = this.getWorkflows()[0];
        let defaultStatus: string = null;
        if (useFromConfig) {
            defaultStatus = this.configService.instant('workflow.defaultStatus');
        }
        if (defaultStatus) {
            result.current = this.getWorkflows().find((w) => w.id === defaultStatus);
        } else {
            result.current = result.initial;
        }
        return result;
    }
    getWorkflows(): Workflow.WorkflowDefinition[] {
        return this.configService.instant('workflow.workflows', [
            Workflow.WORKFLOW_STATUS_UNCHECKED,
            Workflow.WORKFLOW_STATUS_TO_CHECK,
            Workflow.WORKFLOW_STATUS_HASFLAWS,
            Workflow.WORKFLOW_STATUS_CHECKED,
        ]);
    }

    getFilenameWithoutExtension(filename: string) {
        if (filename === null) {
            return null;
        }
        const components = filename.split('.');
        if (components.length === 1) {
            return filename;
        }
        components.splice(components.length - 1, 1);
        return components.join('.');
    }
    copyDataToNode<T extends Node | User>(target: T, source: T) {
        target.properties = source.properties;
        (target as Node).name = (source as Node).name;
        (target as Node).title = (source as Node).title;
        (target as Node).icon = (source as Node).icon;
        (target as Node).preview = (source as Node).preview;
        (target as User).authorityName = (source as User).authorityName;
        (target as User).profile = (source as User).profile;
        (target as User).status = (source as User).status;
    }
    isNodeCollection(node: Node): boolean {
        return node.aspects?.includes(RestConstants.CCM_ASPECT_COLLECTION) || !!node.collection;
    }
    isNodeAssignment(node: NodeEntriesDataType): boolean {
        return (node as Assignment).allowAdditionalDocumentSubmissions !== undefined;
    }
    public getSourceIconPath(src: string) {
        return (
            (this.configuration.assetsBasePath ?? '') +
            'assets/images/sources/' +
            src.toLowerCase() +
            '.png'
        );
    }

    getNodeLink(
        mode: 'routerLink' | 'queryParams' | 'plain',
        node: Node | Assignment,
        short = false,
    ) {
        if (!node?.ref) {
            return null;
        }
        let data: { routerLink: string; queryParams: Params } = null;
        if (this.isNodeAssignment(node)) {
            let mainComponent = 'submitAssignment';
            if (
                new AssignmentPipe().transform(node as Assignment, {
                    mode: 'permissions',
                }) === 'COORDINATOR'
            ) {
                if ((node as Assignment).status === 'DRAFT') {
                    mainComponent = 'manageAssignment';
                } else {
                    mainComponent = 'assignmentSubmission';
                }
            }
            data = {
                routerLink: UIConstants.ROUTER_PREFIX + 'editorial/assignment',
                queryParams: {
                    mainComponent,
                    assignment: node.ref.id,
                },
            };
        } else if (this.isNodeCollection(node as Node)) {
            const scope = (node as Node).collection?.scope;
            const type = (node as Node).collection?.type;
            const queryParams: Params = {
                id: node.ref.id,
            };
            if (type === RestConstants.COLLECTIONTYPE_EDITORIAL) {
                queryParams.scope = RestConstants.COLLECTIONSCOPE_TYPE_EDITORIAL;
            } else if (type === RestConstants.COLLECTIONTYPE_MEDIA_CENTER) {
                queryParams.scope = RestConstants.COLLECTIONSCOPE_TYPE_MEDIA_CENTER;
            } else if (scope === RestConstants.COLLECTIONSCOPE_CUSTOM_PUBLIC) {
                queryParams.scope = RestConstants.COLLECTIONSCOPE_ALL;
            }
            data = {
                routerLink: UIConstants.ROUTER_PREFIX + 'collections',
                queryParams,
            };
        } else {
            if ((node as Node).isDirectory) {
                let path;
                if (
                    (node as Node).properties?.[RestConstants.CCM_PROP_EDUSCOPENAME]?.[0] ===
                    RestConstants.SAFE_SCOPE
                ) {
                    path = UIConstants.ROUTER_PREFIX + 'workspace/safe';
                } else {
                    path = UIConstants.ROUTER_PREFIX + 'workspace';
                }
                data = {
                    routerLink: path,
                    queryParams: { id: node.ref.id },
                };
            } else if (node.ref) {
                const fromHome = node.ref.isHomeRepo || false; //this.networkService.isFromHomeRepository(node);
                data = {
                    routerLink: short
                        ? UIConstants.ROUTER_PREFIX_SHORT + node.ref.id
                        : UIConstants.ROUTER_PREFIX + 'render/' + node.ref.id,
                    queryParams: {
                        repository: fromHome ? null : node.ref.repo,
                        proposal: (node as ProposalNode).proposal?.ref.id,
                        proposalCollection: (node as ProposalNode).proposalCollection?.ref.id,
                    },
                };
            }
        }
        if (data === null) {
            return '';
        }
        if (mode === 'routerLink') {
            return '/' + data.routerLink;
        } else if (mode === 'plain') {
            const urlTree = this.router?.createUrlTree([data.routerLink], data);

            return (
                (this.platformLocation.getBaseHrefFromDOM() ?? '') +
                this.router?.serializeUrl(urlTree).substring(1)
            );
        }
        // enforce clearing of parameters which should only be consumed once
        data.queryParams.redirectFromSSO = null;
        return data.queryParams;
    }

    /**
     * Returns true if this node is a copy of another node, just used as a publish target.
     */
    isNodePublishedCopy(o: Node): boolean {
        return !!o.properties?.[RestConstants.CCM_PROP_PUBLISHED_ORIGINAL]?.[0];
    }

    /**
     * returns true if this is a revoked node
     * (published copy that has been revoked)
     */
    isNodeRevoked(node: Node) {
        return node?.aspects?.includes(RestConstants.CCM_ASPECT_REVOKED);
    }

    /**
     * returns the original node if (collection refs)
     * if the node is not a ref, it will simply return the node id
     */
    public getOriginalId(node: Node) {
        if (node.aspects.includes(RestConstants.CCM_ASPECT_IO_REFERENCE)) {
            return (node as CollectionReference).originalId;
        }
        return node.ref.id;
    }
    /**
     * returns true if all nodes have the requested right
     * mode (only works for collection refs):
     *   Local: check only rights of the node itself
     *   Effective: check only rights of the original node this refers to (collection ref). If it is not a collection ref, fallback to local
     */
    public getNodesRight(
        nodes: (Node | AssignmentFile)[],
        right: string,
        mode = NodesRightMode.Local,
    ) {
        if (nodes == null) return true;
        for (let n of nodes) {
            let node: Node;
            if ((n as AssignmentFile).referNode) {
                node = (n as AssignmentFile).referNode;
            } else {
                node = n as Node;
            }
            let currentMode = mode;
            // if no access effective present and not a collection ref. use the local data
            if (
                !node.aspects?.includes(RestConstants.CCM_ASPECT_IO_REFERENCE) &&
                !node.accessEffective?.length
            ) {
                currentMode = NodesRightMode.Local;
            }
            if (currentMode === NodesRightMode.Effective) {
                if (!node.aspects?.includes(RestConstants.CCM_ASPECT_IO_REFERENCE)) {
                    if (node.accessEffective && node.accessEffective.indexOf(right) !== -1) {
                        continue;
                    }
                }
                if (node.accessEffective && node.accessEffective.indexOf(right) !== -1) {
                    continue;
                }
                if (RestConstants.IMPLICIT_COLLECTION_PERMISSIONS.indexOf(right) === -1) {
                    // permission not matched on original -> implicit permissions from collection may apply
                    return false;
                }
            }
            // check regular node rights
            if (!node.access || node.access.indexOf(right) === -1) {
                return false;
            }
        }
        return true;
    }
    isOerLicense(value: string) {
        return ['CC_0', 'PDM', 'CC_BY', 'CC_BY_SA'].includes(value);
    }

    /**
     * get the required sort by fields to be used when querying subcollections for a given collection
     */
    getSortByForCollection(collection: Node | typeof ROOT): Sort {
        if (collection === ROOT) {
            return {
                active: RestConstants.CM_MODIFIED_DATE,
                direction: 'desc',
            };
        } else {
            const orderCollections =
                collection?.properties?.[
                    RestConstants.CCM_PROP_COLLECTION_SUBCOLLECTION_ORDER_MODE
                ];
            return {
                active: orderCollections?.[0] || RestConstants.CM_MODIFIED_DATE,
                direction:
                    orderCollections?.[0] === RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION
                        ? 'asc'
                        : orderCollections?.[1] === 'true'
                        ? 'asc'
                        : 'desc',
            };
        }
    }

    /**
     * get the required sort by fields to be used when querying references for a given collection
     */
    getSortByForCollectionReferences(collection: Node): Sort {
        const refMode = collection.collection.orderMode;
        const refAscending = collection.collection.orderAscending;
        return {
            active: ((refMode === RestConstants.COLLECTION_ORDER_MODE_CUSTOM
                ? RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION
                : refMode) || RestConstants.CM_MODIFIED_DATE) as any,
            direction: refAscending ? 'asc' : 'desc',
        };
    }

    public handleNodeError(name: string, error: any): number {
        if (error.status === RestConstants.DUPLICATE_NODE_RESPONSE) {
            this.toast.error(null, 'WORKSPACE.TOAST.DUPLICATE_NAME', {
                name,
            });
            return error.status;
        } else if (
            error.error?.message?.includes(
                'org.alfresco.service.cmr.repository.CyclicChildRelationshipException',
            )
        ) {
            this.toast.error(null, 'WORKSPACE.TOAST.CYCLIC_NODE', {
                name,
            });
            return error.status;
        }
        this.toast.error(error);
        return error.status;
    }

    static getActionbarNodes<T>(listNodes: T[], externalNode: T | T[]): T[] {
        return externalNode
            ? Array.isArray(externalNode)
                ? externalNode
                : [externalNode]
            : listNodes && listNodes.length
            ? listNodes
            : null;
    }
}
