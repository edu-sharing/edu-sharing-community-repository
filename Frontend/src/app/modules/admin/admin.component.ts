import { TranslationsService } from '../../translations/translations.service';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { ActivatedRoute, Router } from '@angular/router';
import { Toast } from '../../core-ui-module/toast';
import {
    ConfigurationService,
    DialogButton,
    JobDescription,
    ListItem,
    NodeListElastic,
    RestIamService,
    RestMediacenterService,
} from '../../core-module/core.module';
import { TranslateService } from '@ngx-translate/core';
import { SessionStorageService } from '../../core-module/core.module';
import { RestConnectorService } from '../../core-module/core.module';
import {
    Component,
    ViewChild,
    ElementRef,
    ViewContainerRef,
    ComponentFactoryResolver,
    OnDestroy,
    OnInit,
} from '@angular/core';
import {
    LoginResult,
    ServerUpdate,
    CacheInfo,
    Application,
    Node,
    Authority,
    NodeList,
    NodeWrapper,
    RestoreResult,
} from '../../core-module/core.module';
import { RestAdminService } from '../../core-module/core.module';
import { Helper } from '../../core-module/rest/helper';
import { RestConstants } from '../../core-module/core.module';
import { UIConstants } from '../../core-module/ui/ui-constants';
import { RestNodeService } from '../../core-module/core.module';
import { SuggestItem } from '../../common/ui/autocomplete/autocomplete.component';
import { RestOrganizationService } from '../../core-module/core.module';
import { RestSearchService } from '../../core-module/core.module';
import { RestHelper } from '../../core-module/core.module';
import { Observable, Observer } from 'rxjs';
import { RestNetworkService } from '../../core-module/core.module';
import { MainNavComponent } from '../../main/navigation/main-nav/main-nav.component';
import { CustomHelper } from '../../common/custom-helper';
import { DateHelper } from '../../core-ui-module/DateHelper';
import { CsvHelper } from '../../core-module/csv.helper';
import { trigger } from '@angular/animations';
import { UIAnimation } from '../../core-module/ui/ui-animation';
import IEditorOptions = monaco.editor.IEditorOptions;
import { NgxEditorModel } from 'ngx-monaco-editor';
import { Scope } from '../../core-ui-module/option-item';
import { AboutService } from 'ngx-edu-sharing-api';
import { SkipTarget } from '../../main/navigation/skip-nav/skip-nav.service';
import { AuthoritySearchMode } from '../../shared/components/authority-search-input/authority-search-input.component';
import { PlatformLocation } from '@angular/common';
import { MainNavService } from '../../main/navigation/main-nav.service';
import { DialogsService } from '../../features/dialogs/dialogs.service';

type LuceneData = {
    mode: 'NODEREF' | 'SOLR' | 'ELASTIC';
    store: 'Workspace' | 'Archive';
    offset: number;
    count: number;
    noderef?: string;
    query?: string;
    properties?: string;
    authorities?: Authority[];
    outputMode?: 'view' | 'export';
    exportFormat?: 'json' | 'csv';
};

