import { PlatformLocation } from '@angular/common';
import { AfterViewInit, Component, OnDestroy, OnInit, signal, WritableSignal } from '@angular/core';
import { HOME_REPOSITORY, Node, NodeService } from 'ngx-edu-sharing-api';
import { TranslationsService } from 'ngx-edu-sharing-ui';
import { NgxExtendedPdfViewerService } from 'ngx-extended-pdf-viewer';
import { firstValueFrom } from 'rxjs';
import { RestConstants } from '../../core-module/rest/rest-constants';
import { Closable } from '../../features/dialogs/card-dialog/card-dialog-config';
import { YES_OR_NO } from '../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../../features/dialogs/dialogs.service';
import { Toast } from '../../services/toast';

@Component({
    selector: 'es-pdf-page',
    templateUrl: 'pdf-page.component.html',
    styleUrls: ['pdf-page.component.scss'],
    standalone: false,
})
export class PdfPageComponent implements AfterViewInit, OnDestroy, OnInit {
    static readonly DEFAULT_PDF_MIMETYPE: string = 'application/pdf';
    static readonly DEFAULT_PDF_PREFIX: string = '/eduservlet/download?nodeId=';

    public blob: Blob | undefined;
    editMode: boolean = true;
    hasChanges: WritableSignal<boolean> = signal(true);
    initialized: WritableSignal<boolean> = signal(false);
    isLoading: WritableSignal<boolean> = signal(false);
    isSaving: WritableSignal<boolean> = signal(false);
    language: string = 'de-DE';
    nodeId: string;
    private pdfPrefix: string = PdfPageComponent.DEFAULT_PDF_PREFIX;
    renderNodeId: string;
    private windowRef: any;

    constructor(
        private dialogsService: DialogsService,
        private nodeApi: NodeService,
        private pdfViewerService: NgxExtendedPdfViewerService,
        private platformLocation: PlatformLocation,
        private toast: Toast,
        private translations: TranslationsService,
    ) {}

    /**
     * Initializes the component by defining the language.
     */
    async ngOnInit(): Promise<void> {
        if (this.platformLocation.getBaseHrefFromDOM()) {
            // replace '/' at the end of the base href by '', if existing
            this.pdfPrefix =
                this.platformLocation.getBaseHrefFromDOM()?.replace(/\/$/, '') +
                PdfPageComponent.DEFAULT_PDF_PREFIX;
        }
        await firstValueFrom(this.translations.waitForInit());
        if (this.translations.getLocale()) {
            this.language = this.translations.getLocale();
        }
    }

    /**
     * Initializes event listeners.
     */
    async ngAfterViewInit(): Promise<void> {
        this.initializeCustomEventListeners();
    }

    /**
     * On destroy, remove all event listeners previously added.
     */
    ngOnDestroy(): void {
        window.removeEventListener('message', this.handleApplyNode, false);
    }

    /**
     * Handles the initialization of a custom event to react to node apply.
     */
    private initializeCustomEventListeners(): void {
        // note: the arrow function is necessary to correctly access "this."
        window.addEventListener('message', this.handleApplyNode, false);
    }

    /**
     * Handles the receiving of the APPLY_NODE event.
     *
     * @param event
     */
    private handleApplyNode = async (event: MessageEvent<any>): Promise<void> => {
        // APPLY_NODE event was received and windowRef must exist and not being closed already (as multiple listeners might exist)
        if (event.data.event === 'APPLY_NODE' && !!this.windowRef && !this.windowRef.closed) {
            this.windowRef.close();
            // reset the reference to be not called twice
            this.windowRef = null;
            // use setTimeout to ensure that the windowRef was closed
            // otherwise, the confirm dialog cannot be displayed
            setTimeout(async () => {
                const selectedNode: Node = event.data.data;
                if (selectedNode?.ref.id) {
                    this.nodeId = selectedNode.ref.id;
                    await this.updateNodeId(selectedNode.ref.id);
                }
            }, 100);
        }
    };

