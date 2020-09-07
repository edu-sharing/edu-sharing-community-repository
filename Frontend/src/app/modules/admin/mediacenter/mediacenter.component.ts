import {RestAdminService} from '../../../core-module/rest/services/rest-admin.service';
import {Component, EventEmitter, Output, ViewChild} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {
    NodeStatistics,
    Node,
    Statistics,
    IamGroup,
    Group,
    NodeList,
    Mediacenter
} from '../../../core-module/rest/data-object';
// import {NodeList} from "../../../core-module/core.module";
import {ListItem} from '../../../core-module/ui/list-item';
import {RestConstants} from '../../../core-module/rest/rest-constants';
import {RestHelper} from '../../../core-module/rest/rest-helper';
import {NodeHelper} from '../../../core-ui-module/node-helper';
import {ConfigurationService} from '../../../core-module/rest/services/configuration.service';
import {DialogButton, RestConnectorService, RestIamService, RestMdsService, RestMediacenterService, RestSearchService} from '../../../core-module/core.module';
import {Helper} from '../../../core-module/rest/helper';
import {Toast} from '../../../core-ui-module/toast';
import {OptionItem} from '../../../core-ui-module/option-item';
import {MdsComponent} from '../../../common/ui/mds/mds.component';
import {MdsHelper} from '../../../core-module/rest/mds-helper';
import {UIHelper} from '../../../core-ui-module/ui-helper';

// Charts.js
declare var Chart: any;

@Component({
    selector: 'app-admin-mediacenter',
    templateUrl: 'mediacenter.component.html',
    styleUrls: ['mediacenter.component.scss']
})
export class AdminMediacenterComponent {
    @ViewChild('mediacenterMds') mediacenterMds: MdsComponent;
    // @TODO: declare the mediacenter type when it is finalized in backend
    mediacenters: any[];
    // original link to mediacenter object (contained in mediacenters[])
    currentMediacenter: Mediacenter;
    // copy of the current mediacenter for (temporary) edits
    currentMediacenterCopy: Mediacenter;

    addGroup: Group;
    mediacenterGroups: IamGroup[];
    mediacenterNodes: Node[];
    mediacenterNodesMax = 20;
    mediacenterNodesOffset = 0;
    mediacenterNodesTotal = 0;
    mediacenterNodesSearchWord='';
    hasMoreMediacenterNodes = true;
    isLoadingMediacenterNodes=false;


