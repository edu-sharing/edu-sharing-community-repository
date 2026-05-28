import {
    AssignmentPipe,
    Constrain,
    DefaultGroups,
    ElementType,
    HideMode,
    NodesRightMode,
    OptionItem,
    Scope,
    UIConstants,
} from 'ngx-edu-sharing-ui';
import { Assignment, Node, ROOT } from 'ngx-edu-sharing-api';
import { DialogButton } from '../../core-module/core.module';
import { RestConstants } from '../../core-module/rest/rest-constants';
import { RestHelper } from '../../core-module/rest/rest-helper';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { Closable } from '../../features/dialogs/card-dialog/card-dialog-config';
import { OptionsContext } from './options-context';
import { firstValueFrom, forkJoin } from 'rxjs';

export function createViewOptions({
    service,
    management,
    components,
    data,
}: OptionsContext): OptionItem[] {
    const debugNode = new OptionItem('OPTIONS.DEBUG', 'build', async (object) => {
        let nodes = service.getObjects(object, data);
        console.info(nodes);
        if (nodes.some((n) => n.authorityName)) {
            try {
                nodes = (
                    await forkJoin(
                        nodes.map((n) =>
                            service.nodeServiceLegacy.getNodeMetadata(
                                n.ref?.id || n.properties?.[RestConstants.NODE_ID]?.[0],
                                [RestConstants.ALL],
                            ),
                        ),
                    ).toPromise()
                ).map((n) => n.node);
            } catch (e) {
                console.info(nodes);
                console.warn(e);
            }
        }
        void service.dialogs.openNodeInfoDialog({ nodes });
    });
    debugNode.elementType = [];
    debugNode.onlyDesktop = true;
    debugNode.constrains = [Constrain.AdminOrDebug, Constrain.Selection];
    debugNode.group = DefaultGroups.View;
    debugNode.priority = -100;

    const viewElement = new OptionItem('OPTIONS.VIEW_ELEMENT', 'visibility', async (object) => {
        const node = service.getObjects(object, data)[0];
        await service.uiService.openNode(node, false);
    });
    viewElement.priority = 2;
    viewElement.group = DefaultGroups.View;
    viewElement.showAsAction = true;
    viewElement.constrains = [Constrain.NoBulk, Constrain.HomeRepository];
    viewElement.scopes = [
        Scope.Search,
        Scope.WorkspaceList,
        Scope.EditorialPage,
        Scope.EditorialSidebar,
        Scope.CollectionsReferences,
    ];
    viewElement.customShowCallback = async (nodes: Node[]) => {
        return nodes && nodes.length === 1 && nodes[0].type === RestConstants.CCM_TYPE_MAP;
    };

    const openOriginalNode = new OptionItem(
        'OPTIONS.OPEN_ORIGINAL_NODE',
        'description',
        async (object) => {
            const nodeId = RestHelper.removeSpacesStoreRef(
                service.getObjects(object, data)[0].properties[
                    RestConstants.CCM_PROP_PUBLISHED_ORIGINAL
                ][0],
            );
            UIHelper.goToNode(service.router, { ref: { id: nodeId } } as Node);
        },
    );
    openOriginalNode.constrains = [
        Constrain.Files,
        Constrain.NoBulk,
        Constrain.HomeRepository,
        Constrain.User,
    ];
    openOriginalNode.constrains = [
        Constrain.Files,
        Constrain.NoBulk,
        Constrain.HomeRepository,
        Constrain.User,
    ];
    openOriginalNode.toolpermissions = [RestConstants.TOOLPERMISSION_WORKSPACE];
    openOriginalNode.scopes = [Scope.CollectionsReferences, Scope.Search, Scope.Render];
    openOriginalNode.customEnabledCallback = async (nodes: Node[]) => {
        if (nodes && nodes.length === 1) {
            return new Promise<boolean>((resolve) => {
                const nodeId = RestHelper.removeSpacesStoreRef(
                    nodes[0].properties[RestConstants.CCM_PROP_PUBLISHED_ORIGINAL][0],
                );
                service.nodeServiceLegacy.getNodeMetadata(nodeId).subscribe(
                    () => {
                        resolve(true);
                    },
                    () => {
                        resolve(false);
                    },
                );
            });
        }
        return false;
    };
    openOriginalNode.elementType = [ElementType.NodePublishedCopy, ElementType.NodeRevoked];
    openOriginalNode.group = DefaultGroups.View;
    openOriginalNode.priority = 13;
    openOriginalNode.showAsAction = false;

    const openParentNode = new OptionItem('OPTIONS.SHOW_IN_FOLDER', 'folder', async (object) =>
        service.goToWorkspace((await service.getObjectsAsync(object, data, true))[0]),
    );
    openParentNode.constrains = [
        Constrain.Files,
        Constrain.NoBulk,
        Constrain.HomeRepository,
        Constrain.User,
    ];
    openParentNode.toolpermissions = [RestConstants.TOOLPERMISSION_WORKSPACE];
    openParentNode.scopes = [
        Scope.CollectionsReferences,
        Scope.Search,
        Scope.Render,
        Scope.EditorialPage,
    ];
    openParentNode.customEnabledCallback = async (nodes: Node[]) => {
        if (nodes && nodes.length === 1) {
            return new Promise<boolean>((resolve) => {
                let nodeId = nodes[0].ref.id;
                if (nodes[0].aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) !== -1) {
                    nodeId = nodes[0].properties[RestConstants.CCM_PROP_IO_ORIGINAL][0];
                }
                service.nodeService.getParents(nodeId, { fullPath: false }).subscribe(
                    () => {
                        resolve(true);
                    },
                    (error) => {
                        error.preventDefault();
                        resolve(false);
                    },
                );
            });
        }
        return false;
    };
    openParentNode.group = DefaultGroups.View;
    openParentNode.priority = 15;
    openParentNode.showAsAction = false;

    const openNode = new OptionItem('OPTIONS.SHOW', 'remove_red_eye', (object) =>
        UIHelper.goToNode(service.router, service.getObjects(object, data)[0]),
    );
    openNode.constrains = [Constrain.Files, Constrain.NoBulk];
    openNode.scopes = [Scope.WorkspaceList];
    openNode.group = DefaultGroups.View;
    openNode.priority = 30;

    const editConnectorNode = new OptionItem('OPTIONS.OPEN', 'launch', (node) => {
        void service.uiService.editConnector(service.getObjects(node, data)[0]);
    });
    editConnectorNode.customShowCallback = async (nodes) => {
        return await service.uiService.hasAvailableConnector(nodes ? nodes[0] : null);
    };
    editConnectorNode.elementType = [
        ElementType.Node,
        ElementType.NodePublishedCopy,
        ElementType.NodeChild,
        ElementType.NodeProposal,
    ];
    editConnectorNode.group = DefaultGroups.View;
    editConnectorNode.priority = 20;
    editConnectorNode.showAsAction = true;
    editConnectorNode.constrains = [Constrain.Files, Constrain.NoBulk, Constrain.HomeRepository];

    const topicPage = new OptionItem('OPTIONS.SHOW_TOPIC_PAGE', 'menu_book', async (node) =>
        UIHelper.goToTopicPage(
            service.router,
            (await service.getObjectsAsync(node, data, false))[0],
        ),
    );
    topicPage.constrains = [
        Constrain.HomeRepository,
        Constrain.Collections,
        Constrain.NoBulk,
        Constrain.User,
    ];
    topicPage.customShowCallback = async (objects) => {
        if (
            service.nodeHelper.getNodesRight(
                objects as Node[],
                RestConstants.ACCESS_WRITE,
                NodesRightMode.Effective,
            )
        ) {
            return true;
        }
        if (objects[0].ref.id === ROOT) {
            return false;
        }
        if (!objects[0].aspects?.includes(RestConstants.CCM_ASPECT_COLLECTION)) {
            return false;
        }
        try {
            if (objects[0].properties?.[RestConstants.CCM_PROP_PAGE_CONFIG_REF]?.[0]) {
                return true;
            }
            return (
                await firstValueFrom(
                    service.nodeService.getParents(objects[0].ref.id, {
                        repository: objects[0].ref.repo,
                        fullPath: false,
                    }),
                )
            ).nodes.some(
                (n) => n.properties[RestConstants.CCM_PROP_PAGE_CONFIG_PROPAGATE_REF]?.[0],
            );
        } catch (e) {
            e.preventDefault();
            return false;
        }
    };
    topicPage.showAsAction = true;
    topicPage.group = DefaultGroups.View;
    topicPage.priority = 15;

    const submitAssignment = new OptionItem('OPTIONS.ASSIGNMENT_SUBMIT', 'send', (object) =>
        service.uiService.goToAssignment(service.getObjects(object, data)[0], 'submit'),
    );
    submitAssignment.customShowCallback = async (objects) => {
        const assignment = objects[0] as Assignment;
        return (
            assignment.type === 'SUBMISSION' &&
            new AssignmentPipe().transform(assignment, {
                mode: 'permissions',
            }) !== 'COORDINATOR'
        );
    };
    submitAssignment.elementType = [ElementType.Assignment];
    submitAssignment.constrains = [Constrain.NoBulk, Constrain.User];
    submitAssignment.showAsAction = true;
    submitAssignment.group = DefaultGroups.View;
    submitAssignment.priority = 5;

    const viewAssignmentSubmission = new OptionItem(
        'OPTIONS.ASSIGNMENT_SUBMISSION',
        'inbox',
        (object) =>
            service.uiService.goToAssignment(service.getObjects(object, data)[0], 'submissions'),
    );
    viewAssignmentSubmission.customShowCallback = async (objects) => {
        const assignment = objects[0] as Assignment;
        return (
            assignment.status !== 'DRAFT' &&
            assignment.type === 'SUBMISSION' &&
            new AssignmentPipe().transform(assignment, {
                mode: 'permissions',
            }) === 'COORDINATOR'
        );
    };
    viewAssignmentSubmission.elementType = [ElementType.Assignment];
    viewAssignmentSubmission.constrains = [Constrain.NoBulk, Constrain.User];
    viewAssignmentSubmission.showAsAction = true;
    viewAssignmentSubmission.group = DefaultGroups.View;
    viewAssignmentSubmission.priority = 5;

    const infoVersions = new OptionItem('OPTIONS.WORKSPACE_METADATA', 'info', (node: Node) => {
        service.editorialSidebarService.showOption({
            option: 'WORKSPACE_METADATA',
            trap: false,
        });
    });
    infoVersions.scopes = [Scope.WorkspaceList, Scope.Search, Scope.EditorialPage];
    infoVersions.group = DefaultGroups.View;
    infoVersions.constrains = [
        Constrain.NoBulk,
        Constrain.HomeRepository,
        Constrain.FilesAndDirectories,
    ];
    infoVersions.priority = 20;
    infoVersions.showAsAction = false;

    const downloadNode = service.getDownloadOption(data, false);
    const downloadNodeSafe = service.getDownloadOption(data, true);

    const downloadMetadataNode = new OptionItem(
        'OPTIONS.DOWNLOAD_METADATA',
        'format_align_left',
        (object) =>
            service.dialogs.openGenericDialog({
                title: 'DOWNLOAD_METADATA.TITLE',
                message: 'DOWNLOAD_METADATA.MESSAGE',
                closable: Closable.Casual,
                avatar: {
                    icon: 'format_align_left',
                    kind: 'icon',
                },
                buttons: [
                    {
                        label: 'DOWNLOAD_METADATA.TYPE_TEXT',
                        config: DialogButton.TYPE_CANCEL,
                        callback: (ref) => {
                            ref.close();
                            void service.nodeHelper.downloadNode(
                                service.getObjects(object, data)[0],
                                RestConstants.NODE_VERSION_CURRENT,
                                true,
                            );
                            return null;
                        },
                    },
                    {
                        label: 'DOWNLOAD_METADATA.TYPE_PDF',
                        config: DialogButton.TYPE_PRIMARY,
                        callback: async (ref) => {
                            const node = service.getObjects(object, data)[0];
                            void service.router.navigate([
                                UIConstants.ROUTER_PREFIX + 'pdf-metadata',
                                node.ref.id,
                            ]);
                            return true;
                        },
                    },
                ],
            }),
    );
    downloadMetadataNode.elementType = [
        ElementType.Node,
        ElementType.NodeChild,
        ElementType.NodePublishedCopy,
    ];
    downloadMetadataNode.constrains = [Constrain.Files, Constrain.NoBulk];
    downloadMetadataNode.scopes = [Scope.Render];
    downloadMetadataNode.group = DefaultGroups.View;
    downloadMetadataNode.priority = 50;
    downloadMetadataNode.customShowCallback = async (nodes) => {
        if (!nodes) {
            return false;
        }
        return nodes[0].downloadUrl != null;
    };

    const reportNode = new OptionItem('OPTIONS.NODE_REPORT', 'flag', (node) =>
        service.dialogs.openNodeReportDialog({
            node: service.getObjects(node, data)[0],
            mode: 'NODE_REPORT',
            showOptions: true,
        }),
    );
    reportNode.elementType = [ElementType.Node, ElementType.NodePublishedCopy];
    reportNode.constrains = [Constrain.Files, Constrain.NoBulk, Constrain.HomeRepository];
    reportNode.scopes = [Scope.Search, Scope.CollectionsReferences, Scope.Render];
    reportNode.customShowCallback = async (objects) =>
        objects?.every((n) => (n as Node).access !== null) &&
        (await firstValueFrom(service.configService.get('nodeReport', false)));
    reportNode.group = DefaultGroups.View;
    reportNode.priority = 60;

    const qrCodeNode = new OptionItem('OPTIONS.QR_CODE', 'edu-qr_code', (node) => {
        node = service.getObjects(node, data)[0];
        void service.dialogs.openQrDialog({ node });
    });
    qrCodeNode.elementType = [ElementType.Node, ElementType.NodePublishedCopy];
    qrCodeNode.constrains = [Constrain.NoBulk];
    qrCodeNode.scopes = [Scope.Render, Scope.CollectionsCollection];
    qrCodeNode.group = DefaultGroups.View;
    qrCodeNode.priority = 70;

    const embedNode = new OptionItem('OPTIONS.EMBED', 'perm_media', (node) => {
        node = service.getObjects(node, data)[0];
        void service.dialogs.openNodeEmbedDialog({ node });
    });
    embedNode.elementType = [ElementType.Node, ElementType.NodePublishedCopy];
    embedNode.constrains = [Constrain.NoBulk, Constrain.HomeRepository];
    embedNode.scopes = [Scope.Render];
    embedNode.group = DefaultGroups.View;
    embedNode.priority = 80;

    const feedbackMaterial = new OptionItem('OPTIONS.MATERIAL_FEEDBACK', 'chat_bubble', (object) =>
        service.dialogs.openSendFeedbackDialog({ node: service.getObjects(object, data)[0] }),
    );
    feedbackMaterial.constrains = [Constrain.HomeRepository, Constrain.Files, Constrain.NoBulk];
    feedbackMaterial.permissions = [RestConstants.PERMISSION_FEEDBACK];
    feedbackMaterial.permissionsRightMode = NodesRightMode.Effective;
    feedbackMaterial.scopes = [Scope.Render];
    feedbackMaterial.permissionsMode = HideMode.Hide;
    feedbackMaterial.toolpermissions = [RestConstants.TOOLPERMISSION_MATERIAL_FEEDBACK];
    feedbackMaterial.group = DefaultGroups.View;
    feedbackMaterial.priority = 15;
    // feedback is only shown for non-managers
    feedbackMaterial.customShowCallback = async (objects) =>
        !service.nodeHelper.getNodesRight(
            objects as Node[],
            RestConstants.ACCESS_WRITE,
            NodesRightMode.Effective,
        );

    const feedbackMaterialView = new OptionItem(
        'OPTIONS.MATERIAL_FEEDBACK_VIEW',
        'speaker_notes',
        (object) => (management.materialViewFeedback = service.getObjects(object, data)[0]),
    );
    feedbackMaterialView.constrains = [
        Constrain.HomeRepository,
        Constrain.Files,
        Constrain.NoBulk,
        Constrain.User,
    ];
    feedbackMaterialView.scopes = [Scope.Render];
    feedbackMaterialView.permissions = [RestConstants.ACCESS_DELETE];
    feedbackMaterialView.permissionsRightMode = NodesRightMode.Effective;
    feedbackMaterialView.permissionsMode = HideMode.Hide;
    feedbackMaterialView.toolpermissions = [RestConstants.TOOLPERMISSION_MATERIAL_FEEDBACK];
    feedbackMaterialView.group = DefaultGroups.View;
    feedbackMaterialView.priority = 20;

    return [
        debugNode,
        viewElement,
        openOriginalNode,
        openParentNode,
        openNode,
        editConnectorNode,
        topicPage,
        submitAssignment,
        viewAssignmentSubmission,
        infoVersions,
        downloadNode,
        downloadNodeSafe,
        downloadMetadataNode,
        reportNode,
        qrCodeNode,
        embedNode,
        feedbackMaterial,
        feedbackMaterialView,
    ];
}
