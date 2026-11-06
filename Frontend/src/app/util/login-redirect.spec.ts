import { LOGIN_REDIRECT_PARAM, withLoginRedirect } from './login-redirect';

// jsdom runs on http://localhost/
const ORIGIN = window.location.origin;

describe('withLoginRedirect', () => {
    it('appends the target path of an absolute next', () => {
        expect(
            withLoginRedirect(
                '/edu-sharing/shibboleth',
                `${ORIGIN}/edu-sharing/components/render/abc?x=1`,
            ),
        ).toBe(
            '/edu-sharing/shibboleth?redirect=' +
                encodeURIComponent('/edu-sharing/components/render/abc?x=1'),
        );
    });

    it('keeps existing query parameters of the sso start', () => {
        expect(
            withLoginRedirect('/edu-sharing/sso?id=google', '/edu-sharing/components/workspace'),
        ).toBe(
            '/edu-sharing/sso?id=google&redirect=' +
                encodeURIComponent('/edu-sharing/components/workspace'),
        );
    });

    it('leaves the url alone without a target', () => {
        expect(withLoginRedirect('/edu-sharing/shibboleth', undefined)).toBe(
            '/edu-sharing/shibboleth',
        );
        expect(withLoginRedirect('/edu-sharing/shibboleth', '')).toBe('/edu-sharing/shibboleth');
    });

    it('never hands a foreign target on', () => {
        expect(
            withLoginRedirect('/edu-sharing/shibboleth', 'https://evil.example.org/edu-sharing/'),
        ).toBe('/edu-sharing/shibboleth');
    });

    it('does not touch external login urls', () => {
        expect(
            withLoginRedirect('https://idp.example.org/login', '/edu-sharing/components/workspace'),
        ).toBe('https://idp.example.org/login');
    });

    it('accepts the configured server url as own origin', () => {
        const serverUrl = 'https://repo.example.org/';
        expect(
            withLoginRedirect(
                'https://repo.example.org/edu-sharing/shibboleth',
                'https://repo.example.org/edu-sharing/components/render/abc',
                serverUrl,
            ),
        ).toBe(
            'https://repo.example.org/edu-sharing/shibboleth?redirect=' +
                encodeURIComponent('/edu-sharing/components/render/abc'),
        );
    });

    it('does not add a second redirect parameter', () => {
        const url = `/edu-sharing/shibboleth?${LOGIN_REDIRECT_PARAM}=%2Fedu-sharing%2Fcomponents%2Fa`;
        expect(withLoginRedirect(url, '/edu-sharing/components/b')).toBe(url);
    });
});
