import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, of, Subject, throwError } from 'rxjs';
import {
    catchError,
    distinctUntilChanged,
    filter,
    map,
    shareReplay,
    startWith,
    switchMap,
    take,
    tap,
} from 'rxjs/operators';
import { ApiRequestConfiguration } from '../api-request-configuration';
import { RepositoryMessage } from '../api/models/repository-message';
import { ConfigV1Service } from '../api/services';
import { switchReplay } from '../utils/rxjs-operators/switch-replay';
import { Values } from '../api/models/values';
import { Variables as Vars } from '../api/models/variables';
import { ValuesBackend } from '../api/models/values-backend';
import { TranslationsV1Service } from '../frontend-api/services';

export type ClientConfig = Values;
export type ClientConfigBackend = ValuesBackend;
export type Variables = Vars['current'];
export type TranslationsDict = { [key: string]: string | TranslationsDict };

export type Locale = 'de_DE' | 'en_US' | 'fr_FR' | 'it_IT' | 'none';
export const LANGUAGES: { [key: string]: Locale } = {
    none: 'none',
    de: 'de_DE',
    en: 'en_US',
    fr: 'fr_FR',
    it: 'it_IT',
};

/**
 * Provides system configuration.
 */
@Injectable({
    providedIn: 'root',
})
export class ConfigService {
    private configV1 = inject(ConfigV1Service);
    private translationsV1 = inject(TranslationsV1Service);
    private apiRequestConfiguration = inject(ApiRequestConfiguration);

    private readonly updateTrigger = new Subject<void>();
    private readonly updateTriggerMessages = new Subject<void>();
    private readonly localeSubject = new BehaviorSubject<{
        locale: Locale;
        language: string;
    } | null>(null);
    private configSubject = new BehaviorSubject<Values | undefined>(undefined);
    private readonly config$ = this.updateTrigger.pipe(
        startWith(void 0 as void),
        switchReplay(() => this.configV1.getConfig1()),
        tap((config) => this.configSubject.next(config.current)),
        map((config) => (config.current ? config : null)),
    );
    private readonly message$ = this.updateTriggerMessages.pipe(
        startWith(void 0),
        switchMap(() => this.configV1.getSystemMessages()),
        shareReplay({ bufferSize: 1, refCount: true }),
    );
    private readonly variables$ = this.updateTrigger.pipe(
        startWith(void 0 as void),
        switchReplay(() => this.configV1.getVariables()),
        map((variables) => variables.current ?? null),
    );
    /**
     * The config context of the session (`Config.contextId`), also one an admin enforced for it.
     * Passed to the frontend server, which knows no session.
     */
    private readonly contextId$: Observable<string | undefined> = this.config$.pipe(
        take(1),
        map((config) => config?.contextId ?? undefined),
        // without the config the frontend server falls back to the context of the domain
        catchError(() => of(undefined)),
    );
    private readonly defaultTranslations$ = this.localeSubject.pipe(
        filter((locale) => locale !== null),
        distinctUntilChanged(),
        switchMap((locale) =>
            of({
                locale: locale?.locale,
                language: locale?.language,
                // served by the frontend server, the overrides of the client config already applied
                dict: this.withContext((context) =>
                    this.translationsV1.getTranslations({
                        language: locale?.language as string,
                        ...(context ? { context } : {}),
                    }),
                ),
            }),
        ),
        shareReplay(1),
    ) as unknown as Observable<{
        locale: Locale;
        language: string;
        dict: Observable<TranslationsDict>;
    }>;
    private readonly translationOverrides$ = this.localeSubject.pipe(
        filter((locale) => locale !== null),
        distinctUntilChanged(),
        switchMap((locale) =>
            this.withContext((context) =>
                this.translationsV1.getTranslationOverrides({
                    language: locale?.language as string,
                    ...(context ? { context } : {}),
                }),
            ),
        ),
        map((overrides) => overrides ?? null),
        shareReplay(1),
    );

