import assert from 'node:assert/strict';
import { test } from 'node:test';
import { defaultLocationUrl } from './default-location.ts';

test('a route of the app goes below components/', () => {
    assert.equal(defaultLocationUrl('/edu-sharing', 'workspace'), '/edu-sharing/components/workspace');
    assert.equal(defaultLocationUrl('/edu-sharing', '/search'), '/edu-sharing/components/search');
    assert.equal(defaultLocationUrl('/edu-sharing', 'workspace/files?id=1'), '/edu-sharing/components/workspace/files?id=1');
});

test('an absolute url is taken as is', () => {
    assert.equal(defaultLocationUrl('/edu-sharing', 'https://portal.example.org/start'), 'https://portal.example.org/start');
});

test('without a value it is the login, like the backend default', () => {
    assert.equal(defaultLocationUrl('/edu-sharing', undefined), '/edu-sharing/components/login');
    assert.equal(defaultLocationUrl('/edu-sharing', '  '), '/edu-sharing/components/login');
    assert.equal(defaultLocationUrl('', undefined), '/components/login');
});
