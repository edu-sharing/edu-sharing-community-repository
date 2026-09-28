import assert from 'node:assert/strict';
import { test } from 'node:test';
import type { LrmiMapping } from './config-cache.ts';
import { buildLrmi } from './lrmi.ts';
import type { LoadedNode, NodeProperties } from './node-metadata.ts';

/** exporter.lrmi from edu-sharing.reference.conf, trimmed to what the tests exercise */
const MAPPING: LrmiMapping = {
    properties: {
        identifier: 'sys:node-uuid',
        name: 'cclom:title,cm:name',
        keywords: 'cclom:general_keyword+ccm:classification_keyword',
        encodingFormat: 'AlfrescoMimeType',
        ContentSize: 'cclom:size',
    },
    vcard: {
        creator: 'ccm:lifecyclecontributer_author',
        publisher: 'ccm:lifecyclecontributer_publisher',
    },
};

const RENDER_URL = 'https://repo.example.org/edu-sharing/components/render/n1';

function node(properties: NodeProperties, extra: Partial<LoadedNode> = {}): LoadedNode {
    return { properties, mimetype: undefined, previewUrl: undefined, licenseUrl: undefined, ...extra };
}

test('carries the schema.org frame, the render url and the node level fields', () => {
    const subject = node(
        { 'sys:node-uuid': ['n1'] },
        { previewUrl: 'https://repo.example.org/preview?nodeId=n1', licenseUrl: 'https://creativecommons.org/licenses/by/4.0/' },
    );
    const lrmi = buildLrmi(subject, MAPPING, RENDER_URL);
    assert.equal(lrmi['@context'], 'http://schema.org/');
    assert.deepEqual(lrmi['@type'], ['CreativeWork', 'MediaObject']);
    assert.equal(lrmi['url'], RENDER_URL);
    assert.equal(lrmi['thumbnailUrl'], 'https://repo.example.org/preview?nodeId=n1');
    assert.equal(lrmi['license'], 'https://creativecommons.org/licenses/by/4.0/');
    assert.equal(lrmi['identifier'], 'n1');
});

test('drops fields without a value, as org.json does for null', () => {
    const subject = node({});
    const lrmi = buildLrmi(subject, MAPPING, RENDER_URL);
    for (const key of ['license', 'thumbnailUrl', 'identifier', 'name', 'creator', 'dateCreated']) {
        assert.equal(key in lrmi, false, key);
    }
});

test('"," takes the first filled alternative', () => {
    const withTitle = node({ 'cclom:title': ['Title'], 'cm:name': ['file.pdf'] });
    assert.equal(buildLrmi(withTitle, MAPPING, RENDER_URL)['name'], 'Title');

    const emptyTitle = node({ 'cclom:title': [''], 'cm:name': ['file.pdf'] });
    assert.equal(buildLrmi(emptyTitle, MAPPING, RENDER_URL)['name'], 'file.pdf');
});

test('"+" concatenates, a single value stays a scalar', () => {
    const both = node({ 'cclom:general_keyword': ['a', 'b'], 'ccm:classification_keyword': ['c'] });
    assert.deepEqual(buildLrmi(both, MAPPING, RENDER_URL)['keywords'], ['a', 'b', 'c']);

    const one = node({ 'ccm:classification_keyword': ['c'] });
    assert.equal(buildLrmi(one, MAPPING, RENDER_URL)['keywords'], 'c');
});

test('AlfrescoMimeType falls back to the node mimetype', () => {
    const subject = node({}, { mimetype: 'application/pdf' });
    assert.equal(buildLrmi(subject, MAPPING, RENDER_URL)['encodingFormat'], 'application/pdf');
});

test('formats the *_LONG dates like DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT', () => {
    const subject = node({ 'cm:created_LONG': ['1763387774186'], 'cm:modified_LONG': ['not-a-number'] });
    const lrmi = buildLrmi(subject, MAPPING, RENDER_URL);
    assert.equal(lrmi['dateCreated'], '2025-11-17T13:56:14+00:00');
    // LRMITool passes an unparsable value through
    assert.equal(lrmi['dateModified'], 'not-a-number');
    assert.equal('datePublished' in lrmi, false);
});

test('maps a person vcard to a schema.org Person', () => {
    const subject = node({
        'ccm:lifecyclecontributer_author': ['BEGIN:VCARD\nVERSION:3.0\nN:Miller;Mike\nFN:Mike Miller\nEND:VCARD'],
    });
    assert.deepEqual(buildLrmi(subject, MAPPING, RENDER_URL)['creator'], {
        '@type': 'Person',
        givenName: 'Mike',
        familyName: 'Miller',
    });
});

test('maps an organization vcard to a schema.org Organization', () => {
    const subject = node({
        'ccm:lifecyclecontributer_publisher': ['BEGIN:VCARD\nVERSION:3.0\nORG:edu-sharing.net e.V.\nEND:VCARD'],
    });
    assert.deepEqual(buildLrmi(subject, MAPPING, RENDER_URL)['publisher'], {
        '@type': 'Organization',
        legalName: 'edu-sharing.net e.V.',
    });
});

test('several vcards become an array, nameless ones are skipped', () => {
    const subject = node({
        'ccm:lifecyclecontributer_author': [
            'BEGIN:VCARD\nVERSION:3.0\nN:Miller;Mike\nEND:VCARD',
            'BEGIN:VCARD\nVERSION:3.0\nEND:VCARD',
            'BEGIN:VCARD\r\nVERSION:3.0\r\nN:Doe;Jane\r\nEND:VCARD',
        ],
    });
    assert.deepEqual(buildLrmi(subject, MAPPING, RENDER_URL)['creator'], [
        { '@type': 'Person', givenName: 'Mike', familyName: 'Miller' },
        { '@type': 'Person', givenName: 'Jane', familyName: 'Doe' },
    ]);
});
