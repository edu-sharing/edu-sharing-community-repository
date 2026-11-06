import { EventEmitter, inject, Injectable, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import {
    AssignmentFile,
    AssignmentV1Service,
    AuthenticationService,
    CollectionService as ApiCollectionService,
    HOME_REPOSITORY,
    NetworkService,
    Node,
    NodeListErrorResponses,
    NodeListService,
    NodeService,
    ROOT,
} from 'ngx-edu-sharing-api';
import {
    ClipboardObject,
    Constrain,
    CustomOptions,
    DefaultGroups,
    ElementType,
    HideMode,
    LocalEventsService,
    NodeEntriesDisplayType,
    NodeEntriesGlobalService,
    NodeHelperService as NodeHelperServiceUi,
    NodesRightMode,
    OptionCheckResult,
    OptionData,
    OptionExplanation,
    OptionItem,
    OptionItemToggle,
    OptionsHelperComponents,
    OptionsHelperService as OptionsHelperServiceAbstract,
    Target,
    TemporaryStorageService,
} from 'ngx-edu-sharing-ui';
import {
    firstValueFrom,
    forkJoin as observableForkJoin,
    lastValueFrom,
    Observable,
    of,
    Subject,
    Subscription,
} from 'rxjs';
import { catchError, filter, map, switchMap, tap } from 'rxjs/operators';
import {
    ConfigurationService,
    RestCollectionService,
    RestConnectorService,
    RestHelper,
} from '../core-module/core.module';
import { LocalPermissions, NodeWrapper } from '../core-module/rest/data-object';
import { Helper } from '../core-module/rest/helper';
import { RestConstants } from '../core-module/rest/rest-constants';
import { RestConnectorsService } from '../core-module/rest/services/rest-connectors.service';
import { RestNetworkService } from '../core-module/rest/services/rest-network.service';
import { RestNodeService } from '../core-module/rest/services/rest-node.service';
import { UIService } from '../core-module/rest/services/ui.service';
import { DELETE_OR_CANCEL } from '../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../features/dialogs/dialogs.service';
import { WorkspaceManagementDialogsComponent } from '../features/management-dialogs/management-dialogs.component';
import { MainNavService } from '../main/navigation/main-nav.service';
import { BridgeService } from './bridge.service';
import { MessageType } from '../util/message-type';
import { GlobalCollectionsPageService } from '../pages/collections-page/global-collections-page.service';
import { forkJoinWithErrors } from '../util/rxjs/forkJoinWithErrors';
import { ConfigOptionItem, NodeHelperService } from './node-helper.service';
import { Toast } from './toast';
import { UIHelper } from '../core-ui-module/ui-helper';
import { GlobalOptionsService } from './global-options.service';
import { CollectionNavigationService } from '../features/collection-info/collection-navigation.service';
import { EditorialSidebarService } from '../features/editorial-sidebar/editorial-sidebar.service';
import { OptionsContext } from './options/options-context';
import { createPrimaryOptions } from './options/primary-options';
import { createViewOptions } from './options/view-options';
import { createReuseOptions } from './options/reuse-options';
import { createEditOptions } from './options/edit-options';
import { createFileOperationsOptions } from './options/file-operations-options';
import { createDeleteOptions } from './options/delete-options';
import { createToggleOptions } from './options/toggle-options';

@Injectable()
export class OptionsHelperService extends OptionsHelperServiceAbstract implements OnDestroy {
    nodeHelperService = inject(NodeHelperServiceUi);
    authenticationService = inject(AuthenticationService);
    storage = inject(TemporaryStorageService);
    networkService = inject(NetworkService);
    route = inject(ActivatedRoute);
    public nodeHelper = inject(NodeHelperService);
    private bridge = inject(BridgeService);
    private collectionService = inject(RestCollectionService);
    private collectionApiService = inject(ApiCollectionService);
    public configService = inject(ConfigurationService);
    private globalOptionsService = inject(GlobalOptionsService);
    public nodeEntriesGlobalService = inject(NodeEntriesGlobalService);
    public connector = inject(RestConnectorService);
    public connectors = inject(RestConnectorsService);
    public dialogs = inject(DialogsService);
    public localEvents = inject(LocalEventsService);
    private mainNavService = inject(MainNavService);
    public editorialSidebarService = inject(EditorialSidebarService);
    public collectionNavigation = inject(CollectionNavigationService);
    private nodeList = inject(NodeListService);
    public nodeService = inject(NodeService);
    public nodeServiceLegacy = inject(RestNodeService);
    public router = inject(Router);
    public toast = inject(Toast);
    public assignmentV1Service = inject(AssignmentV1Service);
    private translate = inject(TranslateService);
    private globalCollectionsPageService = inject(GlobalCollectionsPageService);
    public uiService = inject(UIService);

    static DownloadElementTypes = [
        ElementType.Node,
        ElementType.NodeChild,
        ElementType.NodeProposal,
        ElementType.NodePublishedCopy,
    ];
    static ElementTypesAddToCollection = [ElementType.Node, ElementType.NodePublishedCopy];
    /**
     * human readable description of each constrain, used for debugging (explainOptions)
     */
    static ConstrainDescriptions: { [key in Constrain]: string } = {
        [Constrain.CollectionReference]: 'all objects are collection references',
        [Constrain.NoCollectionReference]: 'no object is a collection reference',
        [Constrain.Directory]: 'all objects are directories (ccm:map), no collections',
        [Constrain.Collections]: 'all objects are collections',
        [Constrain.Files]: 'all objects are files (ccm:io)',
        [Constrain.FilesAndDirectories]: 'all objects are ccm:io or ccm:map, no collections',
        [Constrain.Admin]: 'user is admin',
        [Constrain.AdminOrDebug]: 'user is admin or window.esDebug is set',
        [Constrain.NoBulk]: 'at most one object',
        [Constrain.Selection]: 'at least one object selected',
        [Constrain.NoSelection]: 'no object selected',
        [Constrain.ClipboardContent]: 'clipboard has content',
        [Constrain.AddObjects]: 'objects can be added to the current list',
        [Constrain.HomeRepository]: 'all objects are from the home repository',
        [Constrain.GuestOrNotLoggedIn]: 'user is guest or not logged in',
        [Constrain.User]: 'user is logged in',
        [Constrain.NoScope]: 'no (safe) scope is active',
        [Constrain.ReurlMode]: 'reurl is present (LMS picking mode)',
        [Constrain.LTIMode]: 'LTI session is present',
    };

    readonly virtualNodesAdded = new EventEmitter<Node[]>();
    readonly displayTypeChanged = new EventEmitter<NodeEntriesDisplayType>();

    private subscriptions: Subscription[] = [];
    private destroyed = new Subject<void>();

    constructor() {
        const nodeHelperService = inject(NodeHelperServiceUi);
        const authenticationService = inject(AuthenticationService);
        const storage = inject(TemporaryStorageService);
        const networkService = inject(NetworkService);
        const route = inject(ActivatedRoute);
        super(nodeHelperService, authenticationService, storage, networkService, route);
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    override getTypeSingle(node: any): any {
        return super.getTypeSingle(node);
    }
    cutCopyNode(data: OptionData, node: Node, copy: boolean) {
        let list = this.getObjects(node, data);
        if (!list || !list.length) {
            return;
        }
        list = Helper.deepCopy(list);
        const clip: ClipboardObject = { sourceNode: data.parent, nodes: list, copy };
        this.storage.set(TemporaryStorageService.WORKSPACE_CLIPBOARD, clip);
        this.bridge.showTemporaryMessage(MessageType.info, 'WORKSPACE.TOAST.CUT_COPY', {
            count: list.length,
        });
    }

    private getClipboard(): ClipboardObject | null {
        return this.storage.get(TemporaryStorageService.WORKSPACE_CLIPBOARD) as ClipboardObject;
    }

    /** Whether the clipboard holds collections. */
    clipboardContainsCollections(): boolean {
        const clip = this.getClipboard();
        return !!clip?.nodes?.length && clip.nodes.every((node) => this.isCollection(node));
    }

    /** Whether the clipboard holds files, the only nodes a collection can reference. */
    clipboardContainsCollectableNodes(): boolean {
        const clip = this.getClipboard();
        return (
            !!clip?.nodes?.length &&
            clip.nodes.every(
                (node) => !this.isCollection(node) && node.type === RestConstants.CCM_TYPE_IO,
            )
        );
    }

    /** Whether the clipboard was cut inside the collections (collections/references to be moved). */
    private clipboardIsCutFromCollection(): boolean {
        const clip = this.getClipboard();
        return (
            !!clip?.nodes?.length &&
            !clip.copy &&
            clip.nodes.every(
                (node) =>
                    this.isCollection(node) ||
                    node.aspects?.includes(RestConstants.CCM_ASPECT_IO_REFERENCE),
            )
        );
    }

    /**
     * Whether the clipboard can be pasted into a workspace folder: collections and nodes cut
     * inside a collection (collection references) can only be pasted into another collection.
     * Cut nodes can neither be pasted into their source folder again nor a folder into itself.
     */
    canPasteIntoFolder(target?: Node): boolean {
        if (this.clipboardContainsCollections() || this.clipboardIsCutFromCollection()) {
            return false;
        }
        const clip = this.getClipboard();
        if (!clip || !target?.ref?.id) {
            return true;
        }
        if (clip.nodes?.some((node) => node.ref.id === target.ref.id)) {
            return false;
        }
        return clip.copy || clip.sourceNode?.ref?.id !== target.ref.id;
    }

    /** Whether the clipboard can be pasted into the given collection. */
    canPasteIntoCollection(target: Node): boolean {
        const clip = this.getClipboard();
        if (!clip?.nodes?.length || !this.isCollection(target)) {
            return false;
        }
        if (target.ref.id === ROOT) {
            // collections start page: only collections can become level0 collections
            return (
                this.clipboardContainsCollections() &&
                this.connector.hasToolPermissionInstant(
                    RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_COLLECTIONS,
                ) &&
                (clip.copy ||
                    (this.clipboardIsCutFromCollection() &&
                        !clip.nodes.some((node) => node.collection?.level0)))
            );
        }
        if (
            !this.nodeHelper.getNodesRight(
                [target],
                RestConstants.ACCESS_WRITE,
                NodesRightMode.Local,
            )
        ) {
            return false;
        }
        if (!this.clipboardContainsCollections() && !this.clipboardContainsCollectableNodes()) {
            return false;
        }
        if (clip.copy) {
            return true;
        }
        // cut: only moving within the collections is supported, not into the current parent again
        // and a collection not into itself
        return (
            this.clipboardIsCutFromCollection() &&
            !clip.nodes.some(
                (node) =>
                    node.ref.id === target.ref.id ||
                    (node.parent?.id ?? clip.sourceNode?.ref?.id) === target.ref.id,
            )
        );
    }

    private isCollection(node: Node): boolean {
        return !!node?.collection || !!node?.aspects?.includes(RestConstants.CCM_ASPECT_COLLECTION);
    }

    /**
     * Pastes the clipboard into a collection: cut nodes are moved, copied collections are copied
     * and copied files are added as references.
     */
    private async pasteIntoCollection(target: Node): Promise<void> {
        if (!this.canPasteIntoCollection(target)) {
            return;
        }
        if (!this.getClipboard().copy) {
            return this.moveClipboardIntoCollection(target);
        }
        if (this.clipboardContainsCollections()) {
            return this.pasteCollections(target);
        }
        this.addClipboardNodesToCollection(target);
    }

    /** Moves the cut collections/references of the clipboard into the target collection. */
    private async moveClipboardIntoCollection(target: Node): Promise<void> {
        const clip = this.getClipboard();
        // editorial and other collections can not be nested into each other (same as drag & drop)
        const isEditorial = (node: Node) =>
            node.collection?.type === RestConstants.COLLECTIONTYPE_EDITORIAL;
        const toRoot = target.ref.id === ROOT;
        // moving to the start page is always allowed (same as dropping on the breadcrumb home)
        if (
            !toRoot &&
            this.clipboardContainsCollections() &&
            clip.nodes.some((node) => isEditorial(node) !== isEditorial(target))
        ) {
            this.toast.error(null, 'INVALID_OPERATION');
            return;
        }
        this.toast.showProgressSpinner();
        try {
            const moved: Node[] = [];
            for (const node of clip.nodes) {
                const result = await firstValueFrom(
                    this.nodeServiceLegacy.moveNode(
                        toRoot ? RestConstants.COLLECTIONHOME : target.ref.id,
                        node.ref.id,
                    ),
                );
                moved.push({ ...node, ...result?.node, collection: node.collection } as Node);
            }
            if (toRoot) {
                // fake the client state until the index has the new level0 collections
                await this.globalCollectionsPageService.addTemporaryCollections(moved);
                this.localEvents.nodesCreated.emit(moved);
            }
            this.storage.remove(TemporaryStorageService.WORKSPACE_CLIPBOARD);
            this.bridge.showTemporaryMessage(MessageType.info, 'WORKSPACE.TOAST.PASTE', {
                from: clip.sourceNode?.title || clip.sourceNode?.name || '',
                to: toRoot
                    ? this.translate.instant('COLLECTIONS.TITLE')
                    : target.title || target.name,
                count: clip.nodes.length,
                mode: this.translate.instant('WORKSPACE.PASTE_MOVE'),
            });
            if (!toRoot) {
                // the start page shows the moved collections via `nodesCreated`, a reload would
                // only return what the index already knows
                this.localEvents.nodesChanged.emit([target, clip.sourceNode].filter(Boolean));
            }
        } catch (error) {
            if (error?.status === RestConstants.DUPLICATE_NODE_RESPONSE) {
                this.toast.error(null, 'COLLECTIONS.ERROR_NODE_EXISTS');
            } else {
                this.toast.error(error);
            }
            // partially moved nodes are visible after a refresh
            this.localEvents.nodesChanged.emit([target, clip.sourceNode].filter(Boolean));
        } finally {
            this.toast.closeProgressSpinner();
        }
    }

    /** Adds the clipboard nodes to the given collection, they become collection references. */
    private addClipboardNodesToCollection(target: Node): void {
        const clip = this.storage.get(
            TemporaryStorageService.WORKSPACE_CLIPBOARD,
        ) as ClipboardObject;
        if (!clip?.nodes?.length || !target) {
            return;
        }
        // a collection references the original, not a reference node copied inside a collection
        const nodes = clip.nodes.map((node) => ({
            ...node,
            ref: { ...node.ref, id: this.nodeHelper.getOriginalId(node) },
        }));
        this.toast.showProgressSpinner();
        // `addToCollection` reports the result, handles duplicates and emits the node events itself
        this.uiService.addToCollection(target, nodes, false, () => {
            this.storage.remove(TemporaryStorageService.WORKSPACE_CLIPBOARD);
            this.toast.closeProgressSpinner();
        });
    }

    /**
     * Copies the clipboard collection(s) into the target collection, including child collections,
     * references and permissions.
     */
    private async pasteCollections(target: Node): Promise<void> {
        const clip = this.storage.get(
            TemporaryStorageService.WORKSPACE_CLIPBOARD,
        ) as ClipboardObject;
        if (!clip?.nodes?.length || !target) {
            return;
        }
        this.toast.showProgressSpinner();
        try {
            let skippedRefs = 0;
            const created: Node[] = [];
            for (const source of clip.nodes) {
                const copyResponse = await firstValueFrom(
                    this.collectionApiService.copyCollection({
                        repository: HOME_REPOSITORY,
                        sourceCollection: source.ref.id,
                        // the collections root is not a real node, omitting the target copies
                        // into the level0 collections
                        targetCollection: target.ref.id === ROOT ? undefined : target.ref.id,
                        copyRoot: true,
                        copyChildCollections: true,
                        copyRefs: true,
                        copyPermissions: true,
                    }),
                );
                skippedRefs +=
                    copyResponse?.entries?.filter(
                        (entry) => entry?.errorCode === 'NO_PUBLISH_PERMISSION',
                    )?.length ?? 0;
                if (copyResponse?.root) {
                    created.push(copyResponse.root);
                }
            }
            const toRoot = target.ref.id === ROOT;
            if (toRoot) {
                // fake the client state until the index has the new level0 collections
                await this.globalCollectionsPageService.addTemporaryCollections(created);
            }
            this.localEvents.nodesCreated.emit(created);
            this.storage.remove(TemporaryStorageService.WORKSPACE_CLIPBOARD);
            if (skippedRefs > 0) {
                this.toast.toast('COLLECTIONS.TOAST.COPIED_NO_PUBLISH_PERMISSION', {
                    count: skippedRefs,
                });
            } else {
                this.toast.toast('COLLECTIONS.TOAST.COPIED');
            }
            if (!toRoot) {
                // the start page shows the copies via `nodesCreated`, a reload would only return
                // what the index already knows
                this.localEvents.nodesChanged.emit([target]);
            }
        } catch (error) {
            this.toast.error(error);
        } finally {
            this.toast.closeProgressSpinner();
        }
    }

    pasteNode(
        components: OptionsHelperComponents,
        data: OptionData,
        addVirtualNodes = true,
        nodes: Node[] = [],
    ) {
        const clip = this.storage.get(
            TemporaryStorageService.WORKSPACE_CLIPBOARD,
        ) as ClipboardObject;
        // a collection target is not pasted node by node
        if (this.isCollection(data.parent)) {
            void this.pasteIntoCollection(data.parent);
            return;
        }
        // collections and collection references can only be pasted into another collection;
        // checked before the first node only, the recursion continues the started paste
        if (!nodes.length && !this.canPasteIntoFolder(data.parent)) {
            return;
        }
        if (!this.canAddObjects(data)) {
            return;
        }
        if (nodes.length === clip.nodes.length) {
            this.bridge.closeModalDialog();
            this.storage.remove(TemporaryStorageService.WORKSPACE_CLIPBOARD);
            const info: any = {
                from: clip.sourceNode
                    ? clip.sourceNode.name
                    : this.translate.instant('WORKSPACE.COPY_SEARCH'),
                to: data.parent.name,
                count: clip.nodes.length,
                mode: this.translate.instant(
                    'WORKSPACE.' + (clip.copy ? 'PASTE_COPY' : 'PASTE_MOVE'),
                ),
            };
            this.bridge.showTemporaryMessage(MessageType.info, 'WORKSPACE.TOAST.PASTE', info);
            if (addVirtualNodes) {
                this.addVirtualObjects(components, nodes);
            }
            return;
        }
        this.bridge.showProgressSpinner();
        const target = data.parent.ref.id;
        const source = clip.nodes[nodes.length].ref.id;
        if (clip.copy) {
            this.nodeServiceLegacy.copyNode(target, source).subscribe({
                next: (nodeData: NodeWrapper) =>
                    this.pasteNode(components, data, addVirtualNodes, nodes.concat(nodeData.node)),
                error: (error: any) => {
                    if (error.error?.error?.indexOf('DAORestrictedAccessException') !== -1) {
                        this.toast.error(null, 'RESTRICTED_ACCESS_COPY_ERROR');
                    } else {
                        this.nodeHelper.handleNodeError(clip.nodes[nodes.length].name, error);
                    }
                    this.bridge.closeModalDialog();
                },
            });
        } else {
            this.nodeServiceLegacy.moveNode(target, source).subscribe({
                next: (nodeData: NodeWrapper) =>
                    this.pasteNode(components, data, true, nodes.concat(nodeData.node)),
                error: (error: any) => {
                    this.nodeHelper.handleNodeError(clip.nodes[nodes.length].name, error);
                    this.bridge.closeModalDialog();
                },
            });
        }
    }

    /**
     * refresh all bound components with available menu options
     */
    async refreshComponents(components: OptionsHelperComponents, data: OptionData) {
        if (data == null || components == null) {
            // console.info('options helper refresh called but no data previously bound');
            return;
        }
        this.enabledCache = {};
        if (this.subscriptions?.length) {
            this.subscriptions.forEach((s) => s.unsubscribe());
            this.subscriptions = [];
        }

        if (components.list) {
            components.list.setOptions({
                [Target.List]: await this.getAvailableOptions(Target.List, [], components, data),
                [Target.ListDropdown]: await this.getAvailableOptions(
                    Target.ListDropdown,
                    [],
                    components,
                    data,
                ),
            });
        }
        if (components.dropdown) {
            components.dropdown.options = await this.getAvailableOptions(
                Target.ListDropdown,
                [],
                components,
                data,
            );
            components.dropdown.ngOnChanges();
        }
        if (components.actionbar) {
            const actionbars = (
                Array.isArray(components.actionbar) ? components.actionbar : [components.actionbar]
            ).filter(Boolean);
            const options = await this.getAvailableOptions(Target.Actionbar, [], components, data);
            for (const actionbar of actionbars) {
                actionbar.options = options;
                actionbar.invalidate();
            }
        }
    }

    /**
     * get all available default options
     * usefull for duplicating specific options for custom use cases
     */
    async getDefaultOptions(data: OptionData) {
        return this.prepareOptions(this.mainNavService.getDialogs(), null, null, data);
    }
    async getAvailableOptions(
        target: Target,
        objects: Node[],
        components: OptionsHelperComponents,
        data: OptionData,
    ) {
        if (target === Target.List) {
            if (objects == null) {
                // fetch ALL options of ALL items inside list
                // the callback handlers will later decide for the individual node
                objects = null;
            }
        } else if (target === Target.Actionbar) {
            objects = data.selectedObjects || data.activeObjects;
        } else if (target === Target.ListDropdown) {
            if (data.activeObjects) {
                objects = data.activeObjects;
            } else {
                return null;
            }
        }
        let options: OptionItem[] = [];
        if (this.mainNavService.getMainNav()) {
            options = this.prepareOptions(
                this.mainNavService.getDialogs(),
                objects,
                components,
                data,
            );
        } else {
            console.warn(
                'options helper was called without main nav. Can not load default options',
            );
        }
        /*
         // DO NOT DELETE
         // provides a csv-table like structure of all options
        console.info(
            options.map((o) => [
                this.translate.instant(o.name),
                o.scopes?.join(' '),
                o.toolpermissions?.join(' '),
                o.elementType?.join(' '),
                new OptionTooltipPipe(this.translate).getKeyInfo(o)
                ]
            ).map((a) => a.join(',')).join('\n')
        );
         */

        options = this.applyExternalOptions(options, data.customOptions);
        const custom = this.configService.instant<ConfigOptionItem[]>('customOptions');
        void this.nodeHelper.applyCustomNodeOptions(custom, data.allObjects, objects, options);
        // do pre-handle callback options for dropdown + actionbar
        options = await this.filterOptions(options, target, data, objects);
        if (target !== Target.Actionbar) {
            options = options.filter((o) => !(o as OptionItemToggle).isToggle);
            // do not show any actions in the dropdown for no selection, these are reserved for actionbar
            options = options.filter(
                (o) => !o.constrains || o.constrains.indexOf(Constrain.NoSelection) === -1,
            );
        }
        return this.uiService.filterValidOptions(options) as OptionItem[];
    }

    /**
     * Debug helper which mirrors the logic of getAvailableOptions, isOptionAvailable and
     * isOptionEnabled, but evaluates every single check instead of stopping at the first failing one
     */
    override async explainOptions(target: Target, data: OptionData): Promise<OptionExplanation[]> {
        const objects: Node[] | any[] =
            target === Target.Actionbar
                ? data.selectedObjects || data.activeObjects
                : data.activeObjects;
        const management = this.mainNavService.getDialogs();
        const baseOptions = this.prepareOptions(management, objects, null, {
            ...data,
            customOptions: null,
            postPrepareOptions: null,
        });
        let options = this.prepareOptions(management, objects, null, data);
        options = this.applyExternalOptions(options, data.customOptions);
        const custom = this.configService.instant<ConfigOptionItem[]>('customOptions');
        await this.nodeHelper.applyCustomNodeOptions(custom, data.allObjects, objects, options);

        const result: OptionExplanation[] = [];
        for (const option of options) {
            result.push(await this.explainOption(option, target, data, objects));
        }
        for (const option of baseOptions) {
            if (!options.some((o) => o.name === option.name)) {
                result.push({
                    option,
                    visibility: 'hidden',
                    checks: [
                        {
                            check: 'customOptions',
                            expected: 'present',
                            actual: 'removed by customOptions / postPrepareOptions / config',
                            passed: false,
                            effect: 'hide',
                        },
                    ],
                });
            }
        }
        return result;
    }

    private async explainOption(
        option: OptionItem,
        target: Target,
        data: OptionData,
        objects: Node[] | any[],
    ): Promise<OptionExplanation> {
        const checks: OptionCheckResult[] = [];
        const add = (
            check: string,
            expected: string,
            actual: string,
            passed: boolean,
            effect: OptionCheckResult['effect'] = 'hide',
        ) => checks.push({ check, expected, actual, passed, effect });
        const evaluate = async (callback: () => Promise<boolean> | boolean) => {
            try {
                return { value: await callback(), actual: null as string };
            } catch (e) {
                return { value: false, actual: 'error: ' + (e?.message ?? e) };
            }
        };

        if (option.elementType?.length > 0) {
            const types = this.getType(objects as Node[]);
            add(
                'elementType',
                option.elementType.map((t) => ElementType[t]).join(', '),
                types.map((t) => ElementType[t]).join(', '),
                types.every((t) => option.elementType.includes(t)),
            );
        }
        if (option.scopes) {
            add(
                'scopes',
                option.scopes.join(', '),
                data.scope ?? 'null',
                data.scope != null && option.scopes.includes(data.scope),
            );
        }
        // same order as isOptionAvailable: constrains are evaluated before customShowCallback
        for (const c of option.constrains ?? []) {
            const r = await evaluate(
                async () => (await this.objectsMatchesConstrains([c], data, objects)) == null,
            );
            add(
                'constrain: ' + Constrain[c],
                OptionsHelperService.ConstrainDescriptions[c] ?? '',
                r.actual ?? (r.value ? 'matched' : 'not matched'),
                r.value,
            );
        }
        if (option.customShowCallback) {
            const r = await evaluate(() => option.customShowCallback(objects));
            add('customShowCallback', 'true', r.actual ?? String(r.value), r.value !== false);
        }
        let missingToolpermissions: string[] = [];
        if (option.toolpermissions) {
            missingToolpermissions = await this.getMissingToolpermissions(option);
        }
        for (const p of option.toolpermissions ?? []) {
            const has = !missingToolpermissions.includes(p);
            add(
                'toolpermission (' + HideMode[option.toolpermissionsMode] + ')',
                p,
                has ? 'granted' : 'missing',
                has,
                // toolpermissions are always checked for enabled, and additionally for show in Hide mode
                option.toolpermissionsMode === HideMode.Hide ? 'hide' : 'disable',
            );
        }
        let missingPermissions: string[] = [];
        let permissionsError: string = null;
        try {
            missingPermissions = option.permissions
                ? this.getMissingPermissions(option, objects)
                : [];
        } catch (e) {
            permissionsError = 'error: ' + (e?.message ?? e);
        }
        for (const p of option.permissions ?? []) {
            const passed = !permissionsError && !missingPermissions.includes(p);
            add(
                'permission (' +
                    HideMode[option.permissionsMode] +
                    ', ' +
                    NodesRightMode[option.permissionsRightMode] +
                    ')',
                p,
                permissionsError ?? (passed ? 'granted' : 'missing'),
                passed,
                option.permissionsMode === HideMode.Hide ? 'hide' : 'disable',
            );
        }
        if (target !== Target.Actionbar) {
            if ((option as OptionItemToggle).isToggle) {
                add('target: toggle', 'Actionbar', Target[target], false);
            }
            if (option.constrains?.includes(Constrain.NoSelection)) {
                add('target: NoSelection', 'Actionbar', Target[target], false);
            }
        }
        if (option.onlyMobile || option.onlyDesktop || option.mediaQueryType) {
            add(
                'device / media query',
                [
                    option.onlyMobile ? 'onlyMobile' : null,
                    option.onlyDesktop ? 'onlyDesktop' : null,
                    option.mediaQueryType
                        ? option.mediaQueryType + ' ' + option.mediaQueryValue
                        : null,
                ]
                    .filter((v) => v)
                    .join(', '),
                this.uiService.isMobile() ? 'mobile' : 'desktop',
                this.uiService.filterValidOptions([option]).length === 1,
            );
        }
        if (option.customEnabledCallback) {
            const r = await evaluate(() => option.customEnabledCallback(objects));
            add('customEnabledCallback', 'true', r.actual ?? String(r.value), !!r.value, 'disable');
        }
        const failed = checks.filter((c) => !c.passed);
        return {
            option,
            visibility: failed.some((c) => c.effect === 'hide')
                ? 'hidden'
                : failed.length
                ? 'disabled'
                : 'shown',
            checks,
        };
    }

    private prepareOptions(
        management: WorkspaceManagementDialogsComponent,
        objects: Node[] | any[],
        components: OptionsHelperComponents,
        data: OptionData,
    ) {
        const ctx: OptionsContext = {
            service: this,
            management,
            components,
            data,
            queryParams: this.queryParams,
        };
        const options: OptionItem[] = [
            ...createPrimaryOptions(ctx),
            ...createViewOptions(ctx),
            ...createReuseOptions(ctx),
            ...createEditOptions(ctx),
            ...createFileOperationsOptions(ctx),
            ...createDeleteOptions(ctx),
            ...createToggleOptions(ctx),
        ];
        if (data?.postPrepareOptions) {
            data.postPrepareOptions(options, objects);
        }
        if (this.globalOptionsService.postPrepareOptions) {
            this.globalOptionsService.postPrepareOptions(options, data, objects);
        }
        return options;
    }

    getDownloadOption(data: OptionData, safe = false) {
        const downloadNode = new OptionItem(
            'OPTIONS.DOWNLOAD' + (safe ? '_SAFE' : ''),
            'cloud_download',
            (object) => {
                if (data.customDownloadUrl) {
                    this.nodeHelper.downloadUrl(data.customDownloadUrl, 'download', {
                        node: this.getObjects(object, data)?.[0],
                        triggerTrackingEvent: true,
                    });
                    return;
                }
                void this.nodeHelper.downloadNodes(this.getObjects(object, data));
            },
        );
        downloadNode.elementType = OptionsHelperService.DownloadElementTypes;
        downloadNode.constrains = [Constrain.Files];
        downloadNode.group = DefaultGroups.View;
        // downloadNode.key = 'D';
        downloadNode.priority = 40;
        downloadNode.customShowCallback = async (nodes) => {
            return (
                (!!data.customDownloadUrl && !safe) ||
                nodes.some((n) =>
                    (n as Node).properties?.[RestConstants.CCM_PROP_EDUSCOPENAME]?.includes(
                        RestConstants.SAFE_SCOPE,
                    ),
                ) === safe
            );
        };
        downloadNode.customEnabledCallback = async (nodes) => {
            if (!nodes) {
                return false;
            }

            for (let item of nodes) {
                if ((item as AssignmentFile).referNode) {
                    item = (item as AssignmentFile).referNode;
                }
                if (data.customDownloadUrl) {
                    return true;
                }
                // if at least one is allowed -> allow download (download servlet will later filter invalid files)
                if (
                    item.downloadUrl != null &&
                    item.properties &&
                    (!(
                        item.properties[RestConstants.CCM_PROP_IO_WWWURL] &&
                        !item.properties[RestConstants.LOM_PROP_TECHNICAL_LOCATION]
                    ) ||
                        !RestNetworkService.isFromHomeRepo(item)) &&
                    (item.accessEffective || item.access)?.includes(
                        RestConstants.PERMISSION_DOWNLOAD_CONTENT,
                    ) &&
                    this.nodeHelper.referenceOriginalExists(item)
                ) {
                    // bulk upload is not supported for remote nodes
                    if (!RestNetworkService.isFromHomeRepo(item) && nodes.length !== 1) {
                        continue;
                    }
                    return true;
                }
            }
            return false;
        };
        return downloadNode;
    }

    async revokeNode(object: any, data: OptionData) {
        const dialogRef = await this.dialogs.openRevocationDialog({
            node: this.getObjects(object, data)[0],
        });
        dialogRef.afterClosed().subscribe((result) => {
            if (result) {
                this.localEvents.nodesChanged.emit([result.node]);
            }
        });
    }

    private addVirtualObjects(components: OptionsHelperComponents, objects: any[]) {
        objects = objects.map((o: any) => {
            o.virtual = true;
            return o;
        });
        this.virtualNodesAdded.emit(objects);
        if (components.list) {
            components.list.addVirtualNodes(objects);
        }
    }

    bookmarkNodes(nodes: Node[]) {
        this.bridge.showProgressSpinner();
        this.addToNodeStore(nodes).subscribe(() => {
            this.bridge.closeModalDialog();
        });
    }

    private addToNodeStore(nodes: Node[]): Observable<void> {
        return this.nodeList
            .addToNodeList(
                RestConstants.NODE_STORE_LIST,
                nodes.map((node) => node.ref.id),
            )
            .pipe(
                tap(() => {
                    this.toast.toast('SEARCH.ADDED_TO_NODE_STORE', {
                        count: nodes.length,
                    });
                }),
                catchError((errors: NodeListErrorResponses) => {
                    const numberSuccessful = nodes.length - errors.length;
                    if (numberSuccessful > 0) {
                        this.toast.toast('SEARCH.ADDED_TO_NODE_STORE', {
                            count: numberSuccessful,
                        });
                    }
                    for (const { nodeId, error } of errors) {
                        if (error.status === RestConstants.DUPLICATE_NODE_RESPONSE) {
                            this.toast.error(null, 'SEARCH.ADDED_TO_NODE_STORE_EXISTS', {
                                name: RestHelper.getTitle(
                                    nodes.find((node) => node.ref.id === nodeId),
                                ),
                            });
                            error.preventDefault();
                        }
                    }
                    return of(void 0);
                }),
            );
    }

    goToWorkspace(node: Node | any) {
        if (node.aspects.includes(RestConstants.CCM_ASPECT_IO_REFERENCE)) {
            this.nodeServiceLegacy
                .getNodeMetadata(node.properties[RestConstants.CCM_PROP_IO_ORIGINAL][0])
                .subscribe((org) =>
                    UIHelper.goToWorkspace(
                        this.nodeServiceLegacy,
                        this.router,
                        this.connector.getCurrentLogin(),
                        org.node,
                    ),
                );
        } else {
            UIHelper.goToWorkspace(
                this.nodeServiceLegacy,
                this.router,
                this.connector.getCurrentLogin(),
                node,
            );
        }
    }

    async getObjectsAsync(object: Node | any, data: OptionData, resolveOriginals = false) {
        const nodes = NodeHelperService.getActionbarNodes(
            data.selectedObjects || data.activeObjects,
            object,
        );
        if (resolveOriginals) {
            const originals = await lastValueFrom(
                observableForkJoin(
                    nodes.map((n) => {
                        if (n.aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) !== -1) {
                            return this.nodeServiceLegacy.getNodeMetadata(
                                n.properties[RestConstants.CCM_PROP_IO_ORIGINAL][0],
                                [RestConstants.ALL],
                            );
                        } else if (n.type === RestConstants.CCM_TYPE_COLLECTION_PROPOSAL) {
                            return this.nodeServiceLegacy.getNodeMetadata(
                                RestHelper.removeSpacesStoreRef(
                                    n.properties[
                                        RestConstants.CCM_PROP_COLLECTION_PROPOSAL_TARGET
                                    ][0],
                                ),
                                [RestConstants.ALL],
                            );
                        } else {
                            return of({
                                node: n,
                            });
                        }
                    }),
                ),
            );
            return originals.map((o) => o.node);
        }
        return nodes;
    }

    applyExternalOptions(options: OptionItem[], customOptionsIn: CustomOptions) {
        return OptionsHelperService.applyExternalOptions(options, customOptionsIn);
    }

    /**
     * Merges `customOptionsIn` into `options`. Stateless, so it can also be used by components that
     * do not have an `OptionsHelperService` instance available
     */
    static applyExternalOptions(options: OptionItem[], customOptionsIn: CustomOptions) {
        if (!customOptionsIn) {
            return options;
        }
        const customOptions = { ...new CustomOptions(), ...customOptionsIn };
        if (!customOptions.useDefaultOptions) {
            options = [];
        }
        if (customOptions.supportedOptions && Array.isArray(customOptions.supportedOptions)) {
            options = options.filter((o) => customOptions.supportedOptions.indexOf(o.name) !== -1);
        } else if (customOptions.removeOptions) {
            for (const option of customOptions.removeOptions) {
                const index = options.findIndex((o) => o.name === option);
                if (index !== -1) {
                    options.splice(index, 1);
                }
            }
        }
        if (customOptions.addOptions) {
            for (const option of customOptions.addOptions) {
                const existing = options.filter((o) => o.name === option.name);
                if (existing.length === 1) {
                    // only replace changed values
                    for (const key of Object.keys(option)) {
                        (existing[0] as any)[key] = (option as any)[key];
                    }
                } else {
                    options.push(option);
                }
            }
        }
        return options;
    }

    /**
     * Shows a confirmation dialog and removes the given nodes from the current collection if
     * confirmed.
     */
    async removeFromCollection(nodes: Node[], data: OptionData) {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'OPTIONS.REMOVE_REF',
            message: 'COLLECTIONS.REMOVE_FROM_COLLECTION_DIALOG_TEXT',
            messageParameters: { count: nodes.length.toString() },
            buttons: DELETE_OR_CANCEL,
        });
        dialogRef
            .afterClosed()
            .pipe(
                filter((value) => value === 'YES_DELETE'),
                switchMap(() =>
                    forkJoinWithErrors(
                        nodes.map((node: Node) =>
                            this.collectionService
                                .removeFromCollection(node.ref.id, data.parent.ref.id)
                                .pipe(map(() => node)),
                        ),
                    ),
                ),
            )
            .subscribe(({ successes: deletedNodes, errors }) => {
                if (errors.length > 0) {
                    this.toast.error(errors[0]);
                } else {
                    this.toast.toast('COLLECTIONS.REMOVED_FROM_COLLECTION');
                }
                if (deletedNodes.length > 0) {
                    this.localEvents.nodesDeleted.emit(deletedNodes);
                }
            });
    }

    unblockImportedNodes(nodes: Node[]) {
        this.toast.showProgressSpinner();
        observableForkJoin(
            nodes.map((n) => {
                const properties: any = {};
                properties[RestConstants.CCM_PROP_IMPORT_BLOCKED] = [null];
                return new Observable((observer) => {
                    this.nodeServiceLegacy
                        .editNodeMetadataNewVersion(
                            n.ref.id,
                            RestConstants.COMMENT_BLOCKED_IMPORT,
                            properties,
                        )
                        .subscribe(({ node }) => {
                            const permissions = new LocalPermissions();
                            permissions.inherited = true;
                            permissions.permissions = [];
                            this.nodeServiceLegacy
                                .setNodePermissions(node.ref.id, permissions)
                                .subscribe(() => {
                                    observer.next(node);
                                    observer.complete();
                                });
                        });
                });
            }),
        ).subscribe((results: Node[]) => {
            this.toast.closeProgressSpinner();
            this.localEvents.nodesChanged.emit(results);
        });
    }
}
