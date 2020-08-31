import { trigger } from '@angular/animations';
import {
    ApplicationRef,
    Component,
    EventEmitter,
    Input,
    Output, ViewChild,
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import {Observable, Observer} from 'rxjs';
import {
    Collection,
    CollectionUsage,
    ConfigurationService,
    DialogButton,
    LocalPermissions,
    LoginResult,
    Node,
    NodeList,
    NodePermissions,
    NodeShare,
    NodeWrapper,
    Permission,
    RestCollectionService,
    RestConnectorService,
    RestConstants,
    RestHelper,
    RestIamService,
    RestNodeService,
    RestUsageService,
    UsageList,
} from '../../../core-module/core.module';
import { Helper } from '../../../core-module/rest/helper';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import { UIConstants } from '../../../core-module/ui/ui-constants';
import { NodeHelper } from '../../../core-ui-module/node-helper';
import { Toast } from '../../../core-ui-module/toast';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import {SharePublishComponent} from './share-publish/share-publish.component';

@Component({
    selector: 'workspace-share',
    templateUrl: 'share.component.html',
    styleUrls: ['share.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
})
export class WorkspaceShareComponent {
    @ViewChild('publish') publishComponent: SharePublishComponent;
    @Input() sendMessages = true;
    @Input() sendToApi = true;
    @Input() currentPermissions: LocalPermissions = null;
    @Input() set nodes(nodes: []) {
        this.setNodes(nodes);
    }
    @Input() set nodeId(node: string) {
        if (node)
            this.nodeApi
                .getNodeMetadata(node, [RestConstants.ALL])
                .subscribe((data: NodeWrapper) => {
                    this.setNodes([data.node]);
                });
    }
    @Input() set node(node: Node) {
        this.setNodes([node]);
    }

    @Output() onClose = new EventEmitter();
    @Output() onLoading = new EventEmitter();

    readonly ALL_PERMISSIONS = [
        'All',
        'Read',
        'ReadPreview',
        'ReadContent',
        'ReadAll',
        'Comment',
        'Rate',
        'Write',
        'Delete',
        'DeleteChildren',
        'DeleteNode',
        'AddChildren',
        'Consumer',
        'ConsumerMetadata',
        'Editor',
        'Contributor',
        'Collaborator',
        'Coordinator',
        'Publisher',
        'ReadPermissions',
        'ChangePermissions',
        'CCPublish',
        'Comment',
        'Feedback',
        'Deny',
    ];
    readonly PERMISSIONS_FORCES = [
        ['Read', ['ConsumerMetadata']],
        ['Read', ['Consumer']],
        ['ReadPreview', ['ReadAll']],
        ['ReadContent', ['ReadAll']],
        ['ReadAll', ['Consumer']],
        ['Comment', ['Consumer']],
        ['Feedback', ['Consumer']],
        ['Rate', ['Consumer']],
        ['Write', ['Editor']],
        ['DeleteChildren', ['Delete']],
        ['DeleteNode', ['Delete']],
        ['AddChildren', ['Contributor']],
        ['ReadPermissions', ['Contributor']],
        ['Contributor', ['Collaborator']],
    ];

    initialState: string;
    _tab = 0;
    set tab(tab: number) {
        this._tab = tab;
        this.updateButtons();
    }
    get tab() {
        return this._tab;
    }
    permissionsUser: Permission[];
    permissionsGroup: Permission[];
    newPermissions: Permission[] = [];
    inheritAccessDenied = false;
    bulkMode = 'extend';
    owner: Permission;
    linkEnabled: Permission;
    linkDisabled: Permission;
    link = false;
    _nodes: Node[];
    searchStr: string;
    inheritAllowed = false;
    globalSearch = false;
    globalAllowed = false;
    fuzzyAllowed = false;
    history: Node;
    linkNode: Node;
    showLink: boolean;
    isAdmin: boolean;
    publishPermission: boolean;
    isSafe = false;
    collectionColumns = UIHelper.getDefaultCollectionColumns();
    collections: CollectionUsage[];
    // store authorities marked for deletion
    deletedPermissions: string[] = [];
    deletedUsages: any[] = [];
    usages: any;
    showCollections = false;
    buttons: DialogButton[] = [
        new DialogButton('CANCEL', DialogButton.TYPE_CANCEL, () =>
            this.cancel(),
        ),
        new DialogButton(
            'WORKSPACE.BTN_INVITE', // Can be changed in `updateButtons`.
            DialogButton.TYPE_PRIMARY,
            () => this.save(),
        ),
    ];

    private currentType = [
        RestConstants.ACCESS_CONSUMER,
        RestConstants.ACCESS_CC_PUBLISH,
    ];
    inherited: boolean;
    private notifyUsers = true;
    private notifyMessage: string;
    private inherit: Permission[] = [];
    permissions: Permission[] = [];
    private originalPermissions: LocalPermissions[];
    private showChooseType = false;
    private showChooseTypeList: Permission;

    constructor(
        private nodeApi: RestNodeService,
        private translate: TranslateService,
        private collectionService: RestCollectionService,
        private applicationRef: ApplicationRef,
        private config: ConfigurationService,
        private toast: Toast,
        private usageApi: RestUsageService,
        private iam: RestIamService,
        private connector: RestConnectorService,
    ) {
        //this.dataService=new SearchData(iam);

        this.linkEnabled = new Permission();
        this.linkEnabled.authority = {
            authorityName: this.translate.instant(
                'WORKSPACE.SHARE.LINK_ENABLED_INFO',
            ),
            authorityType: 'LINK',
        };
        this.linkEnabled.permissions = [RestConstants.PERMISSION_CONSUMER];
        this.linkDisabled = new Permission();
        this.linkDisabled.authority = {
            authorityName: this.translate.instant(
                'WORKSPACE.SHARE.LINK_DISABLED_INFO',
            ),
            authorityType: 'LINK',
        };
        this.linkDisabled.permissions = [];

        this.connector.isLoggedIn().subscribe((data: LoginResult) => {
            this.isSafe = data.currentScope != null;
            this.connector
                .hasToolPermission(
                    this.isSafe
                        ? RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SAFE
                        : RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH,
                )
                .subscribe((has: boolean) => (this.globalAllowed = has));
            this.connector
                .hasToolPermission(
                    RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_FUZZY,
                )
                .subscribe((has: boolean) => (this.fuzzyAllowed = has));
            this.connector
                .hasToolPermission(
                    RestConstants.TOOLPERMISSION_INVITE_ALLAUTHORITIES,
                )
                .subscribe((has: boolean) => (this.publishPermission = has));
        });
    }

    isCollection() {
        if (this._nodes == null) return true;
        return (
            this._nodes[0].aspects.indexOf(
                RestConstants.CCM_ASPECT_COLLECTION,
            ) !== -1
        );
    }

    openLink() {
        this.linkNode = this._nodes[0];
    }

    addSuggestion(data: any) {
        this.addAuthority(data);
    }

    setNodes(nodes: Node[]) {
        this._nodes = nodes;
        if (nodes == null || nodes.length === 1 && !nodes[0].ref.id) {
            return;
        }
        this.refresh();
    }
    refresh() {
        const isDirectory = new Set(this._nodes.map(n => n.isDirectory));
        if (isDirectory.size !== 1) {
            this.toast.error(
                null,
                'WORKSPACE.SHARE.ERROR_INVALID_TYPE_COMBINATION',
            );
            this.cancel();
            return;
        }
        if (isDirectory.values().next()) {
            this.currentType = [RestConstants.ACCESS_CONSUMER];
        }
        if (this.currentPermissions) {
            this.originalPermissions = Helper.deepCopy(this.currentPermissions);
            this.setPermissions(this.currentPermissions.permissions);
            this.inherited = this.currentPermissions.inherited;
            this.showLink = false;
        } else {
            this.showLink = true;
            this.updateNodeLink();
            this.toast.showProgressDialog();
            Observable.forkJoin(
                this._nodes.map(n => this.nodeApi.getNodePermissions(n.ref.id)),
            ).subscribe(permissions => {
                this.originalPermissions = Helper.deepCopy(
                    permissions.map(p => p.permissions.localPermissions),
                );
                if (permissions.length === 1 && permissions[0].permissions) {
                    //this.originalPermissions=Helper.deepCopy(permissions[0].permissions.localPermissions);
                    this.setPermissions(
                        permissions[0].permissions.localPermissions.permissions,
                    );
                    this.inherited =
                        permissions[0].permissions.localPermissions.inherited;
                    this.setInitialState();
                }
                this.toast.closeModalDialog();
            });
            this.reloadUsages();
        }
        if (this._nodes.length === 1 && this._nodes[0].parent && this._nodes[0].parent.id) {
            this.nodeApi.getNodePermissions(this._nodes[0].parent.id).subscribe(
                (data: NodePermissions) => {
                    if (data.permissions) {
                        this.inherit = data.permissions.inheritedPermissions;
                        this.removePermissions(this.inherit, 'OWNER');
                        this.removePermissions(
                            data.permissions.localPermissions.permissions,
                            'OWNER',
                        );
                        this.inherit = UIHelper.mergePermissions(
                            this.inherit,
                            data.permissions.localPermissions.permissions,
                        );
                        this.initialState = this.getState();
                    }
                },
                (error: any) => {
                    this.inheritAccessDenied = true;
                },
            );
            this.nodeApi.getNodeParents(this._nodes[0].ref.id).subscribe(
                (data: NodeList) => {
                    //this.inheritAllowed = !this.isCollection() && data.nodes.length > 1;
                    // changed in 4.1 to keep inherit state of collections
                    this.inheritAllowed = data.nodes.length > 1;
                },
                error => {
                    // this can be caused if the node is somewhere at a location not fully visible to the user
                    this.inheritAllowed = true;
                },
            );
            if (this._nodes[0].ref.id) {
                this.nodeApi
                    .getNodeMetadata(this._nodes[0].ref.id, [RestConstants.ALL])
                    .subscribe((data: NodeWrapper) => {
                        let authority =
                            data.node.properties[RestConstants.CM_CREATOR][0];
                        let user = data.node.createdBy;

                        if (data.node.properties[RestConstants.CM_OWNER]) {
                            authority =
                                data.node.properties[RestConstants.CM_OWNER][0];
                            user = data.node.owner;
                        }
                        this.owner = new Permission();
                        this.owner.authority = {
                            authorityName: authority,
                            authorityType: 'USER',
                        };
                        (this.owner as any).user = user;
                        this.iam.getUser(authority).subscribe((apiUser) => {
                            (this.owner as any).user = apiUser.person.profile;
                            // force a refresh of the data for ui update
                            this.owner = Helper.deepCopy(this.owner);
                        }, (error) => {
                            // ignore, only relevant for the avatar of the owner
                        });
                        this.owner.permissions = ['Owner'];
                    });
            }
        }
        this.connector.isLoggedIn().subscribe((data: LoginResult) => {
            this.isAdmin = data.isAdmin;
        });
    }

    isDeleted(p: Permission) {
        return this.deletedPermissions.indexOf(p.authority.authorityName) != -1;
    }

    cancel() {
        this.onClose.emit();
    }

    hasUsages() {
        return this.usages && Object.keys(this.usages).length;
    }

    showHistory() {
        this.history = this._nodes[0];
    }

    filterDisabledPermissions(permissions: Permission[]) {
        let result: Permission[] = [];
        if (!permissions) return result;
        for (let p of permissions) {
            if (
                this.deletedPermissions.indexOf(p.authority.authorityName) == -1
            )
                result.push(p);
        }
        return result;
    }

    setPermission(permission: Permission, name: string, status: any) {
        if (status.checked) {
            if (permission.permissions.indexOf(name) == -1)
                permission.permissions.push(name);
        } else {
            let index = permission.permissions.indexOf(name);
            if (index != -1) {
                permission.permissions.splice(index, 1);
            }
        }
        this.applicationRef.tick();
    }

    isImplicitPermission(permission: Permission, name: string) {
        //if(name=="Consumer") // this is the default permission, can't be removed
        //  return true;
        if (name != 'All' && permission.permissions.indexOf('All') != -1)
            // coordinator implies all permissions
            return true;
        if (
            name != 'Coordinator' &&
            permission.permissions.indexOf('Coordinator') != -1
        )
            // coordinator implies all permissions
            return true;
        for (let array of this.PERMISSIONS_FORCES) {
            if (array[0] != name) continue;
            let list = array[1];
            if (!list) return false;
            let result = true;
            for (let perm of list) {
                if (perm == name) continue;
                if (this.hasImplicitPermission(permission, perm)) continue;
                result = false;
                break;
            }
            if (result) return true;
        }
        return false;
    }

    hasImplicitPermission(permission: Permission, name: string) {
        if (permission.permissions.indexOf(name) != -1) return true;
        return this.isImplicitPermission(permission, name);
    }

    updateNodeLink() {
        this.nodeApi
            .getNodeShares(this._nodes[0].ref.id, RestConstants.SHARE_LINK)
            .subscribe((data: NodeShare[]) => {
                this.link = data.length > 0 && data[0].expiryDate != 0;
            });
    }

    setPublish(status: boolean, force = false) {
        if(status && !force) {
            if (this.config.instant('publishingNotice', false)) {
                let cancel = () => {
                    this.toast.closeModalDialog();
                };
                this.toast.showModalDialog(
                    'WORKSPACE.SHARE.PUBLISHING_WARNING_TITLE',
                    'WORKSPACE.SHARE.PUBLISHING_WARNING_MESSAGE',
                    DialogButton.getYesNo(cancel, () => {
                        this.setPublish(status, true);
                        this.toast.closeModalDialog();
                    }),
                    true,
                    cancel,
                );
                return;
            }
        }
        if (this.deletedPermissions.indexOf(RestConstants.AUTHORITY_EVERYONE) !== -1) {
            this.deletedPermissions.splice(this.deletedPermissions.indexOf(RestConstants.AUTHORITY_EVERYONE),1);
        } else {
            let i = this.getAuthorityPos(this.permissions,RestConstants.AUTHORITY_EVERYONE);
            if (i !== -1) {
                this.deletedPermissions.push(RestConstants.AUTHORITY_EVERYONE);
            } else {
                const perm = RestHelper.getAllAuthoritiesPermission();
                perm.permissions = [RestConstants.PERMISSION_CONSUMER];
                this.permissions.push(perm);
            }
        }
        this.setPermissions(this.permissions);
    }

    reloadUsages() {
        this.usageApi
            .getNodeUsagesCollection(this._nodes[0].ref.id)
            .subscribe(collections => {
                this.collections = collections;
                this.usageApi
                    .getNodeUsages(this._nodes[0].ref.id)
                    .subscribe((usages: UsageList) => {
                        const filteredUsages = usages.usages.filter(
                            u =>
                                this.collections.filter(
                                    c => c.nodeId === u.nodeId,
                                ).length === 0,
                        );
                        this.usages = RestUsageService.getNodeUsagesByRepositoryType(
                            filteredUsages,
                        );
                    });
            });
    }

    openCollection(collection: Node) {
        window.open(
            UIConstants.ROUTER_PREFIX + 'collections?id=' + collection.ref.id,
        );
    }

    isStateModified() {
        return this.initialState != this.getState();
    }

    getState() {
        if (this.getPublishActive() || this.getPublishInherit()) {
            return 'PUBLIC';
        }
        for (const perm of this.permissions.concat(this.inherited ? this.inherit : [])) {
            if (
                perm.authority.authorityName !== RestConstants.AUTHORITY_EVERYONE
            )
                return 'SHARED';
        }
        return 'PRIVATE';
    }

    isBulk() {
        return this._nodes && this._nodes.length > 1;
    }

    showShareLink() {
        return (
            !this.isCollection() &&
            this.connector.hasToolPermissionInstant(
                RestConstants.TOOLPERMISSION_INVITE_LINK,
            )
        );
    }

    updateButtons() {
        const saveButton = this.buttons[1];
        saveButton.name = this.tab == 0 ? 'WORKSPACE.BTN_INVITE' : 'APPLY';
    }

    private chooseType() {
        this.showChooseType = true;
    }

    private chooseTypeList(p: Permission) {
        this.showChooseTypeList = p;
    }

    private removePermission(p: Permission) {
        if (this.isDeleted(p))
            this.deletedPermissions.splice(
                this.deletedPermissions.indexOf(p.authority.authorityName),
                1,
            );
        else this.deletedPermissions.push(p.authority.authorityName);
        /*
      if(this.newPermissions.indexOf(p)!=-1)
      this.newPermissions.splice(this.newPermissions.indexOf(p),1);
    this.permissions.splice(this.permissions.indexOf(p),1);
    this.setPermissions(this.permissions);
    */
    }

    private setType(type: any) {
        this.currentType = type.permissions;
        if (type.wasMain) this.showChooseType = false;
        for (let permission of this.newPermissions) {
            permission.permissions = Helper.deepCopy(this.currentType);
        }
    }

    private contains(
        permissions: Permission[],
        permission: Permission,
        comparePermissions: boolean,
    ): boolean {
        for (let p of permissions) {
            if (
                p.authority.authorityName == permission.authority.authorityName
            ) {
                if (!comparePermissions) return true;
                if (Helper.arrayEquals(p.permissions, permission.permissions))
                    return true;
            }
        }
        return false;
    }

    private addAuthority(selected: any) {
        if (selected == null) return;
        let permission: any = new Permission();
        permission.authority = {
            authorityName: selected.authorityName,
            authorityType: selected.authorityType,
        };
        if (selected.authorityType == 'USER') {
            permission.user = selected.profile;
        } else {
            permission.group = selected.profile;
        }
        permission.permissions = this.currentType;
        permission = Helper.deepCopy(permission);
        if (
            this.deletedPermissions.indexOf(
                permission.authority.authorityName,
            ) != -1
        ) {
            this.deletedPermissions.splice(
                this.deletedPermissions.indexOf(
                    permission.authority.authorityName,
                ),
                1,
            );
        } else if (!this.contains(this.permissions, permission, false)) {
            this.newPermissions.push(permission);
            this.permissions.push(permission);
            this.setPermissions(this.permissions);
        } else this.toast.error(null, 'WORKSPACE.PERMISSION_AUTHORITY_EXISTS');
        this.searchStr = '';
    }

    private isNewPermission(p: Permission) {
        if (
            !this.originalPermissions?.length ||
            !this.originalPermissions[0].permissions
        )
            return true;
        return !this.contains(this.originalPermissions[0].permissions, p, true);
    }

    async save() {
        if (this.permissions != null) {
            this.onLoading.emit(true);
            let inherit =
                this.inherited &&
                this.inheritAllowed &&
                !this.isCollection();
            const actions: Observable<void>[] = this._nodes.map((n, i) => {
                return new Observable((observer: Observer<void>) => {
                    let permissions: Permission[] = Helper.deepCopy(this.permissions);
                    if (this.isBulk()) {
                        // keep inherit state of original node
                        inherit = this.originalPermissions[i].inherited;
                        if (this.bulkMode === 'extend') {
                            permissions = UIHelper.mergePermissionsWithHighestPermission(
                                this.originalPermissions[i].permissions,
                                permissions,
                            );
                        } else {
                            // we do nothing, because theo original ones are getting deleted
                        }
                    }
                    permissions = permissions.filter(
                        (p: Permission) => !this.isDeleted(p),
                    );
                    // handle the invitation of group everyone
                    if (this.publishComponent) {
                        permissions = this.publishComponent.updatePermissions(permissions);
                        this.publishComponent.save().subscribe((nodeCopy) => {
                            this.handlePermissionsPerNode(observer, n, permissions, inherit);
                        }, error => {
                            this.toast.error(error)
                            this.onLoading.emit(false);
                        });
                    } else {
                        this.handlePermissionsPerNode(observer, n, permissions, inherit);
                    }
                })
            });
            if(!this.sendToApi) {
                return;
            }
            Observable.forkJoin(actions).subscribe(
                () => {
                    this.updateUsages(
                        RestHelper.copyPermissions(
                            Helper.deepCopy(this.permissions),
                            inherit,
                        ),
                    );
                },
                (error: any) => {
                    this.toast.error(error);
                    this.onLoading.emit(false);
                },
            );
        }
    }

    private updatePermissionInfo() {
        let type: string[];
        for (let permission of this.newPermissions) {
            if (type && !Helper.arrayEquals(type, permission.permissions)) {
                this.currentType = [];
                return;
            }
            type = permission.permissions;
        }
        if (type) this.currentType = type;
    }

    private removePermissions(permissions: Permission[], remove: string) {
        for (let i = 0; i < remove.length; i++) {
            if (
                permissions[i] &&
                permissions[i].authority.authorityType == remove
            ) {
                permissions.splice(i, 1);
                i--;
            }
        }
    }

    private setPermissions(permissions: Permission[]) {
        if (permissions == null) permissions = [];
        this.permissions = permissions;
        this.permissionsUser = this.permissions.slice();
        this.permissionsGroup = this.permissions.slice();
        this.removePermissions(
            this.permissionsUser,
            RestConstants.AUTHORITY_TYPE_GROUP,
        );
        this.removePermissions(
            this.permissionsUser,
            RestConstants.AUTHORITY_TYPE_EVERYONE,
        );
        this.removePermissions(
            this.permissionsGroup,
            RestConstants.AUTHORITY_TYPE_USER,
        );
    }
    getPublishInherit() {
        return this.inherited &&
            this.getAuthorityPos(
                this.inherit,
                RestConstants.AUTHORITY_EVERYONE,
            ) !== -1;
    }
    getPublishActive() {
        return this.getPublishInherit() ||
            this.publishComponent?.shareMode != null;
    }

    private getAuthorityPos(permissions: Permission[], authority: string) {
        let i = 0;
        for (let permission of permissions) {
            if (permission.authority.authorityName == authority) return i;
            i++;
        }
        return -1;
    }

    private updateUsages(
        permissions: LocalPermissions,
        pos = 0,
        error = false,
    ) {
        // skip for bulk mode
        if (pos === this.deletedUsages.length || this.isBulk()) {
            this.onLoading.emit(false);
            this.onClose.emit(this.getEmitObject(permissions));
            if (!error) {
                this.toast.toast('WORKSPACE.PERMISSIONS_UPDATED');
            }
            return;
        }
        let usage = this.deletedUsages[pos];
        // collection
        if (usage.collection) {
            this.collectionService
                .removeFromCollection(usage.resourceId, usage.collection.ref.id)
                .subscribe(
                    () => {
                        this.updateUsages(permissions, pos + 1);
                    },
                    error => {
                        this.toast.error(error);
                        this.updateUsages(permissions, pos + 1, true);
                    },
                );
        } else {
            this.usageApi
                .deleteNodeUsage(this._nodes[0].ref.id, usage.nodeId)
                .subscribe(
                    () => {
                        this.updateUsages(permissions, pos + 1);
                    },
                    error => {
                        this.toast.error(error);
                        this.updateUsages(permissions, pos + 1, true);
                    },
                );
        }
    }

    private getEmitObject(localPermissions: LocalPermissions) {
        return {
            permissions: localPermissions,
            notify: this.notifyUsers,
            notifyMessage: this.notifyMessage,
        };
    }

    private handlePermissionsPerNode(observer: Observer<void>, n: Node, permissions: Permission[], inherit: boolean): void {
        const permissionsCopy = RestHelper.copyAndCleanPermissions(
            permissions,
            inherit,
        );
        if (!this.sendToApi) {
            this.onClose.emit(
                this.getEmitObject(
                    RestHelper.copyPermissions(permissions, inherit),
                ),
            );
            return null;
        }
        this.nodeApi.setNodePermissions(
            n.ref.id,
            permissionsCopy,
            this.notifyUsers && this.sendMessages,
            this.notifyMessage,
            false,
            false, // handle will always be created via publish component
        ).subscribe(() => {
            observer.next();
            observer.complete();
        }, error => {
            observer.error(error);
            observer.complete();
        });
    }

    private setInitialState() {
        this.initialState = this.getState();
    }
}
/*
class SearchData extends Subject<CompleterItem[]> implements CompleterData {
  constructor(private iam: RestIamService) {
    super();
  }

  public search(term: string): void {
    this.iam.searchUsers(term).subscribe((data : IamUsers)=>{
      let matches:CompleterItem[]=[];
      for(let user of data.users){
        matches.push({
          title: user.authorityName,
          description: null,
          originalObject:user
        });
      }
      this.iam.searchGroups(term).subscribe((data : IamGroups)=>{
        for(let user of data.groups){
          matches.push({
            title: user.profile.displayName,
            description: null,
            originalObject:user
          });
        }
        this.next(matches);
    })

    })
  }

  public cancel() {
    // Handle cancel
  }
}
*/
