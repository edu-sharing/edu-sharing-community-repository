import { OptionData, OptionsHelperComponents } from '../options-helper-data.service';
import {
    Constrain,
    ElementType,
    HideMode,
    OptionItem,
    Scope,
    Target,
} from '../../types/option-item';
import {
    Assignment,
    AssignmentFile,
    AuthenticationService,
    GenericAuthority,
    NetworkService,
    Node,
    ProposalNode,
    RestConstants,
} from 'ngx-edu-sharing-api';
import { NodeHelperService } from '../node-helper.service';
import { BehaviorSubject, firstValueFrom } from 'rxjs';
import { ActivatedRoute, Params } from '@angular/router';
import { Optional } from '@angular/core';
import { filter, first } from 'rxjs/operators';
import { NodeEntriesDataType } from '../../node-entries/data-type';
import { TemporaryStorageService } from '../temporary-storage.service';

export abstract class OptionsHelperService {
    protected queryParams: Params;
    protected enabledCache: { [key in string]: { [key in string]: BehaviorSubject<boolean> } } = {};

    constructor(
        protected nodeHelperService: NodeHelperService,
        protected authenticationService: AuthenticationService,
        protected storage: TemporaryStorageService,
        protected networkService: NetworkService,
        @Optional() protected route: ActivatedRoute,
    ) {
        this.route.queryParams.subscribe((queryParams) => (this.queryParams = queryParams));
    }

    getObjects(object: Node | any, data: OptionData) {
        return NodeHelperService.getActionbarNodes(
            data.selectedObjects || data.activeObjects,
            object,
        );
    }

    wrapOptionCallbacks(data: OptionData) {
        if (data.customOptions?.addOptions) {
            for (const option of data.customOptions.addOptions) {
                if (!(option as any).originalCallback) {
                    (option as any).originalCallback = option.callback;
                }
                option.callback = (node) =>
                    (option as any).originalCallback(node, this.getObjects(node, data));
            }
        }
        return data;
    }

    /**
     * overwrite all the show callbacks by using the internal constrains + permission handlers
     * isOptionAvailable will check if customShowCallback exists and will also call it
     */
    private handleCallbacks(options: OptionItem[], objects: Node[] | any, data: OptionData) {
        options.forEach((o) => {
            if (data?.scope === Scope.DebugShowAll) {
                o.showCallback = async () => true;
                o.enabledCallback = async () => true;
                return;
            }
            o.showCallback = async (object) => {
                const list = NodeHelperService.getActionbarNodes(objects, object);
                return await this.isOptionAvailable(o, list, data);
            };
            o.enabledCallback = async (object) => {
                const list = NodeHelperService.getActionbarNodes(objects, object);
                return await this.isOptionEnabled(o, list);
            };
        });
    }

    private async isOptionEnabled(option: OptionItem, objects: Node[] | any) {
        if (
            option.permissionsMode === HideMode.Disable &&
            option.permissions &&
            !this.validatePermissions(option, objects)
        ) {
            return false;
        }
        if (option.toolpermissions != null) {
            if (!(await this.validateToolpermissions(option))) {
                return false;
            }
        }
        if (option.customEnabledCallback) {
            if (!this.enabledCache[option.name]) {
                this.enabledCache[option.name] = {};
            }
            if (this.enabledCache[option.name]?.[objects?.[0]?.ref?.id] !== undefined) {
                return await this.enabledCache[option.name][objects?.[0]?.ref?.id]
                    .pipe(
                        filter((f) => f !== null),
                        first(),
                    )
                    .toPromise();
            }
            this.enabledCache[option.name][objects?.[0]?.ref?.id] = new BehaviorSubject<boolean>(
                null,
            );
            const status = await option.customEnabledCallback(objects);
            this.enabledCache[option.name][objects?.[0]?.ref?.id].next(status);
            return status;
        }
        return true;
    }

    protected getTypeSingle(object: NodeEntriesDataType) {
        if ((object as GenericAuthority).authorityType === RestConstants.AUTHORITY_TYPE_GROUP) {
            return ElementType.Group;
        } else if (
            (object as GenericAuthority).authorityType === RestConstants.AUTHORITY_TYPE_USER
        ) {
            return ElementType.Person;
        } else if ((object as Assignment).allowAdditionalDocumentSubmissions !== undefined) {
            return ElementType.Assignment;
        } else if ((object as AssignmentFile).referNode !== undefined) {
            return ElementType.AssignmentFile;
        } else if ((object as Node).ref) {
            const node = object as Node;
            if (node.type === RestConstants.CCM_TYPE_SAVED_SEARCH) {
                return ElementType.SavedSearch;
            } else if (node.aspects?.indexOf(RestConstants.CCM_ASPECT_IO_CHILDOBJECT) !== -1) {
                return ElementType.NodeChild;
            } else if (node.mediatype === 'folder-link') {
                return ElementType.MapRef;
            } else if (
                (node as ProposalNode).proposal ||
                node.type === RestConstants.CCM_TYPE_COLLECTION_PROPOSAL
            ) {
                return ElementType.NodeProposal;
            } else {
                if (this.nodeHelperService.isNodeRevoked(node)) {
                    return ElementType.NodeRevoked;
                } else if (this.nodeHelperService.isNodePublishedCopy(node)) {
                    return ElementType.NodePublishedCopy;
                } else if (
                    node.properties?.[RestConstants.CCM_PROP_IMPORT_BLOCKED]?.[0] === 'true'
                ) {
                    return ElementType.NodeBlockedImport;
                }
                return ElementType.Node;
            }
        }
        return ElementType.NoneOrUnknown;
    }

