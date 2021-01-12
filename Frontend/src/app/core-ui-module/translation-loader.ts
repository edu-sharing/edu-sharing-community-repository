import { HttpClient } from '@angular/common/http';
import { TranslateLoader } from '@ngx-translate/core';
import {Observable, Observer, of} from 'rxjs';
import {tap, switchMap, map, catchError} from 'rxjs/operators';
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

type Dictionary = { [key: string]: string | Dictionary };

export class TranslationLoader implements TranslateLoader {
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
    getTranslation(lang: string): Observable<Dictionary> {
        if (lang === 'none') {
            return Observable.of({});
        }
        return this.getOriginalTranslations(lang).pipe(
            // Default to empty dictionary if we got nothing
            map(translations => translations || {}),
            switchMap(translations =>
                this.fetchAndApplyOverrides(translations, lang).pipe(
                    catchError((error, obs) => {
                        console.error(error);
                        return of(error);
                    })
                )
            ),
        );
    }

    private getOriginalTranslations(lang: string): Observable<Dictionary> {
        switch (this.getSource()) {
            case 'repository':
                return this.locator.getLanguageDefaults(
                    Translation.LANGUAGES[lang],
                );
            case 'local':
                return this.mergeTranslations(this.fetchTranslations(lang));
        }
    }

    private getSource(): 'repository' | 'local' {
        if (
            (environment.production &&
                Translation.getSource() === TranslationSource.Auto) ||
            Translation.getSource() === TranslationSource.Repository
        ) {
            return 'repository';
        } else {
            return 'local';
        }
    }

    /**
     * Returns an array of Observables that will each fetch a translations json
     * file.
     */
    private fetchTranslations(lang: string): Observable<Dictionary>[] {
        return TRANSLATION_LIST.map(
            translation =>
                `${this.prefix}/${translation}/${lang}${this.suffix}`,
        ).map(url => this.http.get(url) as Observable<Dictionary>);
    }

    /**
     * Takes an array as returned by `fetchTranslations` and converts it to an
     * Observable that yields a single Dictionary object.
     */
    private mergeTranslations(
        translations: Observable<Dictionary>[],
    ): Observable<Dictionary> {
        return Observable.concat(...translations).reduce(
            (acc: Dictionary, value: Dictionary) => {
                for (const prop in value) {
                    if (value.hasOwnProperty(prop)) {
                        acc[prop] = value[prop];
                    }
                }
                return acc;
            },
            {},
        );
    }

    private fetchAndApplyOverrides(
        translations: Dictionary,
        lang: string,
    ): Observable<Dictionary> {
        return this.locator
            .getConfigLanguage(Translation.LANGUAGES[lang])
            .map(overrides => this.applyOverrides(translations, overrides));
    }

    /**
     * Applies `overrides` to `translations` and returns `translations`.
     *
     * Example:
     *  translations = { foo: { bar: 'bar' } }
     *  overrides = { 'foo.bar': 'baz' }
     * results in
     *  translations = { foo: {bar: 'baz' } }
     *
     * @param translations Nested translations object.
     * @param overrides Flat object with dots (.) in keys interpreted as
     * separators.
     */
    private applyOverrides(
        translations: Dictionary,
        overrides: { [key: string]: string },
    ): Dictionary {
        if (overrides) {
            for (const [key, value] of Object.entries<string>(overrides)) {
                let ref = translations;
                const path = key.split('.');
                const pathLast = path.pop();
                for (const item of path) {
                    if (!ref[item]) {
                        ref[item] = {};
                    }
                    const refItem = ref[item];
                    if (typeof refItem === 'string') {
                        throw new Error(
                            'Trying to override leave with sub tree: ' + path,
                        );
                    }
                    ref = refItem;
                }
                ref[pathLast] = value;
            }
        }
        return translations;
    }
}
