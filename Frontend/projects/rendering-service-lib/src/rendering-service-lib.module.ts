import { ModuleWithProviders, NgModule } from '@angular/core';
import { RenderComponent } from './lib/render.component';
import { CommonModule } from '@angular/common';
import {
    getConfigProvider,
    RenderingServiceLibConfigurationParams,
} from './rendering-service-lib-configuration';
import { RevokedComponent } from './lib/generic/revoked/revoked.component';

@NgModule({
    declarations: [],
    imports: [RenderComponent, RevokedComponent, CommonModule],
    exports: [CommonModule, RevokedComponent, RenderComponent],
})
export class RenderingServiceLibModule {
    public static forRoot(
        params: RenderingServiceLibConfigurationParams,
    ): ModuleWithProviders<RenderingServiceLibModule> {
        return {
            ngModule: RenderingServiceLibModule,
            providers: [getConfigProvider(params)],
        };
    }
}
