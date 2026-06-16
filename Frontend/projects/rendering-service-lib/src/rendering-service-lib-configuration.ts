import { Injectable, InjectionToken, Provider } from '@angular/core';

export const RENDERING_SERVICE_LIB_CONFIG = new InjectionToken<RenderingServiceLibConfiguration>(
    'RENDERING_SERVICE_LIB_CONFIG',
);
@Injectable({
    providedIn: 'root',
})
export class RenderingServiceLibConfiguration {
    /**
     * the assets url
     * this is important for modules that require to fetch external javascript, i.e. ng2-pdfjs-viewer
     */
    assetsUrl: string | null = null;
    static create(
        params: RenderingServiceLibConfigurationParams = {},
    ): RenderingServiceLibConfiguration {
        return { ...new RenderingServiceLibConfiguration(), ...params };
    }
}

export type RenderingServiceLibConfigurationParams = Partial<RenderingServiceLibConfiguration>;

export function getConfigProvider(params?: RenderingServiceLibConfigurationParams): Provider[] {
    return [
        // Provide the params given to `forRoot()`. These can be overridden by the application by
        // providing `EDU_SHARING_API_CONFIG` itself.
        {
            provide: RENDERING_SERVICE_LIB_CONFIG,
            useValue: params,
        },
    ];
}
