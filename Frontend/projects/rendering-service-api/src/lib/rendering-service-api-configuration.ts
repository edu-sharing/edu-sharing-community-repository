import { forwardRef, Injectable, InjectionToken, Provider } from '@angular/core';
import { ApiConfiguration } from './api/api-configuration';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { RenderingServiceApiInterceptor } from './rendering-service-api-interceptor';

export const RENDERING_SERVICE_API_CONFIG = new InjectionToken<RenderingServiceApiConfiguration>(
    'RENDERING_SERVICE_API_CONFIG',
);

@Injectable({
    providedIn: 'root',
})
export class RenderingServiceApiConfiguration extends ApiConfiguration {
    static create(
        params: RenderingServiceApiConfigurationParams = {},
    ): RenderingServiceApiConfiguration {
        return { ...new RenderingServiceApiConfiguration(), ...params };
    }
}

export type RenderingServiceApiConfigurationParams = Partial<RenderingServiceApiConfiguration>;

export function getConfigProvider(params?: RenderingServiceApiConfigurationParams): Provider[] {
    return [
        // Provide the params given to `forRoot()`. These can be overridden by the application by
        // providing `EDU_SHARING_API_CONFIG` itself.
        {
            provide: RENDERING_SERVICE_API_CONFIG,
            useValue: params,
        },
        {
            provide: ApiConfiguration,
            deps: [RENDERING_SERVICE_API_CONFIG],
            useFactory: (configParams: RenderingServiceApiConfigurationParams) =>
                RenderingServiceApiConfiguration.create(configParams),
        },
    ];
}
