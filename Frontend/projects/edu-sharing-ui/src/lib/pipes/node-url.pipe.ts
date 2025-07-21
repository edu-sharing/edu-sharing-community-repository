import { Pipe, PipeTransform } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { RepoUrlService } from '../services/repo-url.service';
import { NodeHelperService } from '../services/node-helper.service';

@Pipe({
    name: 'esNodeUrl',
    standalone: false,
})
export class NodeUrlPipe implements PipeTransform {
    constructor(private nodeHelperService: NodeHelperService) {}
    transform(node: Node, mode: 'routerLink' | 'queryParams' | 'plain' = 'plain') {
        return this.nodeHelperService.getNodeLink(mode, node);
    }
}
