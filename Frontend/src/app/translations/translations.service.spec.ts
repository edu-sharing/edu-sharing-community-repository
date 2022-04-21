import { fakeAsync } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import * as rxjs from 'rxjs';
import { BridgeService } from '../core-bridge-module/bridge.service';
import { ConfigurationService, SessionStorageService } from '../core-module/core.module';
import { TranslationsService } from './translations.service';

class BridgeServiceStub {
    isRunningCordova() {
        return false;
    }
}

class ConfigurationServiceStub {
    get(key: string, defaultValue?: any) {
        return rxjs.of(defaultValue);
    }
    instant(key: string, defaultValue?: any) {
        return defaultValue;
    }
    getLocator() {
        return {
            getBridge: () => new BridgeServiceStub(),
        };
    }
}

class SessionStorageServiceStub {
    get(key: string) {
        return rxjs.of(null);
    }
    set(key: string, value: string) {}
}

class ActivatedRouteStub {
    queryParams = rxjs.of({});
}

describe('TranslationsService', () => {
    describe('initialize', () => {
        let translationsService: TranslationsService;
        let translateServiceSpy: any;
        let bridgeServiceStub: BridgeServiceStub;
        let configurationServiceStub: ConfigurationServiceStub;
        let sessionStorageServiceStub: SessionStorageServiceStub;
        let activatedRouteStub: ActivatedRouteStub;

        beforeEach(() => {
            translateServiceSpy = jasmine.createSpyObj('TranslateService', [
                'use',
                'addLangs',
                'getBrowserLang',
                'setDefaultLang',
                'getTranslation',
            ]);
            translateServiceSpy.getTranslation.and.callFake(() => rxjs.of(null));
            translateServiceSpy.use.and.callFake(() => rxjs.of(null));
            bridgeServiceStub = new BridgeServiceStub();
            configurationServiceStub = new ConfigurationServiceStub();
            sessionStorageServiceStub = new SessionStorageServiceStub();
            activatedRouteStub = new ActivatedRouteStub();
            translationsService = new TranslationsService(
                bridgeServiceStub as BridgeService,
                configurationServiceStub as ConfigurationService,
                activatedRouteStub as any as ActivatedRoute,
                sessionStorageServiceStub as any as SessionStorageService,
                translateServiceSpy as TranslateService,
            );
        });

        function callInitialize(): Promise<string> {
            return new Promise((resolve) => {
                fakeAsync(() => {
                    translationsService.initialize().subscribe(() => {
                        resolve(translationsService.getLanguage());
                    }, fail);
                })();
            });
        }

        it('should use default languages', async () => {
            const configurationServiceSpy = spyOn(
                configurationServiceStub,
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
            expect(translateServiceSpy.use.calls.mostRecent().args).toEqual(['de']);
        });

        it('should call translate.setDefaultLang()', async () => {
            await callInitialize();
            expect(translateServiceSpy.setDefaultLang.calls.count()).toBe(1);
            expect(translateServiceSpy.setDefaultLang.calls.mostRecent().args).toEqual(['de']);
        });

        it('should use browserLang', async () => {
            translateServiceSpy.getBrowserLang.and.returnValue('en');
            const lang = await callInitialize();
            expect(lang).toBe('en');
        });

        it('should use sessionStorage', async () => {
            // Should be overridden by sessionStorage
            translateServiceSpy.getBrowserLang.and.returnValue('de');
            const storageGetSpy = spyOn(sessionStorageServiceStub, 'get').and.returnValue(
                rxjs.of('en'),
            );
            const lang = await callInitialize();
            expect(storageGetSpy.calls.count()).toBe(1, 'storage.get was called once');
            expect(storageGetSpy.calls.mostRecent().args).toEqual(['language']);
            expect(lang).toBe('en');
        });

        it('should store used language in sessionStorage', async () => {
            translateServiceSpy.getBrowserLang.and.returnValue('en');
            const storageSetSpy = spyOn(sessionStorageServiceStub, 'set');
            await callInitialize();
            expect(storageSetSpy.calls.count()).toBe(1, 'storage.set was called once');
            expect(storageSetSpy.calls.mostRecent().args).toEqual(['language', 'en']);
        });

        it('should use queryParams', async () => {
            // Should be overridden by queryParams
            translateServiceSpy.getBrowserLang.and.returnValue('de');
            // Should also be overridden by queryParams
            const storageGetSpy = spyOn(sessionStorageServiceStub, 'get').and.returnValue(
                rxjs.of('de'),
            );
            activatedRouteStub.queryParams = rxjs.of({ locale: 'en' });
            const lang = await callInitialize();
            expect(lang).toBe('en');
        });
    });
});
