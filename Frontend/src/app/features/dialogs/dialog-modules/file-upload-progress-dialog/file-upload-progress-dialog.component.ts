import { Component, Inject, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { FormatSizePipe } from 'ngx-edu-sharing-ui';
import {
    DialogButton,
    NodeWrapper,
    RestConstants,
    RestHelper,
    RestNodeService,
} from '../../../../core-module/core.module';
import { UploadProgress } from '../../../../core-module/rest/services/rest-connector.service';
import { CARD_DIALOG_DATA, Closable } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import {
    FileUploadProgressDialogData,
    FileUploadProgressDialogResult,
} from './file-upload-progress-dialog-data';
import { map, switchMap } from 'rxjs/operators';
import { DialogsService } from '../../dialogs.service';
import { from, of } from 'rxjs';
import { NodeService, Node } from 'ngx-edu-sharing-api';

/**
 * A dialog that handles uploading a given list of files and shows a progress bar per file to the
 * user.
 */
@Component({
    selector: 'es-file-upload-progress-dialog',
    templateUrl: './file-upload-progress-dialog.component.html',
    styleUrls: ['./file-upload-progress-dialog.component.scss'],
})
export class FileUploadProgressDialogComponent implements OnInit {
    progress: {
        name: string;
        progress: UploadProgress;
        error?: { key: string; variables?: any };
    }[] = [];
    private resultList: Node[] = [];
    private error = false;
    private existingNodes: Node[];
    processed = 0;
    keep = true;
    @ViewChild('existingFiles') existingFilesRef: TemplateRef<undefined>;

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: FileUploadProgressDialogData,
        private dialogRef: CardDialogRef<
            FileUploadProgressDialogData,
            FileUploadProgressDialogResult
        >,
        private nodeService: RestNodeService,
        private nodeApi: NodeService,
        private dialogs: DialogsService,
        private translate: TranslateService,
    ) {}

    ngOnInit(): void {
        this.dialogRef.patchConfig({
            buttons: [new DialogButton('CANCEL', { color: 'standard' }, () => this._cancel())],
        });
        for (const file of this.data.files) {
            this.progress.push({ name: file.name, progress: { progress: 0 } });
        }
        // check for existing child nodes with the same file name
        this._existingNodes()
            .pipe(
                switchMap((existingNodes: Node[]) => {
                    // open the dialog only if this dialog is still open...
                    if (this.dialogRef.getLifecycleState() !== 'open') return of(false);
                    // if some files are already present as nodes then inform the user and ask whether
                    // to keep or overwrite the files
                    this.existingNodes = existingNodes;
                    if (existingNodes.length > 0) {
                        return from(this._openExistingDialog(existingNodes));
                    }
                    // if there are no existing nodes of the same name just proceed and upload
                    return of(true);
                }),
            )
            .subscribe((doUpload) => {
                if (doUpload) {
                    this._updateSubtitle();
                    this._upload(0);
                } else {
                    this._cancel();
                }
            });
    }

    private async _openExistingDialog(existingNodes: Node[]) {
        const multiple = existingNodes.length > 1;
        let fileName, message, messageParameters;
        if (multiple) {
            fileName = null;
            message = 'WORKSPACE.UPLOAD_EXISTS.MULTIPLE';
            messageParameters = null;
        } else {
            fileName = existingNodes[0].name;
            message = 'WORKSPACE.UPLOAD_EXISTS.SINGLE';
            messageParameters = { fileName: fileName };
        }
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'WORKSPACE.UPLOAD_EXISTS.TITLE',
            message,
            messageParameters,
            contentTemplate: this.existingFilesRef,
            context: { $implicit: multiple },
            buttons: [
                { label: 'CANCEL', config: { color: 'standard' } },
                { label: 'WORKSPACE.UPLOAD_EXISTS.UPLOAD', config: { color: 'primary' } },
            ],
        });
        const result = await dialogRef.afterClosed().toPromise();
        return result !== 'CANCEL';
    }

    private _cancel() {
        // first check whether the dialog has already been closed
        if (this.dialogRef.getLifecycleState() !== 'open') return;
        if (this.resultList.length > 0) {
            // Close with nodes uploaded until now. Could also delete these nodes.
            this.dialogRef.close(this.resultList);
        } else {
            this.dialogRef.close(null);
        }
    }

    /**
     * Returns all child nodes of the parent which match the file name of a
     * file to be uploaded.
     * @private
     */
    private _existingNodes() {
        // get all children to compare against them
        return this.nodeService
            .getChildren(this._getParent(), [RestConstants.FILTER_FILES], {
                propertyFilter: [],
            })
            .pipe(
                map((children) => {
                    let existing = [];
                    const fileNames = Array.from(this.data.files).map((file) => file.name);
                    const childNodes = children.nodes;
                    for (let i = 0; i < childNodes.length; i++) {
                        const childNode = childNodes[i];
                        const childFileName = childNode.name;
                        if (fileNames.some((fName) => fName == childFileName)) {
                            existing.push(childNode);
                        }
                    }
                    return existing;
                }),
            );
    }

    private _getParent(): string {
        return this.data.parent ? this.data.parent.ref.id : RestConstants.INBOX;
    }

    private _upload(number: number) {
        if (number >= this.data.files.length) {
            if (this.error) {
                this.dialogRef.patchConfig({ closable: Closable.Casual });
            } else {
                this._cancel();
            }
            return;
        }
        const file = this.data.files.item(number);
        if (!file.type && !file.size) {
            setTimeout(() => {
                this.progress[number].progress.progress = -1;
                this.progress[number].error = { key: 'FORMAT' };
                this.error = true;
                this._upload(number + 1);
            }, 50);
            return;
        }
        const nextUpload = (node: Node) => () => {
            this.resultList.push(node);
            this.progress[number].progress.progress = 100;
            this.processed++;
            this._updateSubtitle();
            this._upload(number + 1);
        };
        const nextError = (node: Node) => (error: any) => {
            this.error = true;
            this.progress[number].error = this._mapError(error, node);
            this.progress[number].progress.progress = -1;
            this._upload(number + 1);
        };
        // check whether the file is already existing and if so
        // if the user also chose to overwrite the old one only create a new version
        const existingNode = this.existingNodes.find((node) => node.name == file.name);
        if (existingNode && !this.keep) {
            this.nodeApi
                .changeContent(
                    existingNode.ref.repo,
                    existingNode.ref.id,
                    'auto',
                    RestConstants.COMMENT_CONTENT_UPDATE,
                    { file },
                )
                .subscribe(nextUpload(existingNode), nextError(existingNode));
        } else {
            this.nodeService
                .createNode(
                    this._getParent(),
                    RestConstants.CCM_TYPE_IO,
                    [],
                    RestHelper.createNameProperty(file.name),
                    this.keep,
                )
                .subscribe((data: NodeWrapper) => {
                    this.nodeService
                        .uploadNodeContent(
                            data.node.ref.id,
                            file,
                            RestConstants.COMMENT_MAIN_FILE_UPLOAD,
                            'auto',
                            (progress) => {
                                progress.progress = Math.round(progress.progress * 100);
                                this.progress[number].progress = progress;
                            },
                        )
                        .subscribe(nextUpload(data.node), nextError(data.node));
                }, nextError(null));
        }
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
                    actualSize: new FormatSizePipe(this.translate).transform(
                        errorData.details.actualSize,
                    ),
                    maxSize: new FormatSizePipe(this.translate).transform(
                        errorData.details.maxSize,
                    ),
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
