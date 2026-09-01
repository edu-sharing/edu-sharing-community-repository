import { Component, OnInit, TemplateRef, ViewChild, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { FormatSizePipe } from 'ngx-edu-sharing-ui';
import {
    DialogButton,
    RestConstants,
    RestHelper,
    RestNodeService,
} from '../../../../core-module/core.module';
import { UploadProgress } from '../../../../core-module/rest/services/rest-connector.service';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import {
    FileUploadProgressDialogData,
    FileUploadProgressDialogResult,
} from './file-upload-progress-dialog-data';
import { DialogsService } from '../../dialogs.service';
import { ApiErrorResponse, Node, NodeService } from 'ngx-edu-sharing-api';
import { firstValueFrom } from 'rxjs';
import { isDuplicateNodeNameError } from '../../../../util/rest-errors';

/** What to do with a file that already exists at the target location. */
type DuplicateDecision = 'keep' | 'overwrite' | 'cancel';

/**
 * A dialog that handles uploading a given list of files and shows a progress bar per file to the
 * user.
 *
 * Duplicates are not detected upfront: Files are always created with `renameIfExists=false` first,
 * so the backend reports an existing file with a `409` response. Only then the user is asked how to
 * proceed (unless `duplicateBehaviour` already defines it).
 */
@Component({
    selector: 'es-file-upload-progress-dialog',
    templateUrl: './file-upload-progress-dialog.component.html',
    styleUrls: ['./file-upload-progress-dialog.component.scss'],
    standalone: false,
})
export class FileUploadProgressDialogComponent implements OnInit {
    data = inject<FileUploadProgressDialogData>(CARD_DIALOG_DATA);
    private dialogRef =
        inject<CardDialogRef<FileUploadProgressDialogData, FileUploadProgressDialogResult>>(
            CardDialogRef,
        );
    private nodeService = inject(RestNodeService);
    private nodeApi = inject(NodeService);
    private dialogs = inject(DialogsService);
    private translate = inject(TranslateService);
    private formatSizePipe = inject(FormatSizePipe);

    progress: {
        name: string;
        progress: UploadProgress;
        error?: { key: string; variables?: any };
    }[] = [];
    private resultList: Node[] = [];
    private error = false;
    /** The decision the user made for the first duplicate, applied to the whole batch. */
    private duplicateDecision: DuplicateDecision = null;
    /** Lazily fetched children of the target folder, only required to overwrite an existing file. */
    private childNodes: Node[] = null;
    processed = 0;
    /** Bound to the radio group of the "file exists" dialog: `true` = keep both files. */
    keep = true;
    @ViewChild('existingFiles') existingFilesRef: TemplateRef<undefined>;

    ngOnInit(): void {
        this.dialogRef.patchConfig({
            buttons: [
                new DialogButton('CANCEL', { color: 'standard' }, () => this._done('CANCELED')),
            ],
        });
        for (const file of this.data.files) {
            this.progress.push({ name: file.name, progress: { progress: 0 } });
        }
        this._updateSubtitle();
        this._upload(0);
    }

    private _done(status: 'CANCELED' | 'FINISHED') {
        // first check whether the dialog has already been closed
        if (this.dialogRef.getLifecycleState() !== 'open') return;
        if (this.resultList.length > 0) {
            // Close with nodes uploaded until now. Could also delete these nodes.
            this.dialogRef.close({
                status,
                nodes: this.resultList,
            });
        } else {
            this.dialogRef.close(null);
        }
    }

    private _getParent(): string {
        return this.data.parent ? this.data.parent.ref.id : RestConstants.INBOX;
    }

    private _upload(number: number) {
        if (number >= this.data.files.length) {
            if (this.error) {
                this.dialogRef.patchConfig({
                    buttons: DialogButton.getNextCancel(
                        () => this._done('CANCELED'),
                        () => this._done('FINISHED'),
                    ),
                });
            } else {
                this._done('FINISHED');
            }
            return;
        }
        const file = this.data.files[number];
        if (!file.type && !file.size) {
            setTimeout(() => {
                this.progress[number].progress.progress = -1;
                this.progress[number].error = { key: 'FORMAT' };
                this.error = true;
                this._upload(number + 1);
            }, 50);
            return;
        }
        void this._uploadFile(number, file);
    }

    private async _uploadFile(number: number, file: File): Promise<void> {
        let node: Node;
        try {
            // in 'unique' mode duplicates are renamed by the backend, in any other mode we let the
            // backend tell us about an existing file via a 409 response
            node = await this._createChild(file, this.data.duplicateBehaviour === 'unique');
        } catch (error) {
            if (!isDuplicateNodeNameError(error)) {
                this._failed(number, error, null);
                return;
            }
            // the toast/error handler must not report the (expected) conflict
            (error as ApiErrorResponse).preventDefault?.();
            const decision = await this._resolveDuplicate(file);
            if (decision === 'cancel') {
                // keep the files uploaded so far, but don't continue with the remaining ones
                this._done('FINISHED');
                return;
            }
            if (decision === 'overwrite') {
                const existingNode = await this._findExistingNode(file.name);
                if (existingNode) {
                    try {
                        await this._changeContent(
                            number,
                            existingNode,
                            file,
                            RestConstants.COMMENT_CONTENT_UPDATE,
                        );
                    } catch (error) {
                        // don't delete the existing node, it is not ours
                        this._failed(number, error, null);
                        return;
                    }
                    this._succeeded(number, existingNode);
                    return;
                }
                // the existing node could not be resolved -> fall back to keeping both files
            }
            try {
                node = await this._createChild(file, true);
            } catch (error) {
                this._failed(number, error, null);
                return;
            }
        }
        try {
            await this._changeContent(number, node, file, RestConstants.COMMENT_MAIN_FILE_UPLOAD);
        } catch (error) {
            this._failed(number, error, node);
            return;
        }
        this._succeeded(number, node);
    }

    private _createChild(file: File, renameIfExists: boolean): Promise<Node> {
        return firstValueFrom(
            this.nodeApi.createChild({
                repository: RestConstants.HOME_REPOSITORY,
                node: this._getParent(),
                type: RestConstants.CCM_TYPE_IO,
                renameIfExists,
                body: RestHelper.createNameProperty(file.name),
            }),
        );
    }

    private _changeContent(
        number: number,
        node: Node,
        file: File,
        versionComment: string,
    ): Promise<Node> {
        const start = new Date().getTime();
        return this.nodeApi
            .changeContent(
                node.ref.repo,
                node.ref.id,
                'auto',
                versionComment,
                { file },
                ({ loaded, total }) => {
                    const elapsed = (new Date().getTime() - start) / 1000;
                    this.progress[number].progress = {
                        start,
                        loaded,
                        total,
                        elapsed,
                        progress: total ? Math.round((loaded / total) * 100) : 0,
                        remaining: loaded ? ((total - loaded) * elapsed) / loaded : 0,
                    };
                },
            )
            .toPromise();
    }

    private _succeeded(number: number, node: Node): void {
        this.resultList.push(node);
        this.progress[number].progress.progress = 100;
        this.processed++;
        this._updateSubtitle();
        this._upload(number + 1);
    }

    private _failed(number: number, error: any, node: Node): void {
        this.error = true;
        this.progress[number].error = this._mapError(error, node);
        this.progress[number].progress.progress = -1;
        this._upload(number + 1);
    }

    /**
     * Determines how to handle a file that already exists at the target location. The user is asked
     * at most once per dialog, the decision is applied to all remaining files.
     */
    private async _resolveDuplicate(file: File): Promise<DuplicateDecision> {
        if (this.data.duplicateBehaviour === 'replace') {
            return 'overwrite';
        }
        if (this.duplicateDecision) {
            return this.duplicateDecision;
        }
        // don't open the dialog if this dialog has already been closed
        if (this.dialogRef.getLifecycleState() !== 'open') {
            return 'cancel';
        }
        this.duplicateDecision = await this._openExistingDialog(file.name);
        return this.duplicateDecision;
    }

    private async _openExistingDialog(fileName: string): Promise<DuplicateDecision> {
        // the decision applies to all files of this upload
        const multiple = this.data.files.length > 1;
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'WORKSPACE.UPLOAD_EXISTS.TITLE',
            message: multiple
                ? 'WORKSPACE.UPLOAD_EXISTS.MULTIPLE'
                : 'WORKSPACE.UPLOAD_EXISTS.SINGLE',
            messageParameters: multiple ? null : { fileName },
            contentTemplate: this.existingFilesRef,
            context: { $implicit: multiple },
            buttons: [
                { label: 'CANCEL', config: { color: 'standard' } },
                { label: 'WORKSPACE.UPLOAD_EXISTS.UPLOAD', config: { color: 'primary' } },
            ],
        });
        const result = await dialogRef.afterClosed().toPromise();
        if (result === 'CANCEL') {
            return 'cancel';
        }
        return this.keep ? 'keep' : 'overwrite';
    }

    /**
     * Resolves the already existing node with the given file name inside the target folder. The
     * children are only fetched when an existing file actually needs to be overwritten.
     */
    private async _findExistingNode(fileName: string): Promise<Node | null> {
        if (!this.childNodes) {
            try {
                const children = await this.nodeApi
                    .getChildren(this._getParent(), {
                        filter: [RestConstants.FILTER_FILES],
                        maxItems: RestConstants.COUNT_UNLIMITED,
                    })
                    .toPromise();
                this.childNodes = children.nodes;
            } catch (error) {
                console.warn(error);
                return null;
            }
        }
        return this.childNodes.find((node) => node.name === fileName) ?? null;
    }

    private _updateSubtitle(): void {
        this.translate
            .get('WORKSPACE.UPLOAD_SUBTITLE', {
                progress: this.processed,
                total: this.progress.length,
            })
            .subscribe((subtitle) => this.dialogRef.patchConfig({ subtitle }));
    }

    private _mapError(error: any, node: Node = null) {
        // delete the now orphan node since it's empty
        if (node) {
            this.nodeService.deleteNode(node.ref.id, false).subscribe(() => {});
        }
        let i18nName: string;
        let variables: any;
        if (RestHelper.errorMatchesAny(error, RestConstants.CONTENT_VIRUS_SCAN_FAILED_EXCEPTION)) {
            i18nName = 'VIRUS_SCAN_FAILED';
        } else if (RestHelper.errorMatchesAny(error, RestConstants.CONTENT_VIRUS_EXCEPTION)) {
            i18nName = 'VIRUS';
        } else if (
            RestHelper.errorMatchesAny(error, RestConstants.CONTENT_MIMETYPE_VERIFICATION_EXCEPTION)
        ) {
            i18nName = 'MIMETYPE_VERIFICATION';
        } else if (
            RestHelper.errorMatchesAny(
                error,
                RestConstants.CONTENT_NODE_FILE_SIZE_EXCEEDED_EXCEPTION,
            )
        ) {
            i18nName = 'FILE_SIZE_EXCEEDED';
            try {
                const errorData = JSON.parse(error.response);
                variables = {
                    actualSize: this.formatSizePipe.transform(errorData.details.actualSize),
                    maxSize: this.formatSizePipe.transform(errorData.details.maxSize),
                };
            } catch (e) {
                console.warn(e);
            }
        } else if (
            RestHelper.errorMatchesAny(
                error,
                RestConstants.CONTENT_FILE_EXTENSION_VERIFICATION_EXCEPTION,
            )
        ) {
            i18nName = 'FILETYPE_VERIFICATION';
        } else if (RestHelper.errorMatchesAny(error, RestConstants.CONTENT_QUOTA_EXCEPTION)) {
            i18nName = 'QUOTA';
        } else {
            i18nName = 'UNKNOWN';
        }
        return { key: i18nName, variables };
    }
}
