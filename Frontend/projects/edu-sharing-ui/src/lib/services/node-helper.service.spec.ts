import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { ApiHelpersService, ConfigService, NetworkService } from 'ngx-edu-sharing-api';
import { EduSharingUiConfiguration } from '../edu-sharing-ui-configuration';
import { NodeHelperService } from './node-helper.service';
import { RepoUrlService } from './repo-url.service';
import { Toast } from './abstract/toast.service';

const translateServiceMock = {
    currentLang: 'de',
} as TranslateService;

describe('Test NodeHelper', () => {
    describe('Test cc license links', () => {
        let underTest: NodeHelperService;

        beforeEach(() => {
            TestBed.configureTestingModule({
                providers: [
                    NodeHelperService,
                    { provide: TranslateService, useValue: translateServiceMock },
                    { provide: ApiHelpersService, useValue: {} },
                    { provide: NetworkService, useValue: {} },
                    { provide: ConfigService, useValue: {} },
                    { provide: EduSharingUiConfiguration, useValue: {} },
                    { provide: RepoUrlService, useValue: {} },
                    { provide: Toast, useValue: {} },
                ],
            });
            underTest = TestBed.inject(NodeHelperService);
        });
        it('cc by 4.0', async () => {
            translateServiceMock.currentLang = 'de';
            expect(await underTest.getLicenseUrlByString('CC_BY', '4.0', '').toPromise()).toBe(
                'https://creativecommons.org/licenses/by/4.0/deed.de',
            );
            expect(await underTest.getLicenseUrlByString('CC_BY_SA', '4.0', 'de').toPromise()).toBe(
                'https://creativecommons.org/licenses/by-sa/4.0/deed.de',
            );
        });
        it('cc by sa 3.0', async () => {
            translateServiceMock.currentLang = 'de';
            expect(await underTest.getLicenseUrlByString('CC_BY_SA', '3.0', '').toPromise()).toBe(
                'https://creativecommons.org/licenses/by-sa/3.0/deed.de',
            );
            expect(await underTest.getLicenseUrlByString('CC_BY_SA', '3.0', 'fr').toPromise()).toBe(
                'https://creativecommons.org/licenses/by-sa/3.0/fr/deed.de',
            );
            expect(await underTest.getLicenseUrlByString('CC_BY_SA', '3.0', 'de').toPromise()).toBe(
                'https://creativecommons.org/licenses/by-sa/3.0/de/deed.de',
            );
            expect(await underTest.getLicenseUrlByString('CC_BY_SA', '3.0', 'de').toPromise()).toBe(
                'https://creativecommons.org/licenses/by-sa/3.0/de/deed.de',
            );

            translateServiceMock.currentLang = 'en';
            expect(await underTest.getLicenseUrlByString('CC_BY_SA', '3.0', 'de').toPromise()).toBe(
                'https://creativecommons.org/licenses/by-sa/3.0/de/deed.en',
            );
        });
    });
});
