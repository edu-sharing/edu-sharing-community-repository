import { Inject, Optional, Pipe, PipeTransform } from '@angular/core';
import { ASSETS_BASE_PATH } from 'ngx-edu-sharing-ui';

/**
 * Prefixes a path to an asset with the assets base path, if configured.
 *
 * All references to assets have to use this pipe.
 */
@Pipe({
    name: 'esAssetsPath',
})
export class AssetsPathPipe implements PipeTransform {
    constructor(@Optional() @Inject(ASSETS_BASE_PATH) private assetsBasePath: string) {}

    transform(path: string): string {
        if (this.assetsBasePath && path.startsWith('assets/')) {
            return this.assetsBasePath + path;
        } else {
            return path;
        }
    }
}
