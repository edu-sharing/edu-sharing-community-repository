import assert from 'node:assert/strict';
import { test } from 'node:test';
import { securityHeaders } from './security-headers.ts';

/** angular.headers from edu-sharing.reference.conf, trimmed */
const REFERENCE = {
    'X-XSS-Protection': '1',
    'X-Frame-Options': 'sameorigin',
    'Content-Security-Policy': {
        'default-src': "* data: blob: 'self' gap://ready",
        'style-src': "* 'nonce-{{ngCspNonce}}'",
    },
};

test('builds the headers SecurityHeadersFilter sends', () => {
    assert.deepEqual(securityHeaders(REFERENCE, 'abc'), {
        'X-XSS-Protection': '1',
        'X-Frame-Options': 'sameorigin',
        'Content-Security-Policy': "default-src * data: blob: 'self' gap://ready; style-src * 'nonce-abc'; ",
    });
});

test('Referrer-Policy only when configured', () => {
    assert.equal(securityHeaders({ ...REFERENCE, 'Referrer-Policy': 'no-referrer' }, 'n')['Referrer-Policy'], 'no-referrer');
    assert.equal('Referrer-Policy' in securityHeaders(REFERENCE, 'n'), false);
});

test('ignores everything else below angular.headers, as the filter does', () => {
    assert.deepEqual(Object.keys(securityHeaders({ ...REFERENCE, 'X-Custom': 'x' }, 'n')).sort(), [
        'Content-Security-Policy',
        'X-Frame-Options',
        'X-XSS-Protection',
    ]);
});

test('an empty config yields no headers', () => {
    assert.deepEqual(securityHeaders({}, 'n'), {});
});
