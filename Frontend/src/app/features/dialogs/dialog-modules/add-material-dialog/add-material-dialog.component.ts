import { trigger } from '@angular/animations';
import {
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnInit,
    Output,
    signal,
    ViewChild,
    inject,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl } from '@angular/forms';
import {
    ClientutilsV1Service,
    Node,
    RestConstants,
    UserQuota,
    UserService,
    WebsiteInformation,
} from 'ngx-edu-sharing-api';
import { ListItem, UIAnimation } from 'ngx-edu-sharing-ui';
import * as rxjs from 'rxjs';
import { BehaviorSubject } from 'rxjs';
import { debounce, finalize, map, switchMap, tap } from 'rxjs/operators';
import { ConfigurationService, DialogButton } from '../../../../core-module/core.module';
import { Toast } from '../../../../services/toast';
import { CARD_DIALOG_DATA, Closable } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { DialogsService, DialogTemplate } from '../../dialogs.service';
import { AddMaterialDialogData, AddMaterialDialogResult } from './add-material-dialog-data';
import { TemplateSlot } from '../../../../main/navigation/main-nav.service';

@Component({
    selector: 'es-add-material-dialog',
    templateUrl: './add-material-dialog.component.html',
    styleUrls: ['./add-material-dialog.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
        trigger('openOverlay', UIAnimation.openOverlay()),
    ],
    standalone: false,
})
export class AddMaterialDialogComponent implements OnInit {
    data = inject<AddMaterialDialogData | null>(CARD_DIALOG_DATA, { optional: true });
    private dialogRef = inject<CardDialogRef<
        AddMaterialDialogData,
        AddMaterialDialogResult
    > | null>(CardDialogRef<AddMaterialDialogData, AddMaterialDialogResult>, { optional: true });
    private clientUtils = inject(ClientutilsV1Service);
    private configService = inject(ConfigurationService);
    dialogs = inject(DialogsService);
    private toast = inject(Toast);
    private userService = inject(UserService);

    readonly DialogTemplate = DialogTemplate;
    @Input() dialogData?: AddMaterialDialogData;
    @Output() dialogResult: EventEmitter<AddMaterialDialogResult> =
        new EventEmitter<AddMaterialDialogResult>();
    @ViewChild('fileSelect') private file: ElementRef;

    private disabled = true;

    protected readonly linkControl = new FormControl('');
    protected ltiEnabled: boolean;
    protected ltiActivated: boolean;
    protected ltiConsumerKey: string;
    protected ltiSharedSecret: string;
    protected userQuota: UserQuota;
    protected websiteInformation: WebsiteInformation;
    hideFileUpload = signal(false);
    protected hideLink = false;
    protected isFileOver = false;
    protected loadingWebsiteInformation = false;
    protected columns = [
        new ListItem('NODE', RestConstants.LOM_PROP_TITLE),
        new ListItem('NODE', RestConstants.CM_PROP_C_CREATED),
    ];
    protected parent$ = new BehaviorSubject<Node>(null);
    selectedFiles = signal<File[]>([]);

    get currentData(): AddMaterialDialogData {
        return this.data || this.dialogData;
    }

    constructor() {
        this.userService
            .observeCurrentUser()
            .pipe(takeUntilDestroyed())
            .subscribe((user) => (this.userQuota = user?.person.quota));
        this.configService
            .get('upload.lti.enabled', false)
            .subscribe((ltiEnabled) => (this.ltiEnabled = ltiEnabled));
    }

    ngOnInit(): void {
        this.parent$.next(this.currentData.parent);
        this.setState('');
        this.registerLink();
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
                    this.updateHideFileUpload();
                },
                error: () => {
                    this.loadingWebsiteInformation = false;
                },
            });
    }

    private updateHideFileUpload(): void {
        if (this.hideFileUpload() && !this.linkControl.value.trim()) {
            this.hideFileUpload.set(false);
        } else if (!this.hideFileUpload() && this.websiteInformation) {
            this.hideFileUpload.set(true);
        }
    }

    // dialog-specific function due to button patch
    cancel() {
        this.dialogRef?.close(null);
    }

    selectFile() {
        this.file.nativeElement.click();
    }

    /**
     * Closes the dialog and returns the given file list to the caller.
     */
    closeWithFiles(fileList: FileList) {
        this.selectedFiles.set(Array.from(fileList));
        if (this.currentData.showFiles === true) {
            this.setState('');
            this.updateHideFileUpload();
            return;
        }
        const dialogResult: AddMaterialDialogResult = {
            kind: 'file',
            files: this.selectedFiles(),
            parent: this.parent$.value,
        };
        this.dialogRef ? this.dialogRef.close(dialogResult) : this.dialogResult.emit(dialogResult);
    }

    setLink() {
        if (this.disabled) {
            // To nothing
        } else if (this.selectedFiles().length) {
            const dialogResult: AddMaterialDialogResult = {
                kind: 'file',
                files: this.selectedFiles(),
                parent: this.parent$.value,
            };
            this.dialogRef
                ? this.dialogRef.close(dialogResult)
                : this.dialogResult.emit(dialogResult);
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
            this.closeWithLink();
        }
    }

    private closeWithLink(): void {
        const dialogResult: AddMaterialDialogResult = {
            kind: 'link',
            link: this.linkControl.value,
            parent: this.parent$.value,
            lti: this.ltiActivated
                ? {
                      consumerKey: this.ltiConsumerKey,
                      sharedSecret: this.ltiSharedSecret,
                  }
                : null,
        };
        this.dialogRef ? this.dialogRef.close(dialogResult) : this.dialogResult.emit(dialogResult);
    }

    setState(link: string) {
        link = link.trim();
        this.disabled = !link && !this.selectedFiles().length;
        this.updateButtons();
        this.dialogRef?.patchConfig({ closable: Closable.Standard });
    }

    parentSelected(parent: Node) {
        this.parent$.next(parent);
        // dialog-specific patches
        this.updateButtons();
        this.dialogRef?.patchConfig({ closable: Closable.Standard });
    }

    updateButtons() {
        const [okButton] = DialogButton.getOk(() => this.setLink());
        okButton.disabled = !this.canSave();
        const buttons = [...DialogButton.getCancel(() => this.cancel()), okButton];
        this.dialogRef?.patchConfig({ buttons });
    }

    canSave() {
        return !(this.disabled || (this.currentData.chooseParent && !this.parent$.value));
    }

    protected readonly TemplateSlot = TemplateSlot;
    protected readonly DialogsService = DialogsService;

    removeFile(file: File) {
        this.selectedFiles.update((files) => files.filter((f) => f !== file));
        this.setState('');
    }

    /**
     * Clears the entered link and the picked files. Needed when the component is embedded
     * (i.e. not used as a dialog that gets destroyed) and stays open for the next upload.
     */
    reset(): void {
        this.selectedFiles.set([]);
        // silent: the valueChanges pipeline would re-fetch the website information for ''
        this.linkControl.setValue('', { emitEvent: false });
        this.websiteInformation = null;
        this.loadingWebsiteInformation = false;
        this.hideFileUpload.set(false);
        this.ltiActivated = false;
        this.ltiConsumerKey = null;
        this.ltiSharedSecret = null;
        if (this.file) {
            // allows re-picking the same file, which would otherwise not fire a change event
            this.file.nativeElement.value = '';
        }
        this.setState('');
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
