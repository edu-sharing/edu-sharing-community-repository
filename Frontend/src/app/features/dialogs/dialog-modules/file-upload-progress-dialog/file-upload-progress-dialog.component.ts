import { Component, Inject, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import {
    DialogButton,
    Node,
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
    progress: { name: string; progress: UploadProgress; error?: string }[] = [];
    private resultList: Node[] = [];
    private error = false;
    processed = 0;

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: FileUploadProgressDialogData,
        private dialogRef: CardDialogRef<
            FileUploadProgressDialogData,
            FileUploadProgressDialogResult
        >,
        private nodeService: RestNodeService,
        private translate: TranslateService,
    ) {}

    ngOnInit(): void {
        this.dialogRef.patchConfig({
            buttons: [new DialogButton('CANCEL', { color: 'standard' }, () => this._cancel())],
        });
        for (const file of this.data.files) {
            this.progress.push({ name: file.name, progress: { progress: 0 } });
        }
        this._updateSubtitle();
        this._upload(0);
    }

    private _cancel() {
        if (this.resultList.length > 0) {
            // Close with nodes uploaded until now. Could also delete these nodes.
            this.dialogRef.close(this.resultList);
        } else {
            this.dialogRef.close(null);
        }
    }

    private _upload(number: number) {
        if (number >= this.data.files.length) {
            if (this.error) {
                this.dialogRef.patchConfig({ closable: Closable.Casual });
            } else {
                this.dialogRef.close(this.resultList);
            }
            return;
        }
        if (!this.data.files.item(number).type && !this.data.files.item(number).size) {
            setTimeout(() => {
                this.progress[number].progress.progress = -1;
                this.progress[number].error = 'FORMAT';
                this.error = true;
                this._upload(number + 1);
            }, 50);
            return;
        }
        this.nodeService
            .createNode(
                this.data.parent ? this.data.parent.ref.id : RestConstants.INBOX,
                RestConstants.CCM_TYPE_IO,
                [],
                RestHelper.createNameProperty(this.data.files.item(number).name),
                true,
            )
            .subscribe(
                (data: NodeWrapper) => {
                    this.nodeService
                        .uploadNodeContent(
                            data.node.ref.id,
                            this.data.files.item(number),
                            RestConstants.COMMENT_MAIN_FILE_UPLOAD,
                            'auto',
                            (progress) => {
                                progress.progress = Math.round(progress.progress * 100);
                                this.progress[number].progress = progress;
                            },
                        )
                        .subscribe(
                            () => {
                                this.resultList.push(data.node);
                                this.progress[number].progress.progress = 100;
                                this.processed++;
                                this._updateSubtitle();
                                this._upload(number + 1);
                            },
                            (error) => {
                                this.error = true;
                                this.progress[number].error = this._mapError(error, data.node);
                                this.progress[number].progress.progress = -1;
                                this._upload(number + 1);
                            },
                        );
                },
                (error: any) => {
                    this.error = true;
                    this.progress[number].error = this._mapError(error);
                    this.progress[number].progress.progress = -1;
                    this._upload(number + 1);
                },
            );
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
        if (RestHelper.errorMatchesAny(error, RestConstants.CONTENT_QUOTA_EXCEPTION)) {
            return 'QUOTA';
        }
        if (RestHelper.errorMatchesAny(error, RestConstants.CONTENT_VIRUS_EXCEPTION)) {
            return 'VIRUS';
        }
        return 'UNKNOWN';
    }
}
