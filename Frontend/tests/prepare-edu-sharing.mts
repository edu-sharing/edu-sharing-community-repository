// Prepares a fresh Edu-Sharing installation for running e2e test.
//
// Run with
//      ts-node --esm tests/prepare-edu-sharing.mts

import * as dotenv from 'dotenv';
import fetch, { Response } from 'node-fetch';
import { URLSearchParams } from 'url';
import { strict as assert } from 'assert';

dotenv.config();

const rootUrl = process.env.E2E_TEST_BASE_URL + 'rest';

async function checkResponse(response: Response) {
    if (!response.ok) {
        const body = await response.json();
        console.error({ response, body });
        throw new Error('Request failed: ' + response.url);
    }
}

function getCookie(response: Response): string {
    const setCookie = response.headers.get('set-cookie');
    if (!setCookie) {
        throw new Error('No cookie set');
    }
    return setCookie.split(';')[0];
}

async function login() {
    const response = await fetch(rootUrl + '/authentication/v1/validateSession', {
        headers: {
            Authorization: 'Basic ' + Buffer.from('admin:admin').toString('base64'),
        },
    });
    await checkResponse(response);
    const body: any = await response.json();
    assert(body.isAdmin);
    return getCookie(response);
}

async function createTestUser(cookie: string, username: string) {
    const response = await fetch(
        rootUrl +
            '/iam/v1/people/-home-/' +
            username +
            '?' +
            new URLSearchParams({ password: username }),
        {
            method: 'POST',
            headers: { cookie, 'Content-Type': 'application/json', Accept: 'application/json' },
            body: JSON.stringify({
                email: username + '@example.org',
                firstName: username,
                lastName: 'tester',
            }),
        },
    );
    await checkResponse(response);
}

const username = 'e2e';

async function main() {
    const cookie = await login();
    console.log('logged in');
    await createTestUser(cookie, username);
    console.log('created user:', username);
}

main();
