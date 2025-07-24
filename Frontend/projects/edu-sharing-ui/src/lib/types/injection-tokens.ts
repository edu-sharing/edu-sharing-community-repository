import { InjectionToken } from '@angular/core';

export class I18nConfig {
    /**
     * shall the user profile be read
     * If not set, defaults to true
     */
    obeyUserProfile?: boolean = true;

    /**
     * custom additional uris to fetch language files from
     *
     * Might be used if additional angular modules require language data
     *
     * Example
     * {provide: I18N_CONFIG, useValue: { ... }},
     */
    additionalI18nProvider?: (lang: string) => string[];
}

/**
 * additional configuration options
 */
export const I18N_CONFIG = new InjectionToken<I18nConfig>('I18N_CONFIG');
