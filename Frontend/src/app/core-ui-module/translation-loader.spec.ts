import { HttpClient } from '@angular/common/http';
import { fakeAsync, tick } from '@angular/core/testing';
import * as rxjs from 'rxjs';
import { Observable } from 'rxjs';
import { RestLocatorService } from '../core-module/core.module';
import { TranslationLoader, TRANSLATION_LIST } from './translation-loader';
import { Translation } from './translation';
import { TranslationSource } from './translation-source';
import { ConfigService } from 'ngx-edu-sharing-api';

class HttpClientStub {
    get(url: string): Observable<any> {
        return rxjs.of(null);
    }
}

class ConfigStub {
    getCustomTranslations(lang: string): Observable<any> {
        return rxjs.of(null);
    }
    getDefaultTranslations(lang: string): Observable<any> {
        return rxjs.of(null);
    }
}

describe('TranslationLoader', () => {
    let translationLoader: TranslationLoader;
    let httpClient: HttpClientStub;
    let config: ConfigStub;

    beforeEach(() => {
        httpClient = new HttpClientStub();
        config = new ConfigStub();
        translationLoader = new TranslationLoader(
            httpClient as HttpClient,
            config as unknown as ConfigService,
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
                    config,
                    'getCustomTranslations',
                ).and.callThrough();
                const getLanguageDefaultsSpy = spyOn(
                    config,
                    'getDefaultTranslations',
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

            it('should call getCustomTranslations', async () => {
                const getConfigLanguageSpy = spyOn(
                    config,
                    'getCustomTranslations',
                ).and.callThrough();
                await callGetTranslation('de');
                expect(getConfigLanguageSpy.calls.count()).toBe(1);
            });

            it('should call getCustomTranslations with correct locale', async () => {
                const getConfigLanguageSpy = spyOn(
                    config,
                    'getCustomTranslations',
                ).and.callThrough();
                await callGetTranslation('de');
                expect(getConfigLanguageSpy.calls.mostRecent().args).toEqual([
                    'de_DE',
                ]);
            });

            it('should not call getDefaultTranslations', async () => {
                const getLanguageDefaultsSpy = spyOn(
                    config,
                    'getDefaultTranslations',
                ).and.callThrough();
                await callGetTranslation('de');
                expect(getLanguageDefaultsSpy.calls.count()).toBe(0);
            });

            it('should include translations via http', async () => {
                httpClient.get = url => {
                    if (url === 'assets/i18n/common/de.json') {
                        return rxjs.of({ foo: 'bar' });
                    }
                    return rxjs.of(null);
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'bar' });
            });

            it('should merge translations via http', async () => {
                httpClient.get = url => {
                    if (url === 'assets/i18n/common/de.json') {
                        return rxjs.of({ foo: 'bar' });
                    } else if (url === 'assets/i18n/admin/de.json') {
                        return rxjs.of({ bar: 'baz' });
                    }
                    return rxjs.of(null);
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'bar', bar: 'baz' });
            });

            it('should override nested translations via http', async () => {
                httpClient.get = url => {
                    if (url === 'assets/i18n/common/de.json') {
                        return rxjs.of({ prefix: { foo: 'bar' } });
                    } else if (url === 'assets/i18n/admin/de.json') {
                        return rxjs.of({ prefix: { bar: 'baz' } });
                    }
                    return rxjs.of(null);
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ prefix: { bar: 'baz' } });
            });

            it('should include translations via getCustomTranslations', async () => {
                config.getCustomTranslations = lang => {
                    return rxjs.of({ foo: 'bar' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'bar' });
            });

            it('should override translations via getCustomTranslations', async () => {
                httpClient.get = url => {
                    if (url === 'assets/i18n/common/de.json') {
                        return rxjs.of({ foo: 'bar' });
                    }
                    return rxjs.of(null);
                };
                config.getCustomTranslations = lang => {
                    return rxjs.of({ foo: 'baz' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'baz' });
            });

            it('should merge translations via getCustomTranslations', async () => {
                httpClient.get = url => {
                    if (url === 'assets/i18n/common/de.json') {
                        return rxjs.of({ foo: 'bar' });
                    }
                    return rxjs.of(null);
                };
                config.getCustomTranslations = lang => {
                    return rxjs.of({ bar: 'baz' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'bar', bar: 'baz' });
            });

            it('should override nested translations via getCustomTranslations', async () => {
                httpClient.get = url => {
                    if (url === 'assets/i18n/common/de.json') {
                        return rxjs.of({ prefix: { foo: 'bar' } });
                    }
                    return rxjs.of(null);
                };
                config.getCustomTranslations = lang => {
                    return rxjs.of({ prefix: { bar: 'baz' } });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ prefix: { bar: 'baz' } });
            });

            it('should deep-merge translations via getCustomTranslations', async () => {
                httpClient.get = url => {
                    if (url === 'assets/i18n/common/de.json') {
                        return rxjs.of({ prefix: { foo: 'bar' } });
                    }
                    return rxjs.of(null);
                };
                config.getCustomTranslations = lang => {
                    return rxjs.of({ 'prefix.bar': 'baz' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ prefix: { foo: 'bar', bar: 'baz' } });
            });

            it('should deep-merge translations via getCustomTranslations (3 levels)', async () => {
                httpClient.get = url => {
                    if (url === 'assets/i18n/common/de.json') {
                        return rxjs.of({
                            l1: { l2: { l3: { foo: 'bar' } } },
                        });
                    }
                    return rxjs.of(null);
                };
                config.getCustomTranslations = lang => {
                    return rxjs.of({ 'l1.l2.l3.bar': 'baz' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({
                    l1: { l2: { l3: { foo: 'bar', bar: 'baz' } } },
                });
            });

            it('should create missing levels via getCustomTranslations', async () => {
                httpClient.get = url => {
                    if (url === 'assets/i18n/common/de.json') {
                        return rxjs.of({});
                    }
                    return rxjs.of(null);
                };
                config.getCustomTranslations = lang => {
                    return rxjs.of({ 'l1.l2.l3.bar': 'baz' });
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
                    config,
                    'getCustomTranslations',
                ).and.callThrough();
                const getLanguageDefaultsSpy = spyOn(
                    config,
                    'getDefaultTranslations',
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

            it('should call getCustomTranslations', async () => {
                const getConfigLanguageSpy = spyOn(
                    config,
                    'getCustomTranslations',
                ).and.callThrough();
                await callGetTranslation('de');
                expect(getConfigLanguageSpy.calls.count()).toBe(1);
            });

            it('should call getDefaultTranslations', async () => {
                const getLanguageDefaultsSpy = spyOn(
                    config,
                    'getDefaultTranslations',
                ).and.callThrough();
                await callGetTranslation('de');
                expect(getLanguageDefaultsSpy.calls.count()).toBe(1);
            });

            it('should call getDefaultTranslations with correct locale', async () => {
                const getLanguageDefaultsSpy = spyOn(
                    config,
                    'getDefaultTranslations',
                ).and.callThrough();
                await callGetTranslation('de');
                expect(getLanguageDefaultsSpy.calls.mostRecent().args).toEqual([
                    'de_DE',
                ]);
            });

            it('should include translations via getDefaultTranslations', async () => {
                config.getDefaultTranslations = lang => {
                    return rxjs.of({ foo: 'bar' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'bar' });
            });

            it('should include translations via getCustomTranslations', async () => {
                config.getCustomTranslations = lang => {
                    return rxjs.of({ foo: 'bar' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'bar' });
            });

            it('should merge translations via getCustomTranslations', async () => {
                config.getDefaultTranslations = lang => {
                    return rxjs.of({ foo: 'bar' });
                };
                config.getCustomTranslations = lang => {
                    return rxjs.of({ bar: 'baz' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'bar', bar: 'baz' });
            });

            it('should override nested translations via getCustomTranslations', async () => {
                config.getDefaultTranslations = lang => {
                    return rxjs.of({ prefix: { foo: 'bar' } });
                };
                config.getCustomTranslations = lang => {
                    return rxjs.of({ prefix: { bar: 'baz' } });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ prefix: { bar: 'baz' } });
            });

            it('should deep-merge translations via getCustomTranslations', async () => {
                config.getDefaultTranslations = lang => {
                    return rxjs.of({ prefix: { foo: 'bar' } });
                };
                config.getCustomTranslations = lang => {
                    return rxjs.of({ 'prefix.bar': 'baz' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ prefix: { foo: 'bar', bar: 'baz' } });
            });
        });
    });
});
