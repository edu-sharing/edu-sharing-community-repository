import { Pipe, PipeTransform } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { RepoUrlService } from '../services/repo-url.service';

@Pipe({ name: 'esNodeIcon' })
export class NodeIconPipe implements PipeTransform {
    constructor(private repoUrlService: RepoUrlService) {}
    transform(node: Node) {
        return this.repoUrlService.getRepoUrl(node.iconURL, node);
    }
}
