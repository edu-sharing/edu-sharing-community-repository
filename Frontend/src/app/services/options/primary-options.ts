import {
    Constrain,
    DefaultGroups,
    ElementType,
    HideMode,
    NodesRightMode,
    OptionItem,
} from 'ngx-edu-sharing-ui';
import { Node } from 'ngx-edu-sharing-api';
import { RestConstants } from '../../core-module/rest/rest-constants';
import { OptionsContext } from './options-context';

export function createPrimaryOptions({
    service,
    management,
    components,
    data,
    queryParams,
}: OptionsContext): OptionItem[] {
    const applyNode = new OptionItem('APPLY', 'redo', (object) =>
        service.nodeHelper.addNodeToLms(service.getObjects(object, data)[0], queryParams.reurl),
    );

    applyNode.elementType = [ElementType.Node, ElementType.NodePublishedCopy];
    applyNode.permissions = [RestConstants.ACCESS_CC_PUBLISH];
    applyNode.permissionsRightMode = NodesRightMode.Effective;
    applyNode.permissionsMode = HideMode.Disable;
    applyNode.constrains = [Constrain.NoBulk, Constrain.ReurlMode, Constrain.User];
    applyNode.showAsAction = true;
    applyNode.showAlways = true;
    applyNode.group = DefaultGroups.Primary;
    applyNode.priority = 10;
    applyNode.customShowCallback = async (nodes) => {
        // collections are only applicable when explicitly requested via applyCollections
        return service.nodeHelper.isNodeCollection(nodes?.[0])
            ? queryParams.applyCollections === 'true'
            : true;
    };
    applyNode.customEnabledCallback = (nodes) => {
        // collections also have isDirectory set, so check them before the directory branch
        if (service.nodeHelper.isNodeCollection(nodes?.[0])) {
            return queryParams.applyCollections === 'true';
        }
        // either apply directories is true or it is an file
        return (
            (nodes?.[0].isDirectory ? queryParams.applyDirectories === 'true' : true) &&
            // and either onlyDownloadable is explicitly required or the node has a download url
            ((queryParams.onlyDownloadable ?? 'false') === 'false' ||
                !!nodes?.[0].downloadUrl ||
                nodes?.[0].isDirectory)
        );
    };

    const acceptProposal = new OptionItem('OPTIONS.COLLECTION_PROPOSAL_ACCEPT', 'check', (object) =>
        management.addProposalsToCollection(service.getObjects(object, data)),
    );
    /*acceptProposal.customEnabledCallback = (nodes) =>
        nodes.every((n) => (n as ProposalNode).accessible);*/
    acceptProposal.elementType = [ElementType.NodeProposal];
    acceptProposal.constrains = [Constrain.User];
    acceptProposal.group = DefaultGroups.Primary;
    acceptProposal.showAsAction = true;
    acceptProposal.priority = 10;

    const declineProposal = new OptionItem(
        'OPTIONS.COLLECTION_PROPOSAL_DECLINE',
        'clear',
        (object) => management.declineProposals(service.getObjects(object, data)),
    );
    declineProposal.elementType = [ElementType.NodeProposal];
    declineProposal.constrains = [Constrain.User];
    declineProposal.group = DefaultGroups.Primary;
    declineProposal.priority = 20;

    const addNodeToLTIPlatform = new OptionItem('OPTIONS.LTI', 'input', (object) => {
        const nodes: Node[] = service.getObjects(object, data);
        void service.nodeHelper.addNodesToLTIPlatform(nodes);
    });
    addNodeToLTIPlatform.elementType = [ElementType.Node, ElementType.NodePublishedCopy];
    addNodeToLTIPlatform.permissions = [RestConstants.ACCESS_CC_PUBLISH];
    addNodeToLTIPlatform.permissionsRightMode = NodesRightMode.Effective;
    addNodeToLTIPlatform.showAsAction = true;
    addNodeToLTIPlatform.showAlways = true;
    addNodeToLTIPlatform.constrains = [
        Constrain.NoBulk,
        Constrain.Files,
        Constrain.User,
        Constrain.LTIMode,
    ];
    addNodeToLTIPlatform.group = DefaultGroups.Primary;
    addNodeToLTIPlatform.priority = 11;
    addNodeToLTIPlatform.permissions = [RestConstants.ACCESS_CC_PUBLISH];
    addNodeToLTIPlatform.customEnabledCallback = async (nodes: Node[]) => {
        const ltiSession = service.connectors.getRestConnector().getCurrentLogin().ltiSession;
        if (!ltiSession) {
            return false;
        }
        if (!ltiSession.acceptMultiple) {
            if (data.selectedObjects && data.selectedObjects.length > 1) {
                return false;
            }
        }
        /**
         * prevent lti editor as tool with custom content option, embedding nodes as platform created by the same tool
         */
        if (ltiSession.customContentNode && ltiSession.customContentNode.properties) {
            let customContentNodeLtiToolUrl =
                ltiSession.customContentNode.properties['ccm:ltitool_url'][0];
            return nodes.some((n) => {
                let nLtiToolUrlArr = ltiSession.customContentNode.properties['ccm:ltitool_url'];
                if (!Array.isArray(nLtiToolUrlArr) || nLtiToolUrlArr.length == 0) {
                    return true;
                }
                let nLtiToolUrl = nLtiToolUrlArr[0];

                if (
                    n.aspects.includes('ccm:ltitool_node') &&
                    nLtiToolUrl === customContentNodeLtiToolUrl
                ) {
                    return false;
                } else {
                    return true;
                }
            });
        }
        return true;
    };

    const pasteNodeIntoFolder = new OptionItem('OPTIONS.PASTE', 'content_paste', (node) =>
        service.pasteNode(
            components,
            {
                parent: service.getObjects(node, data)[0],
                scope: null,
            },
            false,
        ),
    );
    pasteNodeIntoFolder.elementType = [ElementType.Node];
    pasteNodeIntoFolder.constrains = [
        Constrain.ClipboardContent,
        Constrain.Directory,
        Constrain.NoBulk,
        Constrain.AddObjects,
        Constrain.User,
    ];
    // collections can only be pasted into another collection
    pasteNodeIntoFolder.customShowCallback = async () => !service.clipboardContainsCollections();
    pasteNodeIntoFolder.toolpermissions = [
        RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FOLDERS,
        RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES,
    ];
    pasteNodeIntoFolder.group = DefaultGroups.Primary;

    return [applyNode, acceptProposal, declineProposal, addNodeToLTIPlatform, pasteNodeIntoFolder];
}
