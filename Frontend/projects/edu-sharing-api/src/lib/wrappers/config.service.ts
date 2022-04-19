import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { distinctUntilChanged, map, shareReplay, startWith, switchMap } from 'rxjs/operators';
import { ApiRequestConfiguration } from '../api-request-configuration';
import * as apiModels from '../api/models';
import { ConfigV1Service } from '../api/services';
import { switchReplay } from '../utils/switch-replay';

export type ClientConfig = apiModels.Values;
export type Variables = apiModels.Variables['current'];
export type TranslationsDict = { [key: string]: string | TranslationsDict };

export type Locale = 'de_DE' | 'en_US';

/**
 * Provides system configuration.
 */
@Injectable({
    providedIn: 'root',
})
export class ConfigService {
    private readonly updateTrigger = new Subject<void>();
    private readonly localeSubject = new Subject<Locale>();

    private readonly config$ = this.updateTrigger.pipe(
        startWith(void 0 as void),
        switchReplay(() => this.configV1.getConfig1()),
        map((config) => config.current ?? null),
    );
    private readonly variables$ = this.updateTrigger.pipe(
        startWith(void 0 as void),
        switchReplay(() => this.configV1.getVariables()),
        map((variables) => variables.current ?? null),
    );
    private readonly defaultTranslations$ = this.localeSubject.pipe(
        distinctUntilChanged(),
        switchMap(() => this.configV1.getLanguageDefaults()),
        shareReplay(1),
    ) as unknown as Observable<TranslationsDict>;
    private readonly translationOverrides$ = this.localeSubject.pipe(
        distinctUntilChanged(),
        switchMap(() => this.configV1.getLanguage()),
        map((language) => language.current ?? null),
        shareReplay(1),
    );

    constructor(
        private configV1: ConfigV1Service,
        private apiRequestConfiguration: ApiRequestConfiguration,
    ) {}

    /**
     * Returns the current system configuration.
     *
     * The configuration might depend on the domain via which the application is served.
     *
     * The observable will update on changes.
     */
    observeConfig({ forceUpdate = false } = {}): Observable<ClientConfig | null> {
        if (forceUpdate) {
            this.updateTrigger.next();
        }
        return this.config$;
    }

    /**
     * Returns variables defined in the client configuration.
     *
     * The observable will update on changes.
     */
    observeVariables(): Observable<Variables | null> {
        return this.variables$;
    }

    /**
     * Sets the locale for future API requests.
     *
     * This affects translations and localized MDS widget values.
     */
    setLocale(locale: Locale): void {
        if (locale) {
            this.apiRequestConfiguration.setLocale(locale);
            this.localeSubject.next(locale);
        } else {
            console.warn('Called `setLocale` with undefined `locale`');
        }
    }

    /**
     * Returns accumulated default translations.
     *
     * The observable will update on locale changes.
     *
     * @returns a nested dictionary of default translations
     */
    observeDefaultTranslations(): Observable<TranslationsDict> {
        return this.defaultTranslations$;
    }

    /**
     * Returns translation overrides.
     *
     * The observable will update on locale changes.
     *
     * @returns a flat dictionary of translation overrides, key parts separated by "."
     */
    observeTranslationOverrides(): Observable<{ [key: string]: string } | null> {
        return this.translationOverrides$;
    }
}
