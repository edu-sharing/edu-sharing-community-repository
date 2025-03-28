import { forwardRef, ModuleWithProviders, NgModule, Provider } from '@angular/core';
import { getConfigProvider, BApiConfigurationParams } from './b-api-configuration';
import { ApiModule } from './api/api.module';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { ApiInterceptor } from './api-configuration';

export const API_INTERCEPTOR_PROVIDER: Provider = {
    provide: HTTP_INTERCEPTORS,
    useExisting: forwardRef(() => ApiInterceptor),
    multi: true,
};
@NgModule({
    declarations: [],
    exports: [],
})
export class BApiModule {
    static forRoot(params?: BApiConfigurationParams): ModuleWithProviders<BApiModule> {
        return {
            ngModule: BApiModule,
            providers: [ApiInterceptor, API_INTERCEPTOR_PROVIDER, getConfigProvider(params)],
        };
    }
}
