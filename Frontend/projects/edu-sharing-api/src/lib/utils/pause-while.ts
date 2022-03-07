import * as rxjs from 'rxjs';
import { Observable } from 'rxjs';
import {
    bufferToggle,
    distinctUntilChanged,
    filter,
    mergeMap,
    share,
    shareReplay,
    windowToggle,
} from 'rxjs/operators';

/**
 * Rxjs pipe operator to pause a stream and replay missed values when resumed.
 *
 * @param paused$ An observable that resolves to `true` while the stream should be paused and to
 * `false` while it should be active.
 */
// From https://kddsky.medium.com/pauseable-observables-in-rxjs-58ce2b8c7dfd.
export function pauseWhile<T>(paused$: Observable<boolean>): rxjs.MonoTypeOperatorFunction<T> {
    const pausedDistinct$ = paused$.pipe(distinctUntilChanged(), shareReplay(1));
    const on$ = pausedDistinct$.pipe(filter((paused) => !paused));
    const off$ = pausedDistinct$.pipe(filter((paused) => paused));
    return (source$: Observable<T>) => {
        const sharedSource$ = source$.pipe(share());
        return rxjs.merge(
            // Accumulate values while `off` and emit accumulated values when switching `on`.
            sharedSource$.pipe(
                bufferToggle(off$, () => on$),
                // Expand the array returned by `bufferToggle` to individual emits.
                mergeMap((x) => x),
            ),
            // Emit values normally while `on`.
            sharedSource$.pipe(
                windowToggle(on$, () => off$),
                // Resolve the observable returned by `windowToggle`.
                mergeMap((x) => x),
            ),
        );
    };
}
