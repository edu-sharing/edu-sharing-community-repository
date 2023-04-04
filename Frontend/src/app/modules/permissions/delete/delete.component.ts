import { Component, OnInit, ViewChild } from '@angular/core';
import {
    DeleteMode,
    DialogButton,
    Group,
    JobDescription,
    RestAdminService,
    RestConstants,
    RestIamService,
    SessionStorageService,
} from '../../../core-module/core.module';
import { Toast } from '../../../core-ui-module/toast';
import { TranslateService } from '@ngx-translate/core';
import { AuthorityNamePipe } from '../../../shared/pipes/authority-name.pipe';
import { Helper } from '../../../core-module/rest/helper';
import { AuthoritySearchMode } from '../../../shared/components/authority-search-input/authority-search-input.component';
import {
    InteractionType,
    ListItem,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
} from 'ngx-edu-sharing-ui';
import { User } from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-permissions-delete',
    templateUrl: 'delete.component.html',
    styleUrls: ['delete.component.scss'],
})
export class PermissionsDeleteComponent implements OnInit {
    readonly DisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    readonly AuthoritySearchMode = AuthoritySearchMode;
    @ViewChild(NodeEntriesWrapperComponent)
    nodeEntriesWrapperComponent: NodeEntriesWrapperComponent<User>;
    deleteModes = [DeleteMode.none, DeleteMode.assign, DeleteMode.delete];
    deleteModesFolder = [DeleteMode.none, DeleteMode.assign];
    options: any;
    receiver: User;
    receiverGroup: Group;
    usersDataSource = new NodeDataSource<User>();
    columns: ListItem[] = [];
    deleteResult: string;
    deleteButtons: DialogButton[];
    jobs: JobDescription[];
    job: JobDescription | 'NONE' = 'NONE';

    constructor(
        private iam: RestIamService,
        private admin: RestAdminService,
        private toast: Toast,
        private storage: SessionStorageService,
        private translate: TranslateService,
    ) {
        // send list of target users + options for these specific users
        const defaultOptions = {
            // change this value if the config needs to be reset to default
            version: '1.0.0',
            homeFolder: {
                folders: DeleteMode.none,
                privateFiles: DeleteMode.none,
                ccFiles: DeleteMode.none,
                keepFolderStructure: false,
            },
            sharedFolders: {
                folders: DeleteMode.none,
                privateFiles: DeleteMode.none,
                ccFiles: DeleteMode.none,
            },
            collections: {
                privateCollections: DeleteMode.none,
                publicCollections: DeleteMode.none,
            },
            ratings: {
                delete: false,
            },
            comments: {
                delete: false,
            },
            collectionFeedback: {
                delete: false,
            },
            statistics: {
                delete: false,
            },
            stream: {
                delete: false,
            },
            // change owner + (optional) invite a coordinator group
            // comments, ratings, feedback, stream, statistics
            receiver: '',
            receiverGroup: '',
            // shall the user be found & removed inside contributor metadata
            cleanupMetadata: true,
        };
        this.storage.get('delete_users_options', defaultOptions).subscribe((data: any) => {
            if (data.version === defaultOptions.version) {
                this.options = data;
            } else {
                this.options = defaultOptions;
            }
        });
        this.columns.push(new ListItem('USER', RestConstants.AUTHORITY_NAME));
        this.columns.push(new ListItem('USER', RestConstants.AUTHORITY_FIRSTNAME));
        this.columns.push(new ListItem('USER', RestConstants.AUTHORITY_LASTNAME));
        this.deleteButtons = DialogButton.getOk(() => {
            this.deleteResult = null;
        });
        this.refresh();
    }

    async ngOnInit() {
        this.jobs = (await this.admin.getAllJobs().toPromise()).filter((j) =>
            j.tags?.includes('DeletePersonJob'),
        );
    }

    /**
     * returns a code whether all selected modes seem to be data conform and all user-relevant data will be removed and all options match up
     */
    isValid() {
        return !this.anyModeMatches(DeleteMode.none);
    }

    hasAssigning() {
        return this.anyModeMatches(DeleteMode.assign);
    }

