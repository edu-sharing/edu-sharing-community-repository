import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { NetworkService } from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Node } from '../../core-module/rest/data-object';
import { RestConstants } from '../../core-module/rest/rest-constants';
import { NodeHelperService } from '../../core-ui-module/node-helper.service';
import { getRepoUrl } from '../../util/repo-url';

interface NodeImagePreferences {
    crop?: boolean;
    maxWidth?: number;
    maxHeight?: number;
    width?: number;
    height?: number;
}

@Pipe({ name: 'esNodeImage' })
export class NodeImagePipe implements PipeTransform {
    constructor(
        private nodeHelper: NodeHelperService,
        private sanitizer: DomSanitizer,
        private networkApi: NetworkService,
    ) {}

    transform(node: Node, preferences: NodeImagePreferences): Observable<SafeResourceUrl> {
        if (this.nodeHelper.isNodeCollection(node) && node.preview.isIcon) {
            return null;
        } else if (node.preview.data) {
            return rxjs.of(
                this.sanitizer.bypassSecurityTrustResourceUrl(
                    'data:' + node.preview.mimetype + ';base64,' + node.preview.data,
                ),
            );
        } else {
            return this.getPreviewUrl(node, preferences);
        }
    }

    private getPreviewUrl(node: Node, preferences: NodeImagePreferences): Observable<string> {
        return this.isEduSharingNode(node).pipe(
            map((isEduSharingNode) => {
                let url = getRepoUrl(node.preview.url, node);
                if (isEduSharingNode) {
                    url += Object.entries(preferences)
                        .map(([key, value]) => `&${key}=${value}`)
                        .join('');
                }
                return url;
            }),
        );
    }

    private isEduSharingNode(node: Node): Observable<boolean> {
        return rxjs
            .forkJoin([
                this.networkApi.isFromHomeRepository(node),
                this.networkApi.getRepositoryOfNode(node),
            ])
            .pipe(
                map(
                    ([isFromHomeRepository, repository]) =>
                        isFromHomeRepository ||
                        repository?.repositoryType === RestConstants.REPOSITORY_TYPE_ALFRESCO,
                ),
            );
    }
}
