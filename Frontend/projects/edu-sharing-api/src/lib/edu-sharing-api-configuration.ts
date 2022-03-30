import { Injectable, InjectionToken, Optional, Provider } from '@angular/core';
import { ApiConfiguration } from './api/api-configuration';

export const EDU_SHARING_API_CONFIG = new InjectionToken<EduSharingApiConfigurationParams>(
    'EDU_SHARING_API_CONFIG',
);

@Injectable({
    providedIn: 'root',
})
export class EduSharingApiConfiguration extends ApiConfiguration {
    static create(params: EduSharingApiConfigurationParams = {}): EduSharingApiConfiguration {
        return { ...new EduSharingApiConfiguration(), ...params };
    }

    onError?: (error: any) => void;
}

export type EduSharingApiConfigurationParams = Partial<EduSharingApiConfiguration>;

export function getConfigProvider(params?: EduSharingApiConfigurationParams): Provider[] {
    return [
        // Provide the params given to `forRoot()`. These can be overridden by the application by
        // providing `EDU_SHARING_API_CONFIG` itself.
        {
            provide: EDU_SHARING_API_CONFIG,
            useValue: params,
        },
        // Inject `configuration` as both, `ApiConfiguration` and `EduSharingApiConfiguration`, to pass
        // `rootUrl` on to `ApiModule` while also adding our custom configuration.
        {
            provide: ApiConfiguration,
            deps: [EDU_SHARING_API_CONFIG],
            // deps: [[new Optional(), EDU_SHARING_API_CONFIG]],
            useFactory: (configParams: EduSharingApiConfigurationParams) =>
                EduSharingApiConfiguration.create(configParams),
        },
        {
            provide: EduSharingApiConfiguration,
            deps: [EDU_SHARING_API_CONFIG],
            // deps: [[new Optional(), EDU_SHARING_API_CONFIG]],
            useFactory: (configParams: EduSharingApiConfigurationParams) =>
                EduSharingApiConfiguration.create(configParams),
        },
    ];
}