    /**
     * Updates the node ID by retrieving the node, checking its media type and confirming the changes.
     *
     * @param nodeId
     */
    async updateNodeId(nodeId: string): Promise<void> {
        this.isLoading.set(true);
        try {
            const node: Node = await firstValueFrom(this.nodeApi.getNode(nodeId));
            if (node.mediatype === 'file-pdf') {
                let passExistingPdfCheck: boolean = !this.renderNodeId;
                if (!passExistingPdfCheck) {
                    const dialogRef = await this.dialogsService.openGenericDialog({
                        title: 'PDF_EDITOR.LOAD_CONTENT_TITLE',
                        message: 'PDF_EDITOR.LOAD_CONTENT_MESSAGE',
                        buttons: YES_OR_NO,
                        closable: Closable.Casual,
                    });
                    dialogRef.afterClosed().subscribe(async (response) => {
                        if (response === 'YES') {
                            this.renderNodeId = this.pdfPrefix + nodeId;
                        } else {
                            return;
                        }
                    });
                } else {
                    this.renderNodeId = this.pdfPrefix + nodeId;
                    this.initialized.set(true);
                }
            } else {
                this.toast.error(null, 'PDF_EDITOR.WRONG_FILE_FORMAT');
            }
        } catch (e) {}
        this.isLoading.set(false);
    }

    /**
     * Opens a new window with the Re-URL parameter set.
     */
    openReurlLink(): void {
        const params = {
            reurl: 'IFRAME',
            q: '.pdf',
        };
        this.windowRef = window.open(
            '/edu-sharing/components/search?' + this.buildQueryParams(params),
            '_blank',
        );
    }

    /**
     * Saves the changes made of the PDF by changing the content of the respective node.
     *
     * @param nodeId
     */
    async saveChanges(nodeId: string) {
        this.isSaving.set(true);
        try {
            this.blob = await this.pdfViewerService.getCurrentDocumentAsBlob();
            await firstValueFrom(
                this.nodeApi.changeContent(
                    HOME_REPOSITORY,
                    nodeId,
                    PdfPageComponent.DEFAULT_PDF_MIMETYPE,
                    RestConstants.COMMENT_CONTENT_UPDATE,
                    {
                        file: this.blob,
                    },
                ),
            );
            this.toast.toast('PDF_EDITOR.CHANGES_SAVED_SUCCESSFULLY');
        } catch (e) {}
        this.isSaving.set(false);
    }

    /**
     * Helper function called by edit mode change in order to trigger a reload of the editor,
     * which is necessary to get all editor options work properly.
     */
    async triggerReload() {
        this.initialized.set(false);
        setTimeout(() => {
            this.initialized.set(true);
        });
    }

    /**
     * Reacts to an annotation change to decide, whether changes were made.
     * TODO: annotation change does currently not work for drawings and stamps.
     */
    changeAnnotation() {
        // console.log('change annotation', this.pdfViewerService.getSerializedAnnotations());
    }

    /**
     * Helper function to build query params.
     *
     * @param obj
     * @param prefix
     */
    private buildQueryParams(obj: any, prefix = ''): string {
        const query = [];

        for (const key in obj) {
            if (obj.hasOwnProperty(key)) {
                const value = obj[key];
                const paramKey = prefix ? `${prefix}[${key}]` : key;

                if (typeof value === 'object' && !Array.isArray(value)) {
                    query.push(this.buildQueryParams(value, paramKey));
                } else if (Array.isArray(value)) {
                    value.forEach((v) => {
                        query.push(`${encodeURIComponent(paramKey)}[]=${encodeURIComponent(v)}`);
                    });
                } else {
                    query.push(`${encodeURIComponent(paramKey)}=${encodeURIComponent(value)}`);
                }
            }
        }

        return query.join('&');
    }
}