    protected getType(objects: Node[]): ElementType[] {
        if (objects) {
            const types = Array.from(new Set(objects.map((o) => this.getTypeSingle(o))));
            if (types.length > 0) {
                return types;
            }
        }
        return [ElementType.NoneOrUnknown];
    }

    protected async isOptionAvailable(
        option: OptionItem,
        objects: Node[] | any[],
        data: OptionData,
    ) {
        if (
            option.elementType?.length > 0 &&
            !this.getType(objects).every((t) => option.elementType.includes(t))
        ) {
            // console.log('types not matching', objects, this.getType(objects), option);
            return false;
        }
        if (option.scopes) {
            if (data.scope == null) {
                console.warn('Scope for options was not set, some may missing', option.name);
                return false;
            }
            if (option.scopes.indexOf(data.scope) === -1) {
                // console.log('scopes not matching', objects, option);
                return false;
            }
        }
        if (option.customShowCallback) {
            if ((await option.customShowCallback(objects)) === false) {
                // console.log('customShowCallback  was false', option, objects);
                return false;
            }
        }
        if (option.toolpermissions != null && option.toolpermissionsMode === HideMode.Hide) {
            if (!(await this.validateToolpermissions(option))) {
                // console.log('toolpermissions missing', option, objects);
                return false;
            }
        }
        if (option.permissions != null && option.permissionsMode === HideMode.Hide) {
            if (!this.validatePermissions(option, objects)) {
                // console.log('permissions missing', option, objects);
                return false;
            }
        }
        if (option.constrains != null) {
            const matched = await this.objectsMatchesConstrains(option.constrains, data, objects);
            if (matched != null) {
                // console.log('Constrain failed: ' + matched, option, objects);
                return false;
            }
        }
        // console.log('display option', option, objects);
        return true;
    }

