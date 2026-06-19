import { Pipe, PipeTransform, inject } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { RepoUrlService } from '../services/repo-url.service';

@Pipe({
    name: 'esNodeIcon',
    standalone: false,
})
export class NodeIconPipe implements PipeTransform {
    private repoUrlService = inject(RepoUrlService);

    transform(node: Node) {
        return this.repoUrlService.getRepoUrl((node.relations?.Original || node).icon.url, node);
    }
}
