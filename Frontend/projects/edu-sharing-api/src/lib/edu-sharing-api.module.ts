import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { forwardRef, ModuleWithProviders, NgModule, Provider } from '@angular/core';
import { ApiRequestConfiguration } from './api-request-configuration';
import { ApiInterceptor } from './api.interceptor';
import {
    EduSharingApiConfigurationParams,
    getConfigProvider,
} from './edu-sharing-api-configuration';

export const API_INTERCEPTOR_PROVIDER: Provider = {
    provide: HTTP_INTERCEPTORS,
    useExisting: forwardRef(() => ApiInterceptor),
    multi: true,
};

@NgModule({
    declarations: [],
    imports: [],
    exports: [],
})
export class EduSharingApiModule {
    /**
     * Gets the edu-sharing-api module to include in your application module.
     *
     * Either pass configuration parameters directly to this method, or provide
     * `EDU_SHARING_API_CONFIG`, which allows you to use dependency injection.
     */
    static forRoot(
        params?: EduSharingApiConfigurationParams,
    ): ModuleWithProviders<EduSharingApiModule> {
        return {
            ngModule: EduSharingApiModule,
            providers: [
                ApiRequestConfiguration,
                ApiInterceptor,
                API_INTERCEPTOR_PROVIDER,
                getConfigProvider(params),
            ],
        };
    }
}
