import assert from 'node:assert/strict';
import { test } from 'node:test';
import { autoStaticCacheBytes } from './config.ts';

const MB = 1024 * 1024;
const HOST = 64 * 1024 * MB;

test('the static cache takes 20 % of the container limit', () => {
    assert.equal(autoStaticCacheBytes(256 * MB, HOST), Math.floor(256 * MB * 0.2));
    assert.equal(autoStaticCacheBytes(512 * MB, HOST), Math.floor(512 * MB * 0.2));
    assert.equal(autoStaticCacheBytes(1024 * MB, HOST), Math.floor(1024 * MB * 0.2));
});

test('at least 16 MB, at most 256 MB', () => {
    assert.equal(autoStaticCacheBytes(32 * MB, HOST), 16 * MB);
    assert.equal(autoStaticCacheBytes(4096 * MB, HOST), 256 * MB);
});

test('without a container limit the host memory counts', () => {
    // node reports "no limit" as 0 or as a huge cgroup value
    assert.equal(autoStaticCacheBytes(0, 1024 * MB), Math.floor(1024 * MB * 0.2));
    assert.equal(autoStaticCacheBytes(17592186044416 * MB, 1024 * MB), Math.floor(1024 * MB * 0.2));
    assert.equal(autoStaticCacheBytes(0, HOST), 256 * MB);
});
