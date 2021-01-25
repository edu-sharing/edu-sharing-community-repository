import { ActivatedRoute } from '@angular/router';
import { TranslateService, MissingTranslationHandler, MissingTranslationHandlerParams } from '@ngx-translate/core';
import { BehaviorSubject, Observer, ReplaySubject } from 'rxjs';
import 'rxjs/add/observable/concat';
import 'rxjs/add/observable/forkJoin';
import 'rxjs/add/operator/first';
import { Observable } from 'rxjs/Observable';
import { first, map, switchMap, tap } from 'rxjs/operators';
import 'rxjs/Rx';
import { BridgeService } from '../core-bridge-module/bridge.service';
import {
    ConfigurationService,
    SessionStorageService,
} from '../core-module/core.module';
import { TranslationSource } from './translation-source';

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
    private static languageLoaded = new BehaviorSubject(false);
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
                // console.log('language used: ' + selectedLanguage);
                Translation.setLanguage(selectedLanguage);
                return translate
                    .use(selectedLanguage)
                    .map(() => selectedLanguage);
            }),
            tap(selectedLanguage => {
                Translation.languageLoaded.next(true);
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
                        Translation.languageLoaded.next(true);
                        observer.next(language);
                        observer.complete();
                    });
                });
        });
    }

    static isLanguageLoaded() {
        return Translation.languageLoaded.value;
    }

    static waitForInit() {
        return Translation.languageLoaded.pipe(first(languageLoaded => languageLoaded));
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

export class FallbackTranslationHandler implements MissingTranslationHandler {
    handle(params: MissingTranslationHandlerParams) {
        return (params.interpolateParams as any)?.fallback || params.key;
    }
}