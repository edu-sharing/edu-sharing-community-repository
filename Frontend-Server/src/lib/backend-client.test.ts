import assert from 'node:assert/strict';
import { test } from 'node:test';
import type { FastifyRequest } from 'fastify';
import { anonymousFor, normalizeDomain } from './backend-client.ts';

test('normalizes a host name: lower case, no trailing dot', () => {
    assert.equal(normalizeDomain('Repo.Example.ORG'), 'repo.example.org');
    assert.equal(normalizeDomain('repo.example.org.'), 'repo.example.org');
    assert.equal(normalizeDomain(' repo.example.org '), 'repo.example.org');
});

test('the config fetch carries the host the way a browser sends it', () => {
    const request = { headers: { host: 'Repo.Example.org:8443' }, id: 'r1', hostname: 'Repo.Example.org', protocol: 'https' } as unknown as FastifyRequest;
    const headers = anonymousFor(request);
    assert.equal(headers.host, 'repo.example.org:8443');
    assert.equal(headers['x-forwarded-host'], 'repo.example.org');
});
