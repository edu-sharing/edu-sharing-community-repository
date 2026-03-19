import { Observable, Subject } from 'rxjs';
import { debounceTime, shareReplay, startWith } from 'rxjs/operators';
import { switchReplay } from '../rxjs-operators/switch-replay';

/**
 * default duration (s) cache for an api return function
 */
type CacheEntryType = 'switchReplay' | 'shareReplay';
export const DEFAULT_API_CACHE_DURATION = 5;
class KeyCacheEntry<T> {
    private readonly _trigger = new Subject<void>();
    readonly observable: Observable<T>;
    created: number;

    constructor(getObservable: () => Observable<T>, type: CacheEntryType) {
        this.created = Date.now();
        if (type === 'shareReplay') {
            this.observable = getObservable().pipe(shareReplay(1));
        } else {
            this.observable = this._trigger.pipe(
                startWith(void 0 as void),
                debounceTime(0),
                switchReplay(getObservable),
            );
        }
    }

    reset(): void {
        this._trigger.next();
        this.created = 0;
    }
}

export class KeyCache<T = unknown> {
    private readonly _data: { [key: string]: KeyCacheEntry<T> } = {};

    get(
        key: string,
        getObservable: () => Observable<T>,
        type: CacheEntryType,
        duration = 0,
    ): Observable<T> {
        const entry = this._data[key];
        if (!entry || (duration > 0 && Date.now() - entry.created > duration * 1000)) {
            this._data[key] = new KeyCacheEntry(getObservable, type);
        }
        return this._data[key].observable;
    }

    reset(key: string): void {
        this._data[key]?.reset();
    }
}

/**
 * Share-replays responses with an externally controlled cache.
 *
 * Similar to shareReplayReturnValue, but since this function takes a `KeyCache` object, cache
 * entries can be invalidated.
 *
 * @duration
 * Duration (s) a key stays valid. A value of 0 indicates the cache is persistently valid
 */
export function cachedShareReplay<T>(cache: KeyCache<T>, keyFunction: (...args: any[]) => string) {
    return function (target: any, propertyKey: string, descriptor: PropertyDescriptor) {
        const originalFunction = descriptor.value;
        descriptor.value = function (this: any, ...args: any[]) {
            // console.log('called', propertyKey, args);
            const key = keyFunction(...args);
            return cache.get(key, () => originalFunction.apply(this, args), 'switchReplay');
        };
    };
}
/**
 * Share-replays responses with an externally controlled cache.
 *
 * Similar to shareReplayReturnValue, but since this function takes a `KeyCache` object, cache
 * entries can be invalidated.
 *
 * @duration
 * Duration (s) a key stays valid. A value of 0 indicates the cache is persistently valid
 */
export function cachedApiReplay<T>(
    cache: KeyCache<T>,
    keyFunction: (...args: any[]) => string,
    duration = 0,
) {
    return function (target: any, propertyKey: string, descriptor: PropertyDescriptor) {
        const originalFunction = descriptor.value;
        descriptor.value = function (this: any, ...args: any[]) {
            // console.log('called', propertyKey, args);
            const key = keyFunction(...args);
            return cache.get(
                key,
                () => originalFunction.apply(this, args),
                'shareReplay',
                duration,
            );
        };
    };
}
