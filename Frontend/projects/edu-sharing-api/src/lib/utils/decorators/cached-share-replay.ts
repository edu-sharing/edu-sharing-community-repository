import { Observable, Subject } from 'rxjs';
import { debounceTime, startWith } from 'rxjs/operators';
import { switchReplay } from '../rxjs-operators/switch-replay';

class KeyCacheEntry<T> {
    private readonly _trigger = new Subject<void>();
    readonly observable: Observable<T>;

    constructor(getObservable: () => Observable<T>) {
        this.observable = this._trigger.pipe(
            startWith(void 0 as void),
            debounceTime(0),
            switchReplay(getObservable),
        );
    }

    reset(): void {
        this._trigger.next();
    }
}

export class KeyCache<T = unknown> {
    private readonly _data: { [key: string]: KeyCacheEntry<T> } = {};

    get(key: string, getObservable: () => Observable<T>): Observable<T> {
        if (!(key in this._data)) {
            this._data[key] = new KeyCacheEntry(getObservable);
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
 */
export function cachedShareReplay<T>(cache: KeyCache<T>, keyFunction: (...args: any[]) => string) {
    return function (target: any, propertyKey: string, descriptor: PropertyDescriptor) {
        const originalFunction = descriptor.value;
        descriptor.value = function (this: any, ...args: any[]) {
            // console.log('called', propertyKey, args);
            const key = keyFunction(...args);
            return cache.get(key, () => originalFunction.apply(this, args));
        };
    };
}
