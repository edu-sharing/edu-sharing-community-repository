import { trigger } from '@angular/animations';
import {
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { FormControl } from '@angular/forms';
import { ClientutilsV1Service, WebsiteInformation } from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { catchError, debounce, finalize, map, switchMap, tap } from 'rxjs/operators';
import {
    ConfigurationService,
    DialogButton,
    IamUser,
    ListItem,
    Node,
    ParentList,
    RestConstants,
    RestIamService,
    RestNodeService,
    RestSearchService,
    SessionStorageService,
} from '../../../core-module/core.module';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import { LinkData } from '../../../core-ui-module/node-helper.service';
import { Toast } from '../../../core-ui-module/toast';

@Component({
    selector: 'es-workspace-file-upload-select',
    templateUrl: 'file-upload-select.component.html',
    styleUrls: ['file-upload-select.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
        trigger('openOverlay', UIAnimation.openOverlay()),
    ],
})
export class WorkspaceFileUploadSelectComponent implements OnInit, OnChanges {
    @ViewChild('fileSelect') file: ElementRef;
    @ViewChild('link') linkRef: ElementRef;

    /**
     * priority, useful if the dialog seems not to be in the foreground
     * Values greater 0 will raise the z-index
     */
    @Input() priority = 0;
    /**
     * Allow multiple files uploaded
     * @type {boolean}
     */
    @Input() multiple = true;
    /**
     * Should this widget display that it supports dropping
     * @type {boolean}
     */
    @Input() supportsDrop = true;
    @Input() isFileOver = false;
    /**
     * Allow the user to use a file picker to choose the parent?
     */
    @Input() showPicker = false;
    /**
     * Show the lti option and support generation of lti files?
     * @type {boolean}
     */
    @Input() showLti = true;
    @Input() parent: Node;

    @Output() parentChange = new EventEmitter();
    @Output() onCancel = new EventEmitter();
    @Output() onFileSelected = new EventEmitter<FileList>();
    @Output() onLinkSelected = new EventEmitter<LinkData>();

    disabled = true;
    chooseParent = false;
    showSaveParent = false;
    saveParent = false;
    breadcrumbs: {
        nodes: Node[];
        homeLabel: string;
        homeIcon: string;
    };
    ltiAllowed: boolean;
    ltiActivated: boolean;
    ltiConsumerKey: string;
    ltiSharedSecret: string;
    // private ltiTool: Node;
    buttons: DialogButton[];
    user: IamUser;
    readonly linkControl = new FormControl('');
    websiteInformation: WebsiteInformation;
    hideFileUpload = false;
    loadingWebsiteInformation = false;
    showInvalidUrlMessage = false;
    columns = [
        new ListItem('NODE', RestConstants.LOM_PROP_TITLE),
        new ListItem('NODE', RestConstants.CM_PROP_C_CREATED),
    ];

    constructor(
        private nodeService: RestNodeService,
        private iamService: RestIamService,
        private searchService: RestSearchService,
        private storageService: SessionStorageService,
        public configService: ConfigurationService,
        private toast: Toast,
        private clientUtils: ClientutilsV1Service,
    ) {
        this.setState('');
        this.iamService.getCurrentUserAsync().then((user) => {
            this.user = user;
        });
    }

    ngOnInit(): void {
        this.registerLink();
    }
    ngOnChanges(changes: SimpleChanges) {
        if (changes?.parent) {
            this.getBreadcrumbs(this.parent).subscribe(
                (breadcrumbs) => (this.breadcrumbs = breadcrumbs),
            );
        }
    }

    private registerLink(): void {
        this.linkControl.valueChanges
            .pipe(
                // Don't let the user submit the link until we fetched website information.
                tap(() => this.setState('')),
                map((url) => getValidHttpUrl(url)),
                debounce((url) => (url ? rxjs.timer(500) : rxjs.timer(0))),
                tap(() => {
                    this.loadingWebsiteInformation = true;
                    this.websiteInformation = null;
                    this.showInvalidUrlMessage = false;
                }),
                switchMap((url) =>
                    url ? this.clientUtils.getWebsiteInformation({ url }) : rxjs.of(null),
                ),
                finalize(() => (this.loadingWebsiteInformation = false)),
            )
            .subscribe({
                next: (websiteInformation) => {
                    this.loadingWebsiteInformation = false;
                    this.websiteInformation = websiteInformation;
                    if (websiteInformation) {
                        this.setState(this.linkControl.value);
                    }
                    this.updateShowInvalidUrlMessage();
                    this.updateHideFileUpload();
                },
                error: () => {
                    this.loadingWebsiteInformation = false;
                },
            });
    }

    private updateHideFileUpload(): void {
        if (this.hideFileUpload && !this.linkControl.value.trim()) {
            this.hideFileUpload = false;
        } else if (!this.hideFileUpload && this.websiteInformation) {
            this.hideFileUpload = true;
        }
    }

    private updateShowInvalidUrlMessage(): void {
        this.showInvalidUrlMessage =
            this.linkControl.value.trim() && !this.websiteInformation?.page;
    }

    cancel() {
        this.onCancel.emit();
    }

    selectFile() {
        this.file.nativeElement.click();
    }

    onDrop(fileList: FileList) {
        this.onFileSelected.emit(fileList);
    }

    async filesSelected(event: any) {
        this.onFileSelected.emit(event.target.files);
    }

    setLink() {
        if (this.disabled) {
            // To nothing
        } else if (this.ltiActivated && (!this.ltiConsumerKey || !this.ltiSharedSecret)) {
            const params = {
                link: {
                    caption: 'WORKSPACE.TOAST.LTI_FIELDS_REQUIRED_LINK',
                    callback: () => {
                        this.ltiActivated = false;
                        this.setLink();
                    },
                },
            };
            this.toast.error(null, 'WORKSPACE.TOAST.LTI_FIELDS_REQUIRED', null, null, null, params);
        } else {
            this.emitLinkSelected();
        }
    }

    private emitLinkSelected(): void {
        this.onLinkSelected.emit({
            link: this.linkControl.value,
            lti: this.ltiActivated,
            parent: this.parent,
            consumerKey: this.ltiConsumerKey,
            sharedSecret: this.ltiSharedSecret,
        });
    }

    setState(link: string) {
        link = link.trim();
        this.disabled = !link;
        this.ltiAllowed = true;
        this.updateButtons();
        // if (this.cleanupUrlForLti(link)) {
        //     this.searchService
        //         .search(
        //             [
        //                 {
        //                     property: 'url',
        //                     values: [this.cleanupUrlForLti(link)],
        //                 },
        //             ],
        //             [],
        //             null,
        //             RestConstants.CONTENT_TYPE_ALL,
        //             RestConstants.HOME_REPOSITORY,
        //             RestConstants.DEFAULT,
        //             [],
        //             'tool_instances',
        //         )
        //         .subscribe((result: NodeList) => {
        //             // for now, always allow
        //             this.ltiAllowed = result.nodes.length > 0 || true;
        //             if (result.nodes.length) {
        //                 this.nodeService
        //                     .getNodeMetadata(
        //                         result.nodes[0].parent.id,
        //                         [],
        //                         result.nodes[0].parent.repo,
        //                     )
        //                     .subscribe((data: NodeWrapper) => {
        //                         this.ltiTool = data.node;
        //                     });
        //             }
        //         });
        // }
    }

    parentChoosed(event: Node[]) {
        this.showSaveParent = true;
        this.parent = event[0];
        this.parentChange.emit(this.parent);
        this.chooseParent = false;
    }

    updateButtons() {
        const ok = new DialogButton('OK', { color: 'primary' }, () => this.setLink());
        ok.disabled = this.disabled || (this.showPicker && !this.parent);
        this.buttons = [new DialogButton('CANCEL', { color: 'standard' }, () => this.cancel()), ok];
    }

    private cleanupUrlForLti(link: string) {
        let start = link.indexOf('://');
        if (start == -1) return null;
        start += 3;
        const end = link.indexOf('/', start);
        if (end == -1) return null;
        return link.substr(start, end - start);
    }

    private getBreadcrumbs(node: Node) {
        if (node && node.ref.id !== RestConstants.USERHOME) {
            return this.nodeService.getNodeParents(node.ref.id).pipe(
                map((parentList) => this.getBreadcrumbsByParentList(parentList)),
                catchError(() =>
                    rxjs.of(
                        this.getBreadcrumbsByParentList({
                            nodes: [node],
                            pagination: null,
                            scope: 'UNKNOWN',
                        }),
                    ),
                ),
            );
        } else {
            return rxjs.of(null);
        }
    }

    private getBreadcrumbsByParentList(parentList: ParentList) {
        const nodes = parentList.nodes.reverse();
        switch (parentList.scope) {
            case 'MY_FILES':
            // api will return null if fullPath was requested (i.e. as admin)
            case null:
                return {
                    nodes,
                    homeLabel: 'WORKSPACE.MY_FILES',
                    homeIcon: 'person',
                };
            case 'SHARED_FILES':
                return {
                    nodes,
                    homeLabel: 'WORKSPACE.SHARED_FILES',
                    homeIcon: 'group',
                };
            case 'UNKNOWN':
                return {
                    nodes,
                    homeLabel: 'WORKSPACE.RESTRICTED_FOLDER',
                    homeIcon: 'folder',
                };
            default:
                console.warn(`Unknown scope "${parentList.scope}"`);
                return {
                    nodes,
                    homeLabel: null,
                    homeIcon: null,
                };
        }
    }

    async setSaveParent(status: boolean) {
        if (status) {
            await this.storageService.set('defaultInboxFolder', this.parent.ref.id);
            this.toast.toast('TOAST.STORAGE_LOCATION_SAVED', { name: this.parent.name });
        } else {
            await this.storageService.delete('defaultInboxFolder');
            this.toast.toast('TOAST.STORAGE_LOCATION_RESET');
        }
    }
}

// Adapted from https://stackoverflow.com/questions/5717093/check-if-a-javascript-string-is-a-url
function getValidHttpUrl(url: string): string {
    url = url?.trim();
    if (!url) {
        return null;
    }
    if (!(url.startsWith('http://') || url.startsWith('https://'))) {
        url = 'http://' + url;
    }
    try {
        const parsedUrl = new URL(url);
        if (parsedUrl.protocol === 'http:' || parsedUrl.protocol === 'https:') {
            return url;
        }
    } catch (e) {
        // Return null
    }
    return null;
}
