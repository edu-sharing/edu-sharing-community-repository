import { NodeHelperService } from '../../core-ui-module/node-helper.service';
import { Injectable } from '@angular/core';
import {
    Node,
    NodesRightMode,
    Repository,
    RestConnectorService,
    RestConnectorsService,
    RestConstants,
    RestNetworkService,
} from '../../core-module/core.module';
import { OptionItem } from '../../core-ui-module/option-item';
import { MessageType } from '../../core-module/ui/message-type';

@Injectable()
export class ActionbarHelperService {
    public static getNodes(nodes: Node[], node: Node): Node[] {
        return NodeHelperService.getActionbarNodes(nodes, node);
    }
    constructor(
        private connector: RestConnectorService,
        private networkService: RestNetworkService,
        private connectors: RestConnectorsService,
        private nodeHelper: NodeHelperService,
    ) {}

    /**
     * Add a given option for a specified type and checks the rights if possible
     * returns the option if it could be created, null otherwise
     * @param {string} type
     * @param {Node[]} nodes
     * @param {Function} callback
     * @returns {any}
     */
    async createOptionIfPossible(
        type: string,
        nodes: Node[],
        callback: (node: Node | any) => void,
    ) {
        const repositories = (await this.networkService.getRepositories().toPromise()).repositories;
        let option: OptionItem = null;
        if (type == 'DOWNLOAD') {
            if (this.nodeHelper.allFiles(nodes)) {
                option = new OptionItem('OPTIONS.DOWNLOAD', 'cloud_download', callback);
                option.customEnabledCallback = (list: Node[] | any[]) => {
                    if (!list || !list.length) return false;
                    let isAllowed = false;
                    for (let item of list) {
                        if (item.reference) item = item.reference;
                        // if at least one is allowed -> allow download (download servlet will later filter invalid files)
                        isAllowed =
                            isAllowed ||
                            (nodes &&
                                item.downloadUrl != null &&
                                item.properties &&
                                !item.properties[RestConstants.CCM_PROP_IO_WWWURL]);
                    }
                    console.log(list, isAllowed);
                    return isAllowed;
                };
                option.showCallback = (node: Node) => {
                    return this.nodeHelper.referenceOriginalExists(node);
                };
                // option.isEnabled=option.customEnabledCallback(null);
            }
        }
        if (type == 'ADD_NODE_STORE') {
            option = new OptionItem('SEARCH.ADD_NODE_STORE', 'bookmark_border', callback);
            option.showCallback = (node: Node) => {
                let n = ActionbarHelperService.getNodes(nodes, node);
                if (n == null) return false;
                return n.length && RestNetworkService.allFromHomeRepo(n, repositories);
            };
        }
        if (type == 'NODE_TEMPLATE') {
            if (nodes && nodes.length == 1 && this.nodeHelper.allFolders(nodes)) {
                option = new OptionItem(
                    'WORKSPACE.OPTION.TEMPLATE',
                    'assignment_turned_in',
                    callback,
                );
                option.isEnabled = this.nodeHelper.getNodesRight(nodes, RestConstants.ACCESS_WRITE);
                option.onlyDesktop = true;
            }
        }
        if (type == 'CREATE_VARIANT') {
            option = new OptionItem('WORKSPACE.OPTION.VARIANT', 'call_split', callback);
            option.showCallback = (node: Node) => {
                let n = ActionbarHelperService.getNodes(nodes, node);
                if (n == null) return false;
                option.name =
                    'WORKSPACE.OPTION.VARIANT' +
                    (this.connectors.connectorSupportsEdit(n[0]) ? '_OPEN' : '');
                return (
                    this.nodeHelper.allFiles(n) &&
                    n &&
                    n.length == 1 &&
                    n[0].aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) == -1 &&
                    RestNetworkService.allFromHomeRepo(n) &&
                    !this.connector.getCurrentLogin().isGuest
                );
            };
            option.enabledCallback = (node: Node) => {
                return parseInt(node.size) > 0 && node.downloadUrl !== null;
            };
        }
        if (type === 'QR_CODE') {
            option = new OptionItem('WORKSPACE.OPTION.QR_CODE', 'edu-qr_code', callback);
        }
        if (type == 'ADD_TO_COLLECTION') {
            if (this.connector.getCurrentLogin() && !this.connector.getCurrentLogin().isGuest) {
                option = new OptionItem('WORKSPACE.OPTION.COLLECTION', 'layers', callback);
                option.isEnabled = this.nodeHelper.getNodesRight(
                    nodes,
                    RestConstants.ACCESS_CC_PUBLISH,
                    NodesRightMode.Original,
                );
                option.showAsAction = true;
                option.showCallback = (node: Node) => {
                    let n = ActionbarHelperService.getNodes(nodes, node);
                    if (n == null) return false;
                    return (
                        this.nodeHelper.referenceOriginalExists(node) &&
                        this.nodeHelper.allFiles(nodes) &&
                        n.length > 0
                    );
                };
                option.enabledCallback = (node: Node) => {
                    let list = ActionbarHelperService.getNodes(nodes, node);
                    return this.nodeHelper.getNodesRight(
                        list,
                        RestConstants.ACCESS_CC_PUBLISH,
                        NodesRightMode.Original,
                    );
                };
                option.disabledCallback = () => {
                    this.connectors
                        .getRestConnector()
                        .getBridgeService()
                        .showTemporaryMessage(
                            MessageType.error,
                            null,
                            'WORKSPACE.TOAST.ADD_TO_COLLECTION_DISABLED',
                        );
                };
            }
        }
        if (type == 'DELETE') {
            if (nodes && nodes.length) {
                option = new OptionItem('WORKSPACE.OPTION.DELETE', 'delete', callback);
                option.isEnabled = this.nodeHelper.getNodesRight(
                    nodes,
                    RestConstants.ACCESS_DELETE,
                );
                option.isSeparate = true;
            }
        }
        if (type == 'ADD_TO_STREAM') {
            if (
                this.nodeHelper.allFiles(nodes) &&
                this.connector.getConfigurationService().instant('stream.enabled', false)
            ) {
                option = new OptionItem('WORKSPACE.OPTION.STREAM', 'event', callback);
                option.enabledCallback = (node: Node) => {
                    let n = ActionbarHelperService.getNodes(nodes, node);
                    if (n == null) return false;
                    return (
                        this.nodeHelper.getNodesRight(n, RestConstants.ACCESS_CC_PUBLISH) &&
                        this.connectors
                            .getRestConnector()
                            .hasToolPermissionInstant(RestConstants.TOOLPERMISSION_INVITE_STREAM) &&
                        RestNetworkService.allFromHomeRepo(n, repositories)
                    );
                };
                option.showCallback = (node: Node) => {
                    let n = ActionbarHelperService.getNodes(nodes, node);
                    if (n == null) return false;
                    return (
                        this.nodeHelper.allFiles(nodes) &&
                        RestNetworkService.allFromHomeRepo(n, repositories) &&
                        n.length == 1
                    );
                };
                option.isEnabled = option.enabledCallback(null);
            }
        }
        if (type == 'INVITE') {
            if (nodes && nodes[0].aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) == -1) {
                option = new OptionItem('WORKSPACE.OPTION.INVITE', 'group_add', callback);
                option.isSeparate = this.nodeHelper.allFiles(nodes);
                option.showAsAction = true;
                option.isEnabled = this.nodeHelper.getNodesRight(
                    nodes,
                    RestConstants.ACCESS_CHANGE_PERMISSIONS,
                );
            }
        }
        if (type == 'WORKFLOW') {
            if (
                nodes &&
                nodes.length == 1 &&
                !nodes[0].isDirectory &&
                nodes[0].type != RestConstants.CCM_TYPE_SAVED_SEARCH &&
                nodes[0].aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) == -1
            ) {
                option = new OptionItem('WORKSPACE.OPTION.WORKFLOW', 'swap_calls', callback);
                option.isEnabled = this.nodeHelper.getNodesRight(
                    nodes,
                    RestConstants.ACCESS_CHANGE_PERMISSIONS,
                );
            }
        }
        if (type == 'SHARE_LINK') {
            if (
                nodes &&
                !nodes[0].isDirectory &&
                nodes[0].type != RestConstants.CCM_TYPE_SAVED_SEARCH
            ) {
                option = new OptionItem('WORKSPACE.OPTION.SHARE_LINK', 'link', callback);
                option.isEnabled =
                    this.nodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CHANGE_PERMISSIONS) &&
                    this.connectors
                        .getRestConnector()
                        .hasToolPermissionInstant(RestConstants.TOOLPERMISSION_INVITE);
            }
        }
        // when there are already nodes (action bar), and the option has a show callback, check if it is valid
        if (option && nodes && nodes.length && option.showCallback && !option.showCallback(null)) {
            return null;
        }
        return option;
    }
}
