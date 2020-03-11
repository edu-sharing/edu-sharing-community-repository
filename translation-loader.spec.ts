import { HttpClient } from '@angular/common/http';
import { fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';
import { RestLocatorService } from '../core-module/core.module';
import { TranslationLoader, TRANSLATION_LIST } from './translation-loader';
import { Translation } from './translation';
import { TranslationSource } from './translation-source';

class HttpClientStub {
    get(url: string): Observable<any> {
        return Observable.of(null);
    }
}

class RestLocatorStub {
    getConfigLanguage(lang: string): Observable<any> {
        return Observable.of(null);
    }
    getLanguageDefaults(lang: string): Observable<any> {
        return Observable.of(null);
    }
}

describe('TranslationLoader', () => {
    let translationLoader: TranslationLoader;
    let httpClient: HttpClientStub;
    let locator: RestLocatorStub;

    beforeEach(() => {
        httpClient = new HttpClientStub();
        locator = new RestLocatorStub();
        translationLoader = new TranslationLoader(
            httpClient as HttpClient,
            locator as RestLocatorService,
        );
    });

    describe('getTranslation', () => {
        function callGetTranslation(lang: string) {
            return new Promise(resolve => {
                fakeAsync(() => {
                    translationLoader
                        .getTranslation(lang)
                        .subscribe(result => resolve(result), fail);
                    tick(100);
                })();
            });
        }

        it('should run', async () => {
            const result = await callGetTranslation('de');
            expect(result).toEqual({});
        });

        describe('Source = Local', () => {
            beforeEach(() => {
                Translation.setSource(TranslationSource.Local);
            });

            it('should call nothing for lang=none', async () => {
                const getSpy = spyOn(httpClient, 'get').and.callThrough();
                const getConfigLanguageSpy = spyOn(
                    locator,
                    'getConfigLanguage',
                ).and.callThrough();
                const getLanguageDefaultsSpy = spyOn(
                    locator,
                    'getLanguageDefaults',
                ).and.callThrough();
                await callGetTranslation('none');
                expect(getSpy.calls.count()).toBe(0);
                expect(getConfigLanguageSpy.calls.count()).toBe(0);
                expect(getLanguageDefaultsSpy.calls.count()).toBe(0);
            });

            it('should call http get', async () => {
                const getSpy = spyOn(httpClient, 'get').and.callThrough();
                await callGetTranslation('de');
                expect(getSpy.calls.count()).toBe(TRANSLATION_LIST.length);
                expect(getSpy.calls.first().args).toEqual([
                    'assets/i18n/common/de.json',
                ]);
                expect(getSpy.calls.mostRecent().args).toEqual([
                    'assets/i18n/override/de.json',
                ]);
            });

            it('should call getConfigLanguage', async () => {
                const getConfigLanguageSpy = spyOn(
                    locator,
                    'getConfigLanguage',
                ).and.callThrough();
                await callGetTranslation('de');
                expect(getConfigLanguageSpy.calls.count()).toBe(1);
            });

            it('should not call getLanguageDefaults', async () => {
                const getLanguageDefaultsSpy = spyOn(
                    locator,
                    'getLanguageDefaults',
                ).and.callThrough();
                await callGetTranslation('de');
                expect(getLanguageDefaultsSpy.calls.count()).toBe(0);
            });

            it('should include translations via http', async () => {
                httpClient.get = url => {
                    if (url === 'assets/i18n/common/de.json') {
                        return Observable.of({ foo: 'bar' });
                    }
                    return Observable.of(null);
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'bar' });
            });

            it('should merge translations via http', async () => {
                httpClient.get = url => {
                    if (url === 'assets/i18n/common/de.json') {
                        return Observable.of({ foo: 'bar' });
                    } else if (url === 'assets/i18n/admin/de.json') {
                        return Observable.of({ bar: 'baz' });
                    }
                    return Observable.of(null);
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'bar', bar: 'baz' });
            });

            it('should override nested translations via http', async () => {
                httpClient.get = url => {
                    if (url === 'assets/i18n/common/de.json') {
                        return Observable.of({ prefix: { foo: 'bar' } });
                    } else if (url === 'assets/i18n/admin/de.json') {
                        return Observable.of({ prefix: { bar: 'baz' } });
                    }
                    return Observable.of(null);
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ prefix: { bar: 'baz' } });
            });

            it('should include translations via getConfigLanguage', async () => {
                locator.getConfigLanguage = lang => {
                    return Observable.of({ foo: 'bar' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'bar' });
            });

            it('should override translations via getConfigLanguage', async () => {
                httpClient.get = url => {
                    if (url === 'assets/i18n/common/de.json') {
                        return Observable.of({ foo: 'bar' });
                    }
                    return Observable.of(null);
                };
                locator.getConfigLanguage = lang => {
                    return Observable.of({ foo: 'baz' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'baz' });
            });

            it('should merge translations via getConfigLanguage', async () => {
                httpClient.get = url => {
                    if (url === 'assets/i18n/common/de.json') {
                        return Observable.of({ foo: 'bar' });
                    }
                    return Observable.of(null);
                };
                locator.getConfigLanguage = lang => {
                    return Observable.of({ bar: 'baz' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'bar', bar: 'baz' });
            });

            it('should override nested translations via getConfigLanguage', async () => {
                httpClient.get = url => {
                    if (url === 'assets/i18n/common/de.json') {
                        return Observable.of({ prefix: { foo: 'bar' } });
                    }
                    return Observable.of(null);
                };
                locator.getConfigLanguage = lang => {
                    return Observable.of({ prefix: { bar: 'baz' } });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ prefix: { bar: 'baz' } });
            });

            it('should deep-merge translations via getConfigLanguage', async () => {
                httpClient.get = url => {
                    if (url === 'assets/i18n/common/de.json') {
                        return Observable.of({ prefix: { foo: 'bar' } });
                    }
                    return Observable.of(null);
                };
                locator.getConfigLanguage = lang => {
                    return Observable.of({ 'prefix.bar': 'baz' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ prefix: { foo: 'bar', bar: 'baz' } });
            });

            it('should deep-merge translations via getConfigLanguage (3 levels)', async () => {
                httpClient.get = url => {
                    if (url === 'assets/i18n/common/de.json') {
                        return Observable.of({
                            l1: { l2: { l3: { foo: 'bar' } } },
                        });
                    }
                    return Observable.of(null);
                };
                locator.getConfigLanguage = lang => {
                    return Observable.of({ 'l1.l2.l3.bar': 'baz' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({
                    l1: { l2: { l3: { foo: 'bar', bar: 'baz' } } },
                });
            });

            it('should create missing levels via getConfigLanguage', async () => {
                httpClient.get = url => {
                    if (url === 'assets/i18n/common/de.json') {
                        return Observable.of({});
                    }
                    return Observable.of(null);
                };
                locator.getConfigLanguage = lang => {
                    return Observable.of({ 'l1.l2.l3.bar': 'baz' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({
                    l1: { l2: { l3: { bar: 'baz' } } },
                });
            });
        });

        describe('Source = Repository', () => {
            beforeEach(() => {
                Translation.setSource(TranslationSource.Repository);
            });

            it('should call nothing for lang=none', async () => {
                const getSpy = spyOn(httpClient, 'get').and.callThrough();
                const getConfigLanguageSpy = spyOn(
                    locator,
                    'getConfigLanguage',
                ).and.callThrough();
                const getLanguageDefaultsSpy = spyOn(
                    locator,
                    'getLanguageDefaults',
                ).and.callThrough();
                await callGetTranslation('none');
                expect(getSpy.calls.count()).toBe(0);
                expect(getConfigLanguageSpy.calls.count()).toBe(0);
                expect(getLanguageDefaultsSpy.calls.count()).toBe(0);
            });

            it('should not call http get', async () => {
                const getSpy = spyOn(httpClient, 'get').and.callThrough();
                await callGetTranslation('de');
                expect(getSpy.calls.count()).toBe(0);
            });

            it('should call getConfigLanguage', async () => {
                const getConfigLanguageSpy = spyOn(
                    locator,
                    'getConfigLanguage',
                ).and.callThrough();
                await callGetTranslation('de');
                expect(getConfigLanguageSpy.calls.count()).toBe(1);
            });

            it('should call getLanguageDefaults', async () => {
                const getLanguageDefaultsSpy = spyOn(
                    locator,
                    'getLanguageDefaults',
                ).and.callThrough();
                await callGetTranslation('de');
                expect(getLanguageDefaultsSpy.calls.count()).toBe(1);
            });

            it('should include translations via getLanguageDefaults', async () => {
                locator.getLanguageDefaults = lang => {
                    return Observable.of({ foo: 'bar' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'bar' });
            });

            it('should include translations via getConfigLanguage', async () => {
                locator.getConfigLanguage = lang => {
                    return Observable.of({ foo: 'bar' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'bar' });
            });

            it('should merge translations via getConfigLanguage', async () => {
                locator.getLanguageDefaults = lang => {
                    return Observable.of({ foo: 'bar' });
                };
                locator.getConfigLanguage = lang => {
                    return Observable.of({ bar: 'baz' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'bar', bar: 'baz' });
            });

            it('should override nested translations via getConfigLanguage', async () => {
                locator.getLanguageDefaults = lang => {
                    return Observable.of({ prefix: { foo: 'bar' } });
                };
                locator.getConfigLanguage = lang => {
                    return Observable.of({ prefix: { bar: 'baz' } });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ prefix: { bar: 'baz' } });
            });

            it('should deep-merge translations via getConfigLanguage', async () => {
                locator.getLanguageDefaults = lang => {
                    return Observable.of({ prefix: { foo: 'bar' } });
                };
                locator.getConfigLanguage = lang => {
                    return Observable.of({ 'prefix.bar': 'baz' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ prefix: { foo: 'bar', bar: 'baz' } });
            });
        });
    });
});
