// Prepares a fresh Edu-Sharing installation for running e2e test.
//
// Run with
//      ts-node --esm tests/scripts/prepare-edu-sharing.mts

import { strict as assert } from 'assert';
import * as dotenv from 'dotenv';
import got from 'got';
import { CookieJar } from 'tough-cookie';

dotenv.config();

const rootUrl = process.env.E2E_TEST_BASE_URL + 'rest';
const cookieJar = new CookieJar();

async function login() {
    const body: any = await got
        .get(rootUrl + '/authentication/v1/validateSession', {
            username: 'admin',
            password: 'admin',
            cookieJar,
        })
        .json();
    assert(body.isAdmin);
}

async function createTestUser(username: string) {
    await got
        .post(rootUrl + '/iam/v1/people/-home-/' + username, {
            searchParams: { password: username },
            json: {
                email: username + '@example.org',
                firstName: username,
                lastName: 'tester',
            },
            cookieJar,
        })
        .json();
}

const username = 'e2e';

async function main() {
    await login();
    console.log('logged in');
    await createTestUser(username);
    console.log('created user:', username);
}

main();
