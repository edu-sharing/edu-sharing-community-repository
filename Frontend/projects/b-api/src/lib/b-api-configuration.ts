import { Injectable, InjectionToken, Provider } from '@angular/core';
import { ApiConfiguration } from './api/api-configuration';

export const B_API_CONFIG = new InjectionToken<BApiConfiguration>('B_API_CONFIG');

@Injectable({
    providedIn: 'root',
})
export class BApiConfiguration extends ApiConfiguration {
    token: string = '';
    static create(params: BApiConfigurationParams = {}): BApiConfiguration {
        return { ...new BApiConfiguration(), ...params };
    }
}

export type BApiConfigurationParams = Partial<BApiConfiguration>;

export function getConfigProvider(params?: BApiConfigurationParams): Provider[] {
    return [
        // Provide the params given to `forRoot()`. These can be overridden by the application by
        // providing `EDU_SHARING_API_CONFIG` itself.
        {
            provide: B_API_CONFIG,
            useValue: params,
        },
        {
            provide: ApiConfiguration,
            deps: [B_API_CONFIG],
            useFactory: (configParams: BApiConfigurationParams) =>
                BApiConfiguration.create(configParams),
        },
        {
            provide: BApiConfiguration,
            deps: [B_API_CONFIG],
            useFactory: (configParams: BApiConfigurationParams) =>
                BApiConfiguration.create(configParams),
        },
    ];
}
