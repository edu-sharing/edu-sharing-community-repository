import { Pipe, PipeTransform } from '@angular/core';
import { EduSharingUiConfiguration } from 'ngx-edu-sharing-ui';

/**
 * Prefixes a path to an asset with the assets base path, if configured.
 *
 * All references to assets have to use this pipe.
 */
@Pipe({
    name: 'esAssetsPath',
    standalone: false,
})
export class AssetsPathPipe implements PipeTransform {
    constructor(private configuration: EduSharingUiConfiguration) {}

    transform(path: string): string {
        if (this.configuration.assetsBasePath && path.startsWith('assets/')) {
            return this.configuration.assetsBasePath + path;
        } else {
            return path;
        }
    }
}
