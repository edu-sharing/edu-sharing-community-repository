import { HttpClient } from '@angular/common/http';
import { TranslateLoader } from '@ngx-translate/core';
import { Observable, Observer } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { RestLocatorService } from '../core-module/core.module';
import { Translation } from './translation';
import { TranslationSource } from './translation-source';

export const TRANSLATION_LIST = [
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

    static create(http: HttpClient, locator: RestLocatorService) {
        return new TranslationLoader(http, locator);
    }

    constructor(
        private http: HttpClient,
        private locator: RestLocatorService,
        private prefix: string = 'assets/i18n',
        private suffix: string = '.json',
    ) {}

    /**
     * Gets the translations from the server
     */
    getTranslation(lang: string): Observable<any> {
        if (this.initializing === lang || this.initializedLanguage) {
            return new Observable<any>((observer: Observer<any>) => {
                const callback = () => {
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
        if (lang === 'none') {
            return new Observable<any>((observer: Observer<any>) => {
                this.initializedLanguage = {};
                this.initializing = null;
                observer.next({});
                observer.complete();
            });
        }
        const translations: any = [];
        let maxCount = TRANSLATION_LIST.length;
        if (
            (environment.production &&
                Translation.getSource() === TranslationSource.Auto) ||
            Translation.getSource() === TranslationSource.Repository
        ) {
            maxCount = 1;
            this.locator
                .getLanguageDefaults(Translation.LANGUAGES[lang])
                .subscribe((data: any) => {
                    translations.push(data);
                });
        } else {
            for (const translation of TRANSLATION_LIST) {
                this.http
                    .get(`${this.prefix}/${translation}/${lang}${this.suffix}`)
                    .subscribe((data: any) => translations.push(data));
            }
        }
        return new Observable<any>((observer: Observer<any>) => {
            const callback = () => {
                if (translations.length < maxCount) {
                    setTimeout(callback, 10);
                    return;
                }
                this.locator
                    .getConfigLanguage(Translation.LANGUAGES[lang])
                    .subscribe((data: any) => {
                        translations.push(data);
                        const final: any = {};
                        for (const obj of translations) {
                            for (const key in obj) {
                                if (!obj.hasOwnProperty(key)) {
                                    continue;
                                }
                                // copy all the fields

                                const path = key.split('.');
                                if (path.length === 1) {
                                    final[key] = obj[key];
                                }
                            }
                        }
                        for (const obj of translations) {
                            for (const key in obj) {
                                if (!obj.hasOwnProperty(key)) {
                                    continue;
                                }
                                try {
                                    const path = key.split('.');

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

                                    if (path.length === 1) {
                                        continue;
                                    } else if (path.length === 2) {
                                        final[path[0]][path[1]] = obj[key];
                                    } else if (path.length === 3) {
                                        final[path[0]][path[1]][path[2]] =
                                            obj[key];
                                    } else if (path.length === 4) {
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
