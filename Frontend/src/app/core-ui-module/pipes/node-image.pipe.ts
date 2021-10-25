import {Pipe, PipeTransform} from '@angular/core';
import {RestNetworkService} from '../../core-module/rest/services/rest-network.service';
import {RestConstants} from '../../core-module/rest/rest-constants';
import {NodeHelperService} from '../node-helper.service';
import {Node} from '../../core-module/rest/data-object';
import {DomSanitizer} from '@angular/platform-browser';

@Pipe({name: 'appNodeImage'})
export class NodeImagePipe implements PipeTransform {
    constructor(
        private nodeHelper: NodeHelperService,
        private sanitizer: DomSanitizer,
    ) {
    }
    transform(node: Node, ...args: any[]) {
        if (!this.nodeHelper.isNodeCollection(node) || !node.preview.isIcon) {
            if (node.preview.data) {
                return this.sanitizer.bypassSecurityTrustResourceUrl(
                    'data:' + node.preview.mimetype + ';base64,' + node.preview.data
                );
            } else {
                return node.preview.url + (
                    ((
                            RestNetworkService.isFromHomeRepo(node) ||
                            RestNetworkService.getRepository(node)?.repositoryType === RestConstants.REPOSITORY_TYPE_ALFRESCO
                        )
                    ) ? '&crop=true&maxWidth=300&maxHeight=300' : ''
                );
            }
        }
        return null;
    }
}

