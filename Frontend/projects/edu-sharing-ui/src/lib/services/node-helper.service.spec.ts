import { NodeHelperService } from './node-helper.service';
import { TranslateService } from '@ngx-translate/core';

const translateServiceMock = {
    currentLang: 'de',
} as TranslateService;

describe('Test NodeHelper', () => {
    describe('Test cc license links', () => {
        const underTest = new NodeHelperService(
            translateServiceMock,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
        );
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
