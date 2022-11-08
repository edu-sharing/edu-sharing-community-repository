import { TranslateService } from '@ngx-translate/core';
import { Observable, Observer } from 'rxjs';
import { Params, Router } from '@angular/router';
import { Location } from '@angular/common';
import { DefaultGroups, OptionGroup, OptionItem } from './option-item';
import { UIConstants } from '../core-module/ui/ui-constants';
import { Helper } from '../core-module/rest/helper';
import { HttpClient } from '@angular/common/http';
import { MessageType } from '../core-module/ui/message-type';
import { Toast } from './toast';
import { ComponentFactoryResolver, Injectable, ViewContainerRef } from '@angular/core';
import { BridgeService } from '../core-bridge-module/bridge.service';
import {
    AuthorityProfile,
    CollectionReference,
    NodesRightMode,
    Node,
    Permission,
    User,
    WorkflowDefinition,
    Repository,
    ProposalNode,
    DeepLinkResponse,
} from '../core-module/rest/data-object';
import { TemporaryStorageService } from '../core-module/rest/services/temporary-storage.service';
import { RestConstants } from '../core-module/rest/rest-constants';
import { ConfigurationService } from '../core-module/rest/services/configuration.service';
import { RestHelper } from '../core-module/rest/rest-helper';
import { RestConnectorService } from '../core-module/rest/services/rest-connector.service';
import { ListItem } from '../core-module/ui/list-item';
import { RestNetworkService } from '../core-module/rest/services/rest-network.service';
import { NodePersonNamePipe } from '../shared/pipes/node-person-name.pipe';
import { UniversalNode } from '../common/definitions';
import { FormBuilder } from '@angular/forms';
import { SessionStorageService } from '../core-module/rest/services/session-storage.service';
import { map } from 'rxjs/operators';
import { RestNodeService } from '../core-module/rest/services/rest-node.service';
import { getRepoUrl } from '../util/repo-url';

export type WorkflowDefinitionStatus = {
    current: WorkflowDefinition;
    initial: WorkflowDefinition;
};
export interface ConfigEntry {
    name: string;
    icon: string;
    scope?: string;
    isDisabled?: boolean;
    isSeparate?: boolean;
    isCustom: boolean;
    position?: number;
    url?: string;
    open?: () => void;
}

export interface ConfigOptionItem extends ConfigEntry {
    mode: string;
    scopes: string[];
    ajax: boolean;
    permission: string;
    group: string;
    toolpermission: string;
    isDirectory: string;
    showAsAction: boolean;
    multiple: boolean;
    changeStrategy: string;
}

