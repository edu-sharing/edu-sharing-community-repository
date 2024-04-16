import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { forwardRef, ModuleWithProviders, NgModule, Provider } from '@angular/core';
import { ApiRequestConfiguration } from './api-request-configuration';
import { ApiInterceptor } from './api.interceptor';
import {
    EduSharingApiConfigurationParams,
    getConfigProvider,
} from './edu-sharing-api-configuration';
import { TimingInterceptor } from './timing.iterceptor';
import { ApiHelpersService } from './wrappers/api-helpers.service';

// Note that interceptors provided here will also be called on all requests by the app module using
// this library.
//
// One possible solution would appear to be importing `HttpClientModule` in this module, but that
// doesn't seem to have the desired effect: Usually, interceptors are still shared with the app
// module, unless this module is lazy-loaded, in which case it will overwrite (!) the app module's
// interceptors (see https://angular.io/api/common/http/HttpInterceptor#usage-notes).
//
// The only real solutions seems to be to use custom implementations of `HttpClient` and
// `HttpHandler` that use separate interceptors, but this touches private  implementation details of
// Angular's HTTP service.
export const API_INTERCEPTOR_PROVIDER: Provider = {
    provide: HTTP_INTERCEPTORS,
    useExisting: forwardRef(() => ApiInterceptor),
    multi: true,
};

export const TIMING_INTERCEPTOR_PROVIDER: Provider = {
    provide: HTTP_INTERCEPTORS,
    useExisting: forwardRef(() => TimingInterceptor),
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
                ApiHelpersService,
                API_INTERCEPTOR_PROVIDER,
                // ...[TimingInterceptor, TIMING_INTERCEPTOR_PROVIDER],
                getConfigProvider(params),
            ],
        };
    }
}