@Component({
    selector: 'es-admin-main',
    templateUrl: 'admin.component.html',
    styleUrls: ['admin.component.scss'],
    animations: [trigger('openOverlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST))],
})
export class AdminComponent implements OnInit, OnDestroy {
    readonly AuthoritySearchMode = AuthoritySearchMode;
    readonly SCOPES = Scope;

    constructor(
        private toast: Toast,
        private route: ActivatedRoute,
        private router: Router,
        private platformLocation: PlatformLocation,
        private config: ConfigurationService,
        private translate: TranslateService,
        private translations: TranslationsService,
        private iamService: RestIamService,
        private storage: SessionStorageService,
        private networkService: RestNetworkService,
        private mediacenterService: RestMediacenterService,
        private componentFactoryResolver: ComponentFactoryResolver,
        private viewContainerRef: ViewContainerRef,
        private admin: RestAdminService,
        private connector: RestConnectorService,
        private about: AboutService,
        private node: RestNodeService,
        private searchApi: RestSearchService,
        private mainNav: MainNavService,
        private dialogs: DialogsService,
        private organization: RestOrganizationService,
    ) {
        this.addCustomComponents(
            CustomHelper.getCustomComponents('AdminComponent', this.componentFactoryResolver),
        );
        this.searchColumns.push(new ListItem('NODE', RestConstants.CM_NAME));
        this.searchColumns.push(new ListItem('NODE', RestConstants.NODE_ID));
        this.searchColumns.push(new ListItem('NODE', RestConstants.CM_MODIFIED_DATE));
        this.translations.waitForInit().subscribe(() => {
            this.warningButtons = [
                new DialogButton('CANCEL', { color: 'standard' }, () => {
                    window.history.back();
                }),
                new DialogButton('ADMIN.UNDERSTAND', { color: 'primary' }, () => {
                    this.showWarning = false;
                }),
            ];
            this.xmlCardButtons = [
                new DialogButton('CANCEL', { color: 'standard' }, () => {
                    this.xmlAppProperties = null;
                }),
                new DialogButton('APPLY', { color: 'primary' }, () => {
                    this.saveApp();
                }),
            ];
            this.getTemplates();
            this.connector.isLoggedIn().subscribe((data: LoginResult) => {
                this.loginResult = data;
                this.mediacenterService.getMediacenters().subscribe((mediacenters) => {
                    this.mediacenters = mediacenters;
                    this.init();
                });
            });
        });
    }
    private static MULTILINE_PROPERTIES = ['custom_html_headers', 'public_key'];
    static RS_CONFIG_HELP =
        'https://docs.edu-sharing.com/confluence/edp/de/installation-en/installation-of-the-edu-sharing-rendering-service';
    mailTemplates = [
        'invited',
        'invited_workflow',
        'invited_safe',
        'invited_collection',
        'nodeIssue',
        'userStatusChanged',
        'groupSignupList',
        'groupSignupUser',
        'groupSignupConfirmed',
        'groupSignupRejected',
        'groupSignupAdmin',
        'userRegister',
        'passwordRequest',
        'userRegisterInformation',
    ];
    public mode: string;
    public globalProgress = true;
    public appUrl: string;
    public propertyName: string;
    public chooseDirectory = false;
    public chooseCollection = false;
    public cacheName: string;
    public cacheInfo: string;
    public oai: any = {};
    public job: {
        params?: string;
        name?: string;
        class?: string;
        object?: JobDescription;
    } = {};
    public jobs: any;
    public jobsOpen: boolean[] = [];
    public jobsLogFilter: any = [];
    public jobsLogLevel: any = [];
    public jobsLogData: any = [];
    public jobCodeOptions = {
        minimap: { enabled: false },
        language: 'json',
        autoIndent: true,
        automaticLayout: true,
    };
    public dslCodeOptions = {
        minimap: { enabled: false },
        language: 'json',
        autoIndent: true,
        automaticLayout: true,
    };
    public elasticResponseCodeOptions = {
        minimap: { enabled: false },
        language: 'json',
        autoIndent: true,
        automaticLayout: true,
        readOnly: true,
    };
    public jobClasses: SuggestItem[] = [];
    public jobClassesSuggested: SuggestItem[] = [];
    public lucene: LuceneData = {
        mode: 'NODEREF',
        store: 'Workspace',
        offset: 0,
        count: 100,
        outputMode: 'view',
    };
    public oaiSave = true;
    public repositoryVersion: string;
    public updates: ServerUpdate[] = [];
    public applications: Application[] = [];
    public applicationsOpen: any = {};
    public showWarning = false;
    public dialogTitle: string;
    public dialogMessage: string;
    public dialogButtons: DialogButton[] = [];
    public dialogParameters: any;
    public warningButtons: DialogButton[] = [];
    public xmlCardButtons: DialogButton[] = [];
    public xmlAppProperties: any;
    public xmlAppAdditionalPropertyName: string;
    public xmlAppAdditionalPropertyValue: string;
    parentNode: Node;
    parentCollection: Node;
    parentCollectionType = 'root';
    public catalina: string;
    oaiClasses: string[];
    @ViewChild('catalinaRef') catalinaRef: ElementRef;
    @ViewChild('xmlSelect') xmlSelect: ElementRef;
    @ViewChild('excelSelect') excelSelect: ElementRef;
    @ViewChild('templateSelect') templateSelect: ElementRef;
    @ViewChild('dynamic') dynamicComponent: any;

    buttons: any[] = [];
    availableJobs: JobDescription[];
    excelFile: File;
    collectionsFile: File;
    uploadTempFile: File;
    uploadJobsFile: File;
    uploadOaiFile: File;
    public xmlAppKeys: string[];
    public currentApp: string;
    currentAppXml: string;
    public editableXmls = [{ name: 'HOMEAPP', file: RestConstants.HOME_APPLICATION_XML }];
    searchResponse: NodeList | NodeListElastic;
    searchColumns: ListItem[] = [];
    public selectedTemplate = '';
    public templates: string[];
    public eduGroupSuggestions: SuggestItem[];
    public eduGroupsSelected: SuggestItem[] = [];
    systemChecks: any = [];
    tpChecks: any = [];
    mailReceiver: string;
    mailTemplate: string;
    private loginResult: LoginResult;
    private mediacenters: any[];
    ownAppMode = 'repository';
    authenticateAuthority: Authority;
    private readonly onDestroyTasks: Array<() => void> = [];

    ngOnInit(): void {
        this.mainNav.setMainNavConfig({
            title: 'ADMIN.TITLE',
            currentScope: 'admin',
            searchEnabled: false,
        });
    }

    ngOnDestroy(): void {
        this.onDestroyTasks.forEach((task) => task());
    }

    public startJob() {
        this.storage.set('admin_job', this.job);
        this.globalProgress = true;
        try {
            this.admin
                .startJob(this.job.class, JSON.parse(this.job.params), this.uploadJobsFile)
                .subscribe(
                    () => {
                        this.globalProgress = false;
                        // this.uploadJobsFile = null;
                        this.toast.toast('ADMIN.JOBS.JOB_STARTED');
                    },
                    (error: any) => {
                        this.globalProgress = false;
                        this.toast.error(error);
                    },
                );
        } catch (e) {
            console.warn(e);
            this.toast.error(e);
            this.globalProgress = false;
        }
    }
    public debugNode(node: Node) {
        this.dialogs.openNodeReportDialog({ node });
    }
    public getModeButton(mode = this.mode): any {
        return this.buttons[Helper.indexOfObjectArray(this.buttons, 'id', mode)];
    }
    public searchNoderef() {
        this.storage.set('admin_lucene', this.lucene);
        this.globalProgress = true;
        this.node.getNodeMetadata(this.lucene.noderef, [RestConstants.ALL]).subscribe(
            (node) => {
                this.globalProgress = false;
                this.searchResponse = {
                    nodes: [node.node],
                    pagination: {
                        from: 0,
                        count: 1,
                        total: 1,
                    },
                };
            },
            (error) => {
                this.globalProgress = false;
                this.toast.error(error);
            },
        );
    }
    public searchNodes() {
        this.storage.set('admin_lucene', this.lucene);
        const authorities = [];
        if (this.lucene.authorities) {
            for (const auth of this.lucene.authorities) {
                authorities.push(auth.authorityName);
            }
        }
        const request = {
            offset: this.lucene.offset ? this.lucene.offset : 0,
            count: this.lucene.count,
            propertyFilter: [RestConstants.ALL],
        };
        this.globalProgress = true;
        if (this.lucene.mode === 'SOLR') {
            this.admin
                .searchLucene(this.lucene.query, this.lucene.store, authorities, request)
                .subscribe(
                    (data) => {
                        this.globalProgress = false;
                        this.searchResponse = data;
                    },
                    (error: any) => {
                        this.globalProgress = false;
                        this.toast.error(error);
                    },
                );
        } else if (this.lucene.mode === 'ELASTIC') {
            this.admin.searchElastic(this.lucene.query).subscribe(
                (data) => {
                    this.globalProgress = false;
                    this.searchResponse = data;
                },
                (error: any) => {
                    this.globalProgress = false;
                    this.toast.error(error);
                },
            );
        }
    }
    public addLuceneAuthority(authority: Authority) {
        if (!this.lucene.authorities) this.lucene.authorities = [];
        this.lucene.authorities.push(authority);
    }
    public removeLuceneAuthority(authority: Authority) {
        this.lucene.authorities.splice(this.lucene.authorities.indexOf(authority), 1);
    }
    public isMultilineProperty(key: string) {
        if (AdminComponent.MULTILINE_PROPERTIES.indexOf(key) != -1) return true;
        return this.xmlAppProperties[key].indexOf('\n') != -1;
    }
    public downloadApp(app: Application) {
        Helper.downloadContent(app.file, app.xml);
    }
    public updateExcelFile(event: any) {
        this.excelFile = event.target.files[0];
    }
    public updateUploadFile(event: any, file: string) {
        (this as any)[file] = event.target.files[0];
    }
    public updateUploadOaiFile(event: any) {
        this.uploadOaiFile = event.target.files[0];
    }
    public updateCollectionsFile(event: any) {
        this.collectionsFile = event.target.files[0];
    }
    public importCollections() {
        if (!this.collectionsFile) {
            this.toast.error(null, 'ADMIN.IMPORT.CHOOSE_COLLECTIONS_XML');
            return;
        }
        if (!this.parentCollection && this.parentCollectionType == 'choose') {
            this.toast.error(null, 'ADMIN.IMPORT.CHOOSE_COLLECTION');
            return;
        }
        this.globalProgress = true;
        this.admin
            .importCollections(
                this.collectionsFile,
                this.parentCollectionType == 'root'
                    ? RestConstants.ROOT
                    : this.parentCollection.ref.id,
            )
            .subscribe(
                (data: any) => {
                    this.toast.toast('ADMIN.IMPORT.COLLECTIONS_IMPORTED', { count: data.count });
                    this.globalProgress = false;
                    this.collectionsFile = null;
                },
                (error: any) => {
                    this.toast.error(error);
                    this.globalProgress = false;
                },
            );
    }
    public startUploadTempFile() {
        if (!this.uploadTempFile) {
            this.toast.error(null, 'ADMIN.TOOLKIT.CHOOSE_UPLOAD_TEMP');
            return;
        }
        this.globalProgress = true;
        this.admin.uploadTempFile(this.uploadTempFile).subscribe(
            (data: any) => {
                this.toast.toast('ADMIN.TOOLKIT.UPLOAD_TEMP_DONE', { filename: data.file });
                this.globalProgress = false;
                this.uploadTempFile = null;
            },
            (error: any) => {
                this.toast.error(error);
                this.globalProgress = false;
            },
        );
    }
    public importExcel() {
        if (!this.excelFile) {
            this.toast.error(null, 'ADMIN.IMPORT.CHOOSE_EXCEL');
            return;
        }
        if (!this.parentNode) {
            this.toast.error(null, 'ADMIN.IMPORT.CHOOSE_DIRECTORY');
            return;
        }
        this.globalProgress = true;
        this.admin.importExcel(this.excelFile, this.parentNode.ref.id).subscribe(
            (data: any) => {
                this.toast.toast('ADMIN.IMPORT.EXCEL_IMPORTED', { rows: data.rows });
                this.globalProgress = false;
                this.excelFile = null;
            },
            (error: any) => {
                this.toast.error(error);
                this.globalProgress = false;
            },
        );
    }
    public closeAppEditor() {
        this.xmlAppProperties = null;
        this.xmlAppAdditionalPropertyName = null;
        this.xmlAppAdditionalPropertyValue = null;
    }
    public saveApp() {
        this.globalProgress = true;
        if (this.xmlAppAdditionalPropertyName && this.xmlAppAdditionalPropertyName.trim()) {
            this.xmlAppProperties[this.xmlAppAdditionalPropertyName.trim()] =
                this.xmlAppAdditionalPropertyValue;
        }
        this.admin.updateApplicationXML(this.currentAppXml, this.xmlAppProperties).subscribe(
            () => {
                this.toast.toast('ADMIN.APPLICATIONS.APP_SAVED', { xml: this.currentAppXml });
                this.globalProgress = false;
                this.closeAppEditor();
                this.refreshAppList();
            },
            (error: any) => {
                this.globalProgress = false;
                this.toast.error(error);
            },
        );
    }
    public configApp(app: Application) {
        window.open(app.configUrl);
    }
    public editApp(app: any) {
        this.currentApp = app.name || 'HOMEAPP';
        this.currentAppXml = app.file;
        this.globalProgress = true;
        this.admin.getApplicationXML(app.file).subscribe(
            (data: any[]) => {
                this.globalProgress = false;
                this.xmlAppKeys = Object.keys(data);
                this.xmlAppProperties = data;
            },
            (error: any) => {
                this.globalProgress = false;
                this.toast.error(error);
            },
        );
    }
    public removeApp(app: Application) {
        this.dialogTitle = 'ADMIN.APPLICATIONS.REMOVE_TITLE';
        this.dialogMessage = 'ADMIN.APPLICATIONS.REMOVE_MESSAGE';
        let info = '';
        for (const key in app) {
            if (key == 'xml') continue;
            info += key + ': ' + (app as any)[key] + '\n';
        }

        this.dialogParameters = { info };
        this.dialogButtons = [
            new DialogButton('CANCEL', { color: 'standard' }, () => {
                this.dialogTitle = null;
            }),
            new DialogButton('ADMIN.APPLICATIONS.REMOVE', { color: 'danger' }, () => {
                this.dialogTitle = null;
                this.globalProgress = true;
                this.admin.removeApplication(app.id).subscribe(
                    () => {
                        this.globalProgress = false;
                        this.refreshAppList();
                    },
                    (error: any) => {
                        this.toast.error(error);
                        this.globalProgress = false;
                    },
                );
            }),
        ];
    }
    public setMode(mode: string, skipLocationChange = false) {
        this.router.navigate(['./'], {
            queryParams: { mode },
            relativeTo: this.route,
            skipLocationChange: skipLocationChange,
        });
    }
    public pickDirectory(event: Node[]) {
        this.parentNode = event[0];
        this.chooseDirectory = false;
    }
    public pickCollection(event: Node[]) {
        this.parentCollection = event[0];
        this.chooseCollection = false;
    }
    public registerAppXml(event: any) {
        const file = event.target.files[0];
        if (!file) return;
        this.globalProgress = true;
        this.admin.addApplicationXml(file).subscribe(
            (data: any) => {
                this.toast.toast('ADMIN.APPLICATIONS.APP_REGISTERED');
                this.refreshAppList();
                this.globalProgress = false;
                this.xmlSelect.nativeElement.value = null;
            },
            (error: any) => {
                this.globalProgress = false;
                this.xmlSelect.nativeElement.value = null;
                this.toast.error(error);
            },
        );
    }
    public registerApp() {
        this.globalProgress = true;
        this.admin.addApplication(this.appUrl).subscribe(
            (data: any) => {
                this.toast.toast('ADMIN.APPLICATIONS.APP_REGISTERED');
                this.refreshAppList();
                this.globalProgress = false;
                this.appUrl = '';
            },
            (error: any) => {
                this.globalProgress = false;
                this.toast.error(error);
            },
        );
    }
    public getCacheInfo() {
        this.globalProgress = true;
        this.admin.getCacheInfo(this.cacheInfo).subscribe(
            (data: CacheInfo) => {
                this.globalProgress = false;
                this.dialogTitle = this.cacheInfo;
                this.dialogMessage =
                    'size: ' + data.size + '\nstatistic hits: ' + data.statisticHits;
                this.dialogButtons = DialogButton.getOk(() => {
                    this.dialogTitle = null;
                });
            },
            (error: any) => {
                this.globalProgress = false;
                this.toast.error(error);
            },
        );
    }
    public refreshAppInfo() {
        this.globalProgress = true;
        this.admin.refreshAppInfo().subscribe(
            () => {
                this.globalProgress = false;
                this.toast.toast('ADMIN.TOOLKIT.APP_INFO_REFRESHED');
            },
            (error: any) => {
                this.globalProgress = false;
                this.toast.error(error);
            },
        );
    }
    public refreshEduGroupCache() {
        this.globalProgress = true;
        this.admin.refreshEduGroupCache().subscribe(
            () => {
                this.globalProgress = false;
                this.toast.toast('ADMIN.TOOLKIT.EDU_GROUP_CACHE_REFRESHED');
            },
            (error: any) => {
                this.globalProgress = false;
                this.toast.error(error);
            },
        );
    }
    public refreshCache(sticky: boolean) {
        this.globalProgress = true;
        this.admin
            .refreshCache(this.parentNode ? this.parentNode.ref.id : RestConstants.USERHOME, sticky)
            .subscribe(
                () => {
                    this.globalProgress = false;
                    this.toast.toast('ADMIN.TOOLKIT.CACHE_REFRESHED');
                },
                (error: any) => {
                    this.globalProgress = false;
                    this.toast.error(error);
                },
            );
    }
    public removeAppProperty(pos: number) {
        const key = this.xmlAppKeys[pos];
        this.xmlAppKeys.splice(pos, 1);
        delete this.xmlAppProperties[key];
    }
    public oaiImport() {
        if (!this.oaiPreconditions()) return;
        this.globalProgress = true;
        if (this.oaiSave) {
            this.storage.set('admin_oai', this.oai);
        }
        if (this.uploadOaiFile) {
            this.admin
                .importOAIXML(
                    this.uploadOaiFile,
                    this.oai.recordHandlerClassName,
                    this.oai.binaryHandlerClassName,
                )
                .subscribe(
                    (node) => {
                        this.debugNode(node);
                        this.globalProgress = false;
                    },
                    (error) => {
                        this.toast.error(error);
                        this.globalProgress = false;
                    },
                );
        } else {
            this.admin
                .importOAI(
                    this.oai.url,
                    this.oai.set,
                    this.oai.prefix,
                    this.oai.className,
                    this.oai.importerClassName,
                    this.oai.recordHandlerClassName,
                    this.oai.binaryHandlerClassName,
                    this.oai.persistentHandlerClassName,
                    this.oai.metadata,
                    this.oai.file,
                    this.oai.ids,
                    this.oai.forceUpdate,
                    this.oai.from,
                    this.oai.until,
                    this.oai.periodInDays,
                )
                .subscribe(
                    () => {
                        this.globalProgress = false;
                        const additional: any = {
                            link: {
                                caption: 'ADMIN.IMPORT.OPEN_JOBS',
                                callback: () => this.setMode('JOBS'),
                            },
                        };
                        this.toast.toast('ADMIN.IMPORT.OAI_STARTED', null, null, null, additional);
                    },
                    (error: any) => {
                        this.globalProgress = false;
                        this.toast.error(error);
                    },
                );
        }
    }

    private oaiPreconditions() {
        if (this.uploadOaiFile) return true;
        if (!this.oai.url) {
            this.toast.error(null, 'ADMIN.IMPORT.OAI_NO_URL');
            return false;
        }
        if (!this.oai.set) {
            this.toast.error(null, 'ADMIN.IMPORT.OAI_NO_SET');
            return false;
        }
        if (!this.oai.prefix) {
            this.toast.error(null, 'ADMIN.IMPORT.OAI_NO_PREFIX');
            return false;
        }
        return true;
    }
    public removeImports() {
        if (!this.oaiPreconditions()) return;
        this.globalProgress = true;
        this.admin.removeDeletedImports(this.oai.url, this.oai.set, this.oai.prefix).subscribe(
            (data: any) => {
                this.globalProgress = false;
                this.toast.toast('ADMIN.IMPORT.IMPORTS_REMOVED');
                this.appUrl = '';
            },
            (error: any) => {
                this.globalProgress = false;
                this.toast.error(error);
            },
        );
    }
    public getPropertyValues() {
        this.globalProgress = true;
        this.admin.getPropertyValuespace(this.propertyName).subscribe(
            (data: any) => {
                this.globalProgress = false;
                this.dialogTitle = 'ADMIN.IMPORT.PROPERTY_VALUESPACE';
                this.dialogMessage = data.xml;
                this.dialogButtons = DialogButton.getOk(() => {
                    this.dialogTitle = null;
                });
                this.appUrl = '';
            },
            (error: any) => {
                this.globalProgress = false;
                this.toast.error(error);
            },
        );
    }
    public runUpdate(update: ServerUpdate, execute = false) {
        this.globalProgress = true;
        this.admin.runServerUpdate(update.id, execute).subscribe(
            (data: any) => {
                this.globalProgress = false;
                this.dialogTitle = 'ADMIN.UPDATE.RESULT';
                this.dialogMessage = data.result;
                this.dialogButtons = DialogButton.getOk(() => {
                    this.dialogTitle = null;
                });
                this.refreshUpdateList();
            },
            (error: any) => {
                this.globalProgress = false;
                this.toast.error(error);
            },
        );
    }

    public refreshAppList() {
        this.admin.getApplications().subscribe((data: Application[]) => {
            this.applications = data;
            this.applicationsOpen = {};
            if (this.applications && this.applications.length) {
                this.getAppTypes().forEach((t) => (this.applicationsOpen[t] = true));
            }
        });
    }

    refreshCatalina() {
        this.admin.getCatalina().subscribe((data: string[]) => {
            this.catalina = data.reverse().join('\n');
            this.setCatalinaPosition();
        });
    }

    private setCatalinaPosition() {
        setTimeout(() => {
            if (this.catalinaRef) {
                this.catalinaRef.nativeElement.scrollTop =
                    this.catalinaRef.nativeElement.scrollHeight;
            } else {
                this.setCatalinaPosition();
            }
        }, 50);
    }

    public getTemplates() {
        this.getTemplateFolderId().subscribe((id) => {
            this.node.getChildren(id).subscribe((data) => {
                const templates = [];
                for (const node of data.nodes) {
                    if (node.mimetype == 'text/xml') {
                        templates.push(node.name);
                    }
                }
                this.templates = templates;
                this.selectedTemplate = this.templates[0];
            });
        });
    }

    public getTemplateFolderId() {
        return new Observable<string>((observer: Observer<string>) => {
            this.searchApi
                .searchByProperties(
                    [RestConstants.CM_NAME],
                    ['Edu_Sharing_Sys_Template'],
                    ['='],
                    '',
                    RestConstants.CONTENT_TYPE_FILES_AND_FOLDERS,
                )
                .subscribe((data) => {
                    for (const node of data.nodes) {
                        if (node.isDirectory) {
                            observer.next(node.ref.id);
                            observer.complete();
                            return;
                        }
                    }
                });
        });
    }

    public updateEduGroupSuggestions(event: any) {
        this.organization.getOrganizations(event.input, false).subscribe((data: any) => {
            const ret: SuggestItem[] = [];
            for (const orga of data.organizations) {
                const item = new SuggestItem(
                    orga.authorityName,
                    orga.profile.displayName,
                    'group',
                    '',
                );
                item.originalObject = orga;
                ret.push(item);
            }
            this.eduGroupSuggestions = ret;
        });
    }

    public addEduGroup(data: any) {
        if (Helper.indexOfObjectArray(this.eduGroupsSelected, 'id', data.item.id) < 0)
            this.eduGroupsSelected.push(data.item);
    }

    public removeEduGroup(data: SuggestItem) {
        this.eduGroupsSelected.splice(
            Helper.indexOfObjectArray(this.eduGroupsSelected, 'id', data.id),
            1,
        );
    }

    public uploadTemplate(event: any) {
        const file = event.target.files[0];
        if (!file) return;
        const id = '';
        this.globalProgress = true;
        this.getTemplateFolderId().subscribe((id) => {
            this.node
                .createNode(
                    id,
                    RestConstants.CCM_TYPE_IO,
                    [],
                    RestHelper.createNameProperty(file.name),
                    true,
                )
                .subscribe(
                    (data: NodeWrapper) => {
                        this.node
                            .uploadNodeContent(
                                data.node.ref.id,
                                file,
                                RestConstants.COMMENT_MAIN_FILE_UPLOAD,
                            )
                            .subscribe((data) => {
                                this.getTemplates();
                                this.toast.toast('ADMIN.FOLDERTEMPLATES.UPLOAD_DONE', {
                                    filename: JSON.parse(data.response).node.name,
                                });
                                this.globalProgress = false;
                                this.templateSelect.nativeElement.value = null;
                            });
                    },
                    (error: any) => {
                        this.globalProgress = false;
                        this.templateSelect.nativeElement.value = null;
                        this.toast.error(error);
                    },
                );
        });
    }

    public applyTemplate(position = 0) {
        this.globalProgress = true;
        if (this.eduGroupsSelected.length < 1) {
            this.toast.error(null, 'ADMIN.FOLDERTEMPLATES.MISSING_GROUP');
            this.globalProgress = false;
            return;
        }
        if (this.selectedTemplate == '') {
            this.toast.error(null, 'ADMIN.FOLDERTEMPLATES.MISSING_TEMPLATE');
            this.globalProgress = false;
            return;
        }
        if (position >= this.eduGroupsSelected.length) {
            this.globalProgress = false;
            // done
            return;
        }
        this.admin
            .applyTemplate(this.eduGroupsSelected[position].id, this.selectedTemplate)
            .subscribe(
                (data) => {
                    this.toast.toast('ADMIN.FOLDERTEMPLATES.TEMPLATE_APPLIED', {
                        templatename: this.selectedTemplate,
                        groupname: this.eduGroupsSelected[position].id,
                    });
                    this.applyTemplate(position + 1);
                },
                (error: any) => {
                    this.toast.error(error, 'ADMIN.FOLDERTEMPLATES.TEMPLATE_NOTAPPLIED', {
                        templatename: this.selectedTemplate,
                        groupname: this.eduGroupsSelected[position].id,
                    });
                    this.applyTemplate(position + 1);
                },
            );
    }

    public gotoFoldertemplateFolder() {
        this.getTemplateFolderId().subscribe((id) => {
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'workspace'], {
                queryParams: { id },
            });
        });
    }
    getJobLog(job: any, pos: number) {
        let log = Helper.deepCopy(job.log).reverse();

        if (this.jobsLogLevel[pos]) {
            const result: any = [];
            for (const l of log) {
                if (l.level.syslogEquivalent > this.jobsLogLevel[pos]) continue;
                result.push(l);
            }
            log = result;
        }
        if (this.jobsLogFilter[pos]) {
            const result: any = [];
            for (const l of log) {
                if (
                    l.message.indexOf(this.jobsLogFilter[pos]) == -1 &&
                    l.className.indexOf(this.jobsLogFilter[pos]) == -1
                )
                    continue;
                result.push(l);
            }
            log = result;
        }
        if (log.length <= 200) return log;
        return log.slice(0, 200);
    }
    cancelJob(job: any) {
        this.dialogTitle = 'ADMIN.JOBS.CANCEL_TITLE';
        this.dialogMessage = 'ADMIN.JOBS.CANCEL_MESSAGE';
        this.dialogButtons = DialogButton.getYesNo(
            () => {
                this.dialogTitle = null;
            },
            () => {
                this.dialogTitle = null;
                this.globalProgress = true;
                this.admin.cancelJob(job.jobDetail.name).subscribe(
                    () => {
                        this.toast.toast('ADMIN.JOBS.TOAST_CANCELED');
                        this.globalProgress = false;
                    },
                    (error) => {
                        this.toast.error(error);
                        this.globalProgress = false;
                    },
                );
            },
        );
    }
    reloadJobStatus() {
        this.admin.getJobs().subscribe((jobs) => {
            this.jobs = jobs.filter((j: any) => !!j);
            this.updateJobLogs();
        });
    }
    getMajorVersion(version: string) {
        const v = version.split('.');
        if (v.length < 3) return v;
        v.splice(2, v.length - 2);
        return v.join('.');
    }
    runTpChecks() {
        const checks = [
            RestConstants.TOOLPERMISSION_USAGE_STATISTIC,
            RestConstants.TOOLPERMISSION_INVITE_ALLAUTHORITIES,
            RestConstants.TOOLPERMISSION_PUBLISH_COPY,
            RestConstants.TOOLPERMISSION_GLOBAL_STATISTICS_USER,
            RestConstants.TOOLPERMISSION_GLOBAL_STATISTICS_NODES,
        ];
        this.tpChecks = [];
        this.admin.getToolpermissions(RestConstants.AUTHORITY_EVERYONE).subscribe((tp) => {
            checks.forEach((c) => {
                this.tpChecks.push({
                    name: c,
                    status: tp[c].explicit === 'ALLOWED' ? 'FAIL' : 'OK',
                });
            });
        });
    }
    runChecks() {
        this.systemChecks = [];

        // check versions render service
        this.about.getAbout().subscribe(
            (about) => {
                const repositoryVersion = this.getMajorVersion(about.version.repository);
                const renderServiceVersion = this.getMajorVersion(about.version.renderservice);
                this.systemChecks.push({
                    name: 'RENDERING',
                    status:
                        repositoryVersion == 'unknown'
                            ? 'WARN'
                            : repositoryVersion == renderServiceVersion
                            ? 'OK'
                            : 'FAIL',
                    translate: about.version,
                    callback: () => {
                        this.setMode('APPLICATIONS');
                    },
                });
            },
            (error) => {
                this.systemChecks.push({
                    name: 'RENDERING',
                    status: 'FAIL',
                    error,
                    callback: () => {
                        this.setMode('APPLICATIONS');
                    },
                });
            },
        );
        // check if appid is changed
        this.networkService.getRepositories().subscribe((repos) => {
            const id = repos.repositories.filter((repo) => repo.isHomeRepo)[0].id;
            this.systemChecks.push({
                name: 'APPID',
                status: id == 'local' ? 'WARN' : 'OK',
                translate: { id },
                callback: () => {
                    this.setMode('APPLICATIONS');
                    this.editApp(this.editableXmls.filter((xml) => xml.name == 'HOMEAPP')[0]);
                },
            });
        });
        this.node.getNodePermissions(RestConstants.USERHOME).subscribe(
            (data) => {
                let status = 'OK';
                for (const perm of data.permissions.localPermissions.permissions) {
                    if (perm.authority.authorityName == RestConstants.AUTHORITY_EVERYONE) {
                        status = 'FAIL';
                    }
                }
                this.systemChecks.push(this.createSystemCheck('COMPANY_HOME', status));
            },
            (error) => {
                this.systemChecks.push(this.createSystemCheck('COMPANY_HOME', 'FAIL', error));
            },
        );
        this.admin.getJobs().subscribe((jobs) => {
            let count = 0;
            for (const job of jobs) {
                if (job.status == 'Running') {
                    count++;
                }
            }
            this.systemChecks.push({
                name: 'JOBS_RUNNING',
                status: count == 0 ? 'OK' : 'WARN',
                translate: { count },
            });
        });
        // check status of nodeReport + mail server
        this.admin.getConfigMerged().subscribe((config) => {
            const mail = config.repository.mail;
            if (this.config.instant('nodeReport', false)) {
                this.systemChecks.push({
                    name: 'MAIL_REPORT',
                    status: mail.report.receiver && mail.server.smtp.host ? 'OK' : 'FAIL',
                    translate: mail.report,
                });
            }
            this.systemChecks.push({
                name: 'MAIL_SETUP',
                status: mail.server.smtp.host ? 'OK' : 'FAIL',
                translate: mail.server.smtp,
            });
        });
        this.admin.getApplicationXML(RestConstants.HOME_APPLICATION_XML).subscribe((home) => {
            this.systemChecks.push({
                name: 'CORS',
                status: home.allow_origin
                    ? home.allow_origin.indexOf('http://localhost:54361') != -1
                        ? 'OK'
                        : 'INFO'
                    : 'FAIL',
                translate: home,
                callback: () => {
                    this.setMode('APPLICATIONS');
                    this.editApp(this.editableXmls.filter((xml) => xml.name == 'HOMEAPP')[0]);
                },
            });
            const domainRepo = home.domain;
            let domainRender: string;
            try {
                domainRender = new URL(home.contenturl).host;
            } catch (e) {
                console.warn(e);
            }
            this.systemChecks.push({
                name: 'RS_XSS',
                status: domainRepo == domainRender ? 'FAIL' : home.allow_origin ? 'OK' : 'INFO',
                translate: { repo: domainRepo, render: domainRender },
                callback: () => {
                    window.open(AdminComponent.RS_CONFIG_HELP);
                },
            });
        });
    }
    private createSystemCheck(name: string, status: string, error: any = null) {
        const check: any = {
            name,
            status,
            error,
        };
        if (name == 'COMPANY_HOME') {
            check.callback = () => {
                this.node.getNodeMetadata(RestConstants.USERHOME).subscribe((node) => {
                    UIHelper.goToWorkspaceFolder(this.node, this.router, null, node.node.parent.id);
                });
            };
        }
        return check;
    }
    getChecks(checks: any) {
        checks.sort((a: any, b: any) => {
            const status: any = { FAIL: 0, WARN: 1, INFO: 2, OK: 3 };
            const statusA = status[a.status];
            const statusB = status[b.status];
            if (statusA != statusB) return statusA < statusB ? -1 : 1;
            return a.name.localeCompare(b.name);
        });
        return checks;
    }

    testMail() {
        this.globalProgress = true;
        this.admin.testMail(this.mailReceiver, this.mailTemplate).subscribe(
            () => {
                this.toast.toast('ADMIN.CONFIG.MAIL_SENT', { receiver: this.mailReceiver });
                this.globalProgress = false;
            },
            (error) => {
                this.toast.error(error);
                this.globalProgress = false;
            },
        );
    }

    updateJobLogs() {
        this.jobsLogData = [];
        let i = 0;
        if (this.jobs) {
            for (const job of this.jobs) {
                this.jobsLogData.push(this.getJobLog(job, i));
                i++;
            }
        }
    }

    private prepareJobClasses() {
        this.jobClasses = this.availableJobs.map((j) => {
            const job = new SuggestItem('');
            const id = j.name.split('.');
            job.id = j.name;
            job.title = j.description;
            job.secondaryTitle = id[id.length - 1];
            job.originalObject = j;
            return job;
        });
    }
    getJobName(job: any) {
        if (job && job.class) {
            let name = job.class.split('.');
            name = name[name.length - 1];
            return name;
        }
        return null;
    }

    updateJobSuggestions(event: any) {
        const name = event ? event.input.toString().toLowerCase() : '';
        if (name === '*') {
            this.jobClassesSuggested = this.jobClasses;
        } else {
            console.log(name);
            this.jobClassesSuggested = this.jobClasses.filter(
                (j) =>
                    (j.title && j.title.toLowerCase().indexOf(name) !== -1) ||
                    (j.secondaryTitle && j.secondaryTitle.toLowerCase().indexOf(name) !== -1),
            );
        }
    }

    refreshUpdateList() {
        this.admin.getServerUpdates().subscribe((data: ServerUpdate[]) => {
            this.updates = data;
        });
    }

    exportLucene() {
        if (!this.lucene.properties) {
            this.toast.error(null, 'ADMIN.BROWSER.LUCENE_PROPERTIES_REQUIRED');
            return;
        }
        this.storage.set('admin_lucene', this.lucene);
        this.globalProgress = true;
        const props = this.lucene.properties.split('\n');
        this.admin
            .exportLucene(
                this.lucene.query,
                this.lucene.store,
                props,
                this.lucene.authorities?.map((a) => a.authorityName),
            )
            .subscribe((data) => {
                const filename =
                    'Export-' +
                    DateHelper.formatDate(this.translate, new Date().getTime(), {
                        useRelativeLabels: false,
                    });
                this.globalProgress = false;

                // transform store refs to ids
                data.forEach((d: any) => {
                    Object.keys(d).forEach((k) => {
                        if (d[k]?.id) {
                            d[k] = d[k].id;
                        }
                    });
                });
                if (this.lucene.exportFormat === 'json') {
                    // reformat data, move all parent:: props to a seperate child
                    data.forEach((d: any) => {
                        Object.keys(d)
                            .filter((k) => k.startsWith('parent::'))
                            .forEach((key) => {
                                if (!d.parent) {
                                    d.parent = {};
                                }
                                d.parent[key.substring('parent::'.length)] = d[key];
                                delete d[key];
                            });
                    });
                    Helper.downloadContent(filename + '.json', JSON.stringify(data, null, 2));
                } else {
                    CsvHelper.download(filename, props, data);
                }
            });
    }

    private addCustomComponents(customComponents: any[]) {
        for (const c of customComponents) {
            if (c.targetType == 'BUTTON') {
                const item = c.payload;
                item.factory = c.factory;
                this.buttons.splice(
                    c.payload.position >= 0
                        ? c.payload.position
                        : this.buttons.length + c.payload.position,
                    0,
                    item,
                );
            }
        }
    }

    private initButtons() {
        if (this.loginResult.isAdmin) {
            this.buttons = [
                {
                    id: 'INFO',
                    icon: 'info_outline',
                },
                {
                    id: 'PLUGINS',
                    icon: 'extension',
                },
                {
                    id: 'FRONTPAGE',
                    icon: 'home',
                },
                {
                    id: 'GLOBAL_CONFIG',
                    icon: 'edit',
                },
                {
                    id: 'CONFIG',
                    icon: 'build',
                },
                {
                    id: 'APPLICATIONS',
                    icon: 'apps',
                },
                {
                    id: 'UPDATE',
                    icon: 'update',
                },
                {
                    id: 'IMPORT',
                    icon: 'cloud_download',
                },
                {
                    id: 'JOBS',
                    icon: 'check',
                },
                {
                    id: 'TOOLKIT',
                    icon: 'settings',
                },
                {
                    id: 'BROWSER',
                    icon: 'search',
                },
                {
                    id: 'FOLDERTEMPLATES',
                    icon: 'create_new_folder',
                },
            ];
        }
        if (
            this.loginResult.isAdmin ||
            this.loginResult.toolPermissions.indexOf(
                RestConstants.TOOLPERMISSION_GLOBAL_STATISTICS_NODES,
            ) !== -1 ||
            this.loginResult.toolPermissions.indexOf(
                RestConstants.TOOLPERMISSION_GLOBAL_STATISTICS_USER,
            ) !== -1
        ) {
            this.buttons.splice(1, 0, {
                id: 'STATISTICS',
                icon: 'assessment',
            });
        }
        if (
            this.loginResult.isAdmin ||
            this.mediacenters.filter((mc) => mc.administrationAccess).length
        ) {
            this.buttons.splice(3, 0, {
                id: 'MEDIACENTER',
                icon: 'business',
            });
        }
    }

    private init() {
        this.initButtons();
        this.globalProgress = false;
        this.route.queryParams.subscribe((data: any) => {
            if (data.mode) {
                this.mode = data.mode;
                if (this.getModeButton().factory) {
                    setTimeout(() => {
                        const ref = this.dynamicComponent.createComponent(
                            this.getModeButton().factory,
                        );
                    });
                }
            } else this.setMode(this.buttons[0].id, true);
        });
        if (this.loginResult.isAdmin) {
            this.showWarning = true;
            this.admin.getServerUpdates().subscribe((data: ServerUpdate[]) => {
                this.updates = data;
            });
            this.refreshUpdateList();
            // this.refreshCatalina();
            this.refreshAppList();
            this.storage.get('admin_job', this.job).subscribe((data: any) => {
                this.job = data;
            });
            this.storage.get('admin_lucene', this.lucene).subscribe((data: any) => {
                this.lucene = data;
            });
            this.reloadJobStatus();
            this.runTpChecks();
            this.runChecks();
            this.admin.getAllJobs().subscribe((jobs) => {
                this.availableJobs = jobs;
                this.prepareJobClasses();
            });
            const interval = setInterval(() => {
                if (this.mode == 'JOBS') this.reloadJobStatus();
            }, 10000);
            this.onDestroyTasks.push(() => clearInterval(interval));
            this.admin.getOAIClasses().subscribe((classes: string[]) => {
                this.oaiClasses = classes;
                this.storage.get('admin_oai').subscribe((data: any) => {
                    if (data) this.oai = data;
                    else {
                        this.oai = {
                            className: classes[0],
                            importerClassName:
                                'org.edu_sharing.repository.server.importer.OAIPMHLOMImporter',
                            recordHandlerClassName:
                                'org.edu_sharing.repository.server.importer.RecordHandlerLOM',
                        };
                    }
                    if (!this.oai.binaryHandlerClassName) this.oai.binaryHandlerClassName = '';
                });
            });
            this.admin.getRepositoryVersion().subscribe(
                (data: string) => {
                    this.repositoryVersion = data;
                },
                (error: any) => {
                    this.repositoryVersion =
                        'Error accessing version information. Are you in dev mode?';
                },
            );
        }
    }

    getOwnAppUrl() {
        return (
            this.connector.getAbsoluteEdusharingUrl() +
            'metadata?format=' +
            this.ownAppMode +
            '&external=true'
        );
    }

    copyOwnApp() {
        UIHelper.copyToClipboard(this.getOwnAppUrl());
        this.toast.toast('ADMIN.APPLICATIONS.COPIED_CLIPBOARD');
    }

    getAppTypes() {
        return Array.from(new Set(this.applications.map((a) => a.type)));
    }
    getApplications(type: string) {
        return this.applications.filter((a) => a.type === type);
    }

    modeIsActive(mode: string) {
        if (this.mode === mode) {
            if (this.buttons.filter((b) => b.id === mode).length === 1) {
                return true;
            }
            this.router.navigate([UIConstants.ROUTER_PREFIX, 'workspace']);
        }
        return false;
    }

    fixTp(check: any) {
        this.tpChecks = [];
        this.admin.getToolpermissions(RestConstants.AUTHORITY_EVERYONE).subscribe((tpIn) => {
            const tp: any = {};
            Object.keys(tpIn).forEach((k) => (tp[k] = tpIn[k].explicit));
            tp[check.name] = 'UNDEFINED';
            this.admin
                .setToolpermissions(RestConstants.AUTHORITY_EVERYONE, tp)
                .subscribe(() => this.runTpChecks());
        });
    }

    supportsUpload(job: JobDescription) {
        return job?.params?.some((p) => p.file);
    }

    setJob(item: any) {
        this.job.name = item.item.title;
        this.job.class = item.item.id;
        this.job.object = item.item.originalObject;
    }
    setJobParamsTemplate() {
        const data: any = {};
        let modified = false;
        for (const param of this.job.object.params) {
            if (param.file) {
                continue;
            }
            data[param.name] =
                param.type === 'boolean' ? param.sampleValue === 'true' : param.sampleValue ?? '';
            if (param.values) {
                data[param.name] = param.values.map((v) => v.name).join('|');
            }
            if (param.array) {
                data[param.name] = [data[param.name]];
            }
            modified = true;
        }
        console.log(data, this.job);
        if (modified) {
            this.job.params = JSON.stringify(data, null, 2);
        }
    }

    async authenticateAsUser(force = false) {
        if (!force) {
            this.toast.showConfigurableDialog({
                title: 'ADMIN.TOOLKIT.AUTHENTICATE_AS_USER',
                message: 'ADMIN.TOOLKIT.AUTHENTICATE_AS_USER_DETAILS',
                buttons: DialogButton.getOkCancel(
                    () => this.toast.closeModalDialog(),
                    () => {
                        this.toast.closeModalDialog();
                        this.authenticateAsUser(true);
                    },
                ),
            });
            return;
        }
        try {
            await this.admin
                .switchAuthentication(this.authenticateAuthority.authorityName)
                .toPromise();
            UIHelper.goToDefaultLocation(this.router, this.platformLocation, this.config, true);
        } catch (e) {}
    }
}
