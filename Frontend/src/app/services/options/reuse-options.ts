import {
    Constrain,
    DefaultGroups,
    ElementType,
    HideMode,
    NodesRightMode,
    OptionItem,
} from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../core-module/rest/rest-constants';
import { OptionsContext } from './options-context';
import {
    NodesSelectorConfig,
    TabType,
} from '../../pages/editorial-page/nodes-selector/nodes-selector.component';

export function createReuseOptions({
    service,
    management,
    components,
    data,
}: OptionsContext): OptionItem[] {
    const sortInto = new OptionItem('OPTIONS.MANAGE_CONTENT', 'layers', (object) =>
        service.editorialSidebarService.showOption({
            option: 'MANAGE_CONTENT',
            trap: true,
            optionConfig: {
                state: TabType.COLLECTIONS,
                selection:
                    components.list?.getSelection() ||
                    service.nodeEntriesGlobalService?.getPrimaryInstance()?.selection,
            } as NodesSelectorConfig,
        }),
    );
    sortInto.elementType = [ElementType.Node, ElementType.NodePublishedCopy];
    sortInto.showAsAction = true;
    sortInto.constrains = [Constrain.Files, Constrain.User, Constrain.NoScope];
    sortInto.permissions = [RestConstants.ACCESS_CC_PUBLISH];
    sortInto.permissionsRightMode = NodesRightMode.Effective;
    sortInto.permissionsMode = HideMode.Disable;
    sortInto.group = DefaultGroups.Reuse;
    sortInto.priority = 10;

    const bookmarkNode = new OptionItem('OPTIONS.ADD_NODE_STORE', 'bookmark_border', (object) =>
        service.bookmarkNodes(service.getObjects(object, data)),
    );
    bookmarkNode.elementType = [ElementType.Node, ElementType.NodePublishedCopy];
    bookmarkNode.constrains = [Constrain.Files, Constrain.HomeRepository, Constrain.NoScope];
    bookmarkNode.group = DefaultGroups.Reuse;
    bookmarkNode.priority = 20;
    bookmarkNode.customShowCallback = async (nodes) => {
        if (nodes) {
            return nodes.every((n) => service.nodeHelper.referenceOriginalExists(n));
        }
        return true;
    };

    const shortcutNode = new OptionItem('OPTIONS.ADD_SHORTCUT', 'star', (object) => {
        const nodes = service.getObjects(object, data);
        void service.dialogs.openShortcutManagementDialog({ node: nodes[0] });
    });
    shortcutNode.elementType = [
        ElementType.Group,
        ElementType.MapRef,
        ElementType.Node,
        ElementType.NodeChild,
        ElementType.NodeProposal,
        ElementType.NodePublishedCopy,
        ElementType.Person,
        ElementType.SavedSearch,
    ];
    shortcutNode.constrains = [Constrain.User];
    shortcutNode.group = DefaultGroups.Reuse;
    shortcutNode.priority = 21;

    const createNodeVariant = new OptionItem('OPTIONS.VARIANT', 'call_split', (object) =>
        service.dialogs.openCreateVariantDialog({ node: service.getObjects(object, data)[0] }),
    );
    createNodeVariant.constrains = [
        Constrain.Files,
        Constrain.NoBulk,
        Constrain.HomeRepository,
        Constrain.User,
    ];
    createNodeVariant.toolpermissions = [RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES];
    createNodeVariant.customEnabledCallback = async (nodes) => {
        if (nodes) {
            // do not show variant if it's a licensed material and user doesn't has change permission rights
            return (
                nodes[0].properties?.[RestConstants.CCM_PROP_RESTRICTED_ACCESS]?.[0] !== 'true' ||
                service.nodeHelper.getNodesRight(
                    nodes,
                    RestConstants.ACCESS_CHANGE_PERMISSIONS,
                    NodesRightMode.Effective,
                )
            );
        }
        return true;
    };
    createNodeVariant.customShowCallback = async (nodes) => {
        if (nodes) {
            createNodeVariant.name =
                'OPTIONS.VARIANT' +
                (service.connectors.connectorSupportsEdit(nodes[0]) ? '_OPEN' : '');
            return service.nodeHelper.referenceOriginalExists(nodes[0]);
        }
        return false;
    };
    createNodeVariant.group = DefaultGroups.Reuse;
    createNodeVariant.priority = 30;

    return [sortInto, bookmarkNode, shortcutNode, createNodeVariant];
}
