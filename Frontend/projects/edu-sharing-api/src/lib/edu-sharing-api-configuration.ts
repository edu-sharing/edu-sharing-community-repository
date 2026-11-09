import { HttpRequest } from '@angular/common/http';
import { Injectable, InjectionToken, Provider } from '@angular/core';
import { ApiConfiguration } from './api/api-configuration';
import { FrontendApiConfiguration } from './frontend-api/frontend-api-configuration';
import { ApiErrorResponse } from './models';

export const EDU_SHARING_API_CONFIG = new InjectionToken<EduSharingApiConfigurationParams>(
    'EDU_SHARING_API_CONFIG',
);

@Injectable({
    providedIn: 'root',
})
export class EduSharingApiConfiguration extends ApiConfiguration {
    withCredentials = true;
    static create(params: EduSharingApiConfigurationParams = {}): EduSharingApiConfiguration {
        return { ...new EduSharingApiConfiguration(), ...params };
    }

    onError?: (error: ApiErrorResponse, req: HttpRequest<unknown>) => void;
}

export type EduSharingApiConfigurationParams = Partial<EduSharingApiConfiguration>;

/**
 * The root url of the frontend server's own api: a sibling of the REST root, e.g.
 * `/edu-sharing/rest` -> `/edu-sharing/frontend-api`, also for an absolute url (web components).
 */
export function getFrontendApiRootUrl(restRootUrl: string): string {
    return restRootUrl.replace(/\/[^/]*\/?$/, '/frontend-api');
}

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
        // the generated client of the frontend server (`frontend-api/`) follows the REST root
        {
            provide: FrontendApiConfiguration,
            deps: [EDU_SHARING_API_CONFIG],
            useFactory: (
                configParams: EduSharingApiConfigurationParams,
            ): FrontendApiConfiguration => ({
                rootUrl: getFrontendApiRootUrl(
                    EduSharingApiConfiguration.create(configParams).rootUrl,
                ),
            }),
        },
    ];
}
