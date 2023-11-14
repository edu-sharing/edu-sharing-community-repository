import { Pipe, PipeTransform } from '@angular/core';
import { Repository } from '../../core-module/core.module';
import { NodeHelperService } from '../../services/node-helper.service';

@Pipe({
    name: 'repositoryIcon',
})
export class RepositoryIconPipe implements PipeTransform {
    constructor(private nodeHelper: NodeHelperService) {}

    transform(value: Repository): string {
        return this.nodeHelper.getSourceIconRepoPath(value);
    }
}
