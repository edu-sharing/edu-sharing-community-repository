import { HttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { TranslateLoader, TranslateService } from '@ngx-translate/core';
import { Observer } from 'rxjs';
import 'rxjs/add/observable/concat';
import 'rxjs/add/observable/forkJoin';
import 'rxjs/add/operator/first';
import { Observable } from 'rxjs/Observable';
import 'rxjs/Rx';
import { environment } from '../../environments/environment';
import { BridgeService } from '../core-bridge-module/bridge.service';
import {
    ConfigurationService,
    RestLocatorService,
    SessionStorageService,
} from '../core-module/core.module';
import { TranslationSource } from './translation-source';
import { tap, map, switchMap, delay } from 'rxjs/operators';

export let TRANSLATION_LIST = [
    'common',
    'admin',
    'recycle',
    'workspace',
    'search',
    'collections',
    'login',
    'permissions',
    'oer',
    'messages',
    'register',
    'profiles',
    'services',
    'stream',
    'override',
];

export class Translation {
    /**
     * Initializes ng translate and returns the choosen language
     * @param translate
     */
    static LANGUAGES: any = {
        de: 'de_DE',
        en: 'en_US',
    };
    private static language: string;
    private static languageLoaded = false;
    // none means that only labels should be shown (for dev)
    private static DEFAULT_SUPPORTED_LANGUAGES = ['de', 'en', 'none'];
    private static source = TranslationSource.Auto;

    static initialize(
        translate: TranslateService,
        config: ConfigurationService,
        storage: SessionStorageService,
        route: ActivatedRoute,
    ): Observable<string> {
        const supportedLanguages$: Observable<string[]> = config.get(
            'supportedLanguages',
            Translation.DEFAULT_SUPPORTED_LANGUAGES,
        );
        if (
            config
                .getLocator()
                .getBridge()
                .isRunningCordova()
        ) {
            return supportedLanguages$.switchMap(
                (supportedLanguages: string[]) =>
                    Translation.initializeCordova(
                        translate,
                        config.getLocator().getBridge(),
                        supportedLanguages,
                    ),
            );
        }
        return supportedLanguages$.pipe(
            tap((supportedLanguages: string[]) =>
                translate.addLangs(supportedLanguages),
            ),
            // Select queryParams.locale if set meaningfully
            switchMap((supportedLanguages: string[]) =>
                //
                route.queryParams.first().map(params => {
                    let selectedLanguage: string = null;
                    if (supportedLanguages.indexOf(params.locale) !== -1) {
                        selectedLanguage = params.locale;
                    } else if (params.locale) {
                        console.warn(
                            `Url requested language ${params.locale}, ` +
                                'but it was not found or is not configured in the allowed languages: ' +
                                supportedLanguages,
                        );
                    }
                    return {
                        supportedLanguages,
                        selectedLanguage,
                    };
                }),
            ),
            // Select storage.get('language') if set meaningfully
            switchMap(({ supportedLanguages, selectedLanguage }) => {
                if (selectedLanguage) {
                    return Observable.of({
                        supportedLanguages,
                        selectedLanguage,
                        useStored: false,
                    });
                } else {
                    return storage.get('language').map(storageLanguage => {
                        let useStored = false;
                        if (
                            supportedLanguages.indexOf(storageLanguage) !== -1
                        ) {
                            selectedLanguage = storageLanguage;
                            useStored = true;
                        }
                        return {
                            supportedLanguages,
                            selectedLanguage,
                            useStored,
                        };
                    });
                }
            }),
            map(({ supportedLanguages, selectedLanguage, useStored }) => {
                if (selectedLanguage) {
                    return {
                        supportedLanguages,
                        selectedLanguage,
                        useStored,
                    };
                } else if (
                    // Select browser language if set meaningfully
                    supportedLanguages.indexOf(translate.getBrowserLang()) !==
                    -1
                ) {
                    return {
                        supportedLanguages,
                        selectedLanguage: translate.getBrowserLang(),
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
                if (!useStored) {
                    storage.set('language', selectedLanguage);
                }
                if (selectedLanguage === 'none') {
                    translate.setDefaultLang('none');
                } else {
                    translate.setDefaultLang(supportedLanguages[0]);
                }
            }),
            switchMap(({ supportedLanguages, selectedLanguage, useStored }) =>
                translate.getTranslation(selectedLanguage).map(translation => {
                    return selectedLanguage;
                }),
            ),
            switchMap(selectedLanguage => {
                console.log('language used: ' + selectedLanguage);
                Translation.setLanguage(selectedLanguage);
                return translate
                    .use(selectedLanguage)
                    .map(() => selectedLanguage);
            }),
            tap(selectedLanguage => {
                Translation.languageLoaded = true;
            }),
        );
    }

    static initializeCordova(
        translate: TranslateService,
        bridge: BridgeService,
        supportedLanguages = Translation.DEFAULT_SUPPORTED_LANGUAGES,
    ) {
        return new Observable<string>((observer: Observer<string>) => {
            translate.addLangs(supportedLanguages);
            let language = supportedLanguages[0];
            translate.setDefaultLang(language);
            translate.use(language);
            Translation.setLanguage(language);
            bridge
                .getCordova()
                .getLanguage()
                .subscribe((data: string) => {
                    if (supportedLanguages.indexOf(data) != -1) {
                        language = data;
                    }
                    translate.use(language);
                    Translation.setLanguage(language);
                    translate.getTranslation(language).subscribe(() => {
                        Translation.languageLoaded = true;
                        observer.next(language);
                        observer.complete();
                    });
                });
        });
    }

    static isLanguageLoaded() {
        return Translation.languageLoaded;
    }

    static getLanguage(): string {
        return Translation.language;
    }

    static getISOLanguage(): string {
        return Translation.LANGUAGES[Translation.language];
    }

    static getDateFormat() {
        if (Translation.getLanguage() == 'de') {
            return 'DD.MM.YYYY';
        }
        return 'YYYY/MM/DD';
    }

    /**
     * Set the preferred source for language files
     * Auto: In dev mode, local files are used and in production, repository files are used (default)
     * Repository: Repository files are used
     * Local: Local files (assets/i18n) are used
     */
    static setSource(source: TranslationSource) {
        this.source = source;
    }

    static getSource() {
        return this.source;
    }

    private static setLanguage(language: string) {
        Translation.language = language;
    }
}

export function createTranslateLoader(
    http: HttpClient,
    locator: RestLocatorService,
) {
    return new TranslationLoader(http, locator);
}

export function createTranslateLoaderDummy() {
    return new TranslationLoaderDummy();
}

export class TranslationLoaderDummy implements TranslateLoader {
    constructor() {}

    getTranslation(lang: string): Observable<any> {
        return new Observable<any>((observer: Observer<any>) => {
            observer.next(null);
            observer.complete();
        });
    }
}

export class TranslationLoader implements TranslateLoader {
    private initializing: string = null;
    private initializedLanguage: any;

    constructor(
        private http: HttpClient,
        private locator: RestLocatorService,
        private prefix: string = 'assets/i18n',
        private suffix: string = '.json',
    ) {}

    /**
     * Gets the translations from the server
     * @param lang
     * @returns {any}
     */
    getTranslation(lang: string): Observable<any> {
        if (this.initializing == lang || this.initializedLanguage) {
            return new Observable<any>((observer: Observer<any>) => {
                let callback = () => {
                    if (!this.initializedLanguage) {
                        setTimeout(callback, 10);
                        return;
                    }
                    observer.next(this.initializedLanguage);
                    observer.complete();
                };
                setTimeout(callback);
            });
        }
        this.initializing = lang;
        //return this.http.get(`${this.prefix}/common/${lang}${this.suffix}`)
        //  .map((res: Response) => res.json());
        if (lang == 'none') {
            return new Observable<any>((observer: Observer<any>) => {
                this.initializedLanguage = {};
                this.initializing = null;
                observer.next({});
                observer.complete();
            });
        }
        let translations: any = [];
        let results = 0;
        let maxCount = TRANSLATION_LIST.length;
        if (
            (environment.production &&
                Translation.getSource() == TranslationSource.Auto) ||
            Translation.getSource() == TranslationSource.Repository
        ) {
            maxCount = 1;
            this.locator
                .getLanguageDefaults(Translation.LANGUAGES[lang])
                .subscribe((data: any) => {
                    translations.push(data);
                });
        } else {
            for (let translation of TRANSLATION_LIST) {
                this.http
                    .get(`${this.prefix}/${translation}/${lang}${this.suffix}`)
                    .subscribe((data: any) => translations.push(data));
            }
        }
        return new Observable<any>((observer: Observer<any>) => {
            let callback = () => {
                if (translations.length < maxCount) {
                    setTimeout(callback, 10);
                    return;
                }
                this.locator
                    .getConfigLanguage(Translation.LANGUAGES[lang])
                    .subscribe((data: any) => {
                        translations.push(data);
                        let final: any = {};
                        for (const obj of translations) {
                            for (const key in obj) {
                                //copy all the fields

                                let path = key.split('.');
                                if (path.length == 1) {
                                    final[key] = obj[key];
                                }
                            }
                        }
                        for (const obj of translations) {
                            for (const key in obj) {
                                try {
                                    let path = key.split('.');

                                    // init non-existing objects first
                                    if (path.length >= 2 && !final[path[0]])
                                        final[path[0]] = {};
                                    if (
                                        path.length >= 3 &&
                                        !final[path[0]][path[1]]
                                    )
                                        final[path[0]][path[1]] = {};
                                    if (
                                        path.length >= 4 &&
                                        !final[path[0]][path[1]][path[2]]
                                    )
                                        final[path[0]][path[1]][path[2]] = {};

                                    if (path.length == 1) {
                                        continue;
                                    } else if (path.length == 2) {
                                        final[path[0]][path[1]] = obj[key];
                                    } else if (path.length == 3) {
                                        final[path[0]][path[1]][path[2]] =
                                            obj[key];
                                    } else if (path.length == 4) {
                                        final[path[0]][path[1]][path[2]][
                                            path[3]
                                        ] = obj[key];
                                    }
                                } catch (e) {
                                    console.error(
                                        'error while language override of ' +
                                            key,
                                        e,
                                    );
                                }
                            }
                        }
                        this.initializedLanguage = final;
                        this.initializing = null;
                        observer.next(final);
                        observer.complete();
                    });
            };

            setTimeout(callback, 10);
        });
    }
}
