import { Component, EventEmitter, NgZone, Output, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Group, IamGroup, Mediacenter, Node } from '../../../core-module/rest/data-object';
// import {NodeList} from "../../../core-module/core.module";
import { ListItem } from '../../../core-module/ui/list-item';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { RestHelper } from '../../../core-module/rest/rest-helper';
import {
    DialogButton,
    RequestObject,
    RestConnectorService,
    RestIamService,
    RestMdsService,
    RestMediacenterService,
    RestSearchService,
} from '../../../core-module/core.module';
import { Helper } from '../../../core-module/rest/helper';
import { Toast } from '../../../core-ui-module/toast';
import { CustomOptions, ElementType, OptionItem } from '../../../core-ui-module/option-item';
import { MdsHelper } from '../../../core-module/rest/mds-helper';
import { AuthoritySearchMode } from '../../../shared/components/authority-search-input/authority-search-input.component';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { MdsEditorWrapperComponent } from '../../../features/mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';

// Charts.js
declare var Chart: any;

@Component({
    selector: 'es-admin-mediacenter',
    templateUrl: 'mediacenter.component.html',
    styleUrls: ['mediacenter.component.scss'],
})
export class AdminMediacenterComponent {
    readonly AuthoritySearchMode = AuthoritySearchMode;
    @ViewChild('mediacenterMds') mediacenterMds: MdsEditorWrapperComponent;
    @Output() onOpenNode = new EventEmitter();
    // @TODO: declare the mediacenter type when it is finalized in backend
    mediacenters: any[];
    // original link to mediacenter object (contained in mediacenters[])
    currentMediacenter: Mediacenter;
    // copy of the current mediacenter for (temporary) edits
    currentMediacenterCopy: Mediacenter;

    addGroup: Group;
    mediacenterGroups: IamGroup[];
    mediacenterNodes: Node[];
    mediacenterNodesTotal = 0;
    mediacenterNodesSearchWord = '';
    hasMoreMediacenterNodes = true;
    isLoadingMediacenterNodes = false;
    mediacenterNodesSort = {
        sortBy: RestConstants.LOM_PROP_TITLE,
        sortAscending: true,
    };

    groupColumns: ListItem[];
    nodeColumns: ListItem[];
    groupActions: CustomOptions = {
        useDefaultOptions: false,
    };
    currentTab = 0;
    isAdmin: boolean;
    hasManagePermissions: boolean;
    public mediacentersFile: File;
    public organisationsFile: File;
    public orgMcFile: File;
    public globalProgress = false;

    public removeSchoolsFromMC = false;

    constructor(
        private mediacenterService: RestMediacenterService,
        private mdsService: RestMdsService,
        private translate: TranslateService,
        private connector: RestConnectorService,
        private iamService: RestIamService,
        private ngZone: NgZone,
        private toast: Toast,
    ) {
        this.isAdmin = this.connector.getCurrentLogin().isAdmin;
        this.hasManagePermissions = this.connector.hasToolPermissionInstant(
            RestConstants.TOOLPERMISSION_MEDIACENTER_MANAGE,
        );
        this.refresh();
        this.mdsService.getSet().subscribe((mds) => {
            this.nodeColumns = MdsHelper.getColumns(this.translate, mds, 'mediacenterManaged');
            this.groupColumns = MdsHelper.getColumns(this.translate, mds, 'mediacenterGroups');
        });
        const remove = new OptionItem(
            'ADMIN.MEDIACENTER.GROUPS.REMOVE',
            'delete',
            (authority: Group) => {
                this.toast.showModalDialog(
                    'ADMIN.MEDIACENTER.GROUPS.REMOVE_TITLE',
                    'ADMIN.MEDIACENTER.GROUPS.REMOVE_MESSAGE',
                    DialogButton.getYesNo(
                        () => this.toast.closeModalDialog(),
                        () => {
                            this.toast.closeModalDialog();
                            this.deleteGroup(authority);
                        },
                    ),
                    true,
                    () => this.toast.closeModalDialog(),
                    { name: authority.profile.displayName },
                );
            },
        );
        remove.elementType = [ElementType.Group];
        if (this.isAdmin) {
            this.groupActions.addOptions = [remove];
        }
    }

    setMediacenter(mediacenter: any) {
        this.currentMediacenter = mediacenter;
        this.currentMediacenterCopy = Helper.deepCopy(mediacenter);
        this.mediacenterGroups = null;

        this.resetMediacenterNodes();

        if (mediacenter) {
            this.mediacenterService
                .getManagedGroups(mediacenter.authorityName)
                .subscribe((groups) => {
                    this.mediacenterGroups = groups;
                });

            this.mediacenterNodesTotal = 0;
            this.mediacenterNodes = [];
            UIHelper.waitForComponent(this.ngZone, this, 'mediacenterMds').subscribe(() =>
                this.mediacenterMds.loadMds(),
            );
            // done via mds
            // this.loadMediacenterNodes();
        }
    }

