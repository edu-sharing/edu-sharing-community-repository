import { Pipe, PipeTransform } from '@angular/core';
import { NetworkService } from 'ngx-edu-sharing-api';
import { Repository } from '../../core-module/core.module';
import { NodeHelperService } from '../../core-ui-module/node-helper.service';

@Pipe({ name: 'appNodeSource' })
export class NodeSourcePipe implements PipeTransform {
    private homeRepository: Repository;

    constructor(private nodeHelper: NodeHelperService, private networkApi: NetworkService) {
        this.networkApi.getHomeRepository().subscribe((homeRepository) => {
            this.homeRepository = homeRepository;
        });
    }

    transform(
        replicationSource: string,
        args: {
            mode: 'text' | 'url' | 'escaped';
        },
    ): string {
        const rawSrc = replicationSource ? replicationSource.toString().trim() : 'home';
        if (args.mode === 'text') {
            if (rawSrc === 'home') {
                // FIXME: This will fix the pipe's return value to 'home' for calls before
                // `this.homeRepository` was populated (although that doesn't seem to happen).
                return this.homeRepository?.title || 'home';
            }
            return rawSrc;
        } else if (args.mode === 'url') {
            const src = this.escape(rawSrc);
            return this.nodeHelper.getSourceIconPath(src);
        } else if (args.mode === 'escaped') {
            return this.escape(rawSrc);
        }
        return null;
    }

    private escape(src: string) {
        if (!src) {
            return src;
        }
        src = src.substring(src.lastIndexOf(':') + 1).toLowerCase();
        src = src.replace(/\s/g, '_');
        src = src.replace(/\./g, '_');
        src = src.replace(/\//g, '_');
        return src;
    }
}
