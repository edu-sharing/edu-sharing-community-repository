import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { distinctUntilChanged, map, shareReplay, startWith, switchMap } from 'rxjs/operators';
import { ApiRequestConfiguration } from '../api-request-configuration';
import * as apiModels from '../api/models';
import { ConfigV1Service } from '../api/services';

export type ClientConfig = apiModels.Values;
export type Variables = apiModels.Variables['current'];
export type TranslationsDict = { [key: string]: string | TranslationsDict };

/**
 * Provides system configuration.
 */
@Injectable({
    providedIn: 'root',
})
export class ConfigService {
    private readonly updateTrigger = new Subject<void>();
    private readonly localeSubject = new Subject<string>();

    private readonly config$ = this.updateTrigger.pipe(
        startWith(void 0 as void),
        switchMap(() => this.configV1.getConfig1()),
        map((config) => config.current ?? null),
        shareReplay(1),
    );
    private readonly variables$ = this.updateTrigger.pipe(
        startWith(void 0 as void),
        switchMap(() => this.configV1.getVariables()),
        map((variables) => variables.current ?? null),
        shareReplay(1),
    );
    private readonly customTranslations$ = this.localeSubject.pipe(
        distinctUntilChanged(),
        switchMap(() => this.configV1.getLanguage()),
        map((language) => language.current ?? null),
        shareReplay(1),
    );
    private readonly defaultTranslations$ = this.localeSubject.pipe(
        distinctUntilChanged(),
        switchMap(() => this.configV1.getLanguageDefaults()),
        shareReplay(1),
    ) as unknown as Observable<TranslationsDict>;

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
    getConfig({ forceUpdate = false } = {}): Observable<ClientConfig | null> {
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
    getVariables(): Observable<Variables | null> {
        return this.variables$;
    }

    /**
     * Returns custom translations for the given locale.
     *
     * Sets the given locale for future API requests.
     *
     * The observable will update on locale changes and might emit translations for other locales.
     * 
     * @param locale - Locale to initially fetch translations for.
     */
    getCustomTranslations(locale: string): Observable<{ [key: string]: string } | null> {
        this.setLocale(locale);
        return this.customTranslations$;
    }

    /**
     * Returns accumulated default translations for the given locale.
     *
     * Sets the given locale for future API requests.
     *
     * The observable will update on locale changes and might emit translations for other locales.
     * 
     * @param locale - Locale to initially fetch translations for.
     */
    getDefaultTranslations(locale: string): Observable<TranslationsDict> {
        this.setLocale(locale);
        return this.defaultTranslations$;
    }

    private setLocale(locale: string): void {
        if (locale) {
            this.apiRequestConfiguration.setLocale(locale);
            this.localeSubject.next(locale);
        } else {
            console.warn('Called translation getter with undefined `locale`');
        }
    }
}