    async loadMediacenterNodes() {
        if (!this.hasMoreMediacenterNodes) {
            return;
        }
        if (this.currentMediacenter) {
            const licensedNodeReq: RequestObject = {
                offset: this.mediacenterNodes?.length,
                count: this.mediacenterNodes?.length ? 50 : null,
                propertyFilter: [RestConstants.ALL],
                sortBy: [this.mediacenterNodesSort.sortBy],
                sortAscending: [this.mediacenterNodesSort.sortAscending],
            };
            this.isLoadingMediacenterNodes = true;

            let criteria: any = [];
            if (this.mediacenterNodesSearchWord) {
                criteria.push({
                    property: RestConstants.PRIMARY_SEARCH_CRITERIA,
                    values: [this.mediacenterNodesSearchWord],
                });
            }
            criteria = criteria.concat(
                RestSearchService.convertCritierias(
                    await this.mediacenterMds.getValues(),
                    this.mediacenterMds.currentWidgets,
                ),
            );

            this.mediacenterService
                .getLicensedNodes(
                    this.currentMediacenter.authorityName,
                    criteria,
                    RestConstants.HOME_REPOSITORY,
                    licensedNodeReq,
                )
                .subscribe((data) => {
                    this.mediacenterNodesTotal = data.pagination.total;
                    if (
                        this.mediacenterNodes == null ||
                        (this.mediacenterNodesSearchWord != null &&
                            this.mediacenterNodesSearchWord.trim().length > 0)
                    ) {
                        this.mediacenterNodes = data.nodes;
                    } else {
                        this.mediacenterNodes = this.mediacenterNodes.concat(data.nodes);
                    }
                    this.hasMoreMediacenterNodes =
                        this.mediacenterNodes.length < this.mediacenterNodesTotal;
                    this.isLoadingMediacenterNodes = false;
                });
        }
    }

    searchMediaCenterNodes() {
        this.hasMoreMediacenterNodes = true;
        this.mediacenterNodes = [];
        this.loadMediacenterNodes();
    }

    removeCatalog(catalog: any) {
        this.currentMediacenterCopy.profile.mediacenter.catalogs.splice(
            this.currentMediacenterCopy.profile.mediacenter.catalogs.indexOf(catalog),
            1,
        );
    }

    addCatalog() {
        if (!this.currentMediacenterCopy.profile.mediacenter.catalogs) {
            this.currentMediacenterCopy.profile.mediacenter.catalogs = [];
        }
        this.currentMediacenterCopy.profile.mediacenter.catalogs.push({ name: '', url: '' });
    }

    addMediacenter() {
        this.toast.showInputDialog(
            'ADMIN.MEDIACENTER.ADD_MEDIACENTER_TITLE',
            'ADMIN.MEDIACENTER.ADD_MEDIACENTER_MESSAGE',
            'ADMIN.MEDIACENTER.ADD_MEDIACENTER_LABEL',
            DialogButton.getOkCancel(
                () => this.toast.closeModalDialog(),
                () => {
                    const id = this.toast.dialogInputValue;
                    const profile = {
                        displayName: this.translate.instant(
                            'ADMIN.MEDIACENTER.UNNAMED_MEDIACENTER',
                            { id },
                        ),
                        mediacenter: {
                            id,
                        },
                    };
                    this.toast.showProgressDialog();
                    this.mediacenterService.addMediacenter(id, profile).subscribe(
                        (result) => {
                            RestHelper.waitForResult(
                                () => this.mediacenterService.getMediacenters(),
                                (list: Mediacenter[]) => {
                                    return (
                                        list.filter((r) => r.authorityName === result.authorityName)
                                            .length === 1
                                    );
                                },
                                () => {
                                    this.toast.closeModalDialog();
                                    this.toast.toast('ADMIN.MEDIACENTER.CREATED', { name: id });
                                    this.setMediacenter(null);
                                    this.refresh();
                                },
                            );
                        },
                        (error: any) => {
                            this.toast.error(error);
                            this.toast.closeModalDialog();
                        },
                    );
                },
            ),
            true,
            () => this.toast.closeModalDialog(),
        );
    }

    saveChanges() {
        this.toast.showProgressDialog();
        this.mediacenterService
            .editMediacenter(
                this.currentMediacenterCopy.authorityName,
                this.currentMediacenterCopy.profile,
            )
            .subscribe(
                () => {
                    this.toast.toast('ADMIN.MEDIACENTER.UPDATED', {
                        name: this.currentMediacenterCopy.profile.displayName,
                    });
                    this.toast.closeModalDialog();
                    this.refresh();
                },
                (error: any) => {
                    this.toast.error(error);
                    this.toast.closeModalDialog();
                    this.refresh();
                },
            );
    }

    refresh() {
        this.mediacenters = null;
        this.mediacenterService.getMediacenters().subscribe((m) => {
            this.mediacenters = m.filter((m) => m.administrationAccess);
        });
    }

