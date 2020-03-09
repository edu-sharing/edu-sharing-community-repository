import { fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import {
    ConfigurationService,
    SessionStorageService,
} from '../core-module/core.module';
import { Translation } from './translation';

class ConfigurationStubService {
    get(key: string, defaultValue?: any) {
        return Observable.of(defaultValue);
    }
    instant(key: string, defaultValue?: any) {
        return defaultValue;
    }
    getLocator() {
        return {
            getBridge: () => ({
                isRunningCordova: () => false,
            }),
        };
    }
}

class SessionStorageStubService {
    get(key: string) {
        return Observable.of(null);
    }
    set(key: string, value: string) {}
}

class ActivatedRouteStub {
    queryParams = Observable.of({});
}

describe('Translation', () => {
    describe('initialize', () => {
        let translateServiceSpy: any;
        let configurationStubService: ConfigurationStubService;
        let sessionStorageStubService: SessionStorageStubService;
        let activatedRouteStub: ActivatedRouteStub;

        beforeEach(() => {
            translateServiceSpy = jasmine.createSpyObj('TranslateService', [
                'use',
                'addLangs',
                'getBrowserLang',
                'setDefaultLang',
                'getTranslation',
            ]);
            translateServiceSpy.getTranslation.and.callFake(() =>
                Observable.of(null),
            );
            configurationStubService = new ConfigurationStubService();
            sessionStorageStubService = new SessionStorageStubService();
            activatedRouteStub = new ActivatedRouteStub();
        });

        function callInitialize(): Promise<string> {
            return new Promise(resolve => {
                fakeAsync(() => {
                    Translation.initialize(
                        translateServiceSpy as TranslateService,
                        configurationStubService as ConfigurationService,
                        (sessionStorageStubService as any) as SessionStorageService,
                        (activatedRouteStub as any) as ActivatedRoute,
                    ).subscribe(result => {
                        resolve(result);
                    }, fail);
                    tick(100);
                })();
            });
        }

        it('should use default languages', async () => {
            const configurationServiceSpy = spyOn(
                configurationStubService,
                'get',
            ).and.callThrough();
            await callInitialize();
            expect(configurationServiceSpy.calls.count()).toBe(
                1,
                'configurationService.get was called once',
            );
            expect(configurationServiceSpy.calls.mostRecent().args).toEqual([
                'supportedLanguages',
                ['de', 'en', 'none'], // Translation.DEFAULT_SUPPORTED_LANGUAGES
            ]);
        });

        it('should default to "de"', async () => {
            const lang = await callInitialize();
            expect(lang).toBe('de');
        });

        it('should call translate.use()', async () => {
            await callInitialize();
            expect(translateServiceSpy.use.calls.count()).toBe(1);
            expect(translateServiceSpy.use.calls.mostRecent().args).toEqual([
                'de',
            ]);
        });

        it('should call translate.setDefaultLang()', async () => {
            await callInitialize();
            expect(translateServiceSpy.setDefaultLang.calls.count()).toBe(1);
            expect(
                translateServiceSpy.setDefaultLang.calls.mostRecent().args,
            ).toEqual(['de']);
        });

        it('should use browserLang', async () => {
            translateServiceSpy.getBrowserLang.and.returnValue('en');
            const lang = await callInitialize();
            expect(lang).toBe('en');
        });

        it('should use sessionStorage', async () => {
            // Should be overridden by sessionStorage
            translateServiceSpy.getBrowserLang.and.returnValue('de');
            const storageGetSpy = spyOn(
                sessionStorageStubService,
                'get',
            ).and.returnValue(Observable.of('en'));
            const lang = await callInitialize();
            expect(storageGetSpy.calls.count()).toBe(
                1,
                'storage.get was called once',
            );
            expect(storageGetSpy.calls.mostRecent().args).toEqual(['language']);
            expect(lang).toBe('en');
        });

        it('should store used language in sessionStorage', async () => {
            translateServiceSpy.getBrowserLang.and.returnValue('en');
            const storageSetSpy = spyOn(sessionStorageStubService, 'set');
            await callInitialize();
            expect(storageSetSpy.calls.count()).toBe(
                1,
                'storage.set was called once',
            );
            expect(storageSetSpy.calls.mostRecent().args).toEqual([
                'language',
                'en',
            ]);
        });

        it('should use queryParams', async () => {
            // Should be overridden by queryParams
            translateServiceSpy.getBrowserLang.and.returnValue('de');
            // Should also be overridden by queryParams
            const storageGetSpy = spyOn(
                sessionStorageStubService,
                'get',
            ).and.returnValue(Observable.of('de'));
            activatedRouteStub.queryParams = Observable.of({ locale: 'en' });
            const lang = await callInitialize();
            expect(lang).toBe('en');
        });
    });
});
