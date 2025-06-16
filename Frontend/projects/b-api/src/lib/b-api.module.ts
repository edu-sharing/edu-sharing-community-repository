import { ModuleWithProviders, NgModule } from '@angular/core';
import { BApiConfigurationParams, getConfigProvider } from './b-api-configuration';

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