    groupColumns: ListItem[];
    nodeColumns: ListItem[];
    groupActions: OptionItem[];
    currentTab = 0;
    mediacenterMdsReload = new Boolean(true);
    private isAdmin: boolean;
    private hasManagePermissions: boolean;
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
        private toast: Toast,
    ) {
        this.isAdmin = this.connector.getCurrentLogin().isAdmin;
        this.hasManagePermissions = this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_MEDIACENTER_MANAGE);
        this.refresh();
        this.groupColumns = [
            new ListItem('GROUP', RestConstants.AUTHORITY_DISPLAYNAME),
            new ListItem('GROUP', RestConstants.AUTHORITY_GROUPTYPE)
        ];
        this.mdsService.getSet().subscribe((mds) => {
            this.nodeColumns = MdsHelper.getColumns(this.translate, mds, 'mediacenterManaged');
        });

        this.groupActions = [
            new OptionItem('ADMIN.MEDIACENTER.GROUPS.REMOVE', 'delete', (authority: Group) => {
                this.toast.showModalDialog('ADMIN.MEDIACENTER.GROUPS.REMOVE_TITLE', 'ADMIN.MEDIACENTER.GROUPS.REMOVE_MESSAGE',
                    DialogButton.getYesNo(() => this.toast.closeModalDialog(), () => {
                        this.toast.closeModalDialog();
                        this.deleteGroup(authority)
                    }), true, () => this.toast.closeModalDialog(), {name: authority.profile.displayName});
            })
        ];
    }


    setMediacenter(mediacenter: any) {
        this.currentMediacenter = mediacenter;
        this.currentMediacenterCopy = Helper.deepCopy(mediacenter);
        this.mediacenterGroups = null;

        this.mediacenterNodes = null;
        this.mediacenterNodesTotal = 0;
        this.mediacenterNodesOffset = 0;
        this.hasMoreMediacenterNodes = true

        if (mediacenter) {
            this.mediacenterService.getManagedGroups(mediacenter.authorityName).subscribe((groups) => {
                this.mediacenterGroups = groups;
            });

            this.mediacenterNodesOffset = 0;
            this.mediacenterNodesTotal = 0;
            this.mediacenterNodes = [];
            this.mediacenterMdsReload = new Boolean(true);
            // done via mds
            // this.loadMediacenterNodes();

        }
    }

    loadMediacenterNodes() {
        if (!this.hasMoreMediacenterNodes) {
            return;
        }
        if (this.currentMediacenter) {
            const licensedNodeReq = {
                offset: this.mediacenterNodesOffset,
                count: this.mediacenterNodesMax,
                propertyFilter: [RestConstants.ALL]
            };
            this.isLoadingMediacenterNodes = true;

            let criterias: any = [];
            if (this.mediacenterNodesSearchWord) {
                criterias.push({
                    property: RestConstants.PRIMARY_SEARCH_CRITERIA,
                    values: [this.mediacenterNodesSearchWord],
                });
            }
            criterias = criterias.concat(
                RestSearchService.convertCritierias(
                    this.mediacenterMds.getValues(),
                    this.mediacenterMds.currentWidgets,
                ),
            );
            console.log(this.mediacenterMds.getValues())

            this.mediacenterService.getLicensedNodes(this.currentMediacenter.authorityName, criterias,
                RestConstants.HOME_REPOSITORY, null, licensedNodeReq).subscribe((data) => {
                this.mediacenterNodesTotal = data.pagination.total;
                if (this.mediacenterNodesTotal < (this.mediacenterNodesOffset + this.mediacenterNodesMax)) {
                    this.hasMoreMediacenterNodes = false;
                } else {
                    this.mediacenterNodesOffset = data.pagination.from + this.mediacenterNodesMax;
                }
                if (this.mediacenterNodes == null
                    || (this.mediacenterNodesSearchWord != null && this.mediacenterNodesSearchWord.trim().length > 0)) {
                    this.mediacenterNodes = data.nodes;
                } else {

                    this.mediacenterNodes = this.mediacenterNodes.concat(data.nodes);
                }
                this.isLoadingMediacenterNodes=false;
            });

        }
    }

    searchMediaCenterNodes() {
        this.hasMoreMediacenterNodes = true;
        this.mediacenterNodesOffset = 0;
        this.mediacenterNodes = [];
        this.loadMediacenterNodes()
    }

    removeCatalog(catalog: any) {
        this.currentMediacenterCopy.profile.mediacenter.catalogs.splice(this.currentMediacenterCopy.profile.mediacenter.catalogs.indexOf(catalog), 1);
    }

    addCatalog() {
        if (!this.currentMediacenterCopy.profile.mediacenter.catalogs) {
            this.currentMediacenterCopy.profile.mediacenter.catalogs = [];
        }
        this.currentMediacenterCopy.profile.mediacenter.catalogs.push({name: '', url: ''});
    }

    addMediacenter() {
        this.toast.showInputDialog('ADMIN.MEDIACENTER.ADD_MEDIACENTER_TITLE', 'ADMIN.MEDIACENTER.ADD_MEDIACENTER_MESSAGE', 'ADMIN.MEDIACENTER.ADD_MEDIACENTER_LABEL',
            DialogButton.getOkCancel(() => this.toast.closeModalDialog(), () => {
                const id = this.toast.dialogInputValue;
                const profile = {
                    displayName: this.translate.instant('ADMIN.MEDIACENTER.UNNAMED_MEDIACENTER', {id}),
                    mediacenter: {
                        id
                    }
                };
                this.toast.showProgressDialog();
                this.mediacenterService.addMediacenter(id, profile).subscribe((result) => {
                    RestHelper.waitForResult(() => this.mediacenterService.getMediacenters(), (list: Mediacenter[]) => {
                        return list.filter((r) => r.authorityName === result.authorityName).length === 1;
                    }, () => {
                        this.toast.closeModalDialog();
                        this.toast.toast('ADMIN.MEDIACENTER.CREATED', {name: id});
                        this.setMediacenter(null);
                        this.refresh();
                    });
                }, (error: any) => {
                    this.toast.error(error);
                    this.toast.closeModalDialog();
                })
            }),
            true, () => this.toast.closeModalDialog());
    }

    saveChanges() {
        this.toast.showProgressDialog();
        this.mediacenterService.editMediacenter(this.currentMediacenterCopy.authorityName, this.currentMediacenterCopy.profile).subscribe(() => {
            this.toast.toast('ADMIN.MEDIACENTER.UPDATED', {name: this.currentMediacenterCopy.profile.displayName});
            this.toast.closeModalDialog();
            this.refresh();
        }, (error: any) => {
            this.toast.error(error);
            this.toast.closeModalDialog();
            this.refresh();
        })
    }

    refresh() {
        this.mediacenters = null;
        this.mediacenterService.getMediacenters().subscribe((m) => {
            this.mediacenters = m.filter((m) => m.administrationAccess);
        });
    }

    addCurrentGroup() {
        this.toast.showProgressDialog();
        this.mediacenterService.addManagedGroup(this.currentMediacenterCopy.authorityName, this.addGroup.authorityName).subscribe((groups) => {
            this.mediacenterGroups = groups;
            this.toast.toast('ADMIN.MEDIACENTER.GROUPS.ADDED', {name: this.addGroup.profile.displayName});
            this.toast.closeModalDialog();
            this.addGroup = null;
        }, (error) => {
            this.toast.error(error);
            this.toast.closeModalDialog();
        });
    }

    deleteMediacenter() {
        this.toast.showModalDialog('ADMIN.MEDIACENTER.DELETE_TITLE', 'ADMIN.MEDIACENTER.DELETE_MESSAGE',
            DialogButton.getYesNo(() => this.toast.closeModalDialog(), () => {
                this.toast.showProgressDialog();
                this.mediacenterService.deleteMediacenter(this.currentMediacenter.authorityName).subscribe(() => {
                    this.toast.closeModalDialog();
                    this.toast.toast('ADMIN.MEDIACENTER.DELETED', {name: this.currentMediacenterCopy.profile.displayName});
                    this.setMediacenter(null);
                    this.refresh();
                }, (error: any) => {
                    this.toast.error(error);
                    this.toast.closeModalDialog();
                })
            }), true, () => this.toast.closeModalDialog(), {name: this.currentMediacenterCopy.profile.displayName});
    }

    private deleteGroup(authority: Group) {
        this.toast.showProgressDialog();
        this.mediacenterService.removeManagedGroup(this.currentMediacenterCopy.authorityName, authority.authorityName).subscribe((groups) => {
            this.mediacenterGroups = groups;
            this.toast.toast('ADMIN.MEDIACENTER.GROUPS.REMOVED', {name: authority.profile.displayName});
            this.toast.closeModalDialog();
        }, (error) => {
            this.toast.error(error);
            this.toast.closeModalDialog();
        });
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
        this.mediacenterService.importMediacenters(this.mediacentersFile).subscribe((data: any) => {
            this.toast.toast('ADMIN.MEDIACENTER.IMPORT.IMPORTED', {rows: data.rows});
            this.globalProgress = false;
            this.mediacentersFile = null;
        }, (error: any) => {
            this.toast.error(error);
            this.globalProgress = false;
        });
    }

    public importOrganisations() {
        if (!this.organisationsFile) {
            this.toast.error(null, 'ADMIN.MEDIACENTER.ORGIMPORT.CHOOSE_ORGANISATIONS');
            return;
        }
        this.globalProgress = true;
        this.mediacenterService.importOrganisations(this.organisationsFile).subscribe((data: any) => {
            this.toast.toast('ADMIN.MEDIACENTER.ORGIMPORT.IMPORTED', {rows: data.rows});
            this.globalProgress = false;
            this.organisationsFile = null;
        }, (error: any) => {
            this.toast.error(error);
            this.globalProgress = false;
        });
    }

    // importMcOrgConnections
    public importOrgMc() {
        if (!this.orgMcFile) {
            this.toast.error(null, 'ADMIN.MEDIACENTER.ORG_MC_CONNECT.CHOOSE');
            return;
        }
        this.globalProgress = true;
        this.mediacenterService.importMcOrgConnections(this.orgMcFile, this.removeSchoolsFromMC).subscribe((data: any) => {
            this.toast.toast('ADMIN.MEDIACENTER.ORG_MC_CONNECT.IMPORTED', {rows: data.rows});
            this.globalProgress = false;
            this.orgMcFile = null;
        }, (error: any) => {
            this.toast.error(error);
            this.globalProgress = false;
        });
    }
}
