const SESSION_HEADER_NAME = 'Authentication-Info';
const RENDER_DATA_PATH = 'public/renderdata';
const JOB_INFO_PATH = 'public/job';
const H5P_PATH = 'public/h5p';
const ASSET_PATH = 'public/asset';
const TRACKING_PATH = 'public/tracking';
const DB_NAME = 'Edu-Sharing-Rendering';
const DB_STORE = 'SessionIds';
const DB_VERSION = 2;
/** @type {IDBDatabase} */
let eduServiceWorkerDb;

self.addEventListener('install', async function (event) {
    // Skip the 'waiting' lifecycle phase, to go directly from 'installed' to 'activated', even if
    // there are still previous incarnations of this service worker registration active.
    console.info('install');
    event.waitUntil(self.skipWaiting());
});

self.addEventListener('activate', async function (event) {
    console.info('activate');
    clients.claim();
});

self.addEventListener('controllerchange', () => {
    console.info('New service worker activated. Please reload the page.');
});

self.addEventListener('message', (event) => {
    if (event.data && event.data.type === 'SKIP_WAITING') {
        self.skipWaiting();
    }
});

self.addEventListener('fetch', (event) => {
    const request = event.request;
    const url = request.url;
    if (url.includes(RENDER_DATA_PATH)) {
        event.respondWith(handleRenderDataRequest(request));
    } else if (
        url.includes(JOB_INFO_PATH) ||
        url.includes(H5P_PATH) ||
        url.includes(ASSET_PATH) ||
        url.includes(TRACKING_PATH)
    ) {
        event.respondWith(handleSessionRequest(request));
    } else {
        //use direct return to not break xhr.onreadystatechange
        return;
    }
});

/**
 * Function handleSessionRequest
 *
 * @param {Request} request
 * @returns {Promise<Response>}
 */
const handleSessionRequest = async (request) => {
    await getDb().catch((error) => {
        console.error(error);
        return handleStandardRequest(request);
    });
    const url = new URL(request.url);
    const domain = url.hostname;
    const sessionId = await retrieveSessionIdFromDb(domain);
    if (sessionId === '') {
        return handleStandardRequest(request);
    }
    const modifiedHeaders = new Headers(request.headers);
    modifiedHeaders.set(SESSION_HEADER_NAME, sessionId);
    const modifiedRequest = new Request(request, {
        method: request.method,
        mode: 'cors',
        credentials: 'include',
        headers: modifiedHeaders,
        body:
            request.method !== 'GET' && request.method !== 'HEAD'
                ? await request.clone().blob()
                : undefined,
    });
    return await fetch(modifiedRequest);
};

/**
 * Function handleRenderDataRequest
 *
 * @param {Request} request
 * @returns {Promise<Response>}
 */
const handleRenderDataRequest = async (request) => {
    await getDb().catch((error) => {
        console.error(error);
        return handleStandardRequest(request);
    });
    const url = new URL(request.url);
    const domain = url.hostname;
    const existingSessionId = await retrieveSessionIdFromDb(domain);
    const modifiedHeaders = new Headers(request.headers);
    if (existingSessionId !== '') {
        modifiedHeaders.set(SESSION_HEADER_NAME, existingSessionId);
    }
    const modifiedRequest = new Request(request, {
        mode: 'cors',
        credentials: 'include',
        headers: modifiedHeaders,
    });
    const response = await fetch(modifiedRequest);
    const newSessionId = response.headers.get(SESSION_HEADER_NAME);
    if (newSessionId !== null && newSessionId !== '' && newSessionId !== existingSessionId) {
        await storeSessionIdToDb(newSessionId, domain);
    }
    return response;
};

/**
 * Function handleStandardRequest
 *
 * @param {Request} request
 * @returns {Promise<Response>}
 */
const handleStandardRequest = async (request) => {
    return await fetch(request);
};

/**
 * Function getDb
 *
 * @returns {Promise<String>}
 */
const getDb = () => {
    return new Promise((resolve, reject) => {
        if (eduServiceWorkerDb !== undefined) {
            resolve('success');
        }
        if (!indexedDB) {
            reject(
                new Error(
                    'IndexedDB is not supported by current browser. Header auth with Edu-Sharing Rendering Service is not available',
                ),
            );
        }
        const request = indexedDB.open(DB_NAME, DB_VERSION);
        request.error = (event) => {
            console.error(event);
            reject(new Error('Error opening indexedDB'));
        };
        request.onupgradeneeded = (event) => {
            const db = request.result;
            const oldVersion = event.oldVersion;

            if (oldVersion < 2) {
                if (db.objectStoreNames.contains(DB_STORE)) {
                    db.deleteObjectStore(DB_STORE);
                }
                db.createObjectStore(DB_STORE, { keyPath: 'domain' });
            }
        };
        request.onsuccess = () => {
            eduServiceWorkerDb = request.result;
            resolve('success');
        };
    });
};

/**
 * Function storeSessionIdToDb
 *
 * @param {string} sessionId
 * @param {string} domain
 */
const storeSessionIdToDb = (sessionId, domain) => {
    return new Promise((resolve, reject) => {
        const transaction = eduServiceWorkerDb.transaction(DB_STORE, 'readwrite');
        const store = transaction.objectStore(DB_STORE);
        store.put({ domain: domain, sessionId: sessionId });
        transaction.oncomplete = () => {
            resolve('Indexed DB transaction completed');
        };
        transaction.onerror = (event) => {
            console.error('DB transaction failed. Cannot store session id.');
            console.error(event);
            reject(new Error('Transaction failed'));
        };
        transaction.onabort = (ev) => {
            console.error('DB transaction aborted. Cannot store session id.');
            console.error(ev);
            reject(new Error('Transaction aborted'));
        };
    });
};

/**
 * Function retrieveSessionIdFromDb
 *
 * Gets the stored session id from the database. Returns a promise resolving to the result on success
 * and to an empty string on failure
 *
 * @return {Promise<String>}
 * @param {string} domain
 */
const retrieveSessionIdFromDb = (domain) => {
    return new Promise((resolve, reject) => {
        let result = '';
        const transaction = eduServiceWorkerDb.transaction(DB_STORE, 'readonly');
        const store = transaction.objectStore(DB_STORE);
        const idQuery = store.get(domain);
        idQuery.onsuccess = () => {
            result = idQuery.result !== undefined ? idQuery.result.sessionId : '';
        };
        idQuery.onerror = () => {
            console.error('Query by id failed');
        };
        transaction.oncomplete = () => {
            resolve(result);
        };
        transaction.onerror = (event) => {
            console.error('DB transaction failed. Cannot retrieve session id.');
            console.error(event);
            reject(new Error('Transaction failed'));
        };
        transaction.onabort = (ev) => {
            console.error('DB transaction aborted. Cannot retrieve session id.');
            console.error(ev);
            reject(new Error('Transaction aborted'));
        };
    });
};
