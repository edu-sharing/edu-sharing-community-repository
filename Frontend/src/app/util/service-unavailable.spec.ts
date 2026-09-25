import { isServiceUnavailableRoute, returnUrl } from './service-unavailable';

describe('isServiceUnavailableRoute', () => {
    it('matches the route with and without trailing slash', () => {
        expect(isServiceUnavailableRoute('components/error/503')).toBeTrue();
        expect(isServiceUnavailableRoute('components/error/503/')).toBeTrue();
    });

    it('does not match other error pages', () => {
        expect(isServiceUnavailableRoute('components/error/500')).toBeFalse();
        expect(isServiceUnavailableRoute('components/error/503/details')).toBeFalse();
        expect(isServiceUnavailableRoute('components/workspace')).toBeFalse();
    });
});

describe('returnUrl', () => {
    const BASE = 'https://repo.example.org/edu-sharing/';

    it('returns to a path of the app, keeping query and hash', () => {
        expect(returnUrl('/edu-sharing/components/workspace?id=1#top', BASE)).toBe(
            '/edu-sharing/components/workspace?id=1#top',
        );
    });

    it('falls back to the start page without a usable next', () => {
        for (const next of [
            null,
            undefined,
            '',
            'https://evil.example.org/edu-sharing/',
            '//evil.example.org/edu-sharing/',
            '/\\evil.example.org/edu-sharing/',
            '/other-app/components',
            '/edu-sharing/../other-app/',
            'javascript:alert(1)',
            '/edu-sharing/components\r\nx',
        ]) {
            expect(returnUrl(next, BASE)).withContext(String(next)).toBe('/edu-sharing/');
        }
    });

    it('never returns to the unavailable page itself', () => {
        expect(returnUrl('/edu-sharing/components/error/503?next=%2F', BASE)).toBe('/edu-sharing/');
    });
});