@Injectable()
export class NodeHelperService {
    private viewContainerRef: ViewContainerRef;
    constructor(
        private translate: TranslateService,
        private componentFactoryResolver: ComponentFactoryResolver,
        private config: ConfigurationService,
        private rest: RestConnectorService,
        private bridge: BridgeService,
        private http: HttpClient,
        private connector: RestConnectorService,
        private nodeService: RestNodeService,
        private toast: Toast,
        private router: Router,
        private sessionStorage: SessionStorageService,
        private storage: TemporaryStorageService,
        private location: Location,
        private formBuilder: FormBuilder,
    ) {}
    setViewContainerRef(viewContainerRef: ViewContainerRef) {
        this.viewContainerRef = viewContainerRef;
    }
    /**
     * returns true if all nodes have the requested right
     * mode (only works for collection refs):
     *   Local: check only rights of the node itself
     Original: check only rights of the original node this refers to (collection ref). If it is not a collection ref, fallback to local
     Both: check both rights of node + original combined via or
     *
     */
    public getNodesRight(nodes: any[], right: string, mode = NodesRightMode.Local) {
        if (nodes == null) return true;
        for (const node of nodes) {
            let currentMode = mode;
            // if this is not a collection ref -> force local mode
            if (
                node.aspects &&
                node.aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) === -1
            ) {
                currentMode = NodesRightMode.Local;
            }
            if (currentMode === NodesRightMode.Original || currentMode === NodesRightMode.Both) {
                if (node.accessOriginal && node.accessOriginal.indexOf(right) !== -1) {
                    continue;
                }
                // return false because either on original not found, or both (because both is then also false)
                if (currentMode === NodesRightMode.Both) {
                    return false;
                } else if (RestConstants.IMPLICIT_COLLECTION_PERMISSIONS.indexOf(right) === -1) {
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
    public handleNodeError(name: string, error: any): number {
        if (error.status === RestConstants.DUPLICATE_NODE_RESPONSE) {
            this.bridge.showTemporaryMessage(MessageType.error, 'WORKSPACE.TOAST.DUPLICATE_NAME', {
                name,
            });
            return error.status;
        } else if (error._body) {
            try {
                const json = JSON.parse(error._body);
                if (
                    json.message.startsWith(
                        'org.alfresco.service.cmr.repository.CyclicChildRelationshipException',
                    )
                ) {
                    this.bridge.showTemporaryMessage(
                        MessageType.error,
                        'WORKSPACE.TOAST.CYCLIC_NODE',
                        { name },
                    );
                    return error.status;
                }
            } catch (e) {}
        }
        this.bridge.showTemporaryMessage(MessageType.error, null, null, null, error);
        return error.status;
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

    public downloadUrl(url: string, fileName = 'download') {
        if (this.bridge.isRunningCordova()) {
            this.bridge.showTemporaryMessage(MessageType.info, 'TOAST.DOWNLOAD_STARTED', {
                name: fileName,
            });
            this.bridge.getCordova().downloadContent(
                url,
                fileName,
                (deviceFileName: string) => {
                    if (this.bridge.getCordova().isAndroid()) {
                        this.bridge.showTemporaryMessage(
                            MessageType.info,
                            'TOAST.DOWNLOAD_FINISHED_ANDROID',
                            { name: fileName },
                        );
                    } else {
                        this.bridge.showTemporaryMessage(
                            MessageType.info,
                            'TOAST.DOWNLOAD_FINISHED_IOS',
                            { name: fileName },
                        );
                    }
                },
                () => {
                    this.bridge.showTemporaryMessage(
                        MessageType.error,
                        'TOAST.DOWNLOAD_FAILED',
                        { name: fileName },
                        {
                            link: {
                                caption: 'TOAST.DOWNLOAD_TRY_AGAIN',
                                callback: () => {
                                    this.downloadUrl(url, fileName);
                                },
                            },
                        },
                    );
                },
            );
        } else {
            window.open(url);
        }
    }
    /**
     * Download (a single) node
     */
    public downloadNode(node: any, version = RestConstants.NODE_VERSION_CURRENT, metadata = false) {
        this.downloadUrl(
            getRepoUrl(node.downloadUrl, node) +
                (version && version != RestConstants.NODE_VERSION_CURRENT
                    ? '&version=' + version
                    : '') +
                '&metadata=' +
                metadata,
            node.name + (metadata ? '.txt' : ''),
        );
    }

    /**
     * fetches the preview of the node and appends it at preview.data
     */
    public appendImageData(node: Node, quality = 70): Observable<Node> {
        return new Observable<Node>((observer: Observer<Node>) => {
            const options: any = this.rest.getRequestOptions();
            options.responseType = 'blob';
            const url = getRepoUrl(node.preview.url, node);
            this.rest
                .get(url + '&allowRedirect=false&quality=' + quality, options, false)
                .subscribe(
                    async (data: Blob) => {
                        const reader = new FileReader();
                        reader.onload = () => {
                            const dataUrl = reader.result;
                            node.preview.data = [dataUrl.toString()];
                            observer.next(node);
                            observer.complete();
                        };
                        reader.readAsDataURL(data);
                    },
                    (error) => {
                        observer.error(error);
                        observer.complete();
                    },
                );
        });
    }

    /**
     * Return the license icon of a node
     * @param node
     * @returns {string}
     */
    public getLicenseIcon(node: Node) {
        return node.license ? getRepoUrl(node.license.icon, node) : null;
    }

    /**
     * Get a license icon by using the property value string
     * @param string
     * @param rest
     * @returns {string}
     */
    public getLicenseIconByString(string: String, useNoneAsFallback = true) {
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
        return this.rest.getAbsoluteEndpointUrl() + '../ccimages/licenses/' + icon + '.svg';
    }
    /**
     * Return a translated name of a license name for a node
     * @param node
     * @param translate
     * @returns {string|any|string|any|string|any|string|any|string|any|string}
     */
    public getLicenseName(node: Node) {
        let prop = node.properties[RestConstants.CCM_PROP_LICENSE];
        if (prop) prop = prop[0];
        else prop = '';
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
     */
    public getLicenseUrlByString(licenseProperty: string, licenseVersion: string) {
        const url = (RestConstants.LICENSE_URLS as any)[licenseProperty];
        if (!url) return null;
        return url.replace('#version', licenseVersion);
    }

    /**
     * Get a user name for displaying
     * @param user
     * @returns {string}
     */
    public getUserDisplayName(user: AuthorityProfile | User) {
        return (user.profile.firstName + ' ' + user.profile.lastName).trim();
    }
    isSavedSearchObject(node: Node) {
        return node.mediatype == 'saved_search';
    }

    /**
     * Add custom options to the node menu (loaded via config)
     */
    public applyCustomNodeOptions(
        custom: ConfigOptionItem[],
        allNodes: Node[],
        selectedNodes: Node[],
        options: OptionItem[],
        replaceUrl: any = {},
    ) {
        if (custom) {
            for (const c of custom) {
                let item: OptionItem;
                if (c.changeStrategy === 'remove') {
                    const i = Helper.indexOfObjectArray(options, 'name', c.name);
                    if (i != -1) options.splice(i, 1);
                    continue;
                } else if (c.changeStrategy === 'update') {
                    item = options.find((o) => o.name === c.name);
                    if (!item) {
                        continue;
                    }
                } else {
                    let callback = (node: Node) => {
                        const nodes = node == null ? selectedNodes : [node];
                        let ids = '';
                        if (nodes) {
                            for (const node of nodes) {
                                if (ids) ids += ',';
                                ids += node.ref.id;
                            }
                        }
                        let url = c.url.replace(':id', ids);
                        url = url.replace(':api', this.connector.getAbsoluteEndpointUrl());
                        if (selectedNodes.length === 1) {
                            url = url.replace(
                                ':node',
                                encodeURIComponent(JSON.stringify(nodes[0])),
                            );
                        }
                        if (replaceUrl) {
                            for (const key in replaceUrl) {
                                url = url.replace(key, encodeURIComponent(replaceUrl[key]));
                            }
                        }
                        if (!c.ajax) {
                            window.open(url);
                            return;
                        }
                        this.toast.showProgressDialog();
                        this.http.get(url).subscribe(
                            (data: any) => {
                                if (data.success)
                                    this.toast.error(
                                        data.success,
                                        null,
                                        data.message ? data.success : data.message,
                                        data.message,
                                    );
                                else if (data.error)
                                    this.toast.error(
                                        null,
                                        data.error,
                                        null,
                                        data.message ? data.error : data.message,
                                        data.message,
                                    );
                                else this.toast.error(null);
                                this.toast.closeModalDialog();
                            },
                            (error: any) => {
                                this.toast.error(error);
                                this.toast.closeModalDialog();
                            },
                        );
                    };
                    item = new OptionItem(c.name, c.icon, callback);
                }
                if (c.group) {
                    item.group = (DefaultGroups as any)[
                        Object.keys(DefaultGroups).find(
                            (key) => ((DefaultGroups as any)[key] as OptionGroup).id === c.group,
                        )
                    ];
                }
                if (c.position != null) {
                    let position = c.position;
                    if (c.position < 0) {
                        position =
                            options
                                .filter((o) => o.group.id === item.group.id)
                                .reduce((o1, o2) => (o1.priority > o2.priority ? o1 : o2))
                                .priority - c.position;
                    }
                    item.priority = c.position;
                }
                if (c.icon != null) {
                    item.icon = c.icon;
                }
                item.showAsAction = c.showAsAction;
                item.isSeparate = c.isSeparate;
                item.customEnabledCallback = (nodes) => {
                    if (c.permission) {
                        return this.getNodesRight(nodes, c.permission);
                    }
                    return true;
                };
                item.isEnabled = item.customEnabledCallback(null);
                item.customShowCallback = (nodes) => {
                    if (c.mode == 'nodes' && !nodes?.length) return false;
                    if (c.mode == 'noNodes' && nodes && nodes.length) return false;
                    if (
                        c.mode == 'noNodesNotEmpty' &&
                        ((nodes && nodes.length) || !allNodes || !allNodes.length)
                    )
                        return false;
                    // @ts-ignore
                    if (
                        c.mode == 'nodes' &&
                        c.isDirectory != 'any' &&
                        nodes &&
                        c.isDirectory != nodes[0].isDirectory
                    )
                        return false;
                    if (
                        c.toolpermission &&
                        !this.connector.hasToolPermissionInstant(c.toolpermission)
                    )
                        return false;
                    if (c.multiple != null && !c.multiple && nodes && nodes.length > 1)
                        return false;
                    return true;
                };
                if (c.changeStrategy !== 'update') {
                    options.splice(c.position ?? 0, 0, item);
                }
            }
        }
    }

    /**
     * Apply (redirect url) node for usage by LMS systems
     * @param router
     * @param node
     */
    addNodeToLms(node: Node, reurl: string) {
        this.storage.set(TemporaryStorageService.APPLY_TO_LMS_PARAMETER_NODE, node);
        this.router.navigate(
            [UIConstants.ROUTER_PREFIX + 'apply-to-lms', node.ref.repo, node.ref.id],
            { queryParams: { reurl } },
        );
    }

    addNodesToLTIPlatform(nodes: Node[]) {
        let url = this.connector.createUrl('/lti/v13/generateDeepLinkingResponse', null, []);
        nodes.forEach((n) => {
            if (!url.includes('?')) {
                url += '?nodeIds=' + n.ref.id;
            } else {
                url += '&nodeIds=' + n.ref.id;
            }
        });

        this.connector
            .get<DeepLinkResponse>(url, this.connector.getRequestOptions())
            .subscribe((data: DeepLinkResponse) => {
                this.postLtiDeepLinkResponse(data.jwtDeepLinkResponse, data.ltiDeepLinkReturnUrl);
            });
    }

    private postLtiDeepLinkResponse(jwt: string, url: string) {
        const form = window.document.createElement('form');
        form.setAttribute('method', 'post');
        form.setAttribute('action', url);
        form.appendChild(this.createHiddenElement('JWT', jwt));
        window.document.body.appendChild(form);
        form.submit();
    }

    private createHiddenElement(name: string, value: string): HTMLInputElement {
        const hiddenField = document.createElement('input');
        hiddenField.setAttribute('name', name);
        hiddenField.setAttribute('value', value);
        hiddenField.setAttribute('type', 'hidden');
        return hiddenField;
    }

    /**
     * Download one or multiple nodes
     * @param node
     */
    downloadNodes(nodes: Node[], fileName = 'download.zip') {
        if (nodes.length === 1) return this.downloadNode(nodes[0]);

        const nodesString = RestHelper.getNodeIds(nodes).join(',');
        const url =
            this.connector.getAbsoluteEndpointUrl() +
            '../eduservlet/download?appId=' +
            encodeURIComponent(nodes[0].ref.repo) +
            '&nodeIds=' +
            encodeURIComponent(nodesString) +
            '&fileName=' +
            encodeURIComponent(fileName);
        this.downloadUrl(url, fileName);
    }

    getLRMIProperty(data: any, item: ListItem) {
        // http://dublincore.org/dcx/lrmi-terms/2014-10-24/
        if (item.type == 'NODE') {
            if (item.name == RestConstants.CM_NAME || item.name == RestConstants.CM_PROP_TITLE) {
                return 'name';
            }
            if (item.name == RestConstants.CM_CREATOR) {
                return 'author';
            }
            if (item.name == RestConstants.CM_PROP_C_CREATED) {
                return 'dateCreated';
            }
        }
        return null;
    }
    getLRMIAttribute(data: any, item: ListItem) {
        // http://dublincore.org/dcx/lrmi-terms/2014-10-24/
        if (item.type === 'NODE') {
            if (data.reference) {
                data = data.reference;
            }
            if (item.name === RestConstants.CM_CREATOR) {
                return new NodePersonNamePipe(this.config).transform(data);
            }
            if (item.name === RestConstants.CM_NAME) {
                return data.name;
            }
            if (
                item.name === RestConstants.CM_PROP_TITLE ||
                item.name === RestConstants.LOM_PROP_TITLE
            ) {
                return data.title;
            }
            if (
                item.name === RestConstants.CM_PROP_C_CREATED ||
                item.name === RestConstants.CM_MODIFIED_DATE
            ) {
                return data.properties[item.name + 'ISO8601'];
            }
        }
        return null;
    }

    public getSourceIconRepoPath(repo: Repository) {
        if (repo.icon) return repo.icon;
        if (repo.isHomeRepo) return this.getSourceIconPath('home');
        return this.getSourceIconPath(repo.repositoryType.toLowerCase());
    }
    public getSourceIconPath(src: string) {
        return 'assets/images/sources/' + src.toLowerCase() + '.png';
    }
    public getWorkflowStatusById(id: string): WorkflowDefinition {
        const workflows = this.getWorkflows();
        let pos = Helper.indexOfObjectArray(workflows, 'id', id);
        if (pos == -1) pos = 0;
        const workflow = workflows[pos];
        return workflow;
    }
    public getWorkflowStatus(node: Node, useFromConfig = false): WorkflowDefinitionStatus {
        let value = node.properties[RestConstants.CCM_PROP_WF_STATUS];
        if (value) value = value[0];
        if (!value) {
            return this.getDefaultWorkflowStatus(useFromConfig);
        }
        return {
            current: this.getWorkflowStatusById(value),
            initial: this.getWorkflowStatusById(value),
        };
    }
    getDefaultWorkflowStatus(useFromConfig = false): WorkflowDefinitionStatus {
        const result = {
            current: null as WorkflowDefinition,
            initial: null as WorkflowDefinition,
        };
        result.initial = this.getWorkflows()[0];
        let defaultStatus: string = null;
        if (useFromConfig) {
            defaultStatus = this.config.instant('workflow.defaultStatus');
        }
        if (defaultStatus) {
            result.current = this.getWorkflows().find((w) => w.id === defaultStatus);
        } else {
            result.current = result.initial;
        }
        return result;
    }
    getWorkflows(): WorkflowDefinition[] {
        return this.config.instant('workflow.workflows', [
            RestConstants.WORKFLOW_STATUS_UNCHECKED,
            RestConstants.WORKFLOW_STATUS_TO_CHECK,
            RestConstants.WORKFLOW_STATUS_HASFLAWS,
            RestConstants.WORKFLOW_STATUS_CHECKED,
        ]);
    }

    allFiles(nodes: any[]) {
        let allFiles = true;
        if (nodes) {
            for (let node of nodes) {
                if (!node) continue;
                if (node.reference) node = node.reference;
                if (node.isDirectory || node.type != RestConstants.CCM_TYPE_IO) allFiles = false;
            }
        }
        return allFiles;
    }
    allFolders(nodes: Node[]) {
        let allFolders = true;
        if (nodes) {
            for (const node of nodes) {
                if (!node.isDirectory) allFolders = false;
            }
        }
        return allFolders;
    }
    hasAnimatedPreview(node: Node) {
        return (
            !node.preview.isIcon && (node.mediatype == 'file-video' || node.mimetype == 'image/gif')
        );
    }

    askCCPublish(node: Node) {
        const mail =
            node.createdBy.firstName +
            ' ' +
            node.createdBy.lastName +
            '<' +
            node.createdBy.mailbox +
            '>';
        const subject = this.translate.instant('ASK_CC_PUBLISH_SUBJECT', {
            name: RestHelper.getTitle(node),
        });
        window.location.href = 'mailto:' + mail + '?subject=' + encodeURIComponent(subject);
    }

    /**
     * checks if a doi handle is active (node must be explicitly public and handle it must be present)
     * @param {Node} node
     * @param {Permissions} permissions
     * @returns {boolean}
     */
    isDOIActive(node: Node, permissions: Permission[]) {
        if (
            node.aspects.indexOf(RestConstants.CCM_ASPECT_PUBLISHED) != -1 &&
            node.properties[RestConstants.CCM_PROP_PUBLISHED_HANDLE_ID]
        ) {
            for (const permission of permissions) {
                if (permission.authority.authorityName === RestConstants.AUTHORITY_EVERYONE)
                    return true;
            }
        }
        return false;
    }

    propertiesFromConnector(event: any) {
        const name = event.name + '.' + event.type.filetype;
        const prop = RestHelper.createNameProperty(name);
        prop[RestConstants.LOM_PROP_TECHNICAL_FORMAT] = [event.type.mimetype];
        if (event.type.mimetype == 'application/zip') {
            prop[RestConstants.CCM_PROP_CCRESSOURCETYPE] = [event.type.ccressourcetype];
            prop[RestConstants.CCM_PROP_CCRESSOURCESUBTYPE] = [event.type.ccresourcesubtype];
            prop[RestConstants.CCM_PROP_CCRESSOURCEVERSION] = [event.type.ccressourceversion];
        }
        if (event.type.editorType) {
            prop[RestConstants.CCM_PROP_EDITOR_TYPE] = [event.type.editorType];
        }
        return prop;
    }
    static getActionbarNodes<T>(nodes: T[], node: T): T[] {
        return node ? [node] : nodes && nodes.length ? nodes : null;
    }

    referenceOriginalExists(node: Node | CollectionReference) {
        if (node == null) return true;
        return node.hasOwnProperty('originalId') ? (node as any).originalId != null : true;
    }

    isNodeCollection(node: UniversalNode | any) {
        return (
            (node.aspects && node.aspects.indexOf(RestConstants.CCM_ASPECT_COLLECTION) !== -1) ||
            node.collection
        );
    }

    /**
     * get the value for all nodes, if it is identical. Otherwise, the fallback is returned
     * @param prop
     * @param fallbackNotIdentical Fallback when they're not equaling
     * @param fallbackIsEmpty Fallback when all are empty
     * @param asArray If false, only the first element of the property array will be returned
     */
    getValueForAll(
        nodes: Node[],
        prop: string,
        fallbackNotIdentical: any = '',
        fallbackIsEmpty = fallbackNotIdentical,
        asArray = true,
    ) {
        let found = null;
        let foundAny = false;

        for (let node of nodes) {
            const v = node.properties[prop];
            const value = v ? (asArray ? v : v[0]) : fallbackIsEmpty;
            if (foundAny && found !== value) return fallbackNotIdentical;
            found = value;
            foundAny = true;
        }
        if (!foundAny) return fallbackIsEmpty;
        return found;
    }

    createUrlLink(link: LinkData) {
        const properties: any = {};
        const aspects: string[] = [];
        const url = this.addHttpIfRequired(link.link);
        properties[RestConstants.CCM_PROP_IO_WWWURL] = [url];
        if (link.lti) {
            aspects.push(RestConstants.CCM_ASPECT_TOOL_INSTANCE_LINK);
            properties[RestConstants.CCM_PROP_TOOL_INSTANCE_KEY] = [link.consumerKey];
            properties[RestConstants.CCM_PROP_TOOL_INSTANCE_SECRET] = [link.sharedSecret];
        }
        properties[RestConstants.CCM_PROP_LINKTYPE] = [RestConstants.LINKTYPE_USER_GENERATED];
        return { properties, aspects, url };
    }

    addHttpIfRequired(link: string) {
        if (link.indexOf('://') == -1) {
            return 'http://' + link;
        }
        return link;
    }

    /**
     * Returns true if this node is a copy of another node, just used as a publish target.
     */
    isNodePublishedCopy(o: Node): boolean {
        return !!o.properties?.[RestConstants.CCM_PROP_PUBLISHED_ORIGINAL]?.[0];
    }

    getNodeLink(mode: 'routerLink' | 'queryParams', node: UniversalNode) {
        if (!node?.ref) {
            return null;
        }
        let data: { routerLink: string; queryParams: Params } = null;
        if (this.isNodeCollection(node)) {
            data = {
                routerLink: UIConstants.ROUTER_PREFIX + 'collections',
                queryParams: { id: node.ref.id },
            };
        } else {
            if (node.isDirectory) {
                let path;
                if (
                    node.properties?.[RestConstants.CCM_PROP_EDUSCOPENAME]?.[0] ===
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
                const fromHome = RestNetworkService.isFromHomeRepo(node);
                data = {
                    routerLink: UIConstants.ROUTER_PREFIX + 'render/' + node.ref.id,
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
        }
        // enforce clearing of parameters which should only be consumed once
        data.queryParams.redirectFromSSO = null;
        return data.queryParams;
    }

    /**
     * Returns the full URL to a node, including the server origin and base href.
     */
    getNodeUrl(node: UniversalNode): string {
        const link = this.getNodeLink('queryParams', node);
        if (link) {
            const urlTree = this.router.createUrlTree([this.getNodeLink('routerLink', node)], {
                queryParams: this.getNodeLink('queryParams', node) as Params,
            });
            return location.origin + this.location.prepareExternalUrl(urlTree.toString());
        } else {
            return null;
        }
    }

    copyDataToNode<T extends Node>(target: T, source: T) {
        target.properties = source.properties;
        target.name = source.name;
        target.title = source.title;
    }
    getDefaultInboxFolder() {
        return new Observable<Node>((subscriber) => {
            this.sessionStorage.get('defaultInboxFolder', RestConstants.INBOX).subscribe(
                (id) => {
                    this.nodeService.getNodeMetadata(id).subscribe(
                        (node) => {
                            subscriber.next(node.node);
                            subscriber.complete();
                        },
                        (error) => {
                            console.warn('error resolving defaultInboxFolder', error);
                            return this.nodeService
                                .getNodeMetadata(RestConstants.INBOX)
                                .pipe(map((n) => n.node))
                                .subscribe(subscriber);
                        },
                    );
                },
                (error) => {
                    console.warn('error resolving defaultInboxFolder', error);
                    return this.nodeService
                        .getNodeMetadata(RestConstants.INBOX)
                        .pipe(map((n) => n.node))
                        .subscribe(subscriber);
                },
            );
        });
    }
}

export class LinkData {
    constructor(public link: string) {}
    lti: boolean;
    parent: Node;
    consumerKey: string;
    sharedSecret: string;
}
