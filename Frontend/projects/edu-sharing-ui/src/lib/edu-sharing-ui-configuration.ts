import { Injectable, InjectionToken, Provider } from '@angular/core';
import { Observable } from 'rxjs';
import { AccessibilitySettings } from './types/accessibillity';
import { KeyboardShortcut } from './types/keyboard-shortcuts';

export const EDU_SHARING_UI_CONFIG = new InjectionToken<EduSharingUiConfigurationParams>(
    'EDU_SHARING_API_CONFIG',
);

@Injectable({
    providedIn: 'root',
})
export class EduSharingUiConfiguration {
    production: boolean;
    static create(params: EduSharingUiConfigurationParams = {}): EduSharingUiConfiguration {
        return { ...new EduSharingUiConfiguration(), ...params };
    }
}

export type EduSharingUiConfigurationParams = Partial<EduSharingUiConfiguration>;

export function getConfigProvider(params?: EduSharingUiConfigurationParams): Provider[] {
    return [
        // Provide the params given to `forRoot()`. These can be overridden by the application by
        // providing `EDU_SHARING_API_CONFIG` itself.
        {
            provide: EDU_SHARING_UI_CONFIG,
            useValue: params,
        },
        // Inject `configuration` as both, `ApiConfiguration` and `EduSharingApiConfiguration`, to pass
        // `rootUrl` on to `ApiModule` while also adding our custom configuration.
        {
            provide: EduSharingUiConfiguration,
            deps: [EDU_SHARING_UI_CONFIG],
            // deps: [[new Optional(), EDU_SHARING_API_CONFIG]],
            useFactory: (configParams: EduSharingUiConfigurationParams) =>
                EduSharingUiConfiguration.create(configParams),
        },
        {
            provide: EduSharingUiConfiguration,
            deps: [EDU_SHARING_UI_CONFIG],
            // deps: [[new Optional(), EDU_SHARING_API_CONFIG]],
            useFactory: (configParams: EduSharingUiConfigurationParams) =>
                EduSharingUiConfiguration.create(configParams),
        },
    ];
}
