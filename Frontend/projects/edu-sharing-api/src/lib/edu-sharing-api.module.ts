import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { forwardRef, ModuleWithProviders, NgModule, Provider } from '@angular/core';
import { ApiRequestConfiguration } from './api-request-configuration';
import { ApiInterceptor } from './api.interceptor';
import { ApiConfiguration, ApiConfigurationParams } from './api/api-configuration';

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
    static forRoot(params?: ApiConfigurationParams): ModuleWithProviders<EduSharingApiModule> {
        const providers: Provider[] = [
            ApiRequestConfiguration,
            ApiInterceptor,
            API_INTERCEPTOR_PROVIDER,
        ];
        if (params) {
            providers.push({
                provide: ApiConfiguration,
                useValue: params,
            });
        }
        return {
            ngModule: EduSharingApiModule,
            providers,
        };
    }
}
