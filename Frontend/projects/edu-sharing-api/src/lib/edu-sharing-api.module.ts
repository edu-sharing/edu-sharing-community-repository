import { ModuleWithProviders, NgModule } from '@angular/core';
import { ApiConfiguration, ApiConfigurationParams } from './api/api-configuration';

@NgModule({
    declarations: [],
    imports: [],
    exports: [],
})
export class EduSharingApiModule {
    static forRoot(params: ApiConfigurationParams): ModuleWithProviders<EduSharingApiModule> {
        return {
            ngModule: EduSharingApiModule,
            providers: [
                {
                    provide: ApiConfiguration,
                    useValue: params,
                },
            ],
        };
    }
}
