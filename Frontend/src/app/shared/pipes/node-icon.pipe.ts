import { Pipe, PipeTransform } from '@angular/core';
import { Node } from '../../core-module/rest/data-object';
import { getRepoUrl } from '../../util/repo-url';

@Pipe({ name: 'esNodeIcon' })
export class NodeIconPipe implements PipeTransform {
    transform(node: Node) {
        return getRepoUrl(node.iconURL, node);
    }
}
