import { ApplicationRef, Inject, Injectable, Injector, Optional } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, from, Observable, of, of as observableOf } from 'rxjs';
import { first, map, switchMap, tap } from 'rxjs/operators';
import { ConfigService, LANGUAGES, SessionStorageService } from 'ngx-edu-sharing-api';
import { AppService } from '../services/abstract/app.service';
import { I18N_CONFIG, I18nConfig } from '../types/injection-tokens';

// 'none' means that only labels should be shown (for dev)
const DEFAULT_SUPPORTED_LANGUAGES = [
    'de',
    'de-informal',
    'de-no-binnen-i',
    'en',
    'fr',
    'it',
    'none',
];

@Injectable({ providedIn: 'root' })
export class TranslationsService {
    private language: string;
    private languageLoaded = new BehaviorSubject(false);

    constructor(
        private config: ConfigService,
        private injector: Injector,
        // optional, some web components may don't have it!
        @Optional() private route: ActivatedRoute,
        @Optional() @Inject(I18N_CONFIG) private i18nConfig: I18nConfig,
        private translate: TranslateService,
        private ref: ApplicationRef,
        @Optional() private appService: AppService,
    ) {
        if (!this.i18nConfig) {
            this.i18nConfig = new I18nConfig();
        }
        if (this.i18nConfig.obeyUserProfile) {
            this.injector
                .get(SessionStorageService)
                ?.observe('language')
                .subscribe((lang) => {
                    // language has changed, i.e. user has different preference
                    if (this.translate.currentLang && this.translate.currentLang !== lang) {
                        this.initialize().subscribe(() => {
                            this.ref.tick();
                        });
                    }
                });
        }
    }

    /**
     * Determines and configures the language to use and triggers loading of translations with
     * ngx-translate.
     *
     * Call this once in the app component.
     */
    initialize(): Observable<void> {
        const supportedLanguages$ = from(
            this.config.get('supportedLanguages', DEFAULT_SUPPORTED_LANGUAGES),
        );
        if (this.appService?.isRunningApp()) {
            return supportedLanguages$.pipe(
                switchMap((supportedLanguages: string[]) =>
                    this.initializeCordova(supportedLanguages),
                ),
                map(() => void 0),
            );
        }
        supportedLanguages$
            .pipe(
                tap((supportedLanguages) => {
                    if (!supportedLanguages.includes('none')) {
                        supportedLanguages.push('none');
                    }
                }),
                tap((supportedLanguages: string[]) => this.translate.addLangs(supportedLanguages)),
                // Select queryParams.locale if set meaningfully
                switchMap((supportedLanguages: string[]) =>
                    (this.route ? this.route.queryParams : of(null)).pipe(
                        first(),
                        map((params) => {
                            let selectedLanguage: string = null;
                            if (supportedLanguages.indexOf(params?.locale) !== -1) {
                                selectedLanguage = params?.locale;
                            } else if (params?.locale) {
                                if (params?.locale === 'de') {
                                    const deVariants = supportedLanguages.filter((l) =>
                                        l.startsWith('de-'),
                                    );
                                    if (deVariants?.length === 1) {
                                        selectedLanguage = deVariants[0];
                                    } else {
                                        console.warn(
                                            `Url requested language ${params.locale}, ` +
                                                'but ambiguous variants are present: ' +
                                                supportedLanguages,
                                        );
                                    }
                                } else {
                                    console.warn(
                                        `Url requested language ${params.locale}, ` +
                                            'but it was not found or is not configured in the allowed languages: ' +
                                            supportedLanguages,
                                    );
                                }
                            }
                            return {
                                supportedLanguages,
                                selectedLanguage,
                            };
                        }),
                    ),
                ),
                // Select storage.get('language') if set meaningfully
                switchMap(({ supportedLanguages, selectedLanguage }) => {
                    if (selectedLanguage) {
                        return observableOf({
                            supportedLanguages,
                            selectedLanguage,
                            useStored: false,
                        });
                    } else {
                        if (!this.i18nConfig.obeyUserProfile) {
                            console.info(
                                'obeyUserProfile is disabled, using default language ' +
                                    supportedLanguages[0],
                            );
                            return of({
                                supportedLanguages,
                                selectedLanguage,
                                useStored: false,
                            });
                        }
                        return this.injector
                            .get(SessionStorageService)
                            .get('language')
                            .pipe(
                                map((storageLanguage) => {
                                    let useStored = false;
                                    if (supportedLanguages.indexOf(storageLanguage) !== -1) {
                                        selectedLanguage = storageLanguage;
                                        useStored = true;
                                    }
                                    return {
                                        supportedLanguages,
                                        selectedLanguage,
                                        useStored,
                                    };
                                }),
                            );
                    }
                }),
                // Use browser language if available, otherwise fall back to the first supported
                // language.
                map(({ supportedLanguages, selectedLanguage, useStored }) => {
                    if (selectedLanguage) {
                        return {
                            supportedLanguages,
                            selectedLanguage,
                            useStored,
                        };
                    } else if (
                        // Select browser language if set meaningfully
                        supportedLanguages.indexOf(this.translate.getBrowserLang()) !== -1
                    ) {
                        return {
                            supportedLanguages,
                            selectedLanguage: this.translate.getBrowserLang(),
                            useStored,
                        };
                    } else {
                        // Select first supported language
                        return {
                            supportedLanguages,
                            selectedLanguage: supportedLanguages[0],
                            useStored,
                        };
                    }
                }),
                // Set fallback language
                tap(({ supportedLanguages, selectedLanguage, useStored }) => {
                    if (!useStored && this.i18nConfig.obeyUserProfile) {
                        void this.injector
                            .get(SessionStorageService)
                            .set('language', selectedLanguage);
                    }
                    if (selectedLanguage === 'none') {
                        this.translate.setDefaultLang('none');
                    } else if (selectedLanguage?.startsWith('de-')) {
                        this.translate.setDefaultLang('de');
                    } else {
                        this.translate.setDefaultLang(supportedLanguages[0]);
                    }
                }),
                // Configure `ngx-translate` to use the determined language and trigger loading of
                // translations.
                switchMap(({ selectedLanguage }) => {
                    // console.log('language used: ' + selectedLanguage);
                    this.language = selectedLanguage;
                    return this.translate.use(selectedLanguage).pipe(map(() => void 0));
                }),
            )
            .subscribe(() => {
                // Notify anyone waiting for translations to be loaded.
                this.languageLoaded.next(true);
            });
        return this.waitForInit();
    }

    private async initializeCordova(supportedLanguages = DEFAULT_SUPPORTED_LANGUAGES) {
        this.translate.addLangs(supportedLanguages);
        let language = supportedLanguages[0];
        this.translate.setDefaultLang(language);
        this.translate.use(language);
        this.language = language;
        const data = await this.appService.getLanguage();
        if (supportedLanguages.indexOf(data) != -1) {
            language = data;
        }
        this.language = language;
        this.translate.use(language).subscribe(() => {
            this.languageLoaded.next(true);
        });
        // this.translate.getTranslation(language).subscribe(() => {
        // });
        return this.waitForInit();
    }

    waitForInit(): Observable<void> {
        return this.languageLoaded.pipe(
            first((languageLoaded) => languageLoaded),
            map(() => void 0),
        );
    }

    /** Same as `translate.currentLang`. */
    getLanguage(): string {
        return this.language;
    }

    getISOLanguage(): string {
        return LANGUAGES[this.language];
    }

    getLocale(): string {
        return this.getISOLanguage().replace('_', '-');
    }
}