    private anyModeMatches(mode: DeleteMode) {
        return (
            this.options.homeFolder.folders === mode ||
            this.options.homeFolder.privateFiles === mode ||
            this.options.homeFolder.ccFiles === mode ||
            this.options.sharedFolders.folders === mode ||
            this.options.sharedFolders.privateFiles === mode ||
            this.options.sharedFolders.ccFiles === mode ||
            this.options.collections.privateCollections === mode ||
            this.options.collections.publicCollections === mode
        );
    }

    refresh() {
        this.usersDataSource.isLoading = true;
        const request = { maxItems: RestConstants.COUNT_UNLIMITED };
        this.iam.searchUsers('*', true, 'todelete', request).subscribe(
            (users) => {
                this.usersDataSource.setData(users.users as unknown as User[], users.pagination);
                this.usersDataSource.isLoading = false;
            },
            (error) => {
                this.toast.error(error);
                this.usersDataSource.isLoading = false;
            },
        );
    }

    prepareStart() {
        let message = this.translate.instant('PERMISSIONS.DELETE.CONFIRM.USERS');
        for (const user of this.nodeEntriesWrapperComponent.getSelection().selected) {
            message += '\n' + new AuthorityNamePipe(this.translate).transform(user, null);
        }
        if (this.hasAssigning()) {
            message +=
                '\n\n' +
                this.translate.instant('PERMISSIONS.DELETE.CONFIRM.RECEIVER', {
                    user: new AuthorityNamePipe(this.translate).transform(this.receiver, null),
                });
            message +=
                '\n\n' +
                this.translate.instant('PERMISSIONS.DELETE.CONFIRM.RECEIVER_GROUP', {
                    group: new AuthorityNamePipe(this.translate).transform(
                        this.receiverGroup,
                        null,
                    ),
                });
        }
        message += '\n\n' + this.translate.instant('PERMISSIONS.DELETE.CONFIRM.FINAL');
        this.toast.showModalDialog('PERMISSIONS.DELETE.CONFIRM.CAPTION', message, [
            new DialogButton('CANCEL', { color: 'standard' }, () => this.toast.closeModalDialog()),
            new DialogButton('PERMISSIONS.DELETE.START', { color: 'primary' }, () => this.start()),
        ]);
    }

    start() {
        this.toast.showProgressDialog();
        if (this.job !== 'NONE') {
            this.admin
                .startJobSync(this.job.name, {
                    authorities: this.nodeEntriesWrapperComponent
                        .getSelection()
                        .selected.map((u) => u.authorityName),
                })
                .subscribe(
                    (result) => {
                        this.toast.closeModalDialog();
                        this.deleteResult = JSON.stringify(result, null, 2);
                        this.refresh();
                    },
                    (error) => {
                        this.toast.error(error);
                        this.toast.closeModalDialog();
                    },
                );
            return;
        }

        if (this.hasAssigning()) {
            this.options.receiver = this.receiver.authorityName;
            this.options.receiverGroup = this.receiverGroup.authorityName;
        }
        this.storage.set('delete_users_options', this.options);
        const submit = Helper.deepCopy(this.options);
        delete submit.version;
        this.admin
            .deletePersons(
                this.nodeEntriesWrapperComponent
                    .getSelection()
                    .selected.map((u) => u.authorityName),
                submit,
            )
            .subscribe(
                (result) => {
                    this.toast.closeModalDialog();
                    this.deleteResult = JSON.stringify(result, null, 2);
                    this.refresh();
                },
                (error) => {
                    this.toast.error(error);
                    this.toast.closeModalDialog();
                },
            );
    }

    missingAssigning() {
        return this.hasAssigning() && (this.receiver == null || this.receiverGroup == null);
    }

    canSubmit() {
        return !this.nodeEntriesWrapperComponent.getSelection().isEmpty && !this.missingAssigning();
    }

    allAssigning() {
        return (
            this.options.homeFolder.folders === 'assign' &&
            this.options.homeFolder.privateFiles === 'assign' &&
            this.options.homeFolder.ccFiles === 'assign'
        );
    }

    updateForm() {
        if (!this.allAssigning()) {
            this.options.homeFolder.keepFolderStructure = false;
        }
    }
}