    protected async objectsMatchesConstrains(
        constrains: Constrain[],
        data: OptionData = null,
        objects: Node[] | any[] = null,
    ) {
        // allow all options in debug scope
        if (data?.scope === Scope.DebugShowAll) {
            return null;
        }
        if (constrains.indexOf(Constrain.NoCollectionReference) !== -1) {
            if (
                objects.some(
                    (o) => o.aspects?.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) !== -1,
                )
            ) {
                return Constrain.NoCollectionReference;
            }
        }
        if (constrains.indexOf(Constrain.CollectionReference) !== -1) {
            if (
                objects.some(
                    (o) => o.aspects?.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) === -1,
                )
            ) {
                return Constrain.CollectionReference;
            }
        }
        if (constrains.indexOf(Constrain.NoBulk) !== -1) {
            if (objects.length > 1) {
                return Constrain.NoBulk;
            }
        }
        if (constrains.indexOf(Constrain.Directory) !== -1) {
            if (objects.some((o) => !o.isDirectory || o.collection)) {
                return Constrain.Directory;
            }
        }
        if (constrains.indexOf(Constrain.Collections) !== -1) {
            if (
                objects.some(
                    (o) =>
                        !(o.collection && o.aspects?.includes(RestConstants.CCM_ASPECT_COLLECTION)),
                )
            ) {
                return Constrain.Collections;
            }
        }
        if (constrains.indexOf(Constrain.Files) !== -1) {
            if (objects.some((o) => o.isDirectory || o.type !== RestConstants.CCM_TYPE_IO)) {
                return Constrain.Files;
            }
        }
        if (constrains.indexOf(Constrain.FilesAndDirectories) !== -1) {
            if (
                objects.some(
                    (o) =>
                        o.collection ||
                        (o.type !== RestConstants.CCM_TYPE_IO &&
                            o.type !== RestConstants.CCM_TYPE_MAP),
                )
            ) {
                return Constrain.FilesAndDirectories;
            }
        }
        if (constrains.indexOf(Constrain.Admin) !== -1) {
            if (!(await this.getLogin()).isAdmin) {
                return Constrain.Admin;
            }
        }
        if (constrains.indexOf(Constrain.AdminOrDebug) !== -1) {
            if (!(await this.getLogin()).isAdmin && !(window as any).esDebug) {
                return Constrain.AdminOrDebug;
            }
        }
        if (constrains.indexOf(Constrain.User) !== -1) {
            if ((await this.getLogin())?.statusCode !== RestConstants.STATUS_CODE_OK) {
                return Constrain.User;
            }
        }
        if (constrains.indexOf(Constrain.GuestOrNotLoggedIn) !== -1) {
            if ((await this.getLogin())?.statusCode === RestConstants.STATUS_CODE_OK) {
                return Constrain.GuestOrNotLoggedIn;
            }
        }
        if (constrains.indexOf(Constrain.LTIMode) !== -1) {
            if (!(await this.getLogin())?.ltiSession) {
                return Constrain.LTIMode;
            }
        }
        if (constrains.indexOf(Constrain.NoScope) !== -1) {
            if ((await this.getLogin())?.currentScope) {
                return Constrain.NoScope;
            }
        }
        if (constrains.includes(Constrain.Selection)) {
            if (!(objects && objects.length)) {
                return Constrain.Selection;
            }
        }
        if (constrains.includes(Constrain.NoSelection)) {
            if (objects && objects.length) {
                return Constrain.NoSelection;
            }
        }
        if (constrains.indexOf(Constrain.ClipboardContent) !== -1) {
            if (this.storage.get('workspace_clipboard') == null) {
                return Constrain.ClipboardContent;
            }
        }
        if (constrains.indexOf(Constrain.AddObjects) !== -1) {
            if (!this.canAddObjects(data)) {
                return Constrain.AddObjects;
            }
        }
        if (constrains.indexOf(Constrain.HomeRepository) !== -1) {
            if (!(await this.networkService.allFromHomeRepo(objects))) {
                return Constrain.HomeRepository;
            }
        }
        if (constrains.indexOf(Constrain.ReurlMode) !== -1) {
            if (!this.queryParams.reurl) {
                return Constrain.ReurlMode;
            }
        }
        return null;
    }

    protected canAddObjects(data: OptionData) {
        return (
            data.parent &&
            this.nodeHelperService.getNodesRight([data.parent], RestConstants.ACCESS_ADD_CHILDREN)
        );
    }

    getLogin() {
        return firstValueFrom(this.authenticationService.observeLoginInfo());
    }

    private async validateToolpermissions(option: OptionItem) {
        for (let t of option.toolpermissions) {
            if (!(await this.authenticationService.hasToolpermission(t))) {
                return false;
            }
        }
        return true;
    }

    private validatePermissions(option: OptionItem, objects: Node[] | any[]) {
        return (
            option.permissions.filter(
                (p) =>
                    this.nodeHelperService.getNodesRight(
                        objects,
                        p,
                        option.permissionsRightMode,
                    ) === false,
            ).length === 0
        );
    }

    /**
     * Filter options, can be also used externally
     * @param options
     * @param target
     * @param objects
     */
    async filterOptions(
        options: OptionItem[],
        target: Target,
        data: OptionData = null,
        objects: Node[] | any = null,
    ) {
        if (target === Target.List) {
            /*let optionsAlways = options.filter((o) => o.showAlways);
            const optionsOthers = options.filter((o) => !o.showAlways);
            optionsAlways = this.handleCallbackStates(options, target, objects);
            options = optionsAlways.concat(optionsOthers);*/
            // attach the show callbacks
            this.handleCallbacks(options, target, data);
        } else {
            options = await this.handleCallbackStates(options, target, data, objects);
        }
        options = this.sortOptionsByGroup(options);
        return options;
    }

    private sortOptionsByGroup(options: OptionItem[]) {
        if (!options) {
            return null;
        }
        let result: OptionItem[] = [];
        let groups = Array.from(new Set(options.map((o) => o.group)));
        groups = groups.sort((o1, o2) => (o1.priority > o2.priority ? 1 : -1));
        for (const group of groups) {
            const groupOptions = options.filter((o) => o.group === group);
            if (group == null) {
                console.warn(
                    'There are options not assigned to a group. All options should be assigned to a group',
                    groupOptions,
                );
            }
            groupOptions.sort((o1, o2) => (o1.priority > o2.priority ? 1 : -1));
            result = result.concat(groupOptions);
        }
        return result;
    }

    private async handleCallbackStates(
        options: OptionItem[],
        target: Target,
        data: OptionData,
        objects: Node[] | any[] = null,
    ) {
        this.handleCallbacks(options, objects, data);
        const showState = await Promise.all(
            options.map((o) =>
                o.showCallback(target === Target.List && objects && objects[0] ? objects[0] : null),
            ),
        );
        options = options.filter((o, i) => showState[i]);
        options = options.map((o) => {
            // disable them because the callback will later decide the state
            o.isEnabled = o.customEnabledCallback == null;
            return o;
        });
        return options;
    }

    abstract refreshComponents(
        components: OptionsHelperComponents,
        data: OptionData,
    ): Promise<void>;

    abstract getAvailableOptions(
        target: Target,
        objects: Node[],
        components: OptionsHelperComponents,
        data: OptionData,
    ): Promise<OptionItem[]>;

    abstract pasteNode(
        components: OptionsHelperComponents,
        data: OptionData,
        addVirutalNodes: boolean,
        nodes: Node[],
    ): void;
}
