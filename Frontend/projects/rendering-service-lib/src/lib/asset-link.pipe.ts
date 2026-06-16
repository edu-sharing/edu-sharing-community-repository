import { Injector, Optional, Pipe, PipeTransform } from '@angular/core';
import { RSApiConfiguration } from 'ngx-rendering-service-api';
import { CurrentRenderRootUrlService } from './current-render-root-url.service';

/**
 * this pipe transforms any reported asset data uri from the rendering to the uri that is currently configured for the backend
 * this is required in case the rendering service is behind a proxy, i.e. in dev mode
 */
@Pipe({
    name: 'rsAssetLink',
    standalone: false,
})
export class AssetLinkPipe implements PipeTransform {
    constructor(
        private injector: Injector,
        @Optional() private current: CurrentRenderRootUrlService | null,
    ) {}

    transform(value: string | undefined) {
        if (value) {
            const str = value.split('/public');
            str[0] = this.current?.rootUrl ?? this.injector.get(RSApiConfiguration).rootUrl;
            return str.join('/public');
        }
        return value;
    }
}
