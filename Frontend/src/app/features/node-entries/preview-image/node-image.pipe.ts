import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { environment } from '../../../../environments/environment';
import { Node } from '../../../core-module/rest/data-object';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { RestNetworkService } from '../../../core-module/rest/services/rest-network.service';
import { NodeHelperService } from '../../../core-ui-module/node-helper.service';

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
        let url = node.preview.url;
        // Preview URLs are absolute URLs to the backend server, however, in the local dev
        // environment, we won't have a valid session cookie for the backend since our session runs
        // against localhost. We access preview images via the proxy in this case, so we can present
        // the valid session cookie.
        if (
            !environment.production &&
            window.location.hostname === 'localhost' &&
            RestNetworkService.isFromHomeRepo(node)
        ) {
            url = this.withCurrentOrigin(url);
        }
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

    private withCurrentOrigin(url: string): string {
        const urlObject = new URL(url);
        urlObject.host = window.location.host;
        urlObject.protocol = window.location.protocol;
        return urlObject.href;
    }
}
