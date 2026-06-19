import { forwardRef, ModuleWithProviders, NgModule, Provider } from '@angular/core';
import {
    getConfigProvider,
    RenderingServiceApiConfigurationParams,
} from './rendering-service-api-configuration';
import { RenderingServiceApiInterceptor } from './rendering-service-api-interceptor';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { RenderingServiceModule } from './api/rendering-service.module';

export const API_INTERCEPTOR_PROVIDER: Provider = {
    provide: HTTP_INTERCEPTORS,
    useExisting: forwardRef(() => RenderingServiceApiInterceptor),
    multi: true,
};

@NgModule({
    declarations: [],
    imports: [RenderingServiceModule.forRoot({})],
    exports: [],
})
export class RenderingServiceApiModule {
    static forRoot(
        params?: RenderingServiceApiConfigurationParams,
    ): ModuleWithProviders<RenderingServiceApiModule> {
        return {
            ngModule: RenderingServiceApiModule,
            providers: [
                RenderingServiceApiInterceptor,
                API_INTERCEPTOR_PROVIDER,
                ...getConfigProvider(params),
            ],
        };
    }
}
