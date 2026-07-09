import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Node, NodeService, NodeVersion, NodeVersionEntries } from 'ngx-edu-sharing-api';
import { BehaviorSubject } from 'rxjs';
import { distinctUntilChanged, filter } from 'rxjs/operators';
import { RestConstants, RestNodeService } from '../../../core-module/core.module';
import { SharedModule } from '../../../shared/shared.module';

@Component({
    selector: 'es-workspace-metadata',
    templateUrl: 'metadata.component.html',
    styleUrls: ['metadata.component.scss'],
    imports: [SharedModule],
})
export class WorkspaceMetadataComponent implements OnInit {
    private translate = inject(TranslateService);
    private nodeApi = inject(RestNodeService);
    private nodeService = inject(NodeService);

    @Input() set node(node: Node) {
        this.nodeSubject.next(node);
    }

    @Output() editMetadata = new EventEmitter();
    @Output() display = new EventEmitter<Node>();
    @Output() restore = new EventEmitter();

    dataLoaded = false;
    loading = true;
    nodeObject: Node;
    versions: NodeVersion[];
    versionsLoading = false;

    private nodeSubject = new BehaviorSubject<Node>(null);

    ngOnInit(): void {
        this.nodeSubject
            .pipe(
                filter((node) => node !== null),
                distinctUntilChanged(),
            )
            .subscribe((node) => this.load(node));
    }

    private async load(node: Node) {
        this.versions = null;
        this.versionsLoading = true;
        this.loading = true;
        this.dataLoaded = true;
        this.nodeObject = (
            await this.nodeApi.getNodeMetadata(node.ref.id, [RestConstants.ALL]).toPromise()
        ).node;
        this.loading = false;
        const currentNode = this.nodeObject;
        this.nodeService
            .getVersionsMetadata(this.nodeObject.ref.repo, this.nodeObject.ref.id)
            .subscribe((data: NodeVersionEntries) => {
                if (currentNode !== this.nodeObject) return;
                this.versions = data.versions.reverse();
                for (const version of this.versions) {
                    if (version.comment) {
                        if (
                            version.comment === RestConstants.COMMENT_MAIN_FILE_UPLOAD ||
                            version.comment === RestConstants.COMMENT_METADATA_UPDATE ||
                            version.comment === RestConstants.COMMENT_CONTRIBUTOR_UPDATE ||
                            version.comment === RestConstants.COMMENT_CONTENT_UPDATE ||
                            version.comment === RestConstants.COMMENT_LICENSE_UPDATE ||
                            version.comment === RestConstants.COMMENT_NODE_PUBLISHED ||
                            version.comment === RestConstants.COMMENT_PREVIEW_CHANGED ||
                            version.comment.startsWith(RestConstants.COMMENT_EDITOR_UPLOAD)
                        ) {
                            const parameters = version.comment.split(',');
                            let editor = '';
                            if (parameters.length > 1)
                                editor = this.translate.instant(
                                    'CONNECTOR.' + parameters[1] + '.NAME',
                                );
                            version.comment = this.translate.instant(
                                'WORKSPACE.METADATA.COMMENT.' + parameters[0],
                                { editor },
                            );
                        }
                    }
                }
                let i = 0;
                for (const version of this.versions) {
                    if (this.isCurrentVersion(version)) {
                        this.versions.splice(i, 1);
                        this.versions.splice(0, 0, version);
                        break;
                    }
                    i++;
                }
                this.versionsLoading = false;
            });
    }

    isCurrentVersion(version: NodeVersion): boolean {
        if (!this.nodeObject) return false;
        const prop = this.nodeObject.properties[RestConstants.LOM_PROP_LIFECYCLE_VERSION];
        if (!prop) return false;

        return prop[0] == version.version.major + '.' + version.version.minor;
    }

    doDisplay(version: string = null) {
        this.nodeObject.content.version = version;
        this.display.emit(this.nodeObject);
    }

    displayVersion(version: NodeVersion) {
        if (this.isCurrentVersion(version)) this.doDisplay();
        else this.doDisplay(version.version.major + '.' + version.version.minor);
    }

    edit() {
        this.editMetadata.emit(this.nodeObject);
    }

    restoreVersion(restore: NodeVersion) {
        this.restore.emit({ version: restore, node: this.nodeObject });
    }

    canRevert() {
        return this.nodeObject && this.nodeObject.access.indexOf(RestConstants.ACCESS_WRITE) != -1;
    }
}
