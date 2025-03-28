import { forwardRef, ModuleWithProviders, NgModule, Provider } from '@angular/core';
import { getConfigProvider, BApiConfigurationParams } from './b-api-configuration';
import { ApiModule } from './api/api.module';
import { HTTP_INTERCEPTORS } from '@angular/common/http';

@NgModule({
    declarations: [],
    exports: [],
})
export class BApiModule {
    static forRoot(params?: BApiConfigurationParams): ModuleWithProviders<BApiModule> {
        return {
            ngModule: BApiModule,
            providers: [getConfigProvider(params)],
        };
    }
}