    /**
     * Runs `request` with the context of the session. The frontend server answers an unknown
     * context with 404 (e.g. one created a moment ago that it has not seen yet) - then once more
     * without, so the app still gets the translations of its domain. A warning only: no toast,
     * the frontend api is not handled by the error handler of the REST api anyway.
     */
    private withContext<T>(request: (context?: string) => Observable<T>): Observable<T> {
        return this.contextId$.pipe(
            switchMap((context) =>
                request(context).pipe(
                    catchError((error: unknown) => {
                        if (context && (error as { status?: number })?.status === 404) {
                            console.warn(
                                `translations: context ${context} unknown to the frontend server, using the one of the domain`,
                                error,
                            );
                            return request(undefined);
                        }
                        return throwError(() => error);
                    }),
                ),
            ),
        );
    }

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
        return this.config$.pipe(map((c) => c?.current ?? null));
    }
    /**
     * Returns the current system message (if one should be shown).
     *
     * The message might depend on the domain via which the application is served.
     *
     * The observable will update on changes.
     */
    observeSystemMessages({ forceUpdate = false } = {}): Observable<RepositoryMessage[]> {
        if (forceUpdate) {
            this.updateTriggerMessages.next();
        }
        return this.message$.pipe(filter((message) => message !== undefined));
    }
    /**
     * Returns the current system configuration for the (exposed) backend values.
     *
     * The observable will update on changes.
     */
    observeBackendConfig({ forceUpdate = false } = {}): Observable<ClientConfigBackend | null> {
        if (forceUpdate) {
            this.updateTrigger.next();
        }
        return this.config$.pipe(map((c) => c?.currentBackend ?? null));
    }

    observeEndpointAllowed(endpoint: 'LTI') {
        const EndpointId = {
            LTI: '/lti/v13',
        };
        return this.observeBackendConfig().pipe(
            map((c) =>
                c?.security?.access?.endpoints
                    ? !['disabled', 'admin'].includes(
                          Object.entries(c.security.access.endpoints).find(
                              (e) => e[0] === EndpointId[endpoint],
                          )?.[1] as string,
                      )
                    : true,
            ),
        );
    }

    observeContextId(): Observable<string | null> {
        return this.config$.pipe(map((c) => c?.contextId ?? null));
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
    setLocale(locale: Locale, language: string): void {
        if (locale) {
            this.apiRequestConfiguration.setLocale(locale);
            this.apiRequestConfiguration.setLanguage(language);
            this.localeSubject.next({
                locale: locale,
                language: language,
            });
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
    observeDefaultTranslations(): Observable<{
        locale: Locale;
        language: string;
        dict: Observable<TranslationsDict>;
    }> {
        return this.defaultTranslations$;
    }

    /**
     * Returns translation overrides.
     *
     * Only for clients that load the default translations themselves (dev builds read
     * `assets/i18n` directly) - `observeDefaultTranslations` has them applied already.
     *
     * The observable will update on locale changes.
     *
     * @returns a flat dictionary of translation overrides, key parts separated by "."
     */
    observeTranslationOverrides(): Observable<{ [key: string]: string } | null> {
        return this.translationOverrides$;
    }

    private instantInternal<T>(
        name: string,
        defaultValue?: T,
        object: any = this.configSubject.value,
    ): T {
        if (!object) return defaultValue as T;
        let parts = name.split('.');
        if (parts.length > 1) {
            if (object[parts[0]]) {
                let joined = name.substr(parts[0].length + 1);
                return this.instantInternal(joined, defaultValue, object[parts[0]]);
            } else {
                return defaultValue as T;
            }
        }
        if (object[name] != null) return object[name];
        return defaultValue as T;
    }
    /**
     * @deprecated
     * Like `get`, but assumes that the configuration is already initialized.
     *
     * It is the responsibility of the caller to assure that the configuration is initialized! If you
     * are not sure, use `get` instead.
     */
    public instant<T = string>(name: string, defaultValue?: T): T {
        return this.instantInternal(name, defaultValue);
    }
    public async get<T = string>(name: string, defaultValue?: T): Promise<T> {
        await this.observeConfig().pipe(take(1)).toPromise();
        return this.instantInternal(name, defaultValue);
    }
}
