import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { Node } from '../../../core-module/rest/data-object';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { RestNetworkService } from '../../../core-module/rest/services/rest-network.service';
import { NodeHelperService } from '../../../core-ui-module/node-helper.service';
import { getRepoUrl } from '../../../util/repo-url';

@Pipe({ name: 'appNodeImage' })
export class NodeImagePipe implements PipeTransform {
    constructor(private nodeHelper: NodeHelperService, private sanitizer: DomSanitizer) {}

    transform(node: Node) {
        if (this.nodeHelper.isNodeCollection(node) && node.preview.isIcon) {
            return null;
        } else if (node.preview.data) {
            return this.sanitizer.bypassSecurityTrustResourceUrl(
                'data:' + node.preview.mimetype + ';base64,' + node.preview.data,
            );
        } else {
            return this.getPreviewUrl(node);
        }
    }

    private getPreviewUrl(node: Node): string {
        let url = getRepoUrl(node.preview.url, node);
        if (this.isEduSharingNode(node)) {
            url += '&crop=true&maxWidth=300&maxHeight=300';
        }
        return url;
    }

    private isEduSharingNode(node: Node): boolean {
        return (
            RestNetworkService.isFromHomeRepo(node) ||
            RestNetworkService.getRepository(node)?.repositoryType ===
                RestConstants.REPOSITORY_TYPE_ALFRESCO
        );
    }
}
