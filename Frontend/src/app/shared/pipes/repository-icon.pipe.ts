import { Pipe, PipeTransform, inject } from '@angular/core';
import { Repository } from 'ngx-edu-sharing-api';
import { NodeHelperService } from '../../services/node-helper.service';

@Pipe({
    name: 'repositoryIcon',
    standalone: false,
})
export class RepositoryIconPipe implements PipeTransform {
    private nodeHelper = inject(NodeHelperService);

    transform(value: Repository): string {
        return this.nodeHelper.getSourceIconRepoPath(value);
    }
}
