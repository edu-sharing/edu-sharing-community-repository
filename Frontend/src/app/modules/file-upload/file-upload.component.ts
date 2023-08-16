import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {
    FrameEventsService,
    Node,
    RestConnectorService,
    RestConstants,
    RestNodeService,
    TemporaryStorageService,
} from '../../core-module/core.module';
import { TranslationsService } from 'ngx-edu-sharing-ui';
import { NodeHelperService } from '../../core-ui-module/node-helper.service';

@Component({
    selector: 'es-file-upload',
    templateUrl: 'file-upload.component.html',
    styleUrls: ['file-upload.component.scss'],
})
export class FileUploadComponent {
    filesToUpload: FileList;
    loading = true;
    _showUploadSelect: boolean;

    set showUploadSelect(showUploadSelect: boolean) {
        this._showUploadSelect = showUploadSelect;
    }
    get showUploadSelect() {
        return this._showUploadSelect;
    }
    parent: Node;
    private reurl: string;
    constructor(
        private translations: TranslationsService,
        private nodeHelper: NodeHelperService,
        private connector: RestConnectorService,
        private temporaryStorage: TemporaryStorageService,
        private events: FrameEventsService,
        private router: Router,
        private route: ActivatedRoute,
        private node: RestNodeService,
    ) {
        this.translations.waitForInit().subscribe(() => {
            this.connector.isLoggedIn(false).subscribe((login) => {
                if (login.statusCode === RestConstants.STATUS_CODE_OK) {
                    this.nodeHelper.getDefaultInboxFolder().subscribe((n) => {
                        this.parent = n;
                        this.route.queryParams.subscribe((params) => {
                            this.reurl = params['reurl'];
                        });
                        this._showUploadSelect = true;
                        this.loading = false;
                    });
                }
            });
        });
    }

    uploadNodes(event: FileList) {
        this._showUploadSelect = false;
        this.filesToUpload = event;
    }
    onDone(node: Node[]) {
        if (node == null) {
            // canceled;
            this._showUploadSelect = true;
            return;
        }
        this.nodeHelper.addNodeToLms(node[0], this.reurl);
        window.close();
    }

    cancel() {
        this.events.broadcastEvent(FrameEventsService.EVENT_UPLOAD_CANCELED);
        window.close();
    }
}