    addCurrentGroup() {
        this.toast.showProgressDialog();
        this.mediacenterService
            .addManagedGroup(this.currentMediacenterCopy.authorityName, this.addGroup.authorityName)
            .subscribe(
                (groups) => {
                    this.mediacenterGroups = groups;
                    this.toast.toast('ADMIN.MEDIACENTER.GROUPS.ADDED', {
                        name: this.addGroup.profile.displayName,
                    });
                    this.toast.closeModalDialog();
                    this.addGroup = null;
                },
                (error) => {
                    this.toast.error(error);
                    this.toast.closeModalDialog();
                },
            );
    }

    deleteMediacenter() {
        this.toast.showModalDialog(
            'ADMIN.MEDIACENTER.DELETE_TITLE',
            'ADMIN.MEDIACENTER.DELETE_MESSAGE',
            DialogButton.getYesNo(
                () => this.toast.closeModalDialog(),
                () => {
                    this.toast.showProgressDialog();
                    this.mediacenterService
                        .deleteMediacenter(this.currentMediacenter.authorityName)
                        .subscribe(
                            () => {
                                this.toast.closeModalDialog();
                                this.toast.toast('ADMIN.MEDIACENTER.DELETED', {
                                    name: this.currentMediacenterCopy.profile.displayName,
                                });
                                this.setMediacenter(null);
                                this.refresh();
                            },
                            (error: any) => {
                                this.toast.error(error);
                                this.toast.closeModalDialog();
                            },
                        );
                },
            ),
            true,
            () => this.toast.closeModalDialog(),
            { name: this.currentMediacenterCopy.profile.displayName },
        );
    }

    private deleteGroup(authority: Group) {
        this.toast.showProgressDialog();
        this.mediacenterService
            .removeManagedGroup(this.currentMediacenterCopy.authorityName, authority.authorityName)
            .subscribe(
                (groups) => {
                    this.mediacenterGroups = groups;
                    this.toast.toast('ADMIN.MEDIACENTER.GROUPS.REMOVED', {
                        name: authority.profile.displayName,
                    });
                    this.toast.closeModalDialog();
                },
                (error) => {
                    this.toast.error(error);
                    this.toast.closeModalDialog();
                },
            );
    }

    public updateMediacentersFile(event: any) {
        this.mediacentersFile = event.target.files[0];
    }

    public updateOrganisationsFile(event: any) {
        this.organisationsFile = event.target.files[0];
    }

    public updateOrgMcFile(event: any) {
        this.orgMcFile = event.target.files[0];
    }

    public importMediacenters() {
        if (!this.mediacentersFile) {
            this.toast.error(null, 'ADMIN.MEDIACENTER.IMPORT.CHOOSE_MEDIACENTERS');
            return;
        }
        this.globalProgress = true;
        this.mediacenterService.importMediacenters(this.mediacentersFile).subscribe(
            (data: any) => {
                this.toast.toast('ADMIN.MEDIACENTER.IMPORT.IMPORTED', { rows: data.rows });
                this.globalProgress = false;
                this.mediacentersFile = null;
            },
            (error: any) => {
                this.toast.error(error);
                this.globalProgress = false;
            },
        );
    }

    public importOrganisations() {
        if (!this.organisationsFile) {
            this.toast.error(null, 'ADMIN.MEDIACENTER.ORGIMPORT.CHOOSE_ORGANISATIONS');
            return;
        }
        this.globalProgress = true;
        this.mediacenterService.importOrganisations(this.organisationsFile).subscribe(
            (data: any) => {
                this.toast.toast('ADMIN.MEDIACENTER.ORGIMPORT.IMPORTED', { rows: data.rows });
                this.globalProgress = false;
                this.organisationsFile = null;
            },
            (error: any) => {
                this.toast.error(error);
                this.globalProgress = false;
            },
        );
    }

    // importMcOrgConnections
    public importOrgMc() {
        if (!this.orgMcFile) {
            this.toast.error(null, 'ADMIN.MEDIACENTER.ORG_MC_CONNECT.CHOOSE');
            return;
        }
        this.globalProgress = true;
        this.mediacenterService
            .importMcOrgConnections(this.orgMcFile, this.removeSchoolsFromMC)
            .subscribe(
                (data: any) => {
                    this.toast.toast('ADMIN.MEDIACENTER.ORG_MC_CONNECT.IMPORTED', {
                        rows: data.rows,
                    });
                    this.globalProgress = false;
                    this.orgMcFile = null;
                },
                (error: any) => {
                    this.toast.error(error);
                    this.globalProgress = false;
                },
            );
    }

    setMediacenterNodesSort(sort: { sortBy: string; sortAscending: boolean }) {
        this.mediacenterNodesSort = sort;
        this.resetMediacenterNodes();
        this.loadMediacenterNodes();
    }

    private resetMediacenterNodes() {
        this.mediacenterNodes = null;
        this.mediacenterNodesTotal = 0;
        this.hasMoreMediacenterNodes = true;
    }
}
